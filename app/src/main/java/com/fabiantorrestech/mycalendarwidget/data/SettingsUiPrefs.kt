package com.fabiantorrestech.mycalendarwidget.data

import android.content.Context
import android.net.Uri

/**
 * App-wide settings-screen preferences that belong to neither a widget nor a profile:
 * how the settings screen itself behaves. Stored like [WidgetNameRepository], in plain
 * SharedPreferences, because there is nothing to sync or export here.
 */
object SettingsUiPrefs {

    private const val PREFS_NAME = "settings_ui_prefs"
    private const val KEY_STICKY_PREVIEW = "sticky_preview"
    private const val KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled"
    private const val KEY_AUTO_BACKUP_TREE = "auto_backup_tree"

    /** Whether the preview pane stays pinned above the settings while scrolling (default on). */
    fun stickyPreview(context: Context): Boolean =
        prefs(context).getBoolean(KEY_STICKY_PREVIEW, true)

    fun setStickyPreview(context: Context, sticky: Boolean) {
        prefs(context).edit().putBoolean(KEY_STICKY_PREVIEW, sticky).apply()
    }

    /** Whether tapping Done writes every placed widget's backup to [autoBackupTree] (default off). */
    fun autoBackupEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_BACKUP_ENABLED, false)

    fun setAutoBackupEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO_BACKUP_ENABLED, enabled).apply()
    }

    /** The Storage Access Framework tree the backups are written into, or null when none is chosen. */
    fun autoBackupTree(context: Context): Uri? =
        prefs(context).getString(KEY_AUTO_BACKUP_TREE, null)?.let(Uri::parse)

    fun setAutoBackupTree(context: Context, tree: Uri?) {
        prefs(context).edit().putString(KEY_AUTO_BACKUP_TREE, tree?.toString()).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
