package com.fabiantorrestech.mycalendarwidget.widget.density

import android.os.Build
import com.fabiantorrestech.mycalendarwidget.data.DensityStripMode
import com.fabiantorrestech.mycalendarwidget.data.TimeFormat
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfig
import com.fabiantorrestech.mycalendarwidget.data.density.ColorMath
import com.fabiantorrestech.mycalendarwidget.data.density.DayDensity
import com.fabiantorrestech.mycalendarwidget.data.density.DensityCalculator
import com.fabiantorrestech.mycalendarwidget.data.density.DensityConstants
import com.fabiantorrestech.mycalendarwidget.data.density.DensitySnapshot
import com.fabiantorrestech.mycalendarwidget.data.density.LaneRect
import com.fabiantorrestech.mycalendarwidget.data.density.TonalRamp
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The opaque colours one density render needs. Everything is pre-blended (G5): the
 * widget never draws a translucent layer over the launcher.
 */
data class DensityPalette(
    val busy: Int,
    val free: Int,
    val now: Int,
    val background: Int,
    val onSurface: Int,
    val pill: Int
)

/**
 * Layout numbers shared by the real widget ([DensityWidgetContent]) and the settings
 * preview (`PreviewDensityContent`) — plain floats rather than `Dp` so this file stays
 * Compose-free; callers append `.dp` at the point of use. Hoisted here instead of
 * duplicated in both call sites so the two chrome breakpoints and spacer sizes cannot
 * drift apart (G7).
 */
object DensityLayout {
    /**
     * Below this the axis (and, later, the day bars) is dropped: one launcher row is
     * 104dp on a Pixel 9, three rows 344dp, so 160dp separates "one row" from "two or
     * more".
     */
    const val COMPACT_HEIGHT_DP = 160f

    /**
     * Below this the profile switcher and calendar button no longer fit beside the count
     * (which yields width to the chrome before the chrome is pushed off the row), so they
     * move to a row of their own. Sized so a four-column Pixel widget (about 360dp) keeps
     * the full chrome inline: with the refresh and quick-add extras ([REFRESH_EXTRA_WIDTH_DP],
     * [QUICK_ADD_EXTRA_WIDTH_DP]) the breakpoint reaches 352dp; see [narrowBreakpointDp].
     */
    const val NARROW_WIDTH_DP = 260f

    /** One more 56dp chrome button plus its 4dp gap. */
    const val QUICK_ADD_EXTRA_WIDTH_DP = 60f

    /** The 28dp refresh circle plus its 4dp gap. */
    const val REFRESH_EXTRA_WIDTH_DP = 32f

    /** Where the chrome (profile switcher, calendar button, quick-add button) is drawn. */
    enum class ChromePlacement {
        /** Wide enough: the chrome sits in the headline row after the qualifier. */
        INLINE,
        /** Narrow but tall: the chrome gets a right-aligned row of its own at the bottom. */
        BOTTOM_ROW,
        /** Narrow and short: a single button beside the count, nothing else fits. */
        COMPACT_SINGLE
    }

    /** True when the axis and the look-ahead bars are dropped. */
    fun isCompact(heightDp: Float): Boolean = heightDp < COMPACT_HEIGHT_DP

    /** The width below which the chrome no longer fits in the headline row. */
    fun narrowBreakpointDp(showQuickAdd: Boolean, showRefresh: Boolean): Float =
        NARROW_WIDTH_DP +
            (if (showQuickAdd) QUICK_ADD_EXTRA_WIDTH_DP else 0f) +
            (if (showRefresh) REFRESH_EXTRA_WIDTH_DP else 0f)

    /**
     * The chrome placement for a widget of the given size. Deliberately ignores whether a
     * profile switcher is actually present (fewer than two profiles), as the widget always
     * has: the breakpoints are sized for the fullest chrome so the layout does not jump
     * when a profile is added.
     */
    fun chromePlacement(
        widthDp: Float,
        heightDp: Float,
        showQuickAdd: Boolean,
        showRefresh: Boolean
    ): ChromePlacement {
        val narrow = widthDp < narrowBreakpointDp(showQuickAdd, showRefresh)
        return when {
            !narrow -> ChromePlacement.INLINE
            isCompact(heightDp) -> ChromePlacement.COMPACT_SINGLE
            else -> ChromePlacement.BOTTOM_ROW
        }
    }

