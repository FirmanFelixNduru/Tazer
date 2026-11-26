package com.tazer.taskorganizer.domain

import org.junit.Assert.*
import org.junit.Test

class ScheduleImportTest {
    private fun d(title: String, day: Int, start: String, end: String) =
        ScheduleDraft(title, day, ScheduleTable.time(start)!!, ScheduleTable.time(end, allowMidnight = true)!!)

    @Test
    fun csvParsesCommaAndSemicolonWithQuotesAndBom() {
        val comma = "\uFEFFNama,Hari,Jam Mulai,Jam Selesai\r\n\"Basis Data, Kelas A\",Senin,08:00,09:40\r\n\"Kata \"\"kunci\"\"\",Rabu,10:00,11:40\n"
        assertEquals(listOf(listOf("Nama", "Hari", "Jam Mulai", "Jam Selesai"),
            listOf("Basis Data, Kelas A", "Senin", "08:00", "09:40"), listOf("Kata \"kunci\"", "Rabu", "10:00", "11:40")),
            ScheduleTable.parseCsv(comma))
        val semicolon = "Mata Kuliah;Hari;Jam;Ruang;Dosen\nAlgoritma;Selasa;08.25 - 10.10;Lab 2;Pak Dedi\n\n;;;;\n"
        assertEquals(listOf(listOf("Mata Kuliah", "Hari", "Jam", "Ruang", "Dosen"),
            listOf("Algoritma", "Selasa", "08.25 - 10.10", "Lab 2", "Pak Dedi")), ScheduleTable.parseCsv(semicolon))
        assertEquals(listOf(listOf("baris", "dua\nbaris")), ScheduleTable.parseCsv("baris,\"dua\nbaris\""))
    }

    @Test
    fun headersMapStartEndOrOneRangeColumnAndIgnoreRoomsAndLecturers() {
        val split = ScheduleTable.rows(ScheduleTable.parseCsv("Matkul,Hari,Mulai,Selesai,Ruang\nBasis Data,senin,8:00,9.40,B-201\nKosong,,08:00,09:00,\n"))
        assertEquals(listOf(d("Basis Data", 1, "08:00", "09:40")), split.drafts)
        assertEquals(1, split.skipped)
        val ranged = ScheduleTable.rows(ScheduleTable.parseCsv("Mata Kuliah;Hari;Jam;Dosen\nAlgoritma;Selasa;08:25-10:10;Pak Dedi\nWeb;Sen & Kam;13.00 s/d 14.40;Bu Ratna\nMalam;Jumat;22:00-24:00;-\n"))
        assertEquals(listOf(d("Algoritma", 2, "08:25", "10:10"), d("Web", 1, "13:00", "14:40"), d("Web", 4, "13:00", "14:40"),
            d("Malam", 5, "22:00", "24:00")), ranged.drafts)
        assertEquals(0, ranged.skipped)
    }

    @Test
    fun columnsAreGuessedWhenThereIsNoHeader() {
        val result = ScheduleTable.rows(ScheduleTable.parseCsv("Senin,07:30-09:10,Statistika,A-302\nKamis,13:00-15:30,Jaringan Komputer,C-105\n"))
        assertEquals(listOf(d("Statistika", 1, "07:30", "09:10"), d("Jaringan Komputer", 4, "13:00", "15:30")), result.drafts)
        assertThrows(IllegalArgumentException::class.java) { ScheduleTable.rows(listOf(listOf("apa", "ini"), listOf("tidak", "jelas"))) }
    }

    @Test
    fun daysAndTimesAcceptCommonSpellingsAndSpreadsheetFractions() {
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), listOf("Senin", "SELASA", "rab", "Thursday", "Jum'at", "sat", "Minggu").map(ScheduleTable::day))
        assertEquals(5, ScheduleTable.day("5"))
        assertNull(ScheduleTable.day("Hari libur"))
        assertEquals(listOf(1, 3), ScheduleTable.dayList("Senin, Rabu"))
        assertEquals(listOf(2, 4), ScheduleTable.dayList("Selasa dan Kamis"))
        assertEquals(8 * 60 + 25, ScheduleTable.time("08:25"))
        assertEquals(8 * 60 + 25, ScheduleTable.time("8.25"))
        assertEquals(8 * 60 + 25, ScheduleTable.time("08:25:00"))
        assertEquals(8 * 60, ScheduleTable.time("0.33333333333"))
        assertEquals(12 * 60, ScheduleTable.time("0.5"))
        assertNull(ScheduleTable.time("8"))
        assertNull(ScheduleTable.time("24:00"))
        assertEquals(1440, ScheduleTable.time("24:00", allowMidnight = true))
        assertNull(ScheduleTable.timeRange("10:00-09:00"))
    }

    @Test
    fun previewSkipsDuplicatesAndWarnsAboutClashes() {
        val existing = listOf(d("Basis Data", 1, "08:00", "09:40"))
        val rows = SchedulePreview.analyze(listOf(
            d("basis  data", 1, "08:00", "09:40"),
            d("Algoritma", 1, "09:00", "10:30"),
            d("Algoritma", 1, "09:00", "10:30"),
            d("Statistika", 2, "08:00", "09:00"),
        ), existing)
        assertEquals(listOf(true, false, true, false), rows.map { it.duplicate })
        assertEquals(listOf("Basis Data"), rows[1].clashesWith)
        assertEquals(emptyList<String>(), rows[3].clashesWith)
        assertEquals(1, SchedulePreview.clashes(listOf(rows[1].draft, rows[3].draft), existing))
        assertFalse(ScheduleDraft("", 1, 0, 60).valid)
        assertFalse(ScheduleDraft("A", 8, 0, 60).valid)
    }
}
