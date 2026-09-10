package com.fabiantorrestech.mycalendarwidget.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Refreshes all placed widgets when the day, time, or timezone changes so the displayed
 * "today" rolls over promptly (e.g. at midnight) instead of waiting for the next backstop
 * alarm. These actions are protected broadcasts that are still delivered to manifest-declared
 * receivers on API 26+.
 *
 * `TIME_SET`, `TIMEZONE_CHANGED`, and `MY_PACKAGE_REPLACED` (an app update) can all invalidate a
 * previously-armed [MidnightScheduler] alarm's target instant (or, for a package replace, mean
 * alarms were never re-armed after an update at all), so each of those also re-arms it.
 */
class DateChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val rearmMidnight = when (intent.action) {
            Intent.ACTION_DATE_CHANGED -> false
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> true
            else -> return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                if (rearmMidnight) {
                    MidnightScheduler.schedule(context)
                }
                val manager = GlanceAppWidgetManager(context)
                val ids = manager.getGlanceIds(BridgeCalWidget::class.java)
                ids.forEach { BridgeCalWidget().update(context, it) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
