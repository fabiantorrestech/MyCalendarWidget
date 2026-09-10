package com.fabiantorrestech.mycalendarwidget.ui.sections

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfig
import com.fabiantorrestech.mycalendarwidget.data.WidgetStyle

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
                        onConfigChange(config.copy(widgetStyle = style))
                    }
                )
            }
        }
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    // The density style renders no event text at all, so the controls that describe that
    // text have nothing to act on and are hidden rather than left inert.
    if (config.widgetStyle != WidgetStyle.DENSITY) {
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
    }

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
    ToggleRow(
        label = "Always show today",
        description = "Pin today in the list even when you have no events",
        checked = config.alwaysShowToday,
        onCheckedChange = { onConfigChange(config.copy(alwaysShowToday = it)) }
    )
    if (config.widgetStyle != WidgetStyle.DENSITY) {
        ToggleRow(
            label = "Show multi-day events on every day they span",
            description = "Duplicate spanning events onto each covered day in the visible window",
            checked = config.showSpanningEventsEachDay,
            onCheckedChange = { onConfigChange(config.copy(showSpanningEventsEachDay = it)) }
        )
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

    ToggleRow(
        label = "Show Quick Add (+) button",
        checked = config.showQuickAddFab,
        onCheckedChange = { onConfigChange(config.copy(showQuickAddFab = it)) }
    )
    ToggleRow(
        label = "Show refresh button",
        description = "Manually force the widget to re-fetch calendar data",
        checked = config.showRefreshButton,
        onCheckedChange = { onConfigChange(config.copy(showRefreshButton = it)) }
    )
    ToggleRow(
        label = "Strict Grid Mode",
        description = "Remove widget padding for flush edge-to-edge placement",
        checked = config.strictGridMode,
        onCheckedChange = { onConfigChange(config.copy(strictGridMode = it)) }
    )
}
