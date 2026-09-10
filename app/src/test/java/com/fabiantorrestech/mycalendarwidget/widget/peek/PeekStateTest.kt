package com.fabiantorrestech.mycalendarwidget.widget.peek

import androidx.datastore.preferences.core.preferencesOf
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [PeekState.isOpen] is the whole of the peek's state machine: an expiry timestamp
 * compared against now, never a boolean. These are the three cases that matter — no
 * stored value, a live value, and the exact instant it lapses.
 */
class PeekStateTest {

    @Test
    fun `closed when no preferences at all`() {
        assertFalse(PeekState.isOpen(null, 1_000L))
    }

    @Test
    fun `closed when the key was never written`() {
        assertFalse(PeekState.isOpen(preferencesOf(), 1_000L))
    }

    @Test
    fun `open while the expiry is still in the future`() {
        val prefs = preferencesOf(PeekState.OPEN_UNTIL to 2_000L)
        assertTrue(PeekState.isOpen(prefs, 1_000L))
    }

    @Test
    fun `closed at the exact expiry instant`() {
        val prefs = preferencesOf(PeekState.OPEN_UNTIL to 1_000L)
        assertFalse(PeekState.isOpen(prefs, 1_000L))
    }

    @Test
    fun `closed once the expiry is past`() {
        val prefs = preferencesOf(PeekState.OPEN_UNTIL to 999L)
        assertFalse(PeekState.isOpen(prefs, 1_000L))
    }

    @Test
    fun `ttl is five minutes`() {
        assertTrue(PeekState.TTL_MS == 5 * 60_000L)
    }
}
