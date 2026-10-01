package com.fabiantorrestech.mycalendarwidget.notification

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.fabiantorrestech.mycalendarwidget.R
import com.fabiantorrestech.mycalendarwidget.data.CalendarRepository
import com.fabiantorrestech.mycalendarwidget.data.NotificationRowTap
import com.fabiantorrestech.mycalendarwidget.data.NotificationPrefsRepository
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfig
import com.fabiantorrestech.mycalendarwidget.data.WidgetProfileRepository
import com.fabiantorrestech.mycalendarwidget.data.density.DensityRepository
import com.fabiantorrestech.mycalendarwidget.widget.BridgeCalWidgetReceiver
import com.fabiantorrestech.mycalendarwidget.widget.WidgetClickActions
import com.fabiantorrestech.mycalendarwidget.widget.peek.PeekList
import com.fabiantorrestech.mycalendarwidget.widget.use24Hour
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

/**
 * Posts, refreshes and removes the persistent density notification. It has no settings
 * of its own beyond [NotificationPrefsRepository]: it draws the followed widget's active
 * profile, the same config that widget composes from.
 *
 * It is an ordinary ongoing notification, not a foreground service (see
 * doc/adr/0002): every trigger — its tick, the widgets' calendar-change, date-change and
 * midnight paths, a settings save, boot, and being swiped away — calls [refresh].
 */
object DensityNotifier {

    const val CHANNEL_ID = "density_persistent"
    private const val NOTIFICATION_ID = 0x44454E53
    private const val TAG = "DensityNotifier"

    /** Brings the notification up to date, or removes it if it is off. Never throws. */
    suspend fun refresh(context: Context) {
        try {
            refreshOrThrow(context.applicationContext)
        } catch (e: Exception) {
            Log.w(TAG, "Notification refresh failed", e)
        }
    }

    /** Removes the notification and stops its tick. */
    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        NotificationScheduler.cancel(context)
    }

    /** True when [widgetId] is still a placed BridgeCal widget. */
    fun isPlaced(context: Context, widgetId: Int): Boolean =
        AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, BridgeCalWidgetReceiver::class.java))
            .contains(widgetId)

    // notify() is only reached after areNotificationsEnabled(), which is false whenever
    // POST_NOTIFICATIONS has not been granted on Android 13+.
    @SuppressLint("MissingPermission")
    private suspend fun refreshOrThrow(context: Context) {
        val prefsRepo = NotificationPrefsRepository(context)
        val prefs = prefsRepo.prefsFlow.first()
        if (!prefs.enabled) {
            cancel(context)
            return
        }
        if (!isPlaced(context, prefs.followedWidgetId)) {
            prefsRepo.disable()
            cancel(context)
            return
        }

        // Re-arm the tick before the work below, so a failure never loses the next one.
        NotificationScheduler.schedule(context)

        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        ensureChannel(context)

        val config = WidgetProfileRepository(context, prefs.followedWidgetId).activeConfigFlow.first()
        val notification = withContext(Dispatchers.IO) {
            build(context, config, prefs.followedWidgetId, prefs.rowTap)
        }
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel(context: Context) {
        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
            .setName(context.getString(R.string.notification_channel_name))
            .setDescription(context.getString(R.string.notification_channel_description))
            .setShowBadge(false)
            .build()
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    /** Off the main thread: both repositories query the calendar provider synchronously. */
    private fun build(
        context: Context,
        config: WidgetConfig,
        widgetId: Int,
        rowTap: NotificationRowTap
    ): Notification {
        val zone = ZoneId.systemDefault()
        val snapshot = DensityRepository(context).load(config, zone = zone)

        if (!snapshot.hasPermission) {
            val settings = WidgetClickActions.settingsIntent(context)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            val prompt = DensityNotificationRenderer.noPermission(context)
            return baseBuilder(context, activityIntent(context, settings, 0))
                .setCustomContentView(prompt)
                .build()
        }

        val eventsByDay = CalendarRepository(context).getEventsByDay(PeekList.peekQueryConfig(config))
        val today = Instant.ofEpochMilli(snapshot.nowMillis).atZone(zone).toLocalDate()
        val agenda = NotificationAgenda.rows(eventsByDay, today, snapshot.nowMillis)

        val views = DensityNotificationRenderer.render(
            context = context,
            config = config,
            snapshot = snapshot,
            agenda = agenda,
            use24Hour = use24Hour(context),
            zone = zone,
            rowIntent = { event ->
                when (rowTap) {
                    NotificationRowTap.OPEN_EVENT ->
                        activityIntent(context, WidgetClickActions.eventIntent(event, config), event.id.hashCode())
                    // No handler: the tap falls through to the notification's content
                    // intent, which opens the calendar app.
                    NotificationRowTap.OPEN_CALENDAR -> null
                }
            }
        )

        val openCalendar = activityIntent(context, WidgetClickActions.headerIntent(config), 0)

        // The lock screen gets the collapsed view only: the density headline and strip,
        // no event titles.
        val publicVersion = baseBuilder(context, openCalendar)
            .setCustomContentView(views.collapsed)
            .build()

        return baseBuilder(context, openCalendar)
            .setCustomContentView(views.collapsed)
            .setCustomBigContentView(views.expanded)
            .setPublicVersion(publicVersion)
            .build()
    }

    private fun baseBuilder(context: Context, contentIntent: PendingIntent): NotificationCompat.Builder =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_calendar_open)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setLocalOnly(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(contentIntent)
            .setDeleteIntent(dismissedIntent(context))

    /** Opened straight from the notification: Android 12+ forbids receiver trampolines. */
    private fun activityIntent(context: Context, intent: Intent, requestCode: Int): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun dismissedIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, NotificationReceiver::class.java).setAction(NotificationReceiver.ACTION_DISMISSED),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
