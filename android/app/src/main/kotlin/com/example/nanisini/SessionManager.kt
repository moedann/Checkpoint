package com.example.nanisini

import java.util.UUID

/** Owns independent sessions per app and the shared foreground activity. */
class SessionManager(private val clock: ElapsedClock) {
    private val sessions = linkedMapOf<String, ActiveSession>()
    private var returnPoint: ReturnPoint? = null
    private var currentForegroundPackage: String? = null

    fun snapshot(): Map<String, ActiveSession> = sessions.toMap()
    fun sessionFor(packageName: String): ActiveSession? = sessions[packageName]
    fun sessionById(sessionId: String): ActiveSession? = sessions.values.firstOrNull { it.sessionId == sessionId }
    fun activeSession(): ActiveSession? = currentForegroundPackage?.let(sessions::get)
    fun foregroundPackage(): String? = currentForegroundPackage
    fun currentReturnPoint(): ReturnPoint? = returnPoint

    @Synchronized
    fun onForegroundAppChanged(packageName: String, applicationLabel: String): SessionDecision {
        val previousPackage = currentForegroundPackage
        if (previousPackage != null && previousPackage != packageName) stopForegroundUsage(previousPackage)
        val current = sessions[packageName]
        if (current?.packageName == packageName) {
            currentForegroundPackage = packageName
            val resumed = if (current.timerState == TimerState.RUNNING) startForegroundUsage(packageName) else current
            return SessionDecision(
                resumed,
                resumed.timerState == TimerState.INPUT_REQUIRED,
                resumed.timerState == TimerState.EXPIRED_PENDING,
                resumed.timerState == TimerState.EXTENSION_INPUT,
                false,
            )
        }

        currentForegroundPackage = packageName
        val draft = ActiveSession(
            sessionId = "draft:$packageName",
            packageName = packageName,
            applicationLabel = applicationLabel,
        )
        sessions[packageName] = draft
        return SessionDecision(draft, true, false, false, false)
    }

    @Synchronized
    fun startSession(draft: SessionDraft): ActiveSession {
        sessions[draft.packageName]?.takeIf { it.timerState != TimerState.INPUT_REQUIRED }?.let { return it }
        val now = clock.now()
        return ActiveSession(
            sessionId = UUID.randomUUID().toString(), packageName = draft.packageName,
            applicationLabel = draft.applicationLabel, purposeCategoryId = draft.purposeCategoryId,
            purposeItemId = draft.purposeItemId, purposeText = draft.purposeText,
            timerDurationMillis = draft.durationMillis, timerStartedElapsedRealtime = now,
            timerDeadlineElapsedRealtime = now + draft.durationMillis,
            currentActivityCategoryId = draft.currentActivityCategoryId,
            currentActivityCategoryName = draft.currentActivityCategoryName,
            currentActivityDetail = draft.currentActivityDetail, timerState = TimerState.RUNNING,
            nextActionDetail = draft.nextActionDetail,
            isForeground = currentForegroundPackage == draft.packageName,
            foregroundStartedElapsedRealtime = now.takeIf { currentForegroundPackage == draft.packageName },
        ).also {
            sessions[it.packageName] = it
            returnPoint = ReturnPoint(draft.currentActivityCategoryId, draft.currentActivityCategoryName, draft.currentActivityDetail.orEmpty())
        }
    }

    private fun startForegroundUsage(packageName: String): ActiveSession {
        val current = requireNotNull(sessions[packageName])
        if (current.isForeground || current.timerState != TimerState.RUNNING) return current
        val now = clock.now()
        return current.copy(
            isForeground = true, foregroundStartedElapsedRealtime = now,
        ).also { sessions[packageName] = it }
    }

    fun stopForegroundUsage(packageName: String? = currentForegroundPackage): ActiveSession? {
        val current = packageName?.let(sessions::get) ?: return null
        if (!current.isForeground || packageName == null) return current
        val delta = current.foregroundStartedElapsedRealtime?.let { (clock.now() - it).coerceAtLeast(0L) } ?: 0L
        return current.copy(
            actualForegroundUsageMillis = current.actualForegroundUsageMillis + delta,
            segmentActualUsageMillis = current.segmentActualUsageMillis + delta,
            foregroundStartedElapsedRealtime = null, isForeground = false,
        ).also { sessions[it.packageName] = it }
    }

    fun pauseForegroundSession(): ActiveSession? { val result = stopForegroundUsage(); currentForegroundPackage = null; return result }
    fun suspendForegroundUsage(): ActiveSession? = stopForegroundUsage()
    fun resumeForegroundUsage(): ActiveSession? = currentForegroundPackage?.let(::startForegroundUsage)

