package com.fabiantorrestech.mycalendarwidget.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File

class WidgetConfigRepository(private val context: Context, appWidgetId: Int) {

    private val dataStore: DataStore<Preferences> = getOrCreate(context, appWidgetId)

    companion object {
        private val lock = Any()
        private val stores = mutableMapOf<Int, DataStore<Preferences>>()

        private fun getOrCreate(context: Context, appWidgetId: Int): DataStore<Preferences> =
            synchronized(lock) {
                stores.getOrPut(appWidgetId) {
                    PreferenceDataStoreFactory.create {
                        File(
                            context.applicationContext.filesDir,
                            "datastore/widget_config_${appWidgetId}.preferences_pb"
                        )
                    }
                }
            }
    }

    private object Keys {
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val HEADER_SCALE = floatPreferencesKey("header_scale")
        val SUBHEADER_SCALE = floatPreferencesKey("subheader_scale")
        val DATE_HEADER_SCALE = floatPreferencesKey("date_header_scale")
        val EVENT_TIME_SCALE = floatPreferencesKey("event_time_scale")
        val EVENT_NAME_SCALE = floatPreferencesKey("event_name_scale")
        val DETAIL_SCALE = floatPreferencesKey("detail_scale")
        val FONT_MODE = stringPreferencesKey("font_mode")
        val FONT_UNIVERSAL = stringPreferencesKey("font_universal")
        val FONT_MONTH_HEADER = stringPreferencesKey("font_month_header")
        val FONT_WEEKDAY_HEADER = stringPreferencesKey("font_weekday_header")
        val FONT_DATE_HEADER = stringPreferencesKey("font_date_header")
        val FONT_EVENT_TIME = stringPreferencesKey("font_event_time")
        val FONT_EVENT_NAME = stringPreferencesKey("font_event_name")
        val FONT_DETAIL = stringPreferencesKey("font_detail")
        val ENABLED_CAL_IDS = stringPreferencesKey("enabled_cal_ids")
        val KEYWORD_FILTER = stringPreferencesKey("keyword_filter")
        val FILTER_IS_REGEX = booleanPreferencesKey("filter_is_regex")
        val DEFAULT_CLICK = stringPreferencesKey("default_click")
        val SHOW_QUICK_ADD = booleanPreferencesKey("show_quick_add")
        val SHOW_REFRESH_BUTTON = booleanPreferencesKey("show_refresh_button")
        val STRICT_GRID = booleanPreferencesKey("strict_grid")
        val MAX_TITLE_LINES = intPreferencesKey("max_title_lines")
        val MAX_DETAIL_LINES = intPreferencesKey("max_detail_lines")
        val SHOW_LOCATION = booleanPreferencesKey("show_location")
        val SHOW_DESCRIPTION = booleanPreferencesKey("show_description")
        val DAYS_AHEAD = intPreferencesKey("days_ahead")
        val SHOW_EMPTY_DAYS = booleanPreferencesKey("show_empty_days")
        val ALWAYS_SHOW_TODAY = booleanPreferencesKey("always_show_today")
        val WIDGET_STYLE = stringPreferencesKey("widget_style")
        val ACTIVE_PROFILE = stringPreferencesKey("active_profile")
        val HEADER_NAV_ENABLED = booleanPreferencesKey("header_nav_enabled")
        val HEADER_NAV_STYLE = stringPreferencesKey("header_nav_style")
        val MONTH_OFFSET = intPreferencesKey("month_offset")
        val SHOW_MONTH_IN_HEADER = booleanPreferencesKey("show_month_in_header")
        val CALENDAR_LAUNCH_VIEW = stringPreferencesKey("calendar_launch_view")
        val SYNC_INTERVAL = intPreferencesKey("sync_interval")
        val REFRESH_NONCE = intPreferencesKey("refresh_nonce")
    }

