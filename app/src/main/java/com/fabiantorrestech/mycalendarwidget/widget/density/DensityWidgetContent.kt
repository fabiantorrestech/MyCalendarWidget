package com.fabiantorrestech.mycalendarwidget.widget.density

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
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
import com.fabiantorrestech.mycalendarwidget.R
import com.fabiantorrestech.mycalendarwidget.data.CycleUiStyle
import com.fabiantorrestech.mycalendarwidget.data.FontCategory
import com.fabiantorrestech.mycalendarwidget.data.TimeFormat
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfig
import com.fabiantorrestech.mycalendarwidget.data.WidgetProfileEntry
import com.fabiantorrestech.mycalendarwidget.data.density.DayDensity
import com.fabiantorrestech.mycalendarwidget.data.density.DensityCalculator
import com.fabiantorrestech.mycalendarwidget.data.density.DensitySnapshot
import com.fabiantorrestech.mycalendarwidget.widget.InlineProfileSwitcher
import com.fabiantorrestech.mycalendarwidget.widget.OpenCalendarButton
import com.fabiantorrestech.mycalendarwidget.widget.WidgetClickActions
import com.fabiantorrestech.mycalendarwidget.widget.floatingProfileUiStyle
import com.fabiantorrestech.mycalendarwidget.widget.glanceFont
import java.time.ZoneId
import java.time.format.TextStyle as JvmTextStyle
import java.util.Locale

private const val COUNT_SIZE_SP = 24
private const val COUNT_SENTENCE_SIZE_SP = 17
private const val QUALIFIER_SIZE_SP = 13
private const val LOOKAHEAD_SIZE_SP = 11

/**
 * The density widget: a headline count, a qualifier and a look-ahead summary — never any
 * event text. It sees only a [DensitySnapshot] (geometry and counts) plus the profile and
 * open-calendar chrome shared with the agenda styles.
 *
 * This is the Stage 1, text-only rendering; the strip, axis and day bars land in later tasks.
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
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(16.dp)
            .padding(12.dp)
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

                val headline = DensityCalculator.headline(
                    featured = snapshot.featured,
                    featuredIsToday = snapshot.featuredIsToday,
                    nowMillis = snapshot.nowMillis,
                    rolloverHour = config.densityRolloverHour,
                    countMode = config.densityCountMode,
                    zone = ZoneId.systemDefault(),
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

                if (snapshot.lookahead.isNotEmpty()) {
                    Spacer(modifier = GlanceModifier.height(6.dp))
                    LookaheadSummaryRow(snapshot.lookahead, config)
                }
            }
        }
    }
}

/**
 * Stage 1 placeholder for the look-ahead strip: "Thu 5h · Fri 6h40 · Sat 1h". A later task
 * replaces this whole composable with the bitmap strip and day bars, so it is kept small
 * and self-contained.
 */
@Composable
private fun LookaheadSummaryRow(lookahead: List<DayDensity>, config: WidgetConfig) {
    val locale = Locale.getDefault()
    val summary = lookahead.joinToString(separator = " · ") { day ->
        val weekday = day.date.dayOfWeek.getDisplayName(JvmTextStyle.SHORT, locale)
        "$weekday ${TimeFormat.durationLabel(day.busyMinutes)}"
    }
    Text(
        text = summary,
        style = TextStyle(
            color = GlanceTheme.colors.onSurfaceVariant,
            fontSize = (LOOKAHEAD_SIZE_SP * config.typographyScale.detailScale).sp,
            fontFamily = config.glanceFont(FontCategory.EVENT_TIME)
        ),
        maxLines = 1,
        modifier = GlanceModifier.fillMaxWidth()
    )
}
