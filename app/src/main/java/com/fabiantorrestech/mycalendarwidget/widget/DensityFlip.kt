package com.fabiantorrestech.mycalendarwidget.widget

import androidx.annotation.LayoutRes
import com.fabiantorrestech.mycalendarwidget.R
import com.fabiantorrestech.mycalendarwidget.data.ConfigExporter
import com.fabiantorrestech.mycalendarwidget.data.DisplayMode
import com.fabiantorrestech.mycalendarwidget.data.WidgetConfig
import com.fabiantorrestech.mycalendarwidget.data.WidgetStyle

/**
 * "Flip to the agenda on a timer" (doc/adr/0004): the density widget is composed as a
 * `ViewFlipper` holding the strip and the agenda, and the launcher swaps between them
 * every [WidgetConfig.densityTimedFlipSeconds] while the widget is on screen. The app
 * runs nothing for it, so it costs no battery; the flipper pauses whenever the widget
 * is out of sight.
 */
object DensityFlip {

    /** The flipper replaces the plain strip only while the peek itself is closed. */
    fun flips(config: WidgetConfig, peekOpen: Boolean): Boolean =
        config.widgetStyle == WidgetStyle.DENSITY && config.densityTimedFlip && !peekOpen

    fun intervalMillis(config: WidgetConfig): Int =
        config.densityTimedFlipSeconds.coerceIn(ConfigExporter.TIMED_FLIP_SECONDS_RANGE) * 1000

    /** E-ink swaps pages without the fade: a fade is a run of in-between frames e-paper smears. */
    @LayoutRes
    fun layoutFor(display: DisplayMode): Int =
        if (display == DisplayMode.EINK) R.layout.density_flipper_still else R.layout.density_flipper

    /**
     * Whether a render reads event text. Density stays content-free (G1) while it shows
     * only the strip; the open peek and the flipper's agenda page are the exceptions.
     */
    fun loadsEvents(config: WidgetConfig, peekOpen: Boolean): Boolean =
        config.widgetStyle != WidgetStyle.DENSITY || peekOpen || config.densityTimedFlip
}
