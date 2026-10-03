package com.fabiantorrestech.mycalendarwidget.data

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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
 * Where the notification sits. [PINNED] makes it a colorized foreground-service
 * notification, which Pixel sorts above conversations and every ordinary notification,
 * below only calls (see doc/adr/0003). [TOP] is the main Notifications section at the
 * highest priority an ordinary notification may ask for, without sound.
 */
enum class NotificationPlacement(val displayName: String, val hint: String) {
    PINNED(
        "Pinned above messages (recommended)",
        "Only phone calls, and an alert for its first few seconds, sit above it. " +
            "BridgeCal shows under Active apps while pinned."
    ),
    TOP(
        "Top of the notifications",
        "Calls, media, conversations and pop-up alerts can still sit above it."
    ),
    SILENT(
        "Silent section",
        "With the quiet notifications at the bottom of the shade."
    )
}

/**
 * Whether the notification appears on a secure lock screen. Under [SHOW], Android alone
 * decides how much: the content-free version when the phone hides sensitive content,
 * the full one (event titles included) otherwise. An app cannot override that.
 */
enum class NotificationLockScreen(val displayName: String) {
    SHOW("Show it (recommended)"),
    HIDE("Hide it from the lock screen")
}

/** What the expanded notification's ‹ › arrows move through. */
enum class NotificationPagingMode(val displayName: String) {
    AGENDA_PAGES("Pages of upcoming events"),
    DAYS("One day at a time")
}

/**
 * The persistent notification's own settings. Everything it draws comes from the
 * followed widget's active profile; this holds only whether it is on, which widget it
 * follows, what a row tap does, where it sits and what its arrows page through.
 *
 * [pageOffset] and [pageTouchedAtMillis] are where the arrows were left and when; the
 * offset counts only while the last tap is recent (see `NotificationPaging`).
 */
data class NotificationPrefs(
    val enabled: Boolean = false,
    val followedWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID,
    val rowTap: NotificationRowTap = NotificationRowTap.OPEN_EVENT,
    val placement: NotificationPlacement = NotificationPlacement.PINNED,
    val pagingMode: NotificationPagingMode = NotificationPagingMode.AGENDA_PAGES,
    val pageOffset: Int = 0,
    val pageTouchedAtMillis: Long = 0L,
    val showAddButton: Boolean = true,
    val showRefreshButton: Boolean = true,
    val lockScreen: NotificationLockScreen = NotificationLockScreen.SHOW
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
        // A new key rather than "placement": Pinned became the default after Top/Silent
        // shipped, and the user wanted existing setups moved onto it too, so any choice
        // stored under the old key is deliberately left unread.
        val PLACEMENT = stringPreferencesKey("placement_v2")
        val PAGING_MODE = stringPreferencesKey("paging_mode")
        val PAGE_OFFSET = intPreferencesKey("page_offset")
        val PAGE_TOUCHED_AT = longPreferencesKey("page_touched_at")
        val SHOW_ADD_BUTTON = booleanPreferencesKey("show_add_button")
        val SHOW_REFRESH_BUTTON = booleanPreferencesKey("show_refresh_button")
        val LOCK_SCREEN = stringPreferencesKey("lock_screen")
    }

    val prefsFlow: Flow<NotificationPrefs> = dataStore.data.map { prefs ->
        NotificationPrefs(
            enabled = prefs[Keys.ENABLED] ?: false,
            followedWidgetId = prefs[Keys.FOLLOWED_WIDGET_ID] ?: AppWidgetManager.INVALID_APPWIDGET_ID,
            rowTap = prefs[Keys.ROW_TAP]
                ?.let { name -> NotificationRowTap.entries.firstOrNull { it.name == name } }
                ?: NotificationRowTap.OPEN_EVENT,
            placement = prefs[Keys.PLACEMENT]
                ?.let { name -> NotificationPlacement.entries.firstOrNull { it.name == name } }
                ?: NotificationPlacement.PINNED,
            pagingMode = prefs[Keys.PAGING_MODE]
                ?.let { name -> NotificationPagingMode.entries.firstOrNull { it.name == name } }
                ?: NotificationPagingMode.AGENDA_PAGES,
            pageOffset = prefs[Keys.PAGE_OFFSET] ?: 0,
            pageTouchedAtMillis = prefs[Keys.PAGE_TOUCHED_AT] ?: 0L,
            showAddButton = prefs[Keys.SHOW_ADD_BUTTON] ?: true,
            showRefreshButton = prefs[Keys.SHOW_REFRESH_BUTTON] ?: true,
            lockScreen = prefs[Keys.LOCK_SCREEN]
                ?.let { name -> NotificationLockScreen.entries.firstOrNull { it.name == name } }
                ?: NotificationLockScreen.SHOW
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

    suspend fun setLockScreen(lockScreen: NotificationLockScreen) {
        dataStore.edit { it[Keys.LOCK_SCREEN] = lockScreen.name }
    }

    suspend fun setShowRefreshButton(show: Boolean) {
        dataStore.edit { it[Keys.SHOW_REFRESH_BUTTON] = show }
    }

    suspend fun setShowAddButton(show: Boolean) {
        dataStore.edit { it[Keys.SHOW_ADD_BUTTON] = show }
    }

    suspend fun setPlacement(placement: NotificationPlacement) {
        dataStore.edit { it[Keys.PLACEMENT] = placement.name }
    }

    /** Switching what the arrows page through starts again from the first page. */
    suspend fun setPagingMode(mode: NotificationPagingMode) {
        dataStore.edit {
            it[Keys.PAGING_MODE] = mode.name
            it[Keys.PAGE_OFFSET] = 0
        }
    }

    /**
     * Records an arrow tap: [offset] is the page (or day) the tapped arrow pointed at,
     * worked out when the notification was drawn, and [nowMillis] restarts the idle
     * timer that snaps the arrows back.
     */
    suspend fun setPage(offset: Int, nowMillis: Long) {
        dataStore.edit {
            it[Keys.PAGE_OFFSET] = offset
            it[Keys.PAGE_TOUCHED_AT] = nowMillis
        }
    }
}
