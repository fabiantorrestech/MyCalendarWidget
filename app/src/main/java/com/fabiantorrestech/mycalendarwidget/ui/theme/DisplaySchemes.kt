package com.fabiantorrestech.mycalendarwidget.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.fabiantorrestech.mycalendarwidget.data.DisplayMode
import com.fabiantorrestech.mycalendarwidget.data.density.ColorMath

/**
 * The colour scheme a [DisplayMode] draws with, built from the scheme Color mode would
 * use. The widget's `GlanceTheme`, the settings preview's `MaterialTheme` and the
 * notification's stand-in for the shade all go through here, so the three agree.
 */
object DisplaySchemes {

    /** E-ink's one in-between gray, for buttons and pills: light by day, dark by night. */
    private val EINK_CONTAINER_DAY = Color(0xFFDDDDDD)
    private val EINK_CONTAINER_NIGHT = Color(0xFF333333)

    fun scheme(display: DisplayMode, isDark: Boolean, base: ColorScheme): ColorScheme = when (display) {
        DisplayMode.GRAYSCALE -> grayscale(base)
        DisplayMode.EINK -> eink(isDark, base)
        else -> base
    }

    /** Every role as the gray of the same brightness, so the layout's contrast is kept. */
    private fun grayscale(base: ColorScheme): ColorScheme {
        fun g(c: Color) = Color(ColorMath.toGray(c.toArgb()))
        return base.copy(
            primary = g(base.primary),
            onPrimary = g(base.onPrimary),
            primaryContainer = g(base.primaryContainer),
            onPrimaryContainer = g(base.onPrimaryContainer),
            inversePrimary = g(base.inversePrimary),
            secondary = g(base.secondary),
            onSecondary = g(base.onSecondary),
            secondaryContainer = g(base.secondaryContainer),
            onSecondaryContainer = g(base.onSecondaryContainer),
            tertiary = g(base.tertiary),
            onTertiary = g(base.onTertiary),
            tertiaryContainer = g(base.tertiaryContainer),
            onTertiaryContainer = g(base.onTertiaryContainer),
            background = g(base.background),
            onBackground = g(base.onBackground),
            surface = g(base.surface),
            onSurface = g(base.onSurface),
            surfaceVariant = g(base.surfaceVariant),
            onSurfaceVariant = g(base.onSurfaceVariant),
            surfaceTint = g(base.surfaceTint),
            inverseSurface = g(base.inverseSurface),
            inverseOnSurface = g(base.inverseOnSurface),
            error = g(base.error),
            onError = g(base.onError),
            errorContainer = g(base.errorContainer),
            onErrorContainer = g(base.onErrorContainer),
            outline = g(base.outline),
            outlineVariant = g(base.outlineVariant),
            surfaceBright = g(base.surfaceBright),
            surfaceDim = g(base.surfaceDim),
            surfaceContainer = g(base.surfaceContainer),
            surfaceContainerHigh = g(base.surfaceContainerHigh),
            surfaceContainerHighest = g(base.surfaceContainerHighest),
            surfaceContainerLow = g(base.surfaceContainerLow),
            surfaceContainerLowest = g(base.surfaceContainerLowest)
        )
    }

    /**
     * Black ink on white paper (white on black at night): every ground is the paper,
     * every text and line role the ink, containers one gray. Glance's widget background
     * is `secondaryContainer`, so that one stays paper too.
     */
    private fun eink(isDark: Boolean, base: ColorScheme): ColorScheme {
        val paper = if (isDark) Color.Black else Color.White
        val ink = if (isDark) Color.White else Color.Black
        val container = if (isDark) EINK_CONTAINER_NIGHT else EINK_CONTAINER_DAY
        return base.copy(
            primary = ink,
            onPrimary = paper,
            primaryContainer = container,
            onPrimaryContainer = ink,
            inversePrimary = paper,
            secondary = ink,
            onSecondary = paper,
            secondaryContainer = paper,
            onSecondaryContainer = ink,
            tertiary = ink,
            onTertiary = paper,
            tertiaryContainer = container,
            onTertiaryContainer = ink,
            background = paper,
            onBackground = ink,
            surface = paper,
            onSurface = ink,
            surfaceVariant = container,
            onSurfaceVariant = ink,
            surfaceTint = ink,
            inverseSurface = ink,
            inverseOnSurface = paper,
            error = ink,
            onError = paper,
            errorContainer = container,
            onErrorContainer = ink,
            outline = ink,
            outlineVariant = ink,
            surfaceBright = paper,
            surfaceDim = paper,
            surfaceContainer = paper,
            surfaceContainerHigh = paper,
            surfaceContainerHighest = paper,
            surfaceContainerLow = paper,
            surfaceContainerLowest = paper
        )
    }
}
