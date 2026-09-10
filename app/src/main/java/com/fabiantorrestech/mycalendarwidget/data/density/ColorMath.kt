package com.fabiantorrestech.mycalendarwidget.widget.density

import kotlin.math.roundToInt

/**
 * Colour arithmetic on plain ARGB ints. Pure Kotlin: no Android, no Glance, no Compose,
 * so the widget, the settings preview and the unit tests all share one implementation.
 *
 * The widget never composites alpha (G5) — a "12% text on the ground" tint is produced
 * by [lerp]ing to an opaque colour up front instead of drawing a translucent layer.
 */
object ColorMath {

    /**
     * Blends [a] towards [b] per ARGB channel. [t] is clamped to `0f..1f` and the result
     * is always fully opaque, whatever the alpha of the inputs.
     */
    fun lerp(a: Int, b: Int, t: Float): Int {
        val f = t.coerceIn(0f, 1f)
        val r = channel(a, 16) + (channel(b, 16) - channel(a, 16)) * f
        val g = channel(a, 8) + (channel(b, 8) - channel(a, 8)) * f
        val bl = channel(a, 0) + (channel(b, 0) - channel(a, 0)) * f
        return (0xFF shl 24) or
            (r.roundToInt() shl 16) or
            (g.roundToInt() shl 8) or
            bl.roundToInt()
    }

    /** Perceived brightness on `0f..1f` (Rec. 601 weights). Alpha is ignored. */
    fun luminance(argb: Int): Float {
        val r = channel(argb, 16) / 255f
        val g = channel(argb, 8) / 255f
        val b = channel(argb, 0) / 255f
        return 0.299f * r + 0.587f * g + 0.114f * b
    }

    /** True when white text reads better on [argb] than black does. */
    fun isDark(argb: Int): Boolean = luminance(argb) < 0.5f

    private fun channel(argb: Int, shift: Int): Float = ((argb shr shift) and 0xFF).toFloat()
}
