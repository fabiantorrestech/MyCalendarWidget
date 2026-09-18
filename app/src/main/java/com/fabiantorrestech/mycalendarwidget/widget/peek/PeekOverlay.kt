package com.fabiantorrestech.mycalendarwidget.widget.peek

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
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
import androidx.glance.unit.ColorProvider
import com.fabiantorrestech.mycalendarwidget.R
import com.fabiantorrestech.mycalendarwidget.data.CalendarEvent
import com.fabiantorrestech.mycalendarwidget.data.DensityPeekFormat
import com.fabiantorrestech.mycalendarwidget.data.FontCategory
import com.fabiantorrestech.mycalendarwidget.data.TimeFormat
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfig
import com.fabiantorrestech.mycalendarwidget.widget.WidgetClickActions
import com.fabiantorrestech.mycalendarwidget.widget.density.DensityLayout
import com.fabiantorrestech.mycalendarwidget.widget.density.DensityPalette
import com.fabiantorrestech.mycalendarwidget.widget.glanceFont
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** The peek sheet's base text sizes, shared with the settings preview's mirror of it. */
internal object PeekTypography {
    const val TOP_BAR_SP = 10
    const val HEADER_SP = 14
    const val TIME_SP = 11
    const val TITLE_SP = 13
}

private const val TOP_BAR_SIZE_SP = PeekTypography.TOP_BAR_SP
private const val HEADER_SIZE_SP = PeekTypography.HEADER_SP
private const val TIME_SIZE_SP = PeekTypography.TIME_SP
private const val TITLE_SIZE_SP = PeekTypography.TITLE_SP

/**
 * The peek sheet: the one place in the density widget where event titles are shown.
 *
 * It is a flat [LazyColumn] over [PeekList.items] rather than nested containers, for two
 * reasons. Glance truncates a `Column`/`Row` past ten direct children without erroring,
 * so a per-day `Column` of rows would silently lose events on a busy day; and a flat list
 * with stable ids lets the launcher recycle rows and scroll properly. The "Upcoming ×"
 * bar is a fixed sibling *above* the list, not its first row, so the close control stays
 * reachable however far the list is scrolled; the list fills whatever height is left
 * under it.
 *
 * Every close path funnels through the same [SetPeekAction]: the top bar (the whole row,
 * not just the ×) and, in `GROUPED`, every date header. The sheet's root `Box` is
 * deliberately *not* clickable — a tap anywhere in the list belongs to the row under it,
 * and swallowing touches at the root would make scrolling close the sheet.
 *
 * [palette] comes from the density renderer so the `DATED` date pill sits on the exact
 * ground the strip's own pill tint uses (G5: pre-blended, never a translucent overlay).
 */
@Composable
fun PeekOverlay(
    eventsByDay: Map<LocalDate, List<CalendarEvent>>,
    loading: Boolean,
    config: WidgetConfig,
    context: Context,
    use24Hour: Boolean,
    palette: DensityPalette
) {
    val zone = ZoneId.systemDefault()
    val locale = Locale.getDefault()
    // "Now" and "today" are read once per pass and reused for both the cut-off filter
    // and the day labels: reading System.currentTimeMillis()/LocalDate.now() a second
    // time later in the same composition could straddle a clock tick and disagree with
    // itself (an event judged "in progress" by one read and "over" by the other).
    // remember(...) also means a recomposition that does not change any of these keys
    // does not re-derive the list at all.
    val (today, upcoming, items) = remember(eventsByDay, config.densityPeekFormat, use24Hour) {
        val nowMillis = System.currentTimeMillis()
        val today = LocalDate.now(zone)
        val upcoming = PeekList.upcoming(
            eventsByDay = eventsByDay,
            nowMillis = nowMillis,
            today = today
        )
        Triple(today, upcoming, PeekList.items(upcoming, config.densityPeekFormat))
    }

    Box(modifier = GlanceModifier.fillMaxSize()) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(DensityLayout.WIDGET_PADDING_DP.dp)
        ) {
            TopBar(context)
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
            items(items, itemId = { it.itemId }) { item ->
                when (item.kind) {
                    PeekItemKind.HEADER -> DateHeader(item.date, today, config, context, locale)
                    PeekItemKind.SEPARATOR -> DaySeparator()
                    PeekItemKind.EVENT -> EventRow(
                        event = item.event,
                        date = item.date,
                        config = config,
                        context = context,
                        use24Hour = use24Hour,
                        zone = zone,
                        locale = locale,
                        palette = palette
                    )
                }
            }
            // "Loading" until the query has answered; only then may the sheet claim
            // there is nothing coming up.
            if (loading) {
                item(itemId = PeekList.LOADING_ITEM_ID) {
                    Text(
                        text = context.getString(R.string.peek_loading),
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = (TITLE_SIZE_SP * config.typographyScale.eventNameScale).sp,
                            fontFamily = config.glanceFont(FontCategory.EVENT_NAME)
                        ),
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(DensityLayout.PEEK_ROW_DP.dp)
                    )
                }
            } else if (upcoming.isEmpty()) {
                item(itemId = PeekList.EMPTY_ITEM_ID) {
                    Text(
                        text = context.getString(R.string.peek_empty),
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = (TITLE_SIZE_SP * config.typographyScale.eventNameScale).sp,
                            fontFamily = config.glanceFont(FontCategory.EVENT_NAME)
                        ),
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(DensityLayout.PEEK_ROW_DP.dp)
                    )
                }
            }
            }
        }
    }
}

/**
 * "Upcoming" and the close button. The whole row closes the sheet, not only the ×: the
 * × is the affordance, the row is the target, which is far easier to hit on a widget.
 */
