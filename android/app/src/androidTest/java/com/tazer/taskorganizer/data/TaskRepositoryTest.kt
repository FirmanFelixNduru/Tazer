package com.tazer.taskorganizer.data

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tazer.taskorganizer.domain.FocusMoment
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskRepositoryTest {
    private lateinit var database: TazerDatabase
    private lateinit var repository: TaskRepository

    @Before
    fun setUp() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, TazerDatabase::class.java).build()
        repository = TaskRepository(database)
        repository.saveProfile("alice", "Asia/Jakarta", 480, 1320)
        repository.saveProfile("bob", "Asia/Jakarta", 480, 1320)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun readsAndWritesAreScopedToTheOwner() = runBlocking {
        val task = addTask("alice")
        val bobTask = addTask("bob")
        val schedule = repository.addFixedSchedule("alice", "Makan", FixedScheduleKind.BREAK, 1, 720, 780)
        val session = requireNotNull(repository.addPlannedSession("alice", task.id, 60_000L, 25))

        assertEquals(listOf(task), repository.observeTasks("alice").first())
        assertEquals(listOf(bobTask), repository.observeTasks("bob").first())
        assertEquals(listOf(schedule), repository.observeFixedSchedules("alice").first())
        assertEquals(listOf(session), repository.observeWorkSessions("alice").first())
        assertTrue(repository.observeFixedSchedules("bob").first().isEmpty())
        assertTrue(repository.observeWorkSessions("bob").first().isEmpty())
        assertNull(repository.observeProfile("guest").first())
        assertNull(repository.addPlannedSession("bob", task.id, 60_000L, 25))
        assertFalse(repository.completeTask("bob", task.id, 90_000L))
        assertFalse(editTask("bob", task.id))
        assertFalse(repository.deleteTask("bob", task.id))
        assertFalse(repository.updateFixedSchedule("bob", schedule.id, "Kuliah", FixedScheduleKind.ROUTINE, 2, 600, 660))
        assertFalse(repository.deleteFixedSchedule("bob", schedule.id))
        assertEquals(listOf(task), repository.observeTasks("alice").first())
        assertEquals(listOf(schedule), repository.observeFixedSchedules("alice").first())
    }

    @Test
    fun editingKeepsTheBaselineAndTrimsFields() = runBlocking {
        val task = addTask("alice")
        val schedule = repository.addFixedSchedule("alice", "Makan", FixedScheduleKind.BREAK, 1, 720, 780)

        assertTrue(editTask("alice", task.id))
        assertTrue(repository.updateFixedSchedule("alice", schedule.id, " Kuliah ", FixedScheduleKind.ROUTINE, 2, 600, 660))
        assertEquals(
            task.copy(
                title = "Tugas revisi", course = "Basis Data", description = "Revisi skema",
                deadlineAt = 2_400_000L, difficultyScore = 4, remainingMinutes = 70, analysisSource = AnalysisSource.AI,
            ),
            repository.observeTasks("alice").first().single(),
        )
        assertEquals(
            schedule.copy(label = "Kuliah", kind = FixedScheduleKind.ROUTINE, dayOfWeek = 2, startMinute = 600, endMinute = 660),
            repository.observeFixedSchedules("alice").first().single(),
        )
    }

    @Test
    fun overnightScheduleEditSplitsOnTheNextIsoDay() = runBlocking {
        val schedule = repository.addFixedSchedule("alice", "Makan", FixedScheduleKind.BREAK, 7, 720, 780)
        val kind = FixedScheduleKind.ROUTINE

        assertFalse(repository.saveFixedScheduleRange("bob", schedule.id, "Piket", kind, 7, 1380, 60))
        assertTrue(repository.observeFixedSchedules("bob").first().isEmpty())
        assertTrue(repository.saveFixedScheduleRange("alice", schedule.id, " Piket ", kind, 7, 1380, 60))
        val rows = repository.observeFixedSchedules("alice").first()
        val continuation = rows.single { it.dayOfWeek == 1 }
        assertEquals(FixedSchedule(continuation.id, "alice", "Piket", kind, 1, 0, 60), continuation)
        assertEquals(FixedSchedule(schedule.id, "alice", "Piket", kind, 7, 1380, 1440), rows.last())

        assertTrue(repository.saveFixedScheduleRange("alice", null, "Istirahat", kind, 2, 1380, 0))
        assertEquals(3, repository.observeFixedSchedules("alice").first().size)
    }

    @Test
    fun insertingOvernightContinuationFailureRollsBackFirstRow() = runBlocking {
        val schedule = repository.addFixedSchedule("alice", "Makan", FixedScheduleKind.BREAK, 7, 720, 780)
        database.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER reject_overnight_continuation BEFORE INSERT ON FixedSchedule WHEN NEW.dayOfWeek = 1
            BEGIN SELECT RAISE(ABORT, 'intentional test failure'); END
            """.trimIndent(),
        )

        val failure = runCatching {
            repository.saveFixedScheduleRange("alice", schedule.id, "Piket", FixedScheduleKind.ROUTINE, 7, 1380, 60)
        }.exceptionOrNull()

        assertTrue(failure is SQLiteConstraintException)
        assertEquals(listOf(schedule), repository.observeFixedSchedules("alice").first())
    }

    @Test
    fun reopenKeepsActualHistoryAndArchiveDeletionCascades() = runBlocking {
        val task = addTask("alice")
        val pending = requireNotNull(repository.addPlannedSession("alice", task.id, 0L, 25))
        val done = pending.copy(
            id = UUID.randomUUID().toString(), state = WorkSessionState.DONE,
            startedAt = 0L, endedAt = 900_000L, actualWorkMs = 900_000L, revision = 2,
        )
        database.tazerDao().insertSession(done)
        assertTrue(repository.completeTask("alice", task.id, 900_000L))

        assertFalse(editTask("alice", task.id))
        assertFalse(repository.reopenTask("bob", task.id, 40))
        assertTrue(repository.reopenTask("alice", task.id, 40))
        assertFalse(repository.reopenTask("alice", task.id, 30))
        assertEquals(task.copy(remainingMinutes = 40), repository.observeTasks("alice").first().single())
        assertEquals(
            setOf(done, pending.copy(state = WorkSessionState.CANCELLED, revision = 1)),
            repository.observeWorkSessions("alice").first().toSet(),
        )

        assertTrue(repository.completeTask("alice", task.id, 1_800_000L))
        assertTrue(repository.deleteTask("alice", task.id))
        assertTrue(repository.observeTasks("alice").first().isEmpty())
        assertTrue(repository.observeWorkSessions("alice").first().isEmpty())
    }

    @Test
    fun deletingAnOpenTaskCascadesToDrafts() = runBlocking {
        val task = addTask("alice")
        requireNotNull(repository.addPlannedSession("alice", task.id, 60_000L, 25))

        assertTrue(repository.deleteTask("alice", task.id))
        assertFalse(repository.deleteTask("alice", task.id))
        assertTrue(repository.observeTasks("alice").first().isEmpty())
        assertTrue(repository.observeWorkSessions("alice").first().isEmpty())
    }

    @Test
    fun changingProfilePreservesDependentRows() = runBlocking {
        val task = addTask("alice")
        val schedule = repository.addFixedSchedule("alice", "Makan", FixedScheduleKind.BREAK, 1, 720, 780)
        val session = requireNotNull(repository.addPlannedSession("alice", task.id, 60_000L, 25))

        repository.saveProfile("alice", "Asia/Jakarta", 540, 1260, busyUntilAt = 123L)

        assertEquals(UserProfile("alice", "Asia/Jakarta", 540, 1260, 123L), repository.observeProfile("alice").first())
        assertEquals(listOf(task), repository.observeTasks("alice").first())
        assertEquals(listOf(schedule), repository.observeFixedSchedules("alice").first())
        assertEquals(listOf(session), repository.observeWorkSessions("alice").first())
    }

    @Test
    fun concurrentCompletionCancelsPlanOnlyOnce() = runBlocking {
        val task = addTask("alice")
        val session = requireNotNull(repository.addPlannedSession("alice", task.id, 60_000L, 25))
        val results = List(2) {
            async { repository.completeTask("alice", task.id, 90_000L) }
        }.awaitAll()

        assertEquals(1, results.count { it })
        assertEquals(
            task.copy(status = TaskStatus.DONE, remainingMinutes = 0, completedAt = 90_000L),
            repository.observeTasks("alice").first().single(),
        )
        assertEquals(
            session.copy(state = WorkSessionState.CANCELLED, revision = 1),
            repository.observeWorkSessions("alice").first().single(),
        )
        assertNull(repository.addPlannedSession("alice", task.id, 120_000L, 25))
    }

    @Test
    fun cancellationFailureRollsBackTaskCompletion() = runBlocking {
        val task = addTask("alice")
        val session = requireNotNull(repository.addPlannedSession("alice", task.id, 60_000L, 25))
        database.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER reject_session_cancellation
            BEFORE UPDATE ON WorkSession WHEN NEW.state = 'CANCELLED'
            BEGIN SELECT RAISE(ABORT, 'intentional test failure'); END
            """.trimIndent(),
        )

        val failure = runCatching { repository.completeTask("alice", task.id, 90_000L) }.exceptionOrNull()

        assertTrue(failure is SQLiteConstraintException)
        assertEquals(listOf(task), repository.observeTasks("alice").first())
        assertEquals(listOf(session), repository.observeWorkSessions("alice").first())
    }

    @Test
    fun completionLeavesActiveSessionForTimerDecision() = runBlocking {
        val task = addTask("alice")
        val pending = requireNotNull(repository.addPlannedSession("alice", task.id, 120_000L, 25))
        val running = pending.copy(
            id = UUID.randomUUID().toString(),
            state = WorkSessionState.RUNNING,
            plannedStartAt = 60_000L,
            plannedEndAt = 1_560_000L,
            startedAt = 60_000L,
        )
        database.tazerDao().insertSession(running)

        assertFalse(repository.completeTask("alice", task.id, 90_000L))
        assertFalse(repository.deleteTask("alice", task.id))
        val now = 1_800_000L
        val held = repository.rebuildPlan("alice", now)
        assertEquals(listOf(task.id), held.sessions.map { it.taskId })
        assertEquals(listOf(pending.plannedStartAt), held.sessions.map { it.startAt })
        assertEquals(listOf(task), repository.observeTasks("alice").first())
        assertEquals(listOf(running, pending), repository.observeWorkSessions("alice").first())

        addTask("bob")
        assertTrue(repository.rebuildPlan("bob", now).sessions.isNotEmpty())
    }

    @Test
    fun rebuildingCancelsStaleDraftsAndPreservesHistoryAndMatchingIds() = runBlocking {
        val task = addTask("alice")
        val bobTask = addTask("bob")
        val now = Instant.parse("2026-11-23T01:00:00Z").toEpochMilli()
        val old = requireNotNull(repository.addPlannedSession("alice", task.id, now - 5_400_000L, 25))
        val bobSession = requireNotNull(repository.addPlannedSession("bob", bobTask.id, now, 25))
        val done = old.copy(
            id = UUID.randomUUID().toString(), state = WorkSessionState.DONE,
            startedAt = old.plannedStartAt, endedAt = old.plannedEndAt, actualWorkMs = 1_500_000L, revision = 2,
        )
        database.tazerDao().insertSession(done)

        val plan = repository.rebuildPlan("alice", now)
        val persisted = repository.observeWorkSessions("alice").first()
        val draft = persisted.single { it.state == WorkSessionState.PENDING }
        assertEquals(plan.sessions.single().startAt, draft.plannedStartAt)
        assertEquals(plan.sessions.single().endAt, draft.plannedEndAt)
        assertEquals(25, draft.plannedMinutes)
        assertEquals(0L, draft.revision)
        assertTrue(done in persisted)
        assertTrue(old.copy(state = WorkSessionState.CANCELLED, revision = 1) in persisted)
        assertEquals(listOf(bobSession), repository.observeWorkSessions("bob").first())

        val db = database.openHelper.writableDatabase
        db.execSQL("""
            CREATE TRIGGER reject_draft_update BEFORE UPDATE ON WorkSession
            BEGIN SELECT RAISE(ABORT, 'rebuild should keep existing rows'); END
        """.trimIndent())
        db.execSQL("""
            CREATE TRIGGER reject_draft_insert BEFORE INSERT ON WorkSession
            BEGIN SELECT RAISE(ABORT, 'rebuild should keep existing IDs'); END
        """.trimIndent())
        assertEquals(plan, repository.rebuildPlan("alice", now))
        assertEquals(persisted, repository.observeWorkSessions("alice").first())
    }

    @Test
    fun insertingReplacementFailureRollsBackDraftCancellation() = runBlocking {
        val task = addTask("alice")
        val stale = requireNotNull(repository.addPlannedSession("alice", task.id, 60_000L, 25))
        database.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER reject_replacement BEFORE INSERT ON WorkSession
            BEGIN SELECT RAISE(ABORT, 'intentional test failure'); END
            """.trimIndent(),
        )

        val now = Instant.parse("2026-11-23T01:00:00Z").toEpochMilli()
        val failure = runCatching { repository.rebuildPlan("alice", now) }.exceptionOrNull()

        assertTrue(failure is SQLiteConstraintException)
        assertEquals(listOf(stale), repository.observeWorkSessions("alice").first())
    }

    @Test
    fun foreignKeyRejectsSessionForTaskOwnedByAnotherAccount() = runBlocking {
        val task = addTask("alice")
        val session = requireNotNull(repository.addPlannedSession("alice", task.id, 60_000L, 25))
        val bobTask = addTask("bob")

        val failure = runCatching {
            database.tazerDao().insertSession(session.copy(id = bobTask.id, ownerUid = "bob"))
        }.exceptionOrNull()

        assertTrue(failure is SQLiteConstraintException)
        assertTrue(repository.observeWorkSessions("bob").first().isEmpty())
    }

    @Test
    fun focusStartAndCutoffAreAtomicAndScopedToOwnerAndRevision() = runBlocking {
        val task = addFocusTask("alice")
        val pending = draft(task, "09:25", 25)
        val alternative = draft(task, "09:25", 25)
        val bobTask = addFocusTask("bob")
        val bob = draft(bobTask, "09:25", 25)
        val now = focusMoment("09:25", 60)

        assertNull(repository.focusSession("bob", pending.id))
        assertNull(repository.startFocus("bob", pending.id, pending.revision, now))
        assertNull(repository.startFocus("alice", pending.id, pending.revision + 1, now))
        assertNull(repository.startFocus("alice", pending.id, pending.revision, now.copy(wallMs = now.wallMs - 1)))
        val starts = listOf(pending, alternative).map { session ->
            async { repository.startFocus("alice", session.id, session.revision, now) }
        }.awaitAll()
        val running = starts.filterNotNull().single().active!!
        assertEquals(WorkSessionState.RUNNING, running.state)
        assertEquals(minutes(85), running.cutoffElapsedMs)
        assertEquals(TaskStatus.IN_PROGRESS, repository.observeTasks("alice").first().single().status)
        assertEquals(bob.id, repository.startFocus("bob", bob.id, bob.revision, now)?.active?.id)

        val checked = repository.checkFocus("alice", running.id, running.revision, focusMoment("10:15", 84))!!.active!!
        assertEquals(running.revision, checked.revision)
        assertEquals(minutes(24), checked.lastObservedWorkMs)
        val overtime = repository.recoverFocus("alice", focusMoment("09:45", 85))!!.active!!
        assertEquals(WorkSessionState.OVERTIME, overtime.state)
        assertEquals(running.cutoffElapsedMs, overtime.cutoffElapsedMs)
        assertEquals(running.revision + 1, overtime.revision)
        assertNull(repository.checkFocus("alice", running.id, running.revision, focusMoment("09:50", 90)))
        assertNull(repository.checkFocus("alice", running.id, overtime.revision, focusMoment("09:50", 90)))
        assertNull(repository.finishFocus("alice", running.id, running.revision, focusMoment("09:50", 90)))
        assertNull(repository.extendFocus("alice", running.id, running.revision, 15, focusMoment("09:50", 90)))
        assertEquals(overtime, repository.activeFocus("alice"))
        assertEquals(bob.id, repository.activeFocus("bob")?.id)
    }

    @Test
    fun aLateStartShortensCutoffOrReplansWhenTheRemainingSlotIsTooShort() = runBlocking {
        val task = addFocusTask("alice")
        val pending = draft(task, "09:00", 40)
        val running = repository.startFocus("alice", pending.id, pending.revision, focusMoment("09:10", 60))!!.active!!
        assertEquals(minutes(90), running.cutoffElapsedMs)
        assertEquals(wall("09:40"), running.plannedEndAt)
        repository.pauseFocus("alice", running.id, running.revision, focusMoment("09:11", 61))
        val short = draft(task, "09:30", 25)
        val retry = repository.startFocus("alice", short.id, short.revision, focusMoment("09:35", 85))!!
        assertNull(retry.active)
        assertTrue(retry.plan!!.sessions.all { it.startAt >= wall("09:35") })
        assertEquals(WorkSessionState.CANCELLED, repository.focusSession("alice", short.id)?.state)
    }

    @Test
    fun startingOneMillisecondLateKeepsOriginalCutoffForMinimumAndShortSlots() = runBlocking {
        for ((uid, amount) in listOf("alice" to 25, "bob" to 1)) {
            val task = addFocusTask(uid, estimate = amount)
            val pending = draft(task, "09:00", amount)
            val start = FocusMoment(wall("09:00") + 1, minutes(60) + 1, "boot")
            val active = repository.startFocus(uid, pending.id, pending.revision, start)!!.active!!
            assertEquals(minutes(60 + amount), active.cutoffElapsedMs)
            val before = FocusMoment(pending.plannedEndAt - 1, minutes(60 + amount) - 1, "boot")
            assertEquals(WorkSessionState.RUNNING, repository.checkFocus(uid, active.id, active.revision, before)?.active?.state)
            val cutoff = before.copy(wallMs = before.wallMs + 1, elapsedMs = before.elapsedMs + 1)
            assertEquals(WorkSessionState.OVERTIME, repository.checkFocus(uid, active.id, active.revision, cutoff)?.active?.state)
        }
    }

    @Test
    fun smartShiftRespectsFixedBlocksAndRecordsOvertimeBeforeLateDecision() = runBlocking {
        val task = addFocusTask("alice")
        repository.addFixedSchedule("alice", "Kuliah", FixedScheduleKind.ROUTINE, 1, 600, 660)
        val pending = draft(task, "09:25", 25)
        val original = draft(task, "11:00", 30)
        val running = repository.startFocus("alice", pending.id, pending.revision, focusMoment("09:25", 60))!!.active!!
        val result = repository.extendFocus("alice", running.id, running.revision, 30, focusMoment("10:02", 97))!!
        val closed = repository.focusSession("alice", running.id)!!
        assertNull(result.active)
        assertEquals(wall("11:00"), result.plan!!.sessions.single().startAt)
        assertEquals(WorkSessionState.DONE, closed.state)
        assertEquals(minutes(37), closed.actualWorkMs)
        assertEquals(running.revision + 2, closed.revision)
        assertEquals(30, repository.observeTasks("alice").first().single().remainingMinutes)
        assertEquals(WorkSessionState.CANCELLED, repository.focusSession("alice", original.id)?.state)
    }

    @Test
    fun chainedExtensionReservesContinuousWorkAndCompletionSurvivesReopen() = runBlocking {
        val task = addFocusTask("alice")
        val other = addFocusTask("alice")
        val pending = draft(task, "09:00", 25)
        val running = repository.startFocus("alice", pending.id, pending.revision, focusMoment("09:00", 60))!!.active!!
        val first = repository.extendFocus("alice", running.id, running.revision, 15, focusMoment("09:26", 86))!!
        val extended = first.active!!

        assertEquals(WorkSessionState.EXTENDED, extended.state)
        assertEquals(minutes(101), extended.cutoffElapsedMs)
        assertTrue(first.plan!!.sessions.all { it.taskId == other.id && it.startAt >= wall("09:51") })
        val next = repository.extendFocus("alice", extended.id, extended.revision, 9, focusMoment("09:42", 102))!!
        assertNull(next.active) // 42 continuous minutes leaves only 8.
        val nextDraft = repository.pendingFocus("alice").first { it.taskId == task.id }
        assertTrue(nextDraft.plannedStartAt >= wall("09:52"))
        val startedAt = nextDraft.plannedStartAt
        val resumed = repository.startFocus("alice", nextDraft.id, nextDraft.revision, FocusMoment(
            startedAt, minutes(102) + startedAt - wall("09:42"), "boot",
        ))!!.active!!
        val finish = FocusMoment(startedAt + minutes(2), resumed.activeSinceElapsedMs!! + minutes(2), "boot")
        assertNull(repository.finishFocus("alice", resumed.id, resumed.revision, finish)!!.active)
        assertNull(repository.finishFocus("alice", resumed.id, resumed.revision, finish))
        assertEquals(finish.wallMs, repository.observeTasks("alice").first().single { it.id == task.id }.completedAt)
        assertTrue(repository.pendingFocus("alice").none { it.taskId == task.id })
        assertTrue(repository.reopenTask("alice", task.id, 19))
        val reopened = repository.observeTasks("alice").first().single { it.id == task.id }
        assertEquals(TaskStatus.PENDING, reopened.status)
        assertEquals(19, reopened.remainingMinutes)
        assertNull(reopened.completedAt)
        assertTrue(repository.rebuildPlan("alice", finish.wallMs).sessions.any { it.taskId == task.id })
        assertEquals(minutes(44), repository.observeWorkSessions("alice").first()
            .filter { it.taskId == task.id && it.state == WorkSessionState.DONE }.sumOf { it.actualWorkMs!! })
    }

    @Test
    fun deferAndRebootPreserveHistoryBreaksAndDeadlineRisk() = runBlocking {
        val archived = addFocusTask("alice")
        assertTrue(repository.completeTask("alice", archived.id, wall("08:00")))
        val task = addFocusTask("alice", deadline = "11:30", estimate = 45)
        repository.addFixedSchedule("alice", "Kuliah", FixedScheduleKind.ROUTINE, 1, 660, 680)
        val pending = draft(task, "09:00", 45)
        val running = repository.startFocus("alice", pending.id, pending.revision, focusMoment("09:00", 60))!!.active!!
        val checkpoint = repository.recoverFocus("alice", focusMoment("09:12", 72))!!.active!!
        assertEquals(running.revision, checkpoint.revision)
        val reboot = FocusMoment(wall("09:16"), minutes(2), "new")
        assertNull(repository.recoverFocus("alice", reboot)!!.active)
        val closed = repository.focusSession("alice", running.id)!!
        assertEquals(wall("09:12"), closed.endedAt)
        assertEquals(minutes(12), closed.actualWorkMs)
        assertEquals(wall("09:22"), repository.pendingFocus("alice").first().plannedStartAt)
        assertEquals(wall("09:22"), repository.rebuildPlan("alice", reboot.wallMs).sessions.first().startAt)
        assertNull(repository.recoverFocus("alice", reboot))

        val next = repository.pendingFocus("alice").first()
        val resumed = repository.startFocus("alice", next.id, next.revision, FocusMoment(wall("09:22"), minutes(8), "new"))!!.active!!
        val now = FocusMoment(wall("09:24"), minutes(10), "new")
        val result = repository.deferFocus("alice", wall("11:00"), now)!!
        assertNull(result.active)
        assertEquals(wall("11:00"), repository.observeProfile("alice").first()?.busyUntilAt)
        assertEquals(task.id, result.plan!!.risks.single().taskId)
        assertEquals(31, result.plan.risks.single().missingMinutes)
        val archive = repository.observeTasks("alice").first().single { it.id == archived.id }
        assertEquals(wall("08:00"), archive.completedAt)
        assertEquals(closed, repository.focusSession("alice", closed.id))
        assertEquals(minutes(2), repository.focusSession("alice", resumed.id)?.actualWorkMs)
        assertNull(repository.deferFocus("alice", now.wallMs - 1, now))
        assertEquals(result, repository.deferFocus("alice", wall("11:00"), now))
    }

    @Test
    fun deferUntilNowStillReservesBreakAcrossFixedSchedule() = runBlocking {
        val task = addFocusTask("alice")
        val pending = draft(task, "09:00", 25)
        val running = repository.startFocus("alice", pending.id, pending.revision, focusMoment("09:00", 60))!!.active!!
        repository.addFixedSchedule("alice", "Istirahat", FixedScheduleKind.BREAK, 1, 555, 558)
        val now = focusMoment("09:12", 72)

        val change = repository.deferFocus("alice", now.wallMs, now)!!

        assertNull(change.active)
        assertEquals(wall("09:22"), change.plan!!.sessions.single().startAt)
        assertEquals(28, repository.observeTasks("alice").first().single().remainingMinutes)
        assertEquals(minutes(12), repository.focusSession("alice", running.id)?.actualWorkMs)
    }

    @Test
    fun failedReplanCannotPartiallyInterruptFocus() = runBlocking {
        val task = addFocusTask("alice")
        val pending = draft(task, "09:00", 25)
        val running = repository.startFocus("alice", pending.id, pending.revision, focusMoment("09:00", 60))!!.active!!
        database.openHelper.writableDatabase.execSQL("""
            CREATE TRIGGER reject_focus_replan BEFORE INSERT ON WorkSession WHEN NEW.state = 'PENDING'
            BEGIN SELECT RAISE(ABORT, 'intentional test failure'); END
        """.trimIndent())

        val failure = runCatching {
            repository.deferFocus("alice", wall("09:12"), focusMoment("09:12", 72))
        }.exceptionOrNull()
        assertTrue(failure is SQLiteConstraintException)
        assertEquals(running, repository.activeFocus("alice"))
        assertEquals(task.remainingMinutes, repository.observeTasks("alice").first().single().remainingMinutes)
        assertNull(repository.observeProfile("alice").first()?.busyUntilAt)
    }

    private suspend fun addFocusTask(uid: String, deadline: String = "14:00", estimate: Int = 40) =
        repository.createTask(uid, "Tugas", "Algoritma", "", wall(deadline), 2, estimate, wall("08:00"))

    private suspend fun draft(task: Task, time: String, duration: Int): WorkSession =
        requireNotNull(repository.addPlannedSession(task.ownerUid, task.id, wall(time), duration))

    private fun focusMoment(time: String, elapsed: Int) = FocusMoment(wall(time), minutes(elapsed), "boot")
    private fun wall(time: String) = Instant.parse("2026-11-23T$time:00+07:00").toEpochMilli()
    private fun minutes(value: Int) = value * 60_000L

    private suspend fun addTask(uid: String): Task = repository.createTask(
        uid = uid,
        title = "Tugas",
        course = "Algoritma",
        description = "",
        deadlineAt = 1_800_000L,
        difficultyScore = 2,
        estimatedMinutes = 25,
        createdAt = 0L,
    )

    private suspend fun editTask(uid: String, id: String): Boolean = repository.updateTask(
        uid, id, " Tugas revisi ", " Basis Data ", " Revisi skema ", 2_400_000L, 4, 70, AnalysisSource.AI,
    )
}
