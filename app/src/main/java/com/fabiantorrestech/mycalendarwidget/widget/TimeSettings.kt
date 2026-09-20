package com.fabiantorrestech.mycalendarwidget.widget

import android.content.Context
import android.text.format.DateFormat

/**
 * The single Android seam for the user's 12/24-hour preference. Pure formatting code takes
 * `use24Hour` as a parameter, so this is the only place that reads the system setting.
 */
fun use24Hour(context: Context): Boolean = DateFormat.is24HourFormat(context)
