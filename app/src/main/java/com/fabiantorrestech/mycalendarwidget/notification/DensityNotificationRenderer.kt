package com.fabiantorrestech.mycalendarwidget.notification

import android.app.PendingIntent
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.drawable.Icon
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
 * The custom views one notification post needs: [lockScreen] is [collapsed] without its
 * buttons, for the public version.
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
 * The buttons beside the headline: [refresh] redraws the notification and its widget,
 * [add] opens the calendar's new-event screen. Null hides that button.
 */
class ChromeIntents(val refresh: PendingIntent?, val add: PendingIntent?)

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
     * [chrome] holds the refresh and + buttons' intents; a null one hides that button.
     * [pinned] draws for the colorized foreground-service card (see doc/adr/0003).
     */
    fun render(
        context: Context,
        config: WidgetConfig,
        snapshot: DensitySnapshot,
        body: ExpandedBody,
        nav: NavIntents,
        chrome: ChromeIntents,
        pinned: Boolean,
        use24Hour: Boolean,
        zone: ZoneId,
        rowIntent: (CalendarEvent) -> PendingIntent?
    ): NotificationViews {
        val locale = Locale.getDefault()
        val today = Instant.ofEpochMilli(snapshot.nowMillis).atZone(zone).toLocalDate()
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
        val featuredStrip = strip(
            context, config, snapshot.featured,
            if (snapshot.featuredIsToday) snapshot.nowMillis else null,
            zone, snapshot.visibleCalendarIds, pinned
        )

        val collapsed = collapsedView(context, featuredHeadline, featuredStrip, chrome, pinned)
        // The lock screen gets no buttons: adding an event there would only ask to unlock,
        // and the redacted version should stay a plain read-out.
        val lockScreen = collapsedView(context, featuredHeadline, featuredStrip, ChromeIntents(null, null), pinned)

        val expanded = RemoteViews(context.packageName, R.layout.notification_density_expanded)
        fillChrome(expanded, chrome, pinned, context)
        when (body) {
            is ExpandedBody.AgendaPage -> {
                fillHeader(expanded, featuredHeadline, featuredStrip)
                fillAxis(context, expanded, snapshot.featured, zone, use24Hour)
                fillAgendaRows(context, expanded, body.rows, today, zone, use24Hour, locale, pinned, rowIntent)
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
                    strip(
                        context, config, body.day,
                        if (body.isToday) snapshot.nowMillis else null,
                        zone, snapshot.visibleCalendarIds, pinned
                    )
                }
                fillHeader(expanded, headline, strip)
                fillAxis(context, expanded, body.day, zone, use24Hour)
                fillAgendaRows(context, expanded, body.rows.rows, today, zone, use24Hour, locale, pinned, rowIntent)
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

    private fun fillHeader(views: RemoteViews, headline: DensityHeadline, strip: StripImages) {
        views.setTextViewText(R.id.notification_count, headline.countText)
        views.setTextViewText(R.id.notification_second_line, secondLine(headline))
        setStrip(views, strip)
    }

    /**
     * One strip bitmap per theme. The strip's colours are baked into the bitmap, unlike
     * the text (which follows the shade's text appearances), so a single bitmap would
     * keep the old theme's colours after a light/dark switch until the next refresh.
     */
    private class StripImages(val light: Bitmap, val dark: Bitmap)

    /**
     * Android 12+ draws both and lets the shade pick (see [setStrip]); older versions
     * only get the theme in use now, in both slots.
     */
    private fun strip(
        context: Context,
        config: WidgetConfig,
        day: DayDensity,
        nowMillis: Long?,
        zone: ZoneId,
        visibleCalendarIds: List<Long>,
        pinned: Boolean
    ): StripImages {
        fun render(night: Boolean): Bitmap {
            val metrics = context.resources.displayMetrics
            val widthPx = ((context.resources.configuration.screenWidthDp - SIDE_MARGINS_DP) * metrics.density).toInt()
            val spec = DensitySpecBuilder.stripSpec(
                day = day,
                config = config,
                palette = palette(context, config, night, pinned),
                widthPx = widthPx,
                density = metrics.density,
                nowMillis = nowMillis,
                zone = zone,
                visibleCalendarIds = visibleCalendarIds
            )
            return DensityCanvas.renderStrip(spec)
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            StripImages(light = render(night = false), dark = render(night = true))
        } else {
            render(isNight(context)).let { StripImages(it, it) }
        }
    }

    /**
     * Android 12+'s day/night pair: the shade shows whichever matches its theme at the
     * time, and swaps it when the theme changes, without a new post from this app.
     */
    private fun setStrip(views: RemoteViews, strip: StripImages) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            views.setIcon(
                R.id.notification_strip,
                "setImageIcon",
                Icon.createWithBitmap(strip.light),
                Icon.createWithBitmap(strip.dark)
            )
        } else {
            views.setImageViewBitmap(R.id.notification_strip, strip.light)
        }
    }

    private fun isNight(context: Context): Boolean =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    /**
     * The strip's colours. The shade's real background cannot be read, so the strip's
     * pre-blended halos and gaps are matched to the closest known surface: the system's
     * dynamic `surfaceContainerHigh` on Android 12+ (the shade follows the wallpaper
     * whatever the widget's own Material You setting), else the app's static scheme.
     * The busy colour still follows the widget's settings through
     * [DensitySpecBuilder.palette]. A [pinned] card is the accent container colour
     * (see [accentContainer]), so its strip is matched to that instead.
     */
    private fun palette(context: Context, config: WidgetConfig, night: Boolean, pinned: Boolean): DensityPalette {
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
            background = if (pinned) accentContainer(context, night) else shade.surfaceContainerHigh.toArgb(),
            onSurface = shade.onSurface.toArgb(),
            primary = themed.primary.toArgb()
        )
    }

    /**
     * The pinned card's colour for [night] or day, read from the same resource as the
     * Today pill (`notification_accent_container`, Material You's primaryContainer on
     * Android 12+) through a context forced to that theme, so the strip drawn for each
     * theme matches the card the shade colours for it.
     */
    fun accentContainer(context: Context, night: Boolean): Int {
        val configuration = Configuration(context.resources.configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                (if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO)
        }
        return context.createConfigurationContext(configuration).getColor(R.color.notification_accent_container)
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
        pinned: Boolean,
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
                    // Today sits on a filled accent pill, as on the widget's peek; on the
                    // pinned card, itself the accent container, the pill steps up a tone.
                    val layout = when {
                        !PeekLabels.isToday(item.date, today) -> R.layout.notification_agenda_header
                        pinned -> R.layout.notification_agenda_header_today_pinned
                        else -> R.layout.notification_agenda_header_today
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

    /**
     * The refresh and + buttons: each shown with its intent, hidden without one. On the
     * pinned card the + switches to the stronger accent, since its usual accent
     * container would vanish into the card.
     */
    private fun fillChrome(views: RemoteViews, chrome: ChromeIntents, pinned: Boolean, context: Context) {
        fillButton(views, R.id.notification_refresh, chrome.refresh)
        fillButton(views, R.id.notification_add, chrome.add)
        if (pinned && chrome.add != null) {
            views.setInt(R.id.notification_add, "setBackgroundResource", R.drawable.notification_add_circle_pinned)
            views.setInt(R.id.notification_add, "setColorFilter", context.getColor(R.color.notification_on_accent_strong))
        }
    }

    private fun fillButton(views: RemoteViews, viewId: Int, intent: PendingIntent?) {
        if (intent == null) {
            views.setViewVisibility(viewId, View.GONE)
        } else {
            views.setViewVisibility(viewId, View.VISIBLE)
            views.setOnClickPendingIntent(viewId, intent)
        }
    }

    private fun collapsedView(
        context: Context,
        headline: DensityHeadline,
        strip: StripImages,
        chrome: ChromeIntents,
        pinned: Boolean
    ): RemoteViews =
        RemoteViews(context.packageName, R.layout.notification_density_collapsed).apply {
            setTextViewText(R.id.notification_headline, oneLineHeadline(headline))
            setStrip(this, strip)
            fillChrome(this, chrome, pinned, context)
        }
}
