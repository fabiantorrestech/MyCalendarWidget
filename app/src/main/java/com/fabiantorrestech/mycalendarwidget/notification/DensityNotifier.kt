package com.fabiantorrestech.mycalendarwidget.notification

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.fabiantorrestech.mycalendarwidget.R
import com.fabiantorrestech.mycalendarwidget.data.CalendarRepository
import com.fabiantorrestech.mycalendarwidget.data.NotificationLockScreen
import com.fabiantorrestech.mycalendarwidget.data.NotificationPagingMode
import com.fabiantorrestech.mycalendarwidget.data.NotificationPlacement
import com.fabiantorrestech.mycalendarwidget.data.NotificationPrefs
import com.fabiantorrestech.mycalendarwidget.data.NotificationPrefsRepository
import com.fabiantorrestech.mycalendarwidget.data.NotificationRowTap
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfig
import com.fabiantorrestech.mycalendarwidget.data.WidgetProfileRepository
import com.fabiantorrestech.mycalendarwidget.data.density.DensityRepository
import com.fabiantorrestech.mycalendarwidget.data.density.DensitySnapshot
import com.fabiantorrestech.mycalendarwidget.widget.BridgeCalWidget
import com.fabiantorrestech.mycalendarwidget.widget.BridgeCalWidgetReceiver
import com.fabiantorrestech.mycalendarwidget.widget.WidgetClickActions
import com.fabiantorrestech.mycalendarwidget.widget.peek.PeekList
import com.fabiantorrestech.mycalendarwidget.widget.use24Hour
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Posts, refreshes and removes the persistent density notification. It has no settings
 * of its own beyond [NotificationPrefsRepository]: it draws the followed widget's active
 * profile, the same config that widget composes from.
 *
 * It is an ordinary ongoing notification, not a foreground service (see
 * doc/adr/0002): every trigger — its tick, the widgets' calendar-change, date-change and
 * midnight paths, a widget's own tick and refresh button, a settings save, boot, an
 * arrow tap and being swiped away — calls [refresh] or [refreshIfFollowing].
 */
object DensityNotifier {

    /** IMPORTANCE_LOW: the shade's Silent section. */
    const val CHANNEL_SILENT = "density_persistent"

    /**
     * IMPORTANCE_DEFAULT with no sound or vibration: the main Notifications section and
     * the lock screen. A channel's importance cannot be raised in code once created,
     * which is why the two placements are two channels rather than one.
     */
    const val CHANNEL_TOP = "density_top"

    private const val NOTIFICATION_ID = 0x44454E53
    private const val TAG = "DensityNotifier"

    /** Distinct request codes for the arrows' broadcasts, so each keeps its own target. */
    private const val REQUEST_PAGE_PREVIOUS = 0x50
    private const val REQUEST_PAGE_NEXT = 0x51
    private const val REQUEST_PAGE_FIRST = 0x52
    private const val REQUEST_ADD_EVENT = 0x41
    private const val REQUEST_REFRESH = 0x53

    /**
     * One refresh at a time: two quick arrow taps must post their pages in order, never
     * the older one last.
     */
    private val refreshLock = Mutex()

    /**
     * Brings the notification up to date, or removes it if it is off. With [alsoWidget]
     * the followed widget is redrawn too, so the two never disagree; the widget-side
     * paths pass false, since they have just redrawn it themselves. Never throws.
     */
    suspend fun refresh(context: Context, alsoWidget: Boolean = false) {
        try {
            refreshLock.withLock { refreshOrThrow(context.applicationContext) }
            if (alsoWidget) updateFollowedWidget(context.applicationContext)
        } catch (e: Exception) {
            Log.w(TAG, "Notification refresh failed", e)
        }
    }

    /**
     * The card's "Repost notification" button: takes the notification down and posts it
     * afresh (re-arming its tick on the way), for when it has gone missing or looks
     * stuck. Does nothing when the notification is turned off. Never throws.
     */
    suspend fun repost(context: Context) {
        try {
            refreshLock.withLock {
                NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
                refreshOrThrow(context.applicationContext)
            }
            updateFollowedWidget(context.applicationContext)
        } catch (e: Exception) {
            Log.w(TAG, "Notification repost failed", e)
        }
    }

