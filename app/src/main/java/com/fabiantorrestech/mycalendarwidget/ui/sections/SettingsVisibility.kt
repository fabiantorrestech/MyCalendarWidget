package com.fabiantorrestech.mycalendarwidget.ui.sections

import com.fabiantorrestech.mycalendarwidget.data.WidgetStyle

/**
 * Which optional settings groups the settings screen shows for a widget style. The one
 * place that knows what each style actually reads, so a control is hidden rather than
 * left inert: the density style renders no event text, no month chrome and no agenda
 * list, its refresh cadence is fixed at five minutes, and the Advanced layout presets
 * only write text-line fields it ignores. Everything not listed here (style picker,
 * Material You, the font scales and fonts the style reads, quick-add and refresh
 * toggles, click routing, calendars, profiles, backup) is shown for every style.
 */
data class VisibleSettings(
    /** Max title/detail lines, location, description, spanning-event duplication. */
    val agendaText: Boolean,
    /** Month navigation (and its style) and "show month in header". */
    val monthChrome: Boolean,
    /** The "Date Headers" font-scale slider. */
    val dateHeaderScale: Boolean,
    /** The per-category "Month Header" font dropdown. */
    val monthHeaderFont: Boolean,
    /** Show empty days, always show today, strict grid. */
    val listBehaviour: Boolean,
    /** The look-ahead range relabelled as the peek horizon (density only). */
    val peekHorizon: Boolean,
    /** The Advanced sync-interval slider. */
    val syncInterval: Boolean,
    /** The Advanced Standard/Dense/Minimal preset buttons. */
    val layoutProfiles: Boolean,
    /** The Density section itself. */
    val densitySection: Boolean,
    /**
     * Whether profiles can be added, switched, renamed, reordered or deleted here. The
     * density style uses only the selected profile (and draws no switcher), so its
     * profile controls are shown greyed out with a note rather than removed.
     */
    val profilesEditable: Boolean
) {
    companion object {
        fun forStyle(style: WidgetStyle): VisibleSettings = when (style) {
            WidgetStyle.AGENDA, WidgetStyle.GCAL, WidgetStyle.GCAL_LEFT -> VisibleSettings(
                agendaText = true,
                monthChrome = true,
                dateHeaderScale = true,
                monthHeaderFont = true,
                listBehaviour = true,
                peekHorizon = false,
                syncInterval = true,
                layoutProfiles = true,
                densitySection = false,
                profilesEditable = true
            )
            WidgetStyle.DENSITY -> VisibleSettings(
                agendaText = false,
                monthChrome = false,
                dateHeaderScale = false,
                monthHeaderFont = false,
                listBehaviour = false,
                peekHorizon = true,
                syncInterval = false,
                layoutProfiles = false,
                densitySection = true,
                profilesEditable = false
            )
        }
    }
}
