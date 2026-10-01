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
import com.fabiantorrestech.mycalendarwidget.data.density.DensityCalculator
import com.fabiantorrestech.mycalendarwidget.data.density.DensityHeadline
import com.fabiantorrestech.mycalendarwidget.data.density.DensitySnapshot
import com.fabiantorrestech.mycalendarwidget.ui.theme.DarkColors
import com.fabiantorrestech.mycalendarwidget.ui.theme.LightColors
import com.fabiantorrestech.mycalendarwidget.widget.density.DensityCanvas
import com.fabiantorrestech.mycalendarwidget.widget.density.DensityPalette
import com.fabiantorrestech.mycalendarwidget.widget.density.DensitySpecBuilder
import com.fabiantorrestech.mycalendarwidget.widget.density.SECOND_LINE_SEPARATOR
import com.fabiantorrestech.mycalendarwidget.widget.peek.PeekItemKind
import com.fabiantorrestech.mycalendarwidget.widget.peek.PeekLabels
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/** The two custom views one notification post needs. */
class NotificationViews(val collapsed: RemoteViews, val expanded: RemoteViews)

/**
 * Draws the persistent notification as RemoteViews, from the same pieces the widget
 * uses: [DensityCalculator.headline] for the words, [DensitySpecBuilder] and
 * [DensityCanvas] for the strip bitmap and the axis, [NotificationAgenda] for the rows.
 *
 * The collapsed view carries no event text, so it doubles as the lock-screen public
 * version. Text colours come from the platform's notification text appearances, so they
 * follow the shade; only the strip bitmap needs colours of its own (see [palette]).
 */
object DensityNotificationRenderer {

    /**
     * Width the strip bitmap is drawn at: the screen less the decorated view's side
     * margins. The ImageView stretches it to the real width, so this only needs to be
     * close enough that the caret and chevrons keep their proportions.
     */
    private const val SIDE_MARGINS_DP = 48

    /** The calendar-permission prompt in place of the density content. */
    fun noPermission(context: Context): RemoteViews =
        RemoteViews(context.packageName, R.layout.notification_density_collapsed).apply {
            setTextViewText(R.id.notification_headline, context.getString(R.string.density_no_permission))
            setViewVisibility(R.id.notification_strip, View.GONE)
        }

    /**
     * [rowIntent] is what tapping an event row starts, or null to leave the row without
     * a handler so the tap falls through to the notification's own content intent.
     */
    fun render(
        context: Context,
        config: WidgetConfig,
        snapshot: DensitySnapshot,
        agenda: AgendaRows,
        use24Hour: Boolean,
        zone: ZoneId,
        rowIntent: (CalendarEvent) -> PendingIntent?
    ): NotificationViews {
        val locale = Locale.getDefault()
        val headline = DensityCalculator.headline(
            featured = snapshot.featured,
            featuredIsToday = snapshot.featuredIsToday,
            nowMillis = snapshot.nowMillis,
            rolloverHour = config.densityRolloverHour,
            countMode = config.densityCountMode,
            zone = zone,
            use24Hour = use24Hour,
            locale = locale
        )
        val strip = stripBitmap(context, config, snapshot, zone)

        val collapsed = RemoteViews(context.packageName, R.layout.notification_density_collapsed).apply {
            setTextViewText(R.id.notification_headline, oneLineHeadline(headline))
            setImageViewBitmap(R.id.notification_strip, strip)
        }

        val expanded = RemoteViews(context.packageName, R.layout.notification_density_expanded).apply {
            setTextViewText(R.id.notification_count, headline.countText)
            setTextViewText(R.id.notification_second_line, secondLine(headline))
            setImageViewBitmap(R.id.notification_strip, strip)
            fillAxis(context, this, snapshot, zone, use24Hour)
            fillAgenda(context, this, agenda, snapshot, zone, use24Hour, locale, rowIntent)
        }

        return NotificationViews(collapsed, expanded)
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

    private fun stripBitmap(
        context: Context,
        config: WidgetConfig,
        snapshot: DensitySnapshot,
        zone: ZoneId
    ): Bitmap {
        val metrics = context.resources.displayMetrics
        val widthPx = ((context.resources.configuration.screenWidthDp - SIDE_MARGINS_DP) * metrics.density).toInt()
        val spec = DensitySpecBuilder.stripSpec(
            day = snapshot.featured,
            config = config,
            palette = palette(context, config),
            widthPx = widthPx,
            density = metrics.density,
            // The caret means "you are here": only today's strip may carry one.
            nowMillis = if (snapshot.featuredIsToday) snapshot.nowMillis else null,
            zone = zone,
            visibleCalendarIds = snapshot.visibleCalendarIds
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
        snapshot: DensitySnapshot,
        zone: ZoneId,
        use24Hour: Boolean
    ) {
        views.removeAllViews(R.id.notification_axis)
        DensitySpecBuilder.axisSpec(snapshot.featured, zone, use24Hour).labels.forEach { label ->
            val cell = RemoteViews(context.packageName, R.layout.notification_axis_cell)
            cell.setTextViewText(R.id.notification_axis_label, label.orEmpty())
            views.addView(R.id.notification_axis, cell)
        }
    }

    private fun fillAgenda(
        context: Context,
        views: RemoteViews,
        agenda: AgendaRows,
        snapshot: DensitySnapshot,
        zone: ZoneId,
        use24Hour: Boolean,
        locale: Locale,
        rowIntent: (CalendarEvent) -> PendingIntent?
    ) {
        views.removeAllViews(R.id.notification_agenda)
        val today = Instant.ofEpochMilli(snapshot.nowMillis).atZone(zone).toLocalDate()
        val allDayWord = context.getString(R.string.peek_all_day)

        agenda.rows.forEach { item ->
            when (item.kind) {
                PeekItemKind.HEADER -> {
                    val label = PeekLabels.dateHeader(
                        item.date,
                        today,
                        context.getString(R.string.peek_today),
                        context.getString(R.string.peek_tomorrow),
                        locale
                    )
                    val text = if (PeekLabels.isToday(item.date, today)) bold(label) else label
                    val header = RemoteViews(context.packageName, R.layout.notification_agenda_header)
                    header.setTextViewText(R.id.notification_header_label, text)
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

        when {
            agenda.moreCount > 0 -> {
                views.setTextViewText(
                    R.id.notification_more,
                    context.getString(R.string.notification_more, agenda.moreCount)
                )
                views.setViewVisibility(R.id.notification_more, View.VISIBLE)
            }
            agenda.rows.isEmpty() -> {
                views.setTextViewText(R.id.notification_more, context.getString(R.string.peek_empty))
                views.setViewVisibility(R.id.notification_more, View.VISIBLE)
            }
            else -> views.setViewVisibility(R.id.notification_more, View.GONE)
        }
    }

    private fun bold(text: String): CharSequence =
        SpannableString(text).apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
}
