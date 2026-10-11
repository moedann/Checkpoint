package com.example.nanisini

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class UsageHistoryRepositoryTest {
    @Test
    fun aggregateUsesRealSessionRowsForMonthlyMetrics() {
        val zone = ZoneId.of("Asia/Tokyo")
        val august = ZonedDateTime.of(2026, 8, 8, 12, 0, 0, 0, zone).toInstant().toEpochMilli()
        val july = ZonedDateTime.of(2026, 7, 8, 12, 0, 0, 0, zone).toInstant().toEpochMilli()
        val dao = FakeUsageHistoryDao(
            mutableListOf(
                session("a", "instagram", "Instagram", august, 120, "purpose-any-1", "特に目的はない"),
                session("b", "instagram", "Instagram", august + 1_000, 60, "purpose-contact-1", "返信する"),
                session("c", "youtube", "YouTube", august + 2_000, 300, "purpose-watch-1", "特定の動画を見る"),
                session("old", "youtube", "YouTube", july, 100, "purpose-watch-1", "動画を見る"),
            ),
        )
        val result = UsageHistoryRepository(dao, zoneId = zone).aggregate(java.time.YearMonth.of(2026, 8))

        assertEquals(480L, result.totalUsageSeconds)
        assertEquals(3, result.launchCount)
        assertEquals(100L, result.previousUsageSeconds)
        assertEquals(2, result.topByUsage.size)
        assertEquals("instagram", result.topByLaunches.first().packageName)
        assertEquals(1, result.noPurposeCount)
        assertEquals(3, result.daily.first { it.day == 8 }.launchCount)
    }

    @Test
    fun coordinatorCreatesOneLaunchAndFinalizesExtensionSeparately() {
        val dao = FakeUsageHistoryDao(mutableListOf())
        val repository = UsageHistoryRepository(dao, executor = java.util.concurrent.Executor { it.run() })
        var wallTime = 1_000_000L
        val coordinator = SessionHistoryCoordinator(repository) { wallTime }
        val session = ActiveSession(
            packageName = "instagram",
            applicationLabel = "Instagram",
            purposeCategoryId = "contact",
            purposeItemId = "reply",
            purposeText = "返信する",
            timerDurationMillis = 10_000L,
            timerState = TimerState.RUNNING,
        )

        coordinator.sessionStarted(session)
        assertEquals(1, dao.allSessions.size)
        wallTime += 10_000L
        coordinator.extensionStarted(
            8_000L,
            SessionDraft(
                packageName = "instagram",
                applicationLabel = "Instagram",
                purposeCategoryId = "watch",
                purposeItemId = "video",
                purposeText = "動画を見る",
                durationMillis = 20_000L,
            ),
        )
        wallTime += 20_000L
        coordinator.sessionFinished(session.copy(extensionCount = 1), 15_000L)

        assertEquals(1, dao.allSessions.size)
        assertEquals(1, dao.allExtensions.size)
        assertEquals(23L, dao.allSessions.single().actualUsageSeconds)
        assertEquals(1, dao.allSessions.single().extensionCount)
        assertEquals(15L, dao.allExtensions.single().actualUsageSeconds)
    }

    private fun session(id: String, packageName: String, label: String, startedAt: Long, seconds: Long, purposeId: String, purpose: String) =
        UsageSessionEntity(id, packageName, label, startedAt, startedAt + seconds * 1_000, "rest", purposeId, purpose, seconds, seconds, "activity-study", "勉強", null, 0, startedAt)
}

private class FakeUsageHistoryDao(private val sessions: MutableList<UsageSessionEntity>) : UsageHistoryDao {
    val allSessions: List<UsageSessionEntity> get() = sessions
    val allExtensions = mutableListOf<SessionExtensionEntity>()
    override fun insertSession(session: UsageSessionEntity) { sessions += session }
    override fun updateSession(session: UsageSessionEntity) { sessions.replaceAll { if (it.id == session.id) session else it } }
    override fun insertExtension(extension: SessionExtensionEntity) { allExtensions += extension }
    override fun updateExtension(extension: SessionExtensionEntity) { allExtensions.replaceAll { if (it.id == extension.id) extension else it } }
    override fun sessionsBetween(from: Long, until: Long) = sessions.filter { it.startedAt >= from && it.startedAt < until }.sortedBy { it.startedAt }
    override fun recentSessions(limit: Int) = sessions.sortedByDescending { it.startedAt }.take(limit)
    override fun earliestSessionStart() = sessions.minOfOrNull { it.startedAt }
    override fun deleteAllSessions() = sessions.clear()
}
