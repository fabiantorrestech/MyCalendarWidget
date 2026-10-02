package com.fabiantorrestech.mycalendarwidget.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfigRepository
import com.fabiantorrestech.mycalendarwidget.data.WidgetProfileRepository
import com.fabiantorrestech.mycalendarwidget.notification.DensityNotifier
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

/**
 * Forces a re-read of the calendar. The widget composes from the profile repository's
 * active config (not the legacy per-widget store), and its data loads are keyed on that
 * config, so the nonce has to change in the active profile for anything to reload; the
 * legacy store is bumped too so an export still carries the same value.
 */
class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        val legacy = WidgetConfigRepository(context, appWidgetId)
        val legacyCurrent = legacy.configFlow.first()
        legacy.updateConfig(legacyCurrent.copy(refreshNonce = legacyCurrent.refreshNonce + 1))

        val profiles = WidgetProfileRepository(context, appWidgetId)
        val activeId = profiles.ensureActiveProfileId(legacyCurrent)
        val active = profiles.activeConfigFlow.first()
        profiles.updateProfileConfig(activeId, active.copy(refreshNonce = active.refreshNonce + 1))
        BridgeCalWidget().update(context, glanceId)
        DensityNotifier.refreshIfFollowing(context, appWidgetId)
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
        DensityNotifier.refreshIfFollowing(context, appWidgetId)
    }
}

class JumpToProfileAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val profileId = parameters[targetProfileIdKey] ?: return
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        WidgetProfileRepository(context, appWidgetId).setActiveProfile(profileId)
        BridgeCalWidget().update(context, glanceId)
        DensityNotifier.refreshIfFollowing(context, appWidgetId)
    }
}
