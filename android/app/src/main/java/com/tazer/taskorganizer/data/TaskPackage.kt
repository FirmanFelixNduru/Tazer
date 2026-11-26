package com.tazer.taskorganizer.data

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.Locale
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** A task to share with group mates. It holds no ids, account data, or progress. */
data class TaskPackage(
    val title: String,
    val course: String,
    val description: String,
    val deadlineAt: Long,
    val difficultyScore: Int,
    val estimatedMinutes: Int,
    val subtasks: List<String>,
)

object TaskPackages {
    const val PREFIX = "TAZER-TASK-V1:"
    const val LINK = "tazer://import-task?d="
    private const val MAX_CODE = 24 * 1024
    private val code = Regex("""TAZER-TASK-V1:([A-Za-z0-9_-]+)""")
    // Shared links always use tazer://; the debug build opens its own tazer-debug:// links too.
    private val link = Regex("""tazer(?:-debug)?://import-task\?d=([A-Za-z0-9_-]+)""")
    private val deadlineFormat = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy HH:mm", Locale.forLanguageTag("id-ID"))

    fun from(task: Task, subtasks: List<Subtask>) = TaskPackage(
        task.title, task.course, task.description, task.deadlineAt, task.difficultyScore, task.initialEstimatedMinutes,
        subtasks.sortedBy(Subtask::position).map(Subtask::title).take(40),
    )

    fun encode(task: TaskPackage): String {
        val json = JSONObject()
            .put("v", 1)
            .put("title", task.title)
            .put("course", task.course)
            .put("description", task.description)
            .put("deadline", Instant.ofEpochMilli(task.deadlineAt).toString())
            .put("difficulty", task.difficultyScore)
            .put("minutes", task.estimatedMinutes)
            .put("subtasks", JSONArray(task.subtasks))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.toString().toByteArray(Charsets.UTF_8))
    }

    /** Readable summary for chat apps, followed by the tap-to-import link and the paste code. */
    fun shareText(task: TaskPackage, zone: ZoneId): String {
        val encoded = encode(task)
        return buildString {
            appendLine("📚 ${task.title}")
            appendLine("Mata kuliah: ${task.course}")
            appendLine("Deadline: ${deadlineFormat.format(Instant.ofEpochMilli(task.deadlineAt).atZone(zone))}")
            appendLine("Kesulitan ${task.difficultyScore}/5 · Estimasi ${task.estimatedMinutes} menit")
            if (task.description.isNotBlank()) appendLine().appendLine(task.description.trim())
            if (task.subtasks.isNotEmpty()) {
                appendLine().appendLine("Langkah:")
                task.subtasks.forEachIndexed { index, step -> appendLine("${index + 1}. $step") }
            }
            appendLine().appendLine("Impor ke Tazer: $LINK$encoded")
            append(PREFIX).append(encoded)
        }
    }

    /** Finds a code or link anywhere in pasted text and validates it like the task form does. */
    fun decode(text: String): TaskPackage {
        val encoded = (code.find(text) ?: link.find(text))?.groupValues?.get(1)
        require(encoded != null && encoded.length <= MAX_CODE) { "Kode paket tugas tidak ditemukan." }
        try {
            val json = JSONObject(String(Base64.getUrlDecoder().decode(encoded), Charsets.UTF_8))
            require(json.getInt("v") == 1) { "Paket tugas dibuat oleh versi Tazer yang lebih baru." }
            fun field(key: String, max: Int, min: Int = 1) = json.getString(key).trim()
                .also { require(it.codePointCount(0, it.length) in min..max && '\u0000' !in it) }
            val steps = json.optJSONArray("subtasks") ?: JSONArray()
            require(steps.length() <= 40)
            return TaskPackage(
                field("title", 120), field("course", 120), field("description", 4000, 0),
                Instant.parse(json.getString("deadline")).toEpochMilli(),
                json.getInt("difficulty").also { require(it in 1..5) },
                json.getInt("minutes").also { require(it in 1..10080) },
                (0 until steps.length()).map { index ->
                    steps.getString(index).trim().also { require(it.codePointCount(0, it.length) in 1..120) }
                },
            )
        } catch (failure: JSONException) {
            throw IllegalArgumentException("Paket tugas rusak.", failure)
        } catch (failure: java.time.DateTimeException) {
            throw IllegalArgumentException("Paket tugas rusak.", failure)
        }
    }
}
