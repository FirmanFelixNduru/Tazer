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

    @Upsert
    suspend fun saveProfile(profile: UserProfile)

    @Query("SELECT * FROM Task WHERE ownerUid = :uid ORDER BY completedAt DESC, id ASC")
    fun observeTasks(uid: String): Flow<List<Task>>

    @Query("SELECT * FROM Task WHERE ownerUid = :uid AND id = :taskId")
    suspend fun findTask(uid: String, taskId: String): Task?

    @Insert
    suspend fun insertTask(task: Task)

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

    @Insert
    suspend fun insertFixedSchedule(schedule: FixedSchedule)

    @Query("DELETE FROM FixedSchedule WHERE ownerUid = :uid AND id = :scheduleId")
    suspend fun deleteFixedSchedule(uid: String, scheduleId: String): Int

    @Query("SELECT * FROM WorkSession WHERE ownerUid = :uid ORDER BY plannedStartAt, id")
    fun observeWorkSessions(uid: String): Flow<List<WorkSession>>

    @Insert
    suspend fun insertSession(session: WorkSession)
}
