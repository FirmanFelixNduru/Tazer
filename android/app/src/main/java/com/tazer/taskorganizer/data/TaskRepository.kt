package com.tazer.taskorganizer.data

import androidx.room.withTransaction
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val database: TazerDatabase) {
    private val dao = database.tazerDao()

    fun observeProfile(uid: String): Flow<UserProfile?> = dao.observeProfile(requireUid(uid))

    suspend fun saveProfile(
        uid: String,
        zoneId: String,
        activeStartMinute: Int,
        activeEndMinute: Int,
        busyUntilAt: Long? = null,
    ) {
        requireUid(uid)
        require(zoneId in ZoneId.getAvailableZoneIds())
        requireMinuteRange(activeStartMinute, activeEndMinute)
        dao.saveProfile(UserProfile(uid, zoneId, activeStartMinute, activeEndMinute, busyUntilAt))
    }

    fun observeTasks(uid: String): Flow<List<Task>> = dao.observeTasks(requireUid(uid))

    suspend fun createTask(
        uid: String,
        title: String,
        course: String,
        description: String,
        deadlineAt: Long,
        difficultyScore: Int,
        estimatedMinutes: Int,
        createdAt: Long,
        analysisSource: AnalysisSource = AnalysisSource.OFFLINE,
    ): Task {
        requireUid(uid)
        val trimmedTitle = title.trim()
        val trimmedCourse = course.trim()
        require(trimmedTitle.length in 1..120)
        require(trimmedCourse.length in 1..120)
        require(description.length <= 4000)
        require(difficultyScore in 1..5)
        require(estimatedMinutes in 1..10080)
        val task = Task(
            id = newId(),
            ownerUid = uid,
            title = trimmedTitle,
            course = trimmedCourse,
            description = description,
            deadlineAt = deadlineAt,
            difficultyScore = difficultyScore,
            initialEstimatedMinutes = estimatedMinutes,
            remainingMinutes = estimatedMinutes,
            analysisSource = analysisSource,
            status = TaskStatus.PENDING,
            createdAt = createdAt,
            completedAt = null,
        )
        dao.insertTask(task)
        return task
    }

    suspend fun completeTask(uid: String, taskId: String, completedAt: Long): Boolean {
        requireUid(uid)
        return database.withTransaction {
            val completed = dao.completeTask(uid, taskId, completedAt) == 1
            if (completed) dao.cancelPendingSessions(uid, taskId)
            completed
        }
    }

    fun observeFixedSchedules(uid: String): Flow<List<FixedSchedule>> =
        dao.observeFixedSchedules(requireUid(uid))

    suspend fun addFixedSchedule(
        uid: String,
        label: String,
        kind: FixedScheduleKind,
        dayOfWeek: Int,
        startMinute: Int,
        endMinute: Int,
    ): FixedSchedule {
        requireUid(uid)
        require(label.isNotBlank())
        require(dayOfWeek in 1..7)
        requireMinuteRange(startMinute, endMinute)
        val schedule = FixedSchedule(newId(), uid, label.trim(), kind, dayOfWeek, startMinute, endMinute)
        dao.insertFixedSchedule(schedule)
        return schedule
    }

    suspend fun deleteFixedSchedule(uid: String, scheduleId: String): Boolean =
        dao.deleteFixedSchedule(requireUid(uid), scheduleId) == 1

    fun observeWorkSessions(uid: String): Flow<List<WorkSession>> =
        dao.observeWorkSessions(requireUid(uid))

    suspend fun addPlannedSession(
        uid: String,
        taskId: String,
        plannedStartAt: Long,
        plannedMinutes: Int,
    ): WorkSession? {
        requireUid(uid)
        require(plannedMinutes in 1..50)
        val durationMs = plannedMinutes * 60_000L
        require(plannedStartAt <= Long.MAX_VALUE - durationMs)
        return database.withTransaction {
            val task = dao.findTask(uid, taskId)
            if (task == null || task.status == TaskStatus.DONE) return@withTransaction null
            val session = WorkSession(
                id = newId(),
                taskId = taskId,
                ownerUid = uid,
                state = WorkSessionState.PENDING,
                plannedStartAt = plannedStartAt,
                plannedEndAt = plannedStartAt + durationMs,
                plannedMinutes = plannedMinutes,
                startedAt = null,
                endedAt = null,
                actualWorkMs = null,
                timerBootId = null,
                activeSinceElapsedMs = null,
                cutoffElapsedMs = null,
                lastObservedWorkMs = null,
                revision = 0,
            )
            dao.insertSession(session)
            session
        }
    }

    private fun requireUid(uid: String): String {
        require(uid.isNotBlank())
        return uid
    }

    private fun requireMinuteRange(start: Int, end: Int) {
        require(start in 0..1439 && end in (start + 1)..1440)
    }

    private fun newId(): String = UUID.randomUUID().toString()
}