    /** G7: 12dp of padding on every side of the widget/preview content. */
    const val WIDGET_PADDING_DP = 12f

    /** G7: the strip track is 14dp tall. */
    const val STRIP_HEIGHT_DP = 14f

    /** Gap between the headline row and the strip. */
    const val STRIP_TOP_GAP_DP = 4f

    /** Gap between the strip and the hour axis. */
    const val AXIS_TOP_GAP_DP = 2f

    /** G7: the caret breaks out 2dp above and below the track. */
    const val CARET_OVERHANG_DP = 2f

    /** G7: the background-coloured halo either side of the caret. */
    const val CARET_HALO_DP = 1f

    /** G7: the caret itself is 2dp wide. */
    const val NOW_MARKER_WIDTH_DP = 2f

    /** Height of the chevron marking a block cut at midnight. */
    const val CUT_CHEVRON_DP = 5f

    /** Stroke of that chevron. */
    const val CUT_CHEVRON_STROKE_DP = 1f

    /** How far inside the strip edge the chevron's tip sits. */
    const val CUT_CHEVRON_INSET_DP = 2f

    /**
     * The strip bitmap's full height once the caret's overhang is included — the track
     * stays [STRIP_HEIGHT_DP], but the bitmap (and the `Image` that hosts it) must be
     * tall enough for the caret to break out top and bottom without being clipped.
     */
    const val STRIP_IMAGE_HEIGHT_DP = STRIP_HEIGHT_DP + 2 * CARET_OVERHANG_DP

    /**
     * [STRIP_TOP_GAP_DP] less the caret's overhang, so growing the strip's `Image` to
     * [STRIP_IMAGE_HEIGHT_DP] (which eats into the gap above it) leaves the 14dp track
     * itself exactly where it was before the caret turned on.
     */
    const val STRIP_TOP_GAP_WITH_CARET_DP = STRIP_TOP_GAP_DP - CARET_OVERHANG_DP

    /**
     * [AXIS_TOP_GAP_DP] less the caret's overhang, for the same reason as
     * [STRIP_TOP_GAP_WITH_CARET_DP] but on the strip's bottom edge, so the axis labels
     * below it do not move either.
     */
    const val AXIS_TOP_GAP_WITH_CARET_DP = AXIS_TOP_GAP_DP - CARET_OVERHANG_DP

    /** G7: each look-ahead day bar is 6dp tall. */
    const val DAY_BAR_HEIGHT_DP = 6f

    /** G7: 10dp between look-ahead day bars (and their day labels above them). */
    const val DAY_BAR_GUTTER_DP = 10f

    /** The thin band above a bar that says "an all-day event spans this day". */
    const val ALL_DAY_BAND_DP = 2f

    /** Clear space between that band and the bar under it. */
    const val ALL_DAY_BAND_GAP_DP = 1f

    /** What the look-ahead bars bitmap occupies: band, gap, then the bar itself. */
    const val DAY_BAR_IMAGE_HEIGHT_DP = ALL_DAY_BAND_DP + ALL_DAY_BAND_GAP_DP + DAY_BAR_HEIGHT_DP

    /**
     * A day label keeps its weekday ("Thu (9/18)") only when its column is at least this
     * wide; narrower columns (six or seven bars on a phone-width widget) show the date
     * alone, since an 11sp "Thu (9/18)" needs about 56dp.
     */
    const val DAY_LABEL_WITH_WEEKDAY_MIN_DP = 62f

    /**
     * The width of one look-ahead column: the content width less the gutters between
     * [days] bars, shared equally — the same split [DensityCanvas] draws the bars with.
     */
    fun dayColumnWidthDp(contentWidthDp: Float, days: Int): Float =
        if (days <= 0) contentWidthDp else (contentWidthDp - DAY_BAR_GUTTER_DP * (days - 1)) / days

    /** Gap between the axis (or bottom chrome row) and the look-ahead divider. */
    const val DIVIDER_TOP_GAP_DP = 7f

