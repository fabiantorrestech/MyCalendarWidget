package com.fabiantorrestech.mycalendarwidget.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.Instant
import java.time.ZoneId

/**
 * Arms a dedicated, inexact alarm that fires once at (or shortly after) local midnight so the
 * "today" every widget renders rolls over promptly even if no other broadcast happens to land
 * around that time. This is on top of — not a replacement for — the per-widget periodic sync
 * alarms in [WidgetSyncScheduler] and the best-effort [DateChangeReceiver].
 *
 * Deliberately uses [AlarmManager.RTC] (not `RTC_WAKEUP`): nobody is looking at a widget on a
 * dark, sleeping screen at midnight, so there is no reason to wake the device just to redraw it.
 * [WINDOW_MS] is 10 minutes — the verified minimum window `setWindow` is honored at on Android 12+
 * for non-exact alarms without holding an exact-alarm permission. Both the ±10-minute slack on
 * this alarm and the existing ±5-minute inexact-repeating tick for density widgets are accepted,
 * intentional imprecision — nothing here needs wall-clock-exact firing.
 */
object MidnightScheduler {

    const val ACTION_MIDNIGHT = "com.fabiantorrestech.mycalendarwidget.WIDGET_MIDNIGHT"
    private const val REQUEST_CODE = 0x4D49444E
    const val WINDOW_MS = 10 * 60_000L

    /**
     * Pure: the epoch-millis instant of the *next* local midnight strictly after [nowMillis] in
     * [zone]. No Android imports are touched by this function, so it is exercised directly by a
     * plain JUnit test. `LocalDate.atStartOfDay(zone)` resolves DST gaps (a zone whose clocks
     * skip over 00:00 on a transition night) to the first valid local instant instead of
     * throwing, which is exactly the behavior wanted here.
     */
    fun nextMidnightMillis(nowMillis: Long, zone: ZoneId): Long =
        Instant.ofEpochMilli(nowMillis)
            .atZone(zone)
            .toLocalDate()
            .plusDays(1)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()

    fun schedule(
        context: Context,
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setWindow(
            AlarmManager.RTC,
            nextMidnightMillis(nowMillis, zone),
            WINDOW_MS,
            pendingIntent(context)
        )
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, WidgetSyncReceiver::class.java).setAction(ACTION_MIDNIGHT)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
