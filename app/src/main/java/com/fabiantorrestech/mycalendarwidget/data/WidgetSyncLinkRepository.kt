package com.fabiantorrestech.mycalendarwidget.data

import android.content.Context

object WidgetSyncLinkRepository {

    private const val PREFS_NAME = "widget_sync_links"
    private const val PREFIX = "sync_source_"

    fun getSyncSource(context: Context, widgetId: Int): Int? {
        val raw = prefs(context).getInt("$PREFIX$widgetId", -1)
        return if (raw == -1) null else raw
    }

    /** Resolves transitive sync chains (e.g. C→B→A returns A). Cycle-safe. */
    fun resolveSource(context: Context, widgetId: Int): Int {
        val visited = mutableSetOf<Int>()
        var current = widgetId
        while (true) {
            val source = getSyncSource(context, current) ?: return current
            if (!visited.add(source)) return current // cycle guard
            current = source
        }
    }

    fun setSyncSource(context: Context, widgetId: Int, sourceId: Int?) {
        val edit = prefs(context).edit()
        if (sourceId == null) edit.remove("$PREFIX$widgetId") else edit.putInt("$PREFIX$widgetId", sourceId)
        edit.apply()
    }

    /** Clears the link for the deleted widget AND any widgets pointing to it. */
    fun clearAllLinksFor(context: Context, deletedWidgetId: Int) {
        val p = prefs(context)
        val edit = p.edit()
        edit.remove("$PREFIX$deletedWidgetId")
        p.all.entries
            .filter { it.key.startsWith(PREFIX) && it.value == deletedWidgetId }
            .forEach { edit.remove(it.key) }
        edit.apply()
    }

    fun getDependents(context: Context, sourceId: Int): List<Int> =
        prefs(context).all.entries
            .filter { it.key.startsWith(PREFIX) && it.value == sourceId }
            .mapNotNull { it.key.removePrefix(PREFIX).toIntOrNull() }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
