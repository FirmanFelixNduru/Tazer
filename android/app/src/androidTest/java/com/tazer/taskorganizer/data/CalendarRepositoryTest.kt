package com.tazer.taskorganizer.data

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalendarRepositoryTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val start = LocalDate.of(2026, 11, 26).toEpochDay()
    private val end = start + 2

    @Test
    fun migratesExportedV1AndPreservesExistingRows() = runBlocking {
        val name = "calendar-v1-${UUID.randomUUID()}.db"
        try {
            createV1Fixture(name)
            val database = TazerDatabase.open(context, name)
            try {
                val dao = database.tazerDao()
                val calendar = CalendarRepository(database)
                assertEquals(UserProfile("alice", "Asia/Jakarta", 480, 1320, 5_400_000L), dao.findProfile("alice"))
                assertEquals(UserProfile("bob", "Asia/Makassar", 420, 1200, null), dao.findProfile("bob"))
                assertEquals(
                    Task("task-v1", "alice", "Tugas lama", "Algoritma", "Draf", 7_200_000L,
                        3, 25, 0, AnalysisSource.AI, TaskStatus.DONE, 0L, 1_500_000L),
                    dao.findTask("alice", "task-v1"),
                )
                assertEquals(
                    listOf(FixedSchedule("fixed-v1", "alice", "Kuliah", FixedScheduleKind.ROUTINE, 1, 600, 660)),
                    dao.findFixedSchedules("alice"),
                )
                assertEquals(
                    WorkSession("session-v1", "task-v1", "alice", WorkSessionState.DONE, 0L, 1_500_000L,
                        25, 0L, 1_500_000L, 1_500_000L, null, null, null, null, 4L),
                    dao.findFocusSession("alice", "session-v1"),
                )
                assertEquals(4, database.openHelper.writableDatabase.version)
                assertTrue(calendar.observeEvents("alice").first().isEmpty())

                assertTrue(calendar.saveEvent("alice", null, " Libur ", start, end, null, " Catatan "))
                assertTrue(calendar.saveEvent("bob", null, "Rapat", start, start, 0, ""))
                val aliceEvent = calendar.observeEvents("alice").first().single()
                val bobEvent = calendar.observeEvents("bob").first().single()
                assertEquals(CalendarEvent(aliceEvent.id, "alice", "Libur", start, end, null, "Catatan"), aliceEvent)
                assertEquals("bob", bobEvent.ownerUid)

                assertFalse(calendar.saveEvent("bob", aliceEvent.id, "Edit", start, end, 0, ""))
                assertFalse(calendar.deleteEvent("bob", aliceEvent.id))
                assertEquals(listOf(aliceEvent), calendar.observeEvents("alice").first())
                assertTrue(calendar.saveEvent("alice", aliceEvent.id, "Revisi", end, end, 1439, ""))
                assertEquals(
                    listOf(CalendarEvent(aliceEvent.id, "alice", "Revisi", end, end, 1439, "")),
                    calendar.observeEvents("alice").first(),
                )
                assertTrue(calendar.deleteEvent("alice", aliceEvent.id))
                assertFalse(calendar.saveEvent("alice", aliceEvent.id, "Stale", start, end, null, ""))
                assertTrue(calendar.observeEvents("alice").first().isEmpty())
                assertEquals(listOf(bobEvent), calendar.observeEvents("bob").first())
            } finally {
                database.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }

    @Test
    fun freshDatabaseEnforcesProfileForeignKeyAndRejectsInvalidEvents() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, TazerDatabase::class.java).build()
        try {
            val calendar = CalendarRepository(database)
            assertTrue(runCatching {
                calendar.saveEvent("alice", null, "Tanpa profil", start, end, null, "")
            }.exceptionOrNull() is SQLiteConstraintException)

            database.tazerDao().saveProfile(UserProfile("alice", "Asia/Jakarta", 480, 1320, null))
            assertTrue(runCatching {
                calendar.saveEvent("alice", null, "Tanggal terbalik", end, start, null, "")
            }.exceptionOrNull() is IllegalArgumentException)
            assertTrue(runCatching {
                calendar.saveEvent("alice", null, "Menit di luar batas", start, end, 1440, "")
            }.exceptionOrNull() is IllegalArgumentException)
            assertTrue(calendar.observeEvents("alice").first().isEmpty())

            assertTrue(calendar.saveEvent("alice", null, "Event", start, end, null, ""))
            database.openHelper.writableDatabase.execSQL("DELETE FROM UserProfile WHERE ownerUid = ?", arrayOf("alice"))
            assertTrue(calendar.observeEvents("alice").first().isEmpty())
        } finally {
            database.close()
        }
    }

    private fun createV1Fixture(name: String) {
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { database ->
            database.setForeignKeyConstraintsEnabled(true)
            // The DDL and identity hash come from Room's checked-in v1 export.
            instrumentation.context.assets.open("tazer-v1.sql").bufferedReader().useLines { statements ->
                statements.forEach { database.execSQL(it) }
            }
            database.version = 1
        }
    }
}
