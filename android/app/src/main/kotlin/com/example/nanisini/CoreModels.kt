package com.example.nanisini

data class PurposeCategoryModel(
    val id: String,
    val name: String,
    val displayOrder: Int,
    val isDefault: Boolean,
)

data class PurposeItemModel(
    val id: String,
    val categoryId: String,
    val label: String,
    val isDefault: Boolean,
    val createdAt: Long,
)

data class ActivityCategoryModel(
    val id: String,
    val name: String,
    val displayOrder: Int,
    val isDefault: Boolean,
)

data class ReviewQuestionModel(
    val id: String,
    val bookId: String,
    val question: String,
    val answer: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isEnabled: Boolean,
    val answerCount: Int = 0,
    val correctCount: Int = 0,
)

data class QuestionBookModel(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
)

data class PermissionSnapshot(
    val accessibilityEnabled: Boolean,
    val overlayEnabled: Boolean,
    val serviceRunning: Boolean,
    val lastDetectedApp: String,
    val exclusionStatus: String,
    val timerNotificationsEnabled: Boolean,
    val liveUpdateStatus: String,
)

data class PauseSnapshot(
    val mode: PauseMode,
    val untilElapsedRealtime: Long?,
    val description: String,
)

enum class PauseMode {
    NONE,
    TEMPORARY,
    UNTIL_NEXT_OPEN,
}

data class PrivacyConfiguration(
    val summary: String,
)

data class AppStateSnapshot(
    val introAccepted: Boolean,
    val checkpointEnabled: Boolean = true,
    val reviewCaptchaEnabled: Boolean = true,
    val versionName: String = "1.0.0",
    val permissionSummary: PermissionSnapshot,
    val pauseSummary: PauseSnapshot,
    val purposeCategories: List<PurposeCategoryModel>,
    val purposeItems: List<PurposeItemModel>,
    val activityCategories: List<ActivityCategoryModel>,
    val questionBooks: List<QuestionBookModel>,
    val reviewQuestions: List<ReviewQuestionModel>,
    val selectedQuestionBookIds: Set<String>,
    val reviewQuestionCount: Int,
    val privacyConfiguration: PrivacyConfiguration,
)

data class SystemStateSnapshot(
    val isKeyguardLocked: Boolean,
    val isScreenOn: Boolean,
    val isCallActive: Boolean,
    val isImeForeground: Boolean,
)

data class ForegroundCandidate(
    val packageName: String?,
    val className: String?,
    val eventType: Int,
    val windowId: Int,
    val windowChanges: Int,
)

enum class TimerState {
    INPUT_REQUIRED,
    RUNNING,
    EXPIRED_PENDING,
    EXPIRED_PRESENTED,
    EXTENSION_INPUT,
    CLOSED,
}

enum class PurposeSessionState { ACTIVE, EXPIRED, COMPLETED, ABANDONED }

data class ReturnPoint(
    val categoryId: String?,
    val category: String?,
    val detail: String,
)

data class ActiveSession(
    val sessionId: String = "draft",
    val packageName: String,
    val applicationLabel: String,
    val purposeCategoryId: String? = null,
    val purposeItemId: String? = null,
    val purposeText: String? = null,
    val timerStartedElapsedRealtime: Long = 0L,
    val timerDurationMillis: Long = 0L,
    val timerDeadlineElapsedRealtime: Long = 0L,
    val actualForegroundUsageMillis: Long = 0L,
    val segmentActualUsageMillis: Long = 0L,
    val currentActivityCategoryId: String? = null,
    val currentActivityCategoryName: String? = null,
    val currentActivityDetail: String? = null,
    val timerState: TimerState = TimerState.INPUT_REQUIRED,
    val isForeground: Boolean = false,
    val foregroundStartedElapsedRealtime: Long? = null,
    val extensionCount: Int = 0,
    val purposeSessionState: PurposeSessionState = PurposeSessionState.ACTIVE,
    val nextActionDetail: String? = null,
)

val ActiveSession.durationMillis: Long get() = timerDurationMillis

data class SessionDraft(
    var packageName: String = "",
    var applicationLabel: String = "",
    var purposeCategoryId: String? = null,
    var purposeItemId: String? = null,
    var purposeText: String? = null,
    var durationMillis: Long = 0L,
    var currentActivityCategoryId: String? = null,
    var currentActivityCategoryName: String? = null,
    var currentActivityDetail: String? = null,
    var nextActionDetail: String? = null,
)

data class ExclusionDecision(
    val excluded: Boolean,
    val reason: String,
)

data class SessionDecision(
    val activeSession: ActiveSession?,
    val shouldShowStartOverlay: Boolean,
    val shouldShowExpirationOverlay: Boolean,
    val shouldShowExtensionOverlay: Boolean,
    val shouldHideAll: Boolean,
    val replacedSession: ActiveSession? = null,
)
