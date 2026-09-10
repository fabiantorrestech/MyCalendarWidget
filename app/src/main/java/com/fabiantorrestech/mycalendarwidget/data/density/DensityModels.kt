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
    /** True when the day carries an all-day instance, observed before any filtering. */
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
    val nowMillis: Long
)

/** The two lines of text above the strip. */
data class DensityHeadline(
    val countText: String,
    val qualifierText: String,
    /** True when [countText] reads as a sentence ("Done today") rather than a number. */
    val countIsSentence: Boolean = false
)
