package com.fabiantorrestech.mycalendarwidget.widget

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import com.fabiantorrestech.mycalendarwidget.data.CalendarEvent
import com.fabiantorrestech.mycalendarwidget.data.CalendarLaunchView
import com.fabiantorrestech.mycalendarwidget.data.DefaultClickTarget
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfig
import com.fabiantorrestech.mycalendarwidget.ui.SettingsActivity

object WidgetClickActions {

    private const val PKG_DIGICAL = "com.digibites.calendar"
    private const val PKG_GCAL = "com.google.android.calendar"

    fun eventIntent(event: CalendarEvent, config: WidgetConfig): Intent {
        return when (config.defaultClickTarget) {
            // DigiCal: do NOT setPackage — the OS routes the bare URI intent to the user's
            // registered default calendar handler (DigiCal), which is identical to how the
            // system handles calendar notification taps and correctly opens the event viewer.
            // setPackage hits DigiCal's launcher activity instead of its event viewer.
            DefaultClickTarget.DIGICAL -> viewEventIntent(event)
            // GCal explicitly registers its event viewer for ACTION_VIEW + calendar URI.
            DefaultClickTarget.GCAL -> viewEventIntent(event).setPackage(PKG_GCAL)
            DefaultClickTarget.SYSTEM_DEFAULT -> viewEventIntent(event)
        }
    }

    fun headerIntent(config: WidgetConfig): Intent {
        val timeUri = Uri.parse("content://com.android.calendar/time/${System.currentTimeMillis()}")
        return when (config.defaultClickTarget) {
            DefaultClickTarget.GCAL -> gcalViewIntent(timeUri, config.calendarLaunchView)
            DefaultClickTarget.DIGICAL -> digicalViewIntent(timeUri, config.calendarLaunchView)
            DefaultClickTarget.SYSTEM_DEFAULT -> Intent(Intent.ACTION_VIEW).setData(timeUri)
        }
    }

    // GCal AllInOneActivity viewType: 2=day, 3=week, 5=month, 4=agenda/schedule.
    // WEEK_AGENDA and TEXT_MONTH fall back to their nearest GCal equivalents.
    // YEAR is not supported by GCal; falls back to DEFAULT (last-used view).
    private fun gcalViewIntent(timeUri: Uri, view: CalendarLaunchView): Intent {
        val base = Intent(Intent.ACTION_VIEW).setData(timeUri).setPackage(PKG_GCAL)
        val viewType = when (view) {
            CalendarLaunchView.DAY -> 2
            CalendarLaunchView.WEEK -> 3
            CalendarLaunchView.WEEK_AGENDA -> 3
            CalendarLaunchView.MONTH -> 5
            CalendarLaunchView.TEXT_MONTH -> 5
            CalendarLaunchView.YEAR -> null
            CalendarLaunchView.AGENDA -> 4
            CalendarLaunchView.DEFAULT -> null
        }
        return if (viewType != null) base.putExtra("viewType", viewType) else base
    }

    // DigiCal is targeted explicitly by package. For specific views, DigiCal may accept
    // the `me.everything.digical.extra.VIEW` extra. Values are best-effort (derived from
    // DigiCal's UI names, not a published API) and should be verified on-device.
    // If DigiCal ignores the extra, it opens to its last-used view (acceptable fallback).
    private fun digicalViewIntent(timeUri: Uri, view: CalendarLaunchView): Intent {
        val base = Intent(Intent.ACTION_VIEW).setData(timeUri).setPackage(PKG_DIGICAL)
        val viewExtra = when (view) {
            CalendarLaunchView.DAY -> "day"
            CalendarLaunchView.WEEK -> "week"
            CalendarLaunchView.WEEK_AGENDA -> "week_agenda"
            CalendarLaunchView.MONTH -> "month"
            CalendarLaunchView.TEXT_MONTH -> "text_month"
            CalendarLaunchView.YEAR -> "year"
            CalendarLaunchView.AGENDA -> "agenda"
            CalendarLaunchView.DEFAULT -> null
        }
        return if (viewExtra != null) base.putExtra("me.everything.digical.extra.VIEW", viewExtra) else base
    }

    fun mapsIntent(mapsQuery: String): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(mapsQuery)}"))

    fun meetingIntent(url: String): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(url))

    fun quickAddIntent(): Intent =
        Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.DTSTART, System.currentTimeMillis())
            .putExtra(CalendarContract.Events.DTEND, System.currentTimeMillis() + 3_600_000L)

    fun settingsIntent(context: Context): Intent =
        Intent(context, SettingsActivity::class.java)

    // Bare ACTION_VIEW + URI (no explicit MIME type): Android routes via the registered
    // default calendar handler, which is how system notification taps work — DigiCal and
    // GCal both handle this correctly when set as the system default.
    private fun viewEventIntent(event: CalendarEvent): Intent =
        Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.eventId))
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.dtStart)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.dtEnd)
}
