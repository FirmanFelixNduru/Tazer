package com.tazer.taskorganizer.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IcsImportTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val owner = UUID.randomUUID().toString()
    private val otherOwner = UUID.randomUUID().toString()

    @Test
    fun importIsAtomicDeduplicatedPerOwnerAndMarkedDirtyInTheExistingJournal() = withDatabase { database ->
        val calendar = CalendarRepository(database)
        val dao = database.tazerDao()
        val preview = calendar.previewIcs(owner, ::fixture)
        assertEquals(IcsImportCounts(3, 1, 1), preview.counts)
        assertEquals(1, preview.unsupported)
        assertTrue(calendar.observeEvents(owner).first().isEmpty())
        assertTrue(dao.findFixedSchedules(owner).isEmpty())
        assertEquals(emptyList<String>(), journal(database, owner))

        val first = async { calendar.importIcs(owner, preview) }
        val second = async { calendar.importIcs(owner, preview) }
        val results = listOf(first.await(), second.await()).sortedByDescending { it.totalNew }
        assertEquals(listOf(IcsImportCounts(3, 1, 1), IcsImportCounts(0, 0, 5)), results)
        val events = calendar.observeEvents(owner).first()
        val ids = events.map { UUID.fromString(it.id) }.toSet()
        assertEquals(3, ids.size)
        assertEquals("Lokasi: Kampus\nAgenda", events.single { it.title == "Libur" }.description)
        assertEquals(14 * 60, dao.findFixedSchedules(owner).single().startMinute)
        assertEquals(listOf("calendar_events", "calendar_events", "calendar_events", "fixed_schedules"),
            journal(database, owner))
        assertEquals(IcsImportCounts(0, 0, 5), calendar.previewIcs(owner, ::fixture).counts)

        assertTrue(runCatching { calendar.importIcs(otherOwner, preview) }.exceptionOrNull() is IllegalArgumentException)
        assertTrue(calendar.observeEvents(otherOwner).first().isEmpty())
        val otherPreview = calendar.previewIcs(otherOwner, ::fixture)
        assertEquals(IcsImportCounts(3, 1, 1), calendar.importIcs(otherOwner, otherPreview))
        assertTrue(calendar.observeEvents(otherOwner).first().none { UUID.fromString(it.id) in ids })
        assertEquals(15 * 60, dao.findFixedSchedules(otherOwner).single().startMinute)
    }

    @Test
    fun failedBatchRollsBackRowsAndJournalAndStaleZoneRejectsPreview() = withDatabase { database ->
        val calendar = CalendarRepository(database)
        val preview = calendar.previewIcs(owner, ::fixture)
        val sqlite = database.openHelper.writableDatabase
        sqlite.execSQL("""
            CREATE TRIGGER reject_ics_test BEFORE INSERT ON FixedSchedule
            BEGIN SELECT RAISE(ABORT, 'test rollback'); END
        """.trimIndent())
        assertTrue(runCatching { calendar.importIcs(owner, preview) }.isFailure)
        assertTrue(calendar.observeEvents(owner).first().isEmpty())
        assertTrue(database.tazerDao().findFixedSchedules(owner).isEmpty())
        assertEquals(emptyList<String>(), journal(database, owner))
        sqlite.execSQL("DROP TRIGGER reject_ics_test")

        val dao = database.tazerDao()
        dao.saveProfile(requireNotNull(dao.findProfile(owner)).copy(zoneId = "Asia/Makassar"))
        assertTrue(runCatching { calendar.importIcs(owner, preview) }.exceptionOrNull() is IllegalArgumentException)
        assertEquals(IcsImportCounts(3, 1, 1), calendar.importIcs(owner, calendar.previewIcs(owner, ::fixture)))
        assertEquals(15 * 60, dao.findFixedSchedules(owner).single().startMinute)
    }

    private fun withDatabase(block: suspend CoroutineScope.(TazerDatabase) -> Unit) = runBlocking {
        val name = "ics-import-$owner.db"
        val database = TazerDatabase.open(context, name)
        try {
            database.tazerDao().saveProfile(UserProfile(owner, "Asia/Jakarta", 480, 1320, null))
            database.tazerDao().saveProfile(UserProfile(otherOwner, "Asia/Makassar", 480, 1320, null))
            block(database)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    private fun journal(database: TazerDatabase, uid: String): List<String> =
        database.openHelper.readableDatabase.query(
            """SELECT kind, version, dirty, tombstone FROM SyncEntry
                WHERE ownerUid = ? AND kind != 'profile' ORDER BY kind""",
            arrayOf(uid),
        ).use { rows ->
            buildList {
                while (rows.moveToNext()) {
                    assertEquals(1, rows.getInt(1))
                    assertEquals(1, rows.getInt(2))
                    assertEquals(0, rows.getInt(3))
                    add(rows.getString(0))
                }
            }
        }

    private fun fixture() = """
        BEGIN:VCALENDAR
        VERSION:2.0
        BEGIN:VEVENT
        UID:holiday
        DTSTART;VALUE=DATE:20261126
        DESCRIPTION:Agenda
        LOCATION:Kampus
        SUMMARY:Libur
        END:VEVENT
        BEGIN:VEVENT
        UID:holiday
        DTSTART;VALUE=DATE:20261126
        SUMMARY:Duplikat
        END:VEVENT
        BEGIN:VEVENT
        UID:seminar
        DTSTART:20261126T070000Z
        RRULE:FREQ=WEEKLY;COUNT=2
        SUMMARY:Seminar
        END:VEVENT
        BEGIN:VEVENT
        UID:class
        DTSTART:20261126T070000Z
        DTEND:20261126T080000Z
        RRULE:FREQ=WEEKLY
        SUMMARY:Kuliah
        END:VEVENT
        BEGIN:VEVENT
        UID:unsupported
        DTSTART:20261126T070000Z
        RRULE:FREQ=DAILY
        END:VEVENT
        END:VCALENDAR
    """.trimIndent().byteInputStream()
}
