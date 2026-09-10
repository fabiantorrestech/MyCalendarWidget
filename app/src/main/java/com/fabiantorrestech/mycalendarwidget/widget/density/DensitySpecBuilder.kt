package com.fabiantorrestech.mycalendarwidget.widget.density

import android.os.Build
import com.fabiantorrestech.mycalendarwidget.data.DensityStripMode
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfig
import com.fabiantorrestech.mycalendarwidget.data.density.ColorMath
import com.fabiantorrestech.mycalendarwidget.data.density.DayDensity
import com.fabiantorrestech.mycalendarwidget.data.density.DensityCalculator
import com.fabiantorrestech.mycalendarwidget.data.density.DensityConstants
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
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
     * Below this the profile switcher and calendar button no longer fit beside the
     * qualifier without truncating it to an ellipsis, so they move to a row of their own.
     */
    const val NARROW_WIDTH_DP = 300f

    /** G7: 12dp of padding on every side of the widget/preview content. */
    const val WIDGET_PADDING_DP = 12f

    /** G7: the strip track is 14dp tall. */
    const val STRIP_HEIGHT_DP = 14f

    /** Gap between the headline row and the strip. */
    const val STRIP_TOP_GAP_DP = 4f

    /** Gap between the strip and the hour axis. */
    const val AXIS_TOP_GAP_DP = 2f
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

    /** G7: now-marker 2dp; the track height itself comes from [DensityLayout.STRIP_HEIGHT_DP]. */
    private const val NOW_MARKER_DP = 2f

    /** G9: the free track is 12% of the primary text colour blended onto the ground. */
    private const val FREE_TRACK_TINT = 0.12f
    private const val PILL_TINT_DARK = 0.12f
    private const val PILL_TINT_LIGHT = 0.14f

    /** One axis tick every four hours, as 8a / 12p / 4p / 8p on the default window. */
    private const val AXIS_STEP_MINUTES = 240

    private const val MIN_AXIS_CELLS = 4
    private const val MAX_AXIS_CELLS = 10

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
        val busy = when {
            // 0 means "unset, follow the theme"; anything else is the user's own choice.
            config.densityBusyColor != 0 -> config.densityBusyColor
            config.dynamicColor && sdkInt >= Build.VERSION_CODES.S -> primary
            else -> DensityConstants.DEFAULT_BUSY_COLOR
        }
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
     * here" marker and has no meaning on tomorrow's strip.
     */
    fun stripSpec(
        day: DayDensity,
        config: WidgetConfig,
        palette: DensityPalette,
        widthPx: Int,
        density: Float,
        nowMillis: Long?,
        zone: ZoneId
    ): StripSpec {
        val window = DensityCalculator.windowBounds(
            day.date,
            config.densityWindowStartMinutes,
            config.densityWindowEndMinutes,
            zone
        )
        val windowStart = window.first
        val windowEnd = window.last + 1
        val span = (windowEnd - windowStart).toFloat()
        // Fractions are of the window, never of the day: 08:00 is the left edge.
        val fraction: (Long) -> Float = { millis ->
            if (span <= 0f) 0f else ((millis - windowStart).toFloat() / span).coerceIn(0f, 1f)
        }

        val content = when (config.densityStripMode) {
            DensityStripMode.SHAPE -> StripContent.Shape(
                blocks = day.stripMerged.map { fraction(it.startMillis)..fraction(it.endMillis) },
                colorInt = palette.busy
            )

            // Tonal and Detail differ only in how a rect's colour is chosen, which is the
            // next task's job; until then every lane carries the single busy accent.
            DensityStripMode.TONAL, DensityStripMode.DETAIL -> StripContent.Lanes(
                rects = DensityCalculator.laneRects(day.stripEvents)
                    .map { it.copy(colorInt = palette.busy) },
                widthFractionOf = fraction,
                laneOutlineColor = null
            )
        }

        return StripSpec(
            widthPx = max(1, widthPx),
            trackHeightPx = max(1, (DensityLayout.STRIP_HEIGHT_DP * density).roundToInt()),
            overhangPx = 0,
            haloPx = 0,
            content = content,
            nowFraction = nowMillis?.let {
                DensityCalculator.nowFraction(it, windowStart, windowEnd)
            },
            freeColor = palette.free,
            nowColor = palette.now,
            nowMarkerWidthPx = max(1, (NOW_MARKER_DP * density).roundToInt()),
            backgroundColor = palette.background,
            pxPerDp = density,
            ghost = null
        )
    }

    /**
     * Tick labels every four hours from the window start, placed at their true fraction
     * of the window. The instants come from the same [DensityCalculator.windowBounds] the
     * strip uses, so on a DST day the labels move with the blocks.
     */
    fun axisSpec(
        date: LocalDate,
        config: WidgetConfig,
        zone: ZoneId,
        use24Hour: Boolean
    ): AxisSpec {
        val window = DensityCalculator.windowBounds(
            date,
            config.densityWindowStartMinutes,
            config.densityWindowEndMinutes,
            zone
        )
        val windowStart = window.first
        val span = (window.last + 1 - windowStart).toFloat()
        if (span <= 0f) return AxisSpec(1, listOf(null))

        val ticks = ArrayList<Pair<Float, String>>()
        var minute = config.densityWindowStartMinutes
        while (minute < config.densityWindowEndMinutes) {
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
     * tick error — how far a tick's true fraction sits from the centre of the cell it is
     * rounded into — ties broken toward the fewest cells by only replacing the current
     * best on a strict improvement. The default 08:00–22:00 window lands exactly on
     * seven cells (ticks at 0, 2, 4 and 6).
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
