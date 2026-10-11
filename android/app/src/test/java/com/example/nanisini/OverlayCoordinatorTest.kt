package com.example.nanisini

import org.junit.Assert.*
import org.junit.Test

class OverlayCoordinatorTest {
    @Test fun queueIsDeadlineOrderedAndDeduplicated() {
        val queue = ExpiredOverlayQueue()
        assertTrue(queue.enqueue("instagram", 190)); assertTrue(queue.enqueue("youtube", 180)); assertFalse(queue.enqueue("youtube", 180))
        assertEquals("youtube", queue.next()); assertEquals("instagram", queue.next()); assertNull(queue.next())
    }

    @Test fun expiryWaitsBehindVisibleExpiry() {
        val coordinator = OverlayCoordinator()
        assertEquals("youtube", coordinator.expiryRequested("youtube", 180).sessionId)
        assertEquals("youtube", coordinator.expiryRequested("instagram", 190).sessionId)
        assertEquals("instagram", coordinator.modalResolved("youtube").sessionId)
    }

    @Test fun startSuspendsAndResumesExpiryBeforeQueue() {
        val coordinator = OverlayCoordinator()
        coordinator.expiryRequested("youtube", 180); coordinator.expiryRequested("instagram", 190)
        val start = coordinator.startRequested("draft:chrome")
        assertEquals(ModalOverlay.START, start.modal); assertTrue(start.suspended)
        val resumed = coordinator.modalResolved("draft:chrome")
        assertEquals(ModalOverlay.EXPIRED, resumed.modal); assertEquals("youtube", resumed.sessionId)
    }

}
