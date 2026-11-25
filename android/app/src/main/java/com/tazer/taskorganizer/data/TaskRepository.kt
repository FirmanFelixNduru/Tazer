package com.tazer.taskorganizer.data

import androidx.room.withTransaction
import com.tazer.taskorganizer.domain.PlannedWork
import com.tazer.taskorganizer.domain.SchedulePlan
import com.tazer.taskorganizer.domain.ScheduleRule
import com.tazer.taskorganizer.domain.Scheduler
import com.tazer.taskorganizer.domain.SchedulingTask
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
        val trimmedDescription = description.trim()
        requireTaskFields(trimmedTitle, trimmedCourse, trimmedDescription, difficultyScore, estimatedMinutes)
        val task = Task(
            id = newId(),
            ownerUid = uid,
            title = trimmedTitle,
            course = trimmedCourse,
            description = trimmedDescription,
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
    ): Boolean {
        requireUid(uid)
        val trimmedTitle = title.trim()
        val trimmedCourse = course.trim()
        val trimmedDescription = description.trim()
        requireTaskFields(trimmedTitle, trimmedCourse, trimmedDescription, difficultyScore, remainingMinutes)
        return dao.updateTask(
            uid, id, trimmedTitle, trimmedCourse, trimmedDescription,
            deadlineAt, difficultyScore, remainingMinutes, analysisSource,
        ) == 1
    }

    suspend fun deleteTask(uid: String, id: String): Boolean =
        dao.deleteTask(requireUid(uid), id) == 1

    suspend fun reopenTask(uid: String, id: String, remainingMinutes: Int): Boolean {
        requireUid(uid)
        require(remainingMinutes in 1..10080)
        return dao.reopenTask(uid, id, remainingMinutes) == 1
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
        requireFixedSchedule(label, dayOfWeek, startMinute, endMinute)
        val schedule = FixedSchedule(newId(), uid, label.trim(), kind, dayOfWeek, startMinute, endMinute)
        dao.insertFixedSchedule(schedule)
        return schedule
    }

    suspend fun updateFixedSchedule(
        uid: String,
        id: String,
        label: String,
        kind: FixedScheduleKind,
        dayOfWeek: Int,
        startMinute: Int,
        endMinute: Int,
    ): Boolean {
        requireUid(uid)
        requireFixedSchedule(label, dayOfWeek, startMinute, endMinute)
        return dao.updateFixedSchedule(uid, id, label.trim(), kind, dayOfWeek, startMinute, endMinute) == 1
    }

    suspend fun saveFixedScheduleRange(
        uid: String,
        id: String?,
        label: String,
        kind: FixedScheduleKind,
        dayOfWeek: Int,
        startMinute: Int,
        endMinute: Int,
    ): Boolean {
        requireUid(uid)
        require(startMinute in 0..1439 && endMinute in 0..1440 && startMinute != endMinute)
        val firstEnd = if (endMinute < startMinute) 1440 else endMinute
        return database.withTransaction {
            if (id == null) {
                addFixedSchedule(uid, label, kind, dayOfWeek, startMinute, firstEnd)
            } else if (!updateFixedSchedule(uid, id, label, kind, dayOfWeek, startMinute, firstEnd)) {
                return@withTransaction false
            }
            if (endMinute in 1..<startMinute) {
                val nextDay = dayOfWeek % 7 + 1
                addFixedSchedule(uid, label, kind, nextDay, 0, endMinute)
            }
            true
        }
    }

    suspend fun deleteFixedSchedule(uid: String, scheduleId: String): Boolean =
        dao.deleteFixedSchedule(requireUid(uid), scheduleId) == 1

    fun observeWorkSessions(uid: String): Flow<List<WorkSession>> =
        dao.observeWorkSessions(requireUid(uid))

    suspend fun rebuildPlan(uid: String, now: Long): SchedulePlan {
        requireUid(uid)
        return database.withTransaction {
            val profile = dao.findProfile(uid)
                ?: return@withTransaction SchedulePlan(emptyList(), emptyList())
            val pending = dao.findPendingSessions(uid)
            if (dao.hasActiveSession(uid)) {
                return@withTransaction SchedulePlan(pending.map { it.toPlannedWork() }, emptyList())
            }

            val tasks = dao.findOpenTasks(uid).map {
                SchedulingTask(it.id, it.deadlineAt, it.createdAt, it.remainingMinutes, it.difficultyScore)
            }
            val rules = dao.findFixedSchedules(uid).map {
                ScheduleRule(it.id, it.label, it.dayOfWeek, it.startMinute, it.endMinute)
            }
            val plan = Scheduler.plan(
                tasks = tasks,
                rules = rules,
                zone = ZoneId.of(profile.zoneId),
                activeStartMinute = profile.activeStartMinute,
                activeEndMinute = profile.activeEndMinute,
                now = now,
                busyUntilAt = profile.busyUntilAt,
            )

            val unmatched = plan.sessions.toMutableSet()
            val staleIds = pending.filterNot { unmatched.remove(it.toPlannedWork()) }.map { it.id }
            // Stay below SQLite's bind limit even when many old drafts need cancellation.
            for (ids in staleIds.chunked(900)) dao.cancelStaleSessions(uid, ids)

            val newSessions = plan.sessions.filter { it in unmatched }.map { it.toPendingSession(uid) }
            if (newSessions.isNotEmpty()) dao.insertSessions(newSessions)
            plan
        }
    }

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
            val session = PlannedWork(taskId, plannedStartAt, plannedStartAt + durationMs, plannedMinutes)
                .toPendingSession(uid)
            dao.insertSession(session)
            session
        }
    }

    private fun WorkSession.toPlannedWork() = PlannedWork(taskId, plannedStartAt, plannedEndAt, plannedMinutes)

    private fun PlannedWork.toPendingSession(uid: String) = WorkSession(
        id = newId(),
        taskId = taskId,
        ownerUid = uid,
        state = WorkSessionState.PENDING,
        plannedStartAt = startAt,
        plannedEndAt = endAt,
        plannedMinutes = minutes,
        startedAt = null,
        endedAt = null,
        actualWorkMs = null,
        timerBootId = null,
        activeSinceElapsedMs = null,
        cutoffElapsedMs = null,
        lastObservedWorkMs = null,
        revision = 0,
    )

    private fun requireTaskFields(
        title: String,
        course: String,
        description: String,
        difficultyScore: Int,
        remainingMinutes: Int,
    ) {
        require(title.length in 1..120)
        require(course.length in 1..120)
        require(description.length <= 4000)
        require(difficultyScore in 1..5)
        require(remainingMinutes in 1..10080)
    }

    private fun requireFixedSchedule(label: String, dayOfWeek: Int, startMinute: Int, endMinute: Int) {
        require(label.isNotBlank())
        require(dayOfWeek in 1..7)
        requireMinuteRange(startMinute, endMinute)
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
