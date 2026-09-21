package com.fabiantorrestech.mycalendarwidget.data.density

import com.fabiantorrestech.mycalendarwidget.data.DensityCountMode
import com.fabiantorrestech.mycalendarwidget.data.density.DensityConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

/** Event colour used by the [raw] helper unless a test overrides it. */
private val COLOR_A = 0xFF112233.toInt()

/**
 * Unit tests for the pure density core. Everything is pinned to a fixed zone and
 * locale so the assertions are stable wherever the suite runs.
 */
class DensityCalculatorTest {

    private val zone: ZoneId = ZoneId.of("America/New_York")
    private val locale: Locale = Locale.US
    private val day: LocalDate = LocalDate.of(2026, 8, 16)
    private val tomorrow: LocalDate = day.plusDays(1)

    /** Epoch millis for a wall-clock time on [date] in the fixed test zone. */
    private fun at(date: LocalDate, hour: Int, minute: Int): Long =
        LocalDateTime.of(date, LocalTime.of(hour, minute)).atZone(zone).toInstant().toEpochMilli()

    /** Epoch millis for a wall-clock time on the standard test day. */
    private fun t(hour: Int, minute: Int): Long = at(day, hour, minute)

    private fun interval(fromHour: Int, fromMinute: Int, toHour: Int, toMinute: Int) =
        BusyInterval(t(fromHour, fromMinute), t(toHour, toMinute))

    private fun raw(
        begin: Long,
        end: Long,
        allDay: Boolean = false,
        calendarId: Long = 1L,
        selfAttendeeStatus: Int = 0,
        status: Int = 1,
        availability: Int = 0,
        displayColor: Int = COLOR_A,
        calendarColor: Int = COLOR_A
    ) = RawInstance(
        begin = begin,
        end = end,
        allDay = allDay,
        calendarId = calendarId,
        selfAttendeeStatus = selfAttendeeStatus,
        status = status,
        availability = availability,
        displayColor = displayColor,
        calendarColor = calendarColor
    )

    private fun block(
        fromHour: Int,
        fromMinute: Int,
        toHour: Int,
        toMinute: Int,
        id: Long,
        calendarId: Long = 1L
    ) = EventBlock(t(fromHour, fromMinute), t(toHour, toMinute), calendarId, COLOR_A, id)

    /**
     * The six-event day used by the headline and remaining-count tests:
     * 07:00-07:45 (outside the window), 09:00-10:30, 10:00-11:00, 13:00-14:00,
     * 14:00-15:00, 16:30-17:15.
     */
    private fun standardDay(): DayDensity = DensityCalculator.buildDay(
        date = day,
        raw = listOf(
            raw(t(7, 0), t(7, 45)),
            raw(t(9, 0), t(10, 30)),
            raw(t(10, 0), t(11, 0)),
            raw(t(13, 0), t(14, 0)),
            raw(t(14, 0), t(15, 0)),
            raw(t(16, 30), t(17, 15))
        ),
        enabledCalendarIds = emptySet(),
        windowStartMinutes = 480,
        windowEndMinutes = 1320,
        zone = zone
    )

    // ---------------------------------------------------------------- merge

    @Test
    fun `two overlapping intervals merge into one`() {
        val merged = DensityCalculator.mergeIntervals(
            listOf(interval(9, 0, 10, 30), interval(10, 0, 11, 0))
        )
        assertEquals(listOf(interval(9, 0, 11, 0)), merged)
    }

    @Test
    fun `two back-to-back intervals merge into one`() {
        val merged = DensityCalculator.mergeIntervals(
            listOf(interval(9, 0, 10, 0), interval(10, 0, 11, 0))
        )
        assertEquals(listOf(interval(9, 0, 11, 0)), merged)
    }

    @Test
    fun `three separated intervals stay separate`() {
        val input = listOf(interval(9, 0, 10, 0), interval(11, 0, 12, 0), interval(13, 0, 14, 0))
        assertEquals(input, DensityCalculator.mergeIntervals(input))
    }

    @Test
    fun `a one-minute gap keeps two intervals`() {
        val merged = DensityCalculator.mergeIntervals(
            listOf(interval(9, 0, 10, 0), interval(10, 1, 11, 0))
        )
        assertEquals(listOf(interval(9, 0, 10, 0), interval(10, 1, 11, 0)), merged)
    }

    @Test
    fun `a fully nested interval is absorbed`() {
        val merged = DensityCalculator.mergeIntervals(
            listOf(interval(9, 0, 12, 0), interval(10, 0, 11, 0))
        )
        assertEquals(listOf(interval(9, 0, 12, 0)), merged)
    }

    @Test
    fun `unsorted input is sorted before merging`() {
        val merged = DensityCalculator.mergeIntervals(
            listOf(interval(13, 0, 14, 0), interval(9, 0, 10, 0), interval(9, 30, 11, 0))
        )
        assertEquals(listOf(interval(9, 0, 11, 0), interval(13, 0, 14, 0)), merged)
    }

