package com.fabiantorrestech.mycalendarwidget.notification

import android.app.PendingIntent
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Typeface
import android.os.Build
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.View
import android.widget.RemoteViews
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.toArgb
import com.fabiantorrestech.mycalendarwidget.R
import com.fabiantorrestech.mycalendarwidget.data.CalendarEvent
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfig
import com.fabiantorrestech.mycalendarwidget.data.density.DayDensity
import com.fabiantorrestech.mycalendarwidget.data.density.DensityCalculator
import com.fabiantorrestech.mycalendarwidget.data.density.DensityHeadline
import com.fabiantorrestech.mycalendarwidget.data.density.DensitySnapshot
import com.fabiantorrestech.mycalendarwidget.ui.theme.DarkColors
import com.fabiantorrestech.mycalendarwidget.ui.theme.LightColors
import com.fabiantorrestech.mycalendarwidget.widget.density.DensityCanvas
import com.fabiantorrestech.mycalendarwidget.widget.density.DensityPalette
import com.fabiantorrestech.mycalendarwidget.widget.density.DensitySpecBuilder
import com.fabiantorrestech.mycalendarwidget.widget.density.SECOND_LINE_SEPARATOR
import com.fabiantorrestech.mycalendarwidget.widget.peek.PeekItem
import com.fabiantorrestech.mycalendarwidget.widget.peek.PeekItemKind
import com.fabiantorrestech.mycalendarwidget.widget.peek.PeekLabels
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/**
 * The custom views one notification post needs: [lockScreen] is [collapsed] without the
 * + button, for the public version.
 */
class NotificationViews(val collapsed: RemoteViews, val expanded: RemoteViews, val lockScreen: RemoteViews)

/** What the expanded view shows under its headline, for the paging mode in use. */
sealed interface ExpandedBody {
    /**
     * One page of the upcoming list. The headline and strip stay on the featured day;
     * only the rows page.
     */
    data class AgendaPage(val rows: List<PeekItem>, val pageIndex: Int, val pageCount: Int) : ExpandedBody

    /**
     * One day: its own headline, strip, axis and events. [offset] counts days from the
     * featured day, up to [maxOffset].
     */
    data class Day(
        val day: DayDensity,
        val isToday: Boolean,
        val rows: AgendaRows,
        val offset: Int,
        val maxOffset: Int
    ) : ExpandedBody
}

/** What the arrows' row starts; each is a broadcast to the notification's receiver. */
class NavIntents(val previous: PendingIntent, val next: PendingIntent, val first: PendingIntent)

/**
 * Draws the persistent notification as RemoteViews, from the same pieces the widget
 * uses: [DensityCalculator.headline] for the words, [DensitySpecBuilder] and
 * [DensityCanvas] for the strip bitmap and the axis, [NotificationAgenda] for the rows.
 *
 * The collapsed view always shows the featured day and carries no event text, so it
 * doubles as the lock-screen public version. Text colours come from the platform's
 * notification text appearances, so they follow the shade; only the strip bitmap needs
 * colours of its own (see [palette]).
 */
object DensityNotificationRenderer {

    /**
     * Width the strip bitmap is drawn at: the screen less the decorated view's side
     * margins. The ImageView stretches it to the real width, so this only needs to be
     * close enough that the caret and chevrons keep their proportions.
     */
    private const val SIDE_MARGINS_DP = 48

    /** How faded an arrow at the end of the list is drawn. */
    private const val DISABLED_ALPHA = 0.38f

    /** The calendar-permission prompt in place of the density content. */
    fun noPermission(context: Context): RemoteViews =
        RemoteViews(context.packageName, R.layout.notification_density_collapsed).apply {
            setTextViewText(R.id.notification_headline, context.getString(R.string.density_no_permission))
            setViewVisibility(R.id.notification_strip, View.GONE)
        }

