package com.fabiantorrestech.mycalendarwidget.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class WidgetProfileRepository(private val context: Context, private val widgetId: Int) {

    private val dataStore: DataStore<Preferences> = getOrCreate(
        context, WidgetSyncLinkRepository.resolveSource(context, widgetId)
    )

    companion object {
        private val lock = Any()
        private val stores = mutableMapOf<Int, DataStore<Preferences>>()

        fun getOrCreate(context: Context, resolvedId: Int): DataStore<Preferences> =
            synchronized(lock) {
                stores.getOrPut(resolvedId) {
                    PreferenceDataStoreFactory.create {
                        File(
                            context.applicationContext.filesDir,
                            "datastore/widget_profiles_${resolvedId}.preferences_pb"
                        )
                    }
                }
            }

        fun clearCache(widgetId: Int) = synchronized(lock) { stores.remove(widgetId) }
    }

    private object Keys {
        val PROFILES_JSON = stringPreferencesKey("profiles_json")
        val ACTIVE_PROFILE_ID = stringPreferencesKey("active_profile_id")
        val CYCLE_UI_STYLE = stringPreferencesKey("cycle_ui_style")
    }

    val profilesFlow: Flow<List<WidgetProfileEntry>> = dataStore.data.map { prefs ->
        parseProfiles(prefs[Keys.PROFILES_JSON] ?: "")
    }

    val activeProfileIdFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_PROFILE_ID] ?: ""
    }

    val activeConfigFlow: Flow<WidgetConfig> = dataStore.data.map { prefs ->
        val profiles = parseProfiles(prefs[Keys.PROFILES_JSON] ?: "")
        val activeId = prefs[Keys.ACTIVE_PROFILE_ID] ?: ""
        profiles.firstOrNull { it.id == activeId }?.config
            ?: profiles.firstOrNull()?.config
            ?: WidgetConfig()
    }

    val cycleUiStyleFlow: Flow<CycleUiStyle> = dataStore.data.map { prefs ->
        prefs[Keys.CYCLE_UI_STYLE]
            ?.let { runCatching { CycleUiStyle.valueOf(it) }.getOrNull() }
            ?: CycleUiStyle.PILL
    }

    suspend fun addProfile(name: String, config: WidgetConfig = WidgetConfig()) {
        dataStore.edit { prefs ->
            val profiles = parseProfiles(prefs[Keys.PROFILES_JSON] ?: "").toMutableList()
            profiles.add(WidgetProfileEntry(UUID.randomUUID().toString(), name, config))
            prefs[Keys.PROFILES_JSON] = serializeProfiles(profiles)
        }
    }

    suspend fun renameProfile(profileId: String, name: String) {
        dataStore.edit { prefs ->
            val profiles = parseProfiles(prefs[Keys.PROFILES_JSON] ?: "")
                .map { if (it.id == profileId) it.copy(name = name) else it }
            prefs[Keys.PROFILES_JSON] = serializeProfiles(profiles)
        }
    }

    suspend fun updateProfileConfig(profileId: String, config: WidgetConfig) {
        dataStore.edit { prefs ->
            val profiles = parseProfiles(prefs[Keys.PROFILES_JSON] ?: "")
                .map { if (it.id == profileId) it.copy(config = config) else it }
            prefs[Keys.PROFILES_JSON] = serializeProfiles(profiles)
        }
    }

    suspend fun deleteProfile(profileId: String) {
        dataStore.edit { prefs ->
            val profiles = parseProfiles(prefs[Keys.PROFILES_JSON] ?: "")
            if (profiles.size <= 1) return@edit
            val newProfiles = profiles.filter { it.id != profileId }
            val activeId = prefs[Keys.ACTIVE_PROFILE_ID] ?: ""
            if (activeId == profileId) {
                val deletedIndex = profiles.indexOfFirst { it.id == profileId }
                val next = newProfiles.getOrNull(deletedIndex) ?: newProfiles.last()
                prefs[Keys.ACTIVE_PROFILE_ID] = next.id
            }
            prefs[Keys.PROFILES_JSON] = serializeProfiles(newProfiles)
        }
    }

    suspend fun setActiveProfile(profileId: String) {
        dataStore.edit { prefs ->
            prefs[Keys.ACTIVE_PROFILE_ID] = profileId
        }
    }

    suspend fun cycleProfile(direction: Int) {
        dataStore.edit { prefs ->
            val profiles = parseProfiles(prefs[Keys.PROFILES_JSON] ?: "")
            if (profiles.size <= 1) return@edit
            val activeId = prefs[Keys.ACTIVE_PROFILE_ID] ?: ""
            val currentIndex = profiles.indexOfFirst { it.id == activeId }.takeIf { it >= 0 } ?: 0
            val nextIndex = ((currentIndex + direction) + profiles.size) % profiles.size
            prefs[Keys.ACTIVE_PROFILE_ID] = profiles[nextIndex].id
        }
    }

    suspend fun reorderProfiles(orderedIds: List<String>) {
        dataStore.edit { prefs ->
            val profiles = parseProfiles(prefs[Keys.PROFILES_JSON] ?: "")
            val orderedSet = orderedIds.toSet()
            val reordered = orderedIds.mapNotNull { id -> profiles.firstOrNull { it.id == id } } +
                profiles.filter { it.id !in orderedSet }
            prefs[Keys.PROFILES_JSON] = serializeProfiles(reordered)
        }
    }

    suspend fun setCycleUiStyle(style: CycleUiStyle) {
        dataStore.edit { prefs ->
            prefs[Keys.CYCLE_UI_STYLE] = style.name
        }
    }

    suspend fun migrateIfNeeded(legacyConfig: WidgetConfig) {
        dataStore.edit { prefs ->
            if (prefs[Keys.PROFILES_JSON].isNullOrBlank()) {
                val defaultId = UUID.randomUUID().toString()
                val defaultProfile = WidgetProfileEntry(defaultId, "Default", legacyConfig)
                prefs[Keys.PROFILES_JSON] = serializeProfiles(listOf(defaultProfile))
                prefs[Keys.ACTIVE_PROFILE_ID] = defaultId
            }
        }
    }

    /**
     * Returns a valid active profile id, creating a "Default" profile (seeded from [fallback])
     * and/or repairing a missing/blank active id in a single atomic edit. Use this before writing
     * config so edits are never silently dropped when no profile has been created yet (e.g. the
     * widget config screen opens before the widget's first render runs [migrateIfNeeded]).
     */
    suspend fun ensureActiveProfileId(fallback: WidgetConfig): String {
        var resolvedId = ""
        dataStore.edit { prefs ->
            val profiles = parseProfiles(prefs[Keys.PROFILES_JSON] ?: "")
            if (profiles.isEmpty()) {
                val defaultId = UUID.randomUUID().toString()
                prefs[Keys.PROFILES_JSON] = serializeProfiles(
                    listOf(WidgetProfileEntry(defaultId, "Default", fallback))
                )
                prefs[Keys.ACTIVE_PROFILE_ID] = defaultId
                resolvedId = defaultId
            } else {
                val activeId = prefs[Keys.ACTIVE_PROFILE_ID] ?: ""
                resolvedId = if (profiles.any { it.id == activeId }) {
                    activeId
                } else {
                    profiles.first().id.also { prefs[Keys.ACTIVE_PROFILE_ID] = it }
                }
            }
        }
        return resolvedId
    }

    private fun parseProfiles(json: String): List<WidgetProfileEntry> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                WidgetProfileEntry(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    config = ConfigExporter.fromJson(obj.getJSONObject("config"))
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun serializeProfiles(profiles: List<WidgetProfileEntry>): String {
        val arr = JSONArray()
        profiles.forEach { profile ->
            arr.put(JSONObject().apply {
                put("id", profile.id)
                put("name", profile.name)
                put("config", ConfigExporter.toJson(profile.config))
            })
        }
        return arr.toString()
    }
}
