package com.fabiantorrestech.mycalendarwidget.data.density

import com.fabiantorrestech.mycalendarwidget.data.DensityCountMode
import com.fabiantorrestech.mycalendarwidget.data.TimeFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/**
 * The computational core of the density widget: filtering, merging, windowing, lane
 * layout and headline text. Pure Kotlin — no Android, no ambient clock or zone. Every
 * "now", zone and locale arrives as a parameter.
 *
 * Time ranges are returned as a [LongRange] built with `start until end`: `first` is the
 * inclusive start in epoch millis and `last` is the *exclusive* end minus one, so the
 * exclusive end is `last + 1`. The same convention applies to [dayBounds] and
 * [windowBounds].
 */
object DensityCalculator {

    /** Longest gap still described in minutes rather than as a clock time. */
    private const val QUALIFIER_MINUTES_THRESHOLD = 90

    /**
     * True when an instance should be drawn as busy time. An empty [enabledCalendarIds]
     * means every calendar is enabled — the convention the rest of the app already uses.
     */
    fun isBusy(r: RawInstance, enabledCalendarIds: Set<Long>): Boolean {
        if (r.allDay) return false
        if (r.selfAttendeeStatus == DensityConstants.ATTENDEE_STATUS_DECLINED) return false
        if (r.status == DensityConstants.EVENT_STATUS_CANCELED) return false
        if (r.availability == DensityConstants.AVAILABILITY_FREE) return false
        if (enabledCalendarIds.isNotEmpty() && r.calendarId !in enabledCalendarIds) return false
        return true
    }

    /**
     * Sorts by start and merges overlapping *and* touching intervals into one block;
     * zero-length and negative-length intervals are dropped.
     */
    fun mergeIntervals(intervals: List<BusyInterval>): List<BusyInterval> {
        val sorted = intervals
            .filter { it.endMillis > it.startMillis }
            .sortedWith(compareBy({ it.startMillis }, { it.endMillis }))
        if (sorted.isEmpty()) return emptyList()

        val merged = ArrayList<BusyInterval>(sorted.size)
        var current = sorted[0]
        for (i in 1 until sorted.size) {
            val next = sorted[i]
            current = if (next.startMillis <= current.endMillis) {
                // Touching counts as overlapping, so 10:00-11:00 and 11:00-12:00 become one block.
                BusyInterval(current.startMillis, maxOf(current.endMillis, next.endMillis))
            } else {
                merged.add(current)
                next
            }
        }
        merged.add(current)
        return merged
    }

    /** Trims each interval to [startMillis, endMillis) and drops anything left empty. */
    fun clampTo(intervals: List<BusyInterval>, startMillis: Long, endMillis: Long): List<BusyInterval> =
        intervals.mapNotNull { interval ->
            val start = maxOf(interval.startMillis, startMillis)
            val end = minOf(interval.endMillis, endMillis)
            if (end > start) BusyInterval(start, end) else null
        }

    /**
     * Local midnight to local midnight. `last + 1` is the exclusive end, so the range
     * spans 23 or 25 hours on DST transition days.
     */
    fun dayBounds(date: LocalDate, zone: ZoneId): LongRange {
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start until end
    }

    /**
     * The strip window as minutes of the *local* day: minute 480 is 08:00 local whatever
     * the offset does that day. The arithmetic runs on the local time-line and only then
     * resolves to an instant, so a DST shift moves the instants without moving the wall
     * clock. `last + 1` is the exclusive end.
     */
    fun windowBounds(date: LocalDate, startMinute: Int, endMinute: Int, zone: ZoneId): LongRange {
        val midnight = date.atStartOfDay()
        val start = midnight.plusMinutes(startMinute.toLong()).atZone(zone).toInstant().toEpochMilli()
        val end = midnight.plusMinutes(endMinute.toLong()).atZone(zone).toInstant().toEpochMilli()
        return start until end
    }

    /** The event's own colour when it overrides its calendar's, otherwise the calendar colour. */
    fun resolveColor(r: RawInstance): Int =
        if (r.displayColor != r.calendarColor) r.displayColor else r.calendarColor

