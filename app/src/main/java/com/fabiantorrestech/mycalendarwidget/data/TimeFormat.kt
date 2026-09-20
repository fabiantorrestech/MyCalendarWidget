package com.fabiantorrestech.mycalendarwidget.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Pure clock and date formatting. No Android, no ambient clock: the caller always
 * supplies the instant, the zone, the 24-hour preference and the locale.
 */
object TimeFormat {

    /** 24-hour output is digits only, so it is pinned to a fixed locale for stability. */
    private val HOUR_24 = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    private val HOUR_12_NO_SUFFIX = DateTimeFormatter.ofPattern("h:mm", Locale.US)

    /**
     * [clock]'s 12-hour formatter, keyed by locale: `DateTimeFormatter.ofPattern` parses
     * its pattern string on every call, and unlike [HOUR_24]/[HOUR_12_NO_SUFFIX] this one
     * cannot be a single `private val` because it is locale-dependent. A small cache
     * keeps a repeat call for the same (device-wide, effectively constant) locale from
     * re-parsing the pattern every time.
     */
    private val hour12Formatters = ConcurrentHashMap<Locale, DateTimeFormatter>()

    private fun hour12Formatter(locale: Locale): DateTimeFormatter =
        // computeIfAbsent, not Kotlin's getOrPut: getOrPut on a ConcurrentHashMap is a
        // plain get-then-put and can race, whereas computeIfAbsent is atomic.
        hour12Formatters.computeIfAbsent(locale) { DateTimeFormatter.ofPattern("h:mm a", it) }

    /** "1:30 PM" in 12-hour mode, "13:30" in 24-hour mode. */
    fun clock(millis: Long, zone: ZoneId, use24Hour: Boolean, locale: Locale): String {
        val time = Instant.ofEpochMilli(millis).atZone(zone)
        return if (use24Hour) {
            time.format(HOUR_24)
        } else {
            time.format(hour12Formatter(locale))
        }
    }

    /** "1:30" in 12-hour mode, "13:30" in 24-hour mode — the headline drops the AM/PM marker. */
    fun clockNoSuffix(millis: Long, zone: ZoneId, use24Hour: Boolean): String {
        val time = Instant.ofEpochMilli(millis).atZone(zone)
        return time.format(if (use24Hour) HOUR_24 else HOUR_12_NO_SUFFIX)
    }

    /** "1:30p" in 12-hour mode, "13:30" in 24-hour mode — for tight axis labels. */
    fun compact(millis: Long, zone: ZoneId, use24Hour: Boolean): String {
        val time = Instant.ofEpochMilli(millis).atZone(zone)
        if (use24Hour) return time.format(HOUR_24)
        val suffix = if (time.hour < 12) "a" else "p"
        return time.format(HOUR_12_NO_SUFFIX) + suffix
    }

    /**
     * "8/16". The month-first ordering is fixed for now; making it follow the locale's
     * own short-date ordering is a follow-up.
     */
    fun shortDate(date: LocalDate, locale: Locale): String =
        date.format(DateTimeFormatter.ofPattern("M/d", locale))

    /** "40m". */
    fun minutesLabel(minutes: Int): String = "${minutes}m"

    /**
     * "—" (em dash) for zero, "45m" under an hour, "1h"/"5h" on the hour, otherwise
     * "6h40"/"1h05" with the minute remainder padded to two digits.
     */
    fun durationLabel(minutes: Int): String {
        if (minutes == 0) return "—"
        if (minutes < 60) return "${minutes}m"
        val hours = minutes / 60
        val rest = minutes % 60
        return if (rest == 0) "${hours}h" else "${hours}h${rest.toString().padStart(2, '0')}"
    }
}
