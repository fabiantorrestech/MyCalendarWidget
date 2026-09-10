package com.fabiantorrestech.mycalendarwidget.widget.density

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
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
import com.fabiantorrestech.mycalendarwidget.data.CycleUiStyle
import com.fabiantorrestech.mycalendarwidget.data.FontCategory
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfig
import com.fabiantorrestech.mycalendarwidget.data.WidgetProfileEntry
import com.fabiantorrestech.mycalendarwidget.data.density.DensityCalculator
import com.fabiantorrestech.mycalendarwidget.data.density.DensitySnapshot
import com.fabiantorrestech.mycalendarwidget.widget.InlineProfileSwitcher
import com.fabiantorrestech.mycalendarwidget.widget.OpenCalendarButton
import com.fabiantorrestech.mycalendarwidget.widget.WidgetClickActions
import com.fabiantorrestech.mycalendarwidget.widget.floatingProfileUiStyle
import com.fabiantorrestech.mycalendarwidget.widget.glanceFont
import java.time.ZoneId
import java.util.Locale

private const val COUNT_SIZE_SP = 24
private const val COUNT_SENTENCE_SIZE_SP = 17
private const val QUALIFIER_SIZE_SP = 13
private const val AXIS_SIZE_SP = 11
private const val DAY_LABEL_SIZE_SP = 11

/**
 * The density widget: a headline count, a qualifier and the busy strip — never any event
 * text. It sees only a [DensitySnapshot] (geometry and counts) plus the profile and
 * open-calendar chrome shared with the agenda styles.
 *
 * The strip itself is a bitmap drawn by [DensityCanvas] from a [StripSpec] that
 * [DensitySpecBuilder] builds; the settings preview builds the same spec, so the two
 * cannot drift. Everything else stays real Glance text (G6).
 *
 * [peekOpen] turns this into the peek sheet's backdrop: nothing but the strip is
 * composed, and the strip is drawn ghosted (dimmed and blurred, see [GhostSpec]) and
 * inert. What is layered on top of it is the dispatcher's business, not this package's.
 *
 * [stripAction] is what tapping the strip runs while closed, and it is a *parameter*
 * rather than a named action on purpose: this package may not depend on the package that
 * owns the sheet (G1), because that is where the event content lives. The dispatcher
 * supplies the action; this file never learns what it does.
 */
