package com.fabiantorrestech.mycalendarwidget.notification

import com.fabiantorrestech.mycalendarwidget.data.density.BusyInterval
import com.fabiantorrestech.mycalendarwidget.data.density.DayDensity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

private val ZONE: ZoneId = ZoneId.of("America/Chicago")
private val DATE: LocalDate = LocalDate.of(2026, 10, 3) // a Saturday

private fun at(hour: Int, minute: Int = 0): Long =
    DATE.atStartOfDay(ZONE).plusHours(hour.toLong()).plusMinutes(minute.toLong()).toInstant().toEpochMilli()

private fun day(count: Int, merged: List<BusyInterval>) = DayDensity(
    date = DATE,
    eventCount = count,
    hasAllDay = false,
    dayMerged = merged,
    stripMerged = merged,
    stripEvents = emptyList(),
    busyMinutes = merged.sumOf { it.durationMinutes },
    busyEnds = merged.map { it.endMillis }
)

class NotificationDayHeadlineTest {

    @Test
    fun `a free day says nothing planned`() {
        val headline = NotificationDayHeadline.headline(day(0, emptyList()), ZONE, use24Hour = false, Locale.US)

        assertEquals("Nothing planned", headline.countText)
        assertEquals("", headline.qualifierText)
        assertTrue(headline.countIsSentence)
        assertEquals("Sat (10/3)", headline.dateText)
    }

    @Test
    fun `one event is singular and names its start`() {
        val headline = NotificationDayHeadline.headline(
            day(1, listOf(BusyInterval(at(9, 30), at(10, 30)))), ZONE, use24Hour = false, Locale.US
        )

        assertEquals("1 event", headline.countText)
        assertEquals("first at 9:30", headline.qualifierText)
    }

    @Test
    fun `several events are plural and name the first start`() {
        val headline = NotificationDayHeadline.headline(
            day(3, listOf(BusyInterval(at(8), at(9)), BusyInterval(at(13), at(15)))), ZONE, use24Hour = true, Locale.US
        )

        assertEquals("3 events", headline.countText)
        assertEquals("first at 08:00", headline.qualifierText)
    }

    @Test
    fun `an all-day-only day has a count but no start`() {
        val headline = NotificationDayHeadline.headline(day(1, emptyList()), ZONE, use24Hour = false, Locale.US)

        assertEquals("1 event", headline.countText)
        assertEquals("", headline.qualifierText)
    }
}
