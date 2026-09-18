package com.fabiantorrestech.mycalendarwidget.ui.sections

import com.fabiantorrestech.mycalendarwidget.data.WidgetStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The Appearance section names each font-scale slider and per-category font after what
 * the selected style actually draws with it; the agenda styles keep their original
 * wording, Density is relabelled element by element.
 */
class TypographyLabelsTest {

    @Test
    fun density_relabelsWhatDensityDraws() {
        val l = TypographyLabels.forStyle(WidgetStyle.DENSITY)
        assertEquals("Daily items left", l.headerScale)
        assertEquals("Peek date headers", l.subheaderScale)
        assertEquals("Following days bars", l.eventTimeScale)
        assertEquals("Peek event names", l.eventNameScale)
        assertEquals("Date & next-event line", l.detailScale)
        assertEquals("Daily items left", l.dateHeaderFont)
        assertEquals("Peek date headers", l.weekdayHeaderFont)
        assertEquals("Following days bars", l.eventTimeFont)
        assertEquals("Peek event names", l.eventNameFont)
        assertEquals("Date & next-event line", l.detailFont)
        assertNotNull(l.eventTimeDescription)
    }

    @Test
    fun agenda_keepsTheAgendaLabels() {
        listOf(WidgetStyle.AGENDA, WidgetStyle.GCAL, WidgetStyle.GCAL_LEFT).forEach { style ->
            val l = TypographyLabels.forStyle(style)
            assertEquals("Month header", l.headerScale)
            assertEquals("Weekday Headers", l.subheaderScale)
            assertEquals("Date Headers", l.dateHeaderScale)
            assertEquals("Event Time", l.eventTimeScale)
            assertEquals("Event Names", l.eventNameScale)
            assertEquals("Details", l.detailScale)
            assertEquals("Month Header", l.monthHeaderFont)
            assertEquals("Weekday Headers", l.weekdayHeaderFont)
            assertEquals("Date Headers", l.dateHeaderFont)
            assertEquals("Event Time", l.eventTimeFont)
            assertEquals("Event Names", l.eventNameFont)
            assertEquals("Details", l.detailFont)
            assertNull(l.eventTimeDescription)
            assertNull(l.detailDescription)
        }
    }

    @Test
    fun everyStyleHasLabels() {
        WidgetStyle.entries.forEach { assertNotNull(TypographyLabels.forStyle(it)) }
    }
}
