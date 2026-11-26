package com.tazer.taskorganizer.data

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CloudSyncTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val uid = UUID.randomUUID().toString()
    private val other = UUID.randomUUID().toString()

    @Test
    fun queuesOfflineChangesAcrossCloseAndReplaysSixEntityTypesAndTombstones() = runBlocking {
        val aName = "sync-a-$uid.db"
        val bName = "sync-b-$uid.db"
        val remote = Remote()
        try {
            val taskId = TazerDatabase.open(context, aName).useDb { first ->
                val tasks = TaskRepository(first)
                val calendar = CalendarRepository(first)
                val subtasks = SubtaskRepository(first)
                tasks.saveProfile(uid, "Asia/Jakarta", 480, 1320)
                tasks.saveProfile(other, "Asia/Makassar", 480, 1320)
                val task = tasks.createTask(uid, "Offline", "Algoritma", "", 10_000L, 3, 30, 0L,
                    analyzerBaselineMinutes = 25)
                tasks.createTask(other, "Privat", "Algoritma", "", 10_000L, 3, 30, 0L)
                subtasks.replace(uid, task.id, listOf(SubtaskDraft(null, "Uji", true)))
                assertFalse(subtasks.save(other, task.id, null, "Tolak"))
                tasks.addFixedSchedule(uid, "Kuliah", FixedScheduleKind.ROUTINE, 1, 500, 600)
                calendar.saveEvent(uid, null, "Ujian", 20_000L, 20_002L, null, "")
                first.tazerDao().insertSession(WorkSession(
                    UUID.randomUUID().toString(), task.id, uid, WorkSessionState.DONE,
                    0L, 1_800_000L, 30, 1L, 1_200_001L, 1_200_000L, "desktop-private", 1L, 2L, 3L, 5L,
                ))
                remote.offline = true
                assertTrue(runCatching { CloudSync(first, remote::call).sync(uid) }.exceptionOrNull() is IOException)
                task.id
            }
            TazerDatabase.open(context, aName).useDb { first ->
                TazerDatabase.open(context, bName).useDb { second ->
                    remote.offline = false
                    val from = CloudSync(first, remote::call)
                    val to = CloudSync(second, remote::call)
                    from.sync(uid)
                    TaskRepository(second).createDefaultProfile(uid, "Asia/Makassar")
                    assertTrue(to.sync(uid).replan)
                    assertEquals("Asia/Jakarta", second.tazerDao().findProfile(uid)?.zoneId)
                    assertEquals(25, second.tazerDao().findTask(uid, taskId)?.analyzerBaselineMinutes)
                    assertEquals("Uji", SubtaskRepository(second).observe(uid).first().single().title)
                    assertEquals("Kuliah", second.tazerDao().findFixedSchedules(uid).single().label)
                    assertEquals("Ujian", CalendarRepository(second).observeEvents(uid).first().single().title)
                    val session = second.tazerDao().observeWorkSessions(uid).first().single()
                    assertEquals(1_200_000L, session.actualWorkMs)
                    assertNull(session.timerBootId)
                    assertTrue(remote.rows.keys.none { it.first == other })
                    TaskRepository(first).deleteTask(uid, taskId)
                    from.sync(uid)
                    to.sync(uid)
                    assertNull(second.tazerDao().findTask(uid, taskId))
                    assertTrue(SubtaskRepository(second).observe(uid).first().isEmpty())
                    assertTrue(second.tazerDao().observeWorkSessions(uid).first().isEmpty())
                }
            }
        } finally {
            context.deleteDatabase(aName)
            context.deleteDatabase(bName)
        }
    }

    @Test
    fun acknowledgementCannotClearAnEditMadeDuringARequest() = runBlocking {
        val name = "sync-race-$uid.db"
        try {
            TazerDatabase.open(context, name).useDb { db ->
                val repository = TaskRepository(db)
                repository.saveProfile(uid, "Asia/Jakarta", 480, 1320)
                val task = repository.createTask(uid, "Before", "Course", "", 1000L, 1, 25, 0L)
                val remote = Remote()
                val sync = CloudSync(db, remote::call)
                remote.duringPush = {
                    assertTrue(repository.updateTask(uid, task.id, "During", "Course", "", 1000L, 1, 25,
                        AnalysisSource.OFFLINE))
                }
                sync.sync(uid)
                assertEquals("Before", remote.rows[uid to ("tasks" to task.id)]?.getJSONObject("payload")?.getString("title"))
                sync.sync(uid)
                assertEquals("During", remote.rows[uid to ("tasks" to task.id)]?.getJSONObject("payload")?.getString("title"))
            }
        } finally {
            context.deleteDatabase(name)
        }
    }

    @Test
    fun migratesExportedRoomV3WithoutRemovingRowsOrPauseState() = runBlocking {
        val name = "sync-migrate-$uid.db"
        val taskId = UUID.randomUUID().toString()
        val sessionId = UUID.randomUUID().toString()
        try {
            context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { db ->
                db.setForeignKeyConstraintsEnabled(true)
                instrumentation.context.assets.open("tazer-v3.sql").bufferedReader().useLines { lines ->
                    lines.forEach { db.execSQL(it) }
                }
                db.execSQL("INSERT INTO UserProfile VALUES (?, 'Asia/Jakarta',480,1320,NULL)", arrayOf(uid))
                db.execSQL("INSERT INTO Task VALUES (?,?,'Existing','Course','',900000,2,25,25,'OFFLINE','IN_PROGRESS',0,NULL)",
                    arrayOf(taskId, uid))
                db.execSQL("""
                    INSERT INTO WorkSession(id,taskId,ownerUid,state,plannedStartAt,plannedEndAt,plannedMinutes,revision,pausedRemainingMs,pausedFromState)
                    VALUES (?, ?, ?, 'PAUSED', 0, 1500000, 25, 7, 123456, 'RUNNING')
                """.trimIndent(), arrayOf(sessionId, taskId, uid))
                db.execSQL("INSERT INTO CalendarEvent VALUES (?,?, 'Existing event',20000,20001,NULL,'')",
                    arrayOf(UUID.randomUUID().toString(), uid))
                db.version = 3
            }
            TazerDatabase.open(context, name).useDb { db ->
                assertEquals("Existing", db.tazerDao().findTask(uid, taskId)?.title)
                assertEquals(123456L, db.tazerDao().findFocusSession(uid, sessionId)?.pausedRemainingMs)
                assertEquals("Existing event", CalendarRepository(db).observeEvents(uid).first().single().title)
                assertTrue(SubtaskRepository(db).observe(uid).first().isEmpty())
                val remote = Remote()
                CloudSync(db, remote::call).sync(uid)
                assertEquals(setOf("tasks", "profile", "calendar_events"), remote.rows.keys.map { it.second.first }.toSet())
                assertEquals(5, db.openHelper.writableDatabase.version)
            }
        } finally {
            context.deleteDatabase(name)
        }
    }

    @Test
    fun eventEndTimeAndColourSyncIncludingColourOnlyEditsAndOlderPayloads() = runBlocking {
        val name = "sync-event-v5-$uid.db"
        val eventId = UUID.randomUUID().toString()
        try {
            context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { db ->
                db.setForeignKeyConstraintsEnabled(true)
                instrumentation.context.assets.open("tazer-v3.sql").bufferedReader().useLines { lines ->
                    lines.forEach { db.execSQL(it) }
                }
                db.execSQL("INSERT INTO UserProfile VALUES (?, 'Asia/Jakarta',480,1320,NULL)", arrayOf(uid))
                db.execSQL("INSERT INTO CalendarEvent VALUES (?,?, 'Seminar',20000,20000,540,'')", arrayOf(eventId, uid))
                db.version = 3
            }
            TazerDatabase.open(context, name).useDb { db ->
                assertEquals(5, db.openHelper.writableDatabase.version)
                val calendar = CalendarRepository(db)
                val migrated = calendar.observeEvents(uid).first().single()
                assertNull(migrated.endMinute)
                assertNull(migrated.colorIndex)
                val remote = Remote()
                val sync = CloudSync(db, remote::call)
                val key = uid to ("calendar_events" to eventId)
                sync.sync(uid)

                assertTrue(calendar.saveEvent(uid, eventId, "Seminar", 20000, 20000, 540, "", 600, null))
                sync.sync(uid)
                assertEquals(600, remote.rows.getValue(key).getJSONObject("payload").getInt("endMinute"))

                // A colour-only edit must still reach the journal after the v5 trigger rebuild.
                assertTrue(calendar.saveEvent(uid, eventId, "Seminar", 20000, 20000, 540, "", 600, 3))
                sync.sync(uid)
                assertEquals(3, remote.rows.getValue(key).getJSONObject("payload").getInt("colorIndex"))

                // An older app version writes the event without the 1.8.0 fields.
                val older = JSONObject(remote.rows.getValue(key).getJSONObject("payload").toString())
                    .apply { remove("endMinute"); remove("colorIndex"); put("title", "Seminar lama") }
                remote.inject(uid, "calendar_events", eventId, older, version = 99, writer = UUID.randomUUID().toString())
                sync.sync(uid)
                val pulled = calendar.observeEvents(uid).first().single()
                assertEquals("Seminar lama", pulled.title)
                assertNull(pulled.endMinute)
                assertNull(pulled.colorIndex)
            }
        } finally {
            context.deleteDatabase(name)
        }
    }

    private suspend fun <T> TazerDatabase.useDb(block: suspend (TazerDatabase) -> T): T =
        try { block(this) } finally { close() }

    private class Remote {
        val rows = linkedMapOf<Pair<String, Pair<String, String>>, JSONObject>()
        var offline = false
        var duringPush: (suspend () -> Unit)? = null
        private var cursor = 0L

        fun inject(uid: String, kind: String, id: String, payload: JSONObject, version: Long, writer: String) {
            rows[uid to (kind to id)] = JSONObject().put("kind", kind).put("row_id", id).put("version", version)
                .put("writer", writer).put("payload", payload).put("change_id", ++cursor)
        }

        suspend fun call(uid: String, name: String, body: JSONObject): Any {
            if (offline) throw IOException("Offline fixture")
            if (name == "tazer_sync_pull") {
                return JSONArray(rows.filterKeys { it.first == uid }.values.filter { it.getLong("change_id") > body.getLong("p_after") }
                    .sortedBy { it.getLong("change_id") }.take(body.getInt("p_limit")))
            }
            check(name == "tazer_sync_push")
            val batch = body.getJSONArray("p_rows")
            val action = duringPush.also { duringPush = null }
            action?.invoke()
            var accepted = 0
            for (i in 0 until batch.length()) {
                val row = batch.getJSONObject(i)
                val key = uid to (row.getString("kind") to row.getString("row_id"))
                val existing = rows[key]
                if (existing == null || versionWins(row.getLong("version"), row.getString("writer"),
                    existing.getLong("version"), existing.getString("writer"))) {
                    rows[key] = JSONObject(row.toString()).put("change_id", ++cursor)
                    accepted++
                }
            }
            return accepted
        }
    }
}
