package com.fabiantorrestech.mycalendarwidget.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.fabiantorrestech.mycalendarwidget.data.AutomationProfile
import com.fabiantorrestech.mycalendarwidget.data.CalendarEvent
import com.fabiantorrestech.mycalendarwidget.data.CalendarInfo
import com.fabiantorrestech.mycalendarwidget.data.CalendarRepository
import com.fabiantorrestech.mycalendarwidget.data.ConfigExporter
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfig
import com.fabiantorrestech.mycalendarwidget.data.CycleUiStyle
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfigRepository
import com.fabiantorrestech.mycalendarwidget.data.WidgetNameRepository
import com.fabiantorrestech.mycalendarwidget.data.WidgetProfileEntry
import com.fabiantorrestech.mycalendarwidget.data.WidgetProfileRepository
import com.fabiantorrestech.mycalendarwidget.data.WidgetSummary
import com.fabiantorrestech.mycalendarwidget.data.WidgetSyncLinkRepository
import com.fabiantorrestech.mycalendarwidget.data.toWidgetConfig
import com.fabiantorrestech.mycalendarwidget.widget.BridgeCalWidget
import com.fabiantorrestech.mycalendarwidget.widget.WidgetSyncScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

sealed class ExportState {
    object Idle : ExportState()
    object InProgress : ExportState()
    data class Success(val uri: Uri) : ExportState()
    data class Error(val message: String) : ExportState()
}

