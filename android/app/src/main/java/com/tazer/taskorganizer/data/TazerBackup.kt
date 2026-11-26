package com.tazer.taskorganizer.data

import androidx.room.withTransaction
import androidx.sqlite.db.SimpleSQLiteQuery
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Account content without identity: rows carry no ownerUid, and the format has no place for tokens or API keys.
 * Rows decoded from a file use an empty ownerUid until a restore assigns the signed-in account.
 */
data class BackupData(
    val profile: UserProfile?,
    val tasks: List<Task>,
    val subtasks: List<Subtask>,
    val fixedSchedules: List<FixedSchedule>,
    val events: List<CalendarEvent>,
    val sessions: List<WorkSession>,
    val courseColors: Map<String, Int>,
)

data class BackupCounts(
    val tasks: Int, val subtasks: Int, val fixedSchedules: Int, val events: Int, val sessions: Int, val duplicates: Int,
) {
    val totalNew get() = tasks + subtasks + fixedSchedules + events + sessions
}

data class BackupPreview(val ownerUid: String, val data: BackupData, val counts: BackupCounts, val profileChanges: Boolean)

object TazerBackup {
    const val FORMAT = "tazer-backup"
    const val SCHEMA_VERSION = 1
    const val MAX_BYTES = 8 * 1024 * 1024
    private const val MAX_ROWS = 20_000

    fun encode(data: BackupData, exportedAt: Long, appVersion: String): String = JSONObject().apply {
        put("format", FORMAT)
        put("schemaVersion", SCHEMA_VERSION)
        put("appVersion", appVersion)
        put("exportedAt", Instant.ofEpochMilli(exportedAt).toString())
        data.profile?.let { put("profile", obj("zoneId" to it.zoneId, "activeStartMinute" to it.activeStartMinute, "activeEndMinute" to it.activeEndMinute)) }
        put("tasks", JSONArray(data.tasks.map {
            obj("id" to it.id, "title" to it.title, "course" to it.course, "description" to it.description,
                "deadlineAt" to it.deadlineAt, "difficultyScore" to it.difficultyScore,
                "initialEstimatedMinutes" to it.initialEstimatedMinutes, "remainingMinutes" to it.remainingMinutes,
                "analysisSource" to it.analysisSource.name, "status" to it.status.name, "createdAt" to it.createdAt,
                "completedAt" to it.completedAt, "analyzerBaselineMinutes" to it.analyzerBaselineMinutes)
        }))
        put("subtasks", JSONArray(data.subtasks.map {
            obj("id" to it.id, "taskId" to it.taskId, "title" to it.title, "completed" to it.completed, "position" to it.position)
        }))
        put("fixedSchedules", JSONArray(data.fixedSchedules.map {
            obj("id" to it.id, "label" to it.label, "kind" to it.kind.name, "dayOfWeek" to it.dayOfWeek,
                "startMinute" to it.startMinute, "endMinute" to it.endMinute)
        }))
        put("calendarEvents", JSONArray(data.events.map {
            obj("id" to it.id, "title" to it.title, "startEpochDay" to it.startEpochDay, "endEpochDay" to it.endEpochDay,
                "startMinute" to it.startMinute, "description" to it.description)
        }))
        put("workSessions", JSONArray(data.sessions.map {
            obj("id" to it.id, "taskId" to it.taskId, "plannedStartAt" to it.plannedStartAt, "plannedEndAt" to it.plannedEndAt,
                "plannedMinutes" to it.plannedMinutes, "startedAt" to it.startedAt, "endedAt" to it.endedAt,
                "actualWorkMs" to it.actualWorkMs)
        }))
        put("courseColors", JSONObject(data.courseColors.toSortedMap()))
    }.toString(2)

