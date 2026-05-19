package com.fabiantorrestech.mycalendarwidget.ui.sections

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fabiantorrestech.mycalendarwidget.data.CalendarInfo
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfig

@Composable
fun CalendarFilterSection(
    config: WidgetConfig,
    calendars: List<CalendarInfo>,
    onConfigChange: (WidgetConfig) -> Unit,
    onToggleCalendar: (Long, Boolean) -> Unit
) {
    SectionHeader(title = "Calendars")

    if (calendars.isEmpty()) {
        Text(
            text = "No calendars found. Calendar permission may be required.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    } else {
        calendars.forEach { calendar ->
            val isEnabled = config.enabledCalendarIds.isEmpty() || calendar.id in config.enabledCalendarIds
            CalendarRow(
                calendar = calendar,
                enabled = isEnabled,
                onToggle = { onToggleCalendar(calendar.id, it) }
            )
        }
    }

    Spacer(modifier = Modifier.padding(top = 8.dp))

    var regexError by remember(config.keywordFilter, config.filterIsRegex) {
        mutableStateOf(
            if (config.filterIsRegex && config.keywordFilter.isNotBlank()) {
                runCatching { Regex(config.keywordFilter) }.exceptionOrNull()?.message
            } else null
        )
    }

    OutlinedTextField(
        value = config.keywordFilter,
        onValueChange = {
            onConfigChange(config.copy(keywordFilter = it))
            regexError = if (config.filterIsRegex && it.isNotBlank()) {
                runCatching { Regex(it) }.exceptionOrNull()?.message
            } else null
        },
        label = { Text("Keyword / Regex filter") },
        placeholder = { Text("e.g. Tentative") },
        supportingText = {
            if (regexError != null) {
                Text(
                    text = "Invalid regex: $regexError",
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text("Events matching this pattern will be hidden")
            }
        },
        isError = regexError != null,
        modifier = Modifier.fillMaxWidth()
    )

    ToggleRow(
        label = "Treat as regular expression",
        checked = config.filterIsRegex,
        onCheckedChange = { onConfigChange(config.copy(filterIsRegex = it)) }
    )
}

@Composable
private fun CalendarRow(
    calendar: CalendarInfo,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(12.dp)) {
            drawCircle(Color(calendar.color))
        }
        Spacer(modifier = Modifier.width(8.dp))
        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
            Text(text = calendar.displayName, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = calendar.accountName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}
