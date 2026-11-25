package com.tazer.taskorganizer.data

import androidx.room.withTransaction
import com.tazer.taskorganizer.domain.FocusClock
import com.tazer.taskorganizer.domain.FocusMoment
import com.tazer.taskorganizer.domain.PlannedWork
import com.tazer.taskorganizer.domain.SchedulePlan
import com.tazer.taskorganizer.domain.ScheduleRule
import com.tazer.taskorganizer.domain.Scheduler
import com.tazer.taskorganizer.domain.SchedulingTask
import com.tazer.taskorganizer.domain.TimeBlock
import com.tazer.taskorganizer.domain.continuousWorkMs
import com.tazer.taskorganizer.domain.focusDurationMs
import com.tazer.taskorganizer.domain.remainingAfterInterruption
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

    suspend fun activeFocus(uid: String): WorkSession? = dao.findActiveFocus(requireUid(uid))

    suspend fun focusSession(uid: String, id: String): WorkSession? =
        dao.findFocusSession(requireUid(uid), id)

    suspend fun pendingFocus(uid: String): List<WorkSession> = dao.findPendingSessions(requireUid(uid))

    suspend fun startFocus(uid: String, id: String, revision: Long, now: FocusMoment): FocusChange? =
        database.withTransaction {
            val session = focusSession(uid, id)?.takeIf {
                it.state == WorkSessionState.PENDING && it.revision == revision && now.wallMs >= it.plannedStartAt
            } ?: return@withTransaction null
            if (dao.hasActiveSession(uid)) return@withTransaction null
            val task = dao.findTask(uid, session.taskId)?.takeUnless { it.status == TaskStatus.DONE }
                ?: return@withTransaction null
            val profile = dao.findProfile(uid) ?: return@withTransaction null
            val rules = scheduleRules(uid)
            val breakBlock = breakReservation(uid, now.wallMs, now)
            val durationMs = if (now.wallMs < session.plannedEndAt && breakBlock == null &&
                (profile.busyUntilAt ?: now.wallMs) <= now.wallMs
            ) {
                profile.focusDurationAt(now.wallMs, task, rules, plannedMinutes = session.plannedMinutes)
            } else 0L
            if (durationMs == 0L) {
                return@withTransaction FocusChange(null, applyPlan(profile, now.wallMs, rules, breakBlock))
            }
            val running = session.startTimer(WorkSessionState.RUNNING, durationMs, now)
            saveFocus(session, running)
            check(dao.updateFocusTask(uid, task.id, task.remainingMinutes) == 1)
            val end = now.wallMs + durationMs
            val plan = if (end != session.plannedEndAt) {
                val reserved = TimeBlock(now.wallMs, end + Scheduler.BREAK_MS)
                applyPlan(profile, now.wallMs, rules, reserved, running)
            } else null
            FocusChange(running, plan)
        }

    suspend fun checkFocus(uid: String, id: String, revision: Long, now: FocusMoment): FocusChange? =
        database.withTransaction {
            val session = matchActiveFocus(uid, id, revision) ?: return@withTransaction null
            if (session.state == WorkSessionState.OVERTIME || !session.clock().isSameBoot(now)) return@withTransaction null
            checkpointFocus(session, now)
        }

    suspend fun finishFocus(uid: String, id: String, revision: Long, now: FocusMoment): FocusChange? =
        database.withTransaction {
            val session = matchActiveFocus(uid, id, revision) ?: return@withTransaction null
            if (!session.clock().isSameBoot(now)) return@withTransaction null
            closeFocus(session, now)
            check(dao.completeTask(uid, session.taskId, now.wallMs) == 1)
            FocusChange(null, replanFocus(uid, now))
        }

    suspend fun extendFocus(
        uid: String,
        id: String,
        revision: Long,
        additionalMinutes: Int,
        now: FocusMoment,
    ): FocusChange? = database.withTransaction {
        val session = matchActiveFocus(uid, id, revision) ?: return@withTransaction null
        if (!session.clock().isSameBoot(now) || additionalMinutes !in 1..10080) return@withTransaction null
        val task = requireNotNull(dao.findTask(uid, session.taskId)).copy(remainingMinutes = additionalMinutes)
        val profile = requireNotNull(dao.findProfile(uid))
        val rules = scheduleRules(uid)
        val previous = dao.findFinishedFocusOnBoot(uid, now.bootId).asSequence().map {
            val start = requireNotNull(it.activeSinceElapsedMs)
            TimeBlock(start, start + requireNotNull(it.actualWorkMs))
        }
        val continuous = continuousWorkMs(requireNotNull(session.activeSinceElapsedMs), session.clock().workAt(now), previous)
        val durationMs = if ((profile.busyUntilAt ?: now.wallMs) <= now.wallMs) {
            profile.focusDurationAt(now.wallMs, task, rules, continuous = continuous)
        } else 0L

        closeFocus(session, now)
        check(dao.updateFocusTask(uid, task.id, additionalMinutes) == 1)
        invalidatePendingFocus(uid)
        if (durationMs == 0L) return@withTransaction FocusChange(null, replanFocus(uid, now))
        val minutes = (durationMs / Scheduler.MINUTE_MS).toInt()
        val end = now.wallMs + durationMs
        val extended = PlannedWork(task.id, now.wallMs, end, minutes).toPendingSession(uid)
            .startTimer(WorkSessionState.EXTENDED, durationMs, now)
        dao.insertSession(extended)
        val reserved = TimeBlock(now.wallMs, end + Scheduler.BREAK_MS)
        FocusChange(extended, applyPlan(profile, now.wallMs, rules, reserved, extended))
    }

    suspend fun pauseFocus(uid: String, id: String, revision: Long, now: FocusMoment): FocusChange? =
        database.withTransaction {
            val session = matchActiveFocus(uid, id, revision) ?: return@withTransaction null
            interruptFocus(session, now)
            FocusChange(null, replanFocus(uid, now))
        }

    suspend fun deferFocus(uid: String, resumeAt: Long, now: FocusMoment): FocusChange? =
        database.withTransaction {
            requireUid(uid)
            if (resumeAt < now.wallMs) return@withTransaction null
            val profile = dao.findProfile(uid) ?: return@withTransaction null
            val session = dao.findActiveFocus(uid)
            if (session != null) interruptFocus(session, now)
            if (session != null || profile.busyUntilAt != resumeAt) {
                dao.saveProfile(profile.copy(busyUntilAt = resumeAt))
                invalidatePendingFocus(uid)
            }
            FocusChange(null, replanFocus(uid, now))
        }

    suspend fun recoverFocus(uid: String, now: FocusMoment): FocusChange? = database.withTransaction {
        val session = activeFocus(uid) ?: return@withTransaction null
        if (session.clock().isSameBoot(now)) return@withTransaction checkpointFocus(session, now)
        interruptFocus(session, now)
        FocusChange(null, replanFocus(uid, now))
    }

    suspend fun rebuildPlan(uid: String, now: Long): SchedulePlan {
        requireUid(uid)
        return database.withTransaction {
            val profile = dao.findProfile(uid)
                ?: return@withTransaction SchedulePlan(emptyList(), emptyList())
            if (dao.hasActiveSession(uid)) {
                return@withTransaction SchedulePlan(pendingFocus(uid).map { it.toPlannedWork() }, emptyList())
            }
            applyPlan(profile, now, scheduleRules(uid), breakReservation(uid, now))
        }
    }

    private suspend fun matchActiveFocus(uid: String, id: String, revision: Long): WorkSession? =
        activeFocus(uid)?.takeIf { it.id == id && it.revision == revision }

    private suspend fun checkpointFocus(session: WorkSession, now: FocusMoment): FocusChange {
        val clock = session.clock()
        val crossedCutoff = session.state != WorkSessionState.OVERTIME && clock.cutoffReached(now)
        val updated = session.copy(
            state = if (crossedCutoff) WorkSessionState.OVERTIME else session.state,
            lastObservedWorkMs = clock.workAt(now),
            revision = session.revision + if (crossedCutoff) 1 else 0,
        )
        if (updated != session) saveFocus(session, updated)
        return FocusChange(updated)
    }

    private suspend fun closeFocus(session: WorkSession, now: FocusMoment): Long {
        val clock = session.clock()
        val workMs = clock.workAt(now)
        val sameBoot = clock.isSameBoot(now)
        val endedAt = if (sameBoot) now.wallMs else {
            minOf(now.wallMs, (session.startedAt ?: session.plannedStartAt) + workMs)
        }
        val cutoffRevision = if (session.state != WorkSessionState.OVERTIME && clock.cutoffReached(now)) 1 else 0
        val closed = session.copy(
            state = WorkSessionState.DONE,
            endedAt = endedAt,
            actualWorkMs = workMs,
            lastObservedWorkMs = workMs,
            revision = session.revision + cutoffRevision + 1,
        )
        saveFocus(session, closed)
        return workMs
    }

    private suspend fun interruptFocus(session: WorkSession, now: FocusMoment) {
        val task = requireNotNull(dao.findTask(session.ownerUid, session.taskId))
        val workMs = closeFocus(session, now)
        val remaining = remainingAfterInterruption(task.remainingMinutes, workMs)
        check(dao.updateFocusTask(task.ownerUid, task.id, remaining) == 1)
    }

    private suspend fun saveFocus(previous: WorkSession, updated: WorkSession) {
        check(dao.updateFocus(
            previous.ownerUid, previous.id, previous.revision, updated.state, updated.startedAt,
            updated.endedAt, updated.actualWorkMs, updated.timerBootId, updated.activeSinceElapsedMs,
            updated.cutoffElapsedMs, updated.lastObservedWorkMs, updated.revision,
        ) == 1)
    }

    private fun WorkSession.clock() = FocusClock(timerBootId, activeSinceElapsedMs, cutoffElapsedMs, lastObservedWorkMs)

    private fun WorkSession.startTimer(state: WorkSessionState, durationMs: Long, now: FocusMoment) = copy(
        state = state,
        startedAt = now.wallMs,
        timerBootId = now.bootId,
        activeSinceElapsedMs = now.elapsedMs,
        cutoffElapsedMs = now.elapsedMs + durationMs,
        lastObservedWorkMs = 0L,
        revision = revision + 1,
    )

    private fun UserProfile.focusDurationAt(
        now: Long,
        task: Task,
        rules: List<ScheduleRule>,
        plannedMinutes: Int? = null,
        continuous: Long = 0L,
    ) = focusDurationMs(
        now, task.toSchedulingTask(), rules, ZoneId.of(zoneId), activeStartMinute, activeEndMinute,
        plannedMinutes, continuous,
    )

    private suspend fun breakReservation(uid: String, now: Long, moment: FocusMoment? = null): TimeBlock? {
        val endedAt = dao.findLatestFocusEnd(uid, now, moment?.elapsedMs, moment?.bootId) ?: return null
        val end = endedAt + Scheduler.BREAK_MS
        return TimeBlock(now, end).takeIf { end > now }
    }

    private suspend fun replanFocus(uid: String, now: FocusMoment): SchedulePlan =
        applyPlan(requireNotNull(dao.findProfile(uid)), now.wallMs, scheduleRules(uid), breakReservation(uid, now.wallMs, now))

    private suspend fun scheduleRules(uid: String): List<ScheduleRule> = dao.findFixedSchedules(uid).map {
        ScheduleRule(it.id, it.label, it.dayOfWeek, it.startMinute, it.endMinute)
    }

    private suspend fun applyPlan(
        profile: UserProfile,
        now: Long,
        rules: List<ScheduleRule>,
        reserved: TimeBlock?,
        retained: WorkSession? = null,
    ): SchedulePlan {
        val uid = profile.ownerUid
        val tasks = dao.findOpenTasks(uid).map { task ->
            val schedulingTask = task.toSchedulingTask()
            if (retained != null && task.id == retained.taskId) {
                schedulingTask.copy(remainingMinutes = task.remainingMinutes - retained.plannedMinutes)
            } else schedulingTask
        }
        val plan = Scheduler.plan(
            tasks = tasks,
            rules = rules,
            zone = ZoneId.of(profile.zoneId),
            activeStartMinute = profile.activeStartMinute,
            activeEndMinute = profile.activeEndMinute,
            now = now,
            busyUntilAt = profile.busyUntilAt,
            reserved = listOfNotNull(reserved),
        )
        val unmatched = plan.sessions.toMutableSet()
        val staleIds = pendingFocus(uid).filterNot { unmatched.remove(it.toPlannedWork()) }.map { it.id }
        // Stay below SQLite's bind limit even when many old drafts need cancellation.
        for (ids in staleIds.chunked(900)) dao.cancelStaleSessions(uid, ids)
        val newSessions = plan.sessions.filter { it in unmatched }.map { it.toPendingSession(uid) }
        if (newSessions.isNotEmpty()) dao.insertSessions(newSessions)
        return plan
    }

    private suspend fun invalidatePendingFocus(uid: String) {
        for (ids in pendingFocus(uid).map { it.id }.chunked(900)) dao.cancelStaleSessions(uid, ids)
    }

    private fun Task.toSchedulingTask() = SchedulingTask(id, deadlineAt, createdAt, remainingMinutes, difficultyScore)

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
