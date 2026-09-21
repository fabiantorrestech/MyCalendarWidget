package com.fabiantorrestech.mycalendarwidget.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
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
import com.fabiantorrestech.mycalendarwidget.data.density.DensityCalculator
import com.fabiantorrestech.mycalendarwidget.data.density.DensitySnapshot
import com.fabiantorrestech.mycalendarwidget.widget.density.AxisSpec
import com.fabiantorrestech.mycalendarwidget.data.density.ColorMath
import com.fabiantorrestech.mycalendarwidget.widget.density.DensityCanvas
import com.fabiantorrestech.mycalendarwidget.widget.density.DensityLayout
import com.fabiantorrestech.mycalendarwidget.widget.density.SECOND_LINE_SEPARATOR
import com.fabiantorrestech.mycalendarwidget.widget.density.DensityLayout.ChromePlacement
import com.fabiantorrestech.mycalendarwidget.widget.density.DensityPalette
import com.fabiantorrestech.mycalendarwidget.widget.density.DensitySpecBuilder
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.fabiantorrestech.mycalendarwidget.data.DensityPeekFormat
import com.fabiantorrestech.mycalendarwidget.data.TimeFormat
import com.fabiantorrestech.mycalendarwidget.widget.peek.PeekItemKind
import com.fabiantorrestech.mycalendarwidget.widget.peek.PeekLabels
import com.fabiantorrestech.mycalendarwidget.widget.peek.PeekList
import com.fabiantorrestech.mycalendarwidget.widget.peek.PeekTypography
import com.fabiantorrestech.mycalendarwidget.widget.peek.PASSED_ALPHA
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
    densitySnapshot: DensitySnapshot? = null,
    use24Hour: Boolean = false,
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        // Mirrors the widget's own exhaustive dispatch: a new style must be given an arm here too.
        when (config.widgetStyle) {
            // The density style lays itself out against the widget's own breakpoints, so
            // the card has to hand it a measured size the way LocalSize does on the launcher.
            WidgetStyle.DENSITY -> BoxWithConstraints {
                PreviewDensityContent(
                    snapshot = densitySnapshot,
                    config = config,
                    use24Hour = use24Hour,
                    widthDp = maxWidth,
                    // Unconstrained in a scrolling settings page; assume a two-row widget.
                    heightDp = if (maxHeight.value.isFinite()) maxHeight else PREVIEW_DENSITY_HEIGHT
                )
            }

            WidgetStyle.AGENDA, WidgetStyle.GCAL, WidgetStyle.GCAL_LEFT ->
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
    } else {
        when (config.widgetStyle) {
            WidgetStyle.GCAL_LEFT -> visibleDays.forEachIndexed { index, (date, events) ->
                if (index == 0 && !suppressFirstMonth) PreviewMonthSectionHeader(date, config)
                PreviewDayGroupGcalLeft(date, events, config)
            }

            WidgetStyle.AGENDA, WidgetStyle.GCAL -> visibleDays.forEachIndexed { index, (date, events) ->
                if (index == 0 && !suppressFirstMonth) PreviewMonthSectionHeader(date, config)
                PreviewDayHeader(date, config)
                events.take(2).forEach { event -> PreviewEventChip(event, config) }
            }

            WidgetStyle.DENSITY -> error("DENSITY must not reach PreviewEventList")
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

/** What the card assumes when the settings page gives it no height of its own. */
private val PREVIEW_DENSITY_HEIGHT = 200.dp

/**
 * Compose mirror of the density peek sheet (`PeekOverlay`): the "Upcoming" bar, then the
 * same rows [PeekList] builds for the widget, at the same [DensityLayout.PEEK_*] sizes and
 * the same scale/font fields, so the appearance sliders can be judged against it. Inert:
 * nothing here closes or opens anything.
 */
@Composable
fun PeekPreviewCard(
    config: WidgetConfig,
    eventsByDay: Map<LocalDate, List<CalendarEvent>>,
    use24Hour: Boolean,
    modifier: Modifier = Modifier
) {
    val zone = ZoneId.systemDefault()
    val locale = Locale.getDefault()
    val (today, items) = remember(eventsByDay, config.densityPeekFormat) {
        val today = LocalDate.now(zone)
        val upcoming = PeekList.upcoming(eventsByDay, today)
        today to PeekList.items(upcoming, config.densityPeekFormat, System.currentTimeMillis())
    }
    val timeColumn = if (use24Hour) DensityLayout.PEEK_TIME_COL_24H_DP else DensityLayout.PEEK_TIME_COL_12H_DP

    Card(
        modifier = modifier.fillMaxWidth().height(PREVIEW_DENSITY_HEIGHT),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(DensityLayout.WIDGET_PADDING_DP.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(DensityLayout.PEEK_TOP_BAR_DP.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.peek_upcoming),
                    fontSize = PeekTypography.TOP_BAR_SP.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(9.dp)
                    )
                }
            }
            if (items.isEmpty()) {
                Text(
                    text = stringResource(R.string.peek_empty),
                    fontSize = (PeekTypography.TITLE_SP * config.typographyScale.eventNameScale).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = config.previewFont(FontCategory.EVENT_NAME),
                    modifier = Modifier.fillMaxWidth().height(DensityLayout.PEEK_ROW_DP.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    items(items, key = { it.itemId }) { item ->
                        when (item.kind) {
                            PeekItemKind.HEADER -> PreviewPeekHeader(item.date, today, config, locale)
                            PeekItemKind.SEPARATOR -> {
                                Spacer(modifier = Modifier.height(1.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(DensityLayout.PEEK_SEPARATOR_DP.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant)
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                            }
                            PeekItemKind.EVENT -> item.event?.let { event ->
                                PreviewPeekEventRow(event, item.date, item.passed, config, use24Hour, zone, locale, timeColumn)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewPeekHeader(date: LocalDate, today: LocalDate, config: WidgetConfig, locale: Locale) {
    val label = PeekLabels.dateHeader(
        date = date,
        today = today,
        todayWord = stringResource(R.string.peek_today),
        tomorrowWord = stringResource(R.string.peek_tomorrow),
        locale = locale
    )
    val fontSize = (PeekTypography.HEADER_SP * config.typographyScale.subheaderScale).sp
    val fontFamily = config.previewFont(FontCategory.WEEKDAY_HEADER)
    Row(
        modifier = Modifier.fillMaxWidth().height(DensityLayout.PEEK_HEADER_DP.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (PeekLabels.isToday(date, today)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(DensityLayout.PEEK_TODAY_PILL_RADIUS_DP.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = DensityLayout.PEEK_TODAY_PILL_INSET_DP.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontFamily = fontFamily,
                    maxLines = 1
                )
            }
        } else {
            Text(
                text = label,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                fontFamily = fontFamily,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PreviewPeekEventRow(
    event: CalendarEvent,
    date: LocalDate,
    passed: Boolean,
    config: WidgetConfig,
    use24Hour: Boolean,
    zone: ZoneId,
    locale: Locale,
    timeColumnDp: Float
) {
    val time = PeekLabels.timeLabel(
        event = event,
        date = date,
        zone = zone,
        use24Hour = use24Hour,
        allDayWord = stringResource(R.string.peek_all_day)
    )
    val alpha = if (passed) PASSED_ALPHA else 1f
    Row(
        modifier = Modifier.fillMaxWidth().height(DensityLayout.PEEK_ROW_DP.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (config.densityPeekFormat) {
            DensityPeekFormat.GROUPED -> {}
            DensityPeekFormat.DATED -> {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(vertical = 1.dp, horizontal = 5.dp)
                ) {
                    Text(
                        text = TimeFormat.shortDate(date, locale),
                        fontSize = DensityLayout.PEEK_DATE_PILL_SP.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = config.previewFont(FontCategory.EVENT_TIME),
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.width(7.dp))
            }
        }
        Text(
            text = time,
            fontSize = (PeekTypography.TIME_SP * config.typographyScale.eventTimeScale).sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
            fontFamily = config.previewFont(FontCategory.EVENT_TIME),
            maxLines = 1,
            modifier = Modifier.width(timeColumnDp.dp)
        )
        Spacer(modifier = Modifier.width(7.dp))
        Box(
            modifier = Modifier
                .size(DensityLayout.PEEK_DOT_DP.dp)
                .clip(RoundedCornerShape((DensityLayout.PEEK_DOT_DP / 2f).dp))
                .background(Color(event.displayColor).copy(alpha = alpha))
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = event.title,
            fontSize = (PeekTypography.TITLE_SP * config.typographyScale.eventNameScale).sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            fontFamily = config.previewFont(FontCategory.EVENT_NAME),
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Compose approximation of [com.fabiantorrestech.mycalendarwidget.widget.density.DensityWidgetContent]:
 * count, qualifier, the strip and the hour axis, reading the very same snapshot. The strip
 * bitmap comes from [DensityCanvas] via a [DensitySpecBuilder] spec, so the preview and the
 * widget are the same drawing rather than two drawings that agree today.
 *
 * [widthDp] and [heightDp] stand in for the widget's `LocalSize`, and the three chrome
 * branches below are the same three the widget picks between.
 */
@Composable
private fun PreviewDensityContent(
    snapshot: DensitySnapshot?,
    config: WidgetConfig,
    use24Hour: Boolean,
    widthDp: Dp,
    heightDp: Dp
) {
    val compact = DensityLayout.isCompact(heightDp.value)
    val placement = DensityLayout.chromePlacement(
        widthDp = widthDp.value,
        heightDp = heightDp.value,
        showQuickAdd = config.showQuickAddFab,
        showRefresh = config.showRefreshButton
    )

    Column(modifier = Modifier.padding(DensityLayout.WIDGET_PADDING_DP.dp)) {
        if (snapshot != null) {
            if (!snapshot.hasPermission) {
                Text(
                    text = stringResource(R.string.density_no_permission),
                    fontSize = (13 * config.typographyScale.detailScale).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = config.previewFont(FontCategory.DETAIL)
                )
                return@Column
            }

            val zone = ZoneId.systemDefault()
            val headline = DensityCalculator.headline(
                featured = snapshot.featured,
                featuredIsToday = snapshot.featuredIsToday,
                nowMillis = snapshot.nowMillis,
                rolloverHour = config.densityRolloverHour,
                countMode = config.densityCountMode,
                zone = zone,
                use24Hour = use24Hour,
                locale = Locale.getDefault()
            )
            val countSize = if (headline.countIsSentence) 17 else 24

            // The widget resolves these off GlanceTheme; here they come off the card's own
            // Material scheme, and the spec builder stays free of both.
            val palette = DensitySpecBuilder.palette(
                config = config,
                isDark = isSystemInDarkTheme(),
                background = MaterialTheme.colorScheme.surface.toArgb(),
                onSurface = MaterialTheme.colorScheme.onSurface.toArgb(),
                primary = MaterialTheme.colorScheme.primary.toArgb()
            )
            val density = LocalDensity.current.density
            val spec = DensitySpecBuilder.stripSpec(
                day = snapshot.featured,
                config = config,
                palette = palette,
                widthPx = ((widthDp.value - 2 * DensityLayout.WIDGET_PADDING_DP) * density).toInt(),
                density = density,
                nowMillis = if (snapshot.featuredIsToday) snapshot.nowMillis else null,
                zone = zone,
                visibleCalendarIds = snapshot.visibleCalendarIds
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = headline.countText,
                    fontSize = (countSize * config.typographyScale.headerScale).sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = config.previewFont(FontCategory.DATE_HEADER),
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                when (placement) {
                    ChromePlacement.INLINE, ChromePlacement.COMPACT_SINGLE -> PreviewDensityChrome(placement, config)
                    ChromePlacement.BOTTOM_ROW -> {}
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = headline.dateText,
                    fontSize = (13 * config.typographyScale.detailScale).sp,
                    fontWeight = if (snapshot.featuredIsToday) FontWeight.Bold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    fontFamily = config.previewFont(FontCategory.DETAIL)
                )
                if (headline.qualifierText.isNotBlank()) {
                    Text(
                        text = SECOND_LINE_SEPARATOR + headline.qualifierText,
                        fontSize = (13 * config.typographyScale.detailScale).sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                        fontFamily = config.previewFont(FontCategory.DETAIL)
                    )
                }
            }

            Spacer(modifier = Modifier.height(DensityLayout.STRIP_TOP_GAP_WITH_CARET_DP.dp))

            Image(
                bitmap = remember(spec) { DensityCanvas.renderStrip(spec).asImageBitmap() },
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxWidth().height(DensityLayout.STRIP_IMAGE_HEIGHT_DP.dp)
            )

            if (!compact) {
                Spacer(modifier = Modifier.height(DensityLayout.AXIS_TOP_GAP_WITH_CARET_DP.dp))
                PreviewDensityAxisRow(
                    axis = DensitySpecBuilder.axisSpec(
                        snapshot.featured,
                        zone,
                        use24Hour
                    ),
                    config = config
                )
            }

            if (!compact && config.densityLookaheadDays > 0 && snapshot.lookahead.isNotEmpty()) {
                PreviewDensityLookaheadBars(
                    snapshot = snapshot,
                    config = config,
                    palette = palette,
                    widthPx = ((widthDp.value - 2 * DensityLayout.WIDGET_PADDING_DP) * density).toInt(),
                    contentWidthDp = widthDp.value - 2 * DensityLayout.WIDGET_PADDING_DP,
                    density = density
                )
            }

            if (placement == ChromePlacement.BOTTOM_ROW) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PreviewDensityChrome(placement, config)
                }
            }
        }
    }
}

/** Equal-width cells with a label in each tick's cell — the layout the widget builds. */
@Composable
private fun PreviewDensityAxisRow(axis: AxisSpec, config: WidgetConfig) {
    Row(modifier = Modifier.fillMaxWidth()) {
        axis.labels.forEach { label ->
            Box(modifier = Modifier.weight(1f)) {
                Text(
                    text = label.orEmpty(),
                    fontSize = (11 * config.typographyScale.eventTimeScale).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    fontFamily = config.previewFont(FontCategory.EVENT_TIME)
                )
            }
        }
    }
}

/**
 * Preview mirror of the widget's `DensityLookaheadBars`: divider, day labels, then the
 * same [DensityCanvas.renderLoadBars] bitmap built from the same [DensitySpecBuilder]
 * spec, so the settings preview and the widget draw the identical bars.
 */
@Composable
private fun PreviewDensityLookaheadBars(
    snapshot: DensitySnapshot,
    config: WidgetConfig,
    palette: DensityPalette,
    widthPx: Int,
    contentWidthDp: Float,
    density: Float
) {
    Spacer(modifier = Modifier.height(DensityLayout.DIVIDER_TOP_GAP_DP.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(palette.free))
    )
    Spacer(modifier = Modifier.height(DensityLayout.LABELS_TOP_GAP_DP.dp))

    val labels = DensitySpecBuilder.dayLabels(
        snapshot,
        Locale.getDefault(),
        columnWidthDp = DensityLayout.dayColumnWidthDp(contentWidthDp, snapshot.lookahead.size)
    )
    Row(modifier = Modifier.fillMaxWidth()) {
        // Mirrors the widget: gutter as start-padding on each box after the first
        // (rather than a sibling Spacer) so the two stay structurally identical, even
        // though Compose here has no Glance child-count cap to work around.
        labels.forEachIndexed { index, label ->
            val boxModifier = if (index > 0) {
                Modifier.weight(1f).padding(start = DensityLayout.DAY_BAR_GUTTER_DP.dp)
            } else {
                Modifier.weight(1f)
            }
            Box(modifier = boxModifier) {
                Text(
                    text = label,
                    fontSize = (11 * config.typographyScale.eventTimeScale).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    fontFamily = config.previewFont(FontCategory.EVENT_TIME)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(DensityLayout.BARS_TOP_GAP_DP.dp))

    val barsSpec = DensitySpecBuilder.loadBarsSpec(
        snapshot = snapshot,
        config = config,
        palette = palette,
        widthPx = widthPx,
        density = density
    )
    Image(
        bitmap = remember(barsSpec) { DensityCanvas.renderLoadBars(barsSpec).asImageBitmap() },
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = Modifier.fillMaxWidth().height(DensityLayout.DAY_BAR_IMAGE_HEIGHT_DP.dp)
    )
}

/** The widget's `DensityChrome`: the same three placements, the same button order. */
@Composable
private fun PreviewDensityChrome(
    placement: ChromePlacement,
    config: WidgetConfig
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        when (placement) {
            ChromePlacement.INLINE, ChromePlacement.BOTTOM_ROW -> {
                if (config.showRefreshButton) {
                    PreviewRefreshButton(config)
                    Spacer(modifier = Modifier.width(4.dp))
                }
                PreviewOpenCalendarButton()
                if (config.showQuickAddFab) {
                    Spacer(modifier = Modifier.width(4.dp))
                    PreviewQuickAddButton(config)
                }
            }
            ChromePlacement.COMPACT_SINGLE -> {
                if (config.showQuickAddFab) PreviewQuickAddButton(config) else PreviewOpenCalendarButton()
            }
        }
    }
}

/** The widget's [com.fabiantorrestech.mycalendarwidget.widget.RefreshButton], inert. */
@Composable
private fun PreviewRefreshButton(config: WidgetConfig) {
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

/** The widget's [com.fabiantorrestech.mycalendarwidget.widget.QuickAddButton], inert. */
@Composable
private fun PreviewQuickAddButton(config: WidgetConfig) {
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

/** The widget's [com.fabiantorrestech.mycalendarwidget.widget.OpenCalendarButton], inert. */
@Composable
private fun PreviewOpenCalendarButton() {
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
): CycleUiStyle = when (widgetStyle) {
    WidgetStyle.GCAL_LEFT -> cycleUiStyle
    WidgetStyle.AGENDA, WidgetStyle.GCAL, WidgetStyle.DENSITY ->
        if (cycleUiStyle == CycleUiStyle.TABS) CycleUiStyle.DOTS else cycleUiStyle
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
    val dark = ColorMath.isDark(event.displayColor)
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
    when (config.widgetStyle) {
        WidgetStyle.GCAL -> PreviewEventChipGcal(event, config)
        WidgetStyle.AGENDA, WidgetStyle.GCAL_LEFT -> PreviewEventChipAgenda(event, config)
        WidgetStyle.DENSITY -> error("DENSITY must not reach PreviewEventChip")
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
    val dark = ColorMath.isDark(event.displayColor)
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
