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
import androidx.compose.ui.text.font.FontFamily
import com.fabiantorrestech.mycalendarwidget.data.CalendarEvent
import com.fabiantorrestech.mycalendarwidget.data.CycleUiStyle
import com.fabiantorrestech.mycalendarwidget.data.FontCategory
import com.fabiantorrestech.mycalendarwidget.data.HeaderNavStyle
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfig
import com.fabiantorrestech.mycalendarwidget.data.WidgetFont
import com.fabiantorrestech.mycalendarwidget.data.WidgetProfileEntry
import com.fabiantorrestech.mycalendarwidget.data.WidgetStyle
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun PreviewCard(
    config: WidgetConfig,
    eventsByDay: Map<LocalDate, List<CalendarEvent>>,
    profiles: List<WidgetProfileEntry> = emptyList(),
    activeProfileId: String = "",
    cycleUiStyle: CycleUiStyle = CycleUiStyle.PILL,
    modifier: Modifier = Modifier
) {
    val rootPadding = if (config.strictGridMode) 0.dp else 12.dp
    val floatingMode = !config.showMonthInHeader && !config.headerNavEnabled
    val suppressFirstMonth = config.showMonthInHeader || config.headerNavEnabled
    val floatingContentTopInset = previewFloatingContentTopInset(config, profiles, cycleUiStyle)
    val visibleDays = eventsByDay.entries
        .filter { it.key >= LocalDate.now() }
        .take(3)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        if (floatingMode) {
            Box(modifier = Modifier.padding(rootPadding)) {
                Column(modifier = Modifier.padding(top = floatingContentTopInset)) {
                    PreviewEventList(config, visibleDays, suppressFirstMonth)
                }
                PreviewFloatingControlsOverlay(
                    config = config,
                    profiles = profiles,
                    activeProfileId = activeProfileId,
                    cycleUiStyle = cycleUiStyle,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }
        } else {
            Column(modifier = Modifier.padding(rootPadding)) {
                PreviewHeader(config, profiles, activeProfileId, cycleUiStyle)
                Spacer(modifier = Modifier.height(6.dp))
                PreviewEventList(config, visibleDays, suppressFirstMonth)
            }
        }
    }
}

@Composable
private fun PreviewEventList(
    config: WidgetConfig,
    visibleDays: List<Map.Entry<LocalDate, List<CalendarEvent>>>,
    suppressFirstMonth: Boolean
) {
    if (visibleDays.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(80.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No upcoming events",
                fontSize = (14 * config.typographyScale.detailScale).sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = config.previewFont(FontCategory.DETAIL)
            )
        }
    } else if (config.widgetStyle == WidgetStyle.GCAL_LEFT) {
        visibleDays.forEachIndexed { index, (date, events) ->
            if (index == 0 && !suppressFirstMonth) PreviewMonthSectionHeader(date, config)
            PreviewDayGroupGcalLeft(date, events, config)
        }
    } else {
        visibleDays.forEachIndexed { index, (date, events) ->
            if (index == 0 && !suppressFirstMonth) PreviewMonthSectionHeader(date, config)
            PreviewDayHeader(date, config)
            events.take(2).forEach { event -> PreviewEventChip(event, config) }
        }
    }
}

