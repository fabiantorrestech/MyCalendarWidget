package com.fabiantorrestech.mycalendarwidget.data.density

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.provider.CalendarContract.Instances
import androidx.core.content.ContextCompat

/**
 * The only file in the density feature that touches [android.content.ContentResolver].
 *
 * G1 contract: this class is content-free by construction. [queryRawInstances]'s
 * projection is exactly ten columns — begin/end/all-day/calendar id/self-attendee-status/
 * status/availability and three colour columns — and [queryVisibleCalendarIds]'s is a
 * single id column; neither can carry an event's own words, notes, place or participants,
 * because those columns are never requested. Nothing downstream of [RawInstance] (in this
 * package or the widget's density package) may import the app's other, text-bearing
 * calendar model, or add a text column here; doing so would break the guarantee this class
 * exists to provide.
 */
class DensityCalendarSource(private val context: Context) {

    /** True when the app currently holds READ_CALENDAR. */
    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * The ids of calendars the provider currently marks `VISIBLE` (the same set the user
     * sees checked on in their calendar app), sorted ascending. Used as the Tonal strip's
     * "no explicit calendar filter" rank fallback ([DensitySpecBuilder]'s
     * `enabledSortedCalendarIds`) so that fallback ranks the same calendars, in the same
     * order, regardless of which day is being rendered — a per-day "who actually has an
     * event today" fallback would rank a calendar's tone differently from one day to the
     * next and disagree with what Settings shows the user. Empty when permission is
     * missing, the cursor comes back null, or the query throws (mirrors
     * [queryRawInstances]'s "never see an exception" contract).
     */
    fun queryVisibleCalendarIds(): List<Long> {
        if (!hasPermission()) return emptyList()

        val projection = arrayOf(CalendarContract.Calendars._ID)
        val selection = "${CalendarContract.Calendars.VISIBLE} = 1"

        val cursor = try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                selection,
                null,
                null
            )
        } catch (e: SecurityException) {
            null
        } ?: return emptyList()

        val ids = mutableListOf<Long>()
        cursor.use {
            val idIdx = it.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            while (it.moveToNext()) {
                ids.add(it.getLong(idIdx))
            }
        }
        return ids.sorted()
    }

    /**
     * Raw, content-free instances overlapping `[startMillis, endMillisExclusive)`. Returns
     * an empty list when permission is missing or the cursor comes back null — callers
     * never see an exception from this method.
     */
    fun queryRawInstances(startMillis: Long, endMillisExclusive: Long): List<RawInstance> {
        if (!hasPermission()) return emptyList()

        val uri = Instances.CONTENT_URI.buildUpon().let {
            ContentUris.appendId(it, startMillis)
            ContentUris.appendId(it, endMillisExclusive)
            it.build()
        }

        val projection = arrayOf(
            Instances.BEGIN,
            Instances.END,
            Instances.ALL_DAY,
            Instances.CALENDAR_ID,
            Instances.SELF_ATTENDEE_STATUS,
            Instances.STATUS,
            Instances.AVAILABILITY,
            Instances.DISPLAY_COLOR,
            Instances.CALENDAR_COLOR,
            Instances.EVENT_COLOR
        )

        val selection = "${CalendarContract.Events.DELETED} != 1"

        val result = mutableListOf<RawInstance>()
        // hasPermission() above can race a user revoking READ_CALENDAR from Settings while
        // this query is in flight (e.g. during a periodic widget refresh); the resolver
        // then throws SecurityException instead of returning null, so it must be caught
        // here too for the "never see an exception" contract above to actually hold.
        val cursor = try {
            context.contentResolver.query(
                uri,
                projection,
                selection,
                null,
                "${Instances.BEGIN} ASC"
            )
        } catch (e: SecurityException) {
            null
        } ?: return emptyList()

        cursor.use {
            val beginIdx = it.getColumnIndexOrThrow(Instances.BEGIN)
            val endIdx = it.getColumnIndexOrThrow(Instances.END)
            val allDayIdx = it.getColumnIndexOrThrow(Instances.ALL_DAY)
            val calendarIdIdx = it.getColumnIndexOrThrow(Instances.CALENDAR_ID)
            val selfAttendeeStatusIdx = it.getColumnIndexOrThrow(Instances.SELF_ATTENDEE_STATUS)
            val statusIdx = it.getColumnIndexOrThrow(Instances.STATUS)
            val availabilityIdx = it.getColumnIndexOrThrow(Instances.AVAILABILITY)
            val displayColorIdx = it.getColumnIndexOrThrow(Instances.DISPLAY_COLOR)
            val calendarColorIdx = it.getColumnIndexOrThrow(Instances.CALENDAR_COLOR)
            val eventColorIdx = it.getColumnIndexOrThrow(Instances.EVENT_COLOR)

            while (it.moveToNext()) {
                // 0 is treated the same as null: a present-but-zero CALENDAR_COLOR is not
                // a real colour choice (Detail would otherwise paint a black block), so
                // it falls back to the same default an absent column would.
                val calendarColorRaw = if (it.isNull(calendarColorIdx)) 0 else it.getInt(calendarColorIdx)
                val calendarColor = if (calendarColorRaw != 0) {
                    calendarColorRaw
                } else {
                    DensityConstants.DEFAULT_BUSY_COLOR
                }

                // Prefer EVENT_COLOR when set and non-zero, else DISPLAY_COLOR, else the
                // calendar's own colour, which is already null-safe above.
                val eventColor = if (!it.isNull(eventColorIdx)) it.getInt(eventColorIdx) else 0
                val displayColorRaw = if (it.isNull(displayColorIdx)) 0 else it.getInt(displayColorIdx)
                val displayColor = when {
                    eventColor != 0 -> eventColor
                    displayColorRaw != 0 -> displayColorRaw
                    else -> calendarColor
                }

                result.add(
                    RawInstance(
                        begin = it.getLong(beginIdx),
                        end = it.getLong(endIdx),
                        allDay = it.getInt(allDayIdx) == 1,
                        calendarId = it.getLong(calendarIdIdx),
                        selfAttendeeStatus = it.getInt(selfAttendeeStatusIdx),
                        status = it.getInt(statusIdx),
                        availability = it.getInt(availabilityIdx),
                        displayColor = displayColor,
                        calendarColor = calendarColor
                    )
                )
            }
        }

        return result
    }
}
