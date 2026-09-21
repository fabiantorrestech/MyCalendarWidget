package com.fabiantorrestech.mycalendarwidget.widget.peek

import com.fabiantorrestech.mycalendarwidget.data.CalendarEvent
import com.fabiantorrestech.mycalendarwidget.data.DensityPeekFormat
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfig
import java.time.LocalDate

/** The three row shapes the peek sheet's list draws (the top bar sits above the list). */
enum class PeekItemKind { HEADER, SEPARATOR, EVENT }

/**
 * One flattened row. [event] is set only on [PeekItemKind.EVENT]. Flattening headers,
 * separators and events into one list is what lets the sheet be a single `LazyColumn`
 * with stable `itemId`s rather than nested containers Glance would truncate at ten
 * children.
 */
data class PeekItem(
    val kind: PeekItemKind,
    val itemId: Long,
    val date: LocalDate,
    val event: CalendarEvent?,
    /** A timed event on today that has already ended: drawn faded, still listed. */
    val passed: Boolean = false
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
     * `Long.MIN_VALUE` sentinel is not available. The fixed ids sit immediately above
     * that floor instead: far enough from any real id — a negated epoch day is within a
     * few million of zero, an [eventItemId] is positive — that a collision is
     * impossible. (`+ 1` was the top bar's id before it moved above the list.)
     */
    const val EMPTY_ITEM_ID = Long.MIN_VALUE / 2 + 2

    /** The "loading" row shown until the calendar query has answered; see [EMPTY_ITEM_ID]. */
    const val LOADING_ITEM_ID = Long.MIN_VALUE / 2 + 3

    /** Bits of an [eventItemId] given to the epoch day: 2^20 days runs to the year 4840. */
    private const val EVENT_ITEM_DAY_BITS = 20

    /**
     * An event row's item id: the instance's `Instances._ID` in the high bits and the
     * row's epoch day in the low [EVENT_ITEM_DAY_BITS]. The instance id alone is not
     * enough — a multi-day event has one row per day it spans — and the pair is stable
     * across refreshes. It stays positive (an instance id would need 43 bits to reach
     * the sign), so it never meets the negated-epoch-day ids of the day markers.
     */
    internal fun eventItemId(instanceId: Long, date: LocalDate): Long =
        (instanceId shl EVENT_ITEM_DAY_BITS) or date.toEpochDay()

    /**
     * The config the peek queries with. The agenda list's display conveniences are all
     * wrong for a "what is coming up" sheet: [WidgetConfig.monthOffset] would follow the
     * user's month paging away from today, and
     * [WidgetConfig.showEmptyDays]/[WidgetConfig.alwaysShowToday] would inject days with
     * nothing in them. [WidgetConfig.showSpanningEventsEachDay] is forced *on* rather
     * than off: without it a multi-day event is keyed only under its true start day, so
     * a Mon–Fri all-day vacation viewed on Wednesday (or an overnight timed event) would
     * either be missing from the window or show up under a day that has already passed.
     * Turning it on keys the event on every day it covers instead, and [upcoming] lists
     * it under each of those from today on. Everything else — the calendar filter, the
     * keyword filter, `daysAheadToLoad` — is exactly what the user asked for and is left
     * alone.
     */
    fun peekQueryConfig(config: WidgetConfig): WidgetConfig = config.copy(
        monthOffset = 0,
        showSpanningEventsEachDay = true,
        showEmptyDays = false,
        alwaysShowToday = false
    )

    /**
     * Every event on today or later, flattened out of the by-day map and paired with the
     * day it belongs to. Today's events that have already ended stay on the list (the
     * sheet fades them; see [PeekItem.passed]) so the day reads whole; events on earlier
     * days are gone. A multi-day event is listed under every day it covers from today
     * on, each row labelled for that day by [PeekLabels.timeLabel].
     *
     * Every event is judged by the day it is keyed under, never by the clock. That
     * matters most for all-day events: an all-day instance's `dtStart`/`dtEnd` come back
     * from the provider in *UTC* midnight terms, so west of UTC today's all-day event
     * "ends" hours before the local day does.
     *
     * The ordering is fully specified rather than inherited from the map: day, then
     * all-day before timed, then start, then id. A map whose keys arrive in a different
     * order therefore produces the identical list, which is what makes the `LazyColumn`
     * item ids stable across refreshes.
     */
    fun upcoming(
        eventsByDay: Map<LocalDate, List<CalendarEvent>>,
        today: LocalDate
    ): List<Pair<LocalDate, CalendarEvent>> =
        eventsByDay.entries
            .sortedBy { it.key }
            .flatMap { (date, events) -> events.map { date to it } }
            // A multi-day instance is keyed under every day it spans (peekQueryConfig
            // forces showSpanningEventsEachDay on) and keeps one row per day, the way the
            // look-ahead bars draw it on each. A copy dated before today can only repeat
            // today's copy — the query window opens at today — so it goes.
            .filter { (date, _) -> !date.isBefore(today) }
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
     * sign: negated epoch days for day markers and the positive [eventItemId] (instance
     * and day) for event rows.
     */
    fun items(
        upcoming: List<Pair<LocalDate, CalendarEvent>>,
        format: DensityPeekFormat,
        nowMillis: Long
    ): List<PeekItem> {
        val items = ArrayList<PeekItem>(upcoming.size + 8)

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
            val passed = !event.allDay && event.dtEnd <= nowMillis
            items.add(PeekItem(PeekItemKind.EVENT, eventItemId(event.id, date), date, event, passed = passed))
        }
        return items
    }
}
