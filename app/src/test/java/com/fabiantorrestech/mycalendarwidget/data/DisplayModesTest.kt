package com.fabiantorrestech.mycalendarwidget.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayModesTest {

    @Test
    fun autoFollowsTheSystemGrayscaleSwitch() {
        assertEquals(DisplayMode.GRAYSCALE, DisplayModes.resolve(DisplayMode.AUTO, systemGrayscale = true))
        assertEquals(DisplayMode.COLOR, DisplayModes.resolve(DisplayMode.AUTO, systemGrayscale = false))
    }

    @Test
    fun aManualPickIgnoresTheSystemSwitch() {
        assertEquals(DisplayMode.EINK, DisplayModes.resolve(DisplayMode.EINK, systemGrayscale = true))
        assertEquals(DisplayMode.COLOR, DisplayModes.resolve(DisplayMode.COLOR, systemGrayscale = true))
        assertEquals(DisplayMode.GRAYSCALE, DisplayModes.resolve(DisplayMode.GRAYSCALE, systemGrayscale = false))
    }
}