    /** Parses and validates a whole file; any defect rejects it with [IllegalArgumentException]. */
    fun decode(text: String): BackupData = try {
        require(text.length <= MAX_BYTES)
        val root = JSONObject(text)
        require(root.optString("format") == FORMAT) { "Bukan file backup Tazer." }
        val version = root.optInt("schemaVersion", -1)
        require(version in 1..SCHEMA_VERSION) { "Backup dibuat oleh versi Tazer yang lebih baru." }
        val profile = root.optJSONObject("profile")?.let {
            val zone = it.getString("zoneId")
            require(zone in ZoneId.getAvailableZoneIds())
            val start = it.getInt("activeStartMinute")
            val end = it.getInt("activeEndMinute")
            require(start in 0 until end && end <= 1440)
            UserProfile("", zone, start, end, null)
        }
        val tasks = rows(root, "tasks") {
            val status = TaskStatus.valueOf(it.getString("status"))
            val remaining = it.getInt("remainingMinutes")
            Task(id(it), "", text(it, "title", 1, 120), text(it, "course", 1, 120), text(it, "description", 0, 4000),
                it.getLong("deadlineAt"), it.getInt("difficultyScore").also { score -> require(score in 1..5) },
                it.getInt("initialEstimatedMinutes").also { minutes -> require(minutes in 1..10080) },
                remaining.also { require(if (status == TaskStatus.DONE) it in 0..10080 else it in 1..10080) },
                AnalysisSource.valueOf(it.getString("analysisSource")), status, it.getLong("createdAt"),
                long(it, "completedAt").also { done -> require((status == TaskStatus.DONE) == (done != null)) },
                int(it, "analyzerBaselineMinutes")?.also { minutes -> require(minutes in 1..10080) })
        }
        val subtasks = rows(root, "subtasks") {
            Subtask(id(it), "", it.getString("taskId"), text(it, "title", 1, 120), it.getBoolean("completed"),
                it.getInt("position").also { position -> require(position in 0..39) })
        }
        val fixed = rows(root, "fixedSchedules") {
            val start = it.getInt("startMinute")
            val end = it.getInt("endMinute")
            require(start in 0 until end && end <= 1440)
            FixedSchedule(id(it), "", text(it, "label", 1, 120), FixedScheduleKind.valueOf(it.getString("kind")),
                it.getInt("dayOfWeek").also { day -> require(day in 1..7) }, start, end)
        }
        val events = rows(root, "calendarEvents") {
            val start = it.getLong("startEpochDay")
            val end = it.getLong("endEpochDay")
            require(start <= end)
            CalendarEvent(id(it), "", text(it, "title", 1, 500), start, end,
                int(it, "startMinute")?.also { minute -> require(minute in 0..1439) }, text(it, "description", 0, 10_000))
        }
        val sessions = rows(root, "workSessions") {
            val start = it.getLong("plannedStartAt")
            val end = it.getLong("plannedEndAt")
            require(start < end)
            WorkSession(id(it), it.getString("taskId"), "", WorkSessionState.DONE, start, end,
                it.getInt("plannedMinutes").also { minutes -> require(minutes in 1..10080) },
                long(it, "startedAt"), long(it, "endedAt"), long(it, "actualWorkMs")?.also { ms -> require(ms >= 0) },
                null, null, null, null, 0L)
        }
        require(tasks.size + subtasks.size + fixed.size + events.size + sessions.size <= MAX_ROWS)
        require(listOf(tasks.map { it.id }, subtasks.map { it.id }, fixed.map { it.id }, events.map { it.id },
            sessions.map { it.id }).all { it.size == it.toSet().size })
        val taskIds = tasks.mapTo(mutableSetOf()) { it.id }
        require(subtasks.all { it.taskId in taskIds } && sessions.all { it.taskId in taskIds })
        BackupData(profile, tasks, subtasks, fixed, events, sessions,
            CourseColorStore.decode(root.optJSONObject("courseColors")?.toString()))
    } catch (failure: JSONException) {
        throw IllegalArgumentException("Backup tidak valid.", failure)
    }

    private fun obj(vararg fields: Pair<String, Any?>) =
        JSONObject().apply { fields.forEach { (key, value) -> put(key, value ?: JSONObject.NULL) } }

    private fun <T> rows(root: JSONObject, key: String, read: (JSONObject) -> T): List<T> {
        val array = root.optJSONArray(key) ?: return emptyList()
        require(array.length() <= MAX_ROWS)
        return (0 until array.length()).map { read(array.getJSONObject(it)) }
    }

    private fun id(json: JSONObject) = json.getString("id").also { require(it.length in 1..64 && it.isNotBlank()) }

    private fun text(json: JSONObject, key: String, min: Int, max: Int): String {
        val value = json.getString(key).trim()
        require(value.codePointCount(0, value.length) in min..max && '\u0000' !in value)
        return value
    }

    private fun long(json: JSONObject, key: String): Long? = if (json.isNull(key)) null else json.getLong(key)
    private fun int(json: JSONObject, key: String): Int? = if (json.isNull(key)) null else json.getInt(key)
}

