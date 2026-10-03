package com.fabiantorrestech.mycalendarwidget.spike

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.fabiantorrestech.mycalendarwidget.R

/** THROWAWAY SPIKE: does a colorized foreground-service notification rank above messages? */
class SpikePinService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, "density_top")
            .setSmallIcon(R.drawable.ic_calendar_open)
            .setContentTitle("SPIKE pinned (colorized FGS)")
            .setContentText("Should sit above messages, below calls")
            .setColorized(true)
            .setColor(0xFFD0BCFF.toInt())
            .setOngoing(true)
            .build()
        startForeground(0x5350494B, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        return START_STICKY
    }
}