    @Test
    fun `zero-length intervals are dropped`() {
        val merged = DensityCalculator.mergeIntervals(
            listOf(interval(9, 0, 9, 0), interval(11, 0, 12, 0), BusyInterval(t(13, 0), t(12, 0)))
        )
        assertEquals(listOf(interval(11, 0, 12, 0)), merged)
    }

    @Test
    fun `merging an empty list yields an empty list`() {
        assertEquals(emptyList<BusyInterval>(), DensityCalculator.mergeIntervals(emptyList()))
    }

    // --------------------------------------------------------------- clampTo

    @Test
    fun `clampTo trims intervals to the window and drops those outside`() {
        val clamped = DensityCalculator.clampTo(
            listOf(interval(7, 0, 7, 45), interval(7, 30, 8, 30), interval(21, 30, 22, 30)),
            t(8, 0),
            t(22, 0)
        )
        assertEquals(listOf(interval(8, 0, 8, 30), interval(21, 30, 22, 0)), clamped)
    }

    @Test
    fun `clampTo keeps intervals already inside the window untouched`() {
        val input = listOf(interval(9, 0, 10, 0))
        assertEquals(input, DensityCalculator.clampTo(input, t(8, 0), t(22, 0)))
    }

    // --------------------------------------------------------------- filters

    @Test
    fun `a declined instance is not busy`() {
        val instance = raw(t(9, 0), t(10, 0), selfAttendeeStatus = DensityConstants.ATTENDEE_STATUS_DECLINED)
        assertFalse(DensityCalculator.isBusy(instance, emptySet()))
    }

    @Test
    fun `a canceled instance is not busy`() {
        val instance = raw(t(9, 0), t(10, 0), status = DensityConstants.EVENT_STATUS_CANCELED)
        assertFalse(DensityCalculator.isBusy(instance, emptySet()))
    }

    @Test
    fun `an instance marked free is not busy`() {
        val instance = raw(t(9, 0), t(10, 0), availability = DensityConstants.AVAILABILITY_FREE)
        assertFalse(DensityCalculator.isBusy(instance, emptySet()))
    }

    @Test
    fun `an all-day instance is not busy`() {
        val instance = raw(t(0, 0), at(tomorrow, 0, 0), allDay = true)
        assertFalse(DensityCalculator.isBusy(instance, emptySet()))
    }

    @Test
    fun `an instance on a disabled calendar is not busy`() {
        val instance = raw(t(9, 0), t(10, 0), calendarId = 7L)
        assertFalse(DensityCalculator.isBusy(instance, setOf(1L, 2L)))
    }

    @Test
    fun `an empty enabled calendar set means every calendar is enabled`() {
        val instance = raw(t(9, 0), t(10, 0), calendarId = 99L)
        assertTrue(DensityCalculator.isBusy(instance, emptySet()))
    }

    @Test
    fun `hasAllDay is true even though the all-day instance is excluded everywhere else`() {
        val result = DensityCalculator.buildDay(
            date = day,
            raw = listOf(
                raw(t(0, 0), at(tomorrow, 0, 0), allDay = true),
                raw(t(9, 0), t(10, 0))
            ),
            enabledCalendarIds = emptySet(),
            windowStartMinutes = 480,
            windowEndMinutes = 1320,
            zone = zone
        )
        assertTrue(result.hasAllDay)
        assertEquals(1, result.eventCount)
        assertEquals(60, result.busyMinutes)
        assertEquals(listOf(interval(9, 0, 10, 0)), result.dayMerged)
    }

    @Test
    fun `an all-day instance is judged by its UTC date, not by local overlap`() {
        // The provider hands an all-day instance back as UTC midnights: tomorrow's all-day
        // event begins at 20:00 New York time today, and must not flag today.
        val utcMidnight = tomorrow.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        val allDay = raw(utcMidnight, utcMidnight + 86_400_000L, allDay = true)
        assertFalse(DensityCalculator.buildDay(day, listOf(allDay), emptySet(), 480, 1320, zone).hasAllDay)
        assertTrue(DensityCalculator.buildDay(tomorrow, listOf(allDay), emptySet(), 480, 1320, zone).hasAllDay)
        assertFalse(DensityCalculator.buildDay(tomorrow.plusDays(1), listOf(allDay), emptySet(), 480, 1320, zone).hasAllDay)
    }

    @Test
    fun `hasAllDay is false when the all-day instance is on a disabled calendar`() {
        val result = DensityCalculator.buildDay(
            date = day,
            raw = listOf(raw(t(0, 0), at(tomorrow, 0, 0), allDay = true, calendarId = 7L)),
            enabledCalendarIds = setOf(1L, 2L),
            windowStartMinutes = 480,
            windowEndMinutes = 1320,
            zone = zone
        )
        assertFalse(result.hasAllDay)
    }

