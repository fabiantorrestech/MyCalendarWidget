package com.fabiantorrestech.mycalendarwidget.widget.peek

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
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
}
