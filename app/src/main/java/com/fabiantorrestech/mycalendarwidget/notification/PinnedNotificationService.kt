package com.fabiantorrestech.mycalendarwidget.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.fabiantorrestech.mycalendarwidget.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Holds the persistent notification as its foreground notification while the placement
 * is Pinned, which is what lets Pixel sort it above conversations and every ordinary
 * notification, below only calls (doc/adr/0003). It does no work of its own: the card is
 * built and refreshed by [DensityNotifier] exactly as in the other placements, and every
 * post with [DensityNotifier.NOTIFICATION_ID] while this runs stays its foreground one.
 *
 * Android lets a foreground service start only from the foreground or a few exempt
 * moments (boot, an app update, a notification action), so [tryStart] can fail;
 * [DensityNotifier] then posts the card unpinned until the next chance.
 */
class PinnedNotificationService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // startForeground is due within seconds of the start, and a busy device (just
        // booted, say) can take longer than that to build the card. So the service goes
        // foreground at once with whatever is already posted, or a bare placeholder, and
        // a refresh then posts the real card, which stays its foreground one. The same
        // refresh stops the service again if the notification is off or not Pinned.
        ServiceCompat.startForeground(
            this,
            DensityNotifier.NOTIFICATION_ID,
            postedCard() ?: placeholder(),
            foregroundType()
        )
        scope.launch { DensityNotifier.refresh(this@PinnedNotificationService) }
        // Restarted by the system if the process is killed: the card comes back pinned.
        return START_STICKY
    }

    /** The notification already up, unpinned, so going foreground does not blank it. */
    private fun postedCard(): Notification? =
        getSystemService(NotificationManager::class.java)
            .activeNotifications
            .firstOrNull { it.id == DensityNotifier.NOTIFICATION_ID }
            ?.notification

    /** The card's colour is the theme's accent container, so a theme switch reposts it. */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        scope.launch { DensityNotifier.refresh(this@PinnedNotificationService) }
    }

    override fun onDestroy() {
        isRunning = false
        ServiceCompat.stopForeground(
            this,
            if (removeOnStop) ServiceCompat.STOP_FOREGROUND_REMOVE else ServiceCompat.STOP_FOREGROUND_DETACH
        )
        removeOnStop = false
        scope.cancel()
        super.onDestroy()
    }

    private fun placeholder(): Notification =
        NotificationCompat.Builder(this, DensityNotifier.CHANNEL_TOP)
            .setSmallIcon(R.drawable.ic_calendar_open)
            .setContentTitle(getString(R.string.app_name))
            .setOngoing(true)
            .setSilent(true)
            .build()

    private fun foregroundType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }

    companion object {
        private const val TAG = "PinnedNotification"

        /** True between this process's onCreate and onDestroy of the service. */
        @Volatile
        var isRunning = false
            private set

        /** Read by onDestroy: whether stopping also takes the card down. */
        @Volatile
        private var removeOnStop = false

        /**
         * Starts the service, which then posts the pinned card itself. False when
         * Android refuses a foreground-service start from the background right now.
         */
        fun tryStart(context: Context): Boolean =
            try {
                ContextCompat.startForegroundService(context, Intent(context, PinnedNotificationService::class.java))
                true
            } catch (e: IllegalStateException) {
                // ForegroundServiceStartNotAllowedException on Android 12+.
                Log.i(TAG, "Not allowed to pin from here; posting unpinned", e)
                false
            }

        /**
         * Stops the service if it is running. With [removeNotification] the card goes
         * too (turning the notification off); without, it stays up, unpinned, for the
         * next post to replace (moving to another placement).
         */
        fun stop(context: Context, removeNotification: Boolean) {
            if (!isRunning) return
            removeOnStop = removeNotification
            context.stopService(Intent(context, PinnedNotificationService::class.java))
        }
    }
}
