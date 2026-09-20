package com.fabiantorrestech.mycalendarwidget.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The auto-backup's file naming and per-widget JSON, kept pure so both are pinned. */
class AutoBackupFormatTest {

    @Test
    fun fileNameSlugsTheWidgetName() {
        assertEquals("bridgecal_widget_7_kitchen-tablet.json", AutoBackupFormat.fileName(7, "Kitchen Tablet!"))
        assertEquals("bridgecal_widget_7_a-b.json", AutoBackupFormat.fileName(7, "  a / b  "))
    }

    @Test
    fun fileNameOmitsABlankName() {
        assertEquals("bridgecal_widget_12.json", AutoBackupFormat.fileName(12, ""))
        assertEquals("bridgecal_widget_12.json", AutoBackupFormat.fileName(12, "   "))
    }

    @Test
    fun fileNameCapsTheSlug() {
        val long = "x".repeat(80)
        val name = AutoBackupFormat.fileName(1, long)
        assertTrue(name.length <= "bridgecal_widget_1_".length + 40 + ".json".length)
    }

    @Test
    fun entryCarriesTheWidgetIdentityAndLink() {
        val config = WidgetConfig(densityLookaheadDays = 5)
        val json = AutoBackupFormat.entryJson(config, widgetId = 9, widgetName = "Desk", syncSourceId = 3)
        assertEquals(9, json.getInt("widgetId"))
        assertEquals("Desk", json.getString("widgetName"))
        assertEquals(3, json.getInt("syncSourceId"))
        assertEquals(1, json.getInt("backupVersion"))
        val unlinked = AutoBackupFormat.entryJson(config, widgetId = 9, widgetName = "", syncSourceId = null)
        assertTrue(unlinked.isNull("syncSourceId"))
    }

    @Test
    fun entryStillImportsAsAPlainConfig() {
        val config = WidgetConfig(densityLookaheadDays = 5, densityRolloverHour = 21)
        val json = AutoBackupFormat.entryJson(config, widgetId = 9, widgetName = "Desk", syncSourceId = 3)
        // The stored widget name travels with the file, so it lands in the imported config.
        assertEquals(config.copy(widgetName = "Desk"), ConfigExporter.fromJson(json))
    }
}
