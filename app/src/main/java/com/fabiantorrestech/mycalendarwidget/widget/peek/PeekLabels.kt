package com.fabiantorrestech.mycalendarwidget.widget.peek

import com.fabiantorrestech.mycalendarwidget.data.CalendarEvent
import com.fabiantorrestech.mycalendarwidget.data.TimeFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/** The peek's date headers and time column, kept pure so the wording is unit-tested. */
object PeekLabels {
    /**
     * Marks a row for an event that began on an earlier day, ahead of the time it ends.
     * The arrow and the time are joined by a non-breaking space: the time column is a
     * one-line text view, and at an ordinary space it could wrap and show the arrow alone.
     */
    private const val CONTINUES_UNTIL = "\u2192\u00A0"

    /**
     * The time column of an event's row under [date]. An all-day event, and a timed one
     * that covers [date] from midnight to midnight, read [allDayWord]. A timed event on
     * its first day shows its start, "6:00p"; on a later day it ends on, the arrow and its
     * end, "→ 2:00p", so a carried-over event never passes for one starting that day.
     */
    fun timeLabel(
        event: CalendarEvent,
        date: LocalDate,
        zone: ZoneId,
        use24Hour: Boolean,
        allDayWord: String
    ): String {
        if (event.allDay) return allDayWord
        if (Instant.ofEpochMilli(event.dtStart).atZone(zone).toLocalDate() == date) {
            return TimeFormat.compact(event.dtStart, zone, use24Hour)
        }
        val nextMidnight = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return if (event.dtEnd < nextMidnight) {
            CONTINUES_UNTIL + TimeFormat.compact(event.dtEnd, zone, use24Hour)
        } else {
            allDayWord
        }
    }

    /**
     * "Today (9/17)", "Tomorrow (9/18)", else the short weekday with the date, "Fri (9/18)".
     * [todayWord] and [tomorrowWord] come from string resources.
     */
    fun dateHeader(
        date: LocalDate,
        today: LocalDate,
        todayWord: String,
        tomorrowWord: String,
        locale: Locale
    ): String {
        val word = when (date) {
            today -> todayWord
            today.plusDays(1) -> tomorrowWord
            else -> date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
        }
        return "$word (${TimeFormat.shortDate(date, locale)})"
    }

    /** Only today's header is set in bold. */
    fun isToday(date: LocalDate, today: LocalDate): Boolean = date == today
}