    /**
     * [rowIntent] is what tapping an event row starts, or null to leave the row without
     * a handler so the tap falls through to the notification's own content intent.
     * [addIntent] is the + button's "new event" screen, or null to hide the button.
     */
    fun render(
        context: Context,
        config: WidgetConfig,
        snapshot: DensitySnapshot,
        body: ExpandedBody,
        nav: NavIntents,
        addIntent: PendingIntent?,
        use24Hour: Boolean,
        zone: ZoneId,
        rowIntent: (CalendarEvent) -> PendingIntent?
    ): NotificationViews {
        val locale = Locale.getDefault()
        val today = Instant.ofEpochMilli(snapshot.nowMillis).atZone(zone).toLocalDate()
        val palette = palette(context, config)
        val featuredHeadline = DensityCalculator.headline(
            featured = snapshot.featured,
            featuredIsToday = snapshot.featuredIsToday,
            nowMillis = snapshot.nowMillis,
            rolloverHour = config.densityRolloverHour,
            countMode = config.densityCountMode,
            zone = zone,
            use24Hour = use24Hour,
            locale = locale
        )
        // The caret means "you are here": only today's strip may carry one.
        val featuredStrip = stripBitmap(
            context, config, palette, snapshot.featured,
            if (snapshot.featuredIsToday) snapshot.nowMillis else null,
            zone, snapshot.visibleCalendarIds
        )

        val collapsed = collapsedView(context, featuredHeadline, featuredStrip, addIntent)
        // The lock screen gets no +: adding an event there would only ask to unlock.
        val lockScreen = collapsedView(context, featuredHeadline, featuredStrip, addIntent = null)

        val expanded = RemoteViews(context.packageName, R.layout.notification_density_expanded)
        fillAdd(expanded, addIntent)
        when (body) {
            is ExpandedBody.AgendaPage -> {
                fillHeader(expanded, featuredHeadline, featuredStrip)
                fillAxis(context, expanded, snapshot.featured, zone, use24Hour)
                fillAgendaRows(context, expanded, body.rows, today, zone, use24Hour, locale, rowIntent)
                if (body.rows.isEmpty()) {
                    showMore(expanded, context.getString(R.string.peek_empty))
                } else {
                    expanded.setViewVisibility(R.id.notification_more, View.GONE)
                }
                if (body.pageCount > 1) {
                    fillNav(
                        expanded, nav,
                        label = context.getString(R.string.notification_page_label, body.pageIndex + 1, body.pageCount),
                        atStart = body.pageIndex == 0,
                        atEnd = body.pageIndex >= body.pageCount - 1
                    )
                } else {
                    expanded.setViewVisibility(R.id.notification_nav, View.GONE)
                }
            }

            is ExpandedBody.Day -> {
                val headline = if (body.offset == 0) {
                    featuredHeadline
                } else {
                    NotificationDayHeadline.headline(body.day, zone, use24Hour, locale)
                }
                val strip = if (body.offset == 0) {
                    featuredStrip
                } else {
                    stripBitmap(
                        context, config, palette, body.day,
                        if (body.isToday) snapshot.nowMillis else null,
                        zone, snapshot.visibleCalendarIds
                    )
                }
                fillHeader(expanded, headline, strip)
                fillAxis(context, expanded, body.day, zone, use24Hour)
                fillAgendaRows(context, expanded, body.rows.rows, today, zone, use24Hour, locale, rowIntent)
                if (body.rows.moreCount > 0) {
                    showMore(expanded, context.getString(R.string.notification_more, body.rows.moreCount))
                } else {
                    expanded.setViewVisibility(R.id.notification_more, View.GONE)
                }
                fillNav(
                    expanded, nav,
                    label = PeekLabels.dateHeader(
                        body.day.date,
                        today,
                        context.getString(R.string.peek_today),
                        context.getString(R.string.peek_tomorrow),
                        locale
                    ),
                    atStart = body.offset == 0,
                    atEnd = body.offset >= body.maxOffset
                )
            }
        }

        return NotificationViews(collapsed, expanded, lockScreen)
    }

    /** "3 left · Wed (10/1) · next in 25m", with the count in bold. */
    private fun oneLineHeadline(headline: DensityHeadline): CharSequence {
        val text = headline.countText + SECOND_LINE_SEPARATOR + secondLine(headline)
        return SpannableString(text).apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, headline.countText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun secondLine(headline: DensityHeadline): String =
        if (headline.qualifierText.isBlank()) {
            headline.dateText
        } else {
            headline.dateText + SECOND_LINE_SEPARATOR + headline.qualifierText
        }

    private fun fillHeader(views: RemoteViews, headline: DensityHeadline, strip: Bitmap) {
        views.setTextViewText(R.id.notification_count, headline.countText)
        views.setTextViewText(R.id.notification_second_line, secondLine(headline))
        views.setImageViewBitmap(R.id.notification_strip, strip)
    }

    private fun stripBitmap(
        context: Context,
        config: WidgetConfig,
        palette: DensityPalette,
        day: DayDensity,
        nowMillis: Long?,
        zone: ZoneId,
        visibleCalendarIds: List<Long>
    ): Bitmap {
        val metrics = context.resources.displayMetrics
        val widthPx = ((context.resources.configuration.screenWidthDp - SIDE_MARGINS_DP) * metrics.density).toInt()
        val spec = DensitySpecBuilder.stripSpec(
            day = day,
            config = config,
            palette = palette,
            widthPx = widthPx,
            density = metrics.density,
            nowMillis = nowMillis,
            zone = zone,
            visibleCalendarIds = visibleCalendarIds
        )
        return DensityCanvas.renderStrip(spec)
    }

    /**
     * The strip's colours. The shade's real background cannot be read, so the strip's
     * pre-blended halos and gaps are matched to the closest known surface: the system's
     * dynamic `surfaceContainerHigh` on Android 12+ (the shade follows the wallpaper
     * whatever the widget's own Material You setting), else the app's static scheme.
     * The busy colour still follows the widget's settings through
     * [DensitySpecBuilder.palette].
     */
    private fun palette(context: Context, config: WidgetConfig): DensityPalette {
        val night = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val shade = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (night) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            night -> DarkColors
            else -> LightColors
        }
        val themed = if (config.dynamicColor) shade else if (night) DarkColors else LightColors
        return DensitySpecBuilder.palette(
            config = config,
            isDark = night,
            background = shade.surfaceContainerHigh.toArgb(),
            onSurface = shade.onSurface.toArgb(),
            primary = themed.primary.toArgb()
        )
    }