    @Test
    fun `hasAllDay is false when the all-day instance is declined`() {
        val result = DensityCalculator.buildDay(
            date = day,
            raw = listOf(
                raw(
                    t(0, 0), at(tomorrow, 0, 0),
                    allDay = true,
                    selfAttendeeStatus = DensityConstants.ATTENDEE_STATUS_DECLINED
                )
            ),
            enabledCalendarIds = emptySet(),
            windowStartMinutes = 480,
            windowEndMinutes = 1320,
            zone = zone
        )
        assertFalse(result.hasAllDay)
    }

    @Test
    fun `hasAllDay is false when the all-day instance is canceled`() {
        val result = DensityCalculator.buildDay(
            date = day,
            raw = listOf(
                raw(
                    t(0, 0), at(tomorrow, 0, 0),
                    allDay = true,
                    status = DensityConstants.EVENT_STATUS_CANCELED
                )
            ),
            enabledCalendarIds = emptySet(),
            windowStartMinutes = 480,
            windowEndMinutes = 1320,
            zone = zone
        )
        assertFalse(result.hasAllDay)
    }

    @Test
    fun `hasAllDay is true when the all-day instance is on an enabled calendar`() {
        val result = DensityCalculator.buildDay(
            date = day,
            raw = listOf(raw(t(0, 0), at(tomorrow, 0, 0), allDay = true, calendarId = 1L)),
            enabledCalendarIds = setOf(1L, 2L),
            windowStartMinutes = 480,
            windowEndMinutes = 1320,
            zone = zone
        )
        assertTrue(result.hasAllDay)
    }

    @Test
    fun `eventCount excludes declined and canceled instances`() {
        val result = DensityCalculator.buildDay(
            date = day,
            raw = listOf(
                raw(t(9, 0), t(10, 0)),
                raw(t(11, 0), t(12, 0), selfAttendeeStatus = DensityConstants.ATTENDEE_STATUS_DECLINED),
                raw(t(13, 0), t(14, 0), status = DensityConstants.EVENT_STATUS_CANCELED)
            ),
            enabledCalendarIds = emptySet(),
            windowStartMinutes = 480,
            windowEndMinutes = 1320,
            zone = zone
        )
        assertEquals(1, result.eventCount)
    }

    @Test
    fun `a zero-length instance changes nothing in the resulting day`() {
        val withoutZeroLength = DensityCalculator.buildDay(
            date = day,
            raw = listOf(raw(t(9, 0), t(10, 0)), raw(t(13, 0), t(14, 0))),
            enabledCalendarIds = emptySet(),
            windowStartMinutes = 480,
            windowEndMinutes = 1320,
            zone = zone
        )
        val withZeroLength = DensityCalculator.buildDay(
            date = day,
            raw = listOf(raw(t(9, 0), t(10, 0)), raw(t(13, 0), t(14, 0)), raw(t(11, 0), t(11, 0))),
            enabledCalendarIds = emptySet(),
            windowStartMinutes = 480,
            windowEndMinutes = 1320,
            zone = zone
        )
        assertEquals(withoutZeroLength, withZeroLength)
    }

    @Test
    fun `a negative-length instance changes nothing in the resulting day`() {
        val withoutBad = DensityCalculator.buildDay(
            date = day,
            raw = listOf(raw(t(9, 0), t(10, 0))),
            enabledCalendarIds = emptySet(),
            windowStartMinutes = 480,
            windowEndMinutes = 1320,
            zone = zone
        )
        val withBad = DensityCalculator.buildDay(
            date = day,
            raw = listOf(raw(t(9, 0), t(10, 0)), raw(t(12, 0), t(11, 0))),
            enabledCalendarIds = emptySet(),
            windowStartMinutes = 480,
            windowEndMinutes = 1320,
            zone = zone
        )
        assertEquals(withoutBad, withBad)
    }

    @Test
    fun `instances from other days are ignored`() {
        val result = DensityCalculator.buildDay(
            date = day,
            raw = listOf(
                raw(at(tomorrow, 9, 0), at(tomorrow, 10, 0)),
                raw(t(9, 0), t(10, 0))
            ),
            enabledCalendarIds = emptySet(),
            windowStartMinutes = 480,
            windowEndMinutes = 1320,
            zone = zone
        )
        assertEquals(1, result.eventCount)
        assertEquals(listOf(interval(9, 0, 10, 0)), result.dayMerged)
    }

    // ---------------------------------------------------------------- window

    @Test
    fun `an early event grows the window to its hour and is on the strip`() {
        val result = DensityCalculator.buildDay(
            date = day,
            raw = listOf(raw(t(7, 0), t(7, 45)), raw(t(9, 0), t(10, 0))),
            enabledCalendarIds = emptySet(),
            windowStartMinutes = 480,
            windowEndMinutes = 1320,
            zone = zone
        )
        assertEquals(2, result.eventCount)
        assertEquals(420, result.windowStartMinutes)
        assertEquals(1320, result.windowEndMinutes)
        assertEquals(listOf(interval(7, 0, 7, 45), interval(9, 0, 10, 0)), result.stripMerged)
        assertEquals(listOf(t(7, 0), t(9, 0)), result.stripEvents.map { it.startMillis })
    }

