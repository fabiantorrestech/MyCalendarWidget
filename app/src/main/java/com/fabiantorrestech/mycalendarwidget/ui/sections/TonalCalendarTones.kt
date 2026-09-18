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

/** The busy-colour presets and, while Tonal is selected, the per-calendar tone list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DensityLookControls(
    config: WidgetConfig,
    calendars: List<CalendarInfo>,
    onConfigChange: (WidgetConfig) -> Unit
) {
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

