package com.fabiantorrestech.mycalendarwidget.widget.density

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Pure integer geometry for the strip bitmap — no [android.graphics.Bitmap] and no
 * [android.graphics.Rect]: under `testDebugUnitTest` the stubbed android.jar builds a
 * `Rect` whose fields silently stay 0, so [DensityCanvas] does its arithmetic in
 * [IntRect] and converts only at draw time.
 *
 * The caret overhang and halo are 0 at every Task 7 call site; these cases pin the
 * non-zero behaviour the later "caret breaks out of the track" task turns on.
 */
class DensityCanvasGeometryTest {

    private fun spec(
        nowFraction: Float?,
        overhangPx: Int = 8,
        haloPx: Int = 4
    ) = StripSpec(
        widthPx = 600,
        trackHeightPx = 56,
        overhangPx = overhangPx,
        haloPx = haloPx,
        content = StripContent.Shape(emptyList(), 0),
        nowFraction = nowFraction,
        freeColor = 0,
        nowColor = 0,
        nowMarkerWidthPx = 8,
        backgroundColor = 0
    )

    @Test
    fun heightDerivesFromTrackAndOverhang() {
        assertEquals(72, spec(null).heightPx)
        assertEquals(56, spec(null, overhangPx = 0).heightPx)
    }

    @Test
    fun trackSitsBetweenTheOverhangs() {
        assertEquals(IntRect(0, 8, 600, 64), DensityCanvas.trackRect(spec(null)))
    }

    @Test
    fun trackFillsTheBitmapWhenThereIsNoOverhang() {
        assertEquals(IntRect(0, 0, 600, 56), DensityCanvas.trackRect(spec(null, overhangPx = 0)))
    }

    @Test
    fun noMarkerRectsWithoutANowFraction() {
        assertNull(DensityCanvas.markerRects(spec(null)))
    }

    @Test
    fun markerAtWindowStartKeepsItsHaloOnScreen() {
        val (halo, caret) = DensityCanvas.markerRects(spec(0f))!!
        assertEquals(0, halo.left)
        assertEquals(4, caret.left)
        assertEquals(12, caret.right)
        assertEquals(16, halo.right)
    }

    @Test
    fun markerAtWindowEndKeepsItsHaloOnScreen() {
        val (halo, caret) = DensityCanvas.markerRects(spec(1f))!!
        assertEquals(600, halo.right)
        assertEquals(596, caret.right)
        assertEquals(588, caret.left)
        assertEquals(584, halo.left)
    }

    @Test
    fun markerAtMidWindowIsCentredWithinAPixel() {
        val (_, caret) = DensityCanvas.markerRects(spec(0.5f))!!
        val centre = (caret.left + caret.right) / 2f
        assertTrue("caret centre was $centre", abs(centre - 300f) <= 1f)
        assertEquals(8, caret.right - caret.left)
    }

    @Test
    fun caretSpansTheWholeBitmapHeight() {
        val (halo, caret) = DensityCanvas.markerRects(spec(0.5f))!!
        assertEquals(0, caret.top)
        assertEquals(72, caret.bottom)
        assertEquals(0, halo.top)
        assertEquals(72, halo.bottom)
    }

    @Test
    fun caretSpansOnlyTheTrackWhenThereIsNoOverhang() {
        val (halo, caret) = DensityCanvas.markerRects(spec(0.5f, overhangPx = 0, haloPx = 0))!!
        assertEquals(0, caret.top)
        assertEquals(56, caret.bottom)
        // With no halo the two rects coincide, so nothing is painted twice.
        assertEquals(caret, halo)
    }

    @Test
    fun markerFractionIsClampedToTheWindow() {
        val (_, low) = DensityCanvas.markerRects(spec(-0.5f))!!
        val (_, high) = DensityCanvas.markerRects(spec(1.5f))!!
        assertEquals(4, low.left)
        assertEquals(596, high.right)
    }
}