    /** Gap between the divider and the day-label row. */
    const val LABELS_TOP_GAP_DP = 7f

    /** Gap between the day-label row and the bars bitmap. */
    const val BARS_TOP_GAP_DP = 4f

    /** G7: the peek sheet's "Upcoming" bar and its close button row. */
    const val PEEK_TOP_BAR_DP = 18f

    /** G7: a GROUPED date header row; taller than an event row is thin because its label is set a step above the titles. */
    const val PEEK_HEADER_DP = 20f

    /** G7: one event row in the peek sheet. */
    const val PEEK_ROW_DP = 22f

    /**
     * G7: the DATED day separator's hairline. With 1dp of padding above and below it
     * occupies the 3dp the constraint names.
     */
    const val PEEK_SEPARATOR_DP = 1f

    /** G7: the calendar-colour dot on a peek event row. */
    const val PEEK_DOT_DP = 10f

    /** Corner radius of the filled pill behind the GROUPED peek's "Today" header. */
    const val PEEK_TODAY_PILL_RADIUS_DP = 6f

    /** Horizontal inset of that pill around its label. */
    const val PEEK_TODAY_PILL_INSET_DP = 6f

    /** G7: the DATED date pill's text size, in sp. */
    const val PEEK_DATE_PILL_SP = 9.5f

    /**
     * The peek row's time column. 12-hour times ("10:30a") need more room than 24-hour
     * ones ("10:30"), and a fixed width per mode keeps every dot and title in the list
     * on one vertical line rather than ragged behind times of differing length. The
     * widest label is a carried-over event's end time, "→ 12:30p" / "→ 23:30", which
     * is what these widths are sized for.
     */
    const val PEEK_TIME_COL_12H_DP = 52f
    const val PEEK_TIME_COL_24H_DP = 46f
}

/**
 * The axis as a row of [cellCount] equal-width cells; [labels] has one entry per cell and
 * holds a tick label where a tick starts, null elsewhere.
 *
 * Equal cells rather than proportional weights because Glance's `defaultWeight()` has no
 * ratio, and a label placed in cell `k` therefore has its left edge at exactly `k /
 * cellCount` of the row — no drift from the width of the labels before it. Cell counts
 * stay at or below [MAX_AXIS_CELLS] because a pre-API-31 Glance container tops out at ten
 * children.
 */
data class AxisSpec(val cellCount: Int, val labels: List<String?>)

/**
 * Turns a [DayDensity] plus the user's config into the plain-int specs [DensityCanvas]
 * draws. No Glance and no Compose imports: the settings preview builds the very same
 * specs, so the widget and the preview cannot drift apart.
 *
 * Theme colours arrive already resolved as ARGB ints — resolving a `ColorProvider` needs
 * Glance, and that would defeat the point of this file.
 */
object DensitySpecBuilder {

    /** G9: the free track is 12% of the primary text colour blended onto the ground. */
    private const val FREE_TRACK_TINT = 0.12f
    private const val PILL_TINT_DARK = 0.12f
    private const val PILL_TINT_LIGHT = 0.14f

    /** Detail's contrast guard (brief step 3): thresholds and the edge tint toward onSurface. */
    private const val CONTRAST_HIGH_LUMINANCE = 0.70f
    private const val CONTRAST_LOW_LUMINANCE = 0.30f
    private const val CONTRAST_EDGE_TINT = 0.45f

    /** One axis tick every four hours, as 8a / 12p / 4p / 8p on the default window. */
    private const val AXIS_STEP_MINUTES = 240

    /** How far an overflowing bar's fill is pulled toward the foreground colour. */
    private const val OVERFLOW_HEAVY_T = 0.35f

    private const val MIN_AXIS_CELLS = 4
    private const val MAX_AXIS_CELLS = 10

    /** G7: the shipped default window (08:00-22:00), used when [effectiveWindowMinutes]
     * finds a day's bounds degenerate. */
    private const val DEFAULT_WINDOW_START_MINUTES = 480
    private const val DEFAULT_WINDOW_END_MINUTES = 1320

