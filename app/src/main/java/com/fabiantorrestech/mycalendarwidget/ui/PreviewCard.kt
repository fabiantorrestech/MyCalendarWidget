package com.fabiantorrestech.mycalendarwidget.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fabiantorrestech.mycalendarwidget.R
import com.fabiantorrestech.mycalendarwidget.data.CalendarEvent
import com.fabiantorrestech.mycalendarwidget.data.HeaderNavStyle
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfig
import com.fabiantorrestech.mycalendarwidget.data.WidgetStyle
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun PreviewCard(
    config: WidgetConfig,
    modifier: Modifier = Modifier
) {
    val eventsByDay = previewSampleEvents()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            PreviewHeader(config)
            Spacer(modifier = Modifier.height(6.dp))

            val suppressFirstMonth = config.showMonthInHeader || config.headerNavEnabled
            val visibleDays = eventsByDay.entries
                .filter { it.key >= LocalDate.now() }
                .take(3)

            if (config.widgetStyle == WidgetStyle.GCAL_LEFT) {
                visibleDays.forEachIndexed { index, (date, events) ->
                    if (index == 0 && !suppressFirstMonth) {
                        PreviewMonthSectionHeader(date, config)
                    }
                    PreviewDayGroupGcalLeft(date, events, config)
                }
            } else {
                visibleDays.forEachIndexed { index, (date, events) ->
                    if (index == 0 && !suppressFirstMonth) {
                        PreviewMonthSectionHeader(date, config)
                    }
                    PreviewDayHeader(date, config)
                    events.take(2).forEach { event ->
                        PreviewEventChip(event, config)
                    }
                }
            }
        }
    }
}

private fun previewSampleEvents(): Map<LocalDate, List<CalendarEvent>> {
    val today = LocalDate.now()
    val zone = ZoneId.systemDefault()

    fun epochMillis(date: LocalDate, hour: Int, minute: Int = 0): Long =
        date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

    val todayEvents = listOf(
        CalendarEvent(
            id = 1L, eventId = 1L,
            title = "Team Standup",
            dtStart = epochMillis(today, 9, 30),
            dtEnd = epochMillis(today, 9, 45),
            allDay = false,
            location = null,
            description = null,
            calendarId = 1L,
            calendarColor = 0xFF1A73E8.toInt(),
            eventColor = null,
            selfAttendeeStatus = 1,
            organizer = null,
            eventTimezone = zone.id,
            hasAlarm = false,
            rrule = null,
            meetingUrl = "https://meet.example.com/standup",
            mapsQuery = null
        ),
        CalendarEvent(
            id = 2L, eventId = 2L,
            title = "Design Review",
            dtStart = epochMillis(today, 11, 0),
            dtEnd = epochMillis(today, 12, 0),
            allDay = false,
            location = "Conference Room A",
            description = null,
            calendarId = 1L,
            calendarColor = 0xFF1A73E8.toInt(),
            eventColor = null,
            selfAttendeeStatus = 1,
            organizer = null,
            eventTimezone = zone.id,
            hasAlarm = true,
            rrule = null,
            meetingUrl = null,
            mapsQuery = "Conference Room A"
        ),
        CalendarEvent(
            id = 3L, eventId = 3L,
            title = "Lunch with Alex",
            dtStart = epochMillis(today, 12, 30),
            dtEnd = epochMillis(today, 13, 30),
            allDay = false,
            location = "The Rooftop Café",
            description = null,
            calendarId = 2L,
            calendarColor = 0xFF0F9D58.toInt(),
            eventColor = null,
            selfAttendeeStatus = 1,
            organizer = null,
            eventTimezone = zone.id,
            hasAlarm = false,
            rrule = null,
            meetingUrl = null,
            mapsQuery = "The Rooftop Café"
        )
    )

    val tomorrow = today.plusDays(1)
    val tomorrowEvents = listOf(
        CalendarEvent(
            id = 4L, eventId = 4L,
            title = "Product Planning",
            dtStart = epochMillis(tomorrow, 10, 0),
            dtEnd = epochMillis(tomorrow, 11, 0),
            allDay = false,
            location = "HQ Board Room",
            description = null,
            calendarId = 1L,
            calendarColor = 0xFF1A73E8.toInt(),
            eventColor = null,
            selfAttendeeStatus = 1,
            organizer = null,
            eventTimezone = zone.id,
            hasAlarm = true,
            rrule = null,
            meetingUrl = null,
            mapsQuery = "HQ Board Room"
        ),
        CalendarEvent(
            id = 5L, eventId = 5L,
            title = "Dentist Appointment",
            dtStart = epochMillis(tomorrow, 15, 0),
            dtEnd = epochMillis(tomorrow, 16, 0),
            allDay = false,
            location = null,
            description = null,
            calendarId = 3L,
            calendarColor = 0xFF9C27B0.toInt(),
            eventColor = null,
            selfAttendeeStatus = 1,
            organizer = null,
            eventTimezone = zone.id,
            hasAlarm = true,
            rrule = null,
            meetingUrl = null,
            mapsQuery = null
        )
    )

    val dayAfter = today.plusDays(2)
    val dayAfterEvents = listOf(
        CalendarEvent(
            id = 6L, eventId = 6L,
            title = "Weekly Retrospective",
            dtStart = epochMillis(dayAfter, 14, 0),
            dtEnd = epochMillis(dayAfter, 15, 0),
            allDay = false,
            location = null,
            description = null,
            calendarId = 1L,
            calendarColor = 0xFF1A73E8.toInt(),
            eventColor = null,
            selfAttendeeStatus = 1,
            organizer = null,
            eventTimezone = zone.id,
            hasAlarm = false,
            rrule = null,
            meetingUrl = "https://meet.example.com/retro",
            mapsQuery = null
        )
    )

    return sortedMapOf(
        today to todayEvents,
        tomorrow to tomorrowEvents,
        dayAfter to dayAfterEvents
    )
}