    private fun fillAxis(
        context: Context,
        views: RemoteViews,
        day: DayDensity,
        zone: ZoneId,
        use24Hour: Boolean
    ) {
        views.removeAllViews(R.id.notification_axis)
        DensitySpecBuilder.axisSpec(day, zone, use24Hour).labels.forEach { label ->
            val cell = RemoteViews(context.packageName, R.layout.notification_axis_cell)
            cell.setTextViewText(R.id.notification_axis_label, label.orEmpty())
            views.addView(R.id.notification_axis, cell)
        }
    }

    private fun fillAgendaRows(
        context: Context,
        views: RemoteViews,
        rows: List<PeekItem>,
        today: LocalDate,
        zone: ZoneId,
        use24Hour: Boolean,
        locale: Locale,
        rowIntent: (CalendarEvent) -> PendingIntent?
    ) {
        views.removeAllViews(R.id.notification_agenda)
        val allDayWord = context.getString(R.string.peek_all_day)

        rows.forEach { item ->
            when (item.kind) {
                PeekItemKind.HEADER -> {
                    val label = PeekLabels.dateHeader(
                        item.date,
                        today,
                        context.getString(R.string.peek_today),
                        context.getString(R.string.peek_tomorrow),
                        locale
                    )
                    // Today sits on a filled accent pill, as on the widget's peek.
                    val layout = if (PeekLabels.isToday(item.date, today)) {
                        R.layout.notification_agenda_header_today
                    } else {
                        R.layout.notification_agenda_header
                    }
                    val header = RemoteViews(context.packageName, layout)
                    header.setTextViewText(R.id.notification_header_label, label)
                    views.addView(R.id.notification_agenda, header)
                }
                PeekItemKind.EVENT -> {
                    val event = item.event ?: return@forEach
                    val row = RemoteViews(context.packageName, R.layout.notification_agenda_row)
                    row.setTextViewText(
                        R.id.notification_row_time,
                        PeekLabels.timeLabel(event, item.date, zone, use24Hour, allDayWord)
                    )
                    row.setInt(R.id.notification_row_dot, "setColorFilter", event.displayColor)
                    row.setTextViewText(R.id.notification_row_title, event.title)
                    rowIntent(event)?.let { row.setOnClickPendingIntent(R.id.notification_row, it) }
                    views.addView(R.id.notification_agenda, row)
                }
                // NotificationAgenda only ever emits headers and events.
                PeekItemKind.SEPARATOR -> {}
            }
        }
    }

    private fun showMore(views: RemoteViews, text: String) {
        views.setTextViewText(R.id.notification_more, text)
        views.setViewVisibility(R.id.notification_more, View.VISIBLE)
    }

    /**
     * The ‹ label Today › row. An arrow at the end of the list is faded but keeps its
     * intent (which just redraws the same page): an arrow with no handler would let the
     * tap fall through and open the calendar app instead.
     */
    private fun fillNav(views: RemoteViews, nav: NavIntents, label: String, atStart: Boolean, atEnd: Boolean) {
        views.setViewVisibility(R.id.notification_nav, View.VISIBLE)
        views.setTextViewText(R.id.notification_nav_label, label)

        views.setOnClickPendingIntent(R.id.notification_nav_prev, nav.previous)
        views.setFloat(R.id.notification_nav_prev, "setAlpha", if (atStart) DISABLED_ALPHA else 1f)
        views.setOnClickPendingIntent(R.id.notification_nav_next, nav.next)
        views.setFloat(R.id.notification_nav_next, "setAlpha", if (atEnd) DISABLED_ALPHA else 1f)

        views.setViewVisibility(R.id.notification_nav_first, if (atStart) View.GONE else View.VISIBLE)
        views.setOnClickPendingIntent(R.id.notification_nav_first, nav.first)
    }

    /** The + (add event) button: shown with [addIntent], hidden without one. */
    private fun fillAdd(views: RemoteViews, addIntent: PendingIntent?) {
        if (addIntent == null) {
            views.setViewVisibility(R.id.notification_add, View.GONE)
        } else {
            views.setViewVisibility(R.id.notification_add, View.VISIBLE)
            views.setOnClickPendingIntent(R.id.notification_add, addIntent)
        }
    }

    private fun collapsedView(
        context: Context,
        headline: DensityHeadline,
        strip: Bitmap,
        addIntent: PendingIntent?
    ): RemoteViews =
        RemoteViews(context.packageName, R.layout.notification_density_collapsed).apply {
            setTextViewText(R.id.notification_headline, oneLineHeadline(headline))
            setImageViewBitmap(R.id.notification_strip, strip)
            fillAdd(this, addIntent)
        }
}
