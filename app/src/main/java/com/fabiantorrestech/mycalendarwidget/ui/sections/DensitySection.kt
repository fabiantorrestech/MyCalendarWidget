package com.fabiantorrestech.mycalendarwidget.ui.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fabiantorrestech.mycalendarwidget.data.CalendarInfo
import com.fabiantorrestech.mycalendarwidget.data.DensityCountMode
import com.fabiantorrestech.mycalendarwidget.data.DensityPeekFormat
import com.fabiantorrestech.mycalendarwidget.data.DensityStripMode
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfig
import com.fabiantorrestech.mycalendarwidget.data.density.TonalRamp
import com.fabiantorrestech.mycalendarwidget.widget.density.DensitySpecBuilder
import com.fabiantorrestech.mycalendarwidget.widget.use24Hour

/**
 * The density style's behaviour sliders: the day window, the look-ahead bars, the load
 * baseline, the evening rollover and the peek horizon. Hidden entirely when the active
 * style isn't Density; the density *look* controls (strip mode, peek format, count mode,
 * busy colour, Tonal tones) live in [AppearanceSection].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DensitySection(
    config: WidgetConfig,
    onConfigChange: (WidgetConfig) -> Unit
) {
    if (!VisibleSettings.forStyle(config.widgetStyle).densitySection) return

    SectionHeader(title = "Widget Behavior")

    val context = LocalContext.current
    val use24HourClock = remember(context) { use24Hour(context) }

    val startHour = config.densityWindowStartMinutes / 60
    val endHour = config.densityWindowEndMinutes / 60

    IntSliderRow(
        label = "Today - start time",
        description = "The widget's today bar will grow dynamically at the beginning with events " +
            "that start earlier, even if they occur earlier than this set time. This is just what " +
            "your bar will show on days where your first event is after this set start time.",
        savedValue = startHour,
        range = 0..12,
        valueLabel = { "$it:00" },
        onValueChangeFinished = { hour ->
            val newStartMinutes = hour * 60
            // Invariant: end - start >= 60 (a one-hour window is the shortest allowed).
            // Pushing the start up past (end - 60) drags the end along with it. Start's
            // own range (0..12) tops out at 720, so newStartMinutes + 60 never exceeds
            // 780 — well inside end's valid range (780..1440) — a coerceAtMost(1440)
            // guard would be dead code here and is deliberately omitted.
            val newEndMinutes = if (config.densityWindowEndMinutes < newStartMinutes + 60) {
                newStartMinutes + 60
            } else {
                config.densityWindowEndMinutes
            }
            onConfigChange(
                config.copy(
                    densityWindowStartMinutes = newStartMinutes,
                    densityWindowEndMinutes = newEndMinutes
                )
            )
        }
    )
    IntSliderRow(
        label = "Today - end time",
        description = "This will dynamically grow for days where your events run past this time " +
            "regardless of what you set here. This time that you set will be what the widget's " +
            "ending time normally will display until in the cases when you don't have events that " +
            "go over this set time.",
        savedValue = endHour,
        range = 13..24,
        valueLabel = { "$it:00" },
        onValueChangeFinished = { hour ->
            val newEndMinutes = hour * 60
            // Symmetric guard: pulling the end down below (start + 60) drags the start
            // down with it. End's own range (13..24) bottoms out at 780, so
            // newEndMinutes - 60 never drops below 720 — well inside start's valid range
            // (0..720) — a coerceAtLeast(0) guard would be dead code here and is
            // deliberately omitted.
            val newStartMinutes = if (config.densityWindowStartMinutes > newEndMinutes - 60) {
                newEndMinutes - 60
            } else {
                config.densityWindowStartMinutes
            }
            onConfigChange(
                config.copy(
                    densityWindowStartMinutes = newStartMinutes,
                    densityWindowEndMinutes = newEndMinutes
                )
            )
        }
    )

    IntSliderRow(
        label = "Look-ahead bars (days)",
        description = "How many days are shown to look ahead to under the main bar shown for today.",
        savedValue = config.densityLookaheadDays,
        range = 0..7,
        onValueChangeFinished = { onConfigChange(config.copy(densityLookaheadDays = it)) }
    )

    val baselineSteps = (120..720 step 30).toList()
    StepSliderRow(
        label = "Load baseline",
        description = "The amount of time the following days' progress bars track up to: " +
            "how many of the baseline hours are occupied." + "\n\n" +
            "e.g. with a baseline of 8h, 6h of events fills the bar 75%. Past the baseline the " +
            "bar fills completely, turns heavier and shows a + to say you're over capacity that day.",
        steps = baselineSteps,
        savedIndex = baselineSteps.indexOf(config.densityLoadBaselineMinutes)
            .let { if (it < 0) baselineSteps.indexOf(480) else it },
        labelForIndex = { index -> formatMinutesAsHours(baselineSteps[index]) },
        onIndexChangeFinished = { index ->
            onConfigChange(config.copy(densityLoadBaselineMinutes = baselineSteps[index]))
        }
    )

    IntSliderRow(
        label = "Show tomorrow after",
        description = "Once it's past this time and today's events are over, the main bar " +
            "switches to tomorrow.",
        savedValue = config.densityRolloverHour,
        range = 0..23,
        valueLabel = { formatRolloverHour(it, use24HourClock) },
        onValueChangeFinished = { onConfigChange(config.copy(densityRolloverHour = it)) }
    )
    ToggleRow(
        label = "Don't show tomorrow early",
        description = "If you're still busy at the time above, today stays up until midnight " +
            "instead of switching to tomorrow as soon as your last event ends.",
        checked = config.densityNoEarlyTomorrow,
        onCheckedChange = { onConfigChange(config.copy(densityNoEarlyTomorrow = it)) }
    )

    IntSliderRow(
        label = "Peek horizon (days)",
        description = "How far ahead the peek list reaches",
        savedValue = config.daysAheadToLoad,
        range = 7..90,
        onValueChangeFinished = { onConfigChange(config.copy(daysAheadToLoad = it)) }
    )
}

/** "8h" when [minutes] is a whole number of hours, "2h 30m" otherwise. */
private fun formatMinutesAsHours(minutes: Int): String {
    val hours = minutes / 60
    val remainder = minutes % 60
    return if (remainder == 0) "${hours}h" else "${hours}h ${remainder}m"
}

/** "7 PM" / "19:00" depending on the user's system 12h/24h preference (widget/TimeSettings). */
private fun formatRolloverHour(hour: Int, use24Hour: Boolean): String {
    if (use24Hour) return "%02d:00".format(hour)
    val hour12 = if (hour % 12 == 0) 12 else hour % 12
    val suffix = if (hour < 12) "AM" else "PM"
    return "$hour12 $suffix"
}
