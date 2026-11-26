package com.tazer.taskorganizer.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

@Entity(primaryKeys = ["ownerUid", "kind", "rowId"])
internal data class SyncEntry(
    val ownerUid: String,
    val kind: String,
    val rowId: String,
    val version: Long,
    val writer: String,
    val tombstone: Boolean,
    val dirty: Boolean,
)

@Entity
internal data class SyncCursor(@PrimaryKey val ownerUid: String, val changeId: Long)

@Entity
internal data class SyncDevice(@PrimaryKey val id: Int, val writer: String, val applying: Boolean)

internal enum class SyncKind(
    val wire: String,
    val table: String,
    val strings: List<String>,
    val numbers: List<String>,
    val optionalNumbers: Set<String> = emptySet(),
) {
    PROFILE("profile", "UserProfile", listOf("zoneId"),
        listOf("activeStartMinute", "activeEndMinute", "busyUntilAt"), setOf("busyUntilAt")),
    TASK("tasks", "Task", listOf("title", "course", "description", "analysisSource", "status"),
        listOf("deadlineAt", "difficultyScore", "initialEstimatedMinutes", "remainingMinutes", "createdAt", "completedAt", "analyzerBaselineMinutes"),
        setOf("completedAt", "analyzerBaselineMinutes")),
    FIXED("fixed_schedules", "FixedSchedule", listOf("label", "kind"),
        listOf("dayOfWeek", "startMinute", "endMinute")),
    EVENT("calendar_events", "CalendarEvent", listOf("title", "description"),
        listOf("startEpochDay", "endEpochDay", "startMinute"), setOf("startMinute")),
    SUBTASK("subtasks", "Subtask", listOf("taskId", "title"), listOf("completed", "position")),
    SESSION("work_sessions", "WorkSession", listOf("taskId", "state"),
        listOf("plannedStartAt", "plannedEndAt", "plannedMinutes", "startedAt", "endedAt", "actualWorkMs"),
        setOf("startedAt", "endedAt", "actualWorkMs"));

    val idColumn get() = if (this == PROFILE) "ownerUid" else "id"
    val columns get() = strings + numbers

    companion object {
        fun fromWire(value: String) = entries.firstOrNull { it.wire == value }
    }
}

internal object SyncJournal {
    const val ACTIVE = "'RUNNING','OVERTIME','EXTENDED','PAUSED'"
    private const val writer = "(SELECT writer FROM SyncDevice WHERE id = 0)"

    fun install(db: SupportSQLiteDatabase, seed: Boolean) {
        db.execSQL("INSERT OR IGNORE INTO SyncDevice(id,writer,applying) VALUES (0,?,0)", arrayOf(UUID.randomUUID().toString()))
        for (kind in SyncKind.entries) {
            val portable = if (kind == SyncKind.SESSION) " AND state NOT IN ($ACTIVE)" else ""
            if (seed) db.execSQL("""
                INSERT INTO SyncEntry(ownerUid,kind,rowId,version,writer,tombstone,dirty)
                SELECT ownerUid,'${kind.wire}',${kind.idColumn},1,$writer,0,1 FROM ${kind.table}
                WHERE length(ownerUid) = 36$portable
            """.trimIndent())
            val changed = kind.columns.joinToString(" OR ") { "OLD.$it IS NOT NEW.$it" }
            for (event in listOf("INSERT", "UPDATE", "DELETE")) {
                val reference = if (event == "DELETE") "OLD" else "NEW"
                val tombstone = if (event == "DELETE") 1 else 0
                val rowId = "$reference.${kind.idColumn}"
                val owner = "$reference.ownerUid"
                val conditions = buildString {
                    if (kind == SyncKind.SESSION && event != "DELETE") append(" AND NEW.state NOT IN ($ACTIVE)")
                    if (event == "UPDATE") append(" AND ($changed)")
                }
                db.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS sync_${kind.wire}_$event AFTER $event ON ${kind.table}
                    WHEN length($owner) = 36 AND (SELECT applying FROM SyncDevice WHERE id = 0) = 0$conditions
                    BEGIN
                        INSERT OR IGNORE INTO SyncEntry(ownerUid,kind,rowId,version,writer,tombstone,dirty)
                        VALUES ($owner,'${kind.wire}',$rowId,0,$writer,$tombstone,1);
                        UPDATE SyncEntry SET version = version + 1, writer = $writer, tombstone = $tombstone, dirty = 1
                        WHERE ownerUid = $owner AND kind = '${kind.wire}' AND rowId = $rowId;
                    END
                """.trimIndent())
            }
        }
    }
}

internal fun versionWins(version: Long, writer: String, currentVersion: Long, currentWriter: String): Boolean =
    version > currentVersion || (version == currentVersion && writer > currentWriter)
