package com.tazer.taskorganizer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
internal interface TazerDao {
    @Query("SELECT * FROM UserProfile WHERE ownerUid = :uid")
    fun observeProfile(uid: String): Flow<UserProfile?>

    @Query("SELECT * FROM UserProfile WHERE ownerUid = :uid")
    suspend fun findProfile(uid: String): UserProfile?

    @Upsert
    suspend fun saveProfile(profile: UserProfile)

    @Query("SELECT * FROM Task WHERE ownerUid = :uid ORDER BY completedAt DESC, id ASC")
    fun observeTasks(uid: String): Flow<List<Task>>

    @Query("SELECT * FROM Task WHERE ownerUid = :uid AND status != 'DONE'")
    suspend fun findOpenTasks(uid: String): List<Task>

    @Query("SELECT * FROM Task WHERE ownerUid = :uid AND id = :taskId")
    suspend fun findTask(uid: String, taskId: String): Task?

    @Insert
    suspend fun insertTask(task: Task)

    @Query("""
        UPDATE Task
        SET title = :title, course = :course, description = :description,
            deadlineAt = :deadlineAt, difficultyScore = :difficultyScore,
            remainingMinutes = :remainingMinutes, analysisSource = :analysisSource
        WHERE ownerUid = :uid AND id = :id AND status != 'DONE'
    """)
    suspend fun updateTask(
        uid: String,
        id: String,
        title: String,
        course: String,
        description: String,
        deadlineAt: Long,
        difficultyScore: Int,
        remainingMinutes: Int,
        analysisSource: AnalysisSource,
    ): Int

    @Query("""
        DELETE FROM Task
        WHERE ownerUid = :uid AND id = :id
            AND NOT EXISTS (
                SELECT 1 FROM WorkSession
                WHERE ownerUid = :uid AND taskId = :id
                    AND state IN ('RUNNING', 'OVERTIME', 'EXTENDED')
            )
    """)
    suspend fun deleteTask(uid: String, id: String): Int

    @Query("""
        UPDATE Task
        SET status = 'PENDING', remainingMinutes = :remainingMinutes, completedAt = NULL
        WHERE ownerUid = :uid AND id = :id AND status = 'DONE'
    """)
    suspend fun reopenTask(uid: String, id: String, remainingMinutes: Int): Int

    @Query("""
        UPDATE Task
        SET status = 'DONE', remainingMinutes = 0, completedAt = :completedAt
        WHERE ownerUid = :uid AND id = :taskId AND status != 'DONE'
            AND NOT EXISTS (
                SELECT 1 FROM WorkSession
                WHERE ownerUid = :uid AND taskId = :taskId
                    AND state IN ('RUNNING', 'OVERTIME', 'EXTENDED')
            )
    """)
    suspend fun completeTask(uid: String, taskId: String, completedAt: Long): Int

    @Query("""
        UPDATE WorkSession
        SET state = 'CANCELLED', revision = revision + 1
        WHERE ownerUid = :uid AND taskId = :taskId AND state = 'PENDING'
    """)
    suspend fun cancelPendingSessions(uid: String, taskId: String)

    @Query("""
        SELECT * FROM FixedSchedule WHERE ownerUid = :uid
        ORDER BY dayOfWeek, startMinute, endMinute, id
    """)
    fun observeFixedSchedules(uid: String): Flow<List<FixedSchedule>>

    @Query("SELECT * FROM FixedSchedule WHERE ownerUid = :uid")
    suspend fun findFixedSchedules(uid: String): List<FixedSchedule>

    @Insert
    suspend fun insertFixedSchedule(schedule: FixedSchedule)

    @Query("""
        UPDATE FixedSchedule
        SET label = :label, kind = :kind, dayOfWeek = :dayOfWeek,
            startMinute = :startMinute, endMinute = :endMinute
        WHERE ownerUid = :uid AND id = :id
    """)
    suspend fun updateFixedSchedule(
        uid: String,
        id: String,
        label: String,
        kind: FixedScheduleKind,
        dayOfWeek: Int,
        startMinute: Int,
        endMinute: Int,
    ): Int

    @Query("DELETE FROM FixedSchedule WHERE ownerUid = :uid AND id = :scheduleId")
    suspend fun deleteFixedSchedule(uid: String, scheduleId: String): Int

    @Query("SELECT * FROM WorkSession WHERE ownerUid = :uid ORDER BY plannedStartAt, id")
    fun observeWorkSessions(uid: String): Flow<List<WorkSession>>

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM WorkSession WHERE ownerUid = :uid
            AND state IN ('RUNNING', 'OVERTIME', 'EXTENDED')
        )
    """)
    suspend fun hasActiveSession(uid: String): Boolean

    @Query("""
        SELECT * FROM WorkSession WHERE ownerUid = :uid AND state = 'PENDING'
        ORDER BY plannedStartAt, id
    """)
    suspend fun findPendingSessions(uid: String): List<WorkSession>

    @Query("""
        UPDATE WorkSession
        SET state = 'CANCELLED', revision = revision + 1
        WHERE ownerUid = :uid AND id IN (:ids) AND state = 'PENDING'
    """)
    suspend fun cancelStaleSessions(uid: String, ids: List<String>)

    @Insert
    suspend fun insertSession(session: WorkSession)

    @Insert
    suspend fun insertSessions(sessions: List<WorkSession>)
}
