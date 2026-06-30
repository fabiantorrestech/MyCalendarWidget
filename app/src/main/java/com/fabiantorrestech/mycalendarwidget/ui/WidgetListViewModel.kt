package com.fabiantorrestech.mycalendarwidget.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfigRepository
import com.fabiantorrestech.mycalendarwidget.data.WidgetNameRepository
import com.fabiantorrestech.mycalendarwidget.data.WidgetSummary
import com.fabiantorrestech.mycalendarwidget.data.WidgetSyncLinkRepository
import com.fabiantorrestech.mycalendarwidget.widget.BridgeCalWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WidgetListViewModel(app: Application) : AndroidViewModel(app) {

    private val _widgets = MutableStateFlow<List<WidgetSummary>>(emptyList())
    val widgets: StateFlow<List<WidgetSummary>> = _widgets.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>().applicationContext
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(BridgeCalWidget::class.java)
            val summaries = glanceIds.mapIndexed { index, glanceId ->
                val appWidgetId = manager.getAppWidgetId(glanceId)
                val config = WidgetConfigRepository(context, appWidgetId).configFlow.first()
                val syncSource = WidgetSyncLinkRepository.getSyncSource(context, appWidgetId)
                WidgetSummary(
                    appWidgetId = appWidgetId,
                    name = WidgetNameRepository.getName(context, appWidgetId),
                    style = config.widgetStyle,
                    displayIndex = index + 1,
                    syncSourceId = syncSource
                )
            }
            _widgets.value = summaries
        }
    }
}
