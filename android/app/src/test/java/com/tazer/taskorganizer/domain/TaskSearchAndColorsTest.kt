package com.tazer.taskorganizer.domain

import org.junit.Assert.*
import org.junit.Test

class TaskSearchAndColorsTest {
    private data class Row(val title: String, val course: String, val description: String, val difficulty: Int, val done: Boolean)

    private val rows = listOf(
        Row("Laporan normalisasi", "Basis Data", "Gambar ERD dan tabel", 3, false),
        Row("Kuis SQL", "basis  data", "Latihan JOIN", 2, true),
        Row("Esai sejarah", "Sejarah Indonesia", "Kerajaan Majapahit", 4, false),
        Row("Proyek React", "Pemrograman Web", "Aplikasi to-do dengan REST API", 5, true),
    )

    private fun search(query: TaskQuery, done: Boolean) =
        rows.filter { it.done == done && TaskSearch.matches(query, it.title, it.course, it.description, it.difficulty) }.map { it.title }

    @Test
    fun searchCoversTitleCourseAndDescriptionOnBothTabs() {
        assertEquals(listOf("Laporan normalisasi"), search(TaskQuery("erd"), done = false))
        assertEquals(listOf("Kuis SQL"), search(TaskQuery("join"), done = true))
        assertEquals(listOf("Esai sejarah"), search(TaskQuery("majapahit sejarah"), done = false))
        assertEquals(emptyList<String>(), search(TaskQuery("majapahit react"), done = false))
        assertEquals(listOf("Laporan normalisasi", "Esai sejarah"), search(TaskQuery("  "), done = false))
        assertTrue(TaskSearch.matches(TaskQuery("BÁSIS dátá"), "x", "Basis Data", "", 1))
    }

    @Test
    fun courseAndDifficultyChipsCombineWithText() {
        assertEquals(listOf("Laporan normalisasi"), search(TaskQuery(course = "BASIS DATA"), done = false))
        assertEquals(listOf("Kuis SQL"), search(TaskQuery(course = "Basis Data"), done = true))
        assertEquals(listOf("Proyek React"), search(TaskQuery(difficulty = 5), done = true))
        assertEquals(emptyList<String>(), search(TaskQuery("erd", difficulty = 4), done = false))
        assertFalse(TaskQuery().active)
        assertTrue(TaskQuery(difficulty = 1).active)
        assertEquals(listOf("Basis Data", "Pemrograman Web", "Sejarah Indonesia"), TaskSearch.courses(rows.map { it.course } + " "))
    }

    @Test
    fun courseColoursAreDeterministicAndSpellingInsensitive() {
        val first = CourseColors.automaticIndex("Basis Data")
        repeat(3) { assertEquals(first, CourseColors.automaticIndex("Basis Data")) }
        assertEquals(first, CourseColors.automaticIndex("  basis   DATA "))
        // FNV-1a pins the slot, so colours stay the same across devices and releases.
        assertEquals(listOf(first, CourseColors.automaticIndex("Algoritma")),
            listOf(CourseColors.automaticIndex("BASIS DATA"), CourseColors.automaticIndex("algoritma")))
        val spread = listOf("Basis Data", "Algoritma", "Kalkulus", "Fisika Dasar", "Sejarah Indonesia", "Pemrograman Web",
            "Statistika", "Jaringan Komputer", "Bahasa Inggris", "Etika Profesi").map(CourseColors::automaticIndex).toSet()
        assertTrue("Ten common courses should use several hues", spread.size >= 5)
        // Pinned values guard against an accidental formula change that would recolour every course.
        assertEquals(listOf(2, 5, 1, 5, 7, 0), listOf("Algoritma", "Arsitektur Perangkat Lunak",
            "Jaringan dan Komunikasi Data", "QA", "QA Rili", "Rekayasa Perangkat Lunak").map(CourseColors::automaticIndex))
        val buckets = (1..500).map { CourseColors.automaticIndex("Mata kuliah $it") }.groupingBy { it }.eachCount()
        assertEquals(10, buckets.size)
        assertTrue("Slots should be roughly even: $buckets", buckets.values.all { it in 30..75 })
        assertEquals(4, CourseColors.index("Basis Data", mapOf("basis data" to 4)))
        assertEquals(first, CourseColors.index("Basis Data", mapOf("basis data" to 99)))
    }

    @Test
    fun everySwatchIsReadableOnWhiteAndBlackPages() {
        assertEquals(10, CourseColors.palette.size)
        assertEquals(CourseColors.palette.size, CourseColors.palette.map { it.light.container }.toSet().size)
        for (swatch in CourseColors.palette) {
            for ((page, tone) in listOf(0xFFFFFFFF to swatch.light, 0xFF000000 to swatch.dark)) {
                assertTrue("${swatch.name} label contrast", CourseColors.contrast(tone.ink, tone.container) >= 4.5)
                assertTrue("${swatch.name} dot on chip", CourseColors.contrast(tone.accent, tone.container) >= 1.5)
                assertTrue("${swatch.name} dot on page", CourseColors.contrast(tone.accent, page) >= 3.0)
            }
        }
        assertEquals(21.0, CourseColors.contrast(0xFFFFFFFF, 0xFF000000), 0.01)
    }

    @Test
    fun releaseNotesRenderHeadingsBulletsAndBold() {
        val blocks = ReleaseNotes.parse("""
            ## Tazer **1.7.0**

            - **Pencarian.** Cari tugas di dua tab.
            * Warna per mata kuliah
            Pasang APK langsung
            di atas v1.6.0.

            Teks **tanpa penutup
        """.trimIndent())
        assertEquals(NoteBlock.Heading("Tazer 1.7.0"), blocks[0])
        assertEquals(NoteBlock.Bullet(listOf(NoteSpan("Pencarian.", true), NoteSpan(" Cari tugas di dua tab."))), blocks[1])
        assertEquals(NoteBlock.Bullet(listOf(NoteSpan("Warna per mata kuliah"))), blocks[2])
        assertEquals(NoteBlock.Paragraph(listOf(NoteSpan("Pasang APK langsung di atas v1.6.0."))), blocks[3])
        assertEquals(NoteBlock.Paragraph(listOf(NoteSpan("Teks **tanpa penutup"))), blocks[4])
        assertTrue(blocks.none { block -> block.toString().contains("##") })
        assertEquals(emptyList<NoteBlock>(), ReleaseNotes.parse("\n\n"))
    }
}
