package com.fabiantorrestech.mycalendarwidget.widget.peek

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.fabiantorrestech.mycalendarwidget.widget.BridgeCalWidget

/** True opens the peek, false closes it. */
val peekOpenKey = ActionParameters.Key<Boolean>("peekOpen")

/**
 * The one action that drives the peek: the strip opens it, the sheet's top bar and every
 * date header close it. It writes [PeekState] and then re-runs `provideGlance` through
 * `update`, which is what makes the new state visible — the callback itself cannot
 * recompose anything.
 *
 * Same four-line shape as the actions in `WidgetActions.kt`, deliberately: a peek toggle
 * is not special enough to invent a second pattern for.
 */
class SetPeekAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val open = parameters[peekOpenKey] ?: false
        if (open) PeekState.open(context, glanceId) else PeekState.close(context, glanceId)
        BridgeCalWidget().update(context, glanceId)
    }
}
