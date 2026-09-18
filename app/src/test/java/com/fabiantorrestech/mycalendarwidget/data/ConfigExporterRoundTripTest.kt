package com.fabiantorrestech.mycalendarwidget.data

import com.fabiantorrestech.mycalendarwidget.data.WidgetStyle
import com.fabiantorrestech.mycalendarwidget.data.DensityStripMode
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Closes the "silent drop" bug class: every WidgetConfig field must survive a
 * toJson/fromJson round trip, because profiles are persisted as JSON via
 * ConfigExporter and a field missing there is silently dropped on every profile write.
 */
class ConfigExporterRoundTripTest {

    /** A WidgetConfig with every field set to a non-default value. */
    private fun nonDefaultConfig(): WidgetConfig = WidgetConfig(
        widgetName = "My Widget",
        dynamicColor = false,
        typographyScale = TypographyScale(
            headerScale = 1.5f,
            subheaderScale = 1.4f,
            dateHeaderScale = 1.3f,
            eventTimeScale = 1.2f,
            eventNameScale = 1.1f,
            detailScale = 0.9f
        ),
        fontConfig = FontConfig(
            mode = FontMode.PER_CATEGORY,
            universalFont = WidgetFont.SERIF,
            monthHeaderFont = WidgetFont.MONOSPACE,
            weekdayHeaderFont = WidgetFont.CONDENSED,
            dateHeaderFont = WidgetFont.LIGHT,
            eventTimeFont = WidgetFont.SERIF,
            eventNameFont = WidgetFont.MONOSPACE,
            detailFont = WidgetFont.CONDENSED
        ),
        enabledCalendarIds = setOf(1L, 2L, 3L),
        keywordFilter = "standup",
        filterIsRegex = true,
        defaultClickTarget = DefaultClickTarget.GCAL,
        showQuickAddFab = false,
        showRefreshButton = false,
        strictGridMode = true,
        maxTitleLines = 3,
        maxDetailLines = 2,
        showLocation = false,
        showDescription = true,
        daysAheadToLoad = 14,
        showEmptyDays = true,
        alwaysShowToday = true,
        showSpanningEventsEachDay = true,
        widgetStyle = WidgetStyle.DENSITY,
        calendarLaunchView = CalendarLaunchView.WEEK_AGENDA,
        activeProfile = AutomationProfile.DENSE,
        headerNavEnabled = true,
        headerNavStyle = HeaderNavStyle.CHIPS,
        monthOffset = 2,
        showMonthInHeader = false,
        syncIntervalMinutes = 15,
        refreshNonce = 7,
        densityWindowStartMinutes = 360,
        densityWindowEndMinutes = 1380,
        densityLookaheadDays = 5,
        densityLoadBaselineMinutes = 600,
        densityRolloverHour = 22,
        densityBusyColor = -12345,
        densityStripMode = DensityStripMode.TONAL,
        densityPeekFormat = DensityPeekFormat.DATED,
        densityCountMode = DensityCountMode.FRACTION,
        densityCalendarTones = mapOf(12L to 0, 15L to 3, 99L to 4),
        // WidgetConfig()'s default is 1 — use 2 here so this really is a non-default
        // value, matching the class doc's claim that every field differs from default.
        configVersion = 2
    )

    @Test
    fun `fully non-default config round-trips through toJson and fromJson`() {
        val config = nonDefaultConfig()

        val roundTripped = ConfigExporter.fromJson(ConfigExporter.toJson(config))

        assertEquals(config, roundTripped)
    }

    @Test
    fun `fromJson of empty object equals WidgetConfig defaults`() {
        val roundTripped = ConfigExporter.fromJson(org.json.JSONObject("{}"))

        assertEquals(WidgetConfig(), roundTripped)
    }

    @Test
    fun `a fresh widget is Density on the Tonal strip`() {
        // The placement default lives in three places that must agree: the config's own
        // defaults, the exporter's fallbacks and (checked on device) the datastore's.
        assertEquals(WidgetStyle.DENSITY, WidgetConfig().widgetStyle)
        assertEquals(DensityStripMode.TONAL, WidgetConfig().densityStripMode)
        val fromEmpty = ConfigExporter.fromJson(org.json.JSONObject("{}"))
        assertEquals(WidgetStyle.DENSITY, fromEmpty.widgetStyle)
        assertEquals(DensityStripMode.TONAL, fromEmpty.densityStripMode)
    }

    @Test
    fun `calendarTonesFromJson tolerates garbage and null`() {
        assertEquals(emptyMap<Long, Int>(), ConfigExporter.calendarTonesFromJson("not json"))
        assertEquals(emptyMap<Long, Int>(), ConfigExporter.calendarTonesFromJson(null))
    }
}