class SettingsViewModel(
    private val configRepo: WidgetConfigRepository,
    private val profileRepo: WidgetProfileRepository,
    private val calendarRepo: CalendarRepository,
    private val appContext: Context,
    val appWidgetId: Int
) : ViewModel() {

    val config: StateFlow<WidgetConfig> = profileRepo.activeConfigFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, WidgetConfig())

    val profiles: StateFlow<List<WidgetProfileEntry>> = profileRepo.profilesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activeProfileId: StateFlow<String> = profileRepo.activeProfileIdFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val cycleUiStyle: StateFlow<CycleUiStyle> = profileRepo.cycleUiStyleFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, CycleUiStyle.PILL)

    // Widget name is a per-widget identity (keyed by the real appWidgetId), kept separate from
    // the shared/profile config so synced widgets cannot collide on a name.
    private val _widgetName = MutableStateFlow(WidgetNameRepository.getName(appContext, appWidgetId))
    val widgetName: StateFlow<String> = _widgetName.asStateFlow()

    private val _calendars = MutableStateFlow<List<CalendarInfo>>(emptyList())
    val calendars: StateFlow<List<CalendarInfo>> = _calendars.asStateFlow()

    val previewEvents: StateFlow<Map<LocalDate, List<CalendarEvent>>> =
        config.flatMapLatest { cfg ->
            flow {
                val events = withContext(Dispatchers.IO) { calendarRepo.getEventsByDay(cfg) }
                emit(events)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    private val _hasCalendarPermission = MutableStateFlow(false)
    val hasCalendarPermission: StateFlow<Boolean> = _hasCalendarPermission.asStateFlow()

    private val _syncSource = MutableStateFlow<Int?>(
        WidgetSyncLinkRepository.getSyncSource(appContext, appWidgetId)
    )
    val syncSource: StateFlow<Int?> = _syncSource.asStateFlow()

    private val _allOtherWidgets = MutableStateFlow<List<WidgetSummary>>(emptyList())
    private val _availableWidgetsToSync = MutableStateFlow<List<WidgetSummary>>(emptyList())
    val availableWidgetsToSync: StateFlow<List<WidgetSummary>> = _availableWidgetsToSync.asStateFlow()
    val allOtherWidgets: StateFlow<List<WidgetSummary>> = _allOtherWidgets.asStateFlow()

    init {
        loadCalendars()
        loadAvailableWidgets()
        viewModelScope.launch {
            val legacyConfig = configRepo.configFlow.first()
            profileRepo.migrateIfNeeded(legacyConfig)
        }
    }

    fun refreshPermissionState(context: Context) {
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.READ_CALENDAR
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        _hasCalendarPermission.value = granted
        if (granted) loadCalendars()
    }

    private fun loadCalendars() {
        viewModelScope.launch(Dispatchers.IO) {
            _calendars.value = calendarRepo.getCalendars()
        }
    }

    private fun loadAvailableWidgets() {
        viewModelScope.launch(Dispatchers.IO) {
            val manager = GlanceAppWidgetManager(appContext)
            val glanceIds = manager.getGlanceIds(BridgeCalWidget::class.java)
            val summaries = glanceIds.mapIndexed { index, glanceId ->
                val id = manager.getAppWidgetId(glanceId)
                val config = WidgetConfigRepository(appContext, id).configFlow.first()
                val syncSource = WidgetSyncLinkRepository.getSyncSource(appContext, id)
                WidgetSummary(
                    appWidgetId = id,
                    name = WidgetNameRepository.getName(appContext, id),
                    style = config.widgetStyle,
                    displayIndex = index + 1,
                    syncSourceId = syncSource
                )
            }
            val others = summaries.filter { it.appWidgetId != appWidgetId }
            _allOtherWidgets.value = others
            // Only show widgets that are not themselves synced as potential sync targets (prevents chaining)
            _availableWidgetsToSync.value = others.filter { it.syncSourceId == null }
        }
    }

    fun setSyncSource(sourceId: Int?) {
        viewModelScope.launch {
            configRepo.setSyncSource(sourceId)
            _syncSource.value = WidgetSyncLinkRepository.getSyncSource(appContext, appWidgetId)
            loadAvailableWidgets()
        }
    }

    fun updateConfig(newConfig: WidgetConfig) {
        viewModelScope.launch {
            if (newConfig.syncIntervalMinutes != config.value.syncIntervalMinutes) {
                WidgetSyncScheduler.schedule(appContext, appWidgetId, newConfig.syncIntervalMinutes)
            }
            val profileId = profileRepo.ensureActiveProfileId(config.value)
            profileRepo.updateProfileConfig(profileId, newConfig)
        }
    }

    /**
     * Persists the per-widget name. Returns false (and saves nothing) if [name] would duplicate
     * another widget's name. Refreshes this widget so the new name renders immediately.
     */
    fun setWidgetName(name: String): Boolean {
        val trimmed = name.trim()
        if (WidgetNameRepository.isNameTaken(appContext, trimmed, appWidgetId)) return false
        WidgetNameRepository.setName(appContext, appWidgetId, trimmed)
        _widgetName.value = trimmed
        refreshThisWidget()
        return true
    }

    private fun refreshThisWidget() {
        viewModelScope.launch(Dispatchers.IO) {
            val manager = GlanceAppWidgetManager(appContext)
            val glanceId = manager.getGlanceIds(BridgeCalWidget::class.java)
                .firstOrNull { manager.getAppWidgetId(it) == appWidgetId } ?: return@launch
            BridgeCalWidget().update(appContext, glanceId)
        }
    }

    fun toggleCalendar(calendarId: Long, enabled: Boolean) {
        val current = config.value
        val ids = current.enabledCalendarIds.toMutableSet()
        if (enabled) ids.add(calendarId) else ids.remove(calendarId)
        updateConfig(current.copy(enabledCalendarIds = ids))
    }

    fun applyProfile(profile: AutomationProfile) {
        viewModelScope.launch {
            val profileId = profileRepo.ensureActiveProfileId(config.value)
            val updated = profile.toWidgetConfig(config.value)
            profileRepo.updateProfileConfig(profileId, updated)
        }
    }

    fun addProfile(name: String) {
        viewModelScope.launch {
            profileRepo.addProfile(name, config.value)
        }
    }

    fun renameProfile(id: String, name: String) {
        viewModelScope.launch { profileRepo.renameProfile(id, name) }
    }

    fun deleteProfile(id: String) {
        viewModelScope.launch { profileRepo.deleteProfile(id) }
    }

    fun setActiveProfile(id: String) {
        viewModelScope.launch { profileRepo.setActiveProfile(id) }
    }

    fun moveProfileUp(id: String) {
        val current = profiles.value
        val index = current.indexOfFirst { it.id == id }
        if (index <= 0) return
        val reordered = current.toMutableList().also {
            val tmp = it[index - 1]; it[index - 1] = it[index]; it[index] = tmp
        }
        viewModelScope.launch { profileRepo.reorderProfiles(reordered.map { it.id }) }
    }

    fun moveProfileDown(id: String) {
        val current = profiles.value
        val index = current.indexOfFirst { it.id == id }
        if (index < 0 || index >= current.lastIndex) return
        val reordered = current.toMutableList().also {
            val tmp = it[index + 1]; it[index + 1] = it[index]; it[index] = tmp
        }
        viewModelScope.launch { profileRepo.reorderProfiles(reordered.map { it.id }) }
    }

    fun setCycleUiStyle(style: CycleUiStyle) {
        viewModelScope.launch { profileRepo.setCycleUiStyle(style) }
    }

    fun exportConfig(context: Context, uri: Uri) {
        _exportState.value = ExportState.InProgress
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { ConfigExporter.exportToUri(context, uri, config.value) }
            _exportState.value = result.fold(
                onSuccess = { ExportState.Success(it) },
                onFailure = { ExportState.Error(it.message ?: "Export failed") }
            )
        }
    }

    fun importConfig(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { ConfigExporter.importFromUri(context, uri) }
            result.onSuccess { updateConfig(it) }
        }
    }

    fun clearExportState() {
        _exportState.value = ExportState.Idle
    }

    companion object {
        fun factory(context: Context, appWidgetId: Int): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SettingsViewModel(
                    configRepo = WidgetConfigRepository(context, appWidgetId),
                    profileRepo = WidgetProfileRepository(context, appWidgetId),
                    calendarRepo = CalendarRepository(context),
                    appContext = context,
                    appWidgetId = appWidgetId
                )
            }
        }
    }
}
