package com.fabiantorrestech.mycalendarwidget.widget.peek

import com.fabiantorrestech.mycalendarwidget.data.TimeFormat
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** The Grouped peek's date headers, kept pure so the wording is unit-tested. */
object PeekLabels {
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
