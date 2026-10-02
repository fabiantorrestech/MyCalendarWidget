package com.fabiantorrestech.mycalendarwidget.notification

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationPagingTest {

    private val now = 1_000_000_000L

    @Test
    fun `a recent tap keeps its offset`() {
        assertEquals(3, NotificationPaging.effectiveOffset(3, touchedAtMillis = now - 60_000, nowMillis = now))
    }

    @Test
    fun `exactly two minutes still counts`() {
        assertEquals(3, NotificationPaging.effectiveOffset(3, touchedAtMillis = now - 120_000, nowMillis = now))
    }

    @Test
    fun `after two idle minutes it snaps back to the start`() {
        assertEquals(0, NotificationPaging.effectiveOffset(3, touchedAtMillis = now - 120_001, nowMillis = now))
    }

    @Test
    fun `a negative stored offset reads as the start`() {
        assertEquals(0, NotificationPaging.effectiveOffset(-2, touchedAtMillis = now, nowMillis = now))
    }

    @Test
    fun `clamp keeps the offset on an existing page`() {
        assertEquals(2, NotificationPaging.clamp(5, pageCount = 3))
        assertEquals(1, NotificationPaging.clamp(1, pageCount = 3))
        assertEquals(0, NotificationPaging.clamp(-1, pageCount = 3))
    }

    @Test
    fun `clamp with no pages is the start`() {
        assertEquals(0, NotificationPaging.clamp(4, pageCount = 0))
    }
}
