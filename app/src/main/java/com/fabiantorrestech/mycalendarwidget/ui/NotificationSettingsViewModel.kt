package com.fabiantorrestech.mycalendarwidget.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fabiantorrestech.mycalendarwidget.data.NotificationLockScreen
import com.fabiantorrestech.mycalendarwidget.data.NotificationPagingMode
import com.fabiantorrestech.mycalendarwidget.data.NotificationPlacement
import com.fabiantorrestech.mycalendarwidget.data.NotificationPrefs
import com.fabiantorrestech.mycalendarwidget.data.NotificationPrefsRepository
import com.fabiantorrestech.mycalendarwidget.data.NotificationRowTap
import com.fabiantorrestech.mycalendarwidget.notification.DensityNotifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The main screen's "Persistent notification" card. Every change is saved and then
 * pushed straight to the notification, so turning it on, switching the followed widget
 * or the row action shows up in the shade at once.
 */
class NotificationSettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = NotificationPrefsRepository(app)

    val prefs: StateFlow<NotificationPrefs> =
        repo.prefsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationPrefs())

    fun enable(widgetId: Int) = saveAndRefresh { repo.enable(widgetId) }

    fun disable() = saveAndRefresh { repo.disable() }

    fun follow(widgetId: Int) = saveAndRefresh { repo.setFollowedWidgetId(widgetId) }

    fun setRowTap(rowTap: NotificationRowTap) = saveAndRefresh { repo.setRowTap(rowTap) }

    fun setPlacement(placement: NotificationPlacement) = saveAndRefresh { repo.setPlacement(placement) }

    fun setPagingMode(mode: NotificationPagingMode) = saveAndRefresh { repo.setPagingMode(mode) }

    fun setShowAddButton(show: Boolean) = saveAndRefresh { repo.setShowAddButton(show) }

    fun setShowRefreshButton(show: Boolean) = saveAndRefresh { repo.setShowRefreshButton(show) }

    fun setLockScreen(lockScreen: NotificationLockScreen) = saveAndRefresh { repo.setLockScreen(lockScreen) }

    /** Puts the notification back up now, for when it has gone missing. */
    fun repost() {
        viewModelScope.launch(Dispatchers.IO) { DensityNotifier.repost(getApplication()) }
    }

    private fun saveAndRefresh(save: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            save()
            DensityNotifier.refresh(getApplication())
        }
    }
}
