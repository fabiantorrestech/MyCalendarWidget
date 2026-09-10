package com.fabiantorrestech.mycalendarwidget.widget.density

import com.fabiantorrestech.mycalendarwidget.data.DensityStripMode
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfig
import com.fabiantorrestech.mycalendarwidget.data.density.DensityCalculator
import com.fabiantorrestech.mycalendarwidget.data.density.DensityConstants
import com.fabiantorrestech.mycalendarwidget.data.density.RawInstance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

private const val BACKGROUND = 0xFFFFFFFF.toInt()
private const val ON_SURFACE = 0xFF000000.toInt()
private const val PRIMARY = 0xFF6750A4.toInt()
private const val EVENT_COLOR = 0xFF112233.toInt()

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
}
