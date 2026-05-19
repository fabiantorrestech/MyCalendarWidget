package com.fabiantorrestech.mycalendarwidget.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import com.fabiantorrestech.mycalendarwidget.data.HeaderNavStyle
import com.fabiantorrestech.mycalendarwidget.data.WidgetStyle
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.ColorFilter
import androidx.glance.unit.ColorProvider
import com.fabiantorrestech.mycalendarwidget.R
import com.fabiantorrestech.mycalendarwidget.data.CalendarEvent
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfig
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JvmTextStyle
import java.util.Locale

@Composable
fun BridgeCalWidgetContent(
    eventsByDay: Map<LocalDate, List<CalendarEvent>>,
    config: WidgetConfig,
    context: Context,
    glanceId: GlanceId
) {
    val rootPadding = if (config.strictGridMode) 0.dp else 8.dp

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(16.dp)
            .padding(rootPadding)
    ) {
        WidgetHeader(config, context)
        Spacer(modifier = GlanceModifier.height(4.dp))

        if (eventsByDay.isEmpty()) {
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No upcoming events",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = (14 * config.typographyScale.detailScale).sp
                    )
                )
            }
        } else {
            // Suppress the first month's inline header only when the month is already
            // visible in the widget header (via static title or nav). When both are off,
            // the list shows all month headers — matching Google Calendar's style.
            val suppressFirstMonth = config.showMonthInHeader || config.headerNavEnabled
            val firstDisplayedMonth = if (suppressFirstMonth) {
                eventsByDay.keys.firstOrNull()?.let { YearMonth.of(it.year, it.month) }
            } else null

            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                if (config.widgetStyle == WidgetStyle.GCAL_LEFT) {
                    var lastMonth: java.time.Month? = null
                    eventsByDay.forEach { (date, events) ->
                        if (date.month != lastMonth) {
                            val ym = YearMonth.of(date.year, date.month)
                            if (ym != firstDisplayedMonth) {
                                item(itemId = date.toEpochDay() * 1000 + 999) {
                                    MonthSectionHeader(date, config)
                                }
                            }
                            lastMonth = date.month
                        }
                        item(itemId = date.toEpochDay()) {
                            DayGroupGcalLeft(date, events, config, context)
                        }
                    }
                } else {
                    var lastMonth: java.time.Month? = null
                    eventsByDay.forEach { (date, events) ->
                        if (date.month != lastMonth) {
                            val ym = YearMonth.of(date.year, date.month)
                            if (ym != firstDisplayedMonth) {
                                item(itemId = date.toEpochDay() * 1000 + 999) {
                                    MonthSectionHeader(date, config)
                                }
                            }
                            lastMonth = date.month
                        }
                        item(itemId = date.toEpochDay()) {
                            DayHeader(date, config)
                        }
                        items(events, itemId = { it.id }) { event ->
                            EventChip(event, config, context)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetHeader(config: WidgetConfig, context: Context) {
    val today = LocalDate.now()
    val displayDate = if (config.monthOffset == 0) today
        else today.plusMonths(config.monthOffset.toLong()).withDayOfMonth(1)
    val monthYear = displayDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))

    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            config.headerNavEnabled && config.headerNavStyle == HeaderNavStyle.ARROWS -> {
                // Left arrow — hidden (zero-width placeholder) when at offset 0
                if (config.monthOffset > 0) {
                    Box(
                        modifier = GlanceModifier
                            .width(28.dp)
                            .height(28.dp)
                            .background(GlanceTheme.colors.surfaceVariant)
                            .cornerRadius(14.dp)
                            .clickable(actionRunCallback<UpdateMonthOffsetAction>(
                                actionParametersOf(targetOffsetKey to (config.monthOffset - 1))
                            )),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "<",
                            style = TextStyle(
                                color = GlanceTheme.colors.primary,
                                fontSize = (14 * config.typographyScale.headerScale).sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Spacer(modifier = GlanceModifier.width(4.dp))
                } else {
                    Spacer(modifier = GlanceModifier.width(32.dp))
                }

                Text(
                    text = monthYear,
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = (18 * config.typographyScale.headerScale).sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier
                        .defaultWeight()
                        .clickable(actionStartActivity(WidgetClickActions.headerIntent(config)))
                )

                Spacer(modifier = GlanceModifier.width(4.dp))
                Box(
                    modifier = GlanceModifier
                        .width(28.dp)
                        .height(28.dp)
                        .background(GlanceTheme.colors.surfaceVariant)
                        .cornerRadius(14.dp)
                        .clickable(actionRunCallback<UpdateMonthOffsetAction>(
                            actionParametersOf(targetOffsetKey to (config.monthOffset + 1))
                        )),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ">",
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = (14 * config.typographyScale.headerScale).sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            config.headerNavEnabled && config.headerNavStyle == HeaderNavStyle.CHIPS -> {
                Row(
                    modifier = GlanceModifier.defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (offset in 0..3) {
                        val chipDate = today.plusMonths(offset.toLong())
                        val shortName = chipDate.month
                            .getDisplayName(JvmTextStyle.SHORT, Locale.getDefault())
                        val isSelected = config.monthOffset == offset
                        Box(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .height(28.dp)
                                .background(
                                    if (isSelected) GlanceTheme.colors.primaryContainer
                                    else GlanceTheme.colors.surfaceVariant
                                )
                                .cornerRadius(14.dp)
                                .padding(horizontal = 2.dp)
                                .clickable(actionRunCallback<UpdateMonthOffsetAction>(
                                    actionParametersOf(targetOffsetKey to offset)
                                )),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = shortName,
                                style = TextStyle(
                                    color = if (isSelected) GlanceTheme.colors.onPrimaryContainer
                                            else GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = (11 * config.typographyScale.headerScale).sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                        if (offset < 3) Spacer(modifier = GlanceModifier.width(4.dp))
                    }
                }
            }

            config.showMonthInHeader -> {
                Text(
                    text = monthYear,
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = (18 * config.typographyScale.headerScale).sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier
                        .defaultWeight()
                        .clickable(actionStartActivity(WidgetClickActions.headerIntent(config)))
                )
            }

            else -> {
                Spacer(modifier = GlanceModifier.defaultWeight())
            }
        }

        if (config.showRefreshButton) {
            Spacer(modifier = GlanceModifier.width(4.dp))
            Box(
                modifier = GlanceModifier
                    .size(28.dp)
                    .background(GlanceTheme.colors.surfaceVariant)
                    .cornerRadius(14.dp)
                    .clickable(actionRunCallback<RefreshWidgetAction>()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "↺",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = (14 * config.typographyScale.headerScale).sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        if (config.showQuickAddFab) {
            Spacer(modifier = GlanceModifier.width(4.dp))
            Box(
                modifier = GlanceModifier
                    .width(56.dp)
                    .height(36.dp)
                    .background(GlanceTheme.colors.primaryContainer)
                    .cornerRadius(18.dp)
                    .clickable(actionStartActivity(WidgetClickActions.quickAddIntent())),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_add),
                    contentDescription = "Add event",
                    modifier = GlanceModifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun MonthSectionHeader(date: LocalDate, config: WidgetConfig) {
    Text(
        text = date.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())).uppercase(),
        style = TextStyle(
            color = GlanceTheme.colors.primary,
            fontSize = (13 * config.typographyScale.subheaderScale).sp,
            fontWeight = FontWeight.Bold
        ),
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 20.dp, bottom = 4.dp)
    )
}

@Composable
private fun DayHeader(date: LocalDate, config: WidgetConfig) {
    val today = LocalDate.now()
    val label = when (date) {
        today -> "TODAY"
        today.plusDays(1) -> "TOMORROW"
        else -> date.dayOfWeek.getDisplayName(JvmTextStyle.SHORT, Locale.getDefault()).uppercase() +
            ", " + date.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())).uppercase()
    }
    Text(
        text = label,
        style = TextStyle(
            color = GlanceTheme.colors.secondary,
            fontSize = (11 * config.typographyScale.subheaderScale).sp,
            fontWeight = FontWeight.Bold
        ),
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 16.dp, bottom = 2.dp)
    )
}

@Composable
private fun EventChip(event: CalendarEvent, config: WidgetConfig, context: Context) {
    if (config.widgetStyle == WidgetStyle.GCAL) {
        EventChipGcal(event, config, context)
    } else {
        EventChipAgenda(event, config, context)
    }
}

@Composable
private fun EventChipAgenda(event: CalendarEvent, config: WidgetConfig, context: Context) {
    val intent = WidgetClickActions.eventIntent(event, config)
    val timeLabel = eventTimeLabel(event)
    val hasDetail = (config.showLocation && event.location != null && event.mapsQuery != null) ||
            (config.showDescription && event.description != null && config.maxDetailLines > 0)
    val bottomPad = if (hasDetail) 6.dp else 3.dp

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = bottomPad, start = 4.dp, end = 4.dp)
            .clickable(actionStartActivity(intent)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier
                .width(3.dp)
                .height(36.dp)
                .background(ColorProvider(androidx.compose.ui.graphics.Color(event.displayColor)))
        ) {}

        Spacer(modifier = GlanceModifier.width(6.dp))

        Column(modifier = GlanceModifier.defaultWeight()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = timeLabel,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = (11 * config.typographyScale.dateScale).sp
                    )
                )
                if (event.meetingUrl != null) {
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = "• VIDEO",
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = (10 * config.typographyScale.dateScale).sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
            Text(
                text = event.title,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = (14 * config.typographyScale.detailScale).sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = config.maxTitleLines
            )
            if (config.showLocation && event.location != null && event.mapsQuery != null) {
                Text(
                    text = event.location,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = (11 * config.typographyScale.detailScale).sp
                    ),
                    maxLines = config.maxDetailLines.coerceAtLeast(1)
                )
            }
            if (config.showDescription && event.description != null && config.maxDetailLines > 0) {
                Text(
                    text = event.description,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = (11 * config.typographyScale.detailScale).sp
                    ),
                    maxLines = config.maxDetailLines
                )
            }
        }

        if (event.location != null) {
            Spacer(modifier = GlanceModifier.width(4.dp))
            Image(
                provider = ImageProvider(R.drawable.ic_map_pin),
                contentDescription = null,
                modifier = GlanceModifier.size(32.dp),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant)
            )
        }
    }
}

