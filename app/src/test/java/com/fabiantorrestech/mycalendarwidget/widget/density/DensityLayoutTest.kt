package com.fabiantorrestech.mycalendarwidget.widget.density

import com.fabiantorrestech.mycalendarwidget.widget.density.DensityLayout.ChromePlacement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The size-to-layout decisions the widget and the settings preview share. Sizes are the
 * dp values `LocalSize` would report; the breakpoints themselves are the constants on
 * [DensityLayout], so these tests pin the comparisons, not the numbers.
 */
class DensityLayoutTest {

    @Test
    fun isCompact_trueJustBelowBreakpoint() {
        assertTrue(DensityLayout.isCompact(DensityLayout.COMPACT_HEIGHT_DP - 0.1f))
    }

    @Test
    fun isCompact_falseAtBreakpoint() {
        assertFalse(DensityLayout.isCompact(DensityLayout.COMPACT_HEIGHT_DP))
    }

    @Test
    fun chromePlacement_wideAndTallIsInline() {
        assertEquals(ChromePlacement.INLINE, DensityLayout.chromePlacement(300f, 200f, showQuickAdd = false))
    }

    @Test
    fun chromePlacement_wideAndShortStaysInline() {
        assertEquals(ChromePlacement.INLINE, DensityLayout.chromePlacement(300f, 100f, showQuickAdd = false))
    }

    @Test
    fun chromePlacement_narrowAndTallIsBottomRow() {
        assertEquals(ChromePlacement.BOTTOM_ROW, DensityLayout.chromePlacement(299f, 200f, showQuickAdd = false))
    }

    @Test
    fun chromePlacement_narrowAndShortIsCompactSingle() {
        assertEquals(ChromePlacement.COMPACT_SINGLE, DensityLayout.chromePlacement(299f, 159f, showQuickAdd = false))
    }

    @Test
    fun chromePlacement_quickAddRaisesNarrowBreakpoint() {
        assertEquals(ChromePlacement.INLINE, DensityLayout.chromePlacement(330f, 200f, showQuickAdd = false))
        assertEquals(ChromePlacement.BOTTOM_ROW, DensityLayout.chromePlacement(330f, 200f, showQuickAdd = true))
    }

    @Test
    fun chromePlacement_quickAddBreakpointIsInclusive() {
        val breakpoint = DensityLayout.NARROW_WIDTH_DP + DensityLayout.QUICK_ADD_EXTRA_WIDTH_DP
        assertEquals(ChromePlacement.INLINE, DensityLayout.chromePlacement(breakpoint, 200f, showQuickAdd = true))
    }

    @Test
    fun chromePlacement_providerMinSizeIsCompactSingle() {
        // 180x40dp is what Glance falls back to when a launcher never reports a size.
        assertEquals(ChromePlacement.COMPACT_SINGLE, DensityLayout.chromePlacement(180f, 40f, showQuickAdd = true))
        assertEquals(ChromePlacement.COMPACT_SINGLE, DensityLayout.chromePlacement(180f, 40f, showQuickAdd = false))
    }
}
