package com.example.nanisini

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class AppDetectionAccessibilityService : AccessibilityService() {
    private lateinit var repository: NativeSettingsRepository
    private lateinit var systemStateResolver: SystemStateResolver
    private lateinit var exclusionResolver: ExclusionResolver
    private lateinit var targetAppResolver: TargetAppResolver
    private lateinit var appInfoResolver: AppInfoResolver
    private lateinit var overlayController: OverlayController
    private lateinit var sessionManager: SessionManager
    private lateinit var debouncer: ForegroundAppDebouncer
    private lateinit var historyCoordinator: SessionHistoryCoordinator
    private lateinit var audioFocusController: ModalAudioFocusController
    private lateinit var scheduler: DeadlineScheduler
    private lateinit var timerNotifications: TimerNotificationController
    private val reviewQuestionSelector = ReviewQuestionSelector()
    private val coordinator = OverlayCoordinator()
    private val handler = Handler(Looper.getMainLooper())
    private var safeToShow = true
    private var startProcessing = false
    private var returnOverlaySession: ActiveSession? = null
    private var overlayBackSystemUiGraceUntil = 0L
    private var lastReviewQuestionId: String? = null
    private var runningCheckSystemUiGraceUntil = 0L
    private val sessionReviewQuestions = linkedMapOf<String, List<ReviewQuestionModel>>()
    private val replayedReviewSessions = hashSetOf<String>()

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> enterSafetyState()
                Intent.ACTION_USER_PRESENT, Intent.ACTION_SCREEN_ON -> leaveSafetyState()
                Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> syncTimerNotifications()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        repository = NativeSettingsRepository(applicationContext)
        targetAppResolver = TargetAppResolver(repository)
        systemStateResolver = SystemStateResolver(applicationContext)
        val intelligence = AndroidPackageIntelligence(applicationContext)
        appInfoResolver = AppInfoResolver(applicationContext)
        overlayController = OverlayController(applicationContext) {
            // Predictive Back is dispatched by System UI. Its accessibility event can
            // arrive just after our overlay consumes Back and must not suspend/rebuild it.
            overlayBackSystemUiGraceUntil = SystemElapsedClock.now() + 1_000L
        }
        timerNotifications = TimerNotificationController(applicationContext)
        sessionManager = SessionManager(SystemElapsedClock)
        historyCoordinator = SessionHistoryCoordinator(UsageHistoryRepository.create(applicationContext))
        audioFocusController = ModalAudioFocusController(applicationContext)
        debouncer = ForegroundAppDebouncer()
        exclusionResolver = ExclusionResolver(
            packageName, intelligence.homePackages(), intelligence.settingsPackages(), intelligence.cameraPackages(),
            intelligence.calculatorPackages(), intelligence.dialerPackages() + listOfNotNull(intelligence.dialerRolePackage()),
            intelligence.alarmPackages(), intelligence.systemUiPackages(), intelligence.permissionControllerPackage(),
            intelligence.defaultInputMethodPackage(),
            intelligence.installerPackages(),
            intelligence.defaultExcludedPackages(),
        )
        scheduler = DeadlineScheduler(SystemElapsedClock, sessionManager::nearestRunningDeadline, ::onDeadlinesReached)
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_TIME_CHANGED); addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(screenReceiver, filter, RECEIVER_NOT_EXPORTED) else @Suppress("DEPRECATION") registerReceiver(screenReceiver, filter)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val eventPackage = event.packageName?.toString()?.takeIf { it.isNotBlank() } ?: return
        debouncer.submit(ForegroundCandidate(eventPackage, event.className?.toString(), event.eventType, event.windowId, if (Build.VERSION.SDK_INT >= 28) event.windowChanges else 0), ::handleForegroundCandidate)
    }
    override fun onInterrupt() = Unit

    fun applyEnabledTargetApps(packageNames: Set<String>) {
        if (!::sessionManager.isInitialized) return
        sessionManager.snapshot().keys.filterNot(packageNames::contains).forEach { packageName ->
            val session = sessionManager.sessionFor(packageName) ?: return@forEach
            if (::historyCoordinator.isInitialized && historyCoordinator.hasRecord(packageName)) {
                historyCoordinator.checkpoint(session, sessionManager.elapsedMillis(session))
            }
            sessionManager.closeSession(packageName)?.let {
                if (::timerNotifications.isInitialized) timerNotifications.cancel(it.sessionId)
                coordinator.removeSession(it.sessionId)
            }
        }
        if (::overlayController.isInitialized && coordinator.currentModal == ModalOverlay.NONE) {
            overlayController.dismissAll()
        }
        if (::scheduler.isInitialized) scheduler.schedule()
    }

    override fun onDestroy() {
        if (::scheduler.isInitialized) scheduler.stop()
        runCatching { unregisterReceiver(screenReceiver) }
        if (::sessionManager.isInitialized && ::historyCoordinator.isInitialized) {
            sessionManager.pauseForegroundSession()?.let { historyCoordinator.checkpoint(it, sessionManager.elapsedMillis(it)) }
            sessionManager.snapshot().values.forEach { if (historyCoordinator.hasRecord(it.packageName)) historyCoordinator.checkpoint(it, sessionManager.elapsedMillis(it)) }
        }
        if (::debouncer.isInitialized) debouncer.clear()
        if (::overlayController.isInitialized) overlayController.dismissAll()
        if (::audioFocusController.isInitialized) audioFocusController.release()
        // The in-memory sessions cease to be authoritative when the service dies.
        if (::timerNotifications.isInitialized) timerNotifications.cancelAll()
        instance = null
        super.onDestroy()
    }

    private fun handleForegroundCandidate(candidate: ForegroundCandidate) {
        val packageName = candidate.packageName ?: return
        if (!repository.isCheckpointEnabled()) {
            if (::sessionManager.isInitialized) sessionManager.reset()
            if (::timerNotifications.isInitialized) timerNotifications.cancelAll()
            enterSafetyState(); return
        }
        if (repository.readPauseSummary().mode != PauseMode.NONE) {
            if (::sessionManager.isInitialized) sessionManager.reset()
            if (::timerNotifications.isInitialized) timerNotifications.cancelAll()
            enterSafetyState(); return
        }
        val state = systemStateResolver.snapshot()
        val exclusion = exclusionResolver.resolve(candidate, state)
        val info = appInfoResolver.resolve(candidate.packageName)
        repository.setLastDetectedApp(info.label)
        val isTargetApp = targetAppResolver.isTargetApp(packageName)
        repository.setExclusionStatus(
            when {
                exclusion.excluded -> exclusion.reason
                !isTargetApp -> "Checkpoint対象外"
                else -> "Checkpoint対象"
            },
        )

        val criticalSafety = !state.isScreenOn || state.isKeyguardLocked || state.isCallActive ||
            exclusion.reason in setOf("permission controller", "dialer", "alarm")
        if (criticalSafety) { enterSafetyState(); return }
        if (exclusion.reason == "system ui") {
            if (SystemElapsedClock.now() < overlayBackSystemUiGraceUntil) return
            val closingNotificationShade =
                SystemElapsedClock.now() < runningCheckSystemUiGraceUntil &&
                    overlayController.isRunningCheckOverlayShowing()
            if (closingNotificationShade) return
            enterSafetyState()
            return
        }
        safeToShow = true
        // Our own overlay and an IME are transient windows layered over the real
        // foreground app. They must not replace/pause that app's session or timer UI.
        if (exclusion.reason == "self" || exclusion.reason == "ime") return
        if (exclusion.excluded || !isTargetApp) {
            sessionManager.pauseForegroundSession()?.let { historyCoordinator.checkpoint(it, sessionManager.elapsedMillis(it)) }
            scheduler.schedule()
            syncTimerNotifications()
            return
        }

        val previous = sessionManager.foregroundPackage()
        val decision = sessionManager.onForegroundAppChanged(packageName, info.label)
        decision.replacedSession?.let { replaced ->
            timerNotifications.cancel(replaced.sessionId)
            if (historyCoordinator.hasRecord(replaced.packageName)) {
                historyCoordinator.sessionFinished(replaced, sessionManager.elapsedMillis(replaced))
            }
            coordinator.removeSession(replaced.sessionId)
        }
        if (previous != null && previous != packageName) sessionManager.sessionFor(previous)?.let { if (historyCoordinator.hasRecord(previous)) historyCoordinator.checkpoint(it, sessionManager.elapsedMillis(it)) }
        scheduler.schedule()
        syncTimerNotifications()
        if (decision.shouldShowStartOverlay && decision.activeSession != null) {
            val sessionId = decision.activeSession.sessionId
            val sameStartOverlayIsShowing =
                coordinator.currentModal == ModalOverlay.START &&
                    coordinator.currentSessionId == sessionId &&
                    overlayController.isStartOverlayShowing()
            if (!sameStartOverlayIsShowing) {
                showPlan(coordinator.startRequested(sessionId))
            }
        } else {
            if (
                (decision.activeSession?.timerState == TimerState.EXPIRED_PENDING ||
                    decision.activeSession?.timerState == TimerState.EXPIRED_PRESENTED) &&
                !overlayController.isExpirationOverlayShowing()
            ) {
                showPlan(
                    coordinator.expiryRequested(
                        decision.activeSession.sessionId,
                        decision.activeSession.timerDeadlineElapsedRealtime,
                    ),
                )
            } else if (coordinator.currentModal == ModalOverlay.NONE) showPlan(coordinator.resumeAfterSafety())
        }
    }

    private fun onDeadlinesReached() {
        val expiredSessions = sessionManager.expireDueSessions().sortedBy { it.timerDeadlineElapsedRealtime }
        expiredSessions.forEach { expired ->
            timerNotifications.cancelRunning(expired.sessionId)
            timerNotifications.postExpiration(expired)
            if (historyCoordinator.hasRecord(expired.packageName)) historyCoordinator.checkpoint(expired, sessionManager.elapsedMillis(expired))
            coordinator.expiryRequested(expired.sessionId, expired.timerDeadlineElapsedRealtime)
        }
        if (expiredSessions.isNotEmpty()) {
            presentNextExpiredOverlay()
            handler.postDelayed(::presentNextExpiredOverlay, 300L)
            handler.postDelayed(::presentNextExpiredOverlay, 1_000L)
        }
        syncTimerNotifications()
    }

    private fun presentNextExpiredOverlay() {
        if (!safeToShow || overlayController.isExpirationOverlayShowing()) return
        val expired = sessionManager.snapshot().values
            .filter { it.timerState == TimerState.EXPIRED_PENDING || it.timerState == TimerState.EXPIRED_PRESENTED }
            .minByOrNull { it.timerDeadlineElapsedRealtime }
            ?: return
        showPlan(coordinator.expiryRequested(expired.sessionId, expired.timerDeadlineElapsedRealtime))
    }

    private fun showPlan(plan: OverlayPlan) {
        if (!safeToShow) return
        if (plan.modal == ModalOverlay.NONE) {
            audioFocusController.release()
            sessionManager.resumeForegroundUsage()
        } else {
            audioFocusController.acquire()
            sessionManager.suspendForegroundUsage()?.let { if (historyCoordinator.hasRecord(it.packageName)) historyCoordinator.checkpoint(it, sessionManager.elapsedMillis(it)) }
        }
        syncTimerNotifications()
        when (plan.modal) {
            ModalOverlay.NONE -> { overlayController.hideStartOverlay(); overlayController.hideExpirationOverlay(); overlayController.hideExtensionOverlay(); overlayController.hideReturnPointOverlay() }
            ModalOverlay.START -> showStart(plan.sessionId ?: return)
            ModalOverlay.EXPIRED -> showExpired(plan.sessionId ?: return)
            ModalOverlay.EXTENSION -> showExtension(plan.sessionId ?: return)
            ModalOverlay.RETURN_POINT -> showReturnPoint(plan.sessionId ?: return)
        }
    }

    private fun showStart(draftSessionId: String) {
        val session = sessionManager.snapshot().values.firstOrNull { it.sessionId == draftSessionId } ?: return
        overlayController.hideExpirationOverlay(); overlayController.hideExtensionOverlay()
        val shown = overlayController.showStartOverlay(session, appInfoResolver.resolve(session.packageName), repository.getPurposeCategories(), repository.getPurposeItems(), repository.getActivityCategories(), repository.getRecentActivityDetails(), sessionManager.currentReturnPoint(), onStart = { draft ->
            if (startProcessing) return@showStartOverlay
            startProcessing = true
            try {
                repository.recordActivityDetail(draft.currentActivityDetail.orEmpty())
                showCaptchaForStart(draft, draftSessionId)
            } finally { startProcessing = false }
        }, onCancel = {
            sessionManager.closeSession(session.packageName)?.let { timerNotifications.cancel(it.sessionId) }
            performGlobalAction(GLOBAL_ACTION_HOME)
            showPlan(coordinator.modalResolved(draftSessionId))
        })
        if (!shown) { sessionManager.closeSession(session.packageName); showPlan(coordinator.modalResolved(draftSessionId)) }
    }

    private fun nextReviewQuestion(): ReviewQuestionModel? =
        reviewQuestionSelector.select(repository.getEligibleReviewQuestions(), lastReviewQuestionId)?.also {
            lastReviewQuestionId = it.id
        }

    private fun showCaptchaForStart(draft: SessionDraft, draftSessionId: String) {
        val selectedQuestions = selectReviewQuestionsForCheckpoint()
        showQuestionSequence(draft.packageName, draft.purposeText.orEmpty(), questions = selectedQuestions, onComplete = {
            val active = sessionManager.startSession(draft)
            if (selectedQuestions.isEmpty()) sessionReviewQuestions.remove(active.sessionId)
            else sessionReviewQuestions[active.sessionId] = selectedQuestions
            replayedReviewSessions.remove(active.sessionId)
            timerNotifications.cancelExpiration(active.sessionId)
            if (!historyCoordinator.hasRecord(active.packageName)) historyCoordinator.sessionStarted(active)
            showPlan(coordinator.modalResolved(draftSessionId))
            scheduler.schedule()
        }, onFailure = {
            sessionManager.closeSession(draft.packageName)
            coordinator.presentationFailed()
            performGlobalAction(GLOBAL_ACTION_HOME)
        })
    }

    private fun selectReviewQuestionsForCheckpoint(): List<ReviewQuestionModel> =
        if (repository.isReviewCaptchaEnabled()) {
            reviewQuestionSelector.selectMany(repository.getEligibleReviewQuestions(), repository.getReviewQuestionCount())
        } else {
            emptyList()
        }

    private fun showQuestionSequence(
        packageName: String,
        purposeText: String,
        questions: List<ReviewQuestionModel>? = null,
        onComplete: () -> Unit,
        onFailure: () -> Unit,
    ) {
        if (!repository.isReviewCaptchaEnabled()) { onComplete(); return }
        val selectedQuestions = questions ?: selectReviewQuestionsForCheckpoint()
        fun show(index: Int) {
            if (index >= selectedQuestions.size) { onComplete(); return }
            val shown = overlayController.showReviewCaptchaOverlay(appInfoResolver.resolve(packageName), purposeText, selectedQuestions[index], repository::recordReviewAnswer) { show(index + 1) }
            if (!shown) onFailure()
        }
        if (selectedQuestions.isEmpty()) {
            val shown = overlayController.showReviewCaptchaOverlay(appInfoResolver.resolve(packageName), purposeText, null, repository::recordReviewAnswer, onComplete)
            if (!shown) onFailure()
        } else show(0)
    }

    private fun showExpired(sessionId: String) {
        val session = sessionManager.sessionById(sessionId) ?: run { showPlan(coordinator.modalResolved(sessionId)); return }
        val originalQuestions = sessionReviewQuestions[sessionId].orEmpty()
        if (originalQuestions.isNotEmpty() && sessionId !in replayedReviewSessions) {
            overlayController.hideRunningCheckOverlay()
            showQuestionSequence(
                packageName = session.packageName,
                purposeText = session.purposeText.orEmpty(),
                questions = originalQuestions,
                onComplete = {
                    replayedReviewSessions += sessionId
                    showExpired(sessionId)
                },
                onFailure = {
                    sessionManager.markExpiredPending(sessionId)
                    coordinator.presentationFailed()
                },
            )
            return
        }
        overlayController.hideStartOverlay(); overlayController.hideRunningCheckOverlay(); overlayController.hideExtensionOverlay()
        fun closeAndReturnHome() {
            overlayController.dismissAll()
            val closed = sessionManager.completeSession(session.packageName)
            timerNotifications.cancel(sessionId); syncTimerNotifications()
            closed?.let { historyCoordinator.sessionFinished(it, sessionManager.elapsedMillis(it)) }
            coordinator.removeSession(sessionId)
            sessionReviewQuestions.remove(sessionId)
            replayedReviewSessions.remove(sessionId)
            performGlobalAction(GLOBAL_ACTION_HOME)
            showPlan(coordinator.modalResolved(sessionId)); scheduler.schedule()
        }
        val shown = overlayController.showExpirationOverlay(session, appInfoResolver.resolve(session.packageName), onReturn = {
            closeAndReturnHome()
        }, onExtend = {
            sessionManager.setExtensionInput(session.packageName)
            overlayController.hideExpirationOverlay()
            showPlan(coordinator.extensionRequested(sessionId))
        })
        if (shown) sessionManager.markExpiredPresented(sessionId) else { sessionManager.markExpiredPending(sessionId); coordinator.presentationFailed() }
    }

    private fun showReturnPoint(sessionId: String) {
        val finished = returnOverlaySession ?: return
        val shown = overlayController.showReturnPointOverlay(
            ReturnPoint(finished.currentActivityCategoryId, finished.currentActivityCategoryName, finished.currentActivityDetail.orEmpty()),
            finished.purposeSessionState == PurposeSessionState.COMPLETED,
        ) {
            returnOverlaySession = null
            sessionManager.clearReturnPoint()
            overlayController.dismissAll()
            coordinator.removeSession(sessionId)
            performGlobalAction(GLOBAL_ACTION_HOME)
            showPlan(coordinator.modalResolved(sessionId))
        }
        if (!shown) coordinator.presentationFailed()
    }

    private fun showExtension(sessionId: String) {
        val session = sessionManager.sessionById(sessionId) ?: return
        overlayController.hideExpirationOverlay()
        val shown = overlayController.showExtensionOverlay(
            session,
            appInfoResolver.resolve(session.packageName),
            repository.getPurposeCategories(),
            repository.getPurposeItems(),
            onExtend = { draft ->
                val extensionQuestions = selectReviewQuestionsForCheckpoint()
                showQuestionSequence(session.packageName, session.purposeText.orEmpty(), questions = extensionQuestions, onComplete = {
                    historyCoordinator.extensionStarted(sessionManager.elapsedMillis(session), draft)
                    sessionManager.extendSession(draft)
                    if (extensionQuestions.isEmpty()) sessionReviewQuestions.remove(sessionId)
                    else sessionReviewQuestions[sessionId] = extensionQuestions
                    replayedReviewSessions.remove(sessionId)
                    timerNotifications.cancelExpiration(sessionId)
                    coordinator.removeSession(sessionId)
                    showPlan(coordinator.modalResolved(sessionId)); scheduler.schedule()
                }, onFailure = {
                    sessionManager.markExpiredPending(sessionId)
                    coordinator.presentationFailed()
                })
            },
            onClose = {
                val wasForeground = sessionManager.foregroundPackage() == session.packageName
                overlayController.dismissAll()
                val closed = sessionManager.closeSession(session.packageName) ?: session
                timerNotifications.cancel(closed.sessionId)
                sessionReviewQuestions.remove(sessionId)
                replayedReviewSessions.remove(sessionId)
                syncTimerNotifications()
                historyCoordinator.sessionFinished(closed, sessionManager.elapsedMillis(closed))
                coordinator.removeSession(sessionId)
                if (wasForeground) performGlobalAction(GLOBAL_ACTION_HOME)
                showPlan(coordinator.modalResolved(sessionId)); scheduler.schedule()
            },
        )
        if (!shown) { sessionManager.markExpiredPending(sessionId); coordinator.presentationFailed() }
    }

    private fun syncTimerNotifications() = timerNotifications.sync(sessionManager.snapshot().values)

    fun activeSessionSummary(): Map<String, Any?>? {
        if (!::sessionManager.isInitialized) return null
        val session = sessionManager.activeSession()?.takeIf { it.timerState == TimerState.RUNNING } ?: return null
        return mapOf(
            "applicationLabel" to session.applicationLabel,
            "purposeText" to session.purposeText.orEmpty(),
            "remainingMillis" to sessionManager.remainingMillis(session),
        )
    }

    fun showRunningSessionCheck(sessionId: String) {
        if (!::sessionManager.isInitialized || !::overlayController.isInitialized) return
        if (Build.VERSION.SDK_INT >= 31) {
            performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
        } else {
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
        handler.postDelayed({
            val session = sessionManager.sessionById(sessionId)
                ?.takeIf { it.timerState == TimerState.RUNNING }
                ?: return@postDelayed
            val shown = overlayController.showRunningCheckOverlay(
                session = session,
                appInfo = appInfoResolver.resolve(session.packageName),
                remainingMillis = sessionManager.remainingMillis(session),
                onNext = {
                    overlayController.hideRunningCheckOverlay()
                    val closed = sessionManager.completeSession(session.packageName)
                    timerNotifications.cancel(sessionId)
                    closed?.let { historyCoordinator.sessionFinished(it, sessionManager.elapsedMillis(it)) }
                    coordinator.removeSession(sessionId)
                    syncTimerNotifications()
                    scheduler.schedule()
                    performGlobalAction(GLOBAL_ACTION_HOME)
                },
                onContinue = {
                    overlayController.hideRunningCheckOverlay()
                    val launchIntent = packageManager.getLaunchIntentForPackage(session.packageName)
                    launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    if (launchIntent != null) startActivity(launchIntent) else performGlobalAction(GLOBAL_ACTION_BACK)
                },
            )
            runningCheckSystemUiGraceUntil = if (shown) SystemElapsedClock.now() + 1_500L else 0L
            if (!shown) {
                val launchIntent = packageManager.getLaunchIntentForPackage(session.packageName)
                launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                if (launchIntent != null) startActivity(launchIntent)
            }
        }, 450L)
    }

    private fun enterSafetyState() {
        safeToShow = false
        sessionManager.pauseForegroundSession()?.let { if (historyCoordinator.hasRecord(it.packageName)) historyCoordinator.checkpoint(it, sessionManager.elapsedMillis(it)) }
        if (::scheduler.isInitialized) scheduler.schedule()
        if (::timerNotifications.isInitialized) syncTimerNotifications()
        coordinator.suspendForSafety()
        audioFocusController.release()
        overlayController.dismissAll()
    }
    private fun leaveSafetyState() {
        val state = systemStateResolver.snapshot()
        if (!state.isScreenOn || state.isKeyguardLocked || state.isCallActive) return
        safeToShow = true
        onDeadlinesReached()
        showPlan(coordinator.resumeAfterSafety())
        presentNextExpiredOverlay()
    }

    fun showDiagnosticOverlay() = Unit

    companion object {
        @Volatile var instance: AppDetectionAccessibilityService? = null; private set
        val isRunning get() = instance != null
        fun current(): AppDetectionAccessibilityService? = instance
    }
}
