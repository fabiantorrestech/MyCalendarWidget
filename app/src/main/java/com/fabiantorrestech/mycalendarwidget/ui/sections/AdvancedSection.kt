package com.fabiantorrestech.mycalendarwidget.ui.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fabiantorrestech.mycalendarwidget.data.AutomationProfile
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfig

@Composable
fun AdvancedSection(
    config: WidgetConfig,
    onConfigChange: (WidgetConfig) -> Unit,
    onApplyProfile: (AutomationProfile) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    SectionHeader(title = "Advanced")

    val syncSteps = listOf(0, 2, 5, 10, 15, 30)
    val syncIndex = syncSteps.indexOf(config.syncIntervalMinutes).coerceAtLeast(0)
    StepSliderRow(
        label = "Sync interval",
        steps = syncSteps,
        savedIndex = syncIndex,
        labelForIndex = { idx ->
            if (syncSteps[idx] == 0) "Instant (event-driven)"
            else "Every ${syncSteps[idx]} min"
        },
        onIndexChangeFinished = { idx ->
            onConfigChange(config.copy(syncIntervalMinutes = syncSteps[idx]))
        }
    )

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    Text(
        text = "Layout Profiles",
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(bottom = 4.dp)
    )
    Text(
        text = "Quickly apply a preset configuration. Also switchable via Tasker/MacroDroid broadcast.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AutomationProfile.entries.forEach { profile ->
            val isActive = config.activeProfile == profile
            if (isActive) {
                Button(
                    onClick = { onApplyProfile(profile) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(profile.name.lowercase().replaceFirstChar { it.uppercase() })
                }
            } else {
                OutlinedButton(
                    onClick = { onApplyProfile(profile) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(profile.name.lowercase().replaceFirstChar { it.uppercase() })
                }
            }
        }
    }

    Text(
        text = "Automation intent: com.fabiantorrestech.mycalendarwidget.SET_PROFILE\nExtra: profile = \"standard\" | \"dense\" | \"minimal\"",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp)
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Configuration Backup",
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onExport,
            modifier = Modifier.weight(1f)
        ) {
            Text("Export to Downloads")
        }
        OutlinedButton(
            onClick = onImport,
            modifier = Modifier.weight(1f)
        ) {
            Text("Import Config")
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
}
