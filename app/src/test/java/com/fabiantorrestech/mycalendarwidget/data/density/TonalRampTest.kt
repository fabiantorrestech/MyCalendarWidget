package com.fabiantorrestech.mycalendarwidget.data.density

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

private const val ACCENT = 0xFF7F77DD.toInt()
private const val LIGHT_BG = 0xFFFFFBFE.toInt()
private const val DARK_BG = 0xFF1C1B1F.toInt()

// Chosen so |lum(accent) - lum(WHITE_BG)| is just over TONE_MIN_LUMINANCE_DELTA (~0.183),
// giving a well-defined (positive) compression cap rather than the degenerate case where
// even the accent itself is already closer to the ground than the minimum delta allows.
private const val PASTEL_ACCENT = 0xFFCFC8FF.toInt()
private const val WHITE_BG = 0xFFFFFFFF.toInt()

/**
 * [TonalRamp] is pure Kotlin (G2): no Android imports, so these run as plain JUnit4
 * under `app/src/test/`.
 */
class TonalRampTest {

    @Test
    fun toneZeroIsTheAccentItself() {
        assertEquals(ACCENT, TonalRamp.ramp(ACCENT, LIGHT_BG)[0])
        assertEquals(ACCENT, TonalRamp.ramp(ACCENT, DARK_BG)[0])
    }

    @Test
    fun luminanceMovesStrictlyMonotonicallyTowardTheBackground() {
        val ramp = TonalRamp.ramp(ACCENT, LIGHT_BG)
        val backgroundLum = ColorMath.luminance(LIGHT_BG)
        val distances = ramp.map { abs(ColorMath.luminance(it) - backgroundLum) }
        for (i in 0 until distances.size - 1) {
            assertTrue("distances were $distances", distances[i] > distances[i + 1])
        }
    }

    @Test
    fun accentOnLightIsNotCompressed() {
        val ramp = TonalRamp.ramp(ACCENT, LIGHT_BG)
        val uncompressedLast = ColorMath.lerp(ACCENT, LIGHT_BG, DensityConstants.TONE_RATIOS.last())
        assertEquals(uncompressedLast, ramp.last())
    }

    @Test
    fun accentOnDarkIsCompressedBelowTheLastRatio() {
        val ramp = TonalRamp.ramp(ACCENT, DARK_BG)
        val uncompressedLast = ColorMath.lerp(ACCENT, DARK_BG, DensityConstants.TONE_RATIOS.last())
        assertTrue(ramp.last() != uncompressedLast)

        val accentLum = ColorMath.luminance(ACCENT)
        val bgLum = ColorMath.luminance(DARK_BG)
        val lastLum = ColorMath.luminance(ramp.last())
        val impliedRatio = (lastLum - accentLum) / (bgLum - accentLum)
        assertTrue("implied ratio was $impliedRatio", impliedRatio < 0.60f)
    }

    @Test
    fun pastelAccentKeepsMinimumLuminanceDeltaFromTheGround() {
        val ramp = TonalRamp.ramp(PASTEL_ACCENT, WHITE_BG)
        val delta = abs(ColorMath.luminance(ramp.last()) - ColorMath.luminance(WHITE_BG))
        assertTrue(
            "delta was $delta",
            delta >= DensityConstants.TONE_MIN_LUMINANCE_DELTA - 0.01f
        )
    }

    @Test
    fun bucketReturnsTheAssignmentWhenPresent() {
        assertEquals(3, TonalRamp.bucket(42L, mapOf(42L to 3), listOf(42L)))
    }

    @Test
    fun bucketFallsBackToRankWhenUnassigned() {
        val ids = listOf(10L, 20L, 30L)
        assertEquals(0, TonalRamp.bucket(10L, emptyMap(), ids))
        assertEquals(1, TonalRamp.bucket(20L, emptyMap(), ids))
        assertEquals(2, TonalRamp.bucket(30L, emptyMap(), ids))
    }

    @Test
    fun bucketFallsBackToZeroWhenAbsentFromTheList() {
        assertEquals(0, TonalRamp.bucket(99L, emptyMap(), listOf(10L, 20L)))
    }

    @Test
    fun bucketWrapsAtFive() {
        val ids = (1L..7L).toList()
        assertEquals(0, TonalRamp.bucket(1L, emptyMap(), ids))
        assertEquals(4, TonalRamp.bucket(5L, emptyMap(), ids))
        assertEquals(0, TonalRamp.bucket(6L, emptyMap(), ids))
        assertEquals(1, TonalRamp.bucket(7L, emptyMap(), ids))
    }

    @Test
    fun ranksStayStableWhenAHigherIdIsAdded() {
        val before = listOf(10L, 20L)
        val rank10Before = TonalRamp.bucket(10L, emptyMap(), before)
        val rank20Before = TonalRamp.bucket(20L, emptyMap(), before)

        val after = listOf(10L, 20L, 30L)
        assertEquals(rank10Before, TonalRamp.bucket(10L, emptyMap(), after))
        assertEquals(rank20Before, TonalRamp.bucket(20L, emptyMap(), after))
    }
}