    @Test
    fun `an ordinary day keeps the configured window`() {
        val result = DensityCalculator.buildDay(
            date = day,
            raw = listOf(raw(t(9, 0), t(10, 0))),
            enabledCalendarIds = emptySet(),
            windowStartMinutes = 480,
            windowEndMinutes = 1320,
            zone = zone
        )
        assertEquals(480, result.windowStartMinutes)
        assertEquals(1320, result.windowEndMinutes)
        assertFalse(result.cutAtStart)
        assertFalse(result.cutAtEnd)
    }

    @Test
    fun `a midnight-spanning event is cut at the day end and continues on the next day`() {
        val raw = listOf(raw(t(23, 0), at(tomorrow, 3, 0)))
        val today = DensityCalculator.buildDay(day, raw, emptySet(), 480, 1320, zone)
        assertEquals(1440, today.windowEndMinutes)
        assertTrue(today.cutAtEnd)
        assertFalse(today.cutAtStart)
        assertEquals(listOf(BusyInterval(t(23, 0), at(tomorrow, 0, 0))), today.stripMerged)

        val next = DensityCalculator.buildDay(tomorrow, raw, emptySet(), 480, 1320, zone)
        assertEquals(0, next.windowStartMinutes)
        assertEquals(1320, next.windowEndMinutes)
        assertTrue(next.cutAtStart)
        assertFalse(next.cutAtEnd)
        assertEquals(listOf(BusyInterval(at(tomorrow, 0, 0), at(tomorrow, 3, 0))), next.stripMerged)
    }

    @Test
    fun `a late event grows the window to its hour and is on the strip`() {
        val result = DensityCalculator.buildDay(
            date = day,
            raw = listOf(raw(t(9, 0), t(10, 0)), raw(t(22, 30), t(23, 15))),
            enabledCalendarIds = emptySet(),
            windowStartMinutes = 480,
            windowEndMinutes = 1320,
            zone = zone
        )
        assertEquals(2, result.eventCount)
        assertEquals(480, result.windowStartMinutes)
        assertEquals(1440, result.windowEndMinutes)
        assertEquals(listOf(interval(9, 0, 10, 0), interval(22, 30, 23, 15)), result.stripMerged)
        assertEquals(listOf(t(9, 0), t(22, 30)), result.stripEvents.map { it.startMillis })
    }

    @Test
    fun `an event straddling the window start floors the window to its hour`() {
        val result = DensityCalculator.buildDay(
            date = day,
            raw = listOf(raw(t(7, 30), t(8, 30))),
            enabledCalendarIds = emptySet(),
            windowStartMinutes = 480,
            windowEndMinutes = 1320,
            zone = zone
        )
        assertEquals(420, result.windowStartMinutes)
        assertEquals(listOf(interval(7, 30, 8, 30)), result.stripMerged)
        assertEquals(listOf(t(7, 30) to t(8, 30)), result.stripEvents.map { it.startMillis to it.endMillis })
    }

    @Test
    fun `an event straddling the window end ceils the window to its hour`() {
        val result = DensityCalculator.buildDay(
            date = day,
            raw = listOf(raw(t(21, 30), t(22, 30))),
            enabledCalendarIds = emptySet(),
            windowStartMinutes = 480,
            windowEndMinutes = 1320,
            zone = zone
        )
        assertEquals(1380, result.windowEndMinutes)
        assertEquals(listOf(interval(21, 30, 22, 30)), result.stripMerged)
        assertEquals(listOf(t(21, 30) to t(22, 30)), result.stripEvents.map { it.startMillis to it.endMillis })
    }

    @Test
    fun `an early event contributes to busyMinutes and the strip alike`() {
        val result = DensityCalculator.buildDay(
            date = day,
            raw = listOf(raw(t(7, 0), t(7, 45))),
            enabledCalendarIds = emptySet(),
            windowStartMinutes = 480,
            windowEndMinutes = 1320,
            zone = zone
        )
        assertEquals(45, result.busyMinutes)
        assertEquals(listOf(interval(7, 0, 7, 45)), result.stripMerged)
    }

    // ------------------------------------------------------------------ load

    @Test
    fun `half the baseline is half load`() {
        assertEquals(0.5f, DensityCalculator.dayLoad(240, 480), 0.0001f)
    }

    @Test
    fun `more than the baseline clamps to full load`() {
        assertEquals(1.0f, DensityCalculator.dayLoad(600, 480), 0.0001f)
    }

    @Test
    fun `no busy minutes is zero load`() {
        assertEquals(0f, DensityCalculator.dayLoad(0, 480), 0.0001f)
    }

    @Test
    fun `a zero baseline is zero load`() {
        assertEquals(0f, DensityCalculator.dayLoad(240, 0), 0.0001f)
    }

    @Test
    fun `a day past the baseline overflows`() {
        assertTrue(DensityCalculator.overflowsBaseline(600, 480))
    }

