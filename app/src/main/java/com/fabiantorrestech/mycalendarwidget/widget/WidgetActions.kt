package com.fabiantorrestech.mycalendarwidget.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfigRepository
import com.fabiantorrestech.mycalendarwidget.data.WidgetProfileRepository
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

class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        val repo = WidgetConfigRepository(context, appWidgetId)
        val current = repo.configFlow.first()
        repo.updateConfig(current.copy(refreshNonce = current.refreshNonce + 1))
        BridgeCalWidget().update(context, glanceId)
    }
}

val cycleDirectionKey = ActionParameters.Key<Int>("cycleDirection")
val targetProfileIdKey = ActionParameters.Key<String>("targetProfileId")

class CycleProfileAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val direction = parameters[cycleDirectionKey] ?: 1
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        WidgetProfileRepository(context, appWidgetId).cycleProfile(direction)
        BridgeCalWidget().update(context, glanceId)
    }
}

class JumpToProfileAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val profileId = parameters[targetProfileIdKey] ?: return
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        WidgetProfileRepository(context, appWidgetId).setActiveProfile(profileId)
        BridgeCalWidget().update(context, glanceId)
    }
}
