package com.fabiantorrestech.mycalendarwidget.data

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

/** What tapping one agenda row in the expanded notification does. */
enum class NotificationRowTap(val displayName: String) {
    OPEN_EVENT("Open that event"),
    OPEN_CALENDAR("Open the calendar app")
}

/**
 * The persistent notification's own settings. Everything it draws comes from the
 * followed widget's active profile; this holds only whether it is on, which widget it
 * follows, and what a row tap does.
 */
data class NotificationPrefs(
    val enabled: Boolean = false,
    val followedWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID,
    val rowTap: NotificationRowTap = NotificationRowTap.OPEN_EVENT
)

/**
 * App-wide (not per-widget) store for [NotificationPrefs]. The DataStore is created once
 * per process and shared: two DataStores on one file throw on the second write.
 */
class NotificationPrefsRepository(context: Context) {

    private val dataStore: DataStore<Preferences> = getOrCreate(context)

    companion object {
        private val lock = Any()
        private var store: DataStore<Preferences>? = null

        private fun getOrCreate(context: Context): DataStore<Preferences> =
            synchronized(lock) {
                store ?: PreferenceDataStoreFactory.create {
                    File(context.applicationContext.filesDir, "datastore/notification_prefs.preferences_pb")
                }.also { store = it }
            }
    }

    private object Keys {
        val ENABLED = booleanPreferencesKey("enabled")
        val FOLLOWED_WIDGET_ID = intPreferencesKey("followed_widget_id")
        val ROW_TAP = stringPreferencesKey("row_tap")
    }

    val prefsFlow: Flow<NotificationPrefs> = dataStore.data.map { prefs ->
        NotificationPrefs(
            enabled = prefs[Keys.ENABLED] ?: false,
            followedWidgetId = prefs[Keys.FOLLOWED_WIDGET_ID] ?: AppWidgetManager.INVALID_APPWIDGET_ID,
            rowTap = prefs[Keys.ROW_TAP]
                ?.let { name -> NotificationRowTap.entries.firstOrNull { it.name == name } }
                ?: NotificationRowTap.OPEN_EVENT
        )
    }

    /** Turns the notification on, following [widgetId]. */
    suspend fun enable(widgetId: Int) {
        dataStore.edit {
            it[Keys.ENABLED] = true
            it[Keys.FOLLOWED_WIDGET_ID] = widgetId
        }
    }

    suspend fun disable() {
        dataStore.edit { it[Keys.ENABLED] = false }
    }

    suspend fun setFollowedWidgetId(widgetId: Int) {
        dataStore.edit { it[Keys.FOLLOWED_WIDGET_ID] = widgetId }
    }

    suspend fun setRowTap(rowTap: NotificationRowTap) {
        dataStore.edit { it[Keys.ROW_TAP] = rowTap.name }
    }
}
