package com.fabiantorrestech.mycalendarwidget.data

enum class AutomationProfile { STANDARD, DENSE, MINIMAL }

enum class DefaultClickTarget { SYSTEM_DEFAULT, DIGICAL, GCAL }

enum class WidgetStyle { AGENDA, GCAL, GCAL_LEFT }

enum class HeaderNavStyle { ARROWS, CHIPS }

enum class CalendarLaunchView { DEFAULT, DAY, WEEK, WEEK_AGENDA, MONTH, TEXT_MONTH, YEAR, AGENDA }

enum class WidgetFont(val displayName: String, val glanceFamilyName: String?) {
    DEFAULT("Default", null),
    SERIF("Serif", "serif"),
    MONOSPACE("Monospace", "monospace"),
    CONDENSED("Condensed", "sans-serif-condensed"),
    LIGHT("Light", "sans-serif-light")
}

enum class FontMode { DEFAULT, UNIVERSAL, PER_CATEGORY }

enum class FontCategory { MONTH_HEADER, WEEKDAY_HEADER, DATE_HEADER, EVENT_TIME, EVENT_NAME, DETAIL }

data class FontConfig(
    val mode: FontMode = FontMode.DEFAULT,
    val universalFont: WidgetFont = WidgetFont.DEFAULT,
    val monthHeaderFont: WidgetFont = WidgetFont.DEFAULT,
    val weekdayHeaderFont: WidgetFont = WidgetFont.DEFAULT,
    val dateHeaderFont: WidgetFont = WidgetFont.DEFAULT,
    val eventTimeFont: WidgetFont = WidgetFont.DEFAULT,
    val eventNameFont: WidgetFont = WidgetFont.DEFAULT,
    val detailFont: WidgetFont = WidgetFont.DEFAULT
) {
    fun resolve(category: FontCategory): WidgetFont = when (mode) {
        FontMode.DEFAULT -> WidgetFont.DEFAULT
        FontMode.UNIVERSAL -> universalFont
        FontMode.PER_CATEGORY -> when (category) {
            FontCategory.MONTH_HEADER -> monthHeaderFont
            FontCategory.WEEKDAY_HEADER -> weekdayHeaderFont
            FontCategory.DATE_HEADER -> dateHeaderFont
            FontCategory.EVENT_TIME -> eventTimeFont
            FontCategory.EVENT_NAME -> eventNameFont
            FontCategory.DETAIL -> detailFont
        }
    }
}

data class CalendarInfo(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val color: Int,
    val enabled: Boolean
)

data class TypographyScale(
    val headerScale: Float = 1.0f,
    val subheaderScale: Float = 1.0f,
    val dateHeaderScale: Float = 1.0f,
    val eventTimeScale: Float = 1.0f,
    val eventNameScale: Float = 1.0f,
    val detailScale: Float = 1.0f
)

data class WidgetConfig(
    val widgetName: String = "",
    val dynamicColor: Boolean = true,
    val typographyScale: TypographyScale = TypographyScale(),
    val fontConfig: FontConfig = FontConfig(),
    val enabledCalendarIds: Set<Long> = emptySet(),
    val keywordFilter: String = "",
    val filterIsRegex: Boolean = false,
    val defaultClickTarget: DefaultClickTarget = DefaultClickTarget.SYSTEM_DEFAULT,
    val showQuickAddFab: Boolean = true,
    val showRefreshButton: Boolean = true,
    val strictGridMode: Boolean = false,
    val maxTitleLines: Int = 2,
    val maxDetailLines: Int = 1,
    val showLocation: Boolean = true,
    val showDescription: Boolean = false,
    val daysAheadToLoad: Int = 30,
    val showEmptyDays: Boolean = false,
    val alwaysShowToday: Boolean = false,
    val widgetStyle: WidgetStyle = WidgetStyle.GCAL_LEFT,
    val calendarLaunchView: CalendarLaunchView = CalendarLaunchView.DEFAULT,
    val activeProfile: AutomationProfile = AutomationProfile.STANDARD,
    val headerNavEnabled: Boolean = false,
    val headerNavStyle: HeaderNavStyle = HeaderNavStyle.ARROWS,
    val monthOffset: Int = 0,
    val showMonthInHeader: Boolean = true,
    val syncIntervalMinutes: Int = 0,
    val refreshNonce: Int = 0,
    val configVersion: Int = 1
)

data class WidgetSummary(
    val appWidgetId: Int,
    val name: String,
    val style: WidgetStyle,
    val displayIndex: Int,
    val syncSourceId: Int?
)

fun AutomationProfile.toWidgetConfig(base: WidgetConfig): WidgetConfig = when (this) {
    AutomationProfile.STANDARD -> base.copy(
        typographyScale = base.typographyScale.copy(
            headerScale = 1.0f, subheaderScale = 1.0f,
            dateHeaderScale = 1.0f, eventTimeScale = 1.0f,
            eventNameScale = 1.0f, detailScale = 1.0f
        ),
        maxTitleLines = 2,
        maxDetailLines = 1,
        showLocation = true,
        showDescription = false,
        activeProfile = AutomationProfile.STANDARD
    )
    AutomationProfile.DENSE -> base.copy(
        typographyScale = base.typographyScale.copy(
            headerScale = 0.85f, subheaderScale = 0.85f,
            dateHeaderScale = 0.85f, eventTimeScale = 0.85f,
            eventNameScale = 0.85f, detailScale = 0.85f
        ),
        maxTitleLines = 1,
        maxDetailLines = 0,
        showLocation = false,
        showDescription = false,
        activeProfile = AutomationProfile.DENSE
    )
    AutomationProfile.MINIMAL -> base.copy(
        typographyScale = base.typographyScale.copy(
            headerScale = 0.75f, subheaderScale = 0.75f,
            dateHeaderScale = 0.75f, eventTimeScale = 0.75f,
            eventNameScale = 0.75f, detailScale = 0.75f
        ),
        maxTitleLines = 1,
        maxDetailLines = 0,
        showLocation = false,
        showDescription = false,
        activeProfile = AutomationProfile.MINIMAL
    )
}
