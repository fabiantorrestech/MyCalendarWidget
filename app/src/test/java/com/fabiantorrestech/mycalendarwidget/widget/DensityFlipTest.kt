package com.fabiantorrestech.mycalendarwidget.widget

import com.fabiantorrestech.mycalendarwidget.R
import com.fabiantorrestech.mycalendarwidget.data.DisplayMode
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfig
import com.fabiantorrestech.mycalendarwidget.data.WidgetStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DensityFlipTest {

    private val flipping = WidgetConfig(widgetStyle = WidgetStyle.DENSITY, densityTimedFlip = true)

    @Test
    fun flipsOnlyForDensityWithTheOptionOnAndThePeekClosed() {
        assertTrue(DensityFlip.flips(flipping, peekOpen = false))
        assertFalse(DensityFlip.flips(flipping, peekOpen = true))
        assertFalse(DensityFlip.flips(flipping.copy(densityTimedFlip = false), peekOpen = false))
        assertFalse(DensityFlip.flips(flipping.copy(widgetStyle = WidgetStyle.AGENDA), peekOpen = false))
    }

    @Test
    fun intervalIsTheSettingInMillisecondsWithinItsRange() {
        assertEquals(30_000, DensityFlip.intervalMillis(flipping.copy(densityTimedFlipSeconds = 30)))
        assertEquals(5_000, DensityFlip.intervalMillis(flipping.copy(densityTimedFlipSeconds = 1)))
        assertEquals(120_000, DensityFlip.intervalMillis(flipping.copy(densityTimedFlipSeconds = 999)))
    }

    @Test
    fun einkFlipsWithoutAFade() {
        assertEquals(R.layout.density_flipper_still, DensityFlip.layoutFor(DisplayMode.EINK))
        assertEquals(R.layout.density_flipper, DensityFlip.layoutFor(DisplayMode.GRAYSCALE))
        assertEquals(R.layout.density_flipper, DensityFlip.layoutFor(DisplayMode.COLOR))
    }

    @Test
    fun densityReadsEventTextOnlyWhileThePeekIsOpenOrFlipping() {
        val closed = flipping.copy(densityTimedFlip = false)
        assertFalse(DensityFlip.loadsEvents(closed, peekOpen = false))
        assertTrue(DensityFlip.loadsEvents(closed, peekOpen = true))
        assertTrue(DensityFlip.loadsEvents(flipping, peekOpen = false))
        assertTrue(DensityFlip.loadsEvents(closed.copy(widgetStyle = WidgetStyle.AGENDA), peekOpen = false))
    }
}
