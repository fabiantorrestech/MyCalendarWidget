package com.fabiantorrestech.mycalendarwidget.data

import android.content.Context
import android.provider.Settings
import android.util.Log

/**
 * Turns the stored [DisplayMode] into the one a render uses. [resolve] is pure so the
 * Auto rule is unit-tested; [systemGrayscaleOn] is the one Android read behind it.
 */
object DisplayModes {

    private const val TAG = "DisplayModes"

    /** The secure-settings keys the system grayscale filter (and Grayscaler) writes. */
    const val DALTONIZER_ENABLED = "accessibility_display_daltonizer_enabled"
    const val DALTONIZER = "accessibility_display_daltonizer"

    /** `DALTONIZER`'s value for monochromacy, i.e. grayscale. */
    private const val MONOCHROMACY = 0

    /** Never returns [DisplayMode.AUTO]: Auto is Grayscale while the phone is gray, else Color. */
    fun resolve(mode: DisplayMode, systemGrayscale: Boolean): DisplayMode = when (mode) {
        DisplayMode.AUTO -> if (systemGrayscale) DisplayMode.GRAYSCALE else DisplayMode.COLOR
        else -> mode
    }

    /**
     * True while the phone's grayscale colour correction is on. A key this app may not
     * read (targetSdk 31+ restricts unlisted secure settings) counts as off, so Auto
     * falls back to Color rather than crashing the widget.
     */
    fun systemGrayscaleOn(context: Context): Boolean = try {
        val resolver = context.contentResolver
        (Settings.Secure.getInt(resolver, DALTONIZER_ENABLED, 0) == 1 &&
            Settings.Secure.getInt(resolver, DALTONIZER, -1) == MONOCHROMACY)
            .also { Log.d(TAG, "System grayscale is ${if (it) "on" else "off"}") }
    } catch (e: SecurityException) {
        Log.w(TAG, "System grayscale setting is not readable; Auto stays on Color", e)
        false
    }

    /** The mode [config] renders in right now. Reads the system switch only for Auto. */
    fun effective(context: Context, config: WidgetConfig): DisplayMode =
        if (config.displayMode == DisplayMode.AUTO) {
            resolve(DisplayMode.AUTO, systemGrayscaleOn(context))
        } else {
            config.displayMode
        }
}
