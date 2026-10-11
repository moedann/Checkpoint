package com.example.nanisini

import org.junit.Assert.*
import org.junit.Test

class SessionManagerTest {
    private fun draft(pkg: String = "youtube", duration: Long = 60_000) = SessionDraft(
        packageName = pkg, applicationLabel = pkg, purposeText = "二次関数の解説を見る",
        durationMillis = duration, currentActivityCategoryId = "study",
        currentActivityCategoryName = "勉強", currentActivityDetail = "数学の宿題",
    )

    @Test fun purposeSessionCreationRetainsReturnPoint() {
        val manager = SessionManager(TestClock(0))
        manager.onForegroundAppChanged("youtube", "YouTube")
        val session = manager.startSession(draft())
        assertEquals("youtube", session.packageName)
        assertEquals("数学の宿題", manager.currentReturnPoint()?.detail)
        assertEquals(PurposeSessionState.ACTIVE, session.purposeSessionState)
    }

    @Test fun switchingTargetAppsKeepsOldTimerAndRequiresNormalStartForNewApp() {
        val clock = TestClock(0); val manager = SessionManager(clock)
        manager.onForegroundAppChanged("youtube", "YouTube"); val original = manager.startSession(draft(duration = 600_000))
        clock.advance(300_000)
        val decision = manager.onForegroundAppChanged("chrome", "Chrome")
        assertTrue(decision.shouldShowStartOverlay)
        assertEquals(300_000, manager.elapsedMillis(original))
        assertNull(decision.replacedSession)
        assertEquals(TimerState.RUNNING, manager.sessionFor("youtube")?.timerState)
        assertEquals("draft:chrome", decision.activeSession?.sessionId)
        manager.startSession(draft("chrome", 600_000))
        assertEquals(2, manager.snapshot().size)
        assertEquals("数学の宿題", manager.currentReturnPoint()?.detail)
    }

    @Test fun homeRoundTripResumesSameAppWithoutCreatingNewSession() {
        val manager = SessionManager(TestClock(0))
        manager.onForegroundAppChanged("youtube", "YouTube"); manager.startSession(draft())
        manager.pauseForegroundSession()
        val returned = manager.onForegroundAppChanged("youtube", "YouTube")
        assertFalse(returned.shouldShowStartOverlay)
    }

    @Test fun countdownContinuesWhileTargetAppIsClosed() {
        val clock = TestClock(0); val manager = SessionManager(clock)
        manager.onForegroundAppChanged("youtube", "YouTube"); val session = manager.startSession(draft(duration = 600_000))
        clock.advance(300_000); manager.pauseForegroundSession()
        clock.advance(120_000)
        assertTrue(manager.expireDueSessions().isEmpty())
        clock.advance(180_000)
        assertEquals(session.sessionId, manager.expireDueSessions().single().sessionId)
        assertTrue(manager.expireDueSessions().isEmpty())
    }

    @Test fun notificationDeadlineContinuesOffForeground() {
        val clock = TestClock(1_000); val manager = SessionManager(clock)
        manager.onForegroundAppChanged("youtube", "YouTube")
        manager.startSession(draft(duration = 60_000))
        clock.advance(12_000)
        val paused = manager.pauseForegroundSession()!!
        assertFalse(paused.isForeground)
        assertEquals(61_000, paused.timerDeadlineElapsedRealtime)
        assertEquals(48_000, manager.remainingMillis(paused))
        clock.advance(30_000)
        assertEquals(18_000, manager.remainingMillis(paused))
        val resumed = manager.onForegroundAppChanged("youtube", "YouTube").activeSession!!
        assertTrue(resumed.isForeground)
        assertEquals(61_000, resumed.timerDeadlineElapsedRealtime)
    }

    @Test fun extensionKeepsPurposeAndReturnPointForSameApp() {
        val clock = TestClock(0); val manager = SessionManager(clock)
        manager.onForegroundAppChanged("youtube", "YouTube"); val original = manager.startSession(draft(duration = 100))
        clock.advance(100); manager.expireDueSessions()
        manager.setExtensionInput("youtube")
        val extended = manager.extendSession(draft("youtube", 180_000))!!
        assertEquals(original.sessionId, extended.sessionId)
        assertEquals(original.purposeText, extended.purposeText)
        assertEquals("数学の宿題", manager.currentReturnPoint()?.detail)
    }

    @Test fun extensionCanRecordAConsciousPurposeChangeInTheSameSession() {
        val clock = TestClock(0); val manager = SessionManager(clock)
        manager.onForegroundAppChanged("youtube", "YouTube")
        val original = manager.startSession(draft(duration = 100))
        clock.advance(100); manager.expireDueSessions(); manager.setExtensionInput("youtube")
        val changed = manager.extendSession(
            draft("youtube", 300_000).copy(
                purposeCategoryId = "work",
                purposeItemId = "purpose-work-5",
                purposeText = "Flutterの解説動画を見る",
            ),
        )!!
        assertEquals(original.sessionId, changed.sessionId)
        assertEquals("Flutterの解説動画を見る", changed.purposeText)
        assertEquals("purpose-work-5", changed.purposeItemId)
        assertEquals(1, changed.extensionCount)
    }

    @Test fun abandonStopsOldSessionAndOldExpiryCallbackIsIgnored() {
        val manager = SessionManager(TestClock(0))
        manager.onForegroundAppChanged("youtube", "YouTube"); val old = manager.startSession(draft())
        val closed = manager.abandonSession()!!
        assertEquals(PurposeSessionState.ABANDONED, closed.purposeSessionState)
        assertEquals(TimerState.CLOSED, closed.timerState)
        assertNull(manager.expireSessionById(old.sessionId))
    }

    @Test fun resetClearsTransientPurposeSessionAndReturnPoint() {
        val manager = SessionManager(TestClock(0))
        manager.onForegroundAppChanged("youtube", "YouTube"); manager.startSession(draft())
        manager.reset()
        assertNull(manager.activeSession()); assertNull(manager.currentReturnPoint())
    }
}
