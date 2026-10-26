package com.fabiantorrestech.mycalendarwidget.widget.peek

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.fabiantorrestech.mycalendarwidget.data.WidgetProfileRepository
import com.fabiantorrestech.mycalendarwidget.data.WidgetStyle
import com.fabiantorrestech.mycalendarwidget.widget.BridgeCalWidget
import kotlinx.coroutines.flow.first

/**
 * "Show the agenda on wake": every [WidgetConfig.densityWakeCycleEvery]th time the screen
 * turns on, a density widget with the option on opens on its peek, so the agenda is in
 * view without a tap; every other wake it is back on the strip. The count lives in each
 * widget's Glance state, next to [PeekState]'s.
 *
 * Only a running app hears the screen turn on, so this runs from the Pinned
 * notification's service ([com.fabiantorrestech.mycalendarwidget.notification.PinnedNotificationService]);
 * with the notification off or on another placement the option does nothing.
 */
object WakeCycle {

    private const val TAG = "WakeCycle"

    val COUNT: Preferences.Key<Int> = intPreferencesKey("wake_cycle_count")

    /** True on the wakes that open the agenda: every [every]th one, never for [every] < 2. */
    fun showsAgenda(wakeCount: Int, every: Int): Boolean =
        every >= 2 && wakeCount > 0 && wakeCount % every == 0

    /**
     * The screen came on. Runs after `TickGate.catchUp`, whose held widget ticks close
     * the peek: run first, this would open the agenda only for that tick to shut it.
     */
    suspend fun onScreenOn(context: Context) {
        val manager = GlanceAppWidgetManager(context)
        manager.getGlanceIds(BridgeCalWidget::class.java).forEach { glanceId ->
            val appWidgetId = manager.getAppWidgetId(glanceId)
            val config = WidgetProfileRepository(context, appWidgetId).activeConfigFlow.first()
            if (config.widgetStyle != WidgetStyle.DENSITY || !config.densityWakeCycle) return@forEach

            var count = 0
            updateAppWidgetState(context, glanceId) { prefs ->
                count = (prefs[COUNT] ?: 0) + 1
                prefs[COUNT] = count
            }
            val agenda = showsAgenda(count, config.densityWakeCycleEvery)
            Log.d(TAG, "Widget $appWidgetId wake $count: ${if (agenda) "agenda" else "strip"}")
            if (agenda) PeekState.openFromWake(context, glanceId) else PeekState.close(context, glanceId)
            BridgeCalWidget().update(context, glanceId)
        }
    }
}
