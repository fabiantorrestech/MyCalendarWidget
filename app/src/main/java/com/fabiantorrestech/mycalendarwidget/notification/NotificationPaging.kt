package com.fabiantorrestech.mycalendarwidget.notification

/**
 * Where the expanded notification's ‹ › arrows stand, derived purely from the stored
 * offset and the time of the last tap.
 *
 * The arrows are a momentary look ahead, not a mode, like the widget's peek sheet: once
 * nobody has tapped for [IDLE_RESET_MS], the next refresh draws the first page (or
 * today) again. Nothing needs to clear the stored offset for that to happen.
 */
object NotificationPaging {

    /** Two minutes without a tap and the arrows count as back at the start. */
    const val IDLE_RESET_MS = 120_000L

    /** The offset to draw: [stored] while the last tap is recent, else 0. Never negative. */
    fun effectiveOffset(stored: Int, touchedAtMillis: Long, nowMillis: Long): Int =
        if (nowMillis - touchedAtMillis > IDLE_RESET_MS) 0 else maxOf(0, stored)

    /** [offset] pulled onto one of [pageCount] pages; 0 when there are none. */
    fun clamp(offset: Int, pageCount: Int): Int =
        offset.coerceIn(0, maxOf(0, pageCount - 1))
}
