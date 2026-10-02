package com.fabiantorrestech.mycalendarwidget.notification

import com.fabiantorrestech.mycalendarwidget.data.TimeFormat
import com.fabiantorrestech.mycalendarwidget.data.density.DayDensity
import com.fabiantorrestech.mycalendarwidget.data.density.DensityHeadline
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/**
 * The headline for a day the notification's arrows have moved to. The widget's own
 * `DensityCalculator.headline` speaks of "today" and "tomorrow" only; a day further
 * out reads "3 events · first at 9:00" with the same date text, and no "left" count,
 * since none of it has started.
 */
object NotificationDayHeadline {

    fun headline(day: DayDensity, zone: ZoneId, use24Hour: Boolean, locale: Locale): DensityHeadline {
        val dateText = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale) +
            " (" + TimeFormat.shortDate(day.date, locale) + ")"

        if (day.eventCount == 0) {
            return DensityHeadline("Nothing planned", "", countIsSentence = true, dateText = dateText)
        }

        val count = if (day.eventCount == 1) "1 event" else "${day.eventCount} events"
        // An all-day-only day has a count but no timed block to name a start for.
        val qualifier = day.dayMerged.firstOrNull()
            ?.let { "first at " + TimeFormat.clockNoSuffix(it.startMillis, zone, use24Hour) }
            .orEmpty()
        return DensityHeadline(count, qualifier, dateText = dateText)
    }
}