    /**
     * Builds one day from raw instances. [raw] may contain instances from other days;
     * only those intersecting this day's bounds are considered. Instances straddling
     * midnight are clamped to the day for busy time, but [DayDensity.busyEnds] keeps the
     * unclamped end so "left today" counting stays honest.
     */
    fun buildDay(
        date: LocalDate,
        raw: List<RawInstance>,
        enabledCalendarIds: Set<Long>,
        windowStartMinutes: Int,
        windowEndMinutes: Int,
        zone: ZoneId
    ): DayDensity {
        val dayRange = dayBounds(date, zone)
        val dayStart = dayRange.first
        val dayEnd = dayRange.last + 1

        val onDay = raw.filter { it.end > dayStart && it.begin < dayEnd }
        // hasAllDay still honours the enabled-calendar set and excludes declined/canceled
        // instances; only the all-day-ness itself is not filtered out here.
        val hasAllDay = onDay.any {
            it.allDay &&
                (enabledCalendarIds.isEmpty() || it.calendarId in enabledCalendarIds) &&
                it.selfAttendeeStatus != DensityConstants.ATTENDEE_STATUS_DECLINED &&
                it.status != DensityConstants.EVENT_STATUS_CANCELED
        }

        val busy = onDay
            .filter { isBusy(it, enabledCalendarIds) }
            // Zero-length and negative-length instances are excluded entirely: they pass
            // isBusy but must not inflate eventCount/busyEnds while contributing no interval.
            .filter { it.end > it.begin }
            .sortedWith(compareBy({ it.begin }, { it.end }))

        val dayMerged = mergeIntervals(
            busy.map { BusyInterval(maxOf(it.begin, dayStart), minOf(it.end, dayEnd)) }
        )
        val busyMinutes = dayMerged.sumOf { it.durationMinutes }

        val windowRange = windowBounds(date, windowStartMinutes, windowEndMinutes, zone)
        val windowStart = windowRange.first
        val windowEnd = windowRange.last + 1

        // Ids follow the day's own chronological order, which is also the lane ranking key.
        val stripEvents = busy.mapIndexedNotNull { index, instance ->
            val start = maxOf(instance.begin, windowStart)
            val end = minOf(instance.end, windowEnd)
            if (end > start) {
                EventBlock(start, end, instance.calendarId, resolveColor(instance), index.toLong())
            } else {
                null
            }
        }

        return DayDensity(
            date = date,
            eventCount = busy.size,
            hasAllDay = hasAllDay,
            dayMerged = dayMerged,
            stripMerged = clampTo(dayMerged, windowStart, windowEnd),
            stripEvents = stripEvents,
            busyMinutes = busyMinutes,
            busyEnds = busy.map { it.end }
        )
    }

    /** Busy time as a fraction of a full day's baseline, clamped to 0f..1f. */
    fun dayLoad(busyMinutes: Int, baselineMinutes: Int): Float {
        if (baselineMinutes <= 0) return 0f
        return (busyMinutes.toFloat() / baselineMinutes.toFloat()).coerceIn(0f, 1f)
    }

    /** Events still to come, counting one that is in progress right now. */
    fun remainingCount(day: DayDensity, nowMillis: Long): Int =
        day.busyEnds.count { it > nowMillis }

    /** Where "now" sits inside the window as 0f..1f, or null when it is outside. */
    fun nowFraction(nowMillis: Long, windowStart: Long, windowEnd: Long): Float? {
        if (nowMillis < windowStart || nowMillis > windowEnd) return null
        val span = windowEnd - windowStart
        if (span <= 0L) return 0f
        return (nowMillis - windowStart).toFloat() / span.toFloat()
    }

