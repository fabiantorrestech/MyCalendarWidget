package com.fabiantorrestech.mycalendarwidget.ui.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The auto-backup controls: pick a folder, then turn the backup on. The toggle stays
 * off (and greyed) until a folder exists, because there is nowhere to write.
 */
@Composable
fun AutoBackupSection(
    enabled: Boolean,
    folderLabel: String?,
    onEnabledChange: (Boolean) -> Unit,
    onChooseFolder: () -> Unit
) {
    SectionHeader(title = "Auto-backup")

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Backup folder", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = folderLabel ?: "No folder chosen",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        OutlinedButton(onClick = onChooseFolder) { Text(if (folderLabel == null) "Choose folder" else "Change") }
    }

    if (folderLabel != null) {
        ToggleRow(
            label = "Auto-backup on Done",
            description = "Writes one JSON per placed widget (linked widgets included) into the chosen folder every time you tap Done.",
            checked = enabled,
            onCheckedChange = onEnabledChange
        )
    } else {
        Text(
            text = "Choose a folder to turn on auto-backup. Each placed widget gets its own JSON there, linked widgets included, every time you tap Done.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
}
