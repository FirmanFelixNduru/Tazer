package com.tazer.taskorganizer.data

import java.time.Instant
import java.time.ZoneId
import java.util.Base64
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class BackupAndPackageTest {
    private val uid = "854359ab-dcb7-4b61-8a94-4a56e153bca1"
    private val task = Task("t1", uid, "Laporan ERD", "Basis Data", "Normalisasi tabel", 1_800_000_000_000, 3, 120, 90,
        AnalysisSource.AI, TaskStatus.PENDING, 1_790_000_000_000, null, 160)
    private val done = task.copy(id = "t2", title = "Kuis SQL", status = TaskStatus.DONE, remainingMinutes = 0,
        completedAt = 1_795_000_000_000, analyzerBaselineMinutes = null)
    private val data = BackupData(
        UserProfile(uid, "Asia/Jakarta", 480, 1320, 1_795_000_000_000),
        listOf(task, done),
        listOf(Subtask("s1", uid, "t1", "Gambar ERD", true, 0), Subtask("s2", uid, "t1", "Tulis laporan", false, 1)),
        listOf(FixedSchedule("f1", uid, "Kuliah Algoritma", FixedScheduleKind.ROUTINE, 1, 480, 600)),
        listOf(CalendarEvent("e1", uid, "UTS", 20_800, 20_800, 540, "Ruang 3", endMinute = 660, colorIndex = 4)),
        listOf(WorkSession("w1", "t2", uid, WorkSessionState.DONE, 1_794_000_000_000, 1_794_003_000_000, 50,
            1_794_000_000_000, 1_794_003_000_000, 2_900_000, "boot-1", 10, 20, 30, 4)),
        mapOf("basis data" to 3),
    )

    @Test
    fun backupRoundTripsEveryPortableField() {
        val decoded = TazerBackup.decode(TazerBackup.encode(data, 1_796_000_000_000, "1.7.0"))
        fun anonymous(row: Task) = row.copy(ownerUid = "")
        assertEquals(data.tasks.map(::anonymous), decoded.tasks)
        assertEquals(data.subtasks.map { it.copy(ownerUid = "") }, decoded.subtasks)
        assertEquals(data.fixedSchedules.map { it.copy(ownerUid = "") }, decoded.fixedSchedules)
        assertEquals(data.events.map { it.copy(ownerUid = "") }, decoded.events)
        assertEquals(UserProfile("", "Asia/Jakarta", 480, 1320, null), decoded.profile)
        assertEquals(mapOf("basis data" to 3), decoded.courseColors)
        val session = decoded.sessions.single()
        assertEquals(Triple("w1", "t2", 2_900_000L), Triple(session.id, session.taskId, session.actualWorkMs))
        // Device timer checkpoints are not portable.
        assertNull(session.timerBootId)
        assertEquals(0L, session.revision)
    }

    @Test
    fun backupsFrom170WithoutEventEndOrColourStillRestore() {
        val old = JSONObject(TazerBackup.encode(data, 1_796_000_000_000, "1.7.0"))
        old.getJSONArray("calendarEvents").getJSONObject(0).apply { remove("endMinute"); remove("colorIndex") }
        val event = TazerBackup.decode(old.toString()).events.single()
        assertNull(event.endMinute)
        assertNull(event.colorIndex)
        old.getJSONArray("calendarEvents").getJSONObject(0).put("colorIndex", 12)
        assertThrows(IllegalArgumentException::class.java) { TazerBackup.decode(old.toString()) }
    }

    @Test
    fun backupContainsNoAccountIdentityOrSecrets() {
        val text = TazerBackup.encode(data, 1_796_000_000_000, "1.7.0")
        val root = JSONObject(text)
        assertEquals("tazer-backup", root.getString("format"))
        assertEquals(1, root.getInt("schemaVersion"))
        assertFalse(text.contains(uid))
        for (word in listOf("ownerUid", "token", "access", "refresh", "apiKey", "api_key", "password", "secret", "bearer", "timerBootId")) {
            assertFalse("backup leaks $word", text.contains(word, ignoreCase = true))
        }
    }

    @Test
    fun backupRejectsForeignNewerOrBrokenFiles() {
        val good = JSONObject(TazerBackup.encode(data, 1_796_000_000_000, "1.7.0"))
        fun rejects(mutate: (JSONObject) -> Unit) {
            val copy = JSONObject(good.toString()).also(mutate)
            assertThrows(IllegalArgumentException::class.java) { TazerBackup.decode(copy.toString()) }
        }
        rejects { it.put("format", "other") }
        rejects { it.put("schemaVersion", 2) }
        rejects { it.getJSONArray("tasks").getJSONObject(0).put("difficultyScore", 6) }
        rejects { it.getJSONArray("tasks").getJSONObject(0).put("title", " ") }
        rejects { it.getJSONArray("subtasks").getJSONObject(0).put("taskId", "missing") }
        rejects { it.getJSONArray("tasks").put(JSONObject(it.getJSONArray("tasks").getJSONObject(0).toString())) }
        rejects { it.getJSONArray("fixedSchedules").getJSONObject(0).put("endMinute", 470) }
        rejects { it.getJSONObject("profile").put("zoneId", "Mars/Base") }
        assertThrows(IllegalArgumentException::class.java) { TazerBackup.decode("{not json") }
        val newer = assertThrows(IllegalArgumentException::class.java) {
            TazerBackup.decode(JSONObject(good.toString()).put("schemaVersion", 9).toString())
        }
        assertEquals("Backup dibuat oleh versi Tazer yang lebih baru.", newer.message)
    }

    @Test
    fun courseColourOverridesIgnoreInvalidEntries() {
        assertEquals(mapOf("basis data" to 3), CourseColorStore.decode("""{"basis data":3,"x":99,"y":"2"}"""))
        assertEquals(emptyMap<String, Int>(), CourseColorStore.decode("broken"))
        assertEquals("""{"a":1,"b":2}""", CourseColorStore.encode(mapOf("b" to 2, "a" to 1)))
    }

    @Test
    fun taskPackageRoundTripsFromSharedTextWithoutIds() {
        val pack = TaskPackages.from(task, data.subtasks.reversed())
        assertEquals(listOf("Gambar ERD", "Tulis laporan"), pack.subtasks)
        val text = TaskPackages.shareText(pack, ZoneId.of("Asia/Jakarta"))
        assertTrue(text.contains("Mata kuliah: Basis Data"))
        assertTrue(text.contains("1. Gambar ERD"))
        assertTrue(text.contains(TaskPackages.LINK))
        assertTrue(text.contains(TaskPackages.PREFIX))
        assertEquals(pack, TaskPackages.decode(text))
        assertEquals(pack, TaskPackages.decode("Halo! " + TaskPackages.LINK + TaskPackages.encode(pack) + " cek ya"))
        val json = String(Base64.getUrlDecoder().decode(TaskPackages.encode(pack)))
        assertFalse(json.contains(uid))
        assertFalse(json.contains("\"t1\""))
        assertEquals(Instant.ofEpochMilli(task.deadlineAt).toString(), JSONObject(json).getString("deadline"))
    }

    @Test
    fun taskPackageRejectsMissingTamperedOrOversizedCodes() {
        fun code(json: String) = TaskPackages.PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(json.toByteArray())
        val valid = """{"v":1,"title":"A","course":"B","description":"","deadline":"2026-12-01T00:00:00Z","difficulty":2,"minutes":30,"subtasks":[]}"""
        assertEquals("A", TaskPackages.decode(code(valid)).title)
        for (broken in listOf(valid.replace("\"v\":1", "\"v\":2"), valid.replace("\"difficulty\":2", "\"difficulty\":7"),
            valid.replace("\"title\":\"A\"", "\"title\":\"\""), valid.replace("2026-12-01T00:00:00Z", "besok"), "{oops")) {
            assertThrows(IllegalArgumentException::class.java) { TaskPackages.decode(code(broken)) }
        }
        assertThrows(IllegalArgumentException::class.java) { TaskPackages.decode("tidak ada kode di sini") }
        assertThrows(IllegalArgumentException::class.java) { TaskPackages.decode(TaskPackages.PREFIX + "A".repeat(30_000)) }
    }
}