@Composable
private fun TopBar(context: Context) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(DensityLayout.PEEK_TOP_BAR_DP.dp)
            .clickable(closeAction()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = context.getString(R.string.peek_upcoming),
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = TOP_BAR_SIZE_SP.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight()
        )
        Box(
            modifier = GlanceModifier
                .size(16.dp)
                .background(GlanceTheme.colors.surfaceVariant)
                .cornerRadius(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_close),
                contentDescription = null,
                modifier = GlanceModifier.size(9.dp),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant)
            )
        }
    }
}

/**
 * `GROUPED`'s day marker: "Today", "Tomorrow", or "Fri 3/13". Also a close target, so a
 * user scrolling with a thumb over the headers has a second, larger way out.
 */
@Composable
private fun DateHeader(
    date: LocalDate,
    today: LocalDate,
    config: WidgetConfig,
    context: Context,
    locale: Locale
) {
    val label = PeekLabels.dateHeader(
        date = date,
        today = today,
        todayWord = context.getString(R.string.peek_today),
        tomorrowWord = context.getString(R.string.peek_tomorrow),
        locale = locale
    )
    // Every header is bold; today's additionally sits on a filled pill, which stays
    // distinguishable in grayscale by luminance alone (a colour change would not).
    val fontSize = (HEADER_SIZE_SP * config.typographyScale.subheaderScale).sp
    val fontFamily = config.glanceFont(FontCategory.WEEKDAY_HEADER)
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(DensityLayout.PEEK_HEADER_DP.dp)
            .clickable(closeAction()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (PeekLabels.isToday(date, today)) {
            Box(
                modifier = GlanceModifier
                    .background(GlanceTheme.colors.primaryContainer)
                    .cornerRadius(DensityLayout.PEEK_TODAY_PILL_RADIUS_DP.dp)
                    .padding(horizontal = DensityLayout.PEEK_TODAY_PILL_INSET_DP.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimaryContainer,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontFamily
                    ),
                    maxLines = 1
                )
            }
        } else {
            Text(
                text = label,
                style = TextStyle(
                    color = GlanceTheme.colors.secondary,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamily
                ),
                maxLines = 1
            )
        }
    }
}

/**
 * `DATED`'s day marker: a hairline with 1dp of air either side (G7's 3dp separator).
 * The line and its air are separate `Box`es because an Android view's height *includes*
 * its padding — a single 1dp box with 1dp vertical padding would draw the same 1dp line
 * with no gaps at all.
 */
@Composable
private fun DaySeparator() {
    Box(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(DensityLayout.PEEK_SEPARATOR_DP.dp)
                .background(GlanceTheme.colors.outline)
        ) {}
    }
}

/**
 * One event: an optional date pill (`DATED` only), the start time in a fixed-width
 * column, the calendar colour as a dot, and the title. The time column is fixed width so
 * the dots and titles line up down the list instead of stepping in and out behind times
 * of different lengths.
 */
@Composable
private fun EventRow(
    event: CalendarEvent?,
    date: LocalDate?,
    config: WidgetConfig,
    context: Context,
    use24Hour: Boolean,
    zone: ZoneId,
    locale: Locale,
    palette: DensityPalette
) {
    if (event == null) return
    val time = if (event.allDay) {
        context.getString(R.string.peek_all_day)
    } else {
        TimeFormat.compact(event.dtStart, zone, use24Hour)
    }
    val timeColumn = if (use24Hour) {
        DensityLayout.PEEK_TIME_COL_24H_DP
    } else {
        DensityLayout.PEEK_TIME_COL_12H_DP
    }

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(DensityLayout.PEEK_ROW_DP.dp)
            .clickable(actionStartActivity(WidgetClickActions.eventIntent(event, config))),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Exhaustive over DensityPeekFormat with no else (G3): a third format must be
        // given its own answer here rather than silently inheriting GROUPED's.
        when (config.densityPeekFormat) {
            DensityPeekFormat.GROUPED -> {}
            DensityPeekFormat.DATED -> {
                Box(
                    modifier = GlanceModifier
                        .background(ColorProvider(Color(palette.pill)))
                        .cornerRadius(4.dp)
                        .padding(vertical = 1.dp, horizontal = 5.dp)
                ) {
                    Text(
                        text = date?.let { TimeFormat.shortDate(it, locale) }.orEmpty(),
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = DensityLayout.PEEK_DATE_PILL_SP.sp,
                            fontFamily = config.glanceFont(FontCategory.EVENT_TIME)
                        ),
                        maxLines = 1
                    )
                }
                Spacer(modifier = GlanceModifier.width(7.dp))
            }
        }

        Text(
            text = time,
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = (TIME_SIZE_SP * config.typographyScale.eventTimeScale).sp,
                fontFamily = config.glanceFont(FontCategory.EVENT_TIME)
            ),
            maxLines = 1,
            modifier = GlanceModifier.width(timeColumn.dp)
        )
        Spacer(modifier = GlanceModifier.width(7.dp))
        Box(
            modifier = GlanceModifier
                .size(DensityLayout.PEEK_DOT_DP.dp)
                .background(ColorProvider(Color(event.displayColor)))
                .cornerRadius((DensityLayout.PEEK_DOT_DP / 2f).dp)
        ) {}
        Spacer(modifier = GlanceModifier.width(7.dp))
        Text(
            text = event.title,
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = (TITLE_SIZE_SP * config.typographyScale.eventNameScale).sp,
                fontWeight = FontWeight.Medium,
                fontFamily = config.glanceFont(FontCategory.EVENT_NAME)
            ),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight()
        )
    }
}

private fun closeAction() =
    actionRunCallback<SetPeekAction>(actionParametersOf(peekOpenKey to false))
