package com.fabiantorrestech.mycalendarwidget.data.density

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val BLACK = 0xFF000000.toInt()
private const val WHITE = 0xFFFFFFFF.toInt()
private const val RED = 0xFFFF0000.toInt()
private const val GREEN = 0xFF00FF00.toInt()
private const val BLUE = 0xFF0000FF.toInt()

/**
 * Pure colour arithmetic: the pre-blending that keeps the widget free of alpha
 * compositing (G5) plus the luminance test the event chips already relied on.
 */
class ColorMathTest {

    @Test
    fun lerpAtZeroReturnsTheFirstColor() {
        assertEquals(0xFF123456.toInt(), ColorMath.lerp(0xFF123456.toInt(), WHITE, 0f))
    }

    @Test
    fun lerpAtOneReturnsTheSecondColor() {
        assertEquals(0xFF123456.toInt(), ColorMath.lerp(WHITE, 0xFF123456.toInt(), 1f))
    }

    @Test
    fun lerpBlendsEachChannelIndependently() {
        // Halfway between black and white rounds up to 0x80 on every channel.
        assertEquals(0xFF808080.toInt(), ColorMath.lerp(BLACK, WHITE, 0.5f))
        // Red to blue moves only the red and blue channels.
        assertEquals(0xFF800080.toInt(), ColorMath.lerp(RED, BLUE, 0.5f))
    }

    @Test
    fun lerpForcesAlphaOpaque() {
        val blended = ColorMath.lerp(0x00000000, 0x00FFFFFF, 0f)
        assertEquals(BLACK, blended)
        assertEquals(0xFF, blended ushr 24)
    }

    @Test
    fun lerpClampsTheFraction() {
        assertEquals(BLACK, ColorMath.lerp(BLACK, WHITE, -1f))
        assertEquals(WHITE, ColorMath.lerp(BLACK, WHITE, 2f))
    }

    @Test
    fun luminanceUsesRec601Weights() {
        assertEquals(0f, ColorMath.luminance(BLACK), 0.0001f)
        assertEquals(1f, ColorMath.luminance(WHITE), 0.0001f)
        assertEquals(0.299f, ColorMath.luminance(RED), 0.0001f)
        assertEquals(0.587f, ColorMath.luminance(GREEN), 0.0001f)
        assertEquals(0.114f, ColorMath.luminance(BLUE), 0.0001f)
    }

    @Test
    fun luminanceIgnoresAlpha() {
        assertEquals(ColorMath.luminance(WHITE), ColorMath.luminance(0x00FFFFFF), 0.0001f)
    }

    @Test
    fun isDarkSplitsAtHalfLuminance() {
        assertTrue(ColorMath.isDark(BLACK))
        assertTrue(ColorMath.isDark(BLUE))
        assertFalse(ColorMath.isDark(WHITE))
        assertFalse(ColorMath.isDark(GREEN))
    }

    @Test
    fun isDarkMatchesTheHelperItReplaces() {
        // The two duplicated isDarkColor helpers computed exactly this on 0..1 doubles.
        listOf(BLACK, WHITE, RED, GREEN, BLUE, 0xFF7F77DD.toInt(), 0xFFD85A30.toInt()).forEach { argb ->
            val r = (argb shr 16 and 0xFF) / 255.0
            val g = (argb shr 8 and 0xFF) / 255.0
            val b = (argb and 0xFF) / 255.0
            val expected = 0.299 * r + 0.587 * g + 0.114 * b < 0.5
            assertEquals(expected, ColorMath.isDark(argb))
        }
    }
}
