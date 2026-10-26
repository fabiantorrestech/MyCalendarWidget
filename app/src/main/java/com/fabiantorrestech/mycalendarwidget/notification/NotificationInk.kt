package com.fabiantorrestech.mycalendarwidget.notification

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.fabiantorrestech.mycalendarwidget.R
import com.fabiantorrestech.mycalendarwidget.data.DisplayMode

/**
 * The backgrounds and text/icon colours the notification's Today pill, + and refresh
 * buttons switch to outside Color mode. Resources rather than resolved colours, so the
 * shade can still pick the day or night value itself.
 */
data class NotificationInk(
    @DrawableRes val todayPill: Int,
    @ColorRes val todayText: Int,
    @DrawableRes val addCircle: Int,
    @ColorRes val addTint: Int,
    @DrawableRes val refreshCircle: Int,
    @ColorRes val refreshTint: Int
) {
    companion object {

        private val GRAY = NotificationInk(
            todayPill = R.drawable.notification_pill_gray,
            todayText = R.color.notification_on_gray_container,
            addCircle = R.drawable.notification_circle_gray,
            addTint = R.color.notification_on_gray_container,
            refreshCircle = R.drawable.notification_circle_gray,
            refreshTint = R.color.notification_on_gray_container
        )

        private val GRAY_STRONG = NotificationInk(
            todayPill = R.drawable.notification_pill_gray_strong,
            todayText = R.color.notification_on_gray_strong,
            addCircle = R.drawable.notification_circle_gray_strong,
            addTint = R.color.notification_on_gray_strong,
            refreshCircle = R.drawable.notification_circle_gray_strong,
            refreshTint = R.color.notification_on_gray_strong
        )

        /**
         * Null in Color mode: the layouts' own accent colours (and the pinned card's
         * stronger accent) already apply. A Grayscale pinned card is the gray container
         * itself, so its pill and buttons step up to the strong gray; an E-ink card is
         * paper white or black, against which the ordinary gray already stands out.
         */
        fun forMode(display: DisplayMode, pinned: Boolean): NotificationInk? = when (display) {
            DisplayMode.GRAYSCALE -> if (pinned) GRAY_STRONG else GRAY
            DisplayMode.EINK -> GRAY
            else -> null
        }
    }
}
