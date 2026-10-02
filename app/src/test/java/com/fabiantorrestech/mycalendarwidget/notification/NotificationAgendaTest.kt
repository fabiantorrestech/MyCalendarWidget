package com.fabiantorrestech.mycalendarwidget.notification

import com.fabiantorrestech.mycalendarwidget.data.CalendarEvent
import com.fabiantorrestech.mycalendarwidget.widget.peek.PeekItem
import com.fabiantorrestech.mycalendarwidget.widget.peek.PeekItemKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

private val ZONE: ZoneId = ZoneId.of("America/Chicago")
private val TODAY: LocalDate = LocalDate.of(2026, 3, 10)

/** Epoch millis for [TODAY] + [dayOffset] at [hour]:[minute] in [ZONE]. */
private fun at(dayOffset: Long, hour: Int, minute: Int = 0): Long =
    TODAY.plusDays(dayOffset).atStartOfDay(ZONE).plusHours(hour.toLong())
        .plusMinutes(minute.toLong()).toInstant().toEpochMilli()

private fun event(id: Long, startMillis: Long, endMillis: Long, allDay: Boolean = false) = CalendarEvent(
    id = id,
    eventId = id,
    title = "e$id",
    dtStart = startMillis,
    dtEnd = endMillis,
    allDay = allDay,
    location = null,
    description = null,
    calendarId = 1L,
    calendarColor = 0xFF112233.toInt(),
    eventColor = null,
    selfAttendeeStatus = 0,
    organizer = null,
    eventTimezone = ZONE.id,
    hasAlarm = false,
    rrule = null,
    meetingUrl = null,
    mapsQuery = null
)

/** "H" for a header, the event id for an event: a page read at a glance. */
private fun shape(page: List<PeekItem>): List<String> =
    page.map { if (it.kind == PeekItemKind.HEADER) "H" else it.event!!.id.toString() }

class NotificationAgendaTest {

    // ---- pages() ----

    @Test
    fun `everything fits on one page`() {
        val byDay = mapOf(
            TODAY to listOf(event(1, at(0, 14), at(0, 15))),
            TODAY.plusDays(1) to listOf(event(2, at(1, 9), at(1, 10)))
        )
        val pages = NotificationAgenda.pages(byDay, TODAY, nowMillis = at(0, 8), rowsPerPage = 6)

        assertEquals(listOf(listOf("H", "1", "H", "2")), pages.map(::shape))
    }

    @Test
    fun `events that already ended today are left out`() {
        val byDay = mapOf(
            TODAY to listOf(event(1, at(0, 7), at(0, 8)), event(2, at(0, 14), at(0, 15)))
        )
        val pages = NotificationAgenda.pages(byDay, TODAY, nowMillis = at(0, 9), rowsPerPage = 6)

        assertEquals(listOf(listOf("H", "2")), pages.map(::shape))
    }

    @Test
    fun `an all-day event stays even though its UTC end has passed`() {
        val utc = ZoneId.of("UTC")
        val allDay = event(
            1,
            TODAY.atStartOfDay(utc).toInstant().toEpochMilli(),
            TODAY.plusDays(1).atStartOfDay(utc).toInstant().toEpochMilli(),
            allDay = true
        )
        // 20:00 in Chicago is after the all-day instance's UTC midnight end.
        val pages = NotificationAgenda.pages(mapOf(TODAY to listOf(allDay)), TODAY, at(0, 20), rowsPerPage = 6)

        assertEquals(listOf(listOf("H", "1")), pages.map(::shape))
    }

    @Test
    fun `a day whose events have all ended loses its header`() {
        val byDay = mapOf(
            TODAY to listOf(event(1, at(0, 7), at(0, 8))),
            TODAY.plusDays(1) to listOf(event(2, at(1, 9), at(1, 10)))
        )
        val pages = NotificationAgenda.pages(byDay, TODAY, nowMillis = at(0, 9), rowsPerPage = 6)

        assertEquals(listOf(listOf("H", "2")), pages.map(::shape))
        assertEquals(TODAY.plusDays(1), pages[0][0].date)
    }

    @Test
    fun `a day that runs onto the next page repeats its header there`() {
        val events = (1L..7L).map { event(it, at(0, 9 + it.toInt()), at(0, 10 + it.toInt())) }
        val pages = NotificationAgenda.pages(mapOf(TODAY to events), TODAY, nowMillis = at(0, 8), rowsPerPage = 4)

        assertEquals(
            listOf(listOf("H", "1", "2", "3"), listOf("H", "4", "5", "6"), listOf("H", "7")),
            pages.map(::shape)
        )
    }

    @Test
    fun `a page never ends on a header`() {
        val byDay = mapOf(
            TODAY to listOf(event(1, at(0, 10), at(0, 11)), event(2, at(0, 12), at(0, 13))),
            TODAY.plusDays(1) to listOf(event(3, at(1, 9), at(1, 10)))
        )
        // H 1 2 leaves one slot on a four-row page: too small for a header and its event.
        val pages = NotificationAgenda.pages(byDay, TODAY, nowMillis = at(0, 8), rowsPerPage = 4)

        assertEquals(listOf(listOf("H", "1", "2"), listOf("H", "3")), pages.map(::shape))
        pages.forEach { assertTrue(it.last().kind != PeekItemKind.HEADER) }
    }

    @Test
    fun `nothing coming up gives no pages`() {
        assertTrue(NotificationAgenda.pages(emptyMap(), TODAY, nowMillis = at(0, 8)).isEmpty())
    }

    // ---- dayRows() ----

    @Test
    fun `one day's events without headers`() {
        val byDay = mapOf(
            TODAY to listOf(event(1, at(0, 14), at(0, 15))),
            TODAY.plusDays(2) to listOf(event(2, at(2, 9), at(2, 10)), event(3, at(2, 11), at(2, 12)))
        )
        val day = NotificationAgenda.dayRows(byDay, TODAY.plusDays(2), nowMillis = at(0, 8), maxRows = 6)

        assertEquals(listOf("2", "3"), shape(day.rows))
        assertEquals(0, day.moreCount)
    }

    @Test
    fun `a busy day keeps its last row for the more line`() {
        val events = (1L..8L).map { event(it, at(1, 8 + it.toInt()), at(1, 9 + it.toInt())) }
        val day = NotificationAgenda.dayRows(mapOf(TODAY.plusDays(1) to events), TODAY.plusDays(1), at(0, 8), maxRows = 6)

        assertEquals(listOf("1", "2", "3", "4", "5"), shape(day.rows))
        assertEquals(3, day.moreCount)
    }

    @Test
    fun `today's day rows leave out what already ended`() {
        val byDay = mapOf(TODAY to listOf(event(1, at(0, 7), at(0, 8)), event(2, at(0, 14), at(0, 15))))
        val day = NotificationAgenda.dayRows(byDay, TODAY, nowMillis = at(0, 9), maxRows = 6)

        assertEquals(listOf("2"), shape(day.rows))
    }
}