    @Test
    fun `a day at the baseline does not overflow`() {
        assertFalse(DensityCalculator.overflowsBaseline(480, 480))
        assertFalse(DensityCalculator.overflowsBaseline(240, 480))
    }

    @Test
    fun `a zero baseline never overflows`() {
        assertFalse(DensityCalculator.overflowsBaseline(600, 0))
    }

    // -------------------------------------------------------------- headline

    private fun headlineFor(
        featured: DayDensity,
        nowMillis: Long,
        countMode: DensityCountMode = DensityCountMode.LEFT,
        featuredIsToday: Boolean = true,
        use24Hour: Boolean = false
    ) = DensityCalculator.headline(
        featured = featured,
        featuredIsToday = featuredIsToday,
        nowMillis = nowMillis,
        rolloverHour = 19,
        countMode = countMode,
        zone = zone,
        use24Hour = use24Hour,
        locale = locale
    )

    @Test
    fun `LEFT counts events still to come and names the next one`() {
        val headline = headlineFor(standardDay(), t(12, 20))
        assertEquals("3 left", headline.countText)
        assertEquals("next in 40m", headline.qualifierText)
        assertFalse(headline.countIsSentence)
    }

    @Test
    fun `LEFT during a busy block reports when the block ends`() {
        val headline = headlineFor(standardDay(), t(14, 35))
        assertEquals("2 left", headline.countText)
        assertEquals("ends in 25m", headline.qualifierText)
    }

    @Test
    fun `a next event more than ninety minutes away is given as a clock time`() {
        val far = DensityCalculator.buildDay(
            date = day,
            raw = listOf(raw(t(15, 0), t(16, 0))),
            enabledCalendarIds = emptySet(),
            windowStartMinutes = 480,
            windowEndMinutes = 1320,
            zone = zone
        )
        assertEquals("next at 3:00", headlineFor(far, t(12, 20)).qualifierText)
    }

    @Test
    fun `a next event clock time honours the 24-hour setting`() {
        val far = DensityCalculator.buildDay(
            date = day,
            raw = listOf(raw(t(15, 0), t(16, 0))),
            enabledCalendarIds = emptySet(),
            windowStartMinutes = 480,
            windowEndMinutes = 1320,
            zone = zone
        )
        assertEquals("next at 15:00", headlineFor(far, t(12, 20), use24Hour = true).qualifierText)
    }

    @Test
    fun `a busy block with more than ninety minutes left ends at a clock time`() {
        val headline = headlineFor(standardDay(), t(13, 0))
        assertEquals("ends at 3:00", headline.qualifierText)
    }

    @Test
    fun `LEFT with nothing remaining reads All done`() {
        val headline = headlineFor(standardDay(), t(17, 20))
        assertEquals("All done", headline.countText)
        assertEquals("", headline.qualifierText)
        assertTrue(headline.countIsSentence)
    }

    @Test
    fun `TOTAL reports the whole day count`() {
        val headline = headlineFor(standardDay(), t(12, 20), DensityCountMode.TOTAL)
        assertEquals("6 today", headline.countText)
        assertFalse(headline.countIsSentence)
    }

    @Test
    fun `FRACTION reports remaining over total`() {
        val headline = headlineFor(standardDay(), t(12, 20), DensityCountMode.FRACTION)
        assertEquals("3/6 left", headline.countText)
        assertEquals("next in 40m", headline.qualifierText)
        assertFalse(headline.countIsSentence)
    }

    @Test
    fun `FRACTION with nothing remaining reads zero over total`() {
        val headline = headlineFor(standardDay(), t(17, 20), DensityCountMode.FRACTION)
        assertEquals("0/6", headline.countText)
        assertEquals("all done", headline.qualifierText)
    }

    @Test
    fun `an empty day reads Nothing today`() {
        val empty = DensityCalculator.buildDay(
            date = day,
            raw = emptyList(),
            enabledCalendarIds = emptySet(),
            windowStartMinutes = 480,
            windowEndMinutes = 1320,
            zone = zone
        )
        val headline = headlineFor(empty, t(12, 20))
        assertEquals("Nothing today", headline.countText)
        assertEquals("", headline.qualifierText)
        assertTrue(headline.countIsSentence)
    }

    @Test
    fun `TOTAL after the last event says all done`() {
        val headline = headlineFor(standardDay(), t(17, 20), DensityCountMode.TOTAL)
        assertEquals("6 today", headline.countText)
        assertEquals("all done", headline.qualifierText)
    }

    // -------------------------------------------------------------- rollover

    private fun eveningDay(): DayDensity = DensityCalculator.buildDay(
        date = day,
        raw = listOf(raw(t(20, 0), t(21, 0))),
        enabledCalendarIds = emptySet(),
        windowStartMinutes = 480,
        windowEndMinutes = 1320,
        zone = zone
    )

    @Test
    fun `no rollover before the rollover hour`() {
        assertFalse(DensityCalculator.shouldRollover(standardDay(), t(18, 59), 19, zone))
    }

