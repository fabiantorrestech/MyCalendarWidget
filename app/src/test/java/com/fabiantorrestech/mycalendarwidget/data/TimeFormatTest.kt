package com.fabiantorrestech.mycalendarwidget.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

/** Unit tests for the pure clock/date formatting helpers used by the density widget. */
class TimeFormatTest {

    private val zone: ZoneId = ZoneId.of("America/New_York")
    private val locale: Locale = Locale.US
    private val day: LocalDate = LocalDate.of(2026, 8, 16)

    private fun t(hour: Int, minute: Int): Long =
        LocalDateTime.of(day, LocalTime.of(hour, minute)).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `compact renders an afternoon time with a p suffix in 12-hour mode`() {
        assertEquals("1:30p", TimeFormat.compact(t(13, 30), zone, use24Hour = false))
    }

    @Test
    fun `compact renders an afternoon time without a suffix in 24-hour mode`() {
        assertEquals("13:30", TimeFormat.compact(t(13, 30), zone, use24Hour = true))
    }

    @Test
    fun `compact renders just after midnight as twelve with an a suffix`() {
        assertEquals("12:05a", TimeFormat.compact(t(0, 5), zone, use24Hour = false))
    }

    @Test
    fun `compact pads the hour just after midnight in 24-hour mode`() {
        assertEquals("00:05", TimeFormat.compact(t(0, 5), zone, use24Hour = true))
    }

    @Test
    fun `compact renders noon as twelve with a p suffix`() {
        assertEquals("12:00p", TimeFormat.compact(t(12, 0), zone, use24Hour = false))
    }

    @Test
    fun `clock renders a US 12-hour time with an AM PM suffix`() {
        assertEquals("1:30 PM", TimeFormat.clock(t(13, 30), zone, use24Hour = false, locale = locale))
    }

    @Test
    fun `clock renders a 24-hour time without a suffix`() {
        assertEquals("13:30", TimeFormat.clock(t(13, 30), zone, use24Hour = true, locale = locale))
    }

    @Test
    fun `clockNoSuffix drops the AM PM marker in 12-hour mode`() {
        assertEquals("1:30", TimeFormat.clockNoSuffix(t(13, 30), zone, use24Hour = false))
    }

    @Test
    fun `clockNoSuffix matches clock in 24-hour mode`() {
        assertEquals("13:30", TimeFormat.clockNoSuffix(t(13, 30), zone, use24Hour = true))
    }

    @Test
    fun `shortDate renders month and day`() {
        assertEquals("8/16", TimeFormat.shortDate(day, locale))
    }

    @Test
    fun `minutesLabel appends a minute suffix`() {
        assertEquals("40m", TimeFormat.minutesLabel(40))
    }
}
