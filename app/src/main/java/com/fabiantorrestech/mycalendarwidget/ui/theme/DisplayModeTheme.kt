package com.fabiantorrestech.mycalendarwidget.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.fabiantorrestech.mycalendarwidget.data.DisplayMode
import com.fabiantorrestech.mycalendarwidget.data.DisplayModes
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfig
import com.fabiantorrestech.mycalendarwidget.data.WidgetStyle

/**
 * The display mode the settings previews draw [config] in: the same resolution the
 * widget makes, re-read when the setting changes. Only the density style has one.
 */
@Composable
fun rememberDisplayMode(config: WidgetConfig): DisplayMode {
    val context = LocalContext.current
    return remember(config.widgetStyle, config.displayMode) {
        if (config.widgetStyle == WidgetStyle.DENSITY) DisplayModes.effective(context, config) else DisplayMode.COLOR
    }
}

/** Re-themes [content] (the previews) with [config]'s display-mode scheme. */
@Composable
fun DisplayModeTheme(config: WidgetConfig, content: @Composable () -> Unit) {
    val display = rememberDisplayMode(config)
    if (display == DisplayMode.COLOR) {
        content()
        return
    }
    MaterialTheme(
        colorScheme = DisplaySchemes.scheme(display, isSystemInDarkTheme(), MaterialTheme.colorScheme),
        content = content
    )
}
