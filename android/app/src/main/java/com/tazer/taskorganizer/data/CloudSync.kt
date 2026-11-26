package com.tazer.taskorganizer.data

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

internal data class SyncResult(val changed: Boolean, val replan: Boolean, val postponed: Boolean = false)

internal class CloudSync(
    private val database: TazerDatabase,
    private val rpc: suspend (uid: String, name: String, body: JSONObject) -> Any,
) {
    private val lock = Mutex()
    fun queuedChanges(uid: String) = database.invalidationTracker.createFlow("SyncEntry").map {
        database.withTransaction {
            db.query("SELECT 1 FROM SyncEntry WHERE ownerUid=? AND dirty=1 LIMIT 1", arrayOf(uid)).use { it.moveToFirst() }
        }
    }

    suspend fun sync(uid: String): SyncResult = lock.withLock {
        require(uuid(uid))
        val first = pull(uid)
        if (first.postponed) return@withLock first
        val changes = database.withTransaction {
            db.execSQL("""
                INSERT OR IGNORE INTO SyncEntry(ownerUid,kind,rowId,version,writer,tombstone,dirty)
                SELECT ownerUid,'profile',ownerUid,1,(SELECT writer FROM SyncDevice WHERE id=0),0,1
                FROM UserProfile WHERE ownerUid=?
            """.trimIndent(), arrayOf(uid))
            db.pending(uid)
        }
        for (chunk in changes.chunked(12)) {
            val result = rpc(uid, "tazer_sync_push", JSONObject().put("p_rows", JSONArray(chunk.map(Change::wire))))
            require(result is Number && result.toInt() in 0..chunk.size)
            // An edit made while the request was in flight stays queued.
            database.withTransaction {
                for (change in chunk) db.execSQL(
                    "UPDATE SyncEntry SET dirty=0 WHERE ownerUid=? AND kind=? AND rowId=? AND version=? AND writer=?",
                    arrayOf(uid, change.kind.wire, change.id, change.version, change.writer))
            }
        }
        val last = pull(uid)
        SyncResult(first.changed || last.changed, first.replan || last.replan, last.postponed)
    }

    private val db get() = database.openHelper.writableDatabase

    private suspend fun pull(uid: String): SyncResult {
        val initial = database.withTransaction {
            db.query("SELECT changeId FROM SyncCursor WHERE ownerUid=?", arrayOf(uid)).use {
                if (it.moveToFirst()) it.getLong(0) else 0L
            }
        }
        var cursor = initial
        val latest = linkedMapOf<Pair<SyncKind, String>, Change>()
        do {
            val batch = rpc(uid, "tazer_sync_pull", JSONObject().put("p_after", cursor).put("p_limit", PAGE)) as? JSONArray
                ?: error("Invalid sync feed")
            require(batch.length() <= PAGE)
            for (i in 0 until batch.length()) {
                val row = batch.getJSONObject(i)
                val change = Change.parse(row)
                val next = row.getLong("change_id")
                require(next > cursor)
                cursor = next
                latest[change.kind to change.id] = change
            }
        } while (batch.length() == PAGE)
        if (latest.isEmpty()) return SyncResult(false, false)
        return database.withTransaction {
            val winners = latest.values.filter { db.isWinner(uid, it) }
            if (db.protectsActiveSession(uid, winners)) return@withTransaction SyncResult(false, false, true)
            db.execSQL("UPDATE SyncDevice SET applying=1 WHERE id=0")
            try {
                val deletedTasks = winners.filter { it.kind == SyncKind.TASK && it.payload == null }.map { it.id }.toSet()
                winners.filter { it.payload != null }.sortedBy { it.kind.ordinal }.forEach { db.apply(uid, it, deletedTasks) }
                winners.filter { it.payload == null }.sortedByDescending { it.kind.ordinal }.forEach { db.apply(uid, it, deletedTasks) }
                db.execSQL("INSERT OR REPLACE INTO SyncCursor(ownerUid,changeId) VALUES(?,?)", arrayOf(uid, cursor))
            } finally {
                db.execSQL("UPDATE SyncDevice SET applying=0 WHERE id=0")
            }
            SyncResult(winners.isNotEmpty(), winners.any {
                it.kind == SyncKind.PROFILE || it.kind == SyncKind.TASK || it.kind == SyncKind.FIXED
            })
        }
    }

    private fun SupportSQLiteDatabase.pending(uid: String): List<Change> {
        val changes = mutableListOf<Change>()
        query("SELECT kind,rowId,version,writer,tombstone FROM SyncEntry WHERE ownerUid=? AND dirty=1", arrayOf(uid)).use {
            while (it.moveToNext()) {
                val kind = checkNotNull(SyncKind.fromWire(it.getString(0)))
                val id = it.getString(1)
                val tombstone = it.getInt(4) != 0
                val payload = if (tombstone) null else readRow(uid, kind, id) ?: continue
                changes.add(Change(kind, id, it.getLong(2), it.getString(3), payload))
            }
        }
        return changes.sortedWith(compareBy<Change> { it.kind.ordinal }.thenBy { it.id })
    }

    private fun SupportSQLiteDatabase.readRow(uid: String, kind: SyncKind, id: String): JSONObject? {
        val portable = if (kind == SyncKind.SESSION) " AND state NOT IN (${SyncJournal.ACTIVE})" else ""
        val columns = kind.columns
        return query("SELECT ${columns.joinToString()} FROM ${kind.table} WHERE ownerUid=? AND ${kind.idColumn}=?$portable",
            arrayOf(uid, id)).use { rows ->
            if (!rows.moveToFirst()) return@use null
            JSONObject().also { payload ->
                columns.forEachIndexed { i, name ->
                    val value = when (rows.getType(i)) {
                        Cursor.FIELD_TYPE_NULL -> JSONObject.NULL
                        Cursor.FIELD_TYPE_INTEGER -> rows.getLong(i)
                        Cursor.FIELD_TYPE_STRING -> rows.getString(i)
                        else -> error("Invalid local sync field")
                    }
                    payload.put(name, value)
                }
            }
        }
    }

    private fun SupportSQLiteDatabase.isWinner(uid: String, change: Change) =
        query("SELECT version,writer FROM SyncEntry WHERE ownerUid=? AND kind=? AND rowId=?",
            arrayOf(uid, change.kind.wire, change.id)).use {
            !it.moveToFirst() || versionWins(change.version, change.writer, it.getLong(0), it.getString(1))
        }

    private fun SupportSQLiteDatabase.protectsActiveSession(uid: String, changes: List<Change>): Boolean {
        if (changes.isEmpty()) return false
        return query("SELECT id,taskId FROM WorkSession WHERE ownerUid=? AND state IN (${SyncJournal.ACTIVE})",
            arrayOf(uid)).use { rows ->
            while (rows.moveToNext()) {
                val id = rows.getString(0)
                val taskId = rows.getString(1)
                if (changes.any {
                    it.kind == SyncKind.TASK && it.id == taskId ||
                        it.kind == SyncKind.SESSION && it.id == id
                }) return@use true
            }
            false
        }
    }

    private fun SupportSQLiteDatabase.apply(uid: String, change: Change, deletedTasks: Set<String>) {
        val (kind, id, version, writer, payload) = change
        val where = "ownerUid=? AND ${kind.idColumn}=?"
        val args = arrayOf(uid, id)
        if (payload == null) {
            require(kind != SyncKind.PROFILE)
            if (kind == SyncKind.TASK) queueCascade(uid, id)
            delete(kind.table, where, args)
        } else {
            val values = change.values(uid)
            val parent = values.getAsString("taskId")
            val parentExists = parent == null || parent !in deletedTasks &&
                query("SELECT 1 FROM Task WHERE ownerUid=? AND id=?", arrayOf(uid, parent)).use {
                it.moveToFirst()
            }
            if (parentExists) {
                if (kind == SyncKind.SESSION) {
                    val revision = query("SELECT revision FROM WorkSession WHERE ownerUid=? AND id=?", args).use {
                        if (it.moveToFirst()) it.getLong(0) + 1 else 0L
                    }
                    values.put("revision", revision)
                }
                if (update(kind.table, SQLiteDatabase.CONFLICT_ABORT, values, where, args) == 0) {
                    insert(kind.table, SQLiteDatabase.CONFLICT_ABORT, values)
                }
            } else if (parent !in deletedTasks) {
                check(query("SELECT tombstone FROM SyncEntry WHERE ownerUid=? AND kind='tasks' AND rowId=?",
                    arrayOf(uid, parent)).use { it.moveToFirst() && it.getInt(0) != 0 })
            }
        }
        execSQL("INSERT OR REPLACE INTO SyncEntry(ownerUid,kind,rowId,version,writer,tombstone,dirty) VALUES(?,?,?,?,?,?,0)",
            arrayOf<Any>(uid, kind.wire, id, version, writer, if (payload == null) 1 else 0))
    }

    private fun SupportSQLiteDatabase.queueCascade(uid: String, taskId: String) {
        for (child in listOf(SyncKind.SUBTASK, SyncKind.SESSION)) {
            execSQL("""
                UPDATE SyncEntry SET version=version+1, writer=(SELECT writer FROM SyncDevice WHERE id=0), tombstone=1, dirty=1
                WHERE ownerUid=? AND kind=? AND rowId IN (SELECT id FROM ${child.table} WHERE ownerUid=? AND taskId=?)
            """.trimIndent(), arrayOf(uid, child.wire, uid, taskId))
        }
    }

    private data class Change(val kind: SyncKind, val id: String, val version: Long, val writer: String, val payload: JSONObject?) {
        fun wire(): JSONObject = JSONObject().put("kind", kind.wire).put("row_id", id)
            .put("version", version).put("writer", writer).put("payload", payload ?: JSONObject.NULL)

        fun values(uid: String): ContentValues {
            val data = checkNotNull(payload)
            return ContentValues().apply {
                put("ownerUid", uid)
                if (kind == SyncKind.PROFILE) require(id == uid) else put("id", id)
                for (name in kind.strings) {
                    val value = data.get(name) as? String ?: error("Invalid sync text")
                    require(value.length <= 6000)
                    put(name, value)
                }
                for (name in kind.numbers) {
                    // Rows written by older app versions omit fields added later, such as event colours.
                    if (name in kind.optionalNumbers && (!data.has(name) || data.isNull(name))) putNull(name)
                    else put(name, (data.get(name) as? Number ?: error("Invalid sync number")).toLong())
                }
                validate(kind, this)
            }
        }

        companion object {
            fun parse(row: JSONObject): Change {
                val kind = checkNotNull(SyncKind.fromWire(row.getString("kind")))
                val id = row.getString("row_id")
                val writer = row.getString("writer")
                val version = row.getLong("version")
                require(uuid(id) && uuid(writer) && version in 1..9_007_199_254_740_991L)
                val payload = if (row.isNull("payload")) null else row.getJSONObject("payload")
                return Change(kind, id, version, writer, payload)
            }
        }
    }

    companion object {
        private const val PAGE = 24
        fun uuid(value: String): Boolean = runCatching { UUID.fromString(value).toString() == value }.getOrDefault(false)

        private fun validate(kind: SyncKind, row: ContentValues) {
            fun text(name: String) = row.getAsString(name)
            fun number(name: String) = checkNotNull(row.getAsLong(name))
            fun range(start: String, end: String) {
                require(number(start) in 0..1439 && number(end) in 1..1440 && number(start) < number(end))
            }
            when (kind) {
                SyncKind.PROFILE -> {
                    ZoneId.of(text("zoneId"))
                    range("activeStartMinute", "activeEndMinute")
                }
                SyncKind.TASK -> {
                    require(text("title").isNotBlank() && text("course").isNotBlank())
                    TaskStatus.valueOf(text("status"))
                    AnalysisSource.valueOf(text("analysisSource"))
                    require(number("difficultyScore") in 1..5 && number("initialEstimatedMinutes") in 1..10080 &&
                        number("remainingMinutes") in 0..10080)
                }
                SyncKind.FIXED -> {
                    FixedScheduleKind.valueOf(text("kind"))
                    require(number("dayOfWeek") in 1..7)
                    range("startMinute", "endMinute")
                }
                SyncKind.EVENT -> {
                    require(number("startEpochDay") <= number("endEpochDay"))
                    java.time.LocalDate.ofEpochDay(number("startEpochDay"))
                    java.time.LocalDate.ofEpochDay(number("endEpochDay"))
                    require(row.getAsLong("startMinute")?.let { it in 0..1439 } != false)
                    require(row.getAsLong("endMinute")?.let { it in 1..1440 } != false)
                    require(row.getAsLong("colorIndex")?.let { it in 0..9 } != false)
                }
                SyncKind.SUBTASK -> require(uuid(text("taskId")) && text("title").length in 1..120 &&
                    number("completed") in 0..1 && number("position") in 0..Int.MAX_VALUE.toLong())
                SyncKind.SESSION -> require(uuid(text("taskId")) &&
                    text("state") in setOf("PENDING", "DONE", "CANCELLED") && number("plannedEndAt") > number("plannedStartAt") &&
                    number("plannedMinutes") in 1..10080 && row.getAsLong("actualWorkMs")?.let { it >= 0 } != false)
            }
        }
    }
}