@Composable
private fun PreviewFloatingControlsOverlay(
    config: WidgetConfig,
    profiles: List<WidgetProfileEntry>,
    activeProfileId: String,
    cycleUiStyle: CycleUiStyle,
    modifier: Modifier = Modifier
) {
    val showCycler = profiles.size >= 2
    val floatingCycleUiStyle = previewFloatingCycleUiStyle(config.widgetStyle, cycleUiStyle)
    val showTabs = showCycler && floatingCycleUiStyle == CycleUiStyle.TABS

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        if (showTabs) {
            PreviewProfileTabs(profiles, activeProfileId, transparentBackground = true)
            Spacer(modifier = Modifier.height(2.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showCycler && !showTabs) {
                PreviewInlineProfileSwitcher(
                    profiles,
                    activeProfileId,
                    floatingCycleUiStyle,
                    transparentBackground = true
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Spacer(modifier = Modifier.weight(1f))
            if (config.showRefreshButton) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "↺",
                        fontSize = (14 * config.typographyScale.headerScale).sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = config.previewFont(FontCategory.MONTH_HEADER)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_calendar_open),
                    contentDescription = "Open calendar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
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
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = config.previewFont(FontCategory.MONTH_HEADER)
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewHeader(
    config: WidgetConfig,
    profiles: List<WidgetProfileEntry>,
    activeProfileId: String,
    cycleUiStyle: CycleUiStyle
) {
    val today = LocalDate.now()
    val displayDate = if (config.monthOffset == 0) today
    else today.plusMonths(config.monthOffset.toLong()).withDayOfMonth(1)
    val monthYear = displayDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
    val headerFontSize = (18 * config.typographyScale.headerScale).sp
    val showCycler = profiles.size >= 2

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                config.headerNavEnabled && config.headerNavStyle == HeaderNavStyle.ARROWS -> {
                    Spacer(modifier = Modifier.width(32.dp))

                    Text(
                        text = monthYear,
                        fontSize = headerFontSize,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = config.previewFont(FontCategory.MONTH_HEADER),
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
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = config.previewFont(FontCategory.MONTH_HEADER)
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
                            val isSelected = config.monthOffset == offset
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
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = config.previewFont(FontCategory.MONTH_HEADER)
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
                        fontFamily = config.previewFont(FontCategory.MONTH_HEADER),
                        modifier = Modifier.weight(1f)
                    )
                }

                else -> {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            if (showCycler && cycleUiStyle != CycleUiStyle.TABS) {
                Spacer(modifier = Modifier.width(4.dp))
                PreviewInlineProfileSwitcher(profiles, activeProfileId, cycleUiStyle)
            }

            if (config.showRefreshButton) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "↺",
                        fontSize = (14 * config.typographyScale.headerScale).sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = config.previewFont(FontCategory.MONTH_HEADER)
                    )
                }
            }

            if (!config.showMonthInHeader) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_calendar_open),
                        contentDescription = "Open calendar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
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
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = config.previewFont(FontCategory.MONTH_HEADER)
                    )
                }
            }
        }

        if (showCycler && cycleUiStyle == CycleUiStyle.TABS) {
            Spacer(modifier = Modifier.height(2.dp))
            PreviewProfileTabs(profiles, activeProfileId)
        }
    }
}

@Composable
private fun PreviewInlineProfileSwitcher(
    profiles: List<WidgetProfileEntry>,
    activeProfileId: String,
    cycleUiStyle: CycleUiStyle,
    transparentBackground: Boolean = false
) {
    when (cycleUiStyle) {
        CycleUiStyle.PILL -> PreviewProfilePill(profiles, activeProfileId, transparentBackground)
        CycleUiStyle.DOTS -> PreviewProfileDots(profiles, activeProfileId)
        CycleUiStyle.TABS -> {}
    }
}

