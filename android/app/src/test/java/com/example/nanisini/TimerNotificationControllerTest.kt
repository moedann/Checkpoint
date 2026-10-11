package com.example.nanisini

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TimerNotificationControllerTest {
    @Test fun notificationIdIsStablePerSession() {
        assertEquals(
            TimerNotificationController.notificationIdFor("session-a"),
            TimerNotificationController.notificationIdFor("session-a"),
        )
    }

    @Test fun ordinarySessionsDoNotOverwriteEachOther() {
        assertNotEquals(
            TimerNotificationController.notificationIdFor("youtube-session"),
            TimerNotificationController.notificationIdFor("instagram-session"),
        )
    }

    @Test fun runningAndExpirationNotificationsUseDifferentIds() {
        assertNotEquals(
            TimerNotificationController.notificationIdFor("purpose-session"),
            TimerNotificationController.expirationNotificationIdFor("purpose-session"),
        )
    }

    @Test fun backgroundNotificationUsesTheSameMonotonicDeadline() {
        val paused = ActiveSession(
            sessionId = "session", packageName = "youtube", applicationLabel = "YouTube",
            timerDurationMillis = 60_000, segmentActualUsageMillis = 12_000,
            timerDeadlineElapsedRealtime = 61_000, timerState = TimerState.RUNNING,
            isForeground = false,
        )

        assertEquals(48_000, TimerNotificationController.remainingForNotification(paused, 13_000))
        assertEquals("0:48", TimerNotificationController.formatRemaining(48_000))
    }

    @Test fun foregroundNotificationUsesItsMonotonicDeadline() {
        val running = ActiveSession(
            sessionId = "session", packageName = "youtube", applicationLabel = "YouTube",
            timerDurationMillis = 60_000, timerDeadlineElapsedRealtime = 61_000,
            timerState = TimerState.RUNNING, isForeground = true,
        )

        assertEquals(48_000, TimerNotificationController.remainingForNotification(running, 13_000))
    }
}
