package com.example.nanisini

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.Executors

data class DailyUsage(val day: Int, val usageSeconds: Long, val launchCount: Int, val purposeCount: Int = 0)
data class AppUsage(val packageName: String, val applicationLabel: String, val usageSeconds: Long, val launchCount: Int)
data class Breakdown(val id: String, val label: String, val usageSeconds: Long, val launchCount: Int)
data class RecentUsage(
    val id: String,
    val startedAt: Long,
    val packageName: String,
    val applicationLabel: String,
    val purposeText: String,
    val actualUsageSeconds: Long,
)
data class MonthlyDashboard(
    val year: Int,
    val month: Int,
    val totalUsageSeconds: Long,
    val launchCount: Int,
    val previousUsageSeconds: Long,
    val previousLaunchCount: Int,
    val daily: List<DailyUsage>,
    val topByUsage: List<AppUsage>,
    val topByLaunches: List<AppUsage>,
    val purposeCategories: List<Breakdown>,
    val purposes: List<Breakdown>,
    val noPurposeCount: Int,
    val previousNoPurposeCount: Int,
    val noPurposeApps: List<AppUsage>,
    val activityCategories: List<Breakdown>,
    val activityApps: Map<String, List<AppUsage>>,
    val recent: List<RecentUsage>,
    val earliestMonth: String?,
)

data class TodayDashboard(
    val launchCount: Int,
    val purposeCount: Int,
    val noPurposeCount: Int,
    val topPurposes: List<Breakdown>,
    val recent: List<RecentUsage>,
)

