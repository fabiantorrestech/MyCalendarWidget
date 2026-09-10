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

    /** How far each tone is shifted from the accent, index 0 being the accent itself. */
    val TONE_RATIOS = floatArrayOf(0f, .16f, .32f, .47f, .60f)

    /** Minimum luminance separation two adjacent tones must keep. */
    const val TONE_MIN_LUMINANCE_DELTA = 0.18f
}
