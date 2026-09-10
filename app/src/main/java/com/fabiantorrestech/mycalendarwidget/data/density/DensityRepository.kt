package com.fabiantorrestech.mycalendarwidget.data.density

import android.content.Context
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfig
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Assembles a [DensitySnapshot] from the content-free [DensityCalendarSource] and the pure
 * [DensityCalculator]. This class owns the Android seam (permission check, one content
 * query, wall-clock-to-day bucketing); all filtering, merging and windowing happens in the
 * pure calculator.
 */
class DensityRepository(context: Context) {

    private val source = DensityCalendarSource(context)

    /**
     * Off the main thread is the caller's responsibility — this method performs a
     * synchronous [android.content.ContentResolver] query.
     */
    fun load(
        config: WidgetConfig,
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): DensitySnapshot {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val lookaheadDays = config.densityLookaheadDays

        // The featured day is today, or tomorrow once the evening rollover has happened;
        // the lookahead is `lookaheadDays` days after whichever day ends up featured. The
        // worst case is a rollover, which shifts the whole window one day later, so the
        // last day we could ever need is today + lookaheadDays + 1 (tomorrow, plus
        // `lookaheadDays` more). Querying up to (exclusive) today + lookaheadDays + 2
        // always covers that day, so no follow-up query is ever needed here.
        val queryEndExclusive = today.plusDays(lookaheadDays + 2L)
        val startMillis = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillisExclusive = queryEndExclusive.atStartOfDay(zone).toInstant().toEpochMilli()

        val raw = source.queryRawInstances(startMillis, endMillisExclusive)

        // Build every day in the queried range up front; buildDay filters [raw] down to
        // the instances that intersect each date, so passing the full list to each call is
        // correct (and required for instances that straddle midnight).
        val dayCount = (lookaheadDays + 2)
        val days = (0 until dayCount).associate { offset ->
            val date = today.plusDays(offset.toLong())
            date to DensityCalculator.buildDay(
                date = date,
                raw = raw,
                enabledCalendarIds = config.enabledCalendarIds,
                windowStartMinutes = config.densityWindowStartMinutes,
                windowEndMinutes = config.densityWindowEndMinutes,
                zone = zone
            )
        }

        val todayDensity = requireNotNull(days[today])
        val featuredIsToday = !DensityCalculator.shouldRollover(
            todayDensity,
            nowMillis,
            config.densityRolloverHour,
            zone
        )
        val featuredDate = if (featuredIsToday) today else today.plusDays(1)
        val featured = requireNotNull(days[featuredDate])

        val lookahead = (1..lookaheadDays).map { offset ->
            requireNotNull(days[featuredDate.plusDays(offset.toLong())])
        }

        return DensitySnapshot(
            hasPermission = source.hasPermission(),
            featured = featured,
            featuredIsToday = featuredIsToday,
            lookahead = lookahead,
            nowMillis = nowMillis
        )
    }
}
