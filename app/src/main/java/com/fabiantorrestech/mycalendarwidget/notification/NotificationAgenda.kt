package com.fabiantorrestech.mycalendarwidget.notification

import com.fabiantorrestech.mycalendarwidget.data.CalendarEvent
import com.fabiantorrestech.mycalendarwidget.data.DensityPeekFormat
import com.fabiantorrestech.mycalendarwidget.widget.peek.PeekItem
import com.fabiantorrestech.mycalendarwidget.widget.peek.PeekItemKind
import com.fabiantorrestech.mycalendarwidget.widget.peek.PeekList
import java.time.LocalDate

/** One day's rows in the expanded notification, and how many of its events did not fit. */
data class AgendaRows(val rows: List<PeekItem>, val moreCount: Int)

/**
 * The expanded notification's agenda, derived purely like [PeekList] (no Android, no
 * ambient clock), so the paging rules are unit-tested.
 *
 * A notification cannot scroll, so the list is cut into fixed-size pages the ‹ › arrows
 * move through. It spends no rows on the past: a timed event that has already ended is
 * left out rather than faded, and a day left with nothing upcoming loses its header
 * too. Days are always marked with headers (the peek's GROUPED format); the DATED
 * format's date pill would not fit beside the time column at notification width.
 */
object NotificationAgenda {

    /**
     * Rows per page under the headline, strip and axis, leaving room for the arrows'
     * row inside Android's 252dp expanded notification.
     */
    const val ROWS_PER_PAGE = 6

    /**
     * The upcoming list in the peek's order (day, all-day first, then start), cut into
     * pages of at most [rowsPerPage] rows. A day that runs onto the next page repeats its
     * header there, and a header is only placed where its first event fits under it, so
     * no page ends on one. Empty when nothing is coming up.
     */
    fun pages(
        eventsByDay: Map<LocalDate, List<CalendarEvent>>,
        today: LocalDate,
        nowMillis: Long,
        rowsPerPage: Int = ROWS_PER_PAGE
    ): List<List<PeekItem>> {
        require(rowsPerPage >= 2) { "A page needs room for a header and an event" }
        val all = PeekList.items(upcoming(eventsByDay, today, nowMillis), DensityPeekFormat.GROUPED, nowMillis)

        val pages = ArrayList<List<PeekItem>>()
        var page = ArrayList<PeekItem>(rowsPerPage)
        var header: PeekItem? = null
        all.forEach { item ->
            when (item.kind) {
                PeekItemKind.HEADER -> {
                    header = item
                    if (page.size + 2 > rowsPerPage) {
                        pages.add(page)
                        page = ArrayList(rowsPerPage)
                    }
                    page.add(item)
                }
                else -> {
                    if (page.size + 1 > rowsPerPage) {
                        pages.add(page)
                        page = ArrayList(rowsPerPage)
                        header?.let { page.add(it) }
                    }
                    page.add(item)
                }
            }
        }
        if (page.isNotEmpty()) pages.add(page)
        return pages
    }

    /**
     * One day's events, no headers (the headline above already names the day). When
     * they do not all fit, the last of [maxRows] is kept for a "+N more" line.
     */
    fun dayRows(
        eventsByDay: Map<LocalDate, List<CalendarEvent>>,
        date: LocalDate,
        nowMillis: Long,
        maxRows: Int = ROWS_PER_PAGE
    ): AgendaRows {
        val events = PeekList.items(
            upcoming(eventsByDay.filterKeys { it == date }, date, nowMillis),
            DensityPeekFormat.GROUPED,
            nowMillis
        ).filter { it.kind == PeekItemKind.EVENT }

        if (events.size <= maxRows) return AgendaRows(events, moreCount = 0)
        val shown = events.take(maxOf(0, maxRows - 1))
        return AgendaRows(shown, moreCount = events.size - shown.size)
    }

    /** The peek's upcoming list without the timed events that have already ended. */
    private fun upcoming(
        eventsByDay: Map<LocalDate, List<CalendarEvent>>,
        today: LocalDate,
        nowMillis: Long
    ): List<Pair<LocalDate, CalendarEvent>> =
        PeekList.upcoming(eventsByDay, today)
            .filterNot { (_, event) -> !event.allDay && event.dtEnd <= nowMillis }
}
