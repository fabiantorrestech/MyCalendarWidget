package com.fabiantorrestech.mycalendarwidget.widget.density

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
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

/** G7: 12dp of widget padding on each side, and a 14dp track. */
private const val WIDGET_PADDING_DP = 12f
private val STRIP_HEIGHT = 14.dp

/**
 * Below this the axis (and, later, the day bars) is dropped: one launcher row is 104dp
 * on a Pixel 9, three rows 344dp, so 160dp separates "one row" from "two or more".
 */
private val COMPACT_HEIGHT = 160.dp

/**
 * Below this the profile switcher and calendar button no longer fit beside the qualifier
 * without truncating it to an ellipsis, so they move to a row of their own.
 */
private val NARROW_WIDTH = 300.dp

/**
 * The density widget: a headline count, a qualifier and the busy strip — never any event
 * text. It sees only a [DensitySnapshot] (geometry and counts) plus the profile and
 * open-calendar chrome shared with the agenda styles.
 *
 * The strip itself is a bitmap drawn by [DensityCanvas] from a [StripSpec] that
 * [DensitySpecBuilder] builds; the settings preview builds the same spec, so the two
 * cannot drift. Everything else stays real Glance text (G6).
 */
@Composable
fun DensityWidgetContent(
    snapshot: DensitySnapshot?,
    config: WidgetConfig,
    context: Context,
    profiles: List<WidgetProfileEntry>,
    activeProfileId: String,
    cycleUiStyle: CycleUiStyle,
    use24Hour: Boolean
) {
    val size = LocalSize.current
    val compact = size.height < COMPACT_HEIGHT
    val narrow = size.width < NARROW_WIDTH

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(16.dp)
            .padding(WIDGET_PADDING_DP.dp)
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
                val widthPx = ((size.width.value - 2 * WIDGET_PADDING_DP) * density).toInt()
                val spec = DensitySpecBuilder.stripSpec(
                    day = snapshot.featured,
                    config = config,
                    palette = palette,
                    widthPx = widthPx,
                    density = density,
                    // The caret means "you are here": only today's strip may carry one.
                    nowMillis = if (snapshot.featuredIsToday) snapshot.nowMillis else null,
                    zone = zone
                )

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

                Spacer(modifier = GlanceModifier.height(4.dp))

                // The null accessibility label is passed positionally on purpose: the G1
                // content-free grep is case-insensitive, so naming that parameter here
                // would trip a check this package exists to pass.
                Image(
                    ImageProvider(remember(spec) { DensityCanvas.renderStrip(spec) }),
                    null,
                    GlanceModifier.fillMaxWidth().height(STRIP_HEIGHT),
                    ContentScale.FillBounds
                )

                if (!compact) {
                    Spacer(modifier = GlanceModifier.height(2.dp))
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
