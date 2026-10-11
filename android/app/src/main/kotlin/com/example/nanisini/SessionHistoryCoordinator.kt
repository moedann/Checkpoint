package com.example.nanisini

class SessionHistoryCoordinator(
    private val repository: UsageHistoryRepository,
    private val wallClock: () -> Long = System::currentTimeMillis,
) {
    private data class ExtensionRecord(
        val entity: SessionExtensionEntity,
        var actualUsageMillis: Long = 0L,
    )

    private data class Record(
        var session: UsageSessionEntity,
        var baseUsageMillis: Long = 0L,
        val completedExtensions: MutableList<ExtensionRecord> = mutableListOf(),
        var activeExtension: ExtensionRecord? = null,
    )

    private val records = linkedMapOf<String, Record>()

    fun sessionStarted(session: ActiveSession) {
        val now = wallClock()
        val entity = UsageSessionEntity(
            id = UsageHistoryRepository.newId(),
            packageName = session.packageName,
            applicationLabel = session.applicationLabel,
            startedAt = now,
            endedAt = now,
            purposeCategoryId = session.purposeCategoryId,
            purposeItemId = session.purposeItemId,
            purposeText = session.purposeText,
            plannedDurationSeconds = session.durationMillis / 1_000L,
            actualUsageSeconds = 0L,
            activityCategoryId = session.currentActivityCategoryId,
            activityCategoryName = session.currentActivityCategoryName,
            activityDetail = session.currentActivityDetail,
            extensionCount = 0,
            createdAt = now,
        )
        records[session.packageName] = Record(entity)
        repository.insertSession(entity)
    }

    fun extensionStarted(previousSegmentActualMillis: Long, draft: SessionDraft) {
        val record = records[draft.packageName] ?: return
        finishCurrentSegment(record, previousSegmentActualMillis)
        val now = wallClock()
        val extension = SessionExtensionEntity(
            id = UsageHistoryRepository.newId(),
            sessionId = record.session.id,
            purposeCategoryId = draft.purposeCategoryId,
            purposeItemId = draft.purposeItemId,
            purposeText = draft.purposeText,
            plannedDurationSeconds = draft.durationMillis / 1_000L,
            actualUsageSeconds = 0L,
            startedAt = now,
            endedAt = now,
        )
        record.activeExtension = ExtensionRecord(extension)
        repository.insertExtension(extension)
        val completedUsageMillis = record.baseUsageMillis + record.completedExtensions.sumOf { it.actualUsageMillis }
        repository.updateSession(
            record.session.copy(
                endedAt = now,
                actualUsageSeconds = completedUsageMillis / 1_000L,
                extensionCount = record.completedExtensions.size + 1,
            ),
        )
    }

    fun sessionFinished(session: ActiveSession, currentSegmentActualMillis: Long) {
        val record = records.remove(session.packageName) ?: return
        finishCurrentSegment(record, currentSegmentActualMillis)
        val now = wallClock()
        val extensionUsage = record.completedExtensions.sumOf { it.actualUsageMillis }
        val finalized = record.session.copy(
            endedAt = now,
            actualUsageSeconds = (record.baseUsageMillis + extensionUsage) / 1_000L,
            extensionCount = record.completedExtensions.size,
        )
        repository.updateSession(finalized)
    }

    fun checkpoint(session: ActiveSession, currentSegmentActualMillis: Long) {
        val record = records[session.packageName] ?: return
        val active = record.activeExtension
        if (active == null) {
            record.baseUsageMillis = currentSegmentActualMillis.coerceAtLeast(0L)
        } else {
            active.actualUsageMillis = currentSegmentActualMillis.coerceAtLeast(0L)
            repository.updateExtension(
                active.entity.copy(
                    endedAt = wallClock(),
                    actualUsageSeconds = active.actualUsageMillis / 1_000L,
                ),
            )
        }
        val totalMillis = record.baseUsageMillis +
            record.completedExtensions.sumOf { it.actualUsageMillis } +
            (record.activeExtension?.actualUsageMillis ?: 0L)
        repository.updateSession(
            record.session.copy(
                endedAt = wallClock(),
                actualUsageSeconds = totalMillis / 1_000L,
                extensionCount = record.completedExtensions.size + if (record.activeExtension == null) 0 else 1,
            ),
        )
    }

    fun hasRecord(packageName: String): Boolean = records.containsKey(packageName)

    private fun finishCurrentSegment(record: Record, actualMillis: Long) {
        val active = record.activeExtension
        if (active == null) {
            record.baseUsageMillis = actualMillis.coerceAtLeast(0L)
            return
        }
        active.actualUsageMillis = actualMillis.coerceAtLeast(0L)
        val finalized = active.entity.copy(
            endedAt = wallClock(),
            actualUsageSeconds = active.actualUsageMillis / 1_000L,
        )
        repository.updateExtension(finalized)
        record.completedExtensions += active.copy(entity = finalized)
        record.activeExtension = null
    }
}
