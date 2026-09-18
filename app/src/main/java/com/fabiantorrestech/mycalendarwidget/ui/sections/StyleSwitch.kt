package com.fabiantorrestech.mycalendarwidget.ui.sections

import com.fabiantorrestech.mycalendarwidget.data.DensityStripMode
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfig
import com.fabiantorrestech.mycalendarwidget.data.WidgetStyle

/** The one place the style picker changes a config. */
object StyleSwitch {
    /**
     * [config] with [style] selected. Entering Density from another style also picks
     * the Tonal strip (the mode the user wants a fresh Density widget to start on);
     * re-selecting Density or leaving it leaves every other field alone, so a strip
     * mode chosen on purpose survives a round trip through the agenda styles.
     */
    fun applyStyle(config: WidgetConfig, style: WidgetStyle): WidgetConfig {
        val entering = style == WidgetStyle.DENSITY && config.widgetStyle != WidgetStyle.DENSITY
        return if (entering) {
            config.copy(widgetStyle = style, densityStripMode = DensityStripMode.TONAL)
        } else {
            config.copy(widgetStyle = style)
        }
    }
}