    /**
     * The window bounds actually used for one render. [WidgetConfig.densityWindowStartMinutes]
     * and [WidgetConfig.densityWindowEndMinutes] are guarded in Settings (`DensitySection`)
     * to always keep `end - start >= 60`, but a hand-edited or otherwise corrupted imported
     * profile JSON could still carry a degenerate pair (end <= start); rather than divide by
     * a zero or negative span below, that render falls back to the shipped default window.
     * The stored config itself is left untouched — this is a render-time fallback only.
     */
    private fun effectiveWindowMinutes(day: DayDensity): Pair<Int, Int> =
        if (day.windowEndMinutes <= day.windowStartMinutes) {
            DEFAULT_WINDOW_START_MINUTES to DEFAULT_WINDOW_END_MINUTES
        } else {
            day.windowStartMinutes to day.windowEndMinutes
        }

    /**
     * [background], [onSurface] and [primary] are the theme colours the caller has
     * already resolved — `GlanceTheme.colors.X.getColor(context).toArgb()` in the widget,
     * `MaterialTheme.colorScheme.X.toArgb()` in the preview. [sdkInt] defaults to the
     * device's own `Build.VERSION.SDK_INT` but is a parameter so a plain JVM unit test
     * can exercise both sides of the API-31 dynamic-colour gate without Robolectric.
     */
    fun palette(
        config: WidgetConfig,
        isDark: Boolean,
        background: Int,
        onSurface: Int,
        primary: Int,
        sdkInt: Int = Build.VERSION.SDK_INT
    ): DensityPalette {
        val rawBusy = when {
            // 0 means "unset, follow the theme"; anything else is the user's own choice.
            config.densityBusyColor != 0 -> config.densityBusyColor
            config.dynamicColor && sdkInt >= Build.VERSION_CODES.S -> primary
            else -> DensityConstants.DEFAULT_BUSY_COLOR
        }
        val busy = withLuminanceFloor(rawBusy, background, onSurface)
        return DensityPalette(
            busy = busy,
            free = ColorMath.lerp(background, onSurface, FREE_TRACK_TINT),
            now = DensityConstants.NOW_MARKER_COLOR,
            background = background,
            onSurface = onSurface,
            pill = ColorMath.lerp(
                background,
                onSurface,
                if (isDark) PILL_TINT_DARK else PILL_TINT_LIGHT
            )
        )
    }

