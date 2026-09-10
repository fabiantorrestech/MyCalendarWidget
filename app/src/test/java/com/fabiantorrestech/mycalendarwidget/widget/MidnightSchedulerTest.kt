package com.fabiantorrestech.mycalendarwidget.widget

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Pure JVM tests for [MidnightScheduler.nextMidnightMillis]. No Android classes are touched —
 * this exercises only the pure function so it can run without instrumentation.
 */
class MidnightSchedulerTest {

    private val newYork = ZoneId.of("America/New_York")

    private fun millisAt(zone: ZoneId, year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int = 0): Long =
        ZonedDateTime.of(year, month, day, hour, minute, second, 0, zone).toInstant().toEpochMilli()

    @Test
    fun `spring-forward day rolls to next midnight in the post-transition offset`() {
        val now = millisAt(newYork, 2026, 3, 7, 12, 0)
        val expected = ZonedDateTime.of(2026, 3, 8, 0, 0, 0, 0, newYork).toInstant().toEpochMilli()
        assertEquals(expected, MidnightScheduler.nextMidnightMillis(now, newYork))
        // -05:00 offset before the spring-forward transition
        assertEquals("-05:00", Instant.ofEpochMilli(expected).atZone(newYork).offset.id)
    }

    @Test
    fun `day after spring-forward rolls to midnight in the new offset, 23 hours after the prior target`() {
        val dayBefore = millisAt(newYork, 2026, 3, 7, 12, 0)
        val dayOf = millisAt(newYork, 2026, 3, 8, 12, 0)

        val targetBefore = MidnightScheduler.nextMidnightMillis(dayBefore, newYork)
        val targetOf = MidnightScheduler.nextMidnightMillis(dayOf, newYork)

        val expectedOf = ZonedDateTime.of(2026, 3, 9, 0, 0, 0, 0, newYork).toInstant().toEpochMilli()
        assertEquals(expectedOf, targetOf)
        assertEquals("-04:00", Instant.ofEpochMilli(targetOf).atZone(newYork).offset.id)

        assertEquals(23L, Duration.ofMillis(targetOf - targetBefore).toHours())
    }

    @Test
    fun `fall-back midnight targets are 25 hours apart`() {
        val onOct31 = millisAt(newYork, 2026, 10, 31, 12, 0)
        val onNov1 = millisAt(newYork, 2026, 11, 1, 12, 0)

        val targetOct31 = MidnightScheduler.nextMidnightMillis(onOct31, newYork)
        val targetNov1 = MidnightScheduler.nextMidnightMillis(onNov1, newYork)

        assertEquals(25L, Duration.ofMillis(targetNov1 - targetOct31).toHours())
    }

    @Test
    fun `just before midnight rolls to the next day's midnight`() {
        val now = millisAt(newYork, 2026, 6, 15, 23, 59, 59)
        val expected = ZonedDateTime.of(2026, 6, 16, 0, 0, 0, 0, newYork).toInstant().toEpochMilli()
        assertEquals(expected, MidnightScheduler.nextMidnightMillis(now, newYork))
    }

    @Test
    fun `exactly at midnight rolls to the following day's midnight, not the same instant`() {
        val exactlyMidnight = ZonedDateTime.of(2026, 6, 15, 0, 0, 0, 0, newYork).toInstant().toEpochMilli()
        val expected = ZonedDateTime.of(2026, 6, 16, 0, 0, 0, 0, newYork).toInstant().toEpochMilli()
        assertEquals(expected, MidnightScheduler.nextMidnightMillis(exactlyMidnight, newYork))
    }

    @Test
    fun `same instant in different zones yields different midnight targets`() {
        val now = Instant.parse("2026-06-15T10:00:00Z").toEpochMilli()
        val tokyo = ZoneId.of("Asia/Tokyo")
        val losAngeles = ZoneId.of("America/Los_Angeles")

        val tokyoTarget = MidnightScheduler.nextMidnightMillis(now, tokyo)
        val laTarget = MidnightScheduler.nextMidnightMillis(now, losAngeles)

        assertEquals(0, Instant.ofEpochMilli(tokyoTarget).atZone(tokyo).toLocalTime().toSecondOfDay())
        assertEquals(0, Instant.ofEpochMilli(laTarget).atZone(losAngeles).toLocalTime().toSecondOfDay())
        assert(tokyoTarget != laTarget)
    }

    @Test
    fun `zone without a local midnight on the transition night resolves to the first valid instant instead of throwing`() {
        // America/Santiago has a DST-start gap at local 00:00 on 2026-09-06 (verified via
        // java.time.zone.ZoneRules.getTransition against America/Santiago's rules): the clock
        // jumps from 2026-09-05T23:59:59.999-04:00 straight to 2026-09-06T01:00:00-03:00, so
        // 2026-09-06T00:00 does not exist as a local time in that zone.
        val santiago = ZoneId.of("America/Santiago")
        val now = millisAt(santiago, 2026, 9, 5, 20, 0)

        val target = MidnightScheduler.nextMidnightMillis(now, santiago)

        val resolved = Instant.ofEpochMilli(target).atZone(santiago)
        val expected = ZonedDateTime.of(2026, 9, 6, 1, 0, 0, 0, santiago)
        assertEquals(expected.toInstant().toEpochMilli(), target)
        assertEquals("-03:00", resolved.offset.id)
    }
}
