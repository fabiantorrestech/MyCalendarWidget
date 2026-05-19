package com.fabiantorrestech.mycalendarwidget.automation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.fabiantorrestech.mycalendarwidget.data.AutomationProfile
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfigRepository
import com.fabiantorrestech.mycalendarwidget.widget.BridgeCalWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ProfileReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION = "com.fabiantorrestech.mycalendarwidget.SET_PROFILE"
        const val EXTRA_PROFILE = "profile"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val profileName = intent.getStringExtra(EXTRA_PROFILE) ?: return
        val profile = runCatching { AutomationProfile.valueOf(profileName.uppercase()) }.getOrNull() ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val manager = GlanceAppWidgetManager(context)
                val ids = manager.getGlanceIds(BridgeCalWidget::class.java)
                ids.forEach { glanceId ->
                    val appWidgetId = manager.getAppWidgetId(glanceId)
                    WidgetConfigRepository(context, appWidgetId).applyProfile(profile)
                    BridgeCalWidget().update(context, glanceId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
