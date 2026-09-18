package com.fabiantorrestech.mycalendarwidget.ui.preview

import com.fabiantorrestech.mycalendarwidget.data.WidgetConfig
import com.fabiantorrestech.mycalendarwidget.data.density.DensityCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * The scripted sample calendar every settings preview runs on: today busy with a
 * collision and an all-day event, tomorrow light, the day after over every baseline,
 * the third day medium with an all-day, and further days cycling those three.
 */
class SampleCalendarTest {

    private val zone: ZoneId = ZoneId.of("America/New_York")
    private val today: LocalDate = LocalDate.of(2026, 9, 18)
    private val config = WidgetConfig()

    private fun day(offset: Int, lookahead: Int = 7) = DensityCalculator.buildDay(
        date = today.plusDays(offset.toLong()),
        raw = SampleCalendar.rawInstances(today, zone, lookahead),
        enabledCalendarIds = emptySet(),
        windowStartMinutes = config.densityWindowStartMinutes,
        windowEndMinutes = config.densityWindowEndMinutes,
        zone = zone
    )

    @Test
    fun todayHasACollisionAndAnAllDay() {
        val d = day(0)
        assertTrue(d.hasAllDay)
        // Two timed events overlap, so the lane layout needs a second lane somewhere.
        assertTrue(DensityCalculator.laneRects(d.stripEvents).any { it.depth >= 2 })
        assertTrue(d.eventCount >= 5)
    }

    @Test
    fun tomorrowIsLight() {
        val d = day(1)
        assertEquals(30, d.busyMinutes)
        assertFalse(d.hasAllDay)
    }

    @Test
    fun dayAfterTomorrowOverflowsEveryBaselineStep() {
        // The baseline slider tops out at 12h (720 min).
        assertTrue(day(2).busyMinutes > 720)
    }

    @Test
    fun thirdDayIsMediumWithAllDay() {
        val d = day(3)
        assertTrue(d.hasAllDay)
        assertTrue(d.busyMinutes in 120..360)
    }

    @Test
    fun extraDaysCycleTheTemplates() {
        listOf(4 to 1, 5 to 2, 6 to 3).forEach { (extra, template) ->
            assertEquals("offset $extra", day(template).busyMinutes, day(extra).busyMinutes)
            assertEquals("offset $extra", day(template).hasAllDay, day(extra).hasAllDay)
        }
    }

    @Test
    fun eventsMatchInstancesDayForDay() {
        val events = SampleCalendar.eventsByDay(today, zone, lookaheadDays = 3)
        (0..3).forEach { offset ->
            val date = today.plusDays(offset.toLong())
            assertEquals("day $offset", day(offset, lookahead = 3).eventCount + (if (day(offset, 3).hasAllDay) 1 else 0), events[date]?.size ?: 0)
        }
        assertTrue(events.values.flatten().all { it.title.isNotBlank() })
    }

    @Test
    fun snapshotFeaturesTodayWithLookaheadCount() {
        val now = today.atTime(LocalTime.of(23, 30)).atZone(zone).toInstant().toEpochMilli()
        val snapshot = SampleCalendar.snapshot(config.copy(densityLookaheadDays = 5), now, zone)
        assertTrue(snapshot.featuredIsToday)
        assertEquals(today, snapshot.featured.date)
        assertEquals(5, snapshot.lookahead.size)
        assertEquals(listOf(1L, 2L, 3L, 4L), snapshot.visibleCalendarIds)
    }
}
