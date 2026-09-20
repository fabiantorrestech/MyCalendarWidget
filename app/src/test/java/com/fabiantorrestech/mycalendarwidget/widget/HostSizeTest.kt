package com.fabiantorrestech.mycalendarwidget.widget

import com.fabiantorrestech.mycalendarwidget.widget.density.DensityLayout
import com.fabiantorrestech.mycalendarwidget.widget.density.DensityLayout.ChromePlacement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [HostSize.reportsSize] must agree with Glance 1.1.1's `AppWidgetUtils.extractAllSizes`:
 * a non-empty OPTION_APPWIDGET_SIZES list is authoritative; otherwise any zero among the
 * four min/max option ints makes Glance fall back to the provider's minimum size.
 */
class HostSizeTest {

    @Test
    fun reportsSize_falseWhenNoSizesListAndAllFourOptionsZero() {
        assertFalse(HostSize.reportsSize(sizesCount = 0, minWidthDp = 0, maxWidthDp = 0, minHeightDp = 0, maxHeightDp = 0))
    }

    @Test
    fun reportsSize_trueWhenSizesListNonEmptyEvenIfOptionsZero() {
        assertTrue(HostSize.reportsSize(sizesCount = 2, minWidthDp = 0, maxWidthDp = 0, minHeightDp = 0, maxHeightDp = 0))
    }

    @Test
    fun reportsSize_trueWhenAllFourOptionsNonZero() {
        assertTrue(HostSize.reportsSize(sizesCount = 0, minWidthDp = 300, maxWidthDp = 340, minHeightDp = 200, maxHeightDp = 260))
    }

    @Test
    fun reportsSize_falseWhenAnyOneOptionIsZero() {
        assertFalse(HostSize.reportsSize(sizesCount = 0, minWidthDp = 0, maxWidthDp = 340, minHeightDp = 200, maxHeightDp = 260))
        assertFalse(HostSize.reportsSize(sizesCount = 0, minWidthDp = 300, maxWidthDp = 0, minHeightDp = 200, maxHeightDp = 260))
        assertFalse(HostSize.reportsSize(sizesCount = 0, minWidthDp = 300, maxWidthDp = 340, minHeightDp = 0, maxHeightDp = 260))
        assertFalse(HostSize.reportsSize(sizesCount = 0, minWidthDp = 300, maxWidthDp = 340, minHeightDp = 200, maxHeightDp = 0))
    }

    @Test
    fun assumedHeightIsAboveCompactBreakpoint() {
        assertFalse(DensityLayout.isCompact(HostSize.ASSUMED_HEIGHT_DP))
    }

    @Test
    fun assumedWidthIsTheScreenWidth() {
        assertEquals(411f, HostSize.assumedWidthDp(screenWidthDp = 411f), 0f)
    }

    @Test
    fun assumedHostSizeIsInlineWithOrWithoutQuickAdd() {
        val width = HostSize.assumedWidthDp(411f)
        assertEquals(ChromePlacement.INLINE, DensityLayout.chromePlacement(width, HostSize.ASSUMED_HEIGHT_DP, showQuickAdd = true, showRefresh = true))
        assertEquals(ChromePlacement.INLINE, DensityLayout.chromePlacement(width, HostSize.ASSUMED_HEIGHT_DP, showQuickAdd = false, showRefresh = false))
    }
}