@Composable
private fun EventChipGcal(event: CalendarEvent, config: WidgetConfig, context: Context) {
    val intent = WidgetClickActions.eventIntent(event, config)
    val timeLabel = eventTimeLabel(event)
    val dark = isDarkColor(event.displayColor)
    // Pre-multiplied colors avoid alpha compositing issues in RemoteViews
    val textPrimary = ColorProvider(
        if (dark) androidx.compose.ui.graphics.Color.White
        else androidx.compose.ui.graphics.Color.Black
    )
    val textSecondary = ColorProvider(
        if (dark) androidx.compose.ui.graphics.Color(0xCCFFFFFF.toInt())
        else androidx.compose.ui.graphics.Color(0xCC000000.toInt())
    )
    val hasDetail = (config.showLocation && event.location != null && event.mapsQuery != null) ||
            (config.showDescription && event.description != null && config.maxDetailLines > 0)
    val innerBottomPad = if (hasDetail) 5.dp else 3.dp

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .background(ColorProvider(androidx.compose.ui.graphics.Color(event.displayColor)))
            .cornerRadius(8.dp)
            .clickable(actionStartActivity(intent))
            .padding(start = 8.dp, end = 8.dp, top = 5.dp, bottom = innerBottomPad),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = timeLabel,
                    style = TextStyle(color = textSecondary, fontSize = (10 * config.typographyScale.dateScale).sp)
                )
                if (event.meetingUrl != null) {
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = "• VIDEO",
                        style = TextStyle(color = textPrimary, fontSize = (10 * config.typographyScale.dateScale).sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
            Text(
                text = event.title,
                style = TextStyle(
                    color = textPrimary,
                    fontSize = (14 * config.typographyScale.detailScale).sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = config.maxTitleLines
            )
            if (config.showLocation && event.location != null && event.mapsQuery != null) {
                Text(
                    text = event.location,
                    style = TextStyle(color = textSecondary, fontSize = (11 * config.typographyScale.detailScale).sp),
                    maxLines = config.maxDetailLines.coerceAtLeast(1)
                )
            }
            if (config.showDescription && event.description != null && config.maxDetailLines > 0) {
                Text(
                    text = event.description,
                    style = TextStyle(color = textSecondary, fontSize = (11 * config.typographyScale.detailScale).sp),
                    maxLines = config.maxDetailLines
                )
            }
        }

        if (event.location != null) {
            Spacer(modifier = GlanceModifier.width(4.dp))
            Image(
                provider = ImageProvider(R.drawable.ic_map_pin),
                contentDescription = null,
                modifier = GlanceModifier.size(32.dp),
                colorFilter = ColorFilter.tint(textSecondary)
            )
        }
    }
}

