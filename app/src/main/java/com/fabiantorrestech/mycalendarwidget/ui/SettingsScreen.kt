package com.fabiantorrestech.mycalendarwidget.ui

import android.appwidget.AppWidgetManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fabiantorrestech.mycalendarwidget.data.CycleUiStyle
import com.fabiantorrestech.mycalendarwidget.data.WidgetSummary
import com.fabiantorrestech.mycalendarwidget.ui.sections.AdvancedSection
import com.fabiantorrestech.mycalendarwidget.ui.sections.AppearanceSection
import com.fabiantorrestech.mycalendarwidget.ui.sections.CalendarFilterSection
import com.fabiantorrestech.mycalendarwidget.ui.sections.ClickRoutingSection
import com.fabiantorrestech.mycalendarwidget.ui.sections.DisplaySection
import com.fabiantorrestech.mycalendarwidget.ui.sections.ProfilesSection

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
    val syncSource by viewModel.syncSource.collectAsState()
    val availableWidgets by viewModel.availableWidgetsToSync.collectAsState()
    val allOtherWidgets by viewModel.allOtherWidgets.collectAsState()
    val profiles by viewModel.profiles.collectAsState()
    val activeProfileId by viewModel.activeProfileId.collectAsState()
    val cycleUiStyle by viewModel.cycleUiStyle.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showSyncDialog by rememberSaveable { mutableStateOf(false) }
    var selectedSyncId by rememberSaveable { mutableIntStateOf(-1) }
    var showAddProfileDialog by rememberSaveable { mutableStateOf(false) }
    var newProfileName by rememberSaveable { mutableStateOf("") }

    // Local state for the name field — avoids a DataStore write on every keystroke
    var localWidgetName by rememberSaveable { mutableStateOf(config.widgetName) }
    LaunchedEffect(config.widgetName) {
        if (localWidgetName != config.widgetName) localWidgetName = config.widgetName
    }

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
                    if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                        IconButton(onClick = { showSyncDialog = true }) {
                            Icon(
                                imageVector = if (syncSource != null) Icons.Default.LinkOff else Icons.Default.Link,
                                contentDescription = if (syncSource != null) "Unlink sync" else "Sync with widget"
                            )
                        }
                    }
                    TextButton(onClick = onSaveComplete) { Text("Done") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (showAddProfileDialog) {
            AlertDialog(
                onDismissRequest = { showAddProfileDialog = false; newProfileName = "" },
                title = { Text("New profile") },
                text = {
                    OutlinedTextField(
                        value = newProfileName,
                        onValueChange = { newProfileName = it },
                        label = { Text("Profile name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newProfileName.isNotBlank()) {
                                viewModel.addProfile(newProfileName.trim())
                                showAddProfileDialog = false
                                newProfileName = ""
                            }
                        }
                    ) { Text("Create") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddProfileDialog = false; newProfileName = "" }) { Text("Cancel") }
                }
            )
        }

        if (showSyncDialog) {
            SyncDialog(
                syncSource = syncSource,
                availableWidgets = availableWidgets,
                selectedId = selectedSyncId,
                onSelectId = { selectedSyncId = it },
                onConfirm = {
                    viewModel.setSyncSource(if (selectedSyncId == -1) null else selectedSyncId)
                    showSyncDialog = false
                    selectedSyncId = -1
                },
                onUnlink = {
                    viewModel.setSyncSource(null)
                    showSyncDialog = false
                },
                onDismiss = {
                    showSyncDialog = false
                    selectedSyncId = -1
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 4.dp)
                ) {
                    items(profiles, key = { it.id }) { profile ->
                        FilterChip(
                            selected = profile.id == activeProfileId,
                            onClick = { viewModel.setActiveProfile(profile.id) },
                            label = { Text(profile.name) }
                        )
                    }
                    item {
                        SuggestionChip(
                            onClick = { showAddProfileDialog = true },
                            label = { Text("+ Add") }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (profiles.size >= 2) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        )
                    ) {
                        Text(
                            text = "⚡ More profiles = more memory and battery. Keep the count low for best performance.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            val currentSyncSource = syncSource
            if (currentSyncSource != null) {
                val sourceSummary = allOtherWidgets.firstOrNull { it.appWidgetId == currentSyncSource }
                val sourceLabel = sourceSummary?.let { s -> s.name.ifBlank { "Widget ${s.displayIndex}" } }
                    ?: "Widget $currentSyncSource"
                item {
                    SyncBanner(sourceLabel = sourceLabel)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item {
                OutlinedTextField(
                    value = localWidgetName,
                    onValueChange = { localWidgetName = it },
                    label = { Text("Widget name (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        viewModel.updateConfig(config.copy(widgetName = localWidgetName))
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused && localWidgetName != config.widgetName) {
                                viewModel.updateConfig(config.copy(widgetName = localWidgetName))
                            }
                        }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                ProfilesSection(
                    profiles = profiles,
                    activeProfileId = activeProfileId,
                    cycleUiStyle = cycleUiStyle,
                    onRename = { id, name -> viewModel.renameProfile(id, name) },
                    onDelete = { viewModel.deleteProfile(it) },
                    onMoveUp = { viewModel.moveProfileUp(it) },
                    onMoveDown = { viewModel.moveProfileDown(it) },
                    onCycleStyleChange = { viewModel.setCycleUiStyle(it) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

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

@Composable
private fun SyncBanner(sourceLabel: String) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Link,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Config shared with $sourceLabel — changes here affect both widgets.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SyncDialog(
    syncSource: Int?,
    availableWidgets: List<WidgetSummary>,
    selectedId: Int,
    onSelectId: (Int) -> Unit,
    onConfirm: () -> Unit,
    onUnlink: () -> Unit,
    onDismiss: () -> Unit
) {
    if (syncSource != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Synced config") },
            text = {
                Text("This widget shares its config with another widget. Changes made here apply to both. Tap Unlink to make this widget independent.")
            },
            confirmButton = {
                TextButton(onClick = onUnlink) { Text("Unlink") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Sync config with…") },
            text = {
                if (availableWidgets.isEmpty()) {
                    Text("No other widgets are available to sync with.")
                } else {
                    LazyColumn {
                        items(availableWidgets) { widget ->
                            val label = widget.name.ifBlank { "Widget ${widget.displayIndex}" }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectId(widget.appWidgetId) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedId == widget.appWidgetId,
                                    onClick = { onSelectId(widget.appWidgetId) }
                                )
                                Text(
                                    text = "$label — ${widget.style.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirm,
                    enabled = selectedId != -1 && availableWidgets.isNotEmpty()
                ) { Text("Sync") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        )
    }
}
