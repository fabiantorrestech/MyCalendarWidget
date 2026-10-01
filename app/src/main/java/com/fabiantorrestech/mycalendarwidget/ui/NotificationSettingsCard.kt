package com.fabiantorrestech.mycalendarwidget.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.fabiantorrestech.mycalendarwidget.data.NotificationPrefs
import com.fabiantorrestech.mycalendarwidget.data.NotificationRowTap
import com.fabiantorrestech.mycalendarwidget.data.WidgetSummary

/**
 * On/off for the persistent density notification, which placed widget it draws, and
 * what tapping an agenda row does. The switch is disabled until a widget is placed,
 * because the notification has no settings of its own to draw from.
 */
@Composable
fun NotificationSettingsCard(
    prefs: NotificationPrefs,
    widgets: List<WidgetSummary>,
    onEnabledChange: (Boolean) -> Unit,
    onFollow: (Int) -> Unit,
    onRowTapChange: (NotificationRowTap) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Persistent notification",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Your day's density in the notification shade. Expand it for the agenda.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = prefs.enabled,
                    onCheckedChange = onEnabledChange,
                    enabled = widgets.isNotEmpty() || prefs.enabled
                )
            }

            if (widgets.isEmpty()) {
                Text(
                    text = "Place a BridgeCal widget first: the notification uses its settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (prefs.enabled && widgets.isNotEmpty()) {
                SectionLabel("Uses the settings of")
                widgets.forEach { widget ->
                    RadioRow(
                        label = widget.name.ifBlank { "Widget ${widget.displayIndex}" },
                        selected = widget.appWidgetId == prefs.followedWidgetId,
                        onClick = { onFollow(widget.appWidgetId) }
                    )
                }

                SectionLabel("Tapping an agenda row")
                NotificationRowTap.entries.forEach { rowTap ->
                    RadioRow(
                        label = rowTap.displayName,
                        selected = rowTap == prefs.rowTap,
                        onClick = { onRowTapChange(rowTap) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
