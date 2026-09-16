package com.fabiantorrestech.mycalendarwidget.widget

/**
 * What to do when the launcher never tells the widget how big it is.
 *
 * Glance 1.1.1 (`AppWidgetUtils.extractAllSizes` / `estimateSizes`) reads the host's
 * `OPTION_APPWIDGET_SIZES` list and, failing that, the four `OPTION_APPWIDGET_MIN/MAX_
 * WIDTH/HEIGHT` ints. A host that calls neither `updateAppWidgetSize` nor
 * `setAppWidgetOptions` (the Inkos launcher, for one) leaves all of them empty, and Glance
 * then composes for the provider's minimum: `min(minWidth, minResizeWidth)` by
 * `min(minHeight, minResizeHeight)`, 180x40dp for this widget. That is small enough to
 * drop the density axis and look-ahead bars on a widget the launcher may well be drawing
 * at 400dp tall.
 *
 * [reportsSize] mirrors Glance's test exactly so the fallback only ever fires when Glance's
 * own would; [BridgeCalWidget] then provides an assumed size instead: the screen width
 * (such launchers stretch the widget edge to edge) by [ASSUMED_HEIGHT_DP], which is what
 * the settings preview also assumes when it is given no height. The launcher clips
 * anything taller than the box it actually draws, so the user sets that box high enough.
 */
object HostSize {
    /** Above the density compact breakpoint, so the full layout is composed. */
    const val ASSUMED_HEIGHT_DP = 200f

    /**
     * True when Glance would compose for a host-reported size rather than the provider
     * minimum. A non-empty sizes list is authoritative; otherwise ANY zero among the four
     * option ints sends Glance to the fallback (its check is an `||`, not an `&&`).
     */
    fun reportsSize(
        sizesCount: Int,
        minWidthDp: Int,
        maxWidthDp: Int,
        minHeightDp: Int,
        maxHeightDp: Int
    ): Boolean {
        if (sizesCount > 0) return true
        return minWidthDp != 0 && maxWidthDp != 0 && minHeightDp != 0 && maxHeightDp != 0
    }

    /** The width to assume for a size-blind host: the whole screen. */
    fun assumedWidthDp(screenWidthDp: Float): Float = screenWidthDp
}
