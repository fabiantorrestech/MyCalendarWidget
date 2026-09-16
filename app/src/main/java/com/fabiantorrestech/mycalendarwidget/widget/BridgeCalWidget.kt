package com.fabiantorrestech.mycalendarwidget.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.os.Build
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.material3.ColorProviders
import com.fabiantorrestech.mycalendarwidget.data.CalendarRepository
import com.fabiantorrestech.mycalendarwidget.data.CycleUiStyle
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfigRepository
import com.fabiantorrestech.mycalendarwidget.data.WidgetNameRepository
import com.fabiantorrestech.mycalendarwidget.data.WidgetProfileEntry
import com.fabiantorrestech.mycalendarwidget.data.WidgetProfileRepository
import com.fabiantorrestech.mycalendarwidget.data.WidgetStyle
import com.fabiantorrestech.mycalendarwidget.data.WidgetSyncLinkRepository
import com.fabiantorrestech.mycalendarwidget.data.density.DensityRepository
import com.fabiantorrestech.mycalendarwidget.data.density.DensitySnapshot
import com.fabiantorrestech.mycalendarwidget.ui.theme.DarkColors
import com.fabiantorrestech.mycalendarwidget.ui.theme.LightColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.glance.GlanceTheme
import java.time.LocalDate
import com.fabiantorrestech.mycalendarwidget.data.CalendarEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.datastore.preferences.core.Preferences
import androidx.glance.currentState
import android.util.SizeF
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.os.BundleCompat
import androidx.glance.LocalSize
import androidx.glance.appwidget.LocalAppWidgetOptions
import com.fabiantorrestech.mycalendarwidget.widget.peek.PeekList
import com.fabiantorrestech.mycalendarwidget.widget.peek.PeekState

class BridgeCalWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val configRepo = WidgetConfigRepository(context, appWidgetId)
        val profileRepo = WidgetProfileRepository(context, appWidgetId)
        val calRepo = CalendarRepository(context)

        profileRepo.migrateIfNeeded(configRepo.configFlow.first())
        // Per-widget identity — read from its own (real appWidgetId) store, not the shared config.
        val widgetName = WidgetNameRepository.getName(context, appWidgetId)
        // Read the real, already-stored config before the first composition rather than
        // starting from WidgetConfig() defaults: with the defaults' widgetStyle (not
        // DENSITY), the first frame of a density widget would otherwise think isDensity
        // is false and query CalendarRepository for event content on every cold render,
        // before the real config flow even emits once.
        val initialConfig = profileRepo.activeConfigFlow.first()

        provideContent {
            val config = produceState(initialValue = initialConfig) {
                profileRepo.activeConfigFlow.collect { value = it }
            }.value.copy(widgetName = widgetName)

            val profiles = produceState(initialValue = emptyList<WidgetProfileEntry>()) {
                profileRepo.profilesFlow.collect { value = it }
            }.value

            val activeProfileId = produceState(initialValue = "") {
                profileRepo.activeProfileIdFlow.collect { value = it }
            }.value

            val cycleUiStyle = produceState(initialValue = CycleUiStyle.PILL) {
                profileRepo.cycleUiStyleFlow.collect { value = it }
            }.value

            // The density style is content-free: it must never load event text at all, so the
            // branch sits above the fetch rather than inside the renderer.
            val isDensity = config.widgetStyle == WidgetStyle.DENSITY

            // Read inside provideContent, not in provideGlance: currentState<Preferences>()
            // does see a write made by an ActionCallback followed by update() (verified on
            // device), so the peek needs no separate getAppWidgetState read. The expiry is
            // compared here rather than stored as a boolean, which is what lets the sheet
            // lapse on its own if nothing ever closes it (see PeekState).
            val peekOpen = PeekState.isOpen(currentState<Preferences>(), System.currentTimeMillis())

            val densitySnapshot by produceState<DensitySnapshot?>(
                initialValue = null,
                key1 = config
            ) {
                value = if (isDensity) {
                    withContext(Dispatchers.IO) { DensityRepository(context).load(config) }
                } else {
                    null
                }
            }

            // The density style is content-free until the user asks for content: the
            // repository is queried only when the peek is actually open, so a widget
            // sitting closed on the home screen never reads an event title at all. The
            // key includes peekOpen so opening (or the TTL closing) the sheet re-runs it.
            val loadEvents = !isDensity || peekOpen
            val eventsByDay by produceState<Map<LocalDate, List<CalendarEvent>>>(
                initialValue = emptyMap(),
                key1 = config,
                key2 = peekOpen
            ) {
                value = if (!loadEvents) {
                    emptyMap()
                } else {
                    withContext(Dispatchers.IO) {
                        calRepo.getEventsByDay(
                            if (isDensity) PeekList.peekQueryConfig(config) else config
                        )
                    }
                }
            }

            val colors = if (config.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ColorProviders(
                    light = dynamicLightColorScheme(context),
                    dark = dynamicDarkColorScheme(context)
                )
            } else {
                ColorProviders(light = LightColors, dark = DarkColors)
            }

            // A launcher that never reports a size (see HostSize) leaves Glance composing
            // for the provider minimum, 180x40dp, however big the launcher actually draws
            // the widget. Read the same options bundle Glance's own size pass reads (it is
            // refreshed on onAppWidgetOptionsChanged) and, when it is empty, shadow
            // LocalSize with an assumed full-width size so the density layout is not cut
            // down to its compact form. Only the density content reads LocalSize, so the
            // other styles are untouched; the RemoteViews size map still comes from
            // Glance's own SizeBox, not from this local.
            val options = LocalAppWidgetOptions.current
            val sizesCount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                BundleCompat.getParcelableArrayList(
                    options, AppWidgetManager.OPTION_APPWIDGET_SIZES, SizeF::class.java
                )?.size ?: 0
            } else {
                0
            }
            val hostReportsSize = HostSize.reportsSize(
                sizesCount = sizesCount,
                minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0),
                maxWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0),
                minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0),
                maxHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
            )
            val assumedSize = DpSize(
                HostSize.assumedWidthDp(context.resources.configuration.screenWidthDp.toFloat()).dp,
                HostSize.ASSUMED_HEIGHT_DP.dp
            )

            val themed: @androidx.compose.runtime.Composable () -> Unit = {
                GlanceTheme(colors = colors) {
                    BridgeCalWidgetContent(
                        eventsByDay = eventsByDay,
                        config = config,
                        context = context,
                        glanceId = id,
                        profiles = profiles,
                        activeProfileId = activeProfileId,
                        cycleUiStyle = cycleUiStyle,
                        densitySnapshot = densitySnapshot,
                        use24Hour = remember(context) { use24Hour(context) },
                        peekOpen = peekOpen
                    )
                }
            }
            if (hostReportsSize) {
                themed()
            } else {
                CompositionLocalProvider(LocalSize provides assumedSize) { themed() }
            }
        }
    }
}

class BridgeCalWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BridgeCalWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        // Ensure every widget has a refresh alarm scheduled, even one that was just added or
        // whose settings were never opened. Instant mode (interval 0) still gets a backstop.
        // Glance's own GlanceAppWidgetReceiver.onReceive already runs inside a goAsync() of its
        // own, so this nested goAsync() returns null whenever the process is cold-started by the
        // APPWIDGET_UPDATE broadcast that invokes onUpdate (e.g. right after install, after a
        // force-stop, or at boot) — treat the result as nullable rather than crashing on it.
        val pendingResult: BroadcastReceiver.PendingResult? = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                appWidgetIds.forEach { id ->
                    // The style a widget actually renders with lives in the profile repo's
                    // active config, not the legacy WidgetConfigRepository store.
                    val cfg = WidgetProfileRepository(context, id).activeConfigFlow.first()
                    WidgetSyncScheduler.schedule(context, id, WidgetSyncScheduler.effectiveIntervalMinutes(cfg))
                }
                MidnightScheduler.schedule(context)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach {
            WidgetSyncScheduler.cancel(context, it)
            WidgetSyncLinkRepository.clearAllLinksFor(context, it)
            WidgetNameRepository.clear(context, it)
            WidgetConfigRepository.clearCache(it)
            WidgetProfileRepository.clearCache(it)
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        MidnightScheduler.cancel(context)
    }
}