    val configFlow: Flow<WidgetConfig> = dataStore.data.map { prefs ->
        WidgetConfig(
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true,
            typographyScale = TypographyScale(
                headerScale = prefs[Keys.HEADER_SCALE] ?: 1.0f,
                subheaderScale = prefs[Keys.SUBHEADER_SCALE] ?: 1.0f,
                dateHeaderScale = prefs[Keys.DATE_HEADER_SCALE] ?: 1.0f,
                eventTimeScale = prefs[Keys.EVENT_TIME_SCALE] ?: 1.0f,
                eventNameScale = prefs[Keys.EVENT_NAME_SCALE] ?: 1.0f,
                detailScale = prefs[Keys.DETAIL_SCALE] ?: 1.0f
            ),
            fontConfig = FontConfig(
                mode = prefs[Keys.FONT_MODE]?.let { runCatching { FontMode.valueOf(it) }.getOrNull() } ?: FontMode.DEFAULT,
                universalFont = prefs[Keys.FONT_UNIVERSAL]?.let { runCatching { WidgetFont.valueOf(it) }.getOrNull() } ?: WidgetFont.DEFAULT,
                monthHeaderFont = prefs[Keys.FONT_MONTH_HEADER]?.let { runCatching { WidgetFont.valueOf(it) }.getOrNull() } ?: WidgetFont.DEFAULT,
                weekdayHeaderFont = prefs[Keys.FONT_WEEKDAY_HEADER]?.let { runCatching { WidgetFont.valueOf(it) }.getOrNull() } ?: WidgetFont.DEFAULT,
                dateHeaderFont = prefs[Keys.FONT_DATE_HEADER]?.let { runCatching { WidgetFont.valueOf(it) }.getOrNull() } ?: WidgetFont.DEFAULT,
                eventTimeFont = prefs[Keys.FONT_EVENT_TIME]?.let { runCatching { WidgetFont.valueOf(it) }.getOrNull() } ?: WidgetFont.DEFAULT,
                eventNameFont = prefs[Keys.FONT_EVENT_NAME]?.let { runCatching { WidgetFont.valueOf(it) }.getOrNull() } ?: WidgetFont.DEFAULT,
                detailFont = prefs[Keys.FONT_DETAIL]?.let { runCatching { WidgetFont.valueOf(it) }.getOrNull() } ?: WidgetFont.DEFAULT
            ),
            enabledCalendarIds = prefs[Keys.ENABLED_CAL_IDS]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?.mapNotNull { it.toLongOrNull() }
                ?.toSet() ?: emptySet(),
            keywordFilter = prefs[Keys.KEYWORD_FILTER] ?: "",
            filterIsRegex = prefs[Keys.FILTER_IS_REGEX] ?: false,
            defaultClickTarget = prefs[Keys.DEFAULT_CLICK]
                ?.let { runCatching { DefaultClickTarget.valueOf(it) }.getOrNull() }
                ?: DefaultClickTarget.SYSTEM_DEFAULT,
            showQuickAddFab = prefs[Keys.SHOW_QUICK_ADD] ?: true,
            showRefreshButton = prefs[Keys.SHOW_REFRESH_BUTTON] ?: true,
            strictGridMode = prefs[Keys.STRICT_GRID] ?: false,
            maxTitleLines = prefs[Keys.MAX_TITLE_LINES] ?: 2,
            maxDetailLines = prefs[Keys.MAX_DETAIL_LINES] ?: 1,
            showLocation = prefs[Keys.SHOW_LOCATION] ?: true,
            showDescription = prefs[Keys.SHOW_DESCRIPTION] ?: false,
            daysAheadToLoad = prefs[Keys.DAYS_AHEAD] ?: 30,
            showEmptyDays = prefs[Keys.SHOW_EMPTY_DAYS] ?: false,
            alwaysShowToday = prefs[Keys.ALWAYS_SHOW_TODAY] ?: false,
            widgetStyle = prefs[Keys.WIDGET_STYLE]
                ?.let { runCatching { WidgetStyle.valueOf(it) }.getOrNull() }
                ?: WidgetStyle.GCAL_LEFT,
            calendarLaunchView = prefs[Keys.CALENDAR_LAUNCH_VIEW]
                ?.let { runCatching { CalendarLaunchView.valueOf(it) }.getOrNull() }
                ?: CalendarLaunchView.DEFAULT,
            activeProfile = prefs[Keys.ACTIVE_PROFILE]
                ?.let { runCatching { AutomationProfile.valueOf(it) }.getOrNull() }
                ?: AutomationProfile.STANDARD,
            headerNavEnabled = prefs[Keys.HEADER_NAV_ENABLED] ?: false,
            headerNavStyle = prefs[Keys.HEADER_NAV_STYLE]
                ?.let { runCatching { HeaderNavStyle.valueOf(it) }.getOrNull() }
                ?: HeaderNavStyle.ARROWS,
            monthOffset = prefs[Keys.MONTH_OFFSET] ?: 0,
            showMonthInHeader = prefs[Keys.SHOW_MONTH_IN_HEADER] ?: true,
            syncIntervalMinutes = prefs[Keys.SYNC_INTERVAL] ?: 0,
            refreshNonce = prefs[Keys.REFRESH_NONCE] ?: 0
        )
    }

