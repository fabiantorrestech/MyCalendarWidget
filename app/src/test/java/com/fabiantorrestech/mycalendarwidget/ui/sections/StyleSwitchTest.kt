package com.fabiantorrestech.mycalendarwidget.ui.sections

import com.fabiantorrestech.mycalendarwidget.data.DensityStripMode
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfig
import com.fabiantorrestech.mycalendarwidget.data.WidgetStyle
import org.junit.Assert.assertEquals
import org.junit.Test

/** Switching styles: entering Density picks Tonal once; nothing else is touched. */
class StyleSwitchTest {

    @Test
    fun enteringDensityDefaultsToTonal() {
        val from = WidgetConfig(widgetStyle = WidgetStyle.AGENDA, densityStripMode = DensityStripMode.SHAPE)
        val to = StyleSwitch.applyStyle(from, WidgetStyle.DENSITY)
        assertEquals(WidgetStyle.DENSITY, to.widgetStyle)
        assertEquals(DensityStripMode.TONAL, to.densityStripMode)
    }

    @Test
    fun stayingOnDensityKeepsTheChosenStripMode() {
        val from = WidgetConfig(widgetStyle = WidgetStyle.DENSITY, densityStripMode = DensityStripMode.DETAIL)
        assertEquals(from, StyleSwitch.applyStyle(from, WidgetStyle.DENSITY))
    }

    @Test
    fun leavingDensityKeepsTheStripMode() {
        val from = WidgetConfig(widgetStyle = WidgetStyle.DENSITY, densityStripMode = DensityStripMode.DETAIL)
        val to = StyleSwitch.applyStyle(from, WidgetStyle.GCAL)
        assertEquals(WidgetStyle.GCAL, to.widgetStyle)
        assertEquals(DensityStripMode.DETAIL, to.densityStripMode)
    }

    @Test
    fun agendaSwitchOnlyChangesTheStyle() {
        val from = WidgetConfig(widgetStyle = WidgetStyle.AGENDA, maxTitleLines = 3)
        assertEquals(from.copy(widgetStyle = WidgetStyle.GCAL_LEFT), StyleSwitch.applyStyle(from, WidgetStyle.GCAL_LEFT))
    }
}
