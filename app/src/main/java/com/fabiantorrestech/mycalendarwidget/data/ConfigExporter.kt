package com.fabiantorrestech.mycalendarwidget.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

object ConfigExporter {

    fun toJson(config: WidgetConfig): JSONObject = JSONObject().apply {
        put("configVersion", config.configVersion)
        put("widgetName", config.widgetName)
        put("dynamicColor", config.dynamicColor)
        put("typography", JSONObject().apply {
            put("header", config.typographyScale.headerScale)
            put("subheader", config.typographyScale.subheaderScale)
            put("dateHeader", config.typographyScale.dateHeaderScale)
            put("eventTime", config.typographyScale.eventTimeScale)
            put("eventName", config.typographyScale.eventNameScale)
            put("detail", config.typographyScale.detailScale)
        })
        put("fontConfig", fontConfigToJson(config.fontConfig))
        put("enabledCalendarIds", JSONArray().apply {
            config.enabledCalendarIds.forEach { put(it) }
        })
        put("keywordFilter", config.keywordFilter)
        put("filterIsRegex", config.filterIsRegex)
        put("defaultClickTarget", config.defaultClickTarget.name)
        put("showQuickAddFab", config.showQuickAddFab)
        put("showRefreshButton", config.showRefreshButton)
        put("strictGridMode", config.strictGridMode)
        put("maxTitleLines", config.maxTitleLines)
        put("maxDetailLines", config.maxDetailLines)
        put("showLocation", config.showLocation)
        put("showDescription", config.showDescription)
        put("daysAheadToLoad", config.daysAheadToLoad)
        put("showEmptyDays", config.showEmptyDays)
        put("alwaysShowToday", config.alwaysShowToday)
        put("showSpanningEventsEachDay", config.showSpanningEventsEachDay)
        put("widgetStyle", config.widgetStyle.name)
        put("calendarLaunchView", config.calendarLaunchView.name)
        put("activeProfile", config.activeProfile.name)
        put("headerNavEnabled", config.headerNavEnabled)
        put("headerNavStyle", config.headerNavStyle.name)
        put("monthOffset", config.monthOffset)
        put("showMonthInHeader", config.showMonthInHeader)
        put("syncIntervalMinutes", config.syncIntervalMinutes)
    }

    fun fromJson(json: JSONObject): WidgetConfig {
        val typo = json.optJSONObject("typography")
        val fontConfig = json.optJSONObject("fontConfig")
        val idsArray = json.optJSONArray("enabledCalendarIds")
        val ids = mutableSetOf<Long>()
        if (idsArray != null) {
            for (i in 0 until idsArray.length()) ids.add(idsArray.getLong(i))
        }
        return WidgetConfig(
            configVersion = json.optInt("configVersion", 1),
            widgetName = json.optString("widgetName", ""),
            dynamicColor = json.optBoolean("dynamicColor", true),
            typographyScale = TypographyScale(
                headerScale = typo?.optDouble("header", 1.0)?.toFloat() ?: 1.0f,
                subheaderScale = typo?.optDouble("subheader", 1.0)?.toFloat() ?: 1.0f,
                dateHeaderScale = typo?.optDouble("dateHeader", 1.0)?.toFloat() ?: 1.0f,
                eventTimeScale = typo?.optDouble("eventTime", 1.0)?.toFloat() ?: 1.0f,
                eventNameScale = typo?.optDouble("eventName", 1.0)?.toFloat() ?: 1.0f,
                detailScale = typo?.optDouble("detail", 1.0)?.toFloat() ?: 1.0f
            ),
            fontConfig = fontConfigFromJson(fontConfig),
            enabledCalendarIds = ids,
            keywordFilter = json.optString("keywordFilter", ""),
            filterIsRegex = json.optBoolean("filterIsRegex", false),
            defaultClickTarget = json.optString("defaultClickTarget")
                .let { runCatching { DefaultClickTarget.valueOf(it) }.getOrDefault(DefaultClickTarget.SYSTEM_DEFAULT) },
            showQuickAddFab = json.optBoolean("showQuickAddFab", true),
            showRefreshButton = json.optBoolean("showRefreshButton", true),
            strictGridMode = json.optBoolean("strictGridMode", false),
            maxTitleLines = json.optInt("maxTitleLines", 2),
            maxDetailLines = json.optInt("maxDetailLines", 1),
            showLocation = json.optBoolean("showLocation", true),
            showDescription = json.optBoolean("showDescription", false),
            daysAheadToLoad = json.optInt("daysAheadToLoad", 30),
            showEmptyDays = json.optBoolean("showEmptyDays", false),
            alwaysShowToday = json.optBoolean("alwaysShowToday", false),
            showSpanningEventsEachDay = json.optBoolean("showSpanningEventsEachDay", false),
            widgetStyle = json.optString("widgetStyle")
                .let { runCatching { WidgetStyle.valueOf(it) }.getOrDefault(WidgetStyle.GCAL_LEFT) },
            calendarLaunchView = json.optString("calendarLaunchView")
                .let { runCatching { CalendarLaunchView.valueOf(it) }.getOrDefault(CalendarLaunchView.DEFAULT) },
            activeProfile = json.optString("activeProfile")
                .let { runCatching { AutomationProfile.valueOf(it) }.getOrDefault(AutomationProfile.STANDARD) },
            headerNavEnabled = json.optBoolean("headerNavEnabled", false),
            headerNavStyle = json.optString("headerNavStyle")
                .let { runCatching { HeaderNavStyle.valueOf(it) }.getOrDefault(HeaderNavStyle.ARROWS) },
            monthOffset = json.optInt("monthOffset", 0),
            showMonthInHeader = json.optBoolean("showMonthInHeader", true),
            syncIntervalMinutes = json.optInt("syncIntervalMinutes", 0)
        )
    }

