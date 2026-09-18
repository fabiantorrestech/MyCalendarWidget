package com.fabiantorrestech.mycalendarwidget.ui.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.mutableStateOf
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfig
import com.fabiantorrestech.mycalendarwidget.data.WidgetStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun SectionHeader(title: String) {
    HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun StepSliderRow(
    label: String,
    steps: List<Int>,
    savedIndex: Int,
    labelForIndex: (Int) -> String,
    description: String? = null,
    onIndexChangeFinished: (Int) -> Unit
) {
    var localIndex by remember(savedIndex) { mutableFloatStateOf(savedIndex.toFloat()) }
    val maxIndex = (steps.size - 1).toFloat()

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
                text = labelForIndex(localIndex.roundToInt()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = localIndex,
            onValueChange = { localIndex = it },
            onValueChangeFinished = { onIndexChangeFinished(localIndex.roundToInt()) },
            valueRange = 0f..maxIndex,
            steps = steps.size - 2,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun IntSliderRow(
    label: String,
    savedValue: Int,
    range: IntRange,
    valueLabel: (Int) -> String = { it.toString() },
    description: String? = null,
    onValueChangeFinished: (Int) -> Unit
) {
    var localValue by remember(savedValue) { mutableFloatStateOf(savedValue.toFloat()) }

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = valueLabel(localValue.roundToInt()),
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

@Composable
fun ToggleRow(
    label: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** One segmented row driven entirely by an enum's [entries][Enum]-style option list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> EnumSegmentedRow(
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

/**
 * The "Widget Style" dropdown, first in Appearance. Goes through [StyleSwitch.applyStyle]
 * so entering Density starts on the Tonal strip.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetStylePicker(config: WidgetConfig, onConfigChange: (WidgetConfig) -> Unit) {
    Text(
        text = "Widget Style",
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(bottom = 6.dp)
    )

    var styleMenuExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = styleMenuExpanded,
        onExpandedChange = { styleMenuExpanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = config.widgetStyle.displayName,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = null,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = styleMenuExpanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = styleMenuExpanded,
            onDismissRequest = { styleMenuExpanded = false }
        ) {
            // Driven by the enum itself, so a new style shows up here without another edit.
            WidgetStyle.entries.forEach { style ->
                DropdownMenuItem(
                    text = { Text(style.displayName) },
                    onClick = {
                        styleMenuExpanded = false
                        onConfigChange(StyleSwitch.applyStyle(config, style))
                    }
                )
            }
        }
    }
}
