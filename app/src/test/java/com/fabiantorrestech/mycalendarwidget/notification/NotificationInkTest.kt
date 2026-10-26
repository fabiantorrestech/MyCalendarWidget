package com.fabiantorrestech.mycalendarwidget.notification

import com.fabiantorrestech.mycalendarwidget.R
import com.fabiantorrestech.mycalendarwidget.data.DisplayMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationInkTest {

    @Test
    fun colorKeepsTheLayoutsOwnAccentColours() {
        assertNull(NotificationInk.forMode(DisplayMode.COLOR, pinned = false))
        assertNull(NotificationInk.forMode(DisplayMode.COLOR, pinned = true))
    }

    @Test
    fun grayscaleUsesGrayContainersInTheShade() {
        val ink = NotificationInk.forMode(DisplayMode.GRAYSCALE, pinned = false)!!
        assertEquals(R.drawable.notification_pill_gray, ink.todayPill)
        assertEquals(R.color.notification_on_gray_container, ink.todayText)
        assertEquals(R.drawable.notification_circle_gray, ink.addCircle)
        assertEquals(R.drawable.notification_circle_gray, ink.refreshCircle)
    }

    @Test
    fun grayscalePinnedStepsUpToTheStrongGray() {
        // The pinned card is itself the gray container, so a same-gray pill would vanish.
        val ink = NotificationInk.forMode(DisplayMode.GRAYSCALE, pinned = true)!!
        assertEquals(R.drawable.notification_pill_gray_strong, ink.todayPill)
        assertEquals(R.color.notification_on_gray_strong, ink.todayText)
        assertEquals(R.drawable.notification_circle_gray_strong, ink.addCircle)
        assertEquals(R.color.notification_on_gray_strong, ink.addTint)
    }

    @Test
    fun einkUsesTheLightGrayEvenWhenPinned() {
        // The e-ink card is paper white, so the ordinary gray container already stands out.
        val ink = NotificationInk.forMode(DisplayMode.EINK, pinned = true)!!
        assertEquals(R.drawable.notification_pill_gray, ink.todayPill)
        assertEquals(R.drawable.notification_circle_gray, ink.addCircle)
        assertEquals(R.color.notification_on_gray_container, ink.refreshTint)
    }
}
