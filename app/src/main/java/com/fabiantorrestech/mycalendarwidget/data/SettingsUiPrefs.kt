package com.fabiantorrestech.mycalendarwidget.data

import android.content.Context

/**
 * App-wide settings-screen preferences that belong to neither a widget nor a profile:
 * how the settings screen itself behaves. Stored like [WidgetNameRepository], in plain
 * SharedPreferences, because there is nothing to sync or export here.
 */
object SettingsUiPrefs {

    private const val PREFS_NAME = "settings_ui_prefs"
    private const val KEY_STICKY_PREVIEW = "sticky_preview"

    /** Whether the preview pane stays pinned above the settings while scrolling (default on). */
    fun stickyPreview(context: Context): Boolean =
        prefs(context).getBoolean(KEY_STICKY_PREVIEW, true)

    fun setStickyPreview(context: Context, sticky: Boolean) {
        prefs(context).edit().putBoolean(KEY_STICKY_PREVIEW, sticky).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
