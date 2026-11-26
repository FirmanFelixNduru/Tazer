package com.tazer.taskorganizer.domain

import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class DailyBriefingTest {
    private val jakarta = ZoneId.of("Asia/Jakarta")
    private fun at(text: String, zone: ZoneId = jakarta) =
        LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()

    private fun task(id: String, title: String, deadline: String, remaining: Int = 60, completed: String? = null) =
        BriefingTask(id, title, at(deadline), at("2026-11-01T08:00"), remaining, 3, completed?.let(::at))

    @Test
    fun nextTriggerIsTheNextLocalOccurrenceStrictlyAfterNow() {
        assertEquals(at("2026-11-26T07:00"), DailyBriefing.nextTrigger(at("2026-11-26T06:59"), jakarta, 420))
        assertEquals(at("2026-11-27T07:00"), DailyBriefing.nextTrigger(at("2026-11-26T07:00"), jakarta, 420))
        assertEquals(at("2026-11-27T07:00"), DailyBriefing.nextTrigger(at("2026-11-26T21:30"), jakarta, 420))
        assertEquals(at("2026-11-26T21:00"), DailyBriefing.nextTrigger(at("2026-11-26T07:00"), jakarta, 1260))
        assertEquals(at("2026-11-27T00:00"), DailyBriefing.nextTrigger(at("2026-11-26T23:59"), jakarta, 0))
        assertThrows(IllegalArgumentException::class.java) { DailyBriefing.nextTrigger(0, jakarta, 1440) }
    }

    @Test
    fun rebootOrZoneChangeRecomputesFromTheCurrentClock() {
        // A device that was off at 07:00 reschedules for tomorrow instead of firing a stale briefing.
        val afterBoot = at("2026-11-26T09:15")
        assertEquals(at("2026-11-27T07:00"), DailyBriefing.nextTrigger(afterBoot, jakarta, 420))
        val tokyo = ZoneId.of("Asia/Tokyo")
        assertEquals(at("2026-11-26T21:00", tokyo), DailyBriefing.nextTrigger(afterBoot, tokyo, 1260))
    }

    @Test
    fun dstGapsMoveForwardAndRepeatedHoursFireOnce() {
        val newYork = ZoneId.of("America/New_York")
        // 02:30 does not exist on 2026-03-08; the alarm moves to 03:30 local time.
        assertEquals(at("2026-03-08T03:30", newYork), DailyBriefing.nextTrigger(at("2026-03-08T00:00", newYork), newYork, 150))
        val first = DailyBriefing.nextTrigger(at("2026-11-01T00:00", newYork), newYork, 90)
        assertEquals(at("2026-11-02T01:30", newYork), DailyBriefing.nextTrigger(first, newYork, 90))
    }

    @Test
    fun morningSummarisesTodaysSessionsHoursAndTopPriority() {
        val now = at("2026-11-26T07:00")
        val tasks = listOf(
            task("a", "Esai sejarah", "2026-12-10T10:00", 120),
            task("b", "Laporan basis data", "2026-11-27T09:00", 240),
            task("c", "Kuis lama", "2026-11-20T09:00", 30, completed = "2026-11-19T20:00"),
        )
        val sessions = listOf(
            BriefingSession(at("2026-11-26T09:00"), 50, done = false, open = true),
            BriefingSession(at("2026-11-26T13:00"), 100, done = false, open = true),
            BriefingSession(at("2026-11-26T06:00"), 30, done = true, open = false),
            BriefingSession(at("2026-11-27T09:00"), 50, done = false, open = true),
            BriefingSession(at("2026-11-25T09:00"), 50, done = true, open = false),
        )
        val message = DailyBriefing.morning(now, jakarta, tasks, sessions)
        assertEquals("Hari ini: 3 sesi fokus · 3 jam", message.title)
        assertTrue(message.body, message.body.startsWith("Prioritas #1: Laporan basis data (deadline Jum, 27 Nov 09:00)"))
        assertFalse(message.body.contains("Kuis lama"))
    }

    @Test
    fun morningHandlesAnEmptyDayAndNoTasks() {
        val now = at("2026-11-26T07:00")
        val noSessions = DailyBriefing.morning(now, jakarta, listOf(task("a", "Esai", "2026-12-01T10:00")), emptyList())
        assertEquals("Hari ini belum ada sesi fokus", noSessions.title)
        assertTrue(noSessions.body.contains("Prioritas #1: Esai"))
        val empty = DailyBriefing.morning(now, jakarta, emptyList(), emptyList())
        assertTrue(empty.body.startsWith("Belum ada tugas aktif"))
    }

    @Test
    fun eveningComparesDoneAndRemainingAndCelebratesAFinishedDay() {
        val now = at("2026-11-26T21:00")
        val tasks = listOf(task("a", "Esai", "2026-12-01T10:00", completed = "2026-11-26T15:00"))
        val partial = DailyBriefing.evening(now, jakarta, tasks, listOf(
            BriefingSession(at("2026-11-26T09:00"), 50, done = true, open = false),
            BriefingSession(at("2026-11-26T13:00"), 50, done = true, open = false),
            BriefingSession(at("2026-11-26T19:00"), 50, done = false, open = true),
            BriefingSession(at("2026-11-27T09:00"), 50, done = false, open = true),
        ))
        assertEquals("Review malam: 2 dari 3 sesi selesai", partial.title)
        assertEquals("1 tugas selesai hari ini. 1 sesi masih tersisa. Ketuk untuk evaluasi di Statistik dan susun ulang jadwal besok.",
            partial.body)

        val finished = DailyBriefing.evening(now, jakarta, tasks, listOf(
            BriefingSession(at("2026-11-26T09:00"), 50, done = true, open = false),
        ))
        assertEquals("Semua target hari ini beres", finished.title)
        assertEquals("1 sesi fokus dan 1 tugas selesai. Istirahat yang cukup, lalu lanjutkan besok.", finished.body)

        val idle = DailyBriefing.evening(now, jakarta, emptyList(), emptyList())
        assertEquals("Review malam: hari ini tanpa sesi fokus", idle.title)
    }

    @Test
    fun formatsDurations() {
        assertEquals("45 mnt", DailyBriefing.duration(45))
        assertEquals("2 jam", DailyBriefing.duration(120))
        assertEquals("2 jam 30 mnt", DailyBriefing.duration(150))
    }
}
