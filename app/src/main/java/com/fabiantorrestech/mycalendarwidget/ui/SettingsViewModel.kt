package com.fabiantorrestech.mycalendarwidget.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fabiantorrestech.mycalendarwidget.data.AutomationProfile
import com.fabiantorrestech.mycalendarwidget.data.CalendarEvent
import com.fabiantorrestech.mycalendarwidget.data.CalendarInfo
import com.fabiantorrestech.mycalendarwidget.data.CalendarRepository
import com.fabiantorrestech.mycalendarwidget.data.ConfigExporter
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfig
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfigRepository
import com.fabiantorrestech.mycalendarwidget.data.toWidgetConfig
import com.fabiantorrestech.mycalendarwidget.widget.WidgetSyncScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val calendarRepo: CalendarRepository,
    private val appContext: Context,
    val appWidgetId: Int
) : ViewModel() {

    val config: StateFlow<WidgetConfig> = configRepo.configFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, WidgetConfig())

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

    init {
        loadCalendars()
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

    fun updateConfig(newConfig: WidgetConfig) {
        viewModelScope.launch {
            if (newConfig.syncIntervalMinutes != config.value.syncIntervalMinutes) {
                WidgetSyncScheduler.schedule(appContext, appWidgetId, newConfig.syncIntervalMinutes)
            }
            configRepo.updateConfig(newConfig)
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
            configRepo.applyProfile(profile)
        }
    }

    fun exportConfig(context: Context) {
        _exportState.value = ExportState.InProgress
        viewModelScope.launch {
            val result = ConfigExporter.exportToDownloads(context, config.value)
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
                    calendarRepo = CalendarRepository(context),
                    appContext = context,
                    appWidgetId = appWidgetId
                )
            }
        }
    }
}
