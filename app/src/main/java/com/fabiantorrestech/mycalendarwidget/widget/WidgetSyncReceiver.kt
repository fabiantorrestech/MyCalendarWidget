package com.fabiantorrestech.mycalendarwidget.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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
                        BridgeCalWidget().update(context, it)
                    }
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
                val manager = GlanceAppWidgetManager(context)
                val glanceId = manager.getGlanceIds(BridgeCalWidget::class.java)
                    .firstOrNull { manager.getAppWidgetId(it) == appWidgetId }
                if (glanceId != null) {
                    BridgeCalWidget().update(context, glanceId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
