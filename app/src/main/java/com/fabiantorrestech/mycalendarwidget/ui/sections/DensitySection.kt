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
import com.fabiantorrestech.mycalendarwidget.data.WidgetStyle
import com.fabiantorrestech.mycalendarwidget.data.density.TonalRamp
import com.fabiantorrestech.mycalendarwidget.widget.density.DensitySpecBuilder
import com.fabiantorrestech.mycalendarwidget.widget.use24Hour

/** The eight busy-color presets offered alongside "Follow theme" (0 = follow theme). */
private val BUSY_COLOR_PRESETS = listOf(
    0xFF7F77DD.toInt(),
    0xFF5C6BC0.toInt(),
    0xFF26A69A.toInt(),
    0xFF66BB6A.toInt(),
    0xFFFFA726.toInt(),
    0xFFEF5350.toInt(),
    0xFFAB47BC.toInt(),
    0xFF78909C.toInt()
)

/**
 * Every control for the Density widget style. Hidden entirely when the active style isn't
 * Density (Task 4 already gates [DisplaySection]'s text controls the same way), and the
 * Tonal calendar-tone list only appears while [DensityStripMode.TONAL] is selected.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DensitySection(
    config: WidgetConfig,
    calendars: List<CalendarInfo>,
    onConfigChange: (WidgetConfig) -> Unit
) {
    if (config.widgetStyle != WidgetStyle.DENSITY) return

    SectionHeader(title = "Density")

    val context = LocalContext.current
    val use24HourClock = remember(context) { use24Hour(context) }

    EnumSegmentedRow(
        options = DensityStripMode.entries,
        selected = config.densityStripMode,
        label = { it.displayName },
        onSelect = { onConfigChange(config.copy(densityStripMode = it)) }
    )
    EnumSegmentedRow(
        options = DensityPeekFormat.entries,
        selected = config.densityPeekFormat,
        label = { it.displayName },
        onSelect = { onConfigChange(config.copy(densityPeekFormat = it)) }
    )
    EnumSegmentedRow(
        options = DensityCountMode.entries,
        selected = config.densityCountMode,
        label = { it.displayName },
        onSelect = { onConfigChange(config.copy(densityCountMode = it)) }
    )

    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

    val startHour = config.densityWindowStartMinutes / 60
    val endHour = config.densityWindowEndMinutes / 60

    IntSliderRow(
        label = "Day window starts",
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
        label = "Day window ends",
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
        label = "Days to look ahead",
        savedValue = config.densityLookaheadDays,
        range = 0..7,
        onValueChangeFinished = { onConfigChange(config.copy(densityLookaheadDays = it)) }
    )

    val baselineSteps = (120..720 step 30).toList()
    StepSliderRow(
        label = "Load baseline",
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
        savedValue = config.densityRolloverHour,
        range = 0..23,
        valueLabel = { formatRolloverHour(it, use24HourClock) },
        onValueChangeFinished = { onConfigChange(config.copy(densityRolloverHour = it)) }
    )

    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

    Text(
        text = "Busy color",
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(bottom = 6.dp)
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = config.densityBusyColor == 0,
            onClick = { onConfigChange(config.copy(densityBusyColor = 0)) },
            label = { Text("Follow theme") }
        )
        BUSY_COLOR_PRESETS.forEach { preset ->
            ColorSwatch(
                color = preset,
                size = 32.dp,
                selected = config.densityBusyColor == preset,
                contentDescription = "Busy color ${colorHex(preset)}",
                onClick = { onConfigChange(config.copy(densityBusyColor = preset)) }
            )
        }
    }

    if (config.densityStripMode == DensityStripMode.TONAL) {
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        TonalCalendarTones(config = config, calendars = calendars, onConfigChange = onConfigChange)
    }
}

@Composable
private fun TonalCalendarTones(
    config: WidgetConfig,
    calendars: List<CalendarInfo>,
    onConfigChange: (WidgetConfig) -> Unit
) {
    Text(
        text = "Tonal calendars",
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(bottom = 6.dp)
    )

    if (calendars.isEmpty()) {
        Text(
            text = "No calendars visible",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    // Same inputs the widget itself resolves the palette from (Task 3/DensitySpecBuilder),
    // so the swatches shown here are exactly the tones the strip will draw.
    val isDark = isSystemInDarkTheme()
    val backgroundArgb = MaterialTheme.colorScheme.background.toArgb()
    val onSurfaceArgb = MaterialTheme.colorScheme.onSurface.toArgb()
    val primaryArgb = MaterialTheme.colorScheme.primary.toArgb()
    val palette = remember(config.densityBusyColor, config.dynamicColor, isDark, backgroundArgb, onSurfaceArgb, primaryArgb) {
        DensitySpecBuilder.palette(
            config = config,
            isDark = isDark,
            background = backgroundArgb,
            onSurface = onSurfaceArgb,
            primary = primaryArgb
        )
    }
    val ramp = remember(palette) { TonalRamp.ramp(palette.busy, palette.background) }
    // Matches the widget's own fallback (DensitySpecBuilder.enabledSortedCalendarIds,
    // fed from DensityCalendarSource.queryVisibleCalendarIds): when the user hasn't set
    // an explicit calendar filter, rank over the provider's VISIBLE=1 calendars rather
    // than every calendar CalendarRepository.getCalendars() returns (that list
    // deliberately omits the VISIBLE filter so a hidden calendar can still be toggled
    // back on here) — otherwise this ring can point at a different swatch than the one
    // the widget actually paints for a calendar with no events today.
    val enabledSortedIds = config.enabledCalendarIds.ifEmpty {
        calendars.filter { it.visible }.map { it.id }.toSet()
    }.sorted()

    calendars.forEach { calendar ->
        val assignedTone = config.densityCalendarTones[calendar.id]
            ?: TonalRamp.bucket(calendar.id, emptyMap(), enabledSortedIds)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = calendar.displayName,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ramp.forEachIndexed { index, toneColor ->
                    ColorSwatch(
                        color = toneColor,
                        size = 20.dp,
                        selected = index == assignedTone,
                        contentDescription = "${calendar.displayName} tone ${index + 1}",
                        onClick = {
                            onConfigChange(
                                config.copy(
                                    densityCalendarTones = config.densityCalendarTones + (calendar.id to index)
                                )
                            )
                        }
                    )
                }
            }
        }
    }

    TextButton(
        onClick = { onConfigChange(config.copy(densityCalendarTones = emptyMap())) },
        enabled = config.densityCalendarTones.isNotEmpty()
    ) {
        Text("Reset to automatic")
    }
}

/** One segmented row driven entirely by an enum's [entries][Enum]-style option list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> EnumSegmentedRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                selected = option == selected,
                onClick = { onSelect(option) }
            ) {
                Text(label(option))
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Int,
    size: Dp,
    selected: Boolean,
    contentDescription: String,
    onClick: () -> Unit
) {
    val ringModifier = if (selected) {
        Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(color))
            .then(ringModifier)
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription }
    )
}

private fun colorHex(argb: Int): String = "#" + Integer.toHexString(argb).takeLast(6).uppercase()

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
