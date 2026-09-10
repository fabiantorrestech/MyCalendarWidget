package com.fabiantorrestech.mycalendarwidget.data.density

import kotlin.math.abs

/**
 * Five tones of one accent for the Tonal strip mode, one bucket per calendar. Pure
 * Kotlin (G2): only [ColorMath], no Android.
 */
object TonalRamp {

    /**
     * Five tones from [accent] (index 0, unchanged) toward [background], at
     * [DensityConstants.TONE_RATIOS]. When the raw ratios would bring the last
     * (most-shifted) tone within [DensityConstants.TONE_MIN_LUMINANCE_DELTA] of
     * [background]'s luminance, every ratio is scaled down by the same factor so the
     * last tone lands exactly on that minimum separation instead of blending into the
     * ground. Tone 0 is always the accent, whatever the compression.
     */
    fun ramp(accent: Int, background: Int): IntArray {
        val diff = abs(ColorMath.luminance(accent) - ColorMath.luminance(background))
        val lastRatio = DensityConstants.TONE_RATIOS.last()
        val cap = if (diff > 0f) {
            1f - (DensityConstants.TONE_MIN_LUMINANCE_DELTA / diff)
        } else {
            lastRatio
        }
        val scale = if (cap < lastRatio) (cap / lastRatio).coerceIn(0f, 1f) else 1f

        return IntArray(DensityConstants.TONE_RATIOS.size) { i ->
            ColorMath.lerp(accent, background, DensityConstants.TONE_RATIOS[i] * scale)
        }
    }

    /**
     * The tone index (0..4) [calendarId] draws in. An explicit entry in [assigned] wins
     * outright — clamped into range with [Int.coerceIn] so a corrupted or hand-edited
     * profile (e.g. an assignment left over from a build with a larger
     * [DensityConstants.TONE_COUNT]) can never index [TonalRamp.ramp]'s array out of
     * bounds; otherwise the calendar's rank in [enabledSortedIds] (0 when it is not
     * present at all) picks the tone, wrapping past [DensityConstants.TONE_COUNT].
     * Ranks come from the caller's sorted id list rather than being recomputed here, so
     * adding a calendar with a higher id never reshuffles the ones already assigned.
     */
    fun bucket(calendarId: Long, assigned: Map<Long, Int>, enabledSortedIds: List<Long>): Int {
        assigned[calendarId]?.let { return it.coerceIn(0, DensityConstants.TONE_COUNT - 1) }
        val index = enabledSortedIds.indexOf(calendarId)
        val rank = if (index < 0) 0 else index
        return rank % DensityConstants.TONE_COUNT
    }
}
