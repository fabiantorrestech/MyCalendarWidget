package com.fabiantorrestech.mycalendarwidget.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState

/**
 * Redraws every BridgeCal widget after the phone's grayscale switch flips, so an Auto
 * display mode re-reads it. A bare `update()` may land in a Glance session that is still
 * composed, where nothing the content reads has changed; bumping [EPOCH] in the
 * widget's Glance state is a change the content does read (see `BridgeCalWidget`).
 */
object DisplayModeRefresh {

    val EPOCH: Preferences.Key<Int> = intPreferencesKey("display_mode_epoch")

    suspend fun redrawAll(context: Context) {
        GlanceAppWidgetManager(context).getGlanceIds(BridgeCalWidget::class.java).forEach { id ->
            updateAppWidgetState(context, id) { it[EPOCH] = (it[EPOCH] ?: 0) + 1 }
            BridgeCalWidget().update(context, id)
        }
    }
}
