package com.fabiantorrestech.mycalendarwidget.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fabiantorrestech.mycalendarwidget.ui.sections.AdvancedSection
import com.fabiantorrestech.mycalendarwidget.ui.sections.AppearanceSection
import com.fabiantorrestech.mycalendarwidget.ui.sections.CalendarFilterSection
import com.fabiantorrestech.mycalendarwidget.ui.sections.ClickRoutingSection
import com.fabiantorrestech.mycalendarwidget.ui.sections.DisplaySection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    appWidgetId: Int,
    onSaveComplete: () -> Unit
) {
    val context = LocalContext.current
    val config by viewModel.config.collectAsState()
    val calendars by viewModel.calendars.collectAsState()
    val exportState by viewModel.exportState.collectAsState()
    val previewEvents by viewModel.previewEvents.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportConfig(context, it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importConfig(context, it) }
    }

    LaunchedEffect(exportState) {
        when (val state = exportState) {
            is ExportState.Success -> {
                snackbarHostState.showSnackbar("Backup saved successfully")
                viewModel.clearExportState()
            }
            is ExportState.Error -> {
                snackbarHostState.showSnackbar("Export failed: ${state.message}")
                viewModel.clearExportState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BridgeCal Settings") },
                actions = {
                    TextButton(onClick = onSaveComplete) { Text("Done") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            item {
                Text(
                    text = "Widget Preview",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                PreviewCard(config = config, eventsByDay = previewEvents)
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                AppearanceSection(
                    config = config,
                    onConfigChange = viewModel::updateConfig
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                CalendarFilterSection(
                    config = config,
                    calendars = calendars,
                    onConfigChange = viewModel::updateConfig,
                    onToggleCalendar = { id, enabled ->
                        val currentIds = config.enabledCalendarIds
                        val newIds: Set<Long> = if (currentIds.isEmpty()) {
                            // All calendars currently enabled — build explicit set minus the toggled one
                            if (!enabled) {
                                calendars.map { it.id }.toMutableSet().also { it.remove(id) }
                            } else {
                                emptySet() // already all on, no-op
                            }
                        } else {
                            val ids = currentIds.toMutableSet()
                            if (enabled) ids.add(id) else ids.remove(id)
                            // If everything is now on, collapse back to "all" (empty set)
                            if (ids.size >= calendars.size) emptySet() else ids
                        }
                        viewModel.updateConfig(config.copy(enabledCalendarIds = newIds))
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                ClickRoutingSection(
                    config = config,
                    onConfigChange = viewModel::updateConfig
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                DisplaySection(
                    config = config,
                    onConfigChange = viewModel::updateConfig
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                AdvancedSection(
                    config = config,
                    onConfigChange = viewModel::updateConfig,
                    onApplyProfile = { viewModel.applyProfile(it) },
                    onExport = { exportLauncher.launch("bridgecal_widget_backup_$appWidgetId.json") },
                    onImport = { importLauncher.launch(arrayOf("application/json")) }
                )
            }
        }
    }
}
