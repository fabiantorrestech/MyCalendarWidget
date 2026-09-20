package com.fabiantorrestech.mycalendarwidget.ui.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The last thing on the settings screen: a red "Reset all defaults" that asks first.
 * Every setting of this widget goes back to its defaults — all profiles, the widget
 * name and any sync link — which is why it is red and why it asks.
 */
@Composable
fun ResetSection(onConfirmReset: () -> Unit) {
    var confirming by rememberSaveable { mutableStateOf(false) }

    Spacer(modifier = Modifier.height(24.dp))
    OutlinedButton(
        onClick = { confirming = true },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
    ) {
        Text("Reset all defaults")
    }
    Spacer(modifier = Modifier.height(24.dp))

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("Are you sure?") },
            text = {
                Text(
                    "Every setting of this widget goes back to its defaults: all profiles, " +
                        "the widget name and any sync link."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirming = false
                        onConfirmReset()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) { Text("Cancel") }
            }
        )
    }
}