@Composable
private fun PreviewProfilePill(
    profiles: List<WidgetProfileEntry>,
    activeProfileId: String,
    transparentBackground: Boolean = false
) {
    val active = profiles.firstOrNull { it.id == activeProfileId } ?: profiles.firstOrNull() ?: return
    Row(verticalAlignment = Alignment.CenterVertically) {
        PreviewProfileChevron("<", transparentBackground)
        Spacer(modifier = Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .then(
                    if (transparentBackground) {
                        Modifier.padding(horizontal = 4.dp, vertical = 3.dp)
                    } else {
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = active.name.take(10),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        PreviewProfileChevron(">", transparentBackground)
    }
}

@Composable
private fun PreviewProfileChevron(
    label: String,
    transparentBackground: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .then(
                if (transparentBackground) {
                    Modifier
                } else {
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun PreviewProfileDots(profiles: List<WidgetProfileEntry>, activeProfileId: String) {
    val resolvedActiveId = resolveActiveProfileId(profiles, activeProfileId)
    Row(verticalAlignment = Alignment.CenterVertically) {
        profiles.forEachIndexed { index, profile ->
            val isActive = profile.id == resolvedActiveId
            Box(
                modifier = Modifier
                    .width(if (isActive) 18.dp else 7.dp)
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            )
            if (index < profiles.lastIndex) Spacer(modifier = Modifier.width(5.dp))
        }
    }
}

@Composable
private fun PreviewProfileTabs(
    profiles: List<WidgetProfileEntry>,
    activeProfileId: String,
    transparentBackground: Boolean = false
) {
    val resolvedActiveId = resolveActiveProfileId(profiles, activeProfileId)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        profiles.forEachIndexed { index, profile ->
            val isActive = profile.id == resolvedActiveId
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp)
                    .then(
                        if (transparentBackground) {
                            Modifier.padding(horizontal = 2.dp)
                        } else {
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isActive) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .padding(horizontal = 2.dp)
                        }
                    )
                ,
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = profile.name,
                    fontSize = 10.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = if (transparentBackground) {
                        if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1
                )
            }
            if (index < profiles.lastIndex) Spacer(modifier = Modifier.width(2.dp))
        }
    }
}

private fun previewFloatingCycleUiStyle(
    widgetStyle: WidgetStyle,
    cycleUiStyle: CycleUiStyle
): CycleUiStyle =
    if (cycleUiStyle == CycleUiStyle.TABS && widgetStyle != WidgetStyle.GCAL_LEFT) {
        CycleUiStyle.DOTS
    } else {
        cycleUiStyle
    }

private fun previewFloatingContentTopInset(
    config: WidgetConfig,
    profiles: List<WidgetProfileEntry>,
    cycleUiStyle: CycleUiStyle
) = when {
    profiles.size < 2 -> 0.dp
    previewFloatingCycleUiStyle(config.widgetStyle, cycleUiStyle) == CycleUiStyle.TABS -> 64.dp
    else -> 34.dp
}

private fun resolveActiveProfileId(
    profiles: List<WidgetProfileEntry>,
    activeProfileId: String
): String = profiles.firstOrNull { it.id == activeProfileId }?.id
    ?: profiles.firstOrNull()?.id
    .orEmpty()

@Composable
private fun PreviewMonthSectionHeader(date: LocalDate, config: WidgetConfig) {
    Text(
        text = date.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())).uppercase(),
        fontSize = (13 * config.typographyScale.subheaderScale).sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        fontFamily = config.previewFont(FontCategory.WEEKDAY_HEADER),
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
        fontFamily = config.previewFont(FontCategory.WEEKDAY_HEADER),
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
                color = MaterialTheme.colorScheme.secondary,
                fontFamily = config.previewFont(FontCategory.WEEKDAY_HEADER)
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
                        fontSize = (14 * config.typographyScale.dateHeaderScale).sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontFamily = config.previewFont(FontCategory.DATE_HEADER)
                    )
                }
            } else {
                Text(
                    text = dateNum,
                    fontSize = (18 * config.typographyScale.dateHeaderScale).sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = config.previewFont(FontCategory.DATE_HEADER)
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
            Text(text = event.title, fontSize = (14 * config.typographyScale.eventNameScale).sp, fontWeight = FontWeight.Bold, color = textPrimary, fontFamily = config.previewFont(FontCategory.EVENT_NAME), maxLines = config.maxTitleLines)
            if (!event.allDay) {
                Text(text = timeLabel, fontSize = (11 * config.typographyScale.eventTimeScale).sp, color = textSecondary, fontFamily = config.previewFont(FontCategory.EVENT_TIME))
            }
            if (config.showLocation && event.location != null && event.mapsQuery != null) {
                Text(text = event.location, fontSize = (11 * config.typographyScale.detailScale).sp, color = textSecondary, fontFamily = config.previewFont(FontCategory.DETAIL), maxLines = config.maxDetailLines.coerceAtLeast(1))
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
            Text(text = timeLabel, fontSize = (11 * config.typographyScale.eventTimeScale).sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = config.previewFont(FontCategory.EVENT_TIME))
            Text(text = event.title, fontSize = (14 * config.typographyScale.eventNameScale).sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, fontFamily = config.previewFont(FontCategory.EVENT_NAME), maxLines = config.maxTitleLines)
            if (config.showLocation && event.location != null && event.mapsQuery != null) {
                Text(text = event.location, fontSize = (11 * config.typographyScale.detailScale).sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = config.previewFont(FontCategory.DETAIL), maxLines = config.maxDetailLines.coerceAtLeast(1))
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
            Text(text = timeLabel, fontSize = (10 * config.typographyScale.eventTimeScale).sp, color = textSecondary, fontFamily = config.previewFont(FontCategory.EVENT_TIME))
            Text(text = event.title, fontSize = (14 * config.typographyScale.eventNameScale).sp, fontWeight = FontWeight.Bold, color = textPrimary, fontFamily = config.previewFont(FontCategory.EVENT_NAME), maxLines = config.maxTitleLines)
            if (config.showLocation && event.location != null && event.mapsQuery != null) {
                Text(text = event.location, fontSize = (11 * config.typographyScale.detailScale).sp, color = textSecondary, fontFamily = config.previewFont(FontCategory.DETAIL), maxLines = config.maxDetailLines.coerceAtLeast(1))
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

private fun WidgetFont.toComposeFontFamily(): FontFamily = when (this) {
    WidgetFont.SERIF -> FontFamily.Serif
    WidgetFont.MONOSPACE -> FontFamily.Monospace
    else -> FontFamily.Default
}

private fun WidgetConfig.previewFont(category: FontCategory): FontFamily =
    fontConfig.resolve(category).toComposeFontFamily()

private fun isDarkColor(colorInt: Int): Boolean {
    val r = (colorInt shr 16 and 0xFF) / 255.0
    val g = (colorInt shr 8 and 0xFF) / 255.0
    val b = (colorInt and 0xFF) / 255.0
    return 0.299 * r + 0.587 * g + 0.114 * b < 0.5
}
