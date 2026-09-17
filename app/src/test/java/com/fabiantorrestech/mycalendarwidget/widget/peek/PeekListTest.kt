package com.fabiantorrestech.mycalendarwidget.widget.peek

import com.fabiantorrestech.mycalendarwidget.data.CalendarEvent
import com.fabiantorrestech.mycalendarwidget.data.DensityPeekFormat
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

private fun event(
    id: Long,
    startMillis: Long,
    endMillis: Long,
    allDay: Boolean = false,
    eventId: Long = id,
    title: String = "e$id"
) = CalendarEvent(
    id = id,
    eventId = eventId,
    title = title,
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

/** All-day instances come back from the provider in UTC midnight-to-midnight terms. */
private fun allDayEvent(id: Long, date: LocalDate) = event(
    id = id,
    startMillis = date.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli(),
    endMillis = date.plusDays(1).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli(),
    allDay = true
)

class PeekListTest {

    // ---- upcoming() ----

    @Test
    fun `duplicated id keeps the first day's copy`() {
        val e = event(1L, at(0, 9), at(0, 10))
        val eLater = event(1L, at(1, 9), at(1, 10))
        val map = mapOf(
            TODAY to listOf(e),
            TODAY.plusDays(1) to listOf(eLater)
        )
        val result = PeekList.upcoming(map, at(0, 8), TODAY)
        assertEquals(1, result.size)
        assertEquals(TODAY, result[0].first)
    }

    @Test
    fun `two instances of the same parent event both survive`() {
        val a = event(10L, at(0, 9), at(0, 10), eventId = 99L)
        val b = event(11L, at(1, 9), at(1, 10), eventId = 99L)
        val map = mapOf(TODAY to listOf(a), TODAY.plusDays(1) to listOf(b))
        val result = PeekList.upcoming(map, at(0, 8), TODAY)
        assertEquals(listOf(10L, 11L), result.map { it.second.id })
    }

    @Test
    fun `finished event is dropped and an in-progress one is kept`() {
        val done = event(1L, at(0, 7), at(0, 8))
        val running = event(2L, at(0, 8), at(0, 12))
        val map = mapOf(TODAY to listOf(done, running))
        val result = PeekList.upcoming(map, at(0, 9), TODAY)
        assertEquals(listOf(2L), result.map { it.second.id })
    }

    @Test
    fun `event ending exactly now is dropped`() {
        val e = event(1L, at(0, 8), at(0, 9))
        val map = mapOf(TODAY to listOf(e))
        assertTrue(PeekList.upcoming(map, at(0, 9), TODAY).isEmpty())
    }

    @Test
    fun `yesterday's all-day is dropped, today's and tomorrow's are kept`() {
        val yesterday = allDayEvent(1L, TODAY.minusDays(1))
        // Today's all-day in UTC terms already ended by a late local "now" — the date,
        // not the UTC instant, is what decides an all-day event's fate.
        val today = allDayEvent(2L, TODAY)
        val tomorrow = allDayEvent(3L, TODAY.plusDays(1))
        val map = mapOf(
            TODAY.minusDays(1) to listOf(yesterday),
            TODAY to listOf(today),
            TODAY.plusDays(1) to listOf(tomorrow)
        )
        val result = PeekList.upcoming(map, at(0, 23), TODAY)
        assertEquals(listOf(2L, 3L), result.map { it.second.id })
    }

    @Test
    fun `all-day sorts before timed events on the same day`() {
        val timed = event(1L, at(0, 9), at(0, 10))
        val allDay = allDayEvent(2L, TODAY)
        val map = mapOf(TODAY to listOf(timed, allDay))
        val result = PeekList.upcoming(map, at(0, 8), TODAY)
        assertEquals(listOf(2L, 1L), result.map { it.second.id })
    }

    @Test
    fun `order is stable under shuffled map insertion order`() {
        val d0 = event(1L, at(0, 9), at(0, 10))
        val d1 = event(2L, at(1, 9), at(1, 10))
        val d2 = event(3L, at(2, 9), at(2, 10))
        val inOrder = linkedMapOf(
            TODAY to listOf(d0),
            TODAY.plusDays(1) to listOf(d1),
            TODAY.plusDays(2) to listOf(d2)
        )
        val shuffled = linkedMapOf(
            TODAY.plusDays(2) to listOf(d2),
            TODAY to listOf(d0),
            TODAY.plusDays(1) to listOf(d1)
        )
        val now = at(0, 8)
        assertEquals(
            PeekList.upcoming(inOrder, now, TODAY),
            PeekList.upcoming(shuffled, now, TODAY)
        )
        assertEquals(listOf(1L, 2L, 3L), PeekList.upcoming(shuffled, now, TODAY).map { it.second.id })
    }

    @Test
    fun `equal start times break the tie on id`() {
        val b = event(20L, at(0, 9), at(0, 10))
        val a = event(5L, at(0, 9), at(0, 11))
        val map = mapOf(TODAY to listOf(b, a))
        val result = PeekList.upcoming(map, at(0, 8), TODAY)
        assertEquals(listOf(5L, 20L), result.map { it.second.id })
    }

    @Test
    fun `all-day event spanning three days appears once under today, not the earliest day`() {
        // Same instance (id 1) keyed under Mon, Tue, Wed the way
        // showSpanningEventsEachDay = true keys a repository result; today is Wed (TODAY).
        val spanStart = TODAY.minusDays(2).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        val spanEnd = TODAY.plusDays(1).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        val e = event(1L, spanStart, spanEnd, allDay = true)
        val map = mapOf(
            TODAY.minusDays(2) to listOf(e),
            TODAY.minusDays(1) to listOf(e),
            TODAY to listOf(e)
        )
        val result = PeekList.upcoming(map, at(0, 8), TODAY)
        assertEquals(1, result.size)
        assertEquals(TODAY, result[0].first)
    }

    @Test
    fun `overnight timed event keyed under both days appears once under today`() {
        // 22:00 yesterday to 06:00 today, keyed under both days it spans; "now" is 02:00
        // today, so the event is still in progress.
        val e = event(2L, at(-1, 22), at(0, 6))
        val map = mapOf(
            TODAY.minusDays(1) to listOf(e),
            TODAY to listOf(e)
        )
        val result = PeekList.upcoming(map, at(0, 2), TODAY)
        assertEquals(1, result.size)
        assertEquals(TODAY, result[0].first)
    }

    // ---- items() ----

    @Test
    fun `grouped starts with a header and has one header per day change`() {
        val items = PeekList.items(twoDaysUpcoming(), DensityPeekFormat.GROUPED)
        assertEquals(PeekItemKind.HEADER, items.first().kind)
        assertEquals(
            listOf(TODAY, TODAY.plusDays(1)),
            items.filter { it.kind == PeekItemKind.HEADER }.map { it.date }
        )
        assertTrue(items.none { it.kind == PeekItemKind.SEPARATOR })
        assertEquals(3, items.count { it.kind == PeekItemKind.EVENT })
    }

    @Test
    fun `grouped header itemId is the negated epoch day`() {
        val items = PeekList.items(twoDaysUpcoming(), DensityPeekFormat.GROUPED)
        val header = items.first { it.kind == PeekItemKind.HEADER }
        assertEquals(-TODAY.toEpochDay(), header.itemId)
    }

    @Test
    fun `dated separates days but never before the first day`() {
        val items = PeekList.items(twoDaysUpcoming(), DensityPeekFormat.DATED)
        assertEquals(PeekItemKind.EVENT, items.first().kind)
        assertTrue(items.none { it.kind == PeekItemKind.HEADER })
        val separators = items.filter { it.kind == PeekItemKind.SEPARATOR }
        assertEquals(1, separators.size)
        assertEquals(TODAY.plusDays(1), separators[0].date)
        assertEquals(-TODAY.plusDays(1).toEpochDay(), separators[0].itemId)
        // The separator sits between the last of day one and the first of day two.
        assertEquals(PeekItemKind.SEPARATOR, items[2].kind)
    }

    @Test
    fun `event items carry the event and its id`() {
        val items = PeekList.items(twoDaysUpcoming(), DensityPeekFormat.DATED)
        val events = items.filter { it.kind == PeekItemKind.EVENT }
        assertEquals(listOf(1L, 2L, 3L), events.map { it.itemId })
        assertEquals(listOf(1L, 2L, 3L), events.map { it.event?.id })
        assertEquals(listOf(TODAY, TODAY, TODAY.plusDays(1)), events.map { it.date })
    }

    @Test
    fun `every itemId is unique and stable across two calls`() {
        DensityPeekFormat.entries.forEach { format ->
            val first = PeekList.items(twoDaysUpcoming(), format)
            val second = PeekList.items(twoDaysUpcoming(), format)
            val ids = first.map { it.itemId }
            assertEquals("$format has duplicate itemIds", ids.size, ids.distinct().size)
            assertEquals(ids, second.map { it.itemId })
        }
    }

    @Test
    fun `fixed item ids clear Glance's reserved range`() {
        // Glance throws on any item id at or below Long.MIN_VALUE / 2.
        val floor = Long.MIN_VALUE / 2
        assertTrue(PeekList.EMPTY_ITEM_ID > floor)
        assertTrue(PeekList.LOADING_ITEM_ID > floor)
        assertTrue(PeekList.LOADING_ITEM_ID != PeekList.EMPTY_ITEM_ID)
    }

    @Test
    fun `empty upcoming yields no items`() {
        DensityPeekFormat.entries.forEach { format ->
            assertTrue(PeekList.items(emptyList(), format).isEmpty())
        }
    }

    // ---- peekQueryConfig() ----

    @Test
    fun `peek query config neutralises the agenda-only flags and enables spanning`() {
        val config = WidgetConfig(
            monthOffset = 3,
            showSpanningEventsEachDay = false,
            showEmptyDays = true,
            alwaysShowToday = true,
            daysAheadToLoad = 14
        )
        val peek = PeekList.peekQueryConfig(config)
        assertEquals(0, peek.monthOffset)
        // Spanning must be forced on so a multi-day event in progress is keyed onto
        // today (and every other day it covers) rather than only its true start day.
        assertTrue(peek.showSpanningEventsEachDay)
        assertFalse(peek.showEmptyDays)
        assertFalse(peek.alwaysShowToday)
        // Everything else is left exactly as the user configured it.
        assertEquals(14, peek.daysAheadToLoad)
        assertEquals(config, peek.copy(
            monthOffset = 3,
            showSpanningEventsEachDay = false,
            showEmptyDays = true,
            alwaysShowToday = true
        ))
    }

    /** Two events today, one tomorrow — the shape every `items` case is checked against. */
    private fun twoDaysUpcoming(): List<Pair<LocalDate, CalendarEvent>> = listOf(
        TODAY to event(1L, at(0, 9), at(0, 10)),
        TODAY to event(2L, at(0, 11), at(0, 12)),
        TODAY.plusDays(1) to event(3L, at(1, 9), at(1, 10))
    )
}
