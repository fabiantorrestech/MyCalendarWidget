package com.fabiantorrestech.mycalendarwidget.data

import android.content.Context

/**
 * Stores the user-facing widget name as a per-widget identity, keyed by the **real**
 * appWidgetId (NOT the resolved sync source). This deliberately differs from config/profile
 * storage so that two widgets — even ones synced to each other — keep independent, unique names.
 */
object WidgetNameRepository {

    private const val PREFS_NAME = "widget_names"
    private const val PREFIX = "name_"

    fun getName(context: Context, widgetId: Int): String =
        prefs(context).getString("$PREFIX$widgetId", "") ?: ""

    fun setName(context: Context, widgetId: Int, name: String) {
        prefs(context).edit().putString("$PREFIX$widgetId", name.trim()).apply()
    }

    /** Map of appWidgetId -> name for every widget that has a (non-blank) name set. */
    fun getAllNames(context: Context): Map<Int, String> =
        prefs(context).all.entries
            .filter { it.key.startsWith(PREFIX) }
            .mapNotNull { entry ->
                val id = entry.key.removePrefix(PREFIX).toIntOrNull() ?: return@mapNotNull null
                val value = entry.value as? String ?: return@mapNotNull null
                if (value.isBlank()) null else id to value
            }
            .toMap()

    /** True if [name] (case-insensitive, trimmed) is already used by a widget other than [excludingId]. */
    fun isNameTaken(context: Context, name: String, excludingId: Int): Boolean {
        val candidate = name.trim()
        if (candidate.isBlank()) return false
        return getAllNames(context).any { (id, existing) ->
            id != excludingId && existing.equals(candidate, ignoreCase = true)
        }
    }

    fun clear(context: Context, widgetId: Int) {
        prefs(context).edit().remove("$PREFIX$widgetId").apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
