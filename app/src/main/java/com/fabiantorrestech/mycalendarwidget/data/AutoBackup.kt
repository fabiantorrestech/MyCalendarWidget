package com.fabiantorrestech.mycalendarwidget.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.fabiantorrestech.mycalendarwidget.widget.BridgeCalWidget
import kotlinx.coroutines.flow.first

/**
 * Writes one JSON per placed widget into the folder the user picked (a Storage Access
 * Framework tree). Runs when Settings is closed with Done and the option is on.
 *
 * Each file is the widget's *effective* config plus its identity and sync link
 * ([AutoBackupFormat]). A linked widget's repositories already resolve to its source's
 * store, so its file carries the config it inherits and `syncSourceId` names the source;
 * restoring either file through "Import Config" restores that config, and the link is
 * there to re-make by hand. Files are overwritten in place, never duplicated.
 */
object AutoBackup {

    private const val TAG = "AutoBackup"
    private const val MIME = "application/json"

    /** The number of files written, or the first failure. Nothing is written when disabled or unset. */
    suspend fun backupAll(context: Context): Result<Int> = runCatching {
        if (!SettingsUiPrefs.autoBackupEnabled(context)) return@runCatching 0
        val tree = SettingsUiPrefs.autoBackupTree(context) ?: return@runCatching 0

        val manager = GlanceAppWidgetManager(context)
        val ids = manager.getGlanceIds(BridgeCalWidget::class.java).map { manager.getAppWidgetId(it) }
        var written = 0
        for (id in ids) {
            val config = WidgetProfileRepository(context, id).activeConfigFlow.first()
            val name = WidgetNameRepository.getName(context, id)
            val source = WidgetSyncLinkRepository.getSyncSource(context, id)
            val json = AutoBackupFormat.entryJson(config, id, name, source).toString(2)
            writeFile(context, tree, AutoBackupFormat.fileName(id, name), json)
            written++
        }
        Log.i(TAG, "backed up $written widget(s)")
        written
    }

    private fun writeFile(context: Context, tree: Uri, displayName: String, text: String) {
        val resolver = context.contentResolver
        val parentDoc = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val target = existingChild(context, tree, displayName)
            ?: DocumentsContract.createDocument(resolver, parentDoc, MIME, displayName)
            ?: error("Could not create $displayName in the backup folder")
        // "wt" truncates: an overwritten file never keeps a longer stale tail.
        resolver.openOutputStream(target, "wt")?.use { it.write(text.toByteArray()) }
            ?: error("Could not open $displayName for writing")
    }

    private fun existingChild(context: Context, tree: Uri, displayName: String): Uri? {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        context.contentResolver.query(children, projection, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                if (cursor.getString(1) == displayName) {
                    return DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(0))
                }
            }
        }
        return null
    }

    /** A short, human name for the chosen folder ("Documents", "backups"), or null when none is set. */
    fun folderLabel(context: Context): String? {
        val tree = SettingsUiPrefs.autoBackupTree(context) ?: return null
        val docId = runCatching { DocumentsContract.getTreeDocumentId(tree) }.getOrNull() ?: return tree.toString()
        return docId.substringAfterLast('/').substringAfterLast(':').ifBlank { docId }
    }
}