    @Test
    fun `rollover at the rollover hour when nothing is left`() {
        assertTrue(DensityCalculator.shouldRollover(standardDay(), t(19, 0), 19, zone))
    }

    @Test
    fun `no rollover after the rollover hour while an event is still running`() {
        assertFalse(DensityCalculator.shouldRollover(eveningDay(), t(20, 0), 19, zone))
    }

    @Test
    fun `rollover the moment an event past the rollover hour ends`() {
        assertTrue(DensityCalculator.shouldRollover(eveningDay(), t(21, 0), 19, zone))
    }

    @Test
    fun `no early tomorrow keeps a day busy past the rollover hour until midnight`() {
        assertFalse(DensityCalculator.shouldRollover(eveningDay(), t(21, 0), 19, zone, noEarlyTomorrow = true))
        assertFalse(DensityCalculator.shouldRollover(eveningDay(), t(23, 59), 19, zone, noEarlyTomorrow = true))
    }

    @Test
    fun `no early tomorrow still rolls over at the hour when the day was done before it`() {
        assertFalse(DensityCalculator.shouldRollover(standardDay(), t(18, 59), 19, zone, noEarlyTomorrow = true))
        assertTrue(DensityCalculator.shouldRollover(standardDay(), t(19, 0), 19, zone, noEarlyTomorrow = true))
    }

    @Test
    fun `no early tomorrow counts an event ending exactly at the rollover hour as done`() {
        val endsOnTheHour = DensityCalculator.buildDay(
            date = day,
            raw = listOf(raw(t(18, 0), t(19, 0))),
            enabledCalendarIds = emptySet(),
            windowStartMinutes = 480,
            windowEndMinutes = 1320,
            zone = zone
        )
        assertTrue(DensityCalculator.shouldRollover(endsOnTheHour, t(19, 0), 19, zone, noEarlyTomorrow = true))
    }

    @Test
    fun `a featured tomorrow reports the total and the first start`() {
        val next = DensityCalculator.buildDay(
            date = tomorrow,
            raw = listOf(
                raw(at(tomorrow, 9, 0), at(tomorrow, 10, 0)),
                raw(at(tomorrow, 11, 0), at(tomorrow, 12, 0)),
                raw(at(tomorrow, 14, 0), at(tomorrow, 15, 0))
            ),
            enabledCalendarIds = emptySet(),
            windowStartMinutes = 480,
            windowEndMinutes = 1320,
            zone = zone
        )
        val headline = headlineFor(next, t(19, 30), DensityCountMode.LEFT, featuredIsToday = false)
        assertEquals("3 tomorrow", headline.countText)
        assertEquals("first at 9:00", headline.qualifierText)
        assertFalse(headline.countIsSentence)
    }

    @Test
    fun `an empty tomorrow reads Nothing tomorrow`() {
        val next = DensityCalculator.buildDay(
            date = tomorrow,
            raw = emptyList(),
            enabledCalendarIds = emptySet(),
            windowStartMinutes = 480,
            windowEndMinutes = 1320,
            zone = zone
        )
        val headline = headlineFor(next, t(19, 30), DensityCountMode.LEFT, featuredIsToday = false)
        assertEquals("Nothing tomorrow", headline.countText)
        assertEquals("", headline.qualifierText)
        assertTrue(headline.countIsSentence)
    }

    @Test
    fun `headline carries the featured day as weekday and short date`() {
        val headline = headlineFor(standardDay(), t(10, 0), DensityCountMode.LEFT, featuredIsToday = true)
        assertEquals("Sun (8/16)", headline.dateText)
    }

    @Test
    fun `rollover headline dates tomorrow not today`() {
        val next = DensityCalculator.buildDay(
            date = tomorrow,
            raw = emptyList(),
            enabledCalendarIds = emptySet(),
            windowStartMinutes = 480,
            windowEndMinutes = 1320,
            zone = zone
        )
        val headline = headlineFor(next, t(19, 30), DensityCountMode.LEFT, featuredIsToday = false)
        assertEquals("Mon (8/17)", headline.dateText)
    }

    // -------------------------------------------------------- remainingCount

    @Test
    fun `an in-progress event counts as remaining`() {
        assertEquals(3, DensityCalculator.remainingCount(standardDay(), t(13, 30)))
    }

    @Test
    fun `an event that ended one millisecond ago does not count as remaining`() {
        val featured = standardDay()
        assertEquals(3, DensityCalculator.remainingCount(featured, t(14, 0) - 1L))
        assertEquals(2, DensityCalculator.remainingCount(featured, t(14, 0) + 1L))
    }

    @Test
    fun `an out-of-window event counts as remaining until it ends`() {
        val featured = standardDay()
        assertEquals(6, DensityCalculator.remainingCount(featured, t(7, 30)))
        assertEquals(5, DensityCalculator.remainingCount(featured, t(7, 46)))
    }

    // ----------------------------------------------------------- nowFraction

    @Test
    fun `nowFraction pins to zero before the window`() {
        assertEquals(0f, DensityCalculator.nowFraction(t(7, 0), t(8, 0), t(22, 0))!!, 0.0001f)
    }

