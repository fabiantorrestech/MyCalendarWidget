package com.fabiantorrestech.mycalendarwidget.notification

import com.fabiantorrestech.mycalendarwidget.data.CalendarEvent
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

class NotificationAgendaTest {

    private fun kinds(agenda: AgendaRows) = agenda.rows.map { it.kind }

    @Test
    fun `everything fits - headers and events, nothing more`() {
        val byDay = mapOf(
            TODAY to listOf(event(1, at(0, 14), at(0, 15))),
            TODAY.plusDays(1) to listOf(event(2, at(1, 9), at(1, 10)))
        )
        val agenda = NotificationAgenda.rows(byDay, TODAY, nowMillis = at(0, 8), maxRows = 7)

        assertEquals(
            listOf(PeekItemKind.HEADER, PeekItemKind.EVENT, PeekItemKind.HEADER, PeekItemKind.EVENT),
            kinds(agenda)
        )
        assertEquals(0, agenda.moreCount)
    }

    @Test
    fun `events that already ended today are left out`() {
        val byDay = mapOf(
            TODAY to listOf(
                event(1, at(0, 7), at(0, 8)),
                event(2, at(0, 14), at(0, 15))
            )
        )
        val agenda = NotificationAgenda.rows(byDay, TODAY, nowMillis = at(0, 9), maxRows = 7)

        assertEquals(listOf(2L), agenda.rows.mapNotNull { it.event?.id })
        assertEquals(0, agenda.moreCount)
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
        val agenda = NotificationAgenda.rows(mapOf(TODAY to listOf(allDay)), TODAY, at(0, 20), maxRows = 7)

        assertEquals(listOf(1L), agenda.rows.mapNotNull { it.event?.id })
    }

    @Test
    fun `a day whose events have all ended loses its header`() {
        val byDay = mapOf(
            TODAY to listOf(event(1, at(0, 7), at(0, 8))),
            TODAY.plusDays(1) to listOf(event(2, at(1, 9), at(1, 10)))
        )
        val agenda = NotificationAgenda.rows(byDay, TODAY, nowMillis = at(0, 9), maxRows = 7)

        assertEquals(listOf(PeekItemKind.HEADER, PeekItemKind.EVENT), kinds(agenda))
        assertEquals(TODAY.plusDays(1), agenda.rows.first().date)
    }

    @Test
    fun `overflow keeps one slot for the more line and counts what was cut`() {
        val events = (1L..8L).map { event(it, at(0, 9 + it.toInt()), at(0, 10 + it.toInt())) }
        val agenda = NotificationAgenda.rows(mapOf(TODAY to events), TODAY, nowMillis = at(0, 8), maxRows = 5)

        // Header + 3 events = 4 rows, leaving the fifth for "+5 more".
        assertEquals(4, agenda.rows.size)
        assertEquals(5, agenda.moreCount)
    }

    @Test
    fun `a cut never leaves a header with no event under it`() {
        val byDay = mapOf(
            TODAY to listOf(event(1, at(0, 10), at(0, 11)), event(2, at(0, 12), at(0, 13))),
            TODAY.plusDays(1) to listOf(event(3, at(1, 9), at(1, 10)))
        )
        // Rows would be H E E H E (5); with maxRows 4 the cap is 3 slots: H E E.
        val agenda = NotificationAgenda.rows(byDay, TODAY, nowMillis = at(0, 8), maxRows = 4)

        assertEquals(listOf(PeekItemKind.HEADER, PeekItemKind.EVENT, PeekItemKind.EVENT), kinds(agenda))
        assertEquals(1, agenda.moreCount)
        assertTrue(agenda.rows.last().kind != PeekItemKind.HEADER)
    }

    @Test
    fun `nothing coming up gives no rows`() {
        val agenda = NotificationAgenda.rows(emptyMap(), TODAY, nowMillis = at(0, 8), maxRows = 7)

        assertTrue(agenda.rows.isEmpty())
        assertEquals(0, agenda.moreCount)
    }
}
