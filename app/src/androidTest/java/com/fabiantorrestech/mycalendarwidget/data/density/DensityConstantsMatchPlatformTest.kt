package com.fabiantorrestech.mycalendarwidget.data.density

import android.provider.CalendarContract
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guards the mirrored constants in [DensityConstants] against the platform's own values.
 * This package cannot import `android.provider.CalendarContract` (it must stay pure), so
 * the mirror is checked here instead, on-device. Not run as part of this task (no device
 * guarantee) — it only needs to compile under `./gradlew assembleDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class DensityConstantsMatchPlatformTest {

    @Test
    fun attendeeStatusDeclinedMatchesPlatform() {
        assertEquals(
            CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED,
            DensityConstants.ATTENDEE_STATUS_DECLINED
        )
    }

    @Test
    fun eventStatusCanceledMatchesPlatform() {
        assertEquals(
            CalendarContract.Events.STATUS_CANCELED,
            DensityConstants.EVENT_STATUS_CANCELED
        )
    }

    @Test
    fun availabilityFreeMatchesPlatform() {
        assertEquals(
            CalendarContract.Events.AVAILABILITY_FREE,
            DensityConstants.AVAILABILITY_FREE
        )
    }
}
