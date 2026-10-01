package com.fabiantorrestech.mycalendarwidget.notification

import com.fabiantorrestech.mycalendarwidget.data.CalendarEvent
import com.fabiantorrestech.mycalendarwidget.data.DensityPeekFormat
import com.fabiantorrestech.mycalendarwidget.widget.peek.PeekItem
import com.fabiantorrestech.mycalendarwidget.widget.peek.PeekItemKind
import com.fabiantorrestech.mycalendarwidget.widget.peek.PeekList
import java.time.LocalDate

/** The rows the expanded notification draws, and how many events did not fit. */
data class AgendaRows(val rows: List<PeekItem>, val moreCount: Int)

/**
 * The expanded notification's agenda, derived purely like [PeekList] (no Android, no
 * ambient clock), so the cut-off rules are unit-tested.
 *
 * A notification cannot scroll, so unlike the peek it has a fixed number of rows. It
 * spends none on the past: a timed event that has already ended is left out rather
 * than faded, and a day left with nothing upcoming loses its header too. Days are
 * always marked with headers (the peek's GROUPED format); the DATED format's date pill
 * would not fit beside the time column at notification width.
 */
object NotificationAgenda {

    /**
     * Rows that fit under the headline, strip and axis in Android's 252dp expanded
     * notification, counting the "+N more" line.
     */
    const val MAX_ROWS = 7

    /**
     * Up to [maxRows] rows in the peek's order (day, all-day first, then start). When
     * everything does not fit, the last slot is kept for the "+N more" line, and a day
     * header is never left last with no event under it.
     */
    fun rows(
        eventsByDay: Map<LocalDate, List<CalendarEvent>>,
        today: LocalDate,
        nowMillis: Long,
        maxRows: Int = MAX_ROWS
    ): AgendaRows {
        val upcoming = PeekList.upcoming(eventsByDay, today)
            .filterNot { (_, event) -> !event.allDay && event.dtEnd <= nowMillis }
        val all = PeekList.items(upcoming, DensityPeekFormat.GROUPED, nowMillis)
        val totalEvents = all.count { it.kind == PeekItemKind.EVENT }

        if (all.size <= maxRows) return AgendaRows(all, moreCount = 0)

        val kept = all.take(maxOf(0, maxRows - 1)).toMutableList()
        while (kept.lastOrNull()?.kind == PeekItemKind.HEADER) kept.removeAt(kept.lastIndex)
        val shownEvents = kept.count { it.kind == PeekItemKind.EVENT }
        return AgendaRows(kept, moreCount = totalEvents - shownEvents)
    }
}
