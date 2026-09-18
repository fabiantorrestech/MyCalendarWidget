package com.fabiantorrestech.mycalendarwidget.data.density

/**
 * The handful of CalendarContract values the pure density layer needs, mirrored here so
 * this package never has to touch android.jar.
 */
object DensityConstants {
    /** CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED */
    const val ATTENDEE_STATUS_DECLINED = 2

    /** CalendarContract.Events.STATUS_CANCELED */
    const val EVENT_STATUS_CANCELED = 2

    /** CalendarContract.Events.AVAILABILITY_FREE */
    const val AVAILABILITY_FREE = 1

    /** Fallback busy accent when no theme colour is available. */
    const val DEFAULT_BUSY_COLOR = 0xFF7F77DD.toInt()

    /** The "now" marker drawn on the strip. */
    const val NOW_MARKER_COLOR = 0xFFD85A30.toInt()

    /** Number of tones the Tonal strip mode can assign to calendars. */
    const val TONE_COUNT = 5

    /**
     * The tone each automatic rank draws in: neighbouring ranks land as far apart on the
     * ramp as it allows (two calendars are the accent and the lightest tone), while a
     * calendar's rank, and so its tone, never changes when another calendar is added.
     */
    val RANK_TO_TONE = intArrayOf(0, 4, 2, 3, 1)

    /**
     * How many stacked lanes the strip splits coinciding events into. Two: the track is
     * 14dp and the all-day band sits above it, so a third lane no longer reads.
     */
    const val MAX_LANES = 2

    /** How far each tone is shifted from the accent, index 0 being the accent itself. */
    val TONE_RATIOS = floatArrayOf(0f, .16f, .32f, .47f, .60f)

    /** Minimum luminance separation two adjacent tones must keep. */
    const val TONE_MIN_LUMINANCE_DELTA = 0.18f

    /**
     * Minimum luminance separation the busy accent must keep from the widget
     * background. A pastel Material You primary can sit as close as ~0.17 to a light
     * ground, which (a) leaves the Tonal ramp's compression cap under 0.55 — too little
     * room for five visibly distinct tones (see [TONE_MIN_LUMINANCE_DELTA] and
     * [TonalRamp.ramp]) — and (b) makes Shape's solid busy blocks hard to read. Enforcing
     * 0.40 here keeps both readable without the user ever seeing a separate setting.
     */
    const val BUSY_MIN_LUMINANCE_DELTA = 0.40f
}