    /**
     * [nowMillis] must be null unless [day] is actually today — the caret is the "you are
     * here" marker and has no meaning on tomorrow's strip. [visibleCalendarIds] is the
     * Tonal rank fallback's source list (see [enabledSortedCalendarIds]); it defaults to
     * empty so existing callers/tests that don't care about Tonal rank stability don't
     * need updating.
     */
    fun stripSpec(
        day: DayDensity,
        config: WidgetConfig,
        palette: DensityPalette,
        widthPx: Int,
        density: Float,
        nowMillis: Long?,
        zone: ZoneId,
        visibleCalendarIds: List<Long> = emptyList()
    ): StripSpec {
        // The window is the day's own (DayDensity.windowStartMinutes/EndMinutes): the
        // configured one grown to fit the day, so the strip and the axis agree by construction.
        val (windowStartMinutes, windowEndMinutes) = effectiveWindowMinutes(day)
        val window = DensityCalculator.windowBounds(
            day.date,
            windowStartMinutes,
            windowEndMinutes,
            zone
        )
        val windowStart = window.first
        val windowEnd = window.last + 1
        val span = (windowEnd - windowStart).toFloat()
        // Fractions are of the window, never of the day: its start is the left edge.
        val fraction: (Long) -> Float = { millis ->
            if (span <= 0f) 0f else ((millis - windowStart).toFloat() / span).coerceIn(0f, 1f)
        }

        val content = when (config.densityStripMode) {
            DensityStripMode.SHAPE -> StripContent.Shape(
                blocks = day.stripMerged.map { fraction(it.startMillis)..fraction(it.endMillis) },
                colorInt = palette.busy
            )

            // The calendar colour already resolved onto each rect (Task 2/3) is exactly
            // right for Detail; only the contrast guard needs to touch it here.
            DensityStripMode.DETAIL -> {
                val rects = DensityCalculator.laneRects(day.stripEvents)
                StripContent.Lanes(
                    rects = rects,
                    windowStartMillis = windowStart,
                    windowEndMillis = windowEnd,
                    laneOutlineColor = null,
                    edgeColors = contrastEdgeColors(rects, palette)
                )
            }

            // One accent, five tones, bucketed per calendar; a background-coloured
            // outline marks the split between events sharing an overlap segment.
            DensityStripMode.TONAL -> {
                val ramp = TonalRamp.ramp(palette.busy, palette.background)
                val ids = enabledSortedCalendarIds(config, day, visibleCalendarIds)
                val rects = DensityCalculator.laneRects(day.stripEvents).map { rect ->
                    val tone = TonalRamp.bucket(rect.calendarId, config.densityCalendarTones, ids)
                    rect.copy(colorInt = ramp[tone])
                }
                StripContent.Lanes(
                    rects = rects,
                    windowStartMillis = windowStart,
                    windowEndMillis = windowEnd,
                    laneOutlineColor = palette.background
                )
            }
        }

        return StripSpec(
            widthPx = max(1, widthPx),
            trackHeightPx = max(1, (DensityLayout.STRIP_HEIGHT_DP * density).roundToInt()),
            overhangPx = max(1, (DensityLayout.CARET_OVERHANG_DP * density).roundToInt()),
            haloPx = max(1, (DensityLayout.CARET_HALO_DP * density).roundToInt()),
            content = content,
            nowFraction = nowMillis?.let {
                DensityCalculator.nowFraction(it, windowStart, windowEnd)
            },
            freeColor = palette.free,
            nowColor = palette.now,
            nowMarkerWidthPx = max(1, (DensityLayout.NOW_MARKER_WIDTH_DP * density).roundToInt()),
            backgroundColor = palette.background,
            pxPerDp = density,
            ghost = null,
            cutAtStart = day.cutAtStart,
            cutAtEnd = day.cutAtEnd,
            // The band is the busy colour in every mode, Detail included: it marks the
            // day, not an event, so it never needs a calendar colour of its own.
            allDayBandColor = if (day.hasAllDay) palette.busy else null
        )
    }

    /**
     * The look-ahead day bars: one load fraction per day in [DensitySnapshot.lookahead],
     * against [WidgetConfig.densityLoadBaselineMinutes]. [widthPx] is the bitmap's full
     * width (the bars span the same width as the strip above them); height and gutter
     * come from [DensityLayout] rather than being passed in, so every call site agrees.
     */
    fun loadBarsSpec(
        snapshot: DensitySnapshot,
        config: WidgetConfig,
        palette: DensityPalette,
        widthPx: Int,
        density: Float
    ): LoadBarsSpec {
        val loads = snapshot.lookahead.map {
            DensityCalculator.dayLoad(it.busyMinutes, config.densityLoadBaselineMinutes)
        }
        return LoadBarsSpec(
            widthPx = max(1, widthPx),
            heightPx = max(1, (DensityLayout.DAY_BAR_IMAGE_HEIGHT_DP * density).roundToInt()),
            loads = loads,
            gutterPx = max(0, (DensityLayout.DAY_BAR_GUTTER_DP * density).roundToInt()),
            fillColor = palette.busy,
            trackColor = palette.free,
            bandPx = max(0, (DensityLayout.ALL_DAY_BAND_DP * density).roundToInt()),
            bandGapPx = max(0, (DensityLayout.ALL_DAY_BAND_GAP_DP * density).roundToInt()),
            allDay = snapshot.lookahead.map { it.hasAllDay },
            overflow = snapshot.lookahead.map {
                DensityCalculator.overflowsBaseline(it.busyMinutes, config.densityLoadBaselineMinutes)
            },
            pxPerDp = density,
            backgroundColor = palette.background,
            // Toward onSurface rather than plain black: heavier in light and dark themes
            // alike, where a darkened fill would sink into a dark background.
            overflowFillColor = ColorMath.lerp(palette.busy, palette.onSurface, OVERFLOW_HEAVY_T)
        )
    }

