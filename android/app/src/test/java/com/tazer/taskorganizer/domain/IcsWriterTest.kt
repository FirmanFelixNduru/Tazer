package com.tazer.taskorganizer.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class IcsWriterTest {
    private val zone = ZoneId.of("Asia/Jakarta")
    private val now = LocalDateTime.of(2026, 11, 26, 8, 0).atZone(zone).toInstant().toEpochMilli()
    private fun at(text: String) = LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()
    private fun day(text: String) = LocalDate.parse(text).toEpochDay()

    private val events = listOf(
        ExportEvent("e1", "UTS; Basis Data, ruang 3", day("2026-12-01"), day("2026-12-01"), 9 * 60, "Bawa\nkartu ujian\\KTM"),
        ExportEvent("e2", "Libur semester", day("2026-12-20"), day("2026-12-31"), null, ""),
        ExportEvent("e3", "Kemah kampus", day("2026-12-05"), day("2026-12-06"), 18 * 60 + 30, ""),
        ExportEvent("e4", "Seminar", day("2026-12-08"), day("2026-12-08"), 13 * 60, "", endMinute = 15 * 60 + 30),
    )
    private val blocks = listOf(
        ExportBlock("b1", "Kuliah Algoritma", 1, 8 * 60, 10 * 60),
        ExportBlock("b2", "Ronda malam", 5, 22 * 60, 1440),
    )
    private val sessions = listOf(ExportSession("s1", "Laporan ERD", "Basis Data", at("2026-11-27T13:00"), at("2026-11-27T13:50")))

    private fun written() = IcsWriter.write(zone, now, events, blocks, sessions)

    @Test
    fun followsRfc5545LineRules() {
        val ics = written()
        assertTrue(ics.startsWith("BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:"))
        assertTrue(ics.endsWith("END:VCALENDAR\r\n"))
        assertFalse("bare LF", Regex("(?<!\r)\n").containsMatchIn(ics))
        ics.split("\r\n").forEach { assertTrue(it, it.toByteArray(Charsets.UTF_8).size <= 75) }
        assertEquals(7, Regex("BEGIN:VEVENT").findAll(ics).count())
        assertEquals(7, Regex("\r\nDTSTAMP:20261126T010000Z\r\n").findAll(ics).count())
        assertTrue(ics.contains("DTSTART:20261208T130000\r\nDTEND:20261208T153000\r\n"))
        assertTrue(ics.contains("SUMMARY:UTS\\; Basis Data\\, ruang 3\r\n"))
        assertTrue(ics.contains("RRULE:FREQ=WEEKLY;BYDAY=MO\r\n"))
        assertTrue(ics.contains("DTSTART:20261127T060000Z\r\n"))
    }

    @Test
    fun foldsLongUtf8LinesWithoutSplittingCharacters() {
        val folded = IcsWriter.fold("SUMMARY:" + "Matematika diskrit 🧮 ".repeat(8))
        val lines = folded.split("\r\n")
        assertTrue(lines.size > 1)
        lines.forEach { assertTrue(it.toByteArray(Charsets.UTF_8).size <= 75) }
        assertEquals("SUMMARY:" + "Matematika diskrit 🧮 ".repeat(8), folded.replace("\r\n ", ""))
    }

    @Test
    fun roundTripsThroughTheTazerImporter() {
        val parsed = IcsParser.parse(written().byteInputStream(), zone)
        assertEquals(0, parsed.invalid)
        assertEquals(0, parsed.unsupported)
        val parsedEvents = parsed.items.filterIsInstance<IcsItem.Event>().associateBy { it.uid }
        val exam = parsedEvents.getValue("event-e1@tazer")
        assertEquals("UTS; Basis Data, ruang 3", exam.title)
        assertEquals("Bawa\nkartu ujian\\KTM", exam.description)
        assertEquals(LocalDate.parse("2026-12-01"), exam.startDate)
        assertEquals(LocalDate.parse("2026-12-01"), exam.endDate)
        assertEquals(9 * 60, exam.startMinute)
        val holiday = parsedEvents.getValue("event-e2@tazer")
        assertEquals(LocalDate.parse("2026-12-20") to LocalDate.parse("2026-12-31"), holiday.startDate to holiday.endDate)
        assertNull(holiday.startMinute)
        val camp = parsedEvents.getValue("event-e3@tazer")
        assertEquals(LocalDate.parse("2026-12-06"), camp.endDate)
        assertEquals(18 * 60 + 30, camp.startMinute)
        val seminar = parsedEvents.getValue("event-e4@tazer")
        assertEquals(13 * 60 to 15 * 60 + 30, seminar.startMinute to seminar.endMinute)
        assertNull(exam.endMinute)
        val session = parsedEvents.getValue("session-s1@tazer")
        assertEquals("Fokus: Laporan ERD", session.title)
        assertEquals(13 * 60, session.startMinute)
        assertEquals("Basis Data", session.description)

        val weekly = parsed.items.filterIsInstance<IcsItem.WeeklyBlock>().associateBy { it.uid }
        val lecture = weekly.getValue("fixed-b1@tazer")
        assertEquals(Triple(DayOfWeek.MONDAY, 8 * 60, 10 * 60), Triple(lecture.day, lecture.startMinute, lecture.endMinute))
        val late = weekly.getValue("fixed-b2@tazer")
        assertEquals(Triple(DayOfWeek.FRIDAY, 22 * 60, 1440), Triple(late.day, late.startMinute, late.endMinute))
    }

    @Test
    fun emptyCalendarIsStillValid() {
        val parsed = IcsParser.parse(IcsWriter.write(zone, now, emptyList(), emptyList(), emptyList()).byteInputStream(), zone)
        assertTrue(parsed.items.isEmpty())
    }
}
