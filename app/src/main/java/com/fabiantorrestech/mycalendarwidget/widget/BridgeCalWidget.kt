package com.fabiantorrestech.mycalendarwidget.widget

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
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfig
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfigRepository
import com.fabiantorrestech.mycalendarwidget.data.WidgetSyncLinkRepository
import com.fabiantorrestech.mycalendarwidget.ui.theme.DarkColors
import com.fabiantorrestech.mycalendarwidget.ui.theme.LightColors
import kotlinx.coroutines.Dispatchers
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
        val calRepo = CalendarRepository(context)

        provideContent {
            val config = androidx.compose.runtime.produceState(
                initialValue = WidgetConfig(),
                producer = {
                    configRepo.configFlow.collect { value = it }
                }
            ).value

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
                    glanceId = id
                )
            }
        }
    }
}

class BridgeCalWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BridgeCalWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach {
            WidgetSyncScheduler.cancel(context, it)
            WidgetSyncLinkRepository.clearAllLinksFor(context, it)
            WidgetConfigRepository.clearCache(it)
        }
    }
}