    /**
     * One label per [DensitySnapshot.lookahead] day: the short weekday and date
     * ("Thu (1/1)", "Fri (1/2)", …) when a column of [columnWidthDp] can hold it (see
     * [DensityLayout.DAY_LABEL_WITH_WEEKDAY_MIN_DP]), else the date alone ("1/1"), in
     * [locale]. Pure date formatting: no calendar content, so it stays clear of G1.
     */
    fun dayLabels(snapshot: DensitySnapshot, locale: Locale, columnWidthDp: Float): List<String> {
        val withWeekday = columnWidthDp >= DensityLayout.DAY_LABEL_WITH_WEEKDAY_MIN_DP
        return snapshot.lookahead.map {
            val date = TimeFormat.shortDate(it.date, locale)
            if (withWeekday) it.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale) + " ($date)" else date
        }
    }

    /**
     * Pushes [busy] toward [onSurface] until it keeps
     * [DensityConstants.BUSY_MIN_LUMINANCE_DELTA] luminance away from [background], or
     * returns it unchanged when it already does. Binary search rather than a fixed step:
     * `lerp`'s luminance is (up to rounding) linear in `t`, so the smallest passing `t` is
     * found to float precision in a handful of iterations, and the result is the least
     * possible shift away from the user's or theme's chosen colour. If even `t = 1` (pure
     * [onSurface]) fails the floor, [onSurface] is returned — [ColorMath.lerp] already
     * returns [onSurface] exactly at `t = 1`, so this is not a separate branch.
     *
     * `delta(t)` is a V shape over `t` in `[0, 1]`, not a monotonic one: because
     * `luminance(lerp(busy, onSurface, t))` moves linearly from `luminance(busy)` toward
     * `luminance(onSurface)`, its distance from the fixed `backgroundLum` falls to zero
     * where the interpolated colour's luminance crosses the background's and rises again
     * past that point. A plain binary search over a V is unsound — it would converge on
     * whichever flank it happened to land in. The early-return guard below is what makes
     * it sound here: it only lets the search proceed when `delta(busy)` (i.e. `delta(0)`)
     * is itself under the floor, which confines `t = 0..1` to the single rising flank
     * that starts inside the dip and climbs to `delta(1)` — never the falling one — so
     * the loop's `hi = mid` / `lo = mid` halving (valid only for a monotonic function) is
     * searching a function that actually is monotonic, and finds the smallest `t` that
     * clears the floor rather than an arbitrary one.
     */
    private fun withLuminanceFloor(busy: Int, background: Int, onSurface: Int): Int {
        val backgroundLum = ColorMath.luminance(background)
        fun delta(color: Int) = abs(ColorMath.luminance(color) - backgroundLum)

        if (delta(busy) >= DensityConstants.BUSY_MIN_LUMINANCE_DELTA) return busy

        var lo = 0f
        var hi = 1f
        repeat(24) {
            val mid = (lo + hi) / 2f
            val candidate = ColorMath.lerp(busy, onSurface, mid)
            if (delta(candidate) >= DensityConstants.BUSY_MIN_LUMINANCE_DELTA) {
                hi = mid
            } else {
                lo = mid
            }
        }
        return ColorMath.lerp(busy, onSurface, hi)
    }

    /**
     * The calendar ids Tonal ranks into buckets: the user's own enabled-calendar
     * selection when they have made one; otherwise [visibleCalendarIds] (the provider's
     * own visible-calendar set, queried once per snapshot by
     * `DensityCalendarSource.queryVisibleCalendarIds` — the same rule Settings'
     * `TonalCalendarList` ranks over), which keeps a calendar's rank stable from one day to
     * the next; otherwise (permission missing, so [visibleCalendarIds] comes back empty)
     * every calendar actually present on the strip today, in id order, so a freshly-seen
     * calendar still ranks somewhere rather than crashing.
     */
    private fun enabledSortedCalendarIds(
        config: WidgetConfig,
        day: DayDensity,
        visibleCalendarIds: List<Long>
    ): List<Long> =
        if (config.enabledCalendarIds.isNotEmpty()) {
            config.enabledCalendarIds.sorted()
        } else if (visibleCalendarIds.isNotEmpty()) {
            visibleCalendarIds
        } else {
            day.stripEvents.map { it.calendarId }.distinct().sorted()
        }

    /**
     * Detail's contrast guard: a rect keyed to its index in [rects] gets an edge colour
     * when its calendar colour is nearly indistinguishable from the ground — too light
     * on a light background, too dark on a dark one.
     */
    private fun contrastEdgeColors(rects: List<LaneRect>, palette: DensityPalette): Map<Int, Int> {
        val isDarkGround = ColorMath.isDark(palette.background)
        val edgeColor = ColorMath.lerp(palette.background, palette.onSurface, CONTRAST_EDGE_TINT)
        val edges = mutableMapOf<Int, Int>()
        rects.forEachIndexed { index, rect ->
            val lum = ColorMath.luminance(rect.colorInt)
            val tooClose = (lum > CONTRAST_HIGH_LUMINANCE && !isDarkGround) ||
                (lum < CONTRAST_LOW_LUMINANCE && isDarkGround)
            if (tooClose) edges[index] = edgeColor
        }
        return edges
    }

    /**
     * Tick labels every four hours from the window start, placed at their true fraction
     * of the window. The instants come from the same [DensityCalculator.windowBounds] the
     * strip uses, so on a DST day the labels move with the blocks.
     */
    fun axisSpec(
        day: DayDensity,
        zone: ZoneId,
        use24Hour: Boolean
    ): AxisSpec {
        val date = day.date
        val (windowStartMinutes, windowEndMinutes) = effectiveWindowMinutes(day)
        val window = DensityCalculator.windowBounds(date, windowStartMinutes, windowEndMinutes, zone)
        val windowStart = window.first
        val span = (window.last + 1 - windowStart).toFloat()
        if (span <= 0f) return AxisSpec(1, listOf(null))

        val ticks = ArrayList<Pair<Float, String>>()
        var minute = windowStartMinutes
        while (minute < windowEndMinutes) {
            val at = date.atStartOfDay().plusMinutes(minute.toLong()).atZone(zone)
            val fraction = (at.toInstant().toEpochMilli() - windowStart) / span
            if (fraction in 0f..1f) ticks.add(fraction to hourLabel(at, use24Hour))
            minute += AXIS_STEP_MINUTES
        }
        if (ticks.isEmpty()) return AxisSpec(1, listOf(null))

        val cellCount = axisCellCount(ticks.map { it.first })
        val labels = arrayOfNulls<String>(cellCount)
        ticks.forEach { (fraction, label) ->
            val cell = (fraction * cellCount).roundToInt().coerceIn(0, cellCount - 1)
            if (labels[cell] == null) labels[cell] = label
        }
        return AxisSpec(cellCount, labels.toList())
    }

    /**
     * The cell count (from [MIN_AXIS_CELLS] to [MAX_AXIS_CELLS]) with the lowest maximum
     * tick error — how far a tick's true fraction sits from the left edge of the cell it
     * is rounded into (`indices[i] / cells`, not the cell's centre — a label's own left
     * edge is what actually lands at that fraction) — ties broken toward the fewest cells
     * by only replacing the current best on a strict improvement. The default
     * 08:00–22:00 window lands exactly on seven cells (ticks at 0, 2, 4 and 6).
     */
    private fun axisCellCount(fractions: List<Float>): Int {
        var best = MAX_AXIS_CELLS
        var bestError = Float.MAX_VALUE
        for (cells in MIN_AXIS_CELLS..MAX_AXIS_CELLS) {
            if (fractions.size > cells) continue
            val indices = fractions.map { (it * cells).roundToInt().coerceIn(0, cells - 1) }
            if (indices.distinct().size != indices.size) continue
            val error = fractions.indices.maxOf { i ->
                abs(fractions[i] - indices[i].toFloat() / cells)
            }
            if (error < bestError - 0.0001f) {
                bestError = error
                best = cells
            }
        }
        return best
    }

    /** "8a" / "12p" in 12-hour mode, "08" / "20" in 24-hour mode — hours only, no minutes. */
    private fun hourLabel(at: ZonedDateTime, use24Hour: Boolean): String {
        if (use24Hour) return at.hour.toString().padStart(2, '0')
        val hour12 = if (at.hour % 12 == 0) 12 else at.hour % 12
        return "$hour12" + if (at.hour < 12) "a" else "p"
    }
}