    private fun fontConfigToJson(fontConfig: FontConfig): JSONObject = JSONObject().apply {
        put("mode", fontConfig.mode.name)
        put("universalFont", fontConfig.universalFont.name)
        put("monthHeaderFont", fontConfig.monthHeaderFont.name)
        put("weekdayHeaderFont", fontConfig.weekdayHeaderFont.name)
        put("dateHeaderFont", fontConfig.dateHeaderFont.name)
        put("eventTimeFont", fontConfig.eventTimeFont.name)
        put("eventNameFont", fontConfig.eventNameFont.name)
        put("detailFont", fontConfig.detailFont.name)
    }

    private fun fontConfigFromJson(json: JSONObject?): FontConfig = FontConfig(
        mode = json?.optString("mode")
            ?.let { runCatching { FontMode.valueOf(it) }.getOrNull() }
            ?: FontMode.DEFAULT,
        universalFont = json?.optString("universalFont")
            ?.let { runCatching { WidgetFont.valueOf(it) }.getOrNull() }
            ?: WidgetFont.DEFAULT,
        monthHeaderFont = json?.optString("monthHeaderFont")
            ?.let { runCatching { WidgetFont.valueOf(it) }.getOrNull() }
            ?: WidgetFont.DEFAULT,
        weekdayHeaderFont = json?.optString("weekdayHeaderFont")
            ?.let { runCatching { WidgetFont.valueOf(it) }.getOrNull() }
            ?: WidgetFont.DEFAULT,
        dateHeaderFont = json?.optString("dateHeaderFont")
            ?.let { runCatching { WidgetFont.valueOf(it) }.getOrNull() }
            ?: WidgetFont.DEFAULT,
        eventTimeFont = json?.optString("eventTimeFont")
            ?.let { runCatching { WidgetFont.valueOf(it) }.getOrNull() }
            ?: WidgetFont.DEFAULT,
        eventNameFont = json?.optString("eventNameFont")
            ?.let { runCatching { WidgetFont.valueOf(it) }.getOrNull() }
            ?: WidgetFont.DEFAULT,
        detailFont = json?.optString("detailFont")
            ?.let { runCatching { WidgetFont.valueOf(it) }.getOrNull() }
            ?: WidgetFont.DEFAULT
    )

    suspend fun exportToUri(context: Context, uri: Uri, config: WidgetConfig): Result<Uri> {
        return runCatching {
            val jsonString = toJson(config).toString(2)
            context.contentResolver.openOutputStream(uri)?.use { it.write(jsonString.toByteArray()) }
                ?: error("Could not open output stream")
            uri
        }
    }

    fun importFromUri(context: Context, uri: Uri): Result<WidgetConfig> {
        return runCatching {
            val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                ?: error("Could not read file")
            fromJson(JSONObject(text))
        }
    }
}
