package com.fabiantorrestech.mycalendarwidget.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
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

    /** The configuration the card was last drawn for, to tell which changes matter. */
    private lateinit var lastConfig: Configuration

    /**
     * Ticks are held while the screen is off (see [TickGate]); this runs them as it
     * comes back on. Only a running app can hear the screen come on, which is why
     * holding ticks is limited to the Pinned placement.
     */
    private val screenOnReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            scope.launch(Dispatchers.IO) { TickGate.catchUp(applicationContext) }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        lastConfig = Configuration(resources.configuration)
        ContextCompat.registerReceiver(
            this,
            screenOnReceiver,
            IntentFilter(Intent.ACTION_SCREEN_ON),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
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

    /**
     * The card's colour is the theme's accent container and the strip is drawn at the
     * screen's width, so a theme, wallpaper-colour or rotation change reposts it. Changes
     * that cannot alter anything drawn are ignored.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val changes = lastConfig.diff(newConfig)
        lastConfig = Configuration(newConfig)
        if (changes and IRRELEVANT_CONFIG_CHANGES.inv() == 0) return
        scope.launch { DensityNotifier.refresh(this@PinnedNotificationService) }
    }

    override fun onDestroy() {
        isRunning = false
        unregisterReceiver(screenOnReceiver)
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

        /**
         * Configuration changes that cannot alter the card: input hardware, font size
         * (the bitmaps hold no text; the shade lays out the text itself) and the SIM's
         * country and network codes.
         */
        private const val IRRELEVANT_CONFIG_CHANGES =
            ActivityInfo.CONFIG_KEYBOARD or
                ActivityInfo.CONFIG_KEYBOARD_HIDDEN or
                ActivityInfo.CONFIG_NAVIGATION or
                ActivityInfo.CONFIG_TOUCHSCREEN or
                ActivityInfo.CONFIG_FONT_SCALE or
                ActivityInfo.CONFIG_MCC or
                ActivityInfo.CONFIG_MNC

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
