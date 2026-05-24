package com.fabiantorrestech.mycalendarwidget.ui.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fabiantorrestech.mycalendarwidget.data.CalendarLaunchView
import com.fabiantorrestech.mycalendarwidget.data.DefaultClickTarget
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfig

private data class ClickTargetOption(
    val target: DefaultClickTarget,
    val label: String,
    val description: String
)

private val clickTargetOptions = listOf(
    ClickTargetOption(DefaultClickTarget.SYSTEM_DEFAULT, "System Default", "Opens with whichever calendar app you have set as default"),
    ClickTargetOption(DefaultClickTarget.GCAL, "Google Calendar", "Always open in Google Calendar (must be installed)"),
    ClickTargetOption(DefaultClickTarget.DIGICAL, "DigiCal", "Always open in DigiCal (must be installed)")
)

@Composable
fun ClickRoutingSection(
    config: WidgetConfig,
    onConfigChange: (WidgetConfig) -> Unit
) {
    SectionHeader(title = "Click Routing")

    Text(
        text = "Default app for opening events",
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    clickTargetOptions.forEach { option ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = config.defaultClickTarget == option.target,
                onClick = { onConfigChange(config.copy(defaultClickTarget = option.target)) }
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = option.label, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    Text(
        text = "Events with a location automatically open Maps. Events with a Zoom/Meet/Teams link automatically open the meeting.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp)
    )

    if (config.defaultClickTarget != DefaultClickTarget.SYSTEM_DEFAULT) {
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        CalendarLaunchViewSection(config, onConfigChange)
    }
}

private data class LaunchViewOption(
    val view: CalendarLaunchView,
    val label: String,
    val description: String
)

private val launchViewOptions = listOf(
    LaunchViewOption(CalendarLaunchView.DEFAULT,    "App default",  "Opens in the last-used view"),
    LaunchViewOption(CalendarLaunchView.DAY,        "Day",          "Day view"),
    LaunchViewOption(CalendarLaunchView.WEEK,       "Week",         "Week view"),
    LaunchViewOption(CalendarLaunchView.WEEK_AGENDA,"Week agenda",  "DigiCal only — GCal falls back to week view"),
    LaunchViewOption(CalendarLaunchView.MONTH,      "Month",        "Month view"),
    LaunchViewOption(CalendarLaunchView.TEXT_MONTH, "Text month",   "DigiCal only — GCal falls back to month view"),
    LaunchViewOption(CalendarLaunchView.YEAR,       "Year",         "DigiCal only — GCal falls back to app default"),
    LaunchViewOption(CalendarLaunchView.AGENDA,     "Agenda",       "Agenda / schedule list view")
)

@Composable
private fun CalendarLaunchViewSection(
    config: WidgetConfig,
    onConfigChange: (WidgetConfig) -> Unit
) {
    Text(
        text = "Calendar launch view",
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    launchViewOptions.forEach { option ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = config.calendarLaunchView == option.view,
                onClick = { onConfigChange(config.copy(calendarLaunchView = option.view)) }
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = option.label, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    Text(
        text = "Note: view routing is best-effort. Week agenda, Text month, and Year are DigiCal-only — Google Calendar will use the nearest available view.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp)
    )
}
