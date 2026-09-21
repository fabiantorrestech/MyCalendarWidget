package com.fabiantorrestech.mycalendarwidget.ui.sections

import com.fabiantorrestech.mycalendarwidget.data.CalendarInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Which calendars the Tonal tone list offers: only the ones the widget draws. */
class TonalCalendarListTest {

    private fun calendar(id: Long, name: String, visible: Boolean = true) = CalendarInfo(
        id = id,
        displayName = name,
        accountName = "",
        color = 0,
        enabled = true,
        visible = visible
    )

    /** In display-name order, the way CalendarRepository.getCalendars returns them. */
    private val calendars = listOf(
        calendar(7L, "Birthdays", visible = false),
        calendar(3L, "Home"),
        calendar(9L, "Holidays", visible = false),
        calendar(5L, "Work")
    )

    private fun shownIds(enabled: Set<Long>) = TonalCalendarList.shown(calendars, enabled).map { it.id }

    @Test
    fun explicitFilterShowsOnlyThoseCalendarsInListOrder() {
        // Birthdays is hidden in the calendar app but was enabled by hand: it is drawn.
        assertEquals(listOf(7L, 5L), shownIds(setOf(5L, 7L)))
    }

    @Test
    fun noFilterShowsOnlyTheCalendarsTheCalendarAppShows() {
        assertEquals(listOf(3L, 5L), shownIds(emptySet()))
    }

    @Test
    fun aFilterIdWithNoCalendarIsIgnored() {
        assertEquals(listOf(3L), shownIds(setOf(3L, 42L)))
    }

    @Test
    fun nothingDrawnShowsNothing() {
        val allHidden = calendars.map { it.copy(visible = false) }
        assertTrue(TonalCalendarList.shown(allHidden, emptySet()).isEmpty())
    }

    @Test
    fun rankedIdsAreTheWidgetsRankOrder() {
        assertEquals(listOf(5L, 7L), TonalCalendarList.rankedIds(calendars, setOf(7L, 5L)))
        assertEquals(listOf(3L, 5L), TonalCalendarList.rankedIds(calendars, emptySet()))
    }
}