    @Test
    fun `nowFraction pins to one after the window`() {
        assertEquals(1f, DensityCalculator.nowFraction(t(23, 0), t(8, 0), t(22, 0))!!, 0.0001f)
    }

    @Test
    fun `localMinuteOfDay is wall-clock on the spring-forward day`() {
        val dst = LocalDate.of(2026, 3, 8)
        assertEquals(210, DensityCalculator.localMinuteOfDay(at(dst, 3, 30), dst, zone))
        assertEquals(1440, DensityCalculator.localMinuteOfDay(at(dst.plusDays(1), 0, 0), dst, zone))
    }

    @Test
    fun `nowFraction is zero at the window start`() {
        assertEquals(0f, DensityCalculator.nowFraction(t(8, 0), t(8, 0), t(22, 0))!!, 0.0001f)
    }

    @Test
    fun `nowFraction is one at the window end`() {
        assertEquals(1f, DensityCalculator.nowFraction(t(22, 0), t(8, 0), t(22, 0))!!, 0.0001f)
    }

    @Test
    fun `nowFraction is one half at the window midpoint`() {
        assertEquals(0.5f, DensityCalculator.nowFraction(t(15, 0), t(8, 0), t(22, 0))!!, 0.0001f)
    }

    // ------------------------------------------------------------------- DST

    @Test
    fun `windowBounds uses local minutes on the spring-forward day`() {
        val springForward = LocalDate.of(2026, 3, 8)
        val bounds = DensityCalculator.windowBounds(springForward, 480, 1320, zone)
        assertEquals(at(springForward, 8, 0), bounds.first)
        assertEquals(at(springForward, 22, 0), bounds.last + 1L)
        assertEquals(14L * 60L * 60L * 1000L, bounds.last + 1L - bounds.first)
    }

    @Test
    fun `windowBounds uses local minutes on the fall-back day`() {
        val fallBack = LocalDate.of(2026, 11, 1)
        val bounds = DensityCalculator.windowBounds(fallBack, 480, 1320, zone)
        assertEquals(at(fallBack, 8, 0), bounds.first)
        assertEquals(at(fallBack, 22, 0), bounds.last + 1L)
        assertEquals(14L * 60L * 60L * 1000L, bounds.last + 1L - bounds.first)
    }

    @Test
    fun `dayBounds spans twenty-three hours on the spring-forward day`() {
        val springForward = LocalDate.of(2026, 3, 8)
        val bounds = DensityCalculator.dayBounds(springForward, zone)
        assertEquals(23L * 60L * 60L * 1000L, bounds.last + 1L - bounds.first)
    }

    @Test
    fun `dayBounds spans twenty-five hours on the fall-back day`() {
        val fallBack = LocalDate.of(2026, 11, 1)
        val bounds = DensityCalculator.dayBounds(fallBack, zone)
        assertEquals(25L * 60L * 60L * 1000L, bounds.last + 1L - bounds.first)
    }

    @Test
    fun `dayBounds runs from local midnight to local midnight`() {
        val bounds = DensityCalculator.dayBounds(day, zone)
        assertEquals(t(0, 0), bounds.first)
        assertEquals(at(tomorrow, 0, 0), bounds.last + 1L)
    }

    // ------------------------------------------------------------- laneRects

    @Test
    fun `events that never overlap all sit in lane zero at depth one`() {
        val rects = DensityCalculator.laneRects(
            listOf(block(9, 0, 10, 0, id = 0L), block(11, 0, 12, 0, id = 1L))
        )
        assertEquals(2, rects.size)
        assertTrue(rects.all { it.lane == 0 && it.depth == 1 && it.isEventEnd })
        assertEquals(listOf(t(9, 0) to t(10, 0), t(11, 0) to t(12, 0)), rects.map { it.startMillis to it.endMillis })
    }

    @Test
    fun `two partially overlapping events produce three segments`() {
        val a = block(9, 0, 10, 30, id = 0L)
        val b = block(10, 0, 11, 0, id = 1L)
        val rects = DensityCalculator.laneRects(listOf(a, b))
        assertEquals(4, rects.size)

        val first = rects.filter { it.startMillis == t(9, 0) }
        assertEquals(1, first.size)
        assertEquals(t(10, 0), first[0].endMillis)
        assertEquals(1, first[0].depth)
        assertEquals(0, first[0].lane)

        val middle = rects.filter { it.startMillis == t(10, 0) }
        assertEquals(2, middle.size)
        assertTrue(middle.all { it.depth == 2 && it.endMillis == t(10, 30) })
        assertEquals(listOf(0, 1), middle.map { it.lane })
        // A (id 0) ends at 10:30, B (id 1) runs on to 11:00.
        assertEquals(listOf(true, false), middle.map { it.isEventEnd })

        val last = rects.filter { it.startMillis == t(10, 30) }
        assertEquals(1, last.size)
        assertEquals(t(11, 0), last[0].endMillis)
        assertEquals(1, last[0].depth)
        assertEquals(0, last[0].lane)
        assertTrue(last[0].isEventEnd)
    }

