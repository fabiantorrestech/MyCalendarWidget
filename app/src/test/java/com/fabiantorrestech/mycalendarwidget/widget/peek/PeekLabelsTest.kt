package com.fabiantorrestech.mycalendarwidget.widget.peek

import com.fabiantorrestech.mycalendarwidget.data.CalendarEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** The Grouped peek's date headers: the day word or weekday, then the date in parentheses. */
class PeekLabelsTest {

    private val today = LocalDate.of(2026, 8, 16) // a Sunday
    private val locale = Locale.US

    private fun header(date: LocalDate) =
        PeekLabels.dateHeader(date, today, todayWord = "Today", tomorrowWord = "Tomorrow", locale = locale)

    @Test
    fun todayHeaderIsTheWordWithTheDate() {
        assertEquals("Today (8/16)", header(today))
    }

    @Test
    fun tomorrowHeaderIsTheWordWithTheDate() {
        assertEquals("Tomorrow (8/17)", header(today.plusDays(1)))
    }

    @Test
    fun otherDaysAreWeekdayWithTheDate() {
        assertEquals("Tue (8/18)", header(today.plusDays(2)))
    }

    @Test
    fun onlyTodayIsBold() {
        assertTrue(PeekLabels.isToday(today, today))
        assertFalse(PeekLabels.isToday(today.plusDays(1), today))
    }

    // ---- timeLabel(): the peek row's time column ----

    private val zone: ZoneId = ZoneId.of("America/Chicago")
    private val utc: ZoneId = ZoneId.of("UTC")

    private fun at(date: LocalDate, hour: Int, minute: Int = 0): Long =
        date.atStartOfDay(zone).plusHours(hour.toLong()).plusMinutes(minute.toLong())
            .toInstant().toEpochMilli()

    private fun event(start: Long, end: Long, allDay: Boolean = false) = CalendarEvent(
        id = 1L, eventId = 1L, title = "e", dtStart = start, dtEnd = end, allDay = allDay,
        location = null, description = null, calendarId = 1L, calendarColor = 0,
        eventColor = null, selfAttendeeStatus = 0, organizer = null, eventTimezone = zone.id,
        hasAlarm = false, rrule = null, meetingUrl = null, mapsQuery = null
    )

    /** All-day instances come back from the provider in UTC midnight-to-midnight terms. */
    private fun allDay(first: LocalDate, lastInclusive: LocalDate) = event(
        start = first.atStartOfDay(utc).toInstant().toEpochMilli(),
        end = lastInclusive.plusDays(1).atStartOfDay(utc).toInstant().toEpochMilli(),
        allDay = true
    )

    /** Sun 18:00 until Tue 14:00: starts today, covers Monday whole, ends Tuesday. */
    private val weekendTrip = event(at(today, 18), at(today.plusDays(2), 14))

    private fun label(e: CalendarEvent, date: LocalDate, use24Hour: Boolean = false) =
        PeekLabels.timeLabel(e, date, zone, use24Hour, allDayWord = "All day")

    @Test
    fun allDayEventReadsAllDayOnItsFirstDay() {
        assertEquals("All day", label(allDay(today, today.plusDays(2)), today))
    }

    @Test
    fun allDayEventReadsAllDayOnALaterDay() {
        assertEquals("All day", label(allDay(today, today.plusDays(2)), today.plusDays(1)))
    }

    @Test
    fun timedEventShowsItsStartOnItsFirstDay() {
        assertEquals("6:00p", label(weekendTrip, today))
        assertEquals("18:00", label(weekendTrip, today, use24Hour = true))
    }

    @Test
    fun timedEventShowsAnArrowAndItsEndOnTheDayItEnds() {
        // A non-breaking space, so a one-line text view never wraps the arrow away
        // from its time.
        assertEquals("\u2192\u00A02:00p", label(weekendTrip, today.plusDays(2)))
        assertEquals("\u2192\u00A014:00", label(weekendTrip, today.plusDays(2), use24Hour = true))
    }

    @Test
    fun timedEventReadsAllDayOnADayItCoversWhole() {
        assertEquals("All day", label(weekendTrip, today.plusDays(1)))
    }

    @Test
    fun timedEventEndingAtMidnightReadsAllDayOnItsLastDay() {
        // Sun 18:00 to Tue 00:00: Monday is covered to its very end.
        val e = event(at(today, 18), today.plusDays(2).atStartOfDay(zone).toInstant().toEpochMilli())
        assertEquals("All day", label(e, today.plusDays(1)))
    }
}
