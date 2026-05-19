package com.fabiantorrestech.mycalendarwidget.ui.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fabiantorrestech.mycalendarwidget.data.FontCategory
import com.fabiantorrestech.mycalendarwidget.data.FontMode
import com.fabiantorrestech.mycalendarwidget.data.HeaderNavStyle
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfig
import com.fabiantorrestech.mycalendarwidget.data.WidgetFont
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSection(
    config: WidgetConfig,
    onConfigChange: (WidgetConfig) -> Unit
) {
    SectionHeader(title = "Appearance")

    ToggleRow(
        label = "Month navigation",
        description = "Arrows or chips to jump between months",
        checked = config.headerNavEnabled,
        onCheckedChange = { onConfigChange(config.copy(headerNavEnabled = it)) }
    )

    if (config.headerNavEnabled) {
        val navStyles = listOf(
            HeaderNavStyle.ARROWS to "Arrows",
            HeaderNavStyle.CHIPS to "Month Chips"
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            navStyles.forEachIndexed { index, (style, label) ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = navStyles.size),
                    selected = config.headerNavStyle == style,
                    onClick = { onConfigChange(config.copy(headerNavStyle = style)) }
                ) {
                    Text(label)
                }
            }
        }
    }

    ToggleRow(
        label = "Show month in header",
        description = "Off = month only appears as list section headers (Google Calendar style)",
        checked = config.showMonthInHeader,
        onCheckedChange = { onConfigChange(config.copy(showMonthInHeader = it)) }
    )

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    ToggleRow(
        label = "Material You dynamic color",
        description = "Match system wallpaper palette (Android 12+)",
        checked = config.dynamicColor,
        onCheckedChange = { onConfigChange(config.copy(dynamicColor = it)) }
    )

    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

    Text(
        text = "Font Size Scales",
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )

    ScaleSlider(
        label = "Month header",
        savedValue = config.typographyScale.headerScale,
        onValueChangeFinished = { onConfigChange(config.copy(typographyScale = config.typographyScale.copy(headerScale = it))) }
    )
    ScaleSlider(
        label = "Weekday Headers",
        savedValue = config.typographyScale.subheaderScale,
        onValueChangeFinished = { onConfigChange(config.copy(typographyScale = config.typographyScale.copy(subheaderScale = it))) }
    )
    ScaleSlider(
        label = "Date Headers",
        savedValue = config.typographyScale.dateHeaderScale,
        onValueChangeFinished = { onConfigChange(config.copy(typographyScale = config.typographyScale.copy(dateHeaderScale = it))) }
    )
    ScaleSlider(
        label = "Event Time",
        savedValue = config.typographyScale.eventTimeScale,
        onValueChangeFinished = { onConfigChange(config.copy(typographyScale = config.typographyScale.copy(eventTimeScale = it))) }
    )
    ScaleSlider(
        label = "Event Names",
        savedValue = config.typographyScale.eventNameScale,
        onValueChangeFinished = { onConfigChange(config.copy(typographyScale = config.typographyScale.copy(eventNameScale = it))) }
    )
    ScaleSlider(
        label = "Details",
        savedValue = config.typographyScale.detailScale,
        onValueChangeFinished = { onConfigChange(config.copy(typographyScale = config.typographyScale.copy(detailScale = it))) }
    )

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    Text(
        text = "Fonts",
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
    )

    val fontModes = listOf(FontMode.DEFAULT to "Default", FontMode.UNIVERSAL to "Universal", FontMode.PER_CATEGORY to "Per-Category")
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        fontModes.forEachIndexed { index, (mode, label) ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = fontModes.size),
                selected = config.fontConfig.mode == mode,
                onClick = { onConfigChange(config.copy(fontConfig = config.fontConfig.copy(mode = mode))) }
            ) { Text(label) }
        }
    }

    when (config.fontConfig.mode) {
        FontMode.UNIVERSAL -> {
            FontDropdown(
                label = "Font",
                selected = config.fontConfig.universalFont,
                onSelected = { onConfigChange(config.copy(fontConfig = config.fontConfig.copy(universalFont = it))) }
            )
        }
        FontMode.PER_CATEGORY -> {
            FontDropdown("Month Header", config.fontConfig.monthHeaderFont) {
                onConfigChange(config.copy(fontConfig = config.fontConfig.copy(monthHeaderFont = it)))
            }
            FontDropdown("Weekday Headers", config.fontConfig.weekdayHeaderFont) {
                onConfigChange(config.copy(fontConfig = config.fontConfig.copy(weekdayHeaderFont = it)))
            }
            FontDropdown("Date Headers", config.fontConfig.dateHeaderFont) {
                onConfigChange(config.copy(fontConfig = config.fontConfig.copy(dateHeaderFont = it)))
            }
            FontDropdown("Event Time", config.fontConfig.eventTimeFont) {
                onConfigChange(config.copy(fontConfig = config.fontConfig.copy(eventTimeFont = it)))
            }
            FontDropdown("Event Names", config.fontConfig.eventNameFont) {
                onConfigChange(config.copy(fontConfig = config.fontConfig.copy(eventNameFont = it)))
            }
            FontDropdown("Details", config.fontConfig.detailFont) {
                onConfigChange(config.copy(fontConfig = config.fontConfig.copy(detailFont = it)))
            }
        }
        FontMode.DEFAULT -> {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FontDropdown(label: String, selected: WidgetFont, onSelected: (WidgetFont) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            WidgetFont.entries.forEach { font ->
                DropdownMenuItem(
                    text = { Text(font.displayName) },
                    onClick = { onSelected(font); expanded = false },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@Composable
private fun ScaleSlider(
    label: String,
    savedValue: Float,
    onValueChangeFinished: (Float) -> Unit
) {
    // Local state for smooth dragging; only saves to DataStore on finger-up
    var localValue by remember(savedValue) { mutableFloatStateOf(savedValue) }

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
                text = "${(localValue * 100).roundToInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = localValue,
            onValueChange = { localValue = it },
            onValueChangeFinished = { onValueChangeFinished(localValue) },
            valueRange = 0.7f..1.5f,
            steps = 7,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
