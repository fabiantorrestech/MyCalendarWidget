package com.fabiantorrestech.mycalendarwidget.ui.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfig
import com.fabiantorrestech.mycalendarwidget.data.WidgetStyle
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplaySection(
    config: WidgetConfig,
    onConfigChange: (WidgetConfig) -> Unit
) {
    SectionHeader(title = "Display")

    Text(
        text = "Widget Style",
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(bottom = 6.dp)
    )

    val styles = listOf(
        WidgetStyle.AGENDA to "Standard",
        WidgetStyle.GCAL to "GCal Style 1",
        WidgetStyle.GCAL_LEFT to "GCal Style 2"
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        styles.forEachIndexed { index, (style, label) ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = styles.size),
                selected = config.widgetStyle == style,
                onClick = { onConfigChange(config.copy(widgetStyle = style)) }
            ) {
                Text(label)
            }
        }
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    IntSliderRow(
        label = "Max title lines",
        savedValue = config.maxTitleLines,
        range = 1..3,
        onValueChangeFinished = { onConfigChange(config.copy(maxTitleLines = it)) }
    )
    IntSliderRow(
        label = "Max detail lines",
        savedValue = config.maxDetailLines,
        range = 0..2,
        onValueChangeFinished = { onConfigChange(config.copy(maxDetailLines = it)) }
    )

    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

    ToggleRow(
        label = "Show event location",
        checked = config.showLocation,
        onCheckedChange = { onConfigChange(config.copy(showLocation = it)) }
    )
    ToggleRow(
        label = "Show event description",
        checked = config.showDescription,
        onCheckedChange = { onConfigChange(config.copy(showDescription = it)) }
    )

    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

    IntSliderRow(
        label = "Days to look ahead",
        savedValue = config.daysAheadToLoad,
        range = 7..90,
        onValueChangeFinished = { onConfigChange(config.copy(daysAheadToLoad = it)) }
    )
    ToggleRow(
        label = "Show empty days",
        description = "Include days with no events in the list",
        checked = config.showEmptyDays,
        onCheckedChange = { onConfigChange(config.copy(showEmptyDays = it)) }
    )

    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

    ToggleRow(
        label = "Show Quick Add (+) button",
        checked = config.showQuickAddFab,
        onCheckedChange = { onConfigChange(config.copy(showQuickAddFab = it)) }
    )
    ToggleRow(
        label = "Strict Grid Mode",
        description = "Remove widget padding for flush edge-to-edge placement",
        checked = config.strictGridMode,
        onCheckedChange = { onConfigChange(config.copy(strictGridMode = it)) }
    )
}

@Composable
private fun IntSliderRow(
    label: String,
    savedValue: Int,
    range: IntRange,
    onValueChangeFinished: (Int) -> Unit
) {
    var localValue by remember(savedValue) { mutableFloatStateOf(savedValue.toFloat()) }

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = localValue.roundToInt().toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = localValue,
            onValueChange = { localValue = it },
            onValueChangeFinished = { onValueChangeFinished(localValue.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = range.last - range.first - 1,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
