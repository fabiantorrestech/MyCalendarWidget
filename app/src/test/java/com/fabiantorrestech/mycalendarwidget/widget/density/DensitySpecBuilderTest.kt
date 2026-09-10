package com.fabiantorrestech.mycalendarwidget.widget.density

import com.fabiantorrestech.mycalendarwidget.data.DensityStripMode
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfig
import com.fabiantorrestech.mycalendarwidget.data.density.ColorMath
import com.fabiantorrestech.mycalendarwidget.data.density.DayDensity
import com.fabiantorrestech.mycalendarwidget.data.density.DensityCalculator
import com.fabiantorrestech.mycalendarwidget.data.density.DensityConstants
import com.fabiantorrestech.mycalendarwidget.data.density.DensitySnapshot
import com.fabiantorrestech.mycalendarwidget.data.density.RawInstance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs

private const val BACKGROUND = 0xFFFFFFFF.toInt()
private const val ON_SURFACE = 0xFF000000.toInt()
private const val PRIMARY = 0xFF6750A4.toInt()
private const val EVENT_COLOR = 0xFF112233.toInt()

// Material-default-ish ground/onSurface used for the luminance-floor cases (amendment A),
// matching the values named in the controller amendment.
private const val LIGHT_BG_MD = 0xFFFFFBFE.toInt()
private const val ON_SURFACE_MD = 0xFF1C1B1F.toInt()
private const val PASTEL_ACCENT = 0xFFCFC8FF.toInt()
private const val COMPLIANT_ACCENT = 0xFF7F77DD.toInt()

// A dark accent on a dark ground, lightened toward a light onSurface.
private const val DARK_BG_MD = 0xFF1C1B1F.toInt()
private const val LIGHT_ON_SURFACE_MD = 0xFFE6E1E5.toInt()
private const val DARK_ACCENT = 0xFF3D3A5C.toInt()

/**
 * [DensitySpecBuilder] is Glance/Compose-free but not Android-free (it reads
 * `Build.VERSION.SDK_INT`), so it gets a plain JUnit4 test rather than Robolectric —
 * the API-31 gate is exercised via the injectable `sdkInt` parameter (G11).
 */
class DensitySpecBuilderTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val date: LocalDate = LocalDate.of(2026, 1, 15)
    private val config = WidgetConfig()

    private fun at(hour: Int, minute: Int = 0): Long =
        LocalDateTime.of(date, LocalTime.of(hour, minute)).atZone(zone).toInstant().toEpochMilli()

    private fun singleEventDay(startHour: Int, endHour: Int) = DensityCalculator.buildDay(
        date = date,
        raw = listOf(
            RawInstance(
                begin = at(startHour),
                end = at(endHour),
                allDay = false,
                calendarId = 1L,
                selfAttendeeStatus = 0,
                status = 1,
                availability = 0,
                displayColor = EVENT_COLOR,
                calendarColor = EVENT_COLOR
            )
        ),
        enabledCalendarIds = emptySet(),
        windowStartMinutes = config.densityWindowStartMinutes,
        windowEndMinutes = config.densityWindowEndMinutes,
        zone = zone
    )

    private fun palette(cfg: WidgetConfig = config, sdkInt: Int = 33) = DensitySpecBuilder.palette(
        config = cfg,
        isDark = false,
        background = BACKGROUND,
        onSurface = ON_SURFACE,
        primary = PRIMARY,
        sdkInt = sdkInt
    )

    private fun dayAt(d: LocalDate, busyMinutes: Int = 0) = DayDensity(
        date = d,
        eventCount = 0,
        hasAllDay = false,
        dayMerged = emptyList(),
        stripMerged = emptyList(),
        stripEvents = emptyList(),
        busyMinutes = busyMinutes,
        busyEnds = emptyList()
    )

    @Test
    fun defaultWindowHasSevenAxisCellsWithTicksAtEvenCells() {
        val axis = DensitySpecBuilder.axisSpec(date, config, zone, use24Hour = false)
        assertEquals(7, axis.cellCount)
        assertNotNull(axis.labels[0])
        assertNotNull(axis.labels[2])
        assertNotNull(axis.labels[4])
        assertNotNull(axis.labels[6])
        assertNull(axis.labels[1])
        assertNull(axis.labels[3])
        assertNull(axis.labels[5])
    }

    @Test
    fun shapeStripSpansTheEventsFractionOfTheDefaultWindow() {
        val day = singleEventDay(9, 10)
        val spec = DensitySpecBuilder.stripSpec(day, config, palette(), 1000, 1f, null, zone)
        val shape = spec.content as StripContent.Shape
        assertEquals(1, shape.blocks.size)
        assertEquals(0.07143f, shape.blocks[0].start, 0.001f)
        assertEquals(0.14286f, shape.blocks[0].endInclusive, 0.001f)
    }

    @Test
    fun busyColorPrecedenceNonZeroConfigWins() {
        val cfg = config.copy(densityBusyColor = EVENT_COLOR, dynamicColor = true)
        assertEquals(EVENT_COLOR, palette(cfg, sdkInt = 33).busy)
    }

    @Test
    fun busyColorFollowsDynamicPrimaryOnApi31Plus() {
        val cfg = config.copy(densityBusyColor = 0, dynamicColor = true)
        assertEquals(PRIMARY, palette(cfg, sdkInt = 31).busy)
    }

    @Test
    fun busyColorFallsBackToDefaultBelowApi31() {
        val cfg = config.copy(densityBusyColor = 0, dynamicColor = true)
        assertEquals(DensityConstants.DEFAULT_BUSY_COLOR, palette(cfg, sdkInt = 30).busy)
    }

    @Test
    fun busyColorFallsBackToDefaultWhenDynamicColorIsOff() {
        val cfg = config.copy(densityBusyColor = 0, dynamicColor = false)
        assertEquals(DensityConstants.DEFAULT_BUSY_COLOR, palette(cfg, sdkInt = 33).busy)
    }

    @Test
    fun tonalModeProducesLanesOutlinedInTheBackground() {
        val day = singleEventDay(9, 10)
        val cfg = config.copy(densityStripMode = DensityStripMode.TONAL)
        val spec = DensitySpecBuilder.stripSpec(day, cfg, palette(), 1000, 1f, null, zone)
        val lanes = spec.content as StripContent.Lanes
        assertEquals(BACKGROUND, lanes.laneOutlineColor)
    }

    @Test
    fun detailModeProducesLanesWithNoOutline() {
        val day = singleEventDay(9, 10)
        val cfg = config.copy(densityStripMode = DensityStripMode.DETAIL)
        val spec = DensitySpecBuilder.stripSpec(day, cfg, palette(), 1000, 1f, null, zone)
        val lanes = spec.content as StripContent.Lanes
        assertNull(lanes.laneOutlineColor)
    }

    @Test
    fun shapeModeProducesShapeContent() {
        val day = singleEventDay(9, 10)
        val cfg = config.copy(densityStripMode = DensityStripMode.SHAPE)
        val spec = DensitySpecBuilder.stripSpec(day, cfg, palette(), 1000, 1f, null, zone)
        assertTrue(spec.content is StripContent.Shape)
    }

    // --- Task 10: the caret's overhang, halo and marker width in device pixels -------

    @Test
    fun stripSpecCaretGeometryAtDensityTwo() {
        val day = singleEventDay(9, 10)
        val spec = DensitySpecBuilder.stripSpec(day, config, palette(), 1000, 2f, null, zone)
        assertEquals(4, spec.overhangPx)
        assertEquals(2, spec.haloPx)
        assertEquals(4, spec.nowMarkerWidthPx)
        assertEquals(36, spec.heightPx)
    }

    @Test
    fun stripSpecCaretGeometryAtDensityOne() {
        val day = singleEventDay(9, 10)
        val spec = DensitySpecBuilder.stripSpec(day, config, palette(), 1000, 1f, null, zone)
        assertEquals(2, spec.overhangPx)
        assertEquals(1, spec.haloPx)
        assertEquals(2, spec.nowMarkerWidthPx)
        assertEquals(18, spec.heightPx)
    }

    @Test
    fun stripSpecNowFractionIsNullWhenFeaturedIsNotToday() {
        // The caller (widget/preview) passes nowMillis = null whenever
        // `featuredIsToday` is false; stripSpec itself must then omit nowFraction.
        val day = singleEventDay(9, 10)
        val spec = DensitySpecBuilder.stripSpec(day, config, palette(), 1000, 1f, null, zone)
        assertNull(spec.nowFraction)
    }

    @Test
    fun stripSpecNowFractionIsNonNullWhenFeaturedIsTodayAndNowIsInsideTheWindow() {
        val day = singleEventDay(9, 10)
        val now = at(14, 30)
        val spec = DensitySpecBuilder.stripSpec(day, config, palette(), 1000, 1f, now, zone)
        assertNotNull(spec.nowFraction)
    }

    // --- Amendment A: palette luminance floor ---------------------------------------

    @Test
    fun pastelBusyOnLightGroundIsDarkenedToMeetTheFloor() {
        val cfg = config.copy(densityBusyColor = PASTEL_ACCENT, dynamicColor = false)
        val p = DensitySpecBuilder.palette(
            config = cfg,
            isDark = false,
            background = LIGHT_BG_MD,
            onSurface = ON_SURFACE_MD,
            primary = PRIMARY,
            sdkInt = 33
        )
        assertNotEquals(PASTEL_ACCENT, p.busy)
        val delta = abs(ColorMath.luminance(p.busy) - ColorMath.luminance(LIGHT_BG_MD))
        assertTrue(
            "delta was $delta",
            delta >= DensityConstants.BUSY_MIN_LUMINANCE_DELTA - 0.01f
        )
    }

    @Test
    fun compliantBusyOnLightGroundIsUnchanged() {
        val cfg = config.copy(densityBusyColor = COMPLIANT_ACCENT, dynamicColor = false)
        val p = DensitySpecBuilder.palette(
            config = cfg,
            isDark = false,
            background = LIGHT_BG_MD,
            onSurface = ON_SURFACE_MD,
            primary = PRIMARY,
            sdkInt = 33
        )
        assertEquals(COMPLIANT_ACCENT, p.busy)
    }

    @Test
    fun darkAccentOnDarkGroundIsLightenedTowardALightOnSurface() {
        val cfg = config.copy(densityBusyColor = DARK_ACCENT, dynamicColor = false)
        val p = DensitySpecBuilder.palette(
            config = cfg,
            isDark = true,
            background = DARK_BG_MD,
            onSurface = LIGHT_ON_SURFACE_MD,
            primary = PRIMARY,
            sdkInt = 33
        )
        assertNotEquals(DARK_ACCENT, p.busy)
        assertTrue(ColorMath.luminance(p.busy) > ColorMath.luminance(DARK_ACCENT))
        val delta = abs(ColorMath.luminance(p.busy) - ColorMath.luminance(DARK_BG_MD))
        assertTrue(
            "delta was $delta",
            delta >= DensityConstants.BUSY_MIN_LUMINANCE_DELTA - 0.01f
        )
    }

    // --- Look-ahead bars: dayLabels and loadBarsSpec --------------------------------

    @Test
    fun dayLabelsUsesShortWeekdayNamesInOrder() {
        // 2026-01-01 is a Thursday.
        val lookahead = listOf(
            dayAt(LocalDate.of(2026, 1, 1)),
            dayAt(LocalDate.of(2026, 1, 2)),
            dayAt(LocalDate.of(2026, 1, 3))
        )
        val snapshot = DensitySnapshot(
            hasPermission = true,
            featured = dayAt(date),
            featuredIsToday = true,
            lookahead = lookahead,
            nowMillis = 0L
        )
        assertEquals(
            listOf("Thu", "Fri", "Sat"),
            DensitySpecBuilder.dayLabels(snapshot, Locale.US)
        )
    }

    @Test
    fun loadBarsSpecComputesLoadsFromBusyMinutesAndBaseline() {
        val lookahead = listOf(
            dayAt(date.plusDays(1), busyMinutes = 300),
            dayAt(date.plusDays(2), busyMinutes = 400),
            dayAt(date.plusDays(3), busyMinutes = 60)
        )
        val snapshot = DensitySnapshot(
            hasPermission = true,
            featured = dayAt(date),
            featuredIsToday = true,
            lookahead = lookahead,
            nowMillis = 0L
        )
        val cfg = config.copy(densityLoadBaselineMinutes = 480)
        val p = palette()
        val spec = DensitySpecBuilder.loadBarsSpec(
            snapshot = snapshot,
            config = cfg,
            palette = p,
            widthPx = 1000,
            density = 2f
        )
        assertEquals(3, spec.loads.size)
        assertEquals(0.625f, spec.loads[0], 0.001f)
        assertEquals(0.83333f, spec.loads[1], 0.001f)
        assertEquals(0.125f, spec.loads[2], 0.001f)
        assertEquals(1000, spec.widthPx)
        assertEquals(12, spec.heightPx)
        assertEquals(20, spec.gutterPx)
        assertEquals(p.busy, spec.fillColor)
        assertEquals(p.free, spec.trackColor)
    }
}
