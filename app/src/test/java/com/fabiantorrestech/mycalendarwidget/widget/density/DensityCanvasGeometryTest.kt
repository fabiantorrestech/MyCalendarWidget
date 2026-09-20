package com.fabiantorrestech.mycalendarwidget.widget.density

import com.fabiantorrestech.mycalendarwidget.data.density.LaneRect
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
        haloPx: Int = 4,
        cutAtStart: Boolean = false,
        cutAtEnd: Boolean = false,
        allDayBandColor: Int? = null
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
        backgroundColor = 0,
        pxPerDp = 4f,
        cutAtStart = cutAtStart,
        cutAtEnd = cutAtEnd,
        allDayBandColor = allDayBandColor
    )

    // --- all-day band: the top overhang rows, full width, one pixel clear of the track ---

    @Test
    fun noAllDayBandWithoutAColor() {
        assertNull(DensityCanvas.allDayBandRect(spec(null)))
    }

    @Test
    fun allDayBandFillsTheTopOverhangLessOnePixel() {
        // overhang 8px: the band is rows 0..6, leaving row 7 clear so it never merges
        // with a busy block touching the top of the track.
        assertEquals(IntRect(0, 0, 600, 7), DensityCanvas.allDayBandRect(spec(null, allDayBandColor = 1)))
    }

    @Test
    fun allDayBandUsesTheWholeOverhangWhenItIsTooThinToSpare() {
        assertEquals(IntRect(0, 0, 600, 2), DensityCanvas.allDayBandRect(spec(null, overhangPx = 2, allDayBandColor = 1)))
        assertNull(DensityCanvas.allDayBandRect(spec(null, overhangPx = 0, allDayBandColor = 1)))
    }

    @Test
    fun loadBarBandsSitAboveTheBarsOnlyForAllDayDays() {
        val s = loadBarsSpec(loads = listOf(0.5f, 0.2f, 0f), widthPx = 100, heightPx = 9, gutterPx = 5, bandPx = 2, bandGapPx = 1, allDay = listOf(true, false, true))
        val tracks = DensityCanvas.loadBarTrackRects(s)
        assertTrue(tracks.all { it.top == 3 && it.bottom == 9 })
        val bands = DensityCanvas.loadBarBandRects(s)
        assertEquals(2, bands.size)
        assertEquals(IntRect(0, 0, 30, 2), bands[0])
        assertEquals(IntRect(70, 0, 100, 2), bands[1])
    }

    @Test
    fun loadBarsWithoutBandsKeepTheirOldGeometry() {
        val s = loadBarsSpec(loads = listOf(1f), widthPx = 100, heightPx = 6, gutterPx = 0)
        assertEquals(IntRect(0, 0, 100, 6), DensityCanvas.loadBarTrackRects(s).single())
        assertTrue(DensityCanvas.loadBarBandRects(s).isEmpty())
    }

    // --- midnight cut chevrons (5dp tall -> 21 odd rows, 1dp stroke, 2dp inside the edge; 4 px/dp) ---

    @Test
    fun noChevronWithoutACut() {
        assertTrue(DensityCanvas.cutChevronRects(spec(null), atEnd = true).isEmpty())
        assertTrue(DensityCanvas.cutChevronRects(spec(null), atEnd = false).isEmpty())
    }

    @Test
    fun endChevronSitsInsideTheRightEdge() {
        val rects = DensityCanvas.cutChevronRects(spec(null, cutAtEnd = true), atEnd = true)
        assertTrue(rects.isNotEmpty())
        // The tip touches the inset line (600 - 8); nothing crosses it.
        assertEquals(592, rects.maxOf { it.right })
        assertTrue(rects.all { it.right <= 592 && it.width == 4 })
        // Top and bottom rows sit furthest from the edge: half the height back.
        assertEquals(592 - 4 - 10, rects.first().left)
        assertEquals(592 - 4 - 10, rects.last().left)
    }

    @Test
    fun startChevronMirrorsTheEndOne() {
        val end = DensityCanvas.cutChevronRects(spec(null, cutAtEnd = true), atEnd = true)
        val start = DensityCanvas.cutChevronRects(spec(null, cutAtStart = true), atEnd = false)
        assertEquals(end.size, start.size)
        end.zip(start).forEach { (e, s) ->
            assertEquals(600 - e.right, s.left)
            assertEquals(e.top, s.top)
        }
    }

    @Test
    fun chevronStaysWithinTheTrackHeight() {
        val track = DensityCanvas.trackRect(spec(null))
        val rects = DensityCanvas.cutChevronRects(spec(null, cutAtEnd = true), atEnd = true)
        assertEquals(21, rects.size)
        assertTrue(rects.all { it.top >= track.top && it.bottom <= track.bottom && it.height == 1 })
        // Centred on the track: as much free track above as below (to within a pixel).
        assertTrue(abs((rects.first().top - track.top) - (track.bottom - rects.last().bottom)) <= 1)
    }

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

    // --- Look-ahead bars geometry ----------------------------------------------------

    private fun loadBarsSpec(
        loads: List<Float>,
        widthPx: Int = 100,
        heightPx: Int = 6,
        gutterPx: Int = 10,
        bandPx: Int = 0,
        bandGapPx: Int = 0,
        allDay: List<Boolean> = emptyList(),
        overflow: List<Boolean> = emptyList(),
        pxPerDp: Float = 4f
    ) = LoadBarsSpec(
        widthPx = widthPx,
        heightPx = heightPx,
        loads = loads,
        gutterPx = gutterPx,
        fillColor = 0,
        trackColor = 0,
        bandPx = bandPx,
        bandGapPx = bandGapPx,
        allDay = allDay,
        overflow = overflow,
        pxPerDp = pxPerDp,
        backgroundColor = 0
    )

    // --- lane gaps: the break between stacked lanes is background, same as the outline ---

    private fun twoLanes(): StripSpec {
        val rects = listOf(
            LaneRect(0L, 100L, lane = 0, depth = 2, calendarId = 1L, colorInt = 1, isEventEnd = true),
            LaneRect(0L, 100L, lane = 1, depth = 2, calendarId = 2L, colorInt = 2, isEventEnd = true),
            LaneRect(100L, 200L, lane = 0, depth = 1, calendarId = 1L, colorInt = 1, isEventEnd = true)
        )
        return spec(null, overhangPx = 0).copy(
            content = StripContent.Lanes(rects, windowStartMillis = 0L, windowEndMillis = 200L, laneOutlineColor = 0)
        )
    }

    @Test
    fun laneGapSitsBetweenTheTwoLanesOverTheOverlapOnly() {
        val s = twoLanes()
        val gaps = DensityCanvas.laneGapRects(s, DensityCanvas.trackRect(s), s.content as StripContent.Lanes)
        // Track 56px, gap 8px (2dp at 4px/dp): lanes are 24px each, the gap rows 24..32.
        assertEquals(listOf(IntRect(0, 24, 300, 32)), gaps)
    }

    @Test
    fun noLaneGapWhereOnlyOneLaneRuns() {
        val s = spec(null, overhangPx = 0).copy(
            content = StripContent.Lanes(
                listOf(LaneRect(0L, 200L, lane = 0, depth = 1, calendarId = 1L, colorInt = 1, isEventEnd = true)),
                windowStartMillis = 0L, windowEndMillis = 200L, laneOutlineColor = null
            )
        )
        assertTrue(DensityCanvas.laneGapRects(s, DensityCanvas.trackRect(s), s.content as StripContent.Lanes).isEmpty())
    }

    // --- overflow: a + centred on a full bar past the baseline ---

    @Test
    fun overflowPlusOnlyOnFlaggedColumns() {
        val s = loadBarsSpec(loads = listOf(1f, 1f, 0.5f), widthPx = 100, heightPx = 24, gutterPx = 5, overflow = listOf(true, false, true))
        val tracks = DensityCanvas.loadBarTrackRects(s)
        val rects = DensityCanvas.loadBarOverflowPlusRects(s)
        assertEquals(4, rects.size)
        assertTrue(rects.all { r -> (r.left >= tracks[0].left && r.right <= tracks[0].right) || (r.left >= tracks[2].left && r.right <= tracks[2].right) })
        assertTrue(DensityCanvas.loadBarOverflowPlusRects(loadBarsSpec(loads = listOf(1f))).isEmpty())
    }

    @Test
    fun overflowPlusIsCentredInTheColumn() {
        // One 101px column, 24px bar, 4 px/dp: both arms are centred on (50, 12) within a pixel.
        val s = loadBarsSpec(loads = listOf(1f), widthPx = 101, heightPx = 24, gutterPx = 0, overflow = listOf(true))
        val (horizontal, vertical) = DensityCanvas.loadBarOverflowPlusRects(s)
        assertTrue(abs((horizontal.left + horizontal.right) / 2 - 50) <= 1)
        assertTrue(abs((horizontal.top + horizontal.bottom) / 2 - 12) <= 1)
        assertTrue(abs((vertical.left + vertical.right) / 2 - 50) <= 1)
        assertTrue(abs((vertical.top + vertical.bottom) / 2 - 12) <= 1)
        assertEquals(4, horizontal.height)
        assertEquals(4, vertical.width)
        assertEquals(horizontal.width, vertical.height)
    }

    @Test
    fun overflowPlusFitsInsideTheBar() {
        val s = loadBarsSpec(loads = listOf(1f), widthPx = 100, heightPx = 9, gutterPx = 0, bandPx = 2, bandGapPx = 1, overflow = listOf(true))
        val track = DensityCanvas.loadBarTrackRects(s).single()
        val rects = DensityCanvas.loadBarOverflowPlusRects(s)
        assertEquals(2, rects.size)
        assertTrue(rects.all { it.top >= track.top && it.bottom <= track.bottom && it.left >= track.left && it.right <= track.right })
    }

    @Test
    fun loadBarTrackRectsAreEqualWidthColumnsSeparatedByTheGutter() {
        val rects = DensityCanvas.loadBarTrackRects(loadBarsSpec(listOf(0f, 0.5f, 1f)))
        assertEquals(3, rects.size)
        // colW = (100 - 10*2) / 3 = 26 (integer division).
        assertEquals(IntRect(0, 0, 26, 6), rects[0])
        assertEquals(IntRect(36, 0, 62, 6), rects[1])
        assertEquals(IntRect(72, 0, 98, 6), rects[2])
    }

    @Test
    fun loadBarFillRectsStartAtTheColumnsLeftEdgeAndScaleWithLoad() {
        val spec = loadBarsSpec(listOf(0f, 0.5f, 1f))
        val tracks = DensityCanvas.loadBarTrackRects(spec)
        val fills = DensityCanvas.loadBarFillRects(spec)
        assertEquals(3, fills.size)
        assertEquals(0, fills[0].width)
        assertEquals(13, fills[1].width) // round(0.5 * 26)
        assertEquals(26, fills[2].width)
        fills.forEachIndexed { i, fill ->
            assertEquals(tracks[i].left, fill.left)
            assertEquals(tracks[i].top, fill.top)
            assertEquals(tracks[i].bottom, fill.bottom)
        }
    }

    @Test
    fun loadBarRectsAreEmptyWithNoLoadsOrNoWidth() {
        assertTrue(DensityCanvas.loadBarTrackRects(loadBarsSpec(emptyList())).isEmpty())
        assertTrue(DensityCanvas.loadBarFillRects(loadBarsSpec(emptyList())).isEmpty())
        assertTrue(
            DensityCanvas.loadBarTrackRects(loadBarsSpec(listOf(0.5f), widthPx = 0)).isEmpty()
        )
    }

    @Test
    fun loadBarFillClampsLoadOutsideZeroToOne() {
        val spec = loadBarsSpec(listOf(-1f, 2f))
        val fills = DensityCanvas.loadBarFillRects(spec)
        assertEquals(0, fills[0].width)
        val tracks = DensityCanvas.loadBarTrackRects(spec)
        assertEquals(tracks[1].width, fills[1].width)
    }
}
