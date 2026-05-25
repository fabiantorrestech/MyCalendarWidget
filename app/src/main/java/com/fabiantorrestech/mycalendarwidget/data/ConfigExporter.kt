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
        put("enabledCalendarIds", JSONArray().apply {
            config.enabledCalendarIds.forEach { put(it) }
        })
        put("keywordFilter", config.keywordFilter)
        put("filterIsRegex", config.filterIsRegex)
        put("defaultClickTarget", config.defaultClickTarget.name)
        put("showQuickAddFab", config.showQuickAddFab)
        put("strictGridMode", config.strictGridMode)
        put("maxTitleLines", config.maxTitleLines)
        put("maxDetailLines", config.maxDetailLines)
        put("showLocation", config.showLocation)
        put("showDescription", config.showDescription)
        put("widgetStyle", config.widgetStyle.name)
        put("calendarLaunchView", config.calendarLaunchView.name)
        put("activeProfile", config.activeProfile.name)
    }

    fun fromJson(json: JSONObject): WidgetConfig {
        val typo = json.optJSONObject("typography")
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
            enabledCalendarIds = ids,
            keywordFilter = json.optString("keywordFilter", ""),
            filterIsRegex = json.optBoolean("filterIsRegex", false),
            defaultClickTarget = json.optString("defaultClickTarget")
                .let { runCatching { DefaultClickTarget.valueOf(it) }.getOrDefault(DefaultClickTarget.SYSTEM_DEFAULT) },
            showQuickAddFab = json.optBoolean("showQuickAddFab", true),
            strictGridMode = json.optBoolean("strictGridMode", false),
            maxTitleLines = json.optInt("maxTitleLines", 2),
            maxDetailLines = json.optInt("maxDetailLines", 1),
            showLocation = json.optBoolean("showLocation", true),
            showDescription = json.optBoolean("showDescription", false),
            widgetStyle = json.optString("widgetStyle")
                .let { runCatching { WidgetStyle.valueOf(it) }.getOrDefault(WidgetStyle.GCAL_LEFT) },
            calendarLaunchView = json.optString("calendarLaunchView")
                .let { runCatching { CalendarLaunchView.valueOf(it) }.getOrDefault(CalendarLaunchView.DEFAULT) },
            activeProfile = json.optString("activeProfile")
                .let { runCatching { AutomationProfile.valueOf(it) }.getOrDefault(AutomationProfile.STANDARD) }
        )
    }

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