    /**
     * Slices [events] at every start/end boundary and assigns a lane per slice. Within a
     * segment the active events are ranked by (start, id) so the layout is stable however
     * the input is ordered; beyond [maxLanes] the extra events share the last lane.
     */
    fun laneRects(events: List<EventBlock>, maxLanes: Int = 3): List<LaneRect> {
        if (events.isEmpty()) return emptyList()

        val boundaries = sortedSetOf<Long>()
        for (event in events) {
            boundaries.add(event.startMillis)
            boundaries.add(event.endMillis)
        }
        val edges = boundaries.toList()

        val rects = ArrayList<LaneRect>()
        for (i in 0 until edges.size - 1) {
            val segmentStart = edges[i]
            val segmentEnd = edges[i + 1]
            val active = events
                .filter { it.startMillis <= segmentStart && it.endMillis >= segmentEnd }
                .sortedWith(compareBy({ it.startMillis }, { it.id }))
            if (active.isEmpty()) continue

            val depth = minOf(active.size, maxLanes)
            active.forEachIndexed { index, event ->
                rects.add(
                    LaneRect(
                        startMillis = segmentStart,
                        endMillis = segmentEnd,
                        lane = minOf(index, depth - 1),
                        depth = depth,
                        calendarId = event.calendarId,
                        colorInt = event.colorInt,
                        isEventEnd = segmentEnd == event.endMillis
                    )
                )
            }
        }
        return rects
    }

    /** The two lines of headline text for the featured day. */
    fun headline(
        featured: DayDensity,
        featuredIsToday: Boolean,
        nowMillis: Long,
        rolloverHour: Int,
        countMode: DensityCountMode,
        zone: ZoneId,
        use24Hour: Boolean,
        locale: Locale
    ): DensityHeadline {
        if (!featuredIsToday) {
            if (featured.eventCount == 0) return DensityHeadline("Nothing tomorrow", "")
            val first = featured.dayMerged.firstOrNull()
            val qualifier = if (first == null) {
                ""
            } else {
                "first at " + TimeFormat.clockNoSuffix(first.startMillis, zone, use24Hour)
            }
            return DensityHeadline("${featured.eventCount} tomorrow", qualifier)
        }

        if (featured.eventCount == 0) return DensityHeadline("Nothing today", "")

        val total = featured.eventCount
        val remaining = remainingCount(featured, nowMillis)
        val qualifier = qualifier(featured, nowMillis, zone, use24Hour)

        return when (countMode) {
            DensityCountMode.LEFT ->
                if (remaining == 0) {
                    DensityHeadline("Done today", "")
                } else {
                    DensityHeadline("$remaining left", qualifier)
                }

            DensityCountMode.FRACTION ->
                if (remaining == 0) {
                    DensityHeadline("0/$total", "done today")
                } else {
                    DensityHeadline("$remaining/$total left", qualifier)
                }

            DensityCountMode.TOTAL -> DensityHeadline("$total today", qualifier)
        }
    }

    /**
     * True once the evening has rolled over to tomorrow: past [rolloverHour] locally with
     * nothing left on today's plate.
     */
    fun shouldRollover(today: DayDensity, nowMillis: Long, rolloverHour: Int, zone: ZoneId): Boolean {
        val hour = Instant.ofEpochMilli(nowMillis).atZone(zone).hour
        return hour >= rolloverHour && remainingCount(today, nowMillis) == 0
    }

    /**
     * "ends in 25m" while a block is running, otherwise "next in 40m" for the block after
     * now, otherwise "nothing else today". Gaps longer than 90 minutes are given as a
     * clock time instead. Minutes are truncated, so "40m" means at least 40 minutes.
     */
    private fun qualifier(day: DayDensity, nowMillis: Long, zone: ZoneId, use24Hour: Boolean): String {
        val current = day.dayMerged.firstOrNull { nowMillis >= it.startMillis && nowMillis < it.endMillis }
        if (current != null) {
            val minutes = ((current.endMillis - nowMillis) / 60_000L).toInt()
            return if (minutes <= QUALIFIER_MINUTES_THRESHOLD) {
                "ends in " + TimeFormat.minutesLabel(minutes)
            } else {
                "ends at " + TimeFormat.clockNoSuffix(current.endMillis, zone, use24Hour)
            }
        }

        val next = day.dayMerged.firstOrNull { it.startMillis > nowMillis }
        if (next != null) {
            val minutes = ((next.startMillis - nowMillis) / 60_000L).toInt()
            return if (minutes <= QUALIFIER_MINUTES_THRESHOLD) {
                "next in " + TimeFormat.minutesLabel(minutes)
            } else {
                "next at " + TimeFormat.clockNoSuffix(next.startMillis, zone, use24Hour)
            }
        }

        return "nothing else today"
    }
}
