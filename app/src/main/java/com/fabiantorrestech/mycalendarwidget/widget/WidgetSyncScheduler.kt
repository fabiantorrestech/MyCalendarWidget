package com.fabiantorrestech.mycalendarwidget.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock

object WidgetSyncScheduler {

    private const val ACTION_SYNC = "com.fabiantorrestech.mycalendarwidget.WIDGET_SYNC"

    fun schedule(context: Context, appWidgetId: Int, intervalMinutes: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = buildPendingIntent(context, appWidgetId)
        am.cancel(pi)
        if (intervalMinutes > 0) {
            val intervalMs = intervalMinutes * 60_000L
            am.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + intervalMs,
                intervalMs,
                pi
            )
        }
    }

    fun cancel(context: Context, appWidgetId: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(buildPendingIntent(context, appWidgetId))
    }

    private fun buildPendingIntent(context: Context, appWidgetId: Int): PendingIntent {
        val intent = Intent(context, WidgetSyncReceiver::class.java).apply {
            action = ACTION_SYNC
            putExtra("appWidgetId", appWidgetId)
        }
        return PendingIntent.getBroadcast(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
