package com.tazer.taskorganizer.data

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