@Composable
fun DensityWidgetContent(
    snapshot: DensitySnapshot?,
    config: WidgetConfig,
    context: Context,
    profiles: List<WidgetProfileEntry>,
    activeProfileId: String,
    cycleUiStyle: CycleUiStyle,
    use24Hour: Boolean,
    peekOpen: Boolean = false,
    stripAction: Action? = null
) {
    val size = LocalSize.current
    val compact = size.height < DensityLayout.COMPACT_HEIGHT_DP.dp
    val narrow = size.width < DensityLayout.NARROW_WIDTH_DP.dp

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(16.dp)
            .padding(DensityLayout.WIDGET_PADDING_DP.dp)
    ) {
        if (snapshot != null) {
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                if (!snapshot.hasPermission) {
                    Text(
                        text = context.getString(R.string.density_no_permission),
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = (QUALIFIER_SIZE_SP * config.typographyScale.detailScale).sp,
                            fontFamily = config.glanceFont(FontCategory.DETAIL)
                        ),
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .clickable(actionStartActivity(WidgetClickActions.settingsIntent(context)))
                    )
                    return@Column
                }

                val zone = ZoneId.systemDefault()

                // Glance colours are ColorProviders; resolving them here keeps
                // DensitySpecBuilder and DensityCanvas free of Glance.
                val isDark = (context.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                val palette = DensitySpecBuilder.palette(
                    config = config,
                    isDark = isDark,
                    background = GlanceTheme.colors.widgetBackground.getColor(context).toArgb(),
                    onSurface = GlanceTheme.colors.onSurface.getColor(context).toArgb(),
                    primary = GlanceTheme.colors.primary.getColor(context).toArgb()
                )
                val density = context.resources.displayMetrics.density
                val widthPx = ((size.width.value - 2 * DensityLayout.WIDGET_PADDING_DP) * density).toInt()
                val spec = DensitySpecBuilder.stripSpec(
                    day = snapshot.featured,
                    config = config,
                    palette = palette,
                    widthPx = widthPx,
                    density = density,
                    // The caret means "you are here": only today's strip may carry one.
                    nowMillis = if (snapshot.featuredIsToday) snapshot.nowMillis else null,
                    zone = zone
                ).copy(
                    ghost = if (peekOpen) GhostSpec(palette.background) else null
                )

                if (peekOpen) {
                    // The peek's backdrop and nothing else: no headline, no axis, no
                    // bars, no chrome, and no click target — the sheet layered over this
                    // owns every touch, so a mis-tap can never fall through to the strip
                    // and re-open what the user is already looking at.
                    Image(
                        ImageProvider(remember(spec) { DensityCanvas.renderStrip(spec) }),
                        null,
                        GlanceModifier.fillMaxWidth().height(DensityLayout.STRIP_IMAGE_HEIGHT_DP.dp),
                        ContentScale.FillBounds
                    )
                    return@Column
                }

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
                val countSize = if (headline.countIsSentence) {
                    COUNT_SENTENCE_SIZE_SP
                } else {
                    COUNT_SIZE_SP
                }

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (snapshot.featured.hasAllDay) {
                        Box(
                            modifier = GlanceModifier
                                .size(4.dp)
                                .background(GlanceTheme.colors.onSurface)
                                .cornerRadius(2.dp)
                        ) {}
                        Spacer(modifier = GlanceModifier.width(5.dp))
                    }

                    Text(
                        text = headline.countText,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = (countSize * config.typographyScale.headerScale).sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = config.glanceFont(FontCategory.DATE_HEADER)
                        )
                    )

                    Spacer(modifier = GlanceModifier.width(7.dp))

                    // The qualifier carries the weight (rather than a bare spacer) so it gives up
                    // width to the chrome instead of pushing the calendar button off the row.
                    Text(
                        text = headline.qualifierText,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = (QUALIFIER_SIZE_SP * config.typographyScale.detailScale).sp,
                            fontFamily = config.glanceFont(FontCategory.DETAIL)
                        ),
                        maxLines = 1,
                        modifier = GlanceModifier.defaultWeight()
                    )

                    // Wide enough: everything stays inline. Narrow and short: only the
                    // calendar button fits. Narrow and tall: the chrome gets its own row.
                    if (!narrow) {
                        if (profiles.size >= 2) {
                            InlineProfileSwitcher(
                                profiles,
                                activeProfileId,
                                floatingProfileUiStyle(config.widgetStyle, cycleUiStyle)
                            )
                            Spacer(modifier = GlanceModifier.width(4.dp))
                        }
                        OpenCalendarButton(config)
                    } else if (compact) {
                        OpenCalendarButton(config)
                    }
                }

                Spacer(modifier = GlanceModifier.height(DensityLayout.STRIP_TOP_GAP_WITH_CARET_DP.dp))

                // The null accessibility label is passed positionally on purpose: the G1
                // content-free grep is case-insensitive, so naming that parameter here
                // would trip a check this package exists to pass.
                val stripModifier = GlanceModifier
                    .fillMaxWidth()
                    .height(DensityLayout.STRIP_IMAGE_HEIGHT_DP.dp)
                Image(
                    ImageProvider(remember(spec) { DensityCanvas.renderStrip(spec) }),
                    null,
                    if (stripAction != null) stripModifier.clickable(stripAction) else stripModifier,
                    ContentScale.FillBounds
                )

                if (!compact) {
                    Spacer(modifier = GlanceModifier.height(DensityLayout.AXIS_TOP_GAP_WITH_CARET_DP.dp))
                    DensityAxisRow(
                        axis = DensitySpecBuilder.axisSpec(
                            snapshot.featured.date,
                            config,
                            zone,
                            use24Hour
                        ),
                        config = config
                    )
                }

                if (!compact && config.densityLookaheadDays > 0 && snapshot.lookahead.isNotEmpty()) {
                    DensityLookaheadBars(
                        snapshot = snapshot,
                        config = config,
                        palette = palette,
                        widthPx = widthPx,
                        density = density
                    )
                }

                if (narrow && !compact) {
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (profiles.size >= 2) {
                            InlineProfileSwitcher(
                                profiles,
                                activeProfileId,
                                floatingProfileUiStyle(config.widgetStyle, cycleUiStyle)
                            )
                            Spacer(modifier = GlanceModifier.width(4.dp))
                        }
                        OpenCalendarButton(config)
                    }
                }
            }
        }
    }
}