    /**
     * A widget refreshed itself: refresh the notification as well when it follows that
     * widget. Cheap when it does not: one preferences read.
     */
    suspend fun refreshIfFollowing(context: Context, appWidgetId: Int) {
        val prefs = NotificationPrefsRepository(context.applicationContext).prefsFlow.first()
        if (prefs.enabled && prefs.followedWidgetId == appWidgetId) refresh(context)
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
        ensureChannels(context)

        val channelId = channelFor(prefs.placement)
        // Moving between placements: take the old post down first, so the new one lands
        // in its new section with a fresh ranking rather than as an update in place.
        if (postedChannel(context) != channelId) manager.cancel(NOTIFICATION_ID)

        val config = WidgetProfileRepository(context, prefs.followedWidgetId).activeConfigFlow.first()
        val notification = withContext(Dispatchers.IO) { build(context, config, prefs) }
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun channelFor(placement: NotificationPlacement): String = when (placement) {
        NotificationPlacement.TOP -> CHANNEL_TOP
        NotificationPlacement.SILENT -> CHANNEL_SILENT
    }

    /** The channel the notification is currently posted on, or null if it is not up. */
    private fun postedChannel(context: Context): String? =
        context.getSystemService(NotificationManager::class.java)
            .activeNotifications
            .firstOrNull { it.id == NOTIFICATION_ID }
            ?.notification
            ?.channelId

    private fun ensureChannels(context: Context) {
        val description = context.getString(R.string.notification_channel_description)
        val top = NotificationChannelCompat.Builder(CHANNEL_TOP, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName(context.getString(R.string.notification_channel_top_name))
            .setDescription(description)
            .setSound(null, null)
            .setVibrationEnabled(false)
            .setLightsEnabled(false)
            .setShowBadge(false)
            .build()
        val silent = NotificationChannelCompat.Builder(CHANNEL_SILENT, NotificationManagerCompat.IMPORTANCE_LOW)
            .setName(context.getString(R.string.notification_channel_name))
            .setDescription(description)
            .setShowBadge(false)
            .build()
        NotificationManagerCompat.from(context).createNotificationChannelsCompat(listOf(top, silent))
    }

    /** Off the main thread: the repositories query the calendar provider synchronously. */
    private fun build(context: Context, config: WidgetConfig, prefs: NotificationPrefs): Notification {
        val zone = ZoneId.systemDefault()
        val densityRepo = DensityRepository(context)
        val snapshot = densityRepo.load(config, zone = zone)

        if (!snapshot.hasPermission) {
            val settings = WidgetClickActions.settingsIntent(context)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, prefs.followedWidgetId)
            val prompt = DensityNotificationRenderer.noPermission(context)
            return baseBuilder(context, prefs, activityIntent(context, settings, 0))
                .setCustomContentView(prompt)
                .build()
        }

        val eventsByDay = CalendarRepository(context).getEventsByDay(PeekList.peekQueryConfig(config))
        val today = Instant.ofEpochMilli(snapshot.nowMillis).atZone(zone).toLocalDate()
        val offset = NotificationPaging.effectiveOffset(prefs.pageOffset, prefs.pageTouchedAtMillis, snapshot.nowMillis)

        val (body, lastOffset) = when (prefs.pagingMode) {
            NotificationPagingMode.AGENDA_PAGES -> {
                val pages = NotificationAgenda.pages(eventsByDay, today, snapshot.nowMillis)
                val index = NotificationPaging.clamp(offset, pages.size)
                ExpandedBody.AgendaPage(pages.getOrElse(index) { emptyList() }, index, pages.size) to
                    maxOf(0, pages.size - 1)
            }
            NotificationPagingMode.DAYS -> {
                val featuredDate = snapshot.featured.date
                // The agenda query reaches daysAheadToLoad days past today; after the
                // evening rollover the featured day is already one of them.
                val maxOffset = maxOf(
                    0,
                    config.daysAheadToLoad - 1 - ChronoUnit.DAYS.between(today, featuredDate).toInt()
                )
                val dayOffset = NotificationPaging.clamp(offset, maxOffset + 1)
                val date = featuredDate.plusDays(dayOffset.toLong())
                ExpandedBody.Day(
                    day = dayDensity(densityRepo, config, snapshot, dayOffset, date, zone),
                    isToday = date == today,
                    rows = NotificationAgenda.dayRows(eventsByDay, date, snapshot.nowMillis),
                    offset = dayOffset,
                    maxOffset = maxOffset
                ) to maxOffset
            }
        }
        val current = when (body) {
            is ExpandedBody.AgendaPage -> body.pageIndex
            is ExpandedBody.Day -> body.offset
        }

        val views = DensityNotificationRenderer.render(
            context = context,
            config = config,
            snapshot = snapshot,
            body = body,
            nav = NavIntents(
                previous = pageIntent(context, REQUEST_PAGE_PREVIOUS, maxOf(0, current - 1)),
                next = pageIntent(context, REQUEST_PAGE_NEXT, minOf(lastOffset, current + 1)),
                first = pageIntent(context, REQUEST_PAGE_FIRST, 0)
            ),
            chrome = ChromeIntents(
                refresh = if (prefs.showRefreshButton) refreshIntent(context) else null,
                // The same "new event" screen the widget's + opens.
                add = if (prefs.showAddButton) {
                    activityIntent(context, WidgetClickActions.quickAddIntent(), REQUEST_ADD_EVENT)
                } else {
                    null
                }
            ),
            use24Hour = use24Hour(context),
            zone = zone,
            rowIntent = { event ->
                when (prefs.rowTap) {
                    NotificationRowTap.OPEN_EVENT ->
                        activityIntent(context, WidgetClickActions.eventIntent(event, config), event.id.hashCode())
                    // No handler: the tap falls through to the notification's content
                    // intent, which opens the calendar app.
                    NotificationRowTap.OPEN_CALENDAR -> null
                }
            }
        )

        val openCalendar = activityIntent(context, WidgetClickActions.headerIntent(config), 0)

        // The redacted lock-screen version: the density headline and strip, no event
        // titles and no + button. Android only swaps it in when the phone is set to hide
        // sensitive notification content on the lock screen; an app cannot force that
        // (a channel's lock-screen visibility is the user's to set, not the app's).
        val publicVersion = baseBuilder(context, prefs, openCalendar)
            .setCustomContentView(views.lockScreen)
            .build()

        return baseBuilder(context, prefs, openCalendar)
            .setCustomContentView(views.collapsed)
            .setCustomBigContentView(views.expanded)
            .setPublicVersion(publicVersion)
            .build()
    }

    /** The snapshot already holds the featured day and its look-ahead; query the rest. */
    private fun dayDensity(
        repo: DensityRepository,
        config: WidgetConfig,
        snapshot: DensitySnapshot,
        offset: Int,
        date: LocalDate,
        zone: ZoneId
    ) = when {
        offset == 0 -> snapshot.featured
        offset - 1 < snapshot.lookahead.size -> snapshot.lookahead[offset - 1]
        else -> repo.loadDay(config, date, zone)
    }

    private fun baseBuilder(
        context: Context,
        prefs: NotificationPrefs,
        contentIntent: PendingIntent
    ): NotificationCompat.Builder {
        val visibility = when (prefs.lockScreen) {
            // Private: the public version replaces it when the phone hides sensitive content.
            NotificationLockScreen.SHOW -> NotificationCompat.VISIBILITY_PRIVATE
            // Secret: nothing of it shows on a secure lock screen.
            NotificationLockScreen.HIDE -> NotificationCompat.VISIBILITY_SECRET
        }
        val builder = NotificationCompat.Builder(context, channelFor(prefs.placement))
            .setSmallIcon(R.drawable.ic_calendar_open)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setLocalOnly(true)
            .setVisibility(visibility)
            .setContentIntent(contentIntent)
            .setDeleteIntent(dismissedIntent(context))
        return when (prefs.placement) {
            // The top channel already has no sound or vibration. setSilent is left off
            // here because it files the post under a "silent" group of its own.
            NotificationPlacement.TOP -> builder.setPriority(NotificationCompat.PRIORITY_MAX)
            NotificationPlacement.SILENT -> builder.setSilent(true)
        }
    }

    /** Redraws the followed widget, found the same way WidgetSyncReceiver finds one. */
    private suspend fun updateFollowedWidget(context: Context) {
        val prefs = NotificationPrefsRepository(context).prefsFlow.first()
        if (!prefs.enabled) return
        val manager = GlanceAppWidgetManager(context)
        val glanceId = manager.getGlanceIds(BridgeCalWidget::class.java)
            .firstOrNull { manager.getAppWidgetId(it) == prefs.followedWidgetId }
            ?: return
        BridgeCalWidget().update(context, glanceId)
    }

    /** Opened straight from the notification: Android 12+ forbids receiver trampolines. */
    private fun activityIntent(context: Context, intent: Intent, requestCode: Int): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /** An arrow: tells the receiver which page (or day) to draw next. */
    private fun pageIntent(context: Context, requestCode: Int, target: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, NotificationReceiver::class.java)
                .setAction(NotificationReceiver.ACTION_PAGE)
                .putExtra(NotificationReceiver.EXTRA_TARGET, target),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /** The ↻ button: redraws the notification and its widget at once. */
    private fun refreshIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_REFRESH,
            Intent(context, NotificationReceiver::class.java).setAction(NotificationReceiver.ACTION_REFRESH),
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
