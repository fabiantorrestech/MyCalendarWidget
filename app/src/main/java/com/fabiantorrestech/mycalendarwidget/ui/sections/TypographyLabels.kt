package com.fabiantorrestech.mycalendarwidget.ui.sections

import com.fabiantorrestech.mycalendarwidget.data.WidgetStyle

/**
 * What the Appearance section calls each font-scale slider and per-category font. The
 * six scale fields and six font categories are shared by every style, but each style
 * spends them on different things: the agenda styles on month/weekday/date headers and
 * event rows, the density style on the count, the date line, the axis and day labels,
 * and the peek sheet. Naming them after what the selected style draws keeps the sliders
 * honest; the field names underneath never change.
 */
data class TypographyLabels(
    val headerScale: String,
    val subheaderScale: String,
    val dateHeaderScale: String,
    val eventTimeScale: String,
    val eventNameScale: String,
    val detailScale: String,
    val monthHeaderFont: String,
    val weekdayHeaderFont: String,
    val dateHeaderFont: String,
    val eventTimeFont: String,
    val eventNameFont: String,
    val detailFont: String,
    val eventTimeDescription: String? = null,
    val detailDescription: String? = null
) {
    companion object {
        fun forStyle(style: WidgetStyle): TypographyLabels = when (style) {
            WidgetStyle.AGENDA, WidgetStyle.GCAL, WidgetStyle.GCAL_LEFT -> TypographyLabels(
                headerScale = "Month header",
                subheaderScale = "Weekday Headers",
                dateHeaderScale = "Date Headers",
                eventTimeScale = "Event Time",
                eventNameScale = "Event Names",
                detailScale = "Details",
                monthHeaderFont = "Month Header",
                weekdayHeaderFont = "Weekday Headers",
                dateHeaderFont = "Date Headers",
                eventTimeFont = "Event Time",
                eventNameFont = "Event Names",
                detailFont = "Details"
            )
            // The count is sized by headerScale but set in the DATE_HEADER font, so the
            // two "Daily items left" entries name the same element from both sides.
            WidgetStyle.DENSITY -> TypographyLabels(
                headerScale = "Daily items left",
                subheaderScale = "Peek date headers",
                dateHeaderScale = "Date Headers",
                eventTimeScale = "Following days bars",
                eventNameScale = "Peek event names",
                detailScale = "Date & next-event line",
                monthHeaderFont = "Month Header",
                weekdayHeaderFont = "Peek date headers",
                dateHeaderFont = "Daily items left",
                eventTimeFont = "Following days bars",
                eventNameFont = "Peek event names",
                detailFont = "Date & next-event line",
                eventTimeDescription = "Also the hour axis and the peek's time column",
                detailDescription = null
            )
        }
    }
}
