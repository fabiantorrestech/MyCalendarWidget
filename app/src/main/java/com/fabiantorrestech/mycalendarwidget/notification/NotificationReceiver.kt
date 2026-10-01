package com.fabiantorrestech.mycalendarwidget.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Every path back into the persistent notification that is not already a widget
 * refresh: its own tick ([NotificationScheduler]), the user swiping it away (Android 14+
 * allows that even for an ongoing notification, so it is simply posted again), and boot.
 * Each just runs [DensityNotifier.refresh], which also re-arms the tick and does nothing
 * when the notification is turned off.
 */
class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            NotificationScheduler.ACTION_TICK,
            ACTION_DISMISSED,
            Intent.ACTION_BOOT_COMPLETED -> Unit
            else -> return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                DensityNotifier.refresh(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_DISMISSED = "com.fabiantorrestech.mycalendarwidget.NOTIFICATION_DISMISSED"
    }
}
