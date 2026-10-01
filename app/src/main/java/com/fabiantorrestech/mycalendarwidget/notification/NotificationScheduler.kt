package com.fabiantorrestech.mycalendarwidget.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.fabiantorrestech.mycalendarwidget.widget.WidgetSyncScheduler

/**
 * The persistent notification's own refresh tick, on the density widget's cadence
 * ([WidgetSyncScheduler.DENSITY_INTERVAL_MINUTES]) and the same inexact, non-waking
 * alarm: nobody reads the shade on a sleeping screen, so the device is never woken for it.
 */
object NotificationScheduler {

    const val ACTION_TICK = "com.fabiantorrestech.mycalendarwidget.NOTIFICATION_TICK"
    private const val REQUEST_CODE = 0x4E4F5449

    fun schedule(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context)
        am.cancel(pi)
        val intervalMs = WidgetSyncScheduler.DENSITY_INTERVAL_MINUTES * 60_000L
        am.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + intervalMs,
            intervalMs,
            pi
        )
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, NotificationReceiver::class.java).setAction(ACTION_TICK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