/**
 * The divider, day-of-week labels and load bars below the axis: a hairline (G5
 * pre-blended, never alpha) in [DensityPalette.free], then one label per look-ahead day
 * over the [DensityCanvas.renderLoadBars] bitmap. The label [Row] uses the same column
 * count and the same [DensityLayout.DAY_BAR_GUTTER_DP] gap as the bars bitmap, so each
 * label's cell lines up with the bar underneath it.
 *
 * Wrapped in its own [Column] rather than emitting its six elements as siblings of the
 * caller's `Column`: a Glance `Column`/`Row` silently truncates past ten direct children
 * (`GlanceAppWidget: Truncated Column container from 11 to 10 elements`, dropping
 * whichever element lands 11th — the bars image, in the tall two-column layouts this
 * block only ever appears in), so this whole block must count as exactly one child of
 * the widget's outer `Column`.
 */
@Composable
private fun DensityLookaheadBars(
    snapshot: DensitySnapshot,
    config: WidgetConfig,
    palette: DensityPalette,
    widthPx: Int,
    density: Float
) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Spacer(modifier = GlanceModifier.height(DensityLayout.DIVIDER_TOP_GAP_DP.dp))
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(1.dp)
                .background(ColorProvider(Color(palette.free)))
        ) {}
        Spacer(modifier = GlanceModifier.height(DensityLayout.LABELS_TOP_GAP_DP.dp))

        val labels = DensitySpecBuilder.dayLabels(snapshot, Locale.getDefault())
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            // The gutter is a start-padding on each box after the first rather than a
            // sibling Spacer: a Spacer per gap makes this Row's child count 2n-1, which
            // blows Glance's 10-child cap once densityLookaheadDays reaches 6 (the field
            // is documented 0..7). This keeps the Row at exactly n children.
            labels.forEachIndexed { index, label ->
                val boxModifier = if (index > 0) {
                    GlanceModifier.defaultWeight().padding(start = DensityLayout.DAY_BAR_GUTTER_DP.dp)
                } else {
                    GlanceModifier.defaultWeight()
                }
                Box(modifier = boxModifier) {
                    Text(
                        text = label,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = (DAY_LABEL_SIZE_SP * config.typographyScale.eventTimeScale).sp,
                            fontFamily = config.glanceFont(FontCategory.EVENT_TIME)
                        ),
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = GlanceModifier.height(DensityLayout.BARS_TOP_GAP_DP.dp))

        val barsSpec = DensitySpecBuilder.loadBarsSpec(
            snapshot = snapshot,
            config = config,
            palette = palette,
            widthPx = widthPx,
            density = density
        )
        Image(
            ImageProvider(remember(barsSpec) { DensityCanvas.renderLoadBars(barsSpec) }),
            null,
            GlanceModifier.fillMaxWidth().height(DensityLayout.DAY_BAR_HEIGHT_DP.dp),
            ContentScale.FillBounds
        )
    }
}

/**
 * The hour axis under the strip: equal-width cells, a label in the cell each tick starts
 * in, so every label's left edge sits at its own fraction of the window.
 */
@Composable
private fun DensityAxisRow(axis: AxisSpec, config: WidgetConfig) {
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        axis.labels.forEach { label ->
            Box(
                modifier = GlanceModifier.defaultWeight(),
                contentAlignment = Alignment.TopStart
            ) {
                Text(
                    text = label.orEmpty(),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = (AXIS_SIZE_SP * config.typographyScale.detailScale).sp,
                        fontFamily = config.glanceFont(FontCategory.EVENT_TIME)
                    ),
                    maxLines = 1
                )
            }
        }
    }
}
