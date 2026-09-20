package com.fabiantorrestech.mycalendarwidget.data

import org.json.JSONObject

/**
 * What the auto-backup writes for one placed widget: the plain [ConfigExporter] JSON of
 * its effective config plus the widget's identity and sync link. Kept free of Android so
 * the naming and the extra keys are unit-tested; [ConfigExporter.fromJson] ignores the
 * extra keys, so any of these files still imports through "Import Config".
 */
object AutoBackupFormat {

    const val BACKUP_VERSION = 1

    private const val PREFIX = "bridgecal_widget_"
    private const val MAX_SLUG = 40

    /** `bridgecal_widget_<id>[_<slug>].json`; the slug is the widget name made file-safe. */
    fun fileName(widgetId: Int, widgetName: String): String {
        val slug = widgetName.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .take(MAX_SLUG)
            .trim('-')
        return if (slug.isEmpty()) "$PREFIX$widgetId.json" else "$PREFIX${widgetId}_$slug.json"
    }

    /** The config's export JSON with `widgetId`, `widgetName`, `syncSourceId` and `backupVersion` added. */
    fun entryJson(config: WidgetConfig, widgetId: Int, widgetName: String, syncSourceId: Int?): JSONObject =
        ConfigExporter.toJson(config).apply {
            put("backupVersion", BACKUP_VERSION)
            put("widgetId", widgetId)
            put("widgetName", widgetName)
            put("syncSourceId", syncSourceId ?: JSONObject.NULL)
        }
}
