package com.tazer.taskorganizer.data

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
        assertFalse(repository.deleteFixedSchedule("bob", schedule.id))
        assertEquals(listOf(task), repository.observeTasks("alice").first())
        assertEquals(listOf(schedule), repository.observeFixedSchedules("alice").first())
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
        assertEquals(listOf(task), repository.observeTasks("alice").first())
        assertEquals(listOf(running, pending), repository.observeWorkSessions("alice").first())
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
}
