package com.fabiantorrestech.mycalendarwidget.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.fabiantorrestech.mycalendarwidget.data.DisplayMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplaySchemesTest {

    private val baseLight = lightColorScheme(primary = Color(0xFF6750A4), secondaryContainer = Color(0xFFE8DEF8))
    private val baseDark = darkColorScheme(primary = Color(0xFFD0BCFF), secondaryContainer = Color(0xFF4A4458))

    private fun isNeutral(c: Color): Boolean {
        val argb = c.toArgb()
        return (argb shr 16 and 0xFF) == (argb shr 8 and 0xFF) && (argb shr 8 and 0xFF) == (argb and 0xFF)
    }

    private fun roles(s: ColorScheme) = listOf(
        s.background, s.surface, s.onSurface, s.onSurfaceVariant, s.primary,
        s.primaryContainer, s.secondaryContainer, s.surfaceVariant, s.surfaceContainerHigh
    )

    @Test
    fun grayscaleSchemesAreNeutralInBothThemes() {
        listOf(false to baseLight, true to baseDark).forEach { (dark, base) ->
            val scheme = DisplaySchemes.scheme(DisplayMode.GRAYSCALE, dark, base)
            roles(scheme).forEach { assertTrue("$it is not neutral (dark=$dark)", isNeutral(it)) }
        }
    }

    @Test
    fun einkIsBlackInkOnWhiteByDayAndWhiteOnBlackByNight() {
        val day = DisplaySchemes.scheme(DisplayMode.EINK, false, baseLight)
        assertEquals(Color.White, day.background)
        assertEquals(Color.White, day.secondaryContainer)
        assertEquals(Color.Black, day.onSurface)
        assertEquals(day.onSurface, day.onSurfaceVariant)

        val night = DisplaySchemes.scheme(DisplayMode.EINK, true, baseDark)
        assertEquals(Color.Black, night.background)
        assertEquals(Color.Black, night.secondaryContainer)
        assertEquals(Color.White, night.onSurface)
        assertEquals(night.onSurface, night.onSurfaceVariant)
    }

    @Test
    fun colorReturnsTheBaseSchemeUnchanged() {
        assertSame(baseLight, DisplaySchemes.scheme(DisplayMode.COLOR, false, baseLight))
    }
}