    @Synchronized
    fun expireDueSessions(): List<ActiveSession> {
        return sessions.values
            .filter { it.timerState == TimerState.RUNNING && clock.now() >= it.timerDeadlineElapsedRealtime }
            .mapNotNull { expireSessionById(it.sessionId) }
    }
    @Synchronized
    fun expireSessionById(sessionId: String): ActiveSession? {
        val current = sessionById(sessionId) ?: return null
        if (current.timerState != TimerState.RUNNING) return null
        val stopped = if (current.isForeground) stopForegroundUsage(current.packageName) ?: current else current
        return stopped.copy(timerState = TimerState.EXPIRED_PENDING, purposeSessionState = PurposeSessionState.EXPIRED)
            .also { sessions[it.packageName] = it }
    }
    fun expireSession(packageName: String): ActiveSession? = sessionFor(packageName)?.let { expireSessionById(it.sessionId) }
    fun markExpiredPresented(sessionId: String) = updateState(sessionId, TimerState.EXPIRED_PENDING, TimerState.EXPIRED_PRESENTED)
    fun markExpiredPending(sessionId: String): ActiveSession? = sessionById(sessionId)?.copy(timerState = TimerState.EXPIRED_PENDING)?.also { sessions[it.packageName] = it }
    fun setExtensionInput(packageName: String): ActiveSession? = sessionFor(packageName)?.copy(timerState = TimerState.EXTENSION_INPUT)?.also { sessions[it.packageName] = it }

    @Synchronized
    fun extendSession(draft: SessionDraft): ActiveSession? {
        val current = sessions[draft.packageName] ?: return null
        if (current.timerState !in setOf(TimerState.EXTENSION_INPUT, TimerState.EXPIRED_PRESENTED, TimerState.EXPIRED_PENDING)) return current
        val now = clock.now()
        return current.copy(
            purposeCategoryId = draft.purposeCategoryId,
            purposeItemId = draft.purposeItemId,
            purposeText = draft.purposeText,
            timerDurationMillis = current.timerDurationMillis + draft.durationMillis,
            timerDeadlineElapsedRealtime = now + draft.durationMillis,
            timerState = TimerState.RUNNING, purposeSessionState = PurposeSessionState.ACTIVE,
            extensionCount = current.extensionCount + 1,
            isForeground = currentForegroundPackage == current.packageName,
            foregroundStartedElapsedRealtime = now.takeIf { currentForegroundPackage == current.packageName },
        ).also { sessions[it.packageName] = it }
    }
    fun extendSession(packageName: String, purposeText: String, durationMillis: Long): ActiveSession? =
        sessionFor(packageName)?.let { extendSession(SessionDraft(it.packageName, it.applicationLabel, it.purposeCategoryId, it.purposeItemId, it.purposeText, durationMillis, it.currentActivityCategoryId, it.currentActivityCategoryName, it.currentActivityDetail)) }

    fun completeSession(packageName: String? = currentForegroundPackage): ActiveSession? = finish(packageName, PurposeSessionState.COMPLETED)
    fun abandonSession(packageName: String? = currentForegroundPackage): ActiveSession? = finish(packageName, PurposeSessionState.ABANDONED)
    private fun finish(packageName: String?, state: PurposeSessionState): ActiveSession? {
        packageName ?: return null
        val current = stopForegroundUsage(packageName) ?: sessions[packageName] ?: return null
        sessions.remove(packageName)
        if (currentForegroundPackage == packageName) currentForegroundPackage = null
        return current.copy(timerState = TimerState.CLOSED, purposeSessionState = state, isForeground = false, foregroundStartedElapsedRealtime = null)
    }
    fun closeSession(packageName: String): ActiveSession? = if (sessionFor(packageName) != null) abandonSession(packageName) else null
    fun clearReturnPoint() { returnPoint = null }
    fun reset() { sessions.clear(); currentForegroundPackage = null; returnPoint = null }

    fun remainingMillis(session: ActiveSession): Long =
        if (session.timerState == TimerState.RUNNING) {
            (session.timerDeadlineElapsedRealtime - clock.now()).coerceAtLeast(0L)
        } else {
            (session.timerDurationMillis - elapsedMillis(session)).coerceAtLeast(0L)
        }
    fun elapsedMillis(session: ActiveSession): Long {
        val current = sessionById(session.sessionId) ?: session
        val delta = current.foregroundStartedElapsedRealtime?.let { (clock.now() - it).coerceAtLeast(0L) } ?: 0L
        return current.segmentActualUsageMillis + delta
    }
    fun totalActualUsageMillis(session: ActiveSession): Long = elapsedMillis(session)
    fun nearestRunningDeadline(): Long? = sessions.values
        .filter { it.timerState == TimerState.RUNNING }
        .minOfOrNull { it.timerDeadlineElapsedRealtime }
    private fun updateState(sessionId: String, from: TimerState, to: TimerState): ActiveSession? {
        val current = sessionById(sessionId) ?: return null
        if (current.timerState != from) return current
        return current.copy(timerState = to).also { sessions[it.packageName] = it }
    }
}
