package com.fabiantorrestech.mycalendarwidget.widget.peek

import com.fabiantorrestech.mycalendarwidget.data.CalendarEvent
import com.fabiantorrestech.mycalendarwidget.data.DensityPeekFormat
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfig
import java.time.LocalDate

/** The four row shapes the peek sheet draws. */
enum class PeekItemKind { TOP_BAR, HEADER, SEPARATOR, EVENT }

/**
 * One flattened row. [date] is null only on [PeekItemKind.TOP_BAR]; [event] is set only
 * on [PeekItemKind.EVENT]. Flattening headers, separators and events into one list is
 * what lets the sheet be a single `LazyColumn` with stable `itemId`s rather than nested
 * containers Glance would truncate at ten children.
 */
data class PeekItem(
    val kind: PeekItemKind,
    val itemId: Long,
    val date: LocalDate?,
    val event: CalendarEvent?
)

/**
 * The peek sheet's list, derived purely: no Android, no ambient clock, no repository —
 * `nowMillis` and `today` are always parameters (G2), so every ordering and cut-off rule
 * below is unit-testable.
 *
 * This file lives in `widget/peek/` and not `widget/density/` precisely because it
 * touches [CalendarEvent], which carries a title. The density pipeline stays
 * content-free by construction: nothing under `widget/density/` may import this file or
 * [CalendarEvent] (G1).
 */
object PeekList {

    /**
     * Glance reserves every item id at or below `Long.MIN_VALUE / 2` for the ids it
     * generates itself and throws `IllegalArgumentException` on anything lower ("You may
     * not specify item ids less than -4611686018427387904 in a Glance"), so the obvious
     * `Long.MIN_VALUE` sentinel is not available. These two sit immediately above that
     * floor instead: far enough from any real id — a negated epoch day is within a few
     * million of zero, an `Instances._ID` is positive — that a collision is impossible.
     */
    const val TOP_BAR_ITEM_ID = Long.MIN_VALUE / 2 + 1

    /** The "nothing coming up" row; see [TOP_BAR_ITEM_ID]. */
    const val EMPTY_ITEM_ID = Long.MIN_VALUE / 2 + 2

    /**
     * The config the peek queries with. The agenda list's display conveniences are all
     * wrong for a "what is coming up" sheet: [WidgetConfig.monthOffset] would follow the
     * user's month paging away from today, [WidgetConfig.showSpanningEventsEachDay]
     * would repeat a multi-day event on every day it covers (and each repeat carries the
     * same `id`, so they would be deduped anyway), and
     * [WidgetConfig.showEmptyDays]/[WidgetConfig.alwaysShowToday] would inject days with
     * nothing in them. Everything else — the calendar filter, the keyword filter,
     * `daysAheadToLoad` — is exactly what the user asked for and is left alone.
     */
    fun peekQueryConfig(config: WidgetConfig): WidgetConfig = config.copy(
        monthOffset = 0,
        showSpanningEventsEachDay = false,
        showEmptyDays = false,
        alwaysShowToday = false
    )

    /**
     * Every event still ahead of [nowMillis], flattened out of the by-day map and paired
     * with the day it belongs to.
     *
     * Two different cut-offs, because all-day events have two different clocks. A timed
     * event is over when its `dtEnd` has passed. An all-day instance's `dtStart`/`dtEnd`
     * come back from the provider in *UTC* midnight terms, so west of UTC today's all-day
     * event "ends" hours before the local day does — judging it by its date instead keeps
     * it on the sheet until the day itself rolls over.
     *
     * The ordering is fully specified rather than inherited from the map: day, then
     * all-day before timed, then start, then id. A map whose keys arrive in a different
     * order therefore produces the identical list, which is what makes the `LazyColumn`
     * item ids stable across refreshes.
     */
    fun upcoming(
        eventsByDay: Map<LocalDate, List<CalendarEvent>>,
        nowMillis: Long,
        today: LocalDate
    ): List<Pair<LocalDate, CalendarEvent>> =
        eventsByDay.entries
            .sortedBy { it.key }
            .flatMap { (date, events) -> events.map { date to it } }
            // The same instance can appear under two days when it spans them; the first
            // (earliest) day wins, so a running multi-day event stays anchored where it
            // started rather than jumping forward.
            .distinctBy { it.second.id }
            .filter { (date, event) ->
                if (event.allDay) !date.isBefore(today) else event.dtEnd > nowMillis
            }
            .sortedWith(
                compareBy(
                    { it.first },
                    { !it.second.allDay },
                    { it.second.dtStart },
                    { it.second.id }
                )
            )

    /**
     * Flattens [upcoming] into rows, with the day boundary marked the way [format] asks
     * for: a date header above each day in `GROUPED`, a hairline between days in `DATED`
     * (where every row already carries its own date pill, so a header would be noise).
     *
     * Item ids must be unique across the whole list, so the id spaces are kept apart by
     * sign: [TOP_BAR_ITEM_ID] for the top bar, negated epoch days for day markers, and
     * the event's own (positive) `Instances._ID` for event rows.
     */
    fun items(
        upcoming: List<Pair<LocalDate, CalendarEvent>>,
        format: DensityPeekFormat
    ): List<PeekItem> {
        val items = ArrayList<PeekItem>(upcoming.size + 8)
        items.add(PeekItem(PeekItemKind.TOP_BAR, TOP_BAR_ITEM_ID, null, null))

        var lastDate: LocalDate? = null
        upcoming.forEach { (date, event) ->
            if (date != lastDate) {
                when (format) {
                    DensityPeekFormat.GROUPED ->
                        items.add(PeekItem(PeekItemKind.HEADER, -date.toEpochDay(), date, null))
                    // No separator above the very first day: it would read as a rule
                    // under the top bar rather than as a divider between two days.
                    DensityPeekFormat.DATED ->
                        if (lastDate != null) {
                            items.add(
                                PeekItem(PeekItemKind.SEPARATOR, -date.toEpochDay(), date, null)
                            )
                        }
                }
                lastDate = date
            }
            items.add(PeekItem(PeekItemKind.EVENT, event.id, date, event))
        }
        return items
    }
}