@Composable
private fun PreviewHeader(config: WidgetConfig) {
    val today = LocalDate.now()
    val monthYear = today.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
    val headerFontSize = (18 * config.typographyScale.headerScale).sp

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            config.headerNavEnabled && config.headerNavStyle == HeaderNavStyle.ARROWS -> {
                // Left arrow placeholder (preview always at offset 0, so left is disabled)
                Spacer(modifier = Modifier.width(32.dp))

                Text(
                    text = monthYear,
                    fontSize = headerFontSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ">",
                        fontSize = (14 * config.typographyScale.headerScale).sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            config.headerNavEnabled && config.headerNavStyle == HeaderNavStyle.CHIPS -> {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (offset in 0..3) {
                        val chipDate = today.plusMonths(offset.toLong())
                        val shortName = chipDate.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                        val isSelected = offset == 0
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .padding(horizontal = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = shortName,
                                fontSize = (11 * config.typographyScale.headerScale).sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (offset < 3) Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            }

            config.showMonthInHeader -> {
                Text(
                    text = monthYear,
                    fontSize = headerFontSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }

            else -> {
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        if (config.showQuickAddFab) {
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    fontSize = (18 * config.typographyScale.headerScale).sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PreviewMonthSectionHeader(date: LocalDate, config: WidgetConfig) {
    Text(
        text = date.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())).uppercase(),
        fontSize = (13 * config.typographyScale.subheaderScale).sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 4.dp)
    )
}

@Composable
private fun PreviewDayHeader(date: LocalDate, config: WidgetConfig) {
    val today = LocalDate.now()
    val label = when (date) {
        today -> "TODAY"
        today.plusDays(1) -> "TOMORROW"
        else -> date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase() +
            ", " + date.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())).uppercase()
    }
    Text(
        text = label,
        fontSize = (11 * config.typographyScale.subheaderScale).sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
    )
}

@Composable
private fun PreviewDayGroupGcalLeft(date: LocalDate, events: List<CalendarEvent>, config: WidgetConfig) {
    val today = LocalDate.now()
    val isToday = date == today
    val dayAbbr = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3)
    val dateNum = date.dayOfMonth.toString()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier
                .width(52.dp)
                .padding(end = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = dayAbbr,
                fontSize = (11 * config.typographyScale.subheaderScale).sp,
                color = MaterialTheme.colorScheme.secondary
            )
            if (isToday) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dateNum,
                        fontSize = (14 * config.typographyScale.dateScale).sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            } else {
                Text(
                    text = dateNum,
                    fontSize = (18 * config.typographyScale.dateScale).sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            events.take(2).forEach { event ->
                PreviewEventChipGcalLeftItem(event, config)
            }
        }
    }
}

@Composable
private fun PreviewEventChipGcalLeftItem(event: CalendarEvent, config: WidgetConfig) {
    val timeLabel = previewTimeRangeLabel(event)
    val dark = isDarkColor(event.displayColor)
    val textPrimary = if (dark) Color.White else Color.Black
    val textSecondary = if (dark) Color.White.copy(alpha = 0.75f) else Color.Black.copy(alpha = 0.65f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(event.displayColor))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = event.title, fontSize = (14 * config.typographyScale.detailScale).sp, fontWeight = FontWeight.Bold, color = textPrimary, maxLines = config.maxTitleLines)
            if (!event.allDay) {
                Text(text = timeLabel, fontSize = (11 * config.typographyScale.dateScale).sp, color = textSecondary)
            }
            if (config.showLocation && event.location != null && event.mapsQuery != null) {
                Text(text = event.location, fontSize = (11 * config.typographyScale.detailScale).sp, color = textSecondary, maxLines = config.maxDetailLines.coerceAtLeast(1))
            }
        }
        if (event.location != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                painter = painterResource(R.drawable.ic_map_pin),
                contentDescription = null,
                tint = textSecondary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun PreviewEventChip(event: CalendarEvent, config: WidgetConfig) {
    if (config.widgetStyle == WidgetStyle.GCAL) {
        PreviewEventChipGcal(event, config)
    } else {
        PreviewEventChipAgenda(event, config)
    }
}

@Composable
private fun PreviewEventChipAgenda(event: CalendarEvent, config: WidgetConfig) {
    val timeLabel = previewTimeLabel(event)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .width(3.dp).height(32.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(event.displayColor))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = timeLabel, fontSize = (11 * config.typographyScale.dateScale).sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = event.title, fontSize = (14 * config.typographyScale.detailScale).sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, maxLines = config.maxTitleLines)
            if (config.showLocation && event.location != null && event.mapsQuery != null) {
                Text(text = event.location, fontSize = (11 * config.typographyScale.detailScale).sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = config.maxDetailLines.coerceAtLeast(1))
            }
        }
        if (event.location != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                painter = painterResource(R.drawable.ic_map_pin),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun PreviewEventChipGcal(event: CalendarEvent, config: WidgetConfig) {
    val timeLabel = previewTimeLabel(event)
    val dark = isDarkColor(event.displayColor)
    val textPrimary = if (dark) Color.White else Color.Black
    val textSecondary = if (dark) Color.White.copy(alpha = 0.75f) else Color.Black.copy(alpha = 0.65f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(event.displayColor))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = timeLabel, fontSize = (10 * config.typographyScale.dateScale).sp, color = textSecondary)
            Text(text = event.title, fontSize = (14 * config.typographyScale.detailScale).sp, fontWeight = FontWeight.Bold, color = textPrimary, maxLines = config.maxTitleLines)
            if (config.showLocation && event.location != null && event.mapsQuery != null) {
                Text(text = event.location, fontSize = (11 * config.typographyScale.detailScale).sp, color = textSecondary, maxLines = config.maxDetailLines.coerceAtLeast(1))
            }
        }
        if (event.location != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                painter = painterResource(R.drawable.ic_map_pin),
                contentDescription = null,
                tint = textSecondary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

private fun previewTimeLabel(event: CalendarEvent): String =
    if (event.allDay) "All day"
    else java.time.Instant.ofEpochMilli(event.dtStart)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))

private fun previewTimeRangeLabel(event: CalendarEvent): String {
    if (event.allDay) return "All day"
    val zone = ZoneId.systemDefault()
    val fmt = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    val start = java.time.Instant.ofEpochMilli(event.dtStart).atZone(zone).format(fmt)
    val end = java.time.Instant.ofEpochMilli(event.dtEnd).atZone(zone).format(fmt)
    return "$start–$end"
}

private fun isDarkColor(colorInt: Int): Boolean {
    val r = (colorInt shr 16 and 0xFF) / 255.0
    val g = (colorInt shr 8 and 0xFF) / 255.0
    val b = (colorInt and 0xFF) / 255.0
    return 0.299 * r + 0.587 * g + 0.114 * b < 0.5
}
