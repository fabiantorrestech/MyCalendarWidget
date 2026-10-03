package com.fabiantorrestech.mycalendarwidget.notification

import android.content.Context
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import com.fabiantorrestech.mycalendarwidget.widget.WidgetSyncReceiver
import java.util.concurrent.ConcurrentHashMap

/**
 * Decides whether a periodic five-minute tick does its work now. Only the ticks pass
 * through here (the notification's [NotificationScheduler] alarm and each widget's
 * WidgetSyncScheduler alarm); calendar changes, midnight, boot, buttons and settings
 * always refresh at once.
 *
 * Two ticks are skipped, neither of which changes what anyone sees:
 * - **Already fresh.** The notification and the widget it follows are one pair, and
 *   both ticks refresh the whole pair, so whichever fires second within
 *   [PAIR_FRESH_MS] would only repeat the first.
 * - **Screen off, while Pinned.** Nobody can see the shade or the home screen, so the
 *   tick is noted instead, and [PinnedNotificationService] runs it via [catchUp] when
 *   the screen comes back on, before the lock screen is read. Without the pinning
 *   service nothing would hear the screen come on, so Top and Silent never skip.
 *
 * In-memory only: a new process starts with nothing fresh and nothing skipped, which
 * at worst costs one extra refresh.
 */
object TickGate {

    /**
     * Under the five-minute tick, so the pair's refresh never waits longer than now:
     * the tick that refreshed the pair is always due again before anything is skipped
     * past it.
     */
    const val PAIR_FRESH_MS = 4 * 60_000L

    private const val TAG = "TickGate"

    /**
     * elapsedRealtime of the last refresh of both the notification and its widget, or of
     * a tick let through to do one.
     */
    @Volatile
    private var pairRefreshedAt: Long? = null

    @Volatile
    private var notificationSkipped = false
    private val skippedWidgetIds: MutableSet<Int> = ConcurrentHashMap.newKeySet()

    /** The notification and its followed widget were both just redrawn. */
    fun markPairRefreshed() {
        pairRefreshedAt = SystemClock.elapsedRealtime()
    }

    fun pairIsFresh(): Boolean {
        val at = pairRefreshedAt ?: return false
        return SystemClock.elapsedRealtime() - at < PAIR_FRESH_MS
    }

    /** The notification's tick: true when it should run now, false when skipped. */
    fun admitNotificationTick(context: Context): Boolean {
        if (pairIsFresh()) {
            Log.d(TAG, "Notification tick skipped: pair already fresh")
            return false
        }
        if (screenOffWhilePinned(context)) {
            notificationSkipped = true
            Log.d(TAG, "Notification tick held until the screen comes on")
            return false
        }
        notificationSkipped = false
        // Claimed now, not when the refresh ends: the widget's tick often fires within the
        // second it takes, and must already see the pair as taken.
        markPairRefreshed()
        return true
    }

    /**
     * A widget's tick: true when it should run now. [followedByNotification] says the
     * notification follows this widget, so the pair's freshness counts for it.
     */
    fun admitWidgetTick(context: Context, appWidgetId: Int, followedByNotification: Boolean): Boolean {
        if (followedByNotification && pairIsFresh()) {
            Log.d(TAG, "Widget $appWidgetId tick skipped: pair already fresh")
            return false
        }
        if (screenOffWhilePinned(context)) {
            skippedWidgetIds += appWidgetId
            Log.d(TAG, "Widget $appWidgetId tick held until the screen comes on")
            return false
        }
        skippedWidgetIds -= appWidgetId
        // Claimed now, as in admitNotificationTick.
        if (followedByNotification) markPairRefreshed()
        return true
    }

    /**
     * The screen came on: run the ticks held while it was off. The widgets go first; the
     * followed one refreshes the notification with it, so the notification's own held
     * tick then finds the pair fresh and is not repeated.
     */
    suspend fun catchUp(context: Context) {
        val widgetIds = skippedWidgetIds.toList()
        skippedWidgetIds.removeAll(widgetIds.toSet())
        val notification = notificationSkipped
        notificationSkipped = false

        widgetIds.forEach { WidgetSyncReceiver.syncWidget(context, it) }
        if (notification && !pairIsFresh()) DensityNotifier.refresh(context, alsoWidget = true)
    }

    private fun screenOffWhilePinned(context: Context): Boolean =
        PinnedNotificationService.isRunning &&
            !context.getSystemService(PowerManager::class.java).isInteractive
}
