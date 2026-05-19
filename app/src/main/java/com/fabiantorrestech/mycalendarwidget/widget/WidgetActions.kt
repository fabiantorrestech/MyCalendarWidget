package com.fabiantorrestech.mycalendarwidget.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfigRepository
import kotlinx.coroutines.flow.first

val targetOffsetKey = ActionParameters.Key<Int>("targetOffset")

class UpdateMonthOffsetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val targetOffset = parameters[targetOffsetKey] ?: return
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        val repo = WidgetConfigRepository(context, appWidgetId)
        val current = repo.configFlow.first()
        repo.updateConfig(current.copy(monthOffset = targetOffset))
        BridgeCalWidget().update(context, glanceId)
    }
}
