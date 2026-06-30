package com.fabiantorrestech.mycalendarwidget.widget

import android.appwidget.AppWidgetManager
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
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfig
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfigRepository
import com.fabiantorrestech.mycalendarwidget.data.WidgetNameRepository
import com.fabiantorrestech.mycalendarwidget.data.WidgetProfileEntry
import com.fabiantorrestech.mycalendarwidget.data.WidgetProfileRepository
import com.fabiantorrestech.mycalendarwidget.data.WidgetSyncLinkRepository
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

        provideContent {
            val config = produceState(initialValue = WidgetConfig()) {
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

            val eventsByDay by produceState<Map<LocalDate, List<CalendarEvent>>>(
                initialValue = emptyMap(),
                key1 = config
            ) {
                value = withContext(Dispatchers.IO) { calRepo.getEventsByDay(config) }
            }

            val colors = if (config.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ColorProviders(
                    light = dynamicLightColorScheme(context),
                    dark = dynamicDarkColorScheme(context)
                )
            } else {
                ColorProviders(light = LightColors, dark = DarkColors)
            }

            GlanceTheme(colors = colors) {
                BridgeCalWidgetContent(
                    eventsByDay = eventsByDay,
                    config = config,
                    context = context,
                    glanceId = id,
                    profiles = profiles,
                    activeProfileId = activeProfileId,
                    cycleUiStyle = cycleUiStyle
                )
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
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                appWidgetIds.forEach { id ->
                    val interval = WidgetConfigRepository(context, id).configFlow.first().syncIntervalMinutes
                    WidgetSyncScheduler.schedule(context, id, interval)
                }
            } finally {
                pendingResult.finish()
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
}
