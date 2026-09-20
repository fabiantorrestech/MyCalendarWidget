package com.fabiantorrestech.mycalendarwidget.widget.peek

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.state.updateAppWidgetState

/**
 * Whether the density widget's peek sheet is open, stored per widget in Glance's own
 * default `PreferencesGlanceStateDefinition` — [com.fabiantorrestech.mycalendarwidget.widget.BridgeCalWidget]
 * deliberately does not override `stateDefinition`, so this rides along with whatever
 * Glance already persists for the widget and survives a process death.
 *
 * The stored value is an **expiry instant**, never a boolean. A boolean would need
 * somebody to come back and clear it; an instant closes itself. A widget left open in a
 * launcher folder the user never reopens therefore lapses on its own after [TTL_MS],
 * and a stale `peek_open_until` written before a reboot cannot resurrect the sheet.
 * The scheduled refreshes close it explicitly on top of that (see `WidgetSyncReceiver`
 * and `DateChangeReceiver`) — the TTL is the safety net, not the primary close path.
 *
 * [isOpen] is pure so it can be unit-tested without a device: the caller supplies both
 * the `Preferences` snapshot and `nowMillis`.
 */
object PeekState {

    val OPEN_UNTIL: Preferences.Key<Long> = longPreferencesKey("peek_open_until")

    /** How long an untouched peek stays open. */
    const val TTL_MS = 5 * 60_000L

    /**
     * Strictly greater than: at the exact expiry instant the peek is already closed, so
     * "open until T" and "closed from T" describe the same moment rather than leaving a
     * one-millisecond window where both are true.
     */
    fun isOpen(prefs: Preferences?, nowMillis: Long): Boolean =
        (prefs?.get(OPEN_UNTIL) ?: 0L) > nowMillis

    suspend fun open(
        context: Context,
        glanceId: GlanceId,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        updateAppWidgetState(context, glanceId) { it[OPEN_UNTIL] = nowMillis + TTL_MS }
    }

    /**
     * Removes the key rather than writing a past instant: an absent key is the same
     * "closed" that a widget which has never been peeked at reports, so there is only
     * one closed representation to reason about.
     */
    suspend fun close(context: Context, glanceId: GlanceId) {
        updateAppWidgetState(context, glanceId) { it.remove(OPEN_UNTIL) }
    }
}