@Composable
private fun DayGroupGcalLeft(
    date: LocalDate,
    events: List<CalendarEvent>,
    config: WidgetConfig,
    context: Context
) {
    val today = LocalDate.now()
    val isToday = date == today
    val dayAbbr = date.dayOfWeek.getDisplayName(JvmTextStyle.SHORT, Locale.getDefault()).take(3)
    val dateNum = date.dayOfMonth.toString()

    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = GlanceModifier.width(52.dp).padding(end = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = dayAbbr,
                style = TextStyle(
                    color = GlanceTheme.colors.secondary,
                    fontSize = (11 * config.typographyScale.subheaderScale).sp
                )
            )
            if (isToday) {
                Box(
                    modifier = GlanceModifier
                        .size(32.dp)
                        .background(GlanceTheme.colors.primary)
                        .cornerRadius(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dateNum,
                        style = TextStyle(
                            color = GlanceTheme.colors.onPrimary,
                            fontSize = (14 * config.typographyScale.dateScale).sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            } else {
                Text(
                    text = dateNum,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = (18 * config.typographyScale.dateScale).sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Column(modifier = GlanceModifier.defaultWeight()) {
            events.forEach { event ->
                EventChipGcalLeftItem(event, config, context)
            }
        }
    }
}

@Composable
private fun EventChipGcalLeftItem(event: CalendarEvent, config: WidgetConfig, context: Context) {
    val intent = WidgetClickActions.eventIntent(event, config)
    val timeLabel = eventTimeRangeLabel(event)
    val dark = isDarkColor(event.displayColor)
    val textPrimary = ColorProvider(
        if (dark) androidx.compose.ui.graphics.Color.White
        else androidx.compose.ui.graphics.Color.Black
    )
    val textSecondary = ColorProvider(
        if (dark) androidx.compose.ui.graphics.Color(0xCCFFFFFF.toInt())
        else androidx.compose.ui.graphics.Color(0xCC000000.toInt())
    )
    val hasDetail = (config.showLocation && event.location != null && event.mapsQuery != null) ||
            (config.showDescription && event.description != null && config.maxDetailLines > 0)
    val innerBottomPad = if (hasDetail) 5.dp else 3.dp

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .background(ColorProvider(androidx.compose.ui.graphics.Color(event.displayColor)))
            .cornerRadius(8.dp)
            .clickable(actionStartActivity(intent))
            .padding(start = 8.dp, end = 8.dp, top = 5.dp, bottom = innerBottomPad),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = event.title,
                style = TextStyle(
                    color = textPrimary,
                    fontSize = (14 * config.typographyScale.detailScale).sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = config.maxTitleLines
            )
            if (!event.allDay) {
                Text(
                    text = timeLabel,
                    style = TextStyle(
                        color = textSecondary,
                        fontSize = (11 * config.typographyScale.dateScale).sp
                    )
                )
            }
            if (config.showLocation && event.location != null && event.mapsQuery != null) {
                Text(
                    text = event.location,
                    style = TextStyle(color = textSecondary, fontSize = (11 * config.typographyScale.detailScale).sp),
                    maxLines = config.maxDetailLines.coerceAtLeast(1)
                )
            }
            if (config.showDescription && event.description != null && config.maxDetailLines > 0) {
                Text(
                    text = event.description,
                    style = TextStyle(color = textSecondary, fontSize = (11 * config.typographyScale.detailScale).sp),
                    maxLines = config.maxDetailLines
                )
            }
        }

        if (event.location != null) {
            Spacer(modifier = GlanceModifier.width(4.dp))
            Image(
                provider = ImageProvider(R.drawable.ic_map_pin),
                contentDescription = null,
                modifier = GlanceModifier.size(32.dp),
                colorFilter = ColorFilter.tint(textSecondary)
            )
        }
    }
}

private fun eventTimeLabel(event: CalendarEvent): String =
    if (event.allDay) "All day"
    else java.time.Instant.ofEpochMilli(event.dtStart)
        .atZone(java.time.ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))

private fun eventTimeRangeLabel(event: CalendarEvent): String {
    if (event.allDay) return "All day"
    val zone = java.time.ZoneId.systemDefault()
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
