package com.example.nanisini

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update

@Entity(
    tableName = "usage_sessions",
    indices = [
        Index(value = ["startedAt"]),
        Index(value = ["packageName", "startedAt"]),
        Index(value = ["purposeCategoryId", "startedAt"]),
        Index(value = ["activityCategoryId", "startedAt"]),
    ],
)
data class UsageSessionEntity(
    @androidx.room.PrimaryKey val id: String,
    val packageName: String,
    val applicationLabel: String,
    val startedAt: Long,
    val endedAt: Long,
    val purposeCategoryId: String?,
    val purposeItemId: String?,
    val purposeText: String?,
    val plannedDurationSeconds: Long,
    val actualUsageSeconds: Long,
    val activityCategoryId: String?,
    val activityCategoryName: String?,
    val activityDetail: String?,
    val extensionCount: Int,
    val createdAt: Long,
)

@Entity(
    tableName = "session_extensions",
    foreignKeys = [
        ForeignKey(
            entity = UsageSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["sessionId"]), Index(value = ["startedAt"])],
)
data class SessionExtensionEntity(
    @androidx.room.PrimaryKey val id: String,
    val sessionId: String,
    val purposeCategoryId: String?,
    val purposeItemId: String?,
    val purposeText: String?,
    val plannedDurationSeconds: Long,
    val actualUsageSeconds: Long,
    val startedAt: Long,
    val endedAt: Long,
)

@Dao
interface UsageHistoryDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertSession(session: UsageSessionEntity)

    @Update
    fun updateSession(session: UsageSessionEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertExtension(extension: SessionExtensionEntity)

    @Update
    fun updateExtension(extension: SessionExtensionEntity)

    @Query("SELECT * FROM usage_sessions WHERE startedAt >= :from AND startedAt < :until ORDER BY startedAt ASC")
    fun sessionsBetween(from: Long, until: Long): List<UsageSessionEntity>

    @Query("SELECT * FROM usage_sessions ORDER BY startedAt DESC LIMIT :limit")
    fun recentSessions(limit: Int): List<UsageSessionEntity>

    @Query("SELECT MIN(startedAt) FROM usage_sessions")
    fun earliestSessionStart(): Long?

    @Query("DELETE FROM usage_sessions")
    fun deleteAllSessions()
}

@Database(
    entities = [UsageSessionEntity::class, SessionExtensionEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class UsageHistoryDatabase : RoomDatabase() {
    abstract fun usageHistoryDao(): UsageHistoryDao

    companion object {
        @Volatile private var instance: UsageHistoryDatabase? = null

        fun get(context: Context): UsageHistoryDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                UsageHistoryDatabase::class.java,
                "nanishini_usage.db",
            ).build().also { instance = it }
        }
    }
}
