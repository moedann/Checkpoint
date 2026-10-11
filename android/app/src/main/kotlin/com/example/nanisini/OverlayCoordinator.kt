package com.example.nanisini

import java.util.PriorityQueue

data class ExpiredRequest(val sessionId: String, val expiredAtElapsedRealtime: Long, val sequence: Long)

class ExpiredOverlayQueue {
    private var sequence = 0L
    private val ids = mutableSetOf<String>()
    private val queue = PriorityQueue(compareBy<ExpiredRequest> { it.expiredAtElapsedRealtime }.thenBy { it.sequence })
    fun enqueue(sessionId: String, expiredAtElapsedRealtime: Long): Boolean = if (ids.add(sessionId)) {
        queue += ExpiredRequest(sessionId, expiredAtElapsedRealtime, sequence++); true
    } else false
    fun peek(): String? = queue.peek()?.sessionId
    fun remove(sessionId: String): Boolean { ids.remove(sessionId); return queue.removeIf { it.sessionId == sessionId } }
    fun contains(sessionId: String): Boolean = sessionId in ids
    fun next(): String? { val item = queue.poll() ?: return null; ids.remove(item.sessionId); return item.sessionId }
    fun isEmpty(): Boolean = queue.isEmpty()
}

enum class ModalOverlay { NONE, START, EXPIRED, EXTENSION, RETURN_POINT }
data class OverlayPlan(val modal: ModalOverlay, val sessionId: String?, val suspended: Boolean = false)

class OverlayCoordinator(private val expiredQueue: ExpiredOverlayQueue = ExpiredOverlayQueue()) {
    var currentModal: ModalOverlay = ModalOverlay.NONE; private set
    var currentSessionId: String? = null; private set
    private var suspendedModal: ModalOverlay = ModalOverlay.NONE
    private var suspendedSessionId: String? = null

    fun expiryRequested(sessionId: String, deadline: Long): OverlayPlan {
        expiredQueue.enqueue(sessionId, deadline)
        return nextPlan()
    }
    fun startRequested(draftSessionId: String): OverlayPlan {
        if (currentModal == ModalOverlay.EXPIRED || currentModal == ModalOverlay.EXTENSION) {
            suspendedModal = currentModal; suspendedSessionId = currentSessionId
        }
        currentModal = ModalOverlay.START; currentSessionId = draftSessionId
        return OverlayPlan(currentModal, currentSessionId, suspendedModal != ModalOverlay.NONE)
    }
    fun returnPointRequested(sessionId: String): OverlayPlan {
        currentModal = ModalOverlay.RETURN_POINT; currentSessionId = sessionId
        return OverlayPlan(currentModal, currentSessionId)
    }
    fun extensionRequested(sessionId: String): OverlayPlan {
        currentModal = ModalOverlay.EXTENSION; currentSessionId = sessionId
        return OverlayPlan(currentModal, currentSessionId)
    }
    fun modalResolved(sessionId: String?): OverlayPlan {
        if (sessionId != null) expiredQueue.remove(sessionId)
        currentModal = ModalOverlay.NONE; currentSessionId = null
        if (suspendedModal != ModalOverlay.NONE) {
            currentModal = suspendedModal; currentSessionId = suspendedSessionId
            suspendedModal = ModalOverlay.NONE; suspendedSessionId = null
            return OverlayPlan(currentModal, currentSessionId)
        }
        return nextPlan()
    }
    fun suspendForSafety(): OverlayPlan {
        if (currentModal != ModalOverlay.NONE) { suspendedModal = currentModal; suspendedSessionId = currentSessionId }
        currentModal = ModalOverlay.NONE; currentSessionId = null
        return OverlayPlan(ModalOverlay.NONE, null, suspendedModal != ModalOverlay.NONE)
    }
    fun resumeAfterSafety(): OverlayPlan = if (suspendedModal != ModalOverlay.NONE) {
        currentModal = suspendedModal; currentSessionId = suspendedSessionId; suspendedModal = ModalOverlay.NONE; suspendedSessionId = null
        OverlayPlan(currentModal, currentSessionId)
    } else nextPlan()
    fun removeSession(sessionId: String) { expiredQueue.remove(sessionId); if (currentSessionId == sessionId) { currentModal = ModalOverlay.NONE; currentSessionId = null } }
    fun presentationFailed() {
        currentModal = ModalOverlay.NONE
        currentSessionId = null
    }
    private fun nextPlan(): OverlayPlan {
        if (currentModal != ModalOverlay.NONE) return OverlayPlan(currentModal, currentSessionId)
        val id = expiredQueue.peek() ?: return OverlayPlan(ModalOverlay.NONE, null)
        currentModal = ModalOverlay.EXPIRED; currentSessionId = id
        return OverlayPlan(currentModal, id)
    }
}