/** Exports the signed-in account and merges a backup into it without touching the session. */
class BackupRepository(private val database: TazerDatabase) {
    private val dao = database.tazerDao()
    private val calendar = database.calendarDao()
    private val subtasks = database.subtaskDao()

    suspend fun export(uid: String, courseColors: Map<String, Int>): BackupData = database.withTransaction {
        BackupData(
            dao.findProfile(uid),
            dao.observeTasks(uid).first(),
            subtasks.observe(uid).first(),
            dao.findFixedSchedules(uid),
            calendar.observeEvents(uid).first(),
            // Only finished history is portable; plans are rebuilt and live timers stay on this device.
            dao.observeWorkSessions(uid).first().filter { it.state == WorkSessionState.DONE },
            courseColors,
        )
    }

    suspend fun preview(uid: String, data: BackupData): BackupPreview = database.withTransaction {
        val plan = plan(uid, data)
        val current = dao.findProfile(uid)
        BackupPreview(uid, data, plan.counts, data.profile != null && current != null &&
            data.profile.copy(ownerUid = uid, busyUntilAt = current.busyUntilAt) != current)
    }

    suspend fun restore(uid: String, preview: BackupPreview): BackupCounts = database.withTransaction {
        require(uid == preview.ownerUid)
        val plan = plan(uid, preview.data)
        val current = requireNotNull(dao.findProfile(uid))
        preview.data.profile?.let { dao.saveProfile(it.copy(ownerUid = uid, busyUntilAt = current.busyUntilAt)) }
        plan.tasks.forEach { dao.insertTask(it) }
        plan.subtasks.forEach { subtasks.put(it) }
        plan.fixed.forEach { dao.insertFixedSchedule(it) }
        plan.events.forEach { calendar.insertEvent(it) }
        if (plan.sessions.isNotEmpty()) dao.insertSessions(plan.sessions)
        plan.counts
    }

    private class Plan(
        val tasks: List<Task>, val subtasks: List<Subtask>, val fixed: List<FixedSchedule>,
        val events: List<CalendarEvent>, val sessions: List<WorkSession>, val counts: BackupCounts,
    )

    /**
     * Keeps original ids so a later Cloud Sync pull merges with restored rows. A row this account already has is a
     * duplicate; an id owned by another account on this device gets a stable derived id instead.
     */
    private fun plan(uid: String, data: BackupData): Plan {
        var duplicates = 0
        /** The id this row has in the account, and whether it still needs inserting. */
        fun resolve(table: String, id: String): Pair<String, Boolean> {
            val derived = UUID.nameUUIDFromBytes("tazer-backup:$table:${uid.length}:$uid:$id".toByteArray()).toString()
            val owner = owner(table, id)
            return when {
                owner == uid -> id to false
                owner(table, derived) == uid -> derived to false
                owner == null -> id to true
                else -> derived to true
            }.also { if (!it.second) duplicates++ }
        }
        val taskIds = mutableMapOf<String, String>()
        val tasks = data.tasks.mapNotNull { task ->
            val (id, insert) = resolve("Task", task.id)
            taskIds[task.id] = id
            task.copy(id = id, ownerUid = uid).takeIf { insert }
        }
        val newSubtasks = data.subtasks.mapNotNull { row ->
            val (id, insert) = resolve("Subtask", row.id)
            row.copy(id = id, ownerUid = uid, taskId = taskIds.getValue(row.taskId)).takeIf { insert }
        }
        val fixed = data.fixedSchedules.mapNotNull { row ->
            val (id, insert) = resolve("FixedSchedule", row.id)
            row.copy(id = id, ownerUid = uid).takeIf { insert }
        }
        val events = data.events.mapNotNull { row ->
            val (id, insert) = resolve("CalendarEvent", row.id)
            row.copy(id = id, ownerUid = uid).takeIf { insert }
        }
        val sessions = data.sessions.mapNotNull { row ->
            val (id, insert) = resolve("WorkSession", row.id)
            row.copy(id = id, ownerUid = uid, taskId = taskIds.getValue(row.taskId)).takeIf { insert }
        }
        return Plan(tasks, newSubtasks, fixed, events, sessions,
            BackupCounts(tasks.size, newSubtasks.size, fixed.size, events.size, sessions.size, duplicates))
    }

    private fun owner(table: String, id: String): String? =
        database.query(SimpleSQLiteQuery("SELECT ownerUid FROM `$table` WHERE id = ?", arrayOf(id))).use {
            if (it.moveToFirst()) it.getString(0) else null
        }
}
