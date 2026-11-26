package com.tazer.taskorganizer.integration

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ImageScanTest {
    private val server = """
        {"title":"Laporan praktikum","course":"Basis Data","description":"Normalisasi tabel",
         "deadlineIso":"2026-12-04T23:59","difficultyScore":3,"estimatedMinutes":120,
         "subtasks":["Baca modul","Gambar ERD","Tulis laporan"],"warnings":["Jam deadline ditebak"]}
    """.trimIndent()

    private fun expectError(expected: IntegrationError, action: () -> Unit) {
        val failure = assertThrows(IntegrationException::class.java) { action() }
        assertEquals(expected, failure.code)
    }

    @Test
    fun downsizesTheLongestEdgeTo1280AndKeepsTheAspectRatio() {
        assertEquals(1280 to 960, scanTargetSize(4000, 3000))
        assertEquals(720 to 1280, scanTargetSize(1080, 1920))
        assertEquals(800 to 600, scanTargetSize(800, 600))
        assertEquals(1280 to 1, scanTargetSize(20000, 10))
        assertEquals(2, scanSampleSize(4000, 3000))
        assertEquals(1, scanSampleSize(2000, 1000))
        assertEquals(1, scanSampleSize(800, 600))
        assertEquals(4, scanSampleSize(6000, 4000))
        // The sampled bitmap never drops below the target size before the final scale.
        listOf(1281 to 10, 2560 to 1440, 9000 to 9000).forEach { (w, h) ->
            assertTrue(maxOf(w, h) / scanSampleSize(w, h) >= 1280)
        }
    }

    @Test
    fun buildsABase64JsonBodyWithLocalContextWithinTheLimit() {
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 1, 2, 3)
        val body = scanBody(jpeg, LocalDate.of(2026, 11, 26), ZoneId.of("Asia/Jakarta"))
        assertEquals(setOf("mimeType", "imageBase64", "localDate", "timeZone"), body.keys().asSequence().toSet())
        assertEquals("image/jpeg", body.getString("mimeType"))
        assertArrayEquals(jpeg, Base64.getDecoder().decode(body.getString("imageBase64")))
        assertEquals("2026-11-26", body.getString("localDate"))
        assertEquals("Asia/Jakarta", body.getString("timeZone"))
        scanBody(ByteArray(SCAN_MAX_BYTES), LocalDate.of(2026, 1, 1), ZoneId.of("UTC"))
        expectError(IntegrationError.PAYLOAD_TOO_LARGE) { scanBody(ByteArray(SCAN_MAX_BYTES + 1), LocalDate.now(), ZoneId.of("UTC")) }
        expectError(IntegrationError.PAYLOAD_TOO_LARGE) { scanBody(ByteArray(0), LocalDate.now(), ZoneId.of("UTC")) }
    }

    @Test
    fun parsesEveryFieldOfTheServerExtraction() {
        val result = parseJson(server).imageExtraction()
        assertEquals("Laporan praktikum", result.title)
        assertEquals("Basis Data", result.course)
        assertEquals(LocalDateTime.of(2026, 12, 4, 23, 59), result.deadline)
        assertEquals(3, result.difficultyScore)
        assertEquals(120, result.estimatedMinutes)
        assertEquals(listOf("Baca modul", "Gambar ERD", "Tulis laporan"), result.subtasks)
        assertEquals(listOf("Jam deadline ditebak"), result.warnings)
        val absent = JSONObject(server).put("deadlineIso", JSONObject.NULL)
        assertNull(absent.imageExtraction().deadline)
        assertNull(JSONObject(server).put("deadlineIso", "").imageExtraction().deadline)
    }

    @Test
    fun keepsReadableFieldsWhenOptionalPartsAreMalformed() {
        val fuzzy = JSONObject(server)
            .put("deadlineIso", "besok sore")
            .put("subtasks", JSONArray(listOf("hanya satu")))
            .put("warnings", JSONArray(listOf("  Teks buram ", 7, "")))
            .imageExtraction()
        assertNull(fuzzy.deadline)
        assertTrue(fuzzy.subtasks.isEmpty())
        assertEquals("Teks buram", fuzzy.warnings.first())
        assertTrue(fuzzy.warnings.last().contains("deadline"))
        assertNull(JSONObject(server).put("deadlineIso", "2026-02-30T10:00").imageExtraction().deadline)
    }

    @Test
    fun rejectsOutputOutsideTheContract() {
        expectError(IntegrationError.INVALID_RESPONSE) { JSONObject(server).put("title", " ").imageExtraction() }
        expectError(IntegrationError.INVALID_RESPONSE) { JSONObject(server).put("title", "x".repeat(121)).imageExtraction() }
        expectError(IntegrationError.INVALID_RESPONSE) { JSONObject(server).put("difficultyScore", 6).imageExtraction() }
        expectError(IntegrationError.INVALID_RESPONSE) { JSONObject(server).put("estimatedMinutes", 1.5).imageExtraction() }
        expectError(IntegrationError.INVALID_RESPONSE) { JSONObject(server).put("course", 5).imageExtraction() }
        expectError(IntegrationError.INVALID_RESPONSE) { JSONObject(server).put("leaked", "x").imageExtraction() }
    }

    @Test
    fun backendErrorsMapToScanFriendlyCodes() {
        expectError(IntegrationError.PAYLOAD_TOO_LARGE) {
            HttpResponse(413, """{"error":{"code":"PAYLOAD_TOO_LARGE","message":"x"}}""", null).backendJson()
        }
        expectError(IntegrationError.VALIDATION_ERROR) {
            HttpResponse(400, """{"error":{"code":"VALIDATION_ERROR","message":"x"}}""", null).backendJson()
        }
        assertEquals("Laporan praktikum", HttpResponse(200, server, null).backendJson().imageExtraction().title)
    }

    @Test
    fun readsGeminiAnswerTextWithoutThoughtParts() {
        val payload = JSONObject("""
            {"candidates":[{"finishReason":"STOP","content":{"parts":[{"text":"plan","thought":true},{"text":"{\"a\":"},{"text":"1}"}]}}]}
        """.trimIndent())
        assertEquals("{\"a\":1}", geminiText(payload))
        expectError(IntegrationError.AI_UNAVAILABLE) {
            geminiText(JSONObject("""{"candidates":[{"finishReason":"SAFETY"}]}"""))
        }
    }
}
