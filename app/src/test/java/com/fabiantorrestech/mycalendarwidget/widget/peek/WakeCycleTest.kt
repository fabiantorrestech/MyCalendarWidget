package com.fabiantorrestech.mycalendarwidget.widget.peek

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeCycleTest {

    @Test
    fun everyOtherWakeAlternates() {
        assertFalse(WakeCycle.showsAgenda(wakeCount = 1, every = 2))
        assertTrue(WakeCycle.showsAgenda(wakeCount = 2, every = 2))
        assertFalse(WakeCycle.showsAgenda(wakeCount = 3, every = 2))
        assertTrue(WakeCycle.showsAgenda(wakeCount = 4, every = 2))
    }

    @Test
    fun everyThirdWakeShowsTheAgendaOnceInThree() {
        assertFalse(WakeCycle.showsAgenda(wakeCount = 2, every = 3))
        assertTrue(WakeCycle.showsAgenda(wakeCount = 3, every = 3))
    }

    @Test
    fun anOutOfRangeEveryNeverShowsTheAgenda() {
        assertFalse(WakeCycle.showsAgenda(wakeCount = 5, every = 1))
        assertFalse(WakeCycle.showsAgenda(wakeCount = 0, every = 0))
    }
}
