package com.fabiantorrestech.mycalendarwidget.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.ZoneId

class CalendarRepository(private val context: Context) {

    private val meetingUrlPatterns = listOf(
        "zoom.us/j/",
        "meet.google.com/",
        "teams.microsoft.com/l/meetup-join/",
        "webex.com/meet/"
    )

    fun getEventsByDay(config: WidgetConfig): Map<LocalDate, List<CalendarEvent>> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
            != PackageManager.PERMISSION_GRANTED) {
            return emptyMap()
        }

        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now()
        val windowBase = if (config.monthOffset == 0) today
            else today.plusMonths(config.monthOffset.toLong()).withDayOfMonth(1)
        val windowStart = windowBase.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val windowEnd = windowStart + config.daysAheadToLoad.toLong() * 24 * 60 * 60 * 1000

        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().let {
            ContentUris.appendId(it, windowStart)
            ContentUris.appendId(it, windowEnd)
            it.build()
        }

        val projection = arrayOf(
            CalendarContract.Instances._ID,
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.CALENDAR_COLOR,
            CalendarContract.Instances.DISPLAY_COLOR,
            CalendarContract.Instances.SELF_ATTENDEE_STATUS,
            CalendarContract.Instances.ORGANIZER,
            CalendarContract.Instances.EVENT_TIMEZONE,
            CalendarContract.Instances.HAS_ALARM,
            CalendarContract.Instances.RRULE
        )

        val selection = "${CalendarContract.Events.DELETED} != 1"

        val events = mutableListOf<CalendarEvent>()
        val cursor = context.contentResolver.query(uri, projection, selection, null, "${CalendarContract.Instances.BEGIN} ASC")

        cursor?.use {
            val idIdx = it.getColumnIndexOrThrow(CalendarContract.Instances._ID)
            val eventIdIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
            val titleIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val beginIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val endIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.END)
            val allDayIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
            val locationIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_LOCATION)
            val descIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.DESCRIPTION)
            val calIdIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_ID)
            val calColorIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_COLOR)
            val displayColorIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.DISPLAY_COLOR)
            val attendeeIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.SELF_ATTENDEE_STATUS)
            val organizerIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.ORGANIZER)
            val tzIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_TIMEZONE)
            val alarmIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.HAS_ALARM)
            val rruleIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.RRULE)

            while (it.moveToNext()) {
                val location = it.getString(locationIdx)?.takeIf { s -> s.isNotBlank() }
                val description = it.getString(descIdx)?.takeIf { s -> s.isNotBlank() }
                val meetingUrl = extractMeetingUrl(description, location)
                val mapsQuery = if (meetingUrl == null) extractMapsQuery(location) else null
                val displayColorVal = it.getInt(displayColorIdx)
                val calColorVal = it.getInt(calColorIdx)
                val eventColor = if (displayColorVal != calColorVal) displayColorVal else null

                events.add(CalendarEvent(
                    id = it.getLong(idIdx),
                    eventId = it.getLong(eventIdIdx),
                    title = it.getString(titleIdx) ?: "(No title)",
                    dtStart = it.getLong(beginIdx),
                    dtEnd = it.getLong(endIdx),
                    allDay = it.getInt(allDayIdx) == 1,
                    location = location,
                    description = description,
                    calendarId = it.getLong(calIdIdx),
                    calendarColor = calColorVal,
                    eventColor = eventColor,
                    selfAttendeeStatus = it.getInt(attendeeIdx),
                    organizer = it.getString(organizerIdx),
                    eventTimezone = it.getString(tzIdx) ?: "UTC",
                    hasAlarm = it.getInt(alarmIdx) == 1,
                    rrule = it.getString(rruleIdx),
                    meetingUrl = meetingUrl,
                    mapsQuery = mapsQuery
                ))
            }
        }

        return applyFiltersAndGroup(events, config, zoneId)
    }

    fun getCalendars(): List<CalendarInfo> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
            != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }

        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR
        )

        val calendars = mutableListOf<CalendarInfo>()
        val cursor = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection, null, null, "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC"
        )

        cursor?.use {
            val idIdx = it.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val nameIdx = it.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val accountIdx = it.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
            val colorIdx = it.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_COLOR)

            while (it.moveToNext()) {
                calendars.add(CalendarInfo(
                    id = it.getLong(idIdx),
                    displayName = it.getString(nameIdx) ?: "Unknown",
                    accountName = it.getString(accountIdx) ?: "",
                    color = it.getInt(colorIdx),
                    enabled = true
                ))
            }
        }

        return calendars
    }

    private fun applyFiltersAndGroup(
        events: List<CalendarEvent>,
        config: WidgetConfig,
        zoneId: ZoneId
    ): Map<LocalDate, List<CalendarEvent>> {
        var filtered = events

        if (config.enabledCalendarIds.isNotEmpty()) {
            filtered = filtered.filter { it.calendarId in config.enabledCalendarIds }
        }

        if (config.keywordFilter.isNotBlank()) {
            filtered = if (config.filterIsRegex) {
                val regex = runCatching {
                    Regex(config.keywordFilter, RegexOption.IGNORE_CASE)
                }.getOrNull()
                if (regex != null) {
                    filtered.filter { event ->
                        val text = "${event.title} ${event.location.orEmpty()} ${event.description.orEmpty()}"
                        regex.containsMatchIn(text)
                    }
                } else filtered
            } else {
                filtered.filter { event ->
                    event.title.contains(config.keywordFilter, ignoreCase = true) ||
                        event.location?.contains(config.keywordFilter, ignoreCase = true) == true
                }
            }
        }

        val grouped = LinkedHashMap<LocalDate, MutableList<CalendarEvent>>()
        for (event in filtered) {
            val date = if (event.allDay) {
                LocalDate.ofEpochDay(event.dtStart / 86_400_000L)
            } else {
                java.time.Instant.ofEpochMilli(event.dtStart).atZone(zoneId).toLocalDate()
            }
            grouped.getOrPut(date) { mutableListOf() }.add(event)
        }

        if (config.showEmptyDays) {
            val today = LocalDate.now()
            val windowBase = if (config.monthOffset == 0) today
                else today.plusMonths(config.monthOffset.toLong()).withDayOfMonth(1)
            repeat(config.daysAheadToLoad) { i ->
                grouped.putIfAbsent(windowBase.plusDays(i.toLong()), mutableListOf())
            }
        }

        return grouped.mapValues { (_, list) ->
            list.sortedWith(compareBy({ !it.allDay }, { it.dtStart }, { it.title }))
        }.entries.sortedBy { it.key }.associate { it.key to it.value }
    }

    private fun extractMeetingUrl(description: String?, location: String?): String? {
        val text = "${description.orEmpty()} ${location.orEmpty()}"
        for (pattern in meetingUrlPatterns) {
            val idx = text.indexOf(pattern)
            if (idx >= 0) {
                val start = text.lastIndexOf("http", idx).takeIf { it >= 0 } ?: idx
                val end = text.indexOfFirst(start) { c -> c == ' ' || c == '\n' || c == '"' || c == ')' }
                return if (end > start) text.substring(start, end) else text.substring(start)
            }
        }
        if (location != null && (location.startsWith("http://") || location.startsWith("https://"))) {
            return location
        }
        return null
    }

    private fun String.indexOfFirst(from: Int, predicate: (Char) -> Boolean): Int {
        for (i in from until length) if (predicate(this[i])) return i
        return length
    }

    private fun extractMapsQuery(location: String?): String? {
        if (location.isNullOrBlank()) return null
        if (location.startsWith("http://") || location.startsWith("https://")) return null
        return location
    }
}
