package com.fabiantorrestech.mycalendarwidget.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.fabiantorrestech.mycalendarwidget.notification.DensityNotifier
import com.fabiantorrestech.mycalendarwidget.notification.TickGate
import com.fabiantorrestech.mycalendarwidget.widget.peek.PeekState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Every refresh that reaches a widget here also closes its peek sheet. The peek is a
 * momentary "what's next" glance, not a mode: a widget that quietly repainted itself
 * under an open sheet would leave the user reading a list whose backdrop had moved on,
 * and at midnight would leave yesterday's list on screen. Closing before the update
 * means the very same update redraws the widget in its normal state — one repaint, not
 * two. [PeekState]'s TTL is the backstop for the case where no refresh is scheduled at
 * all; this is the primary path.
 *
 * `CalendarUpdateReceiver` deliberately does *not* do this: a calendar sync firing while
 * the user is reading the sheet should refresh its contents, not snatch it away.
 */
class WidgetSyncReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == MidnightScheduler.ACTION_MIDNIGHT) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                try {
                    // Re-arm tomorrow's alarm first: if updating widgets below throws, we must
                    // not lose the next midnight rollover.
                    MidnightScheduler.schedule(context)
                    val manager = GlanceAppWidgetManager(context)
                    manager.getGlanceIds(BridgeCalWidget::class.java).forEach {
                        PeekState.close(context, it)
                        BridgeCalWidget().update(context, it)
                    }
                    DensityNotifier.refresh(context)
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        val appWidgetId = intent.getIntExtra("appWidgetId", AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                // A tick that would only repeat the notification's refresh of this widget,
                // or that no one could see with the screen off, is skipped (see TickGate).
                val followed = DensityNotifier.isFollowing(context, appWidgetId)
                if (TickGate.admitWidgetTick(context, appWidgetId, followed)) {
                    syncWidget(context, appWidgetId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        /**
         * One widget's tick: close its peek, redraw it, and refresh the notification if
         * it follows this widget. Also run by TickGate for a tick held while the screen
         * was off.
         */
        suspend fun syncWidget(context: Context, appWidgetId: Int) {
            val manager = GlanceAppWidgetManager(context)
            val glanceId = manager.getGlanceIds(BridgeCalWidget::class.java)
                .firstOrNull { manager.getAppWidgetId(it) == appWidgetId }
            if (glanceId != null) {
                PeekState.close(context, glanceId)
                BridgeCalWidget().update(context, glanceId)
            }
            DensityNotifier.refreshIfFollowing(context, appWidgetId)
        }
    }
}