class UsageHistoryRepository(
    private val dao: UsageHistoryDao,
    private val executor: Executor = Executors.newSingleThreadExecutor(),
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    fun execute(task: () -> Unit) = executor.execute(task)

    fun insertSession(session: UsageSessionEntity) = execute { dao.insertSession(session) }
    fun updateSession(session: UsageSessionEntity) = execute { dao.updateSession(session) }
    fun insertExtension(extension: SessionExtensionEntity) = execute { dao.insertExtension(extension) }
    fun updateExtension(extension: SessionExtensionEntity) = execute { dao.updateExtension(extension) }
    fun deleteHistory(onComplete: (() -> Unit)? = null) = execute { dao.deleteAllSessions(); onComplete?.invoke() }

    fun loadDashboard(year: Int, month: Int, callback: (Result<MonthlyDashboard>) -> Unit) = execute {
        callback(runCatching { aggregate(YearMonth.of(year, month)) })
    }

    fun loadTodayDashboard(callback: (Result<TodayDashboard>) -> Unit) = execute {
        callback(runCatching { aggregateToday() })
    }

    internal fun aggregateToday(now: Instant = Instant.now()): TodayDashboard {
        val date = now.atZone(zoneId).toLocalDate()
        val start = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val sessions = dao.sessionsBetween(start, end)
        fun isNoPurpose(row: UsageSessionEntity) =
            row.purposeItemId == "purpose-any-1" || row.purposeText == "特に目的はない"
        val noPurpose = sessions.count(::isNoPurpose)
        val purposes = sessions.filterNot(::isNoPurpose)
            .filter { !it.purposeText.isNullOrBlank() }
            .groupBy { it.purposeItemId ?: it.purposeText.orEmpty() }
            .map { (id, rows) ->
                Breakdown(id, rows.last().purposeText.orEmpty(), rows.sumOf { it.actualUsageSeconds }, rows.size)
            }
            .sortedByDescending { it.launchCount }
            .take(3)
        return TodayDashboard(
            launchCount = sessions.size,
            purposeCount = sessions.size - noPurpose,
            noPurposeCount = noPurpose,
            topPurposes = purposes,
            recent = sessions.sortedByDescending { it.startedAt }.take(3).map {
                RecentUsage(it.id, it.startedAt, it.packageName, it.applicationLabel, it.purposeText.orEmpty(), it.actualUsageSeconds)
            },
        )
    }

    internal fun aggregate(month: YearMonth): MonthlyDashboard {
        val range = range(month)
        val previousRange = range(month.minusMonths(1))
        val sessions = dao.sessionsBetween(range.first, range.second)
        val previous = dao.sessionsBetween(previousRange.first, previousRange.second)
        val apps = sessions.groupBy { it.packageName }.map { (packageName, rows) ->
            AppUsage(packageName, rows.last().applicationLabel, rows.sumOf { it.actualUsageSeconds }, rows.size)
        }
        fun breakdown(
            key: (UsageSessionEntity) -> String?,
            label: (UsageSessionEntity) -> String?,
        ): List<Breakdown> = sessions.filter { !key(it).isNullOrBlank() }.groupBy { key(it).orEmpty() }.map { (id, rows) ->
            Breakdown(id, label(rows.last()).orEmpty().ifBlank { "未設定" }, rows.sumOf { it.actualUsageSeconds }, rows.size)
        }.sortedByDescending { it.launchCount }

        val noPurpose = sessions.filter { it.purposeItemId == "purpose-any-1" || it.purposeText == "特に目的はない" }
        val previousNoPurpose = previous.count { it.purposeItemId == "purpose-any-1" || it.purposeText == "特に目的はない" }
        val activityGroups = sessions.filter { !it.activityCategoryId.isNullOrBlank() }.groupBy { it.activityCategoryId.orEmpty() }
        return MonthlyDashboard(
            year = month.year,
            month = month.monthValue,
            totalUsageSeconds = sessions.sumOf { it.actualUsageSeconds },
            launchCount = sessions.size,
            previousUsageSeconds = previous.sumOf { it.actualUsageSeconds },
            previousLaunchCount = previous.size,
            daily = (1..month.lengthOfMonth()).map { day ->
                val rows = sessions.filter { Instant.ofEpochMilli(it.startedAt).atZone(zoneId).dayOfMonth == day }
                DailyUsage(day, rows.sumOf { it.actualUsageSeconds }, rows.size, rows.count {
                    it.purposeItemId != "purpose-any-1" && it.purposeText != "特に目的はない"
                })
            },
            topByUsage = apps.sortedByDescending { it.usageSeconds }.take(5),
            topByLaunches = apps.sortedByDescending { it.launchCount }.take(5),
            purposeCategories = breakdown(
                {
                    when {
                        it.purposeItemId == "purpose-any-1" || it.purposeText == "特に目的はない" -> "no-purpose"
                        it.purposeItemId == "purpose-any-2" -> "other"
                        else -> it.purposeCategoryId
                    }
                },
                { row ->
                    when {
                        row.purposeItemId == "purpose-any-1" || row.purposeText == "特に目的はない" -> "特に目的はない"
                        row.purposeItemId == "purpose-any-2" -> "その他"
                        else -> defaultPurposeCategories.firstOrNull { it.id == row.purposeCategoryId }?.name ?: row.purposeCategoryId
                    }
                },
            ),
            purposes = breakdown({ it.purposeItemId ?: it.purposeText }, { it.purposeText }),
            noPurposeCount = noPurpose.size,
            previousNoPurposeCount = previousNoPurpose,
            noPurposeApps = noPurpose.groupBy { it.packageName }.map { (packageName, rows) ->
                AppUsage(packageName, rows.last().applicationLabel, rows.sumOf { it.actualUsageSeconds }, rows.size)
            }.sortedByDescending { it.launchCount }.take(3),
            activityCategories = breakdown({ it.activityCategoryId }, { it.activityCategoryName }),
            activityApps = activityGroups.mapValues { (_, rows) ->
                rows.groupBy { it.packageName }.map { (packageName, appRows) ->
                    AppUsage(packageName, appRows.last().applicationLabel, appRows.sumOf { it.actualUsageSeconds }, appRows.size)
                }.sortedByDescending { it.launchCount }.take(3)
            },
            recent = dao.recentSessions(5).map {
                RecentUsage(it.id, it.startedAt, it.packageName, it.applicationLabel, it.purposeText.orEmpty(), it.actualUsageSeconds)
            },
            earliestMonth = dao.earliestSessionStart()?.let { Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate().let(YearMonth::from).toString() },
        )
    }

    private fun range(month: YearMonth): Pair<Long, Long> {
        val start = month.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = month.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return start to end
    }

    companion object {
        fun create(context: android.content.Context) = UsageHistoryRepository(UsageHistoryDatabase.get(context).usageHistoryDao())
        fun newId(): String = UUID.randomUUID().toString()
    }
}