    @Test
    fun `isEventEnd is true on exactly one rect per event`() {
        val rects = DensityCalculator.laneRects(
            listOf(block(9, 0, 10, 30, id = 0L), block(10, 0, 11, 0, id = 1L))
        )
        assertEquals(2, rects.count { it.isEventEnd })
        assertEquals(setOf(t(10, 30), t(11, 0)), rects.filter { it.isEventEnd }.map { it.endMillis }.toSet())
    }

    @Test
    fun `three concurrent events cap at depth two by default`() {
        // The strip is 14dp; with the all-day band above it, two lanes is the most that
        // still reads. The third event shares the last lane.
        val rects = DensityCalculator.laneRects(
            listOf(block(9, 0, 10, 0, id = 0L), block(9, 0, 10, 0, id = 1L), block(9, 0, 10, 0, id = 2L))
        )
        assertEquals(3, rects.size)
        assertTrue(rects.all { it.depth == 2 && it.isEventEnd })
        assertEquals(listOf(0, 1, 1), rects.map { it.lane })
        assertEquals(DensityConstants.MAX_LANES, 2)
    }

    @Test
    fun `three lanes are still available when asked for`() {
        val rects = DensityCalculator.laneRects(
            listOf(block(9, 0, 10, 0, id = 0L), block(9, 0, 10, 0, id = 1L), block(9, 0, 10, 0, id = 2L)),
            maxLanes = 3
        )
        assertTrue(rects.all { it.depth == 3 })
        assertEquals(listOf(0, 1, 2), rects.map { it.lane })
    }

    @Test
    fun `lane ranking is stable when the input order is shuffled`() {
        val a = block(9, 0, 10, 0, id = 0L)
        val b = block(9, 0, 10, 0, id = 1L)
        val c = block(9, 30, 10, 0, id = 2L)
        val ordered = DensityCalculator.laneRects(listOf(a, b, c), maxLanes = 3)
        val shuffled = DensityCalculator.laneRects(listOf(c, b, a), maxLanes = 3)
        assertEquals(ordered, shuffled)
        // The ranking within each segment is by (start, id), not input order: a and b
        // (both starting 9:00) take lanes 0 and 1 by id, and c (starting 9:30, after
        // both are already active) is forced into lane 2 regardless of where it sat in
        // the input list.
        assertEquals(listOf(0, 1, 0, 1, 2), shuffled.map { it.lane })
    }

    @Test
    fun `an event nested inside another produces a stepped shape`() {
        val outer = block(9, 0, 11, 0, id = 0L)
        val inner = block(9, 30, 10, 0, id = 1L)
        val rects = DensityCalculator.laneRects(listOf(outer, inner))
        assertEquals(4, rects.size)
        assertEquals(
            listOf(1, 2, 2, 1),
            rects.map { it.depth }
        )
        assertEquals(
            listOf(
                t(9, 0) to t(9, 30),
                t(9, 30) to t(10, 0),
                t(9, 30) to t(10, 0),
                t(10, 0) to t(11, 0)
            ),
            rects.map { it.startMillis to it.endMillis }
        )
        assertEquals(listOf(0, 0, 1, 0), rects.map { it.lane })
        assertEquals(listOf(false, false, true, true), rects.map { it.isEventEnd })
    }

    @Test
    fun `laneRects of no events is empty`() {
        assertEquals(emptyList<LaneRect>(), DensityCalculator.laneRects(emptyList()))
    }

    @Test
    fun `maxLanes of one collapses every overlap to lane zero at depth one`() {
        val a = block(9, 0, 10, 0, id = 0L)
        val b = block(9, 30, 10, 30, id = 1L)
        val c = block(9, 45, 10, 15, id = 2L)
        val rects = DensityCalculator.laneRects(listOf(a, b, c), maxLanes = 1)
        assertTrue(rects.isNotEmpty())
        assertTrue(rects.all { it.lane == 0 && it.depth == 1 })
    }

    @Test
    fun `maxLanes of zero throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            DensityCalculator.laneRects(listOf(block(9, 0, 10, 0, id = 0L)), maxLanes = 0)
        }
    }

    // ---------------------------------------------------------- resolveColor

    @Test
    fun `resolveColor prefers the display colour when it differs`() {
        val instance = raw(
            t(9, 0), t(10, 0),
            displayColor = 0xFFAABBCC.toInt(),
            calendarColor = 0xFF112233.toInt()
        )
        assertEquals(0xFFAABBCC.toInt(), DensityCalculator.resolveColor(instance))
    }

    @Test
    fun `resolveColor falls back to the calendar colour when they match`() {
        val instance = raw(
            t(9, 0), t(10, 0),
            displayColor = 0xFF112233.toInt(),
            calendarColor = 0xFF112233.toInt()
        )
        assertEquals(0xFF112233.toInt(), DensityCalculator.resolveColor(instance))
    }
}
