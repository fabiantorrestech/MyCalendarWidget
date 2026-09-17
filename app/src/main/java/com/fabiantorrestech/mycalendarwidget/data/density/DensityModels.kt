package com.fabiantorrestech.mycalendarwidget.data.density

import java.time.LocalDate

/**
 * A calendar instance stripped of every piece of text: the density pipeline sees only
 * geometry, calendar identity and colour, never the event's own words or attendees.
 */
data class RawInstance(
    val begin: Long,
    val end: Long,
    val allDay: Boolean,
    val calendarId: Long,
    val selfAttendeeStatus: Int,
    val status: Int,
    val availability: Int,
    val displayColor: Int,
    val calendarColor: Int
)

/** Half-open [startMillis, endMillis). */
data class BusyInterval(val startMillis: Long, val endMillis: Long) {
    val durationMinutes: Int get() = ((endMillis - startMillis) / 60_000L).toInt()
}

/** One event clamped to the strip window, never merged. */
data class EventBlock(
    val startMillis: Long,
    val endMillis: Long,
    val calendarId: Long,
    val colorInt: Int,
    /**
     * The event's per-day chronological index (its position in the day's own
     * start-then-end sort order), not a stable identity across days or renders. Used
     * only as [DensityCalculator.laneRects]'s tiebreaker when two events share a start
     * time, so the lane assignment is deterministic regardless of input order.
     */
    val id: Long
)

/** One drawable rect: a slice of an event within one overlap segment. */
data class LaneRect(
    val startMillis: Long,
    val endMillis: Long,
    val lane: Int,
    val depth: Int,
    val calendarId: Long,
    val colorInt: Int,
    val isEventEnd: Boolean
)

/** Everything the widget needs to draw one day. */
data class DayDensity(
    val date: LocalDate,
    /** Day-scoped busy event count; includes events outside the strip window. */
    val eventCount: Int,
    /**
     * True when the day carries an all-day instance that honours the enabled-calendar
     * set and excludes declined/cancelled instances, same as every other busy check here
     * — only the all-day exclusion itself is skipped (an all-day instance has no time
     * span to filter on).
     */
    val hasAllDay: Boolean,
    /** Merged over the whole day; drives day load and the headline qualifier. */
    val dayMerged: List<BusyInterval>,
    /** [dayMerged] clamped to the strip window; drives the Shape strip. */
    val stripMerged: List<BusyInterval>,
    /** One entry per busy event, clamped to the window; drives the Tonal/Detail strips. */
    val stripEvents: List<EventBlock>,
    /** Sum of the [dayMerged] durations. */
    val busyMinutes: Int,
    /** Unclamped end of every busy event on this day, for "left today" counting. */
    val busyEnds: List<Long>
)

/** One rendered snapshot: the featured day plus the lookahead strip. */
data class DensitySnapshot(
    val hasPermission: Boolean,
    /** Today, or tomorrow once the evening rollover has happened. */
    val featured: DayDensity,
    val featuredIsToday: Boolean,
    /** The N days *after* the featured day. */
    val lookahead: List<DayDensity>,
    val nowMillis: Long,
    /**
     * Ids of the calendars the provider marks visible, sorted ascending — the Tonal
     * strip's rank fallback when the user hasn't set an explicit calendar filter (see
     * `DensityCalendarSource.queryVisibleCalendarIds`). Defaults to empty so existing
     * snapshot construction (tests, other call sites) doesn't need updating; an empty list
     * here just means the tone-rank fallback falls further back to the per-day list.
     */
    val visibleCalendarIds: List<Long> = emptyList()
)

/** The two lines of text above the strip. */
data class DensityHeadline(
    val countText: String,
    val qualifierText: String,
    /** True when [countText] reads as a sentence ("Done today") rather than a number. */
    val countIsSentence: Boolean = false,
    /** The featured day as weekday and short date ("Sun (8/16)"): the day the count is about. */
    val dateText: String = ""
)