    suspend fun updateConfig(config: WidgetConfig) {
        dataStore.edit { prefs ->
            prefs[Keys.DYNAMIC_COLOR] = config.dynamicColor
            prefs[Keys.HEADER_SCALE] = config.typographyScale.headerScale
            prefs[Keys.SUBHEADER_SCALE] = config.typographyScale.subheaderScale
            prefs[Keys.DATE_HEADER_SCALE] = config.typographyScale.dateHeaderScale
            prefs[Keys.EVENT_TIME_SCALE] = config.typographyScale.eventTimeScale
            prefs[Keys.EVENT_NAME_SCALE] = config.typographyScale.eventNameScale
            prefs[Keys.DETAIL_SCALE] = config.typographyScale.detailScale
            prefs[Keys.FONT_MODE] = config.fontConfig.mode.name
            prefs[Keys.FONT_UNIVERSAL] = config.fontConfig.universalFont.name
            prefs[Keys.FONT_MONTH_HEADER] = config.fontConfig.monthHeaderFont.name
            prefs[Keys.FONT_WEEKDAY_HEADER] = config.fontConfig.weekdayHeaderFont.name
            prefs[Keys.FONT_DATE_HEADER] = config.fontConfig.dateHeaderFont.name
            prefs[Keys.FONT_EVENT_TIME] = config.fontConfig.eventTimeFont.name
            prefs[Keys.FONT_EVENT_NAME] = config.fontConfig.eventNameFont.name
            prefs[Keys.FONT_DETAIL] = config.fontConfig.detailFont.name
            prefs[Keys.ENABLED_CAL_IDS] = config.enabledCalendarIds.joinToString(",")
            prefs[Keys.KEYWORD_FILTER] = config.keywordFilter
            prefs[Keys.FILTER_IS_REGEX] = config.filterIsRegex
            prefs[Keys.DEFAULT_CLICK] = config.defaultClickTarget.name
            prefs[Keys.SHOW_QUICK_ADD] = config.showQuickAddFab
            prefs[Keys.SHOW_REFRESH_BUTTON] = config.showRefreshButton
            prefs[Keys.STRICT_GRID] = config.strictGridMode
            prefs[Keys.MAX_TITLE_LINES] = config.maxTitleLines
            prefs[Keys.MAX_DETAIL_LINES] = config.maxDetailLines
            prefs[Keys.SHOW_LOCATION] = config.showLocation
            prefs[Keys.SHOW_DESCRIPTION] = config.showDescription
            prefs[Keys.DAYS_AHEAD] = config.daysAheadToLoad
            prefs[Keys.SHOW_EMPTY_DAYS] = config.showEmptyDays
            prefs[Keys.ALWAYS_SHOW_TODAY] = config.alwaysShowToday
            prefs[Keys.WIDGET_STYLE] = config.widgetStyle.name
            prefs[Keys.ACTIVE_PROFILE] = config.activeProfile.name
            prefs[Keys.HEADER_NAV_ENABLED] = config.headerNavEnabled
            prefs[Keys.HEADER_NAV_STYLE] = config.headerNavStyle.name
            prefs[Keys.MONTH_OFFSET] = config.monthOffset
            prefs[Keys.SHOW_MONTH_IN_HEADER] = config.showMonthInHeader
            prefs[Keys.CALENDAR_LAUNCH_VIEW] = config.calendarLaunchView.name
            prefs[Keys.SYNC_INTERVAL] = config.syncIntervalMinutes
            prefs[Keys.REFRESH_NONCE] = config.refreshNonce
        }
    }

    suspend fun applyProfile(profile: AutomationProfile) {
        val current = configFlow.first()
        updateConfig(profile.toWidgetConfig(current))
    }
}
