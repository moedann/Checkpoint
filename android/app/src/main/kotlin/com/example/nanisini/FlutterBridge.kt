package com.example.nanisini

import android.app.Activity
import android.Manifest
import android.os.Build
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import java.io.ByteArrayOutputStream
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class FlutterBridge(
    private val activity: Activity,
    private val repository: NativeSettingsRepository,
) : MethodChannel.MethodCallHandler {
    private val historyRepository = UsageHistoryRepository.create(activity.applicationContext)
    // App enumeration and icon PNG conversion are expensive. Reuse the result
    // instead of rebuilding and retransferring it on every activity resume.
    private val installedTargetAppsCache: List<Map<String, Any?>> by lazy(LazyThreadSafetyMode.NONE) {
        loadInstalledTargetApps()
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        try {
            when (call.method) {
                "getAppState" -> result.success(repository.loadState(permissionChecker(), AppDetectionAccessibilityService.isRunning, repository.getLastDetectedApp(), repository.getExclusionStatus(), AppDetectionAccessibilityService::class.java.name).toMap() + mapOf(
                    "targetApps" to installedTargetAppsCache.map { app ->
                        val packageName = app["packageName"] as String
                        app + ("enabled" to (packageName in repository.getEnabledTargetPackages()))
                    },
                    "activeSession" to AppDetectionAccessibilityService.current()?.activeSessionSummary(),
                ))
                "getMonthlyDashboard" -> {
                    val now = java.time.YearMonth.now()
                    val year = call.argument<Number>("year")?.toInt() ?: now.year
                    val month = call.argument<Number>("month")?.toInt() ?: now.monthValue
                    historyRepository.loadDashboard(year, month) { dashboard ->
                        activity.runOnUiThread {
                            dashboard.fold(
                                onSuccess = { result.success(it.toMap()) },
                                onFailure = { result.error("dashboard_error", it.message, null) },
                            )
                        }
                    }
                }
                "getTodayDashboard" -> historyRepository.loadTodayDashboard { dashboard ->
                    activity.runOnUiThread {
                        dashboard.fold(
                            onSuccess = { result.success(it.toMap()) },
                            onFailure = { result.error("today_error", it.message, null) },
                        )
                    }
                }
                "deleteUsageHistory" -> historyRepository.deleteHistory {
                    activity.runOnUiThread { result.success(null) }
                }
                "deleteAllData" -> historyRepository.deleteHistory {
                    repository.clearAllSettings()
                    activity.runOnUiThread { result.success(null) }
                }
                "acceptIntro" -> {
                    repository.markIntroAccepted()
                    result.success(null)
                }
                "openAccessibilitySettings" -> {
                    // Pixel builds protect ACCESSIBILITY_DETAILS_SETTINGS with the
                    // signature-only OPEN_ACCESSIBILITY_DETAILS_SETTINGS permission.
                    // Third-party apps must open the public accessibility list.
                    activity.startActivity(
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                    result.success(null)
                }
                "openOverlaySettings" -> {
                    activity.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${activity.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    result.success(null)
                }
                "requestNotificationPermission" -> {
                    if (Build.VERSION.SDK_INT >= 33) {
                        activity.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST)
                    }
                    result.success(null)
                }
                "openNotificationSettings" -> {
                    activity.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
                    })
                    result.success(null)
                }
                "pauseForMinutes" -> {
                    val minutes = (call.argument<Number>("minutes")?.toInt() ?: 15).coerceAtLeast(1)
                    repository.pauseForMinutes(minutes)
                    result.success(null)
                }
                "pauseUntilNextAppOpen" -> {
                    repository.pauseUntilNextAppOpen()
                    result.success(null)
                }
                "resumeFeature" -> {
                    repository.resumeFeature()
                    result.success(null)
                }
                "setCheckpointEnabled" -> { repository.setCheckpointEnabled(call.argument<Boolean>("enabled") ?: true); result.success(null) }
                "setReviewCaptchaEnabled" -> { repository.setReviewCaptchaEnabled(call.argument<Boolean>("enabled") ?: true); result.success(null) }
                "setEnabledTargetPackages" -> {
                    val packages = call.argument<List<String>>("packageNames").orEmpty().toSet()
                    val installedPackages = installedTargetAppsCache
                        .mapNotNullTo(hashSetOf()) { it["packageName"] as? String }
                    // The UI intentionally omits uninstalled candidates. Preserve their
                    // existing defaults/settings when the user edits visible switches.
                    val enabled = (repository.getEnabledTargetPackages() - installedPackages) +
                        packages.intersect(installedPackages)
                    repository.setEnabledTargetPackages(enabled)
                    AppDetectionAccessibilityService.current()?.applyEnabledTargetApps(enabled)
                    result.success(null)
                }
                "runOverlayTest" -> {
                    AppDetectionAccessibilityService.current()?.showDiagnosticOverlay()
                    result.success(null)
                }
                "addPurposeItem" -> {
                    val categoryId = call.argument<String>("categoryId").orEmpty()
                    val label = call.argument<String>("label").orEmpty()
                    result.success(repository.addPurposeItem(categoryId, label))
                }
                "updatePurposeItem" -> {
                    val item = call.arguments as? Map<String, Any?>
                    result.success(repository.updatePurposeItem(item?.toPurposeItemModel() ?: error("missing item")))
                }
                "deletePurposeItem" -> {
                    result.success(repository.deletePurposeItem(call.argument<String>("id").orEmpty()))
                }
                "addActivityCategory" -> {
                    result.success(repository.addActivityCategory(call.argument<String>("name").orEmpty()))
                }
                "updateActivityCategory" -> {
                    val item = call.arguments as? Map<String, Any?>
                    result.success(repository.updateActivityCategory(item?.toActivityCategoryModel() ?: error("missing item")))
                }
                "deleteActivityCategory" -> {
                    result.success(repository.deleteActivityCategory(call.argument<String>("id").orEmpty()))
                }
                "addReviewQuestion" -> result.success(
                    repository.addReviewQuestion(call.argument<String>("bookId").orEmpty(), call.argument<String>("question").orEmpty(), call.argument<String>("answer").orEmpty()) != null,
                )
                "addReviewQuestions" -> {
                    val items = call.argument<List<Map<String, Any?>>>("items").orEmpty().map {
                        it["question"].toString() to it["answer"].toString()
                    }
                    result.success(repository.addReviewQuestions(call.argument<String>("bookId").orEmpty(), items))
                }
                "updateReviewQuestion" -> {
                    val item = call.arguments as? Map<String, Any?> ?: error("missing item")
                    result.success(repository.updateReviewQuestion(item.toReviewQuestionModel()))
                }
                "deleteReviewQuestion" -> result.success(repository.deleteReviewQuestion(call.argument<String>("id").orEmpty()))
                "addQuestionBook" -> result.success(repository.addQuestionBook(call.argument<String>("title").orEmpty()) != null)
                "updateQuestionBook" -> result.success(repository.updateQuestionBook(call.argument<String>("id").orEmpty(), call.argument<String>("title").orEmpty()))
                "deleteQuestionBook" -> result.success(repository.deleteQuestionBook(call.argument<String>("id").orEmpty()))
                "setSelectedQuestionBookIds" -> { repository.setSelectedQuestionBookIds(call.argument<List<String>>("ids").orEmpty().toSet()); result.success(null) }
                "setReviewQuestionCount" -> { repository.setReviewQuestionCount(call.argument<Number>("count")?.toInt() ?: 1); result.success(null) }
                else -> result.notImplemented()
            }
        } catch (exception: Exception) {
            result.error("bridge_error", exception.message, null)
        }
    }

    private fun permissionChecker(): PermissionChecker = PermissionChecker(activity.applicationContext)

    private fun loadInstalledTargetApps(): List<Map<String, Any?>> {
        val packageManager = activity.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val installedByPackage = packageManager.queryIntentActivities(intent, 0)
            .associateBy { it.activityInfo.packageName }
        return SupportedTargetApps.all.asSequence()
            .mapNotNull { target -> installedByPackage[target.packageName]?.let { target to it } }
            .map {
                mapOf<String, Any?>(
                    "packageName" to it.first.packageName,
                    "label" to it.second.loadLabel(packageManager).toString().ifBlank { it.first.displayName },
                    "icon" to drawableToPng(it.second.loadIcon(packageManager)),
                )
            }
            .sortedBy { (it["label"] as String).lowercase() }
            .toList()
    }

    private fun drawableToPng(drawable: Drawable): ByteArray {
        // The Flutter list displays these at 38dp; 48dp retains headroom while
        // reducing bitmap allocation, PNG encoding, and platform-channel IO.
        val size = (48 * activity.resources.displayMetrics.density).toInt().coerceAtLeast(48)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        return ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            bitmap.recycle()
            output.toByteArray()
        }
    }

    private fun AppStateSnapshot.toMap(): Map<String, Any?> {
        return mapOf(
            "introAccepted" to introAccepted,
            "checkpointEnabled" to checkpointEnabled,
            "reviewCaptchaEnabled" to reviewCaptchaEnabled,
            "versionName" to versionName,
            "permissionSummary" to permissionSummary.toMap(),
            "pauseSummary" to pauseSummary.toMap(),
            "purposeCategories" to purposeCategories.map { it.toMap() },
            "purposeItems" to purposeItems.map { it.toMap() },
            "activityCategories" to activityCategories.map { it.toMap() },
            "questionBooks" to questionBooks.map { it.toMap() },
            "reviewQuestions" to reviewQuestions.map { it.toMap() },
            "selectedQuestionBookIds" to selectedQuestionBookIds.toList(),
            "reviewQuestionCount" to reviewQuestionCount,
            "privacyConfiguration" to privacyConfiguration.toMap(),
        )
    }

    private fun PermissionSnapshot.toMap(): Map<String, Any?> = mapOf(
        "accessibilityEnabled" to accessibilityEnabled,
        "overlayEnabled" to overlayEnabled,
        "serviceRunning" to serviceRunning,
        "lastDetectedApp" to lastDetectedApp,
        "exclusionStatus" to exclusionStatus,
        "timerNotificationsEnabled" to timerNotificationsEnabled,
        "liveUpdateStatus" to liveUpdateStatus,
    )

    private fun PauseSnapshot.toMap(): Map<String, Any?> = mapOf(
        "mode" to mode.name,
        "untilElapsedRealtime" to untilElapsedRealtime,
        "description" to description,
    )

    private fun PrivacyConfiguration.toMap(): Map<String, Any?> = mapOf("summary" to summary)

    private fun PurposeCategoryModel.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name,
        "displayOrder" to displayOrder,
        "isDefault" to isDefault,
    )

    private fun PurposeItemModel.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "categoryId" to categoryId,
        "label" to label,
        "isDefault" to isDefault,
        "createdAt" to createdAt,
    )

    private fun ActivityCategoryModel.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name,
        "displayOrder" to displayOrder,
        "isDefault" to isDefault,
    )

    private fun ReviewQuestionModel.toMap(): Map<String, Any?> = mapOf(
        "id" to id, "bookId" to bookId, "question" to question, "answer" to answer,
        "createdAt" to createdAt, "updatedAt" to updatedAt, "isEnabled" to isEnabled,
        "answerCount" to answerCount, "correctCount" to correctCount,
    )

    private fun QuestionBookModel.toMap(): Map<String, Any?> = mapOf("id" to id, "title" to title, "createdAt" to createdAt, "updatedAt" to updatedAt)

    private fun Map<String, Any?>.toReviewQuestionModel(): ReviewQuestionModel = ReviewQuestionModel(
        id = this["id"]?.toString().orEmpty(),
        bookId = this["bookId"]?.toString().orEmpty(),
        question = this["question"]?.toString().orEmpty(),
        answer = this["answer"]?.toString().orEmpty(),
        createdAt = (this["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        updatedAt = (this["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        isEnabled = this["isEnabled"] as? Boolean ?: true,
        answerCount = (this["answerCount"] as? Number)?.toInt() ?: 0,
        correctCount = (this["correctCount"] as? Number)?.toInt() ?: 0,
    )

    private fun MonthlyDashboard.toMap(): Map<String, Any?> = mapOf(
        "year" to year,
        "month" to month,
        "totalUsageSeconds" to totalUsageSeconds,
        "launchCount" to launchCount,
        "previousUsageSeconds" to previousUsageSeconds,
        "previousLaunchCount" to previousLaunchCount,
        "daily" to daily.map { mapOf("day" to it.day, "usageSeconds" to it.usageSeconds, "launchCount" to it.launchCount, "purposeCount" to it.purposeCount) },
        "topByUsage" to topByUsage.map { it.toMap() },
        "topByLaunches" to topByLaunches.map { it.toMap() },
        "purposeCategories" to purposeCategories.map { it.toMap() },
        "purposes" to purposes.map { it.toMap() },
        "noPurposeCount" to noPurposeCount,
        "previousNoPurposeCount" to previousNoPurposeCount,
        "noPurposeApps" to noPurposeApps.map { it.toMap() },
        "activityCategories" to activityCategories.map { it.toMap() },
        "activityApps" to activityApps.mapValues { (_, apps) -> apps.map { it.toMap() } },
        "recent" to recent.map {
            mapOf(
                "id" to it.id,
                "startedAt" to it.startedAt,
                "packageName" to it.packageName,
                "applicationLabel" to it.applicationLabel,
                "purposeText" to it.purposeText,
                "actualUsageSeconds" to it.actualUsageSeconds,
            )
        },
        "earliestMonth" to earliestMonth,
    )

    private fun TodayDashboard.toMap(): Map<String, Any?> = mapOf(
        "launchCount" to launchCount,
        "purposeCount" to purposeCount,
        "noPurposeCount" to noPurposeCount,
        "topPurposes" to topPurposes.map { it.toMap() },
        "recent" to recent.map {
            mapOf(
                "id" to it.id,
                "startedAt" to it.startedAt,
                "packageName" to it.packageName,
                "applicationLabel" to it.applicationLabel,
                "purposeText" to it.purposeText,
                "actualUsageSeconds" to it.actualUsageSeconds,
            )
        },
    )

    private fun AppUsage.toMap(): Map<String, Any?> = mapOf(
        "packageName" to packageName,
        "applicationLabel" to applicationLabel,
        "usageSeconds" to usageSeconds,
        "launchCount" to launchCount,
    )

    private fun Breakdown.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "label" to label,
        "usageSeconds" to usageSeconds,
        "launchCount" to launchCount,
    )

    private fun Map<String, Any?>.toPurposeItemModel(): PurposeItemModel {
        return PurposeItemModel(
            id = (this["id"] as? String).orEmpty(),
            categoryId = (this["categoryId"] as? String).orEmpty(),
            label = (this["label"] as? String).orEmpty(),
            isDefault = (this["isDefault"] as? Boolean) ?: false,
            createdAt = (this["createdAt"] as? Number)?.toLong() ?: 0L,
        )
    }

    private fun Map<String, Any?>.toActivityCategoryModel(): ActivityCategoryModel {
        return ActivityCategoryModel(
            id = (this["id"] as? String).orEmpty(),
            name = (this["name"] as? String).orEmpty(),
            displayOrder = (this["displayOrder"] as? Number)?.toInt() ?: 0,
            isDefault = (this["isDefault"] as? Boolean) ?: false,
        )
    }

    private companion object { const val NOTIFICATION_PERMISSION_REQUEST = 7401 }
}
