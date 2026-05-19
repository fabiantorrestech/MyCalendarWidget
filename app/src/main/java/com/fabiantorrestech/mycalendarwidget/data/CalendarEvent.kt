package com.fabiantorrestech.mycalendarwidget.data

data class CalendarEvent(
    val id: Long,       // Instances._ID — unique per occurrence, used for LazyColumn itemId
    val eventId: Long,  // Instances.EVENT_ID — stable parent ID, used for intent URIs
    val title: String,
    val dtStart: Long,
    val dtEnd: Long,
    val allDay: Boolean,
    val location: String?,
    val description: String?,
    val calendarId: Long,
    val calendarColor: Int,
    val eventColor: Int?,
    val selfAttendeeStatus: Int,
    val organizer: String?,
    val eventTimezone: String,
    val hasAlarm: Boolean,
    val rrule: String?,
    val meetingUrl: String?,
    val mapsQuery: String?
) {
    val displayColor: Int get() = eventColor ?: calendarColor
}
