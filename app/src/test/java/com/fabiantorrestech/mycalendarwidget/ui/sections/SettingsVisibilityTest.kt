package com.fabiantorrestech.mycalendarwidget.ui.sections

import com.fabiantorrestech.mycalendarwidget.data.WidgetStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Which settings each widget style shows. The density style renders no event text, no
 * month chrome and no agenda list, and its refresh cadence is fixed, so those controls
 * are hidden for it rather than left inert; the three agenda styles show everything but
 * the density-only controls.
 */
class SettingsVisibilityTest {

    private val everythingButDensity = VisibleSettings(
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

    @Test
    fun density_hidesAgendaOnlySettings() {
        val v = VisibleSettings.forStyle(WidgetStyle.DENSITY)
        assertEquals(false, v.agendaText)
        assertEquals(false, v.monthChrome)
        assertEquals(false, v.dateHeaderScale)
        assertEquals(false, v.monthHeaderFont)
        assertEquals(false, v.listBehaviour)
        assertEquals(false, v.syncInterval)
        assertEquals(false, v.layoutProfiles)
    }

    @Test
    fun density_locksProfiles() {
        assertEquals(false, VisibleSettings.forStyle(WidgetStyle.DENSITY).profilesEditable)
        assertEquals(true, VisibleSettings.forStyle(WidgetStyle.AGENDA).profilesEditable)
    }

    @Test
    fun density_keepsPeekHorizonAndDensitySection() {
        val v = VisibleSettings.forStyle(WidgetStyle.DENSITY)
        assertEquals(true, v.peekHorizon)
        assertEquals(true, v.densitySection)
    }

    @Test
    fun agenda_showsEverythingButDensity() {
        assertEquals(everythingButDensity, VisibleSettings.forStyle(WidgetStyle.AGENDA))
    }

    @Test
    fun gcal_showsEverythingButDensity() {
        assertEquals(everythingButDensity, VisibleSettings.forStyle(WidgetStyle.GCAL))
    }

    @Test
    fun gcalLeft_showsEverythingButDensity() {
        assertEquals(everythingButDensity, VisibleSettings.forStyle(WidgetStyle.GCAL_LEFT))
    }

    @Test
    fun everyStyleProducesASpec() {
        WidgetStyle.entries.forEach { assertNotNull(VisibleSettings.forStyle(it)) }
    }
}
