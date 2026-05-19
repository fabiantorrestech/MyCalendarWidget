package com.fabiantorrestech.mycalendarwidget.data

enum class AutomationProfile { STANDARD, DENSE, MINIMAL }

enum class DefaultClickTarget { SYSTEM_DEFAULT, DIGICAL, GCAL }

enum class WidgetStyle { AGENDA, GCAL, GCAL_LEFT }

enum class HeaderNavStyle { ARROWS, CHIPS }

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
    val dateScale: Float = 1.0f,
    val detailScale: Float = 1.0f
)

data class WidgetConfig(
    val dynamicColor: Boolean = true,
    val typographyScale: TypographyScale = TypographyScale(),
    val enabledCalendarIds: Set<Long> = emptySet(),
    val keywordFilter: String = "",
    val filterIsRegex: Boolean = false,
    val defaultClickTarget: DefaultClickTarget = DefaultClickTarget.SYSTEM_DEFAULT,
    val showQuickAddFab: Boolean = true,
    val strictGridMode: Boolean = false,
    val maxTitleLines: Int = 2,
    val maxDetailLines: Int = 1,
    val showLocation: Boolean = true,
    val showDescription: Boolean = false,
    val daysAheadToLoad: Int = 30,
    val showEmptyDays: Boolean = false,
    val widgetStyle: WidgetStyle = WidgetStyle.AGENDA,
    val activeProfile: AutomationProfile = AutomationProfile.STANDARD,
    val headerNavEnabled: Boolean = false,
    val headerNavStyle: HeaderNavStyle = HeaderNavStyle.ARROWS,
    val monthOffset: Int = 0,
    val showMonthInHeader: Boolean = true,
    val syncIntervalMinutes: Int = 0,
    val configVersion: Int = 1
)

fun AutomationProfile.toWidgetConfig(base: WidgetConfig): WidgetConfig = when (this) {
    AutomationProfile.STANDARD -> base.copy(
        typographyScale = base.typographyScale.copy(headerScale = 1.0f, subheaderScale = 1.0f, dateScale = 1.0f, detailScale = 1.0f),
        maxTitleLines = 2,
        maxDetailLines = 1,
        showLocation = true,
        showDescription = false,
        activeProfile = AutomationProfile.STANDARD
    )
    AutomationProfile.DENSE -> base.copy(
        typographyScale = base.typographyScale.copy(headerScale = 0.85f, subheaderScale = 0.85f, dateScale = 0.85f, detailScale = 0.85f),
        maxTitleLines = 1,
        maxDetailLines = 0,
        showLocation = false,
        showDescription = false,
        activeProfile = AutomationProfile.DENSE
    )
    AutomationProfile.MINIMAL -> base.copy(
        typographyScale = base.typographyScale.copy(headerScale = 0.75f, subheaderScale = 0.75f, dateScale = 0.75f, detailScale = 0.75f),
        maxTitleLines = 1,
        maxDetailLines = 0,
        showLocation = false,
        showDescription = false,
        activeProfile = AutomationProfile.MINIMAL
    )
}
