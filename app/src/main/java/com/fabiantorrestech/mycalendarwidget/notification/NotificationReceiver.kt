package com.fabiantorrestech.mycalendarwidget.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fabiantorrestech.mycalendarwidget.data.NotificationPrefsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Every path back into the persistent notification that is not already a widget
 * refresh: its own tick ([NotificationScheduler]), the user swiping it away (Android 14+
 * allows that even for an ongoing notification, so it is simply posted again), an
 * arrow tap, the ↻ button, and boot. Each runs [DensityNotifier.refresh], which also
 * re-arms the tick and does nothing when the notification is turned off.
 *
 * The tick, boot and the ↻ button redraw the followed widget as well, so the widget
 * refreshes as often as the notification does. A swipe or an arrow tap changes nothing
 * the widget shows, so those leave it alone.
 */
class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alsoWidget = when (intent.action) {
            NotificationScheduler.ACTION_TICK, Intent.ACTION_BOOT_COMPLETED, ACTION_REFRESH -> true
            ACTION_DISMISSED, ACTION_PAGE -> false
            else -> return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                if (intent.action == ACTION_PAGE) {
                    NotificationPrefsRepository(context).setPage(
                        offset = intent.getIntExtra(EXTRA_TARGET, 0),
                        nowMillis = System.currentTimeMillis()
                    )
                }
                DensityNotifier.refresh(context, alsoWidget)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_DISMISSED = "com.fabiantorrestech.mycalendarwidget.NOTIFICATION_DISMISSED"
        const val ACTION_PAGE = "com.fabiantorrestech.mycalendarwidget.NOTIFICATION_PAGE"
        const val ACTION_REFRESH = "com.fabiantorrestech.mycalendarwidget.NOTIFICATION_REFRESH"

        /** The page (or day) offset an arrow points at; see [NotificationPaging]. */
        const val EXTRA_TARGET = "target"
    }
}
