package com.example.nanisini

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class NativeSettingsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        recordVersionInfo()
        seedQuestionBooksIfNeeded()
    }

    fun loadState(
        permissionChecker: PermissionChecker,
        serviceRunning: Boolean,
        lastDetectedApp: String,
        exclusionStatus: String,
        serviceClassName: String,
    ): AppStateSnapshot {
        val introAccepted = prefs.getBoolean(KEY_INTRO_ACCEPTED, false)
        val pauseSummary = readPauseSummary(SystemElapsedClock)
        val permissionSummary = permissionChecker.snapshot(serviceRunning, lastDetectedApp, exclusionStatus, serviceClassName)
        return AppStateSnapshot(
            introAccepted = introAccepted,
            checkpointEnabled = isCheckpointEnabled(),
            reviewCaptchaEnabled = isReviewCaptchaEnabled(),
            versionName = prefs.getString(KEY_VERSION_NAME, "1.0.0") ?: "1.0.0",
            permissionSummary = permissionSummary,
            pauseSummary = pauseSummary,
            purposeCategories = getPurposeCategories(),
            purposeItems = getPurposeItems(),
            activityCategories = getActivityCategories(),
            questionBooks = getQuestionBooks(),
            reviewQuestions = getReviewQuestions(),
            selectedQuestionBookIds = getSelectedQuestionBookIds(),
            reviewQuestionCount = getReviewQuestionCount(),
            privacyConfiguration = PrivacyConfiguration(privacySummaryText),
        )
    }

    fun markIntroAccepted() {
        prefs.edit().putBoolean(KEY_INTRO_ACCEPTED, true).apply()
    }

    fun getIntroAccepted(): Boolean = prefs.getBoolean(KEY_INTRO_ACCEPTED, false)

    fun isCheckpointEnabled(): Boolean = prefs.getBoolean(KEY_CHECKPOINT_ENABLED, true)
    fun setCheckpointEnabled(enabled: Boolean) { prefs.edit().putBoolean(KEY_CHECKPOINT_ENABLED, enabled).apply() }
    fun isReviewCaptchaEnabled(): Boolean = prefs.getBoolean(KEY_REVIEW_CAPTCHA_ENABLED, true)
    fun setReviewCaptchaEnabled(enabled: Boolean) { prefs.edit().putBoolean(KEY_REVIEW_CAPTCHA_ENABLED, enabled).apply() }

    fun clearAllSettings() {
        prefs.edit().clear().apply()
        recordVersionInfo()
        seedQuestionBooksIfNeeded()
    }

    fun pauseForMinutes(minutes: Int, clock: ElapsedClock = SystemElapsedClock) {
        val until = clock.now() + minutes.coerceAtLeast(1) * 60_000L
        prefs.edit()
            .putString(KEY_PAUSE_MODE, PauseMode.TEMPORARY.name)
            .putLong(KEY_PAUSE_UNTIL, until)
            .apply()
    }

    fun pauseUntilNextAppOpen() {
        prefs.edit()
            .putString(KEY_PAUSE_MODE, PauseMode.UNTIL_NEXT_OPEN.name)
            .remove(KEY_PAUSE_UNTIL)
            .apply()
    }

    fun resumeFeature() {
        prefs.edit().remove(KEY_PAUSE_MODE).remove(KEY_PAUSE_UNTIL).apply()
    }

    fun readPauseSummary(clock: ElapsedClock = SystemElapsedClock): PauseSnapshot {
        val modeName = prefs.getString(KEY_PAUSE_MODE, null)
        val mode = runCatching { PauseMode.valueOf(modeName ?: PauseMode.NONE.name) }.getOrDefault(PauseMode.NONE)
        val until = if (prefs.contains(KEY_PAUSE_UNTIL)) prefs.getLong(KEY_PAUSE_UNTIL, 0L) else null
        return when (mode) {
            PauseMode.TEMPORARY -> {
                val target = until ?: 0L
                if (target <= 0L || clock.now() >= target) {
                    resumeFeature()
                    PauseSnapshot(PauseMode.NONE, null, "一時停止はありません。")
                } else {
                    val remainingMinutes = ((target - clock.now()) / 60_000L).coerceAtLeast(1L)
                    PauseSnapshot(PauseMode.TEMPORARY, target, "${remainingMinutes}分停止中")
                }
            }

            PauseMode.UNTIL_NEXT_OPEN -> PauseSnapshot(PauseMode.UNTIL_NEXT_OPEN, null, "次のアプリ起動まで停止中")
            PauseMode.NONE -> PauseSnapshot(PauseMode.NONE, null, "一時停止はありません。")
        }
    }

    fun setLastDetectedApp(appLabel: String) {
        prefs.edit().putString(KEY_LAST_DETECTED_APP, appLabel).apply()
    }

    fun getLastDetectedApp(): String = prefs.getString(KEY_LAST_DETECTED_APP, "") ?: ""

    fun setExclusionStatus(status: String) {
        prefs.edit().putString(KEY_EXCLUSION_STATUS, status).apply()
    }

    fun getExclusionStatus(): String = prefs.getString(KEY_EXCLUSION_STATUS, "未確認") ?: "未確認"

    fun getEnabledTargetPackages(): Set<String> {
        if (!prefs.contains(KEY_ENABLED_TARGET_PACKAGES)) {
            return SupportedTargetApps.defaultEnabledPackageNames
        }
        return prefs.getStringSet(KEY_ENABLED_TARGET_PACKAGES, emptySet())
            ?.intersect(SupportedTargetApps.packageNames)
            ?: emptySet()
    }

    fun setEnabledTargetPackages(packageNames: Set<String>) {
        prefs.edit()
            .putStringSet(KEY_ENABLED_TARGET_PACKAGES, packageNames.intersect(SupportedTargetApps.packageNames))
            .apply()
    }

    fun recordActivityDetail(detail: String, now: Long = System.currentTimeMillis()) {
        val trimmed = detail.trim()
        if (trimmed.isBlank()) return
        val entries = getRecentActivityDetailEntries(now)
            .filterNot { it.first.equals(trimmed, ignoreCase = true) }
            .toMutableList()
        entries.add(0, trimmed to now)
        val json = JSONArray()
        entries.take(20).forEach { (text, timestamp) ->
            json.put(JSONObject().put("text", text).put("timestamp", timestamp))
        }
        prefs.edit().putString(KEY_RECENT_ACTIVITY_DETAILS, json.toString()).apply()
    }

    fun getRecentActivityDetails(now: Long = System.currentTimeMillis()): List<String> =
        getRecentActivityDetailEntries(now).map { it.first }

    private fun getRecentActivityDetailEntries(now: Long): List<Pair<String, Long>> {
        val cutoff = now - 24 * 60 * 60 * 1000L
        val raw = prefs.getString(KEY_RECENT_ACTIVITY_DETAILS, null) ?: return emptyList()
        return runCatching {
            val json = JSONArray(raw)
            buildList {
                for (index in 0 until json.length()) {
                    val item = json.getJSONObject(index)
                    val text = item.optString("text").trim()
                    val timestamp = item.optLong("timestamp")
                    if (text.isNotBlank() && timestamp >= cutoff) add(text to timestamp)
                }
            }
        }.getOrDefault(emptyList())
    }

    fun getPurposeCategories(): List<PurposeCategoryModel> {
        return defaultPurposeCategories + readUserPurposeCategories()
    }

    fun getPurposeItems(): List<PurposeItemModel> {
        return defaultPurposeItems + readUserPurposeItems()
    }

    fun addPurposeItem(categoryId: String, label: String): Boolean {
        val trimmed = label.trim()
        if (trimmed.isBlank()) {
            return false
        }
        if (getPurposeCategories().none { it.id == categoryId }) {
            return false
        }
        val existing = getPurposeItems().any { it.categoryId == categoryId && it.label.equals(trimmed, ignoreCase = true) }
        if (existing) {
            return false
        }
        val items = readUserPurposeItems().toMutableList()
        items += PurposeItemModel("user-purpose-${System.nanoTime()}", categoryId, trimmed, false, System.currentTimeMillis())
        writeUserPurposeItems(items)
        return true
    }

    fun updatePurposeItem(item: PurposeItemModel): Boolean {
        if (item.isDefault) {
            return false
        }
        val items = readUserPurposeItems().toMutableList()
        val index = items.indexOfFirst { it.id == item.id }
        if (index < 0) {
            return false
        }
        items[index] = item.copy(label = item.label.trim())
        writeUserPurposeItems(items)
        return true
    }

    fun deletePurposeItem(id: String): Boolean {
        val items = readUserPurposeItems().toMutableList()
        val removed = items.removeAll { it.id == id }
        if (removed) {
            writeUserPurposeItems(items)
        }
        return removed
    }

    fun getActivityCategories(): List<ActivityCategoryModel> {
        return defaultActivityCategories + readUserActivityCategories()
    }

    fun addActivityCategory(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            return false
        }
        if (getActivityCategories().any { it.name.equals(trimmed, ignoreCase = true) }) {
            return false
        }
        val items = readUserActivityCategories().toMutableList()
        items += ActivityCategoryModel("user-activity-${System.nanoTime()}", trimmed, 100 + items.size, false)
        writeUserActivityCategories(items)
        return true
    }

    fun updateActivityCategory(item: ActivityCategoryModel): Boolean {
        if (item.isDefault) {
            return false
        }
        val items = readUserActivityCategories().toMutableList()
        val index = items.indexOfFirst { it.id == item.id }
        if (index < 0) {
            return false
        }
        items[index] = item.copy(name = item.name.trim())
        writeUserActivityCategories(items)
        return true
    }

    fun deleteActivityCategory(id: String): Boolean {
        val items = readUserActivityCategories().toMutableList()
        val removed = items.removeAll { it.id == id }
        if (removed) {
            writeUserActivityCategories(items)
        }
        return removed
    }

    fun hasStoredHistoryKeys(): Boolean {
        return prefs.contains("history") || prefs.contains("session_history") || prefs.contains("usage_history")
    }

    fun getReviewQuestions(): List<ReviewQuestionModel> = runCatching {
        val array = JSONArray(prefs.getString(KEY_REVIEW_QUESTIONS, "[]") ?: "[]")
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    ReviewQuestionModel(
                        id = item.getString("id"),
                        bookId = item.optString("bookId", DEFAULT_BOOK_ID),
                        question = item.getString("question"),
                        answer = item.getString("answer"),
                        createdAt = item.optLong("createdAt", 0L),
                        updatedAt = item.optLong("updatedAt", item.optLong("createdAt", 0L)),
                        isEnabled = item.optBoolean("isEnabled", true),
                        answerCount = item.optInt("answerCount", 0),
                        correctCount = item.optInt("correctCount", 0),
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())

    fun addReviewQuestion(bookId: String, question: String, answer: String): ReviewQuestionModel? {
        val cleanQuestion = question.trim()
        val cleanAnswer = answer.trim()
        if (cleanQuestion.isBlank() || cleanAnswer.isBlank()) return null
        if (getQuestionBooks().none { it.id == bookId }) return null
        val now = System.currentTimeMillis()
        val item = ReviewQuestionModel(UUID.randomUUID().toString(), bookId, cleanQuestion, cleanAnswer, now, now, true)
        writeReviewQuestions(getReviewQuestions() + item)
        return item
    }

    fun addReviewQuestions(bookId: String, items: List<Pair<String, String>>): Int {
        if (getQuestionBooks().none { it.id == bookId }) return 0
        val valid = items.mapNotNull { (question, answer) ->
            val cleanQuestion = question.trim()
            val cleanAnswer = answer.trim()
            if (cleanQuestion.isBlank() || cleanAnswer.isBlank()) null
            else System.currentTimeMillis().let { now -> ReviewQuestionModel(UUID.randomUUID().toString(), bookId, cleanQuestion, cleanAnswer, now, now, true) }
        }
        if (valid.isNotEmpty()) writeReviewQuestions(getReviewQuestions() + valid)
        return valid.size
    }

    fun updateReviewQuestion(item: ReviewQuestionModel): Boolean {
        if (item.question.isBlank() || item.answer.isBlank()) return false
        val items = getReviewQuestions().toMutableList()
        val index = items.indexOfFirst { it.id == item.id }
        if (index < 0) return false
        items[index] = item.copy(question = item.question.trim(), answer = item.answer.trim(), updatedAt = System.currentTimeMillis())
        writeReviewQuestions(items)
        return true
    }

    fun deleteReviewQuestion(id: String): Boolean {
        val items = getReviewQuestions().toMutableList()
        val removed = items.removeAll { it.id == id }
        if (removed) writeReviewQuestions(items)
        return removed
    }

    private fun writeReviewQuestions(items: List<ReviewQuestionModel>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().put("id", item.id).put("question", item.question).put("answer", item.answer)
                .put("bookId", item.bookId).put("createdAt", item.createdAt).put("updatedAt", item.updatedAt)
                .put("isEnabled", item.isEnabled).put("answerCount", item.answerCount).put("correctCount", item.correctCount))
        }
        prefs.edit().putString(KEY_REVIEW_QUESTIONS, array.toString()).apply()
    }

    fun recordReviewAnswer(id: String, correct: Boolean) {
        val items = getReviewQuestions().toMutableList()
        val index = items.indexOfFirst { it.id == id }
        if (index < 0) return
        val item = items[index]
        items[index] = item.copy(answerCount = item.answerCount + 1, correctCount = item.correctCount + if (correct) 1 else 0, updatedAt = System.currentTimeMillis())
        writeReviewQuestions(items)
    }

    fun getQuestionBooks(): List<QuestionBookModel> = runCatching {
        val array = JSONArray(prefs.getString(KEY_QUESTION_BOOKS, "[]") ?: "[]")
        buildList { for (i in 0 until array.length()) { val o = array.getJSONObject(i); add(QuestionBookModel(o.getString("id"), o.getString("title"), o.optLong("createdAt"), o.optLong("updatedAt"))) } }
    }.getOrDefault(emptyList())

    private fun writeQuestionBooks(items: List<QuestionBookModel>) { val a = JSONArray(); items.forEach { a.put(JSONObject().put("id", it.id).put("title", it.title).put("createdAt", it.createdAt).put("updatedAt", it.updatedAt)) }; prefs.edit().putString(KEY_QUESTION_BOOKS, a.toString()).apply() }
    fun addQuestionBook(title: String): QuestionBookModel? { val clean = title.trim(); if (clean.isBlank()) return null; val now = System.currentTimeMillis(); return QuestionBookModel(UUID.randomUUID().toString(), clean, now, now).also { writeQuestionBooks(getQuestionBooks() + it) } }
    fun updateQuestionBook(id: String, title: String): Boolean { val clean = title.trim(); val books = getQuestionBooks().toMutableList(); val i = books.indexOfFirst { it.id == id }; if (i < 0 || clean.isBlank()) return false; books[i] = books[i].copy(title = clean, updatedAt = System.currentTimeMillis()); writeQuestionBooks(books); return true }
    fun deleteQuestionBook(id: String): Boolean { val books = getQuestionBooks().toMutableList(); if (!books.removeAll { it.id == id }) return false; writeQuestionBooks(books); writeReviewQuestions(getReviewQuestions().filterNot { it.bookId == id }); setSelectedQuestionBookIds(getSelectedQuestionBookIds() - id); return true }
    fun getSelectedQuestionBookIds(): Set<String> = prefs.getStringSet(KEY_SELECTED_BOOK_IDS, null)?.toSet() ?: getQuestionBooks().map { it.id }.toSet()
    fun setSelectedQuestionBookIds(ids: Set<String>) { prefs.edit().putStringSet(KEY_SELECTED_BOOK_IDS, ids.intersect(getQuestionBooks().map { it.id }.toSet())).apply() }
    fun getReviewQuestionCount(): Int = prefs.getInt(KEY_REVIEW_COUNT, 1).takeIf { it in setOf(1,2,3,5) } ?: 1
    fun setReviewQuestionCount(count: Int) { if (count in setOf(1,2,3,5)) prefs.edit().putInt(KEY_REVIEW_COUNT, count).apply() }
    fun getEligibleReviewQuestions(): List<ReviewQuestionModel> { val ids = getSelectedQuestionBookIds(); return getReviewQuestions().filter { it.isEnabled && it.bookId in ids } }

    private fun seedQuestionBooksIfNeeded() {
        val now = System.currentTimeMillis()
        if (getQuestionBooks().isEmpty()) {
            writeQuestionBooks(listOf(QuestionBookModel(DEFAULT_BOOK_ID, "初期問題冊子", now, now)))
            val migrated = getReviewQuestions().map { if (it.bookId.isBlank()) it.copy(bookId = DEFAULT_BOOK_ID, updatedAt = now) else it }
            if (migrated.isNotEmpty()) writeReviewQuestions(migrated)
            setSelectedQuestionBookIds(setOf(DEFAULT_BOOK_ID))
        }
        if (prefs.getBoolean(KEY_INITIAL_QUESTIONS_V2, false)) return
        // These five questions were part of the existing implementation. Seed
        // them only when there are no questions, so user data is never replaced
        // or duplicated. V2 also repairs installs that created the initial book
        // while the questions were temporarily not seeded.
        if (getReviewQuestions().isEmpty()) {
            val seeds = listOf(
                "1200円の商品を25%引きで購入した。価格はいくら？\n選択肢A 900円\n選択肢B 800円\n選択肢C 1000円" to "900円",
                "3、6、12、24の次の数は？\n選択肢A 36\n選択肢B 48\n選択肢C 60" to "48",
                "『簡潔』の反対に近い言葉は？\n選択肢A 明瞭\n選択肢B 冗長\n選択肢C 短縮" to "冗長",
                "5人が同じ速さで作業し10分で完成した。10人なら何分？\n選択肢A 20分\n選択肢B 10分\n選択肢C 5分" to "5分",
                "赤・青・黄の3色から2色を選ぶ組み合わせは何通り？\n選択肢A 3通り\n選択肢B 6通り\n選択肢C 9通り" to "3通り",
            )
            addReviewQuestions(DEFAULT_BOOK_ID, seeds)
        }
        prefs.edit()
            .putBoolean(KEY_REVIEW_QUESTIONS_SEEDED, true)
            .putBoolean(KEY_INITIAL_QUESTIONS_V2, true)
            .apply()
    }

    private fun readUserPurposeItems(): List<PurposeItemModel> {
        return readPurposeItemArray(KEY_USER_PURPOSE_ITEMS)
    }

    private fun writeUserPurposeItems(items: List<PurposeItemModel>) {
        writePurposeItemArray(KEY_USER_PURPOSE_ITEMS, items)
    }

    private fun readUserActivityCategories(): List<ActivityCategoryModel> {
        return readActivityCategoryArray(KEY_USER_ACTIVITY_CATEGORIES)
    }

    private fun writeUserActivityCategories(items: List<ActivityCategoryModel>) {
        writeActivityCategoryArray(KEY_USER_ACTIVITY_CATEGORIES, items)
    }

    private fun readUserPurposeCategories(): List<PurposeCategoryModel> {
        val array = JSONArray(prefs.getString(KEY_USER_PURPOSE_CATEGORIES, "[]") ?: "[]")
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    PurposeCategoryModel(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        displayOrder = item.optInt("displayOrder", 100 + index),
                        isDefault = false,
                    ),
                )
            }
        }
    }

    private fun readPurposeItemArray(key: String): List<PurposeItemModel> {
        val array = JSONArray(prefs.getString(key, "[]") ?: "[]")
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    PurposeItemModel(
                        id = item.getString("id"),
                        categoryId = item.getString("categoryId"),
                        label = item.getString("label"),
                        isDefault = false,
                        createdAt = item.optLong("createdAt", 0L),
                    ),
                )
            }
        }
    }

    private fun writePurposeItemArray(key: String, items: List<PurposeItemModel>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(item.toJson())
        }
        prefs.edit().putString(key, array.toString()).apply()
    }

    private fun readActivityCategoryArray(key: String): List<ActivityCategoryModel> {
        val array = JSONArray(prefs.getString(key, "[]") ?: "[]")
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    ActivityCategoryModel(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        displayOrder = item.optInt("displayOrder", 100 + index),
                        isDefault = false,
                    ),
                )
            }
        }
    }

    private fun writeActivityCategoryArray(key: String, items: List<ActivityCategoryModel>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(item.toJson())
        }
        prefs.edit().putString(key, array.toString()).apply()
    }

    private fun recordVersionInfo() {
        val packageInfo = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
        val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
        prefs.edit()
            .putString(KEY_VERSION_NAME, packageInfo.versionName)
            .putLong(KEY_VERSION_CODE, versionCode)
            .apply()
    }

    private fun PurposeItemModel.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("categoryId", categoryId)
        .put("label", label)
        .put("createdAt", createdAt)

    private fun ActivityCategoryModel.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("displayOrder", displayOrder)

    companion object {
        private const val PREFS_NAME = "nanishini_prefs"
        private const val KEY_INTRO_ACCEPTED = "intro_accepted"
        private const val KEY_PAUSE_MODE = "pause_mode"
        private const val KEY_PAUSE_UNTIL = "pause_until"
        private const val KEY_LAST_DETECTED_APP = "last_detected_app"
        private const val KEY_EXCLUSION_STATUS = "exclusion_status"
        private const val KEY_ENABLED_TARGET_PACKAGES = "enabled_target_packages"
        private const val KEY_RECENT_ACTIVITY_DETAILS = "recent_activity_details"
        private const val KEY_USER_PURPOSE_ITEMS = "user_purpose_items"
        private const val KEY_USER_ACTIVITY_CATEGORIES = "user_activity_categories"
        private const val KEY_USER_PURPOSE_CATEGORIES = "user_purpose_categories"
        private const val KEY_VERSION_NAME = "version_name"
        private const val KEY_VERSION_CODE = "version_code"
        private const val KEY_REVIEW_QUESTIONS = "review_questions"
        private const val KEY_REVIEW_QUESTIONS_SEEDED = "review_questions_seeded"
        private const val KEY_INITIAL_QUESTIONS_V2 = "initial_questions_v2"
        private const val KEY_QUESTION_BOOKS = "question_books"
        private const val KEY_SELECTED_BOOK_IDS = "selected_question_book_ids"
        private const val KEY_REVIEW_COUNT = "review_question_count"
        private const val KEY_CHECKPOINT_ENABLED = "checkpoint_enabled"
        private const val KEY_REVIEW_CAPTCHA_ENABLED = "review_captcha_enabled"
        const val DEFAULT_BOOK_ID = "default-question-book"
    }
}
