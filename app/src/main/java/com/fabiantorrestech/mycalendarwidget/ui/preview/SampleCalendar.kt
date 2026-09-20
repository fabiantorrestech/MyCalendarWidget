package com.fabiantorrestech.mycalendarwidget.ui.preview

import com.fabiantorrestech.mycalendarwidget.data.CalendarEvent
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfig
import com.fabiantorrestech.mycalendarwidget.data.density.DensityCalculator
import com.fabiantorrestech.mycalendarwidget.data.density.DensitySnapshot
import com.fabiantorrestech.mycalendarwidget.data.density.RawInstance
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * The scripted calendar every settings preview runs on, so the previews show the same
 * shapes to everyone and never read the user's own events: today is busy with gaps, one
 * collision and an all-day event; tomorrow is light; the day after is over any load
 * baseline; the third day is medium with an all-day event; further days cycle the last
 * three. Pure and deterministic given a date, so the previews can be unit-tested.
 *
 * This lives under `ui/` rather than `data/density/`: it carries titles, which the
 * density pipeline itself must never see.
 */
object SampleCalendar {

    data class SampleCalendarInfo(val id: Long, val name: String, val color: Int)

    val calendars: List<SampleCalendarInfo> = listOf(
        SampleCalendarInfo(1L, "Work", 0xFF5C6BC0.toInt()),
        SampleCalendarInfo(2L, "Personal", 0xFF26A69A.toInt()),
        SampleCalendarInfo(3L, "Health", 0xFFEF5350.toInt()),
        SampleCalendarInfo(4L, "Family", 0xFFFFA726.toInt())
    )

    /** One scripted event: minutes are wall-clock minutes of the day at [dayOffset]. */
    data class SampleEvent(
        val dayOffset: Int,
        val startMinute: Int,
        val endMinute: Int,
        val title: String,
        val calendarId: Long,
        val allDay: Boolean = false
    )

    private fun m(h: Int, min: Int = 0) = h * 60 + min

    private val today: List<SampleEvent> = listOf(
        SampleEvent(0, 0, 1440, "Conference (all day)", 1L, allDay = true),
        SampleEvent(0, m(8, 30), m(9, 15), "Team standup", 1L),
        SampleEvent(0, m(10), m(11, 30), "Design review", 1L),
        SampleEvent(0, m(10, 30), m(11), "1:1 with Sam", 2L),
        SampleEvent(0, m(13), m(14), "Lunch with Priya", 2L),
        SampleEvent(0, m(15), m(17, 30), "Focus block", 1L),
        SampleEvent(0, m(19), m(20), "Gym", 3L)
    )

    private val light: List<SampleEvent> = listOf(
        SampleEvent(0, m(12), m(12, 30), "Dentist", 3L)
    )

    /** 13 hours, past the top of the baseline slider (12h). */
    private val overflow: List<SampleEvent> = listOf(
        SampleEvent(0, m(8), m(13), "Workshop", 1L),
        SampleEvent(0, m(13), m(18, 30), "Client onsite", 1L),
        SampleEvent(0, m(19), m(21, 30), "Dinner with the team", 2L)
    )

    private val medium: List<SampleEvent> = listOf(
        SampleEvent(0, 0, 1440, "Mom's birthday", 4L, allDay = true),
        SampleEvent(0, m(9), m(10), "Errands", 2L),
        SampleEvent(0, m(14), m(16), "Study group", 1L)
    )

    private val templates = listOf(light, overflow, medium)

    /** The scripted events for today plus [lookaheadDays] further days. */
    fun plan(lookaheadDays: Int): List<SampleEvent> {
        val days = ArrayList<SampleEvent>()
        days += today
        for (offset in 1..maxOf(lookaheadDays, 0)) {
            // Offsets 1, 2, 3 are light, overflow, medium; later days cycle the same three.
            val template = templates[(offset - 1) % templates.size]
            days += template.map { it.copy(dayOffset = offset) }
        }
        return days
    }

    private fun colorOf(calendarId: Long): Int = calendars.first { it.id == calendarId }.color

    private fun startMillis(e: SampleEvent, today: LocalDate, zone: ZoneId): Long {
        val date = today.plusDays(e.dayOffset.toLong())
        // All-day instances come back from the provider as UTC midnights; timed ones sit on
        // the local time-line.
        return if (e.allDay) {
            date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        } else {
            date.atStartOfDay().plusMinutes(e.startMinute.toLong()).atZone(zone).toInstant().toEpochMilli()
        }
    }

    private fun endMillis(e: SampleEvent, today: LocalDate, zone: ZoneId): Long {
        val date = today.plusDays(e.dayOffset.toLong())
        return if (e.allDay) {
            date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        } else {
            date.atStartOfDay().plusMinutes(e.endMinute.toLong()).atZone(zone).toInstant().toEpochMilli()
        }
    }

    /** The plan as the content-free instances the density calculator consumes. */
    fun rawInstances(today: LocalDate, zone: ZoneId, lookaheadDays: Int): List<RawInstance> =
        plan(lookaheadDays).map { e ->
            RawInstance(
                begin = startMillis(e, today, zone),
                end = endMillis(e, today, zone),
                allDay = e.allDay,
                calendarId = e.calendarId,
                selfAttendeeStatus = 0,
                status = 1,
                availability = 0,
                displayColor = colorOf(e.calendarId),
                calendarColor = colorOf(e.calendarId)
            )
        }

    /** The plan as titled events keyed by day, for the agenda and peek previews. */
    fun eventsByDay(today: LocalDate, zone: ZoneId, lookaheadDays: Int): Map<LocalDate, List<CalendarEvent>> =
        plan(lookaheadDays).mapIndexed { index, e ->
            val date = today.plusDays(e.dayOffset.toLong())
            date to CalendarEvent(
                id = index + 1L,
                eventId = index + 1L,
                title = e.title,
                dtStart = startMillis(e, today, zone),
                dtEnd = endMillis(e, today, zone),
                allDay = e.allDay,
                location = null,
                description = null,
                calendarId = e.calendarId,
                calendarColor = colorOf(e.calendarId),
                eventColor = null,
                selfAttendeeStatus = 0,
                organizer = null,
                eventTimezone = zone.id,
                hasAlarm = false,
                rrule = null,
                meetingUrl = null,
                mapsQuery = null
            )
        }.groupBy({ it.first }, { it.second })

    /**
     * The density snapshot for the sample: today is always featured (no evening rollover,
     * so the busy day is what the preview shows), followed by the configured look-ahead.
     */
    fun snapshot(config: WidgetConfig, nowMillis: Long, zone: ZoneId): DensitySnapshot {
        val today = java.time.Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val lookaheadDays = config.densityLookaheadDays.coerceAtLeast(0)
        val raw = rawInstances(today, zone, lookaheadDays)
        fun build(offset: Int) = DensityCalculator.buildDay(
            date = today.plusDays(offset.toLong()),
            raw = raw,
            enabledCalendarIds = emptySet(),
            windowStartMinutes = config.densityWindowStartMinutes,
            windowEndMinutes = config.densityWindowEndMinutes,
            zone = zone
        )
        return DensitySnapshot(
            hasPermission = true,
            featured = build(0),
            featuredIsToday = true,
            lookahead = (1..lookaheadDays).map { build(it) },
            nowMillis = nowMillis,
            visibleCalendarIds = calendars.map { it.id }
        )
    }
}
