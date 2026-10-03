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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.fabiantorrestech.mycalendarwidget.data.NotificationLockScreen
import com.fabiantorrestech.mycalendarwidget.data.NotificationPagingMode
import com.fabiantorrestech.mycalendarwidget.data.NotificationPlacement
import com.fabiantorrestech.mycalendarwidget.data.NotificationPrefs
import com.fabiantorrestech.mycalendarwidget.data.NotificationRowTap
import com.fabiantorrestech.mycalendarwidget.data.WidgetSummary

/**
 * On/off for the persistent density notification, which placed widget it draws, where
 * it sits, what its arrows move through, and what tapping an agenda row does. The switch is disabled until a widget is placed,
 * because the notification has no settings of its own to draw from.
 */
@Composable
fun NotificationSettingsCard(
    prefs: NotificationPrefs,
    widgets: List<WidgetSummary>,
    onEnabledChange: (Boolean) -> Unit,
    onFollow: (Int) -> Unit,
    onRowTapChange: (NotificationRowTap) -> Unit,
    onPlacementChange: (NotificationPlacement) -> Unit,
    onPagingModeChange: (NotificationPagingMode) -> Unit,
    onShowAddButtonChange: (Boolean) -> Unit,
    onShowRefreshButtonChange: (Boolean) -> Unit,
    onLockScreenChange: (NotificationLockScreen) -> Unit,
    onRepost: () -> Unit
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
                // Swiping it away already brings it straight back; this is for when it
                // is missing anyway (a battery saver, a cleared app, a launcher hiccup).
                OutlinedButton(
                    onClick = onRepost,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Repost notification")
                }

                SectionLabel("Uses the settings of")
                widgets.forEach { widget ->
                    RadioRow(
                        label = widget.name.ifBlank { "Widget ${widget.displayIndex}" },
                        selected = widget.appWidgetId == prefs.followedWidgetId,
                        onClick = { onFollow(widget.appWidgetId) }
                    )
                }

                SectionLabel("Where it sits")
                NotificationPlacement.entries.forEach { placement ->
                    RadioRow(
                        label = placement.displayName,
                        selected = placement == prefs.placement,
                        onClick = { onPlacementChange(placement) }
                    )
                }
                Text(
                    text = "Calls, media, conversations and pop-up alerts can still sit above it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                SectionLabel("On the lock screen")
                NotificationLockScreen.entries.forEach { lockScreen ->
                    RadioRow(
                        label = lockScreen.displayName,
                        selected = lockScreen == prefs.lockScreen,
                        onClick = { onLockScreenChange(lockScreen) }
                    )
                }
                Text(
                    text = "When shown, event titles appear there too unless your phone hides " +
                        "sensitive notification content on the lock screen (Settings › " +
                        "Notifications). Then only the count and the bar show.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                SwitchRow(
                    label = "Show ↻ (refresh) button",
                    checked = prefs.showRefreshButton,
                    onCheckedChange = onShowRefreshButtonChange
                )
                SwitchRow(
                    label = "Show + (add event) button",
                    checked = prefs.showAddButton,
                    onCheckedChange = onShowAddButtonChange
                )

                SectionLabel("The ‹ › arrows move through")
                NotificationPagingMode.entries.forEach { mode ->
                    RadioRow(
                        label = mode.displayName,
                        selected = mode == prefs.pagingMode,
                        onClick = { onPagingModeChange(mode) }
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
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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
