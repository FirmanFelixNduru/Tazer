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

    private val zone = java.time.ZoneId.of("Asia/Jakarta")
    private val now = java.time.LocalDateTime.of(2026, 11, 25, 10, 0).atZone(zone).toInstant().toEpochMilli()
    private fun facts(row: Row) = TaskFacts(row.title, row.course, row.description, row.difficulty, now + 86_400_000, 60)
    private fun search(query: TaskQuery, done: Boolean) =
        TaskSearch.apply(query, rows.filter { it.done == done }, now, zone, ::facts).map { it.title }

    @Test
    fun searchCoversTitleCourseAndDescriptionOnBothTabs() {
        assertEquals(listOf("Laporan normalisasi"), search(TaskQuery("erd"), done = false))
        assertEquals(listOf("Kuis SQL"), search(TaskQuery("join"), done = true))
        assertEquals(listOf("Esai sejarah"), search(TaskQuery("majapahit sejarah"), done = false))
        assertEquals(emptyList<String>(), search(TaskQuery("majapahit react"), done = false))
        assertEquals(listOf("Laporan normalisasi", "Esai sejarah"), search(TaskQuery("  "), done = false))
        assertTrue(TaskSearch.matches(TaskQuery("BÁSIS dátá"), TaskFacts("x", "Basis Data", "", 1, now, 30), now, zone))
    }

    @Test
    fun filterPanelCombinesCoursesDifficultiesDeadlineDurationSubtasksAndSort() {
        val hour = 3_600_000L
        data class T(val id: String, val course: String, val difficulty: Int, val deadline: Long, val minutes: Int, val open: Int)
        val ranked = listOf(
            T("a", "RPL", 4, now + 3 * hour, 120, 2),          // later today
            T("b", "rpl", 2, now - hour, 20, 0),                 // overdue
            T("c", "Basis Data", 5, now + 50 * hour, 45, 1),    // in two days
            T("d", "Jaringan", 1, now + 6 * 24 * hour, 90, 0),  // next Tuesday, outside this week
        )
        fun ids(query: TaskQuery) = TaskSearch.apply(query, ranked, now, zone) {
            TaskFacts(it.id, it.course, "", it.difficulty, it.deadline, it.minutes, it.open)
        }.map { it.id }
        assertEquals(listOf("a", "b"), ids(TaskQuery(courses = setOf("RPL"))))
        assertEquals(listOf("a", "c"), ids(TaskQuery(difficulties = setOf(4, 5))))
        assertEquals(listOf("a"), ids(TaskQuery(deadline = DeadlineRange.TODAY)))
        assertEquals(listOf("a", "c"), ids(TaskQuery(deadline = DeadlineRange.THREE_DAYS)))
        assertEquals(listOf("a", "c"), ids(TaskQuery(deadline = DeadlineRange.THIS_WEEK)))
        assertEquals(listOf("b"), ids(TaskQuery(deadline = DeadlineRange.OVERDUE)))
        assertEquals(listOf("b"), ids(TaskQuery(duration = DurationRange.SHORT)))
        assertEquals(listOf("c", "d"), ids(TaskQuery(duration = DurationRange.MEDIUM)))
        assertEquals(listOf("a"), ids(TaskQuery(duration = DurationRange.LONG)))
        assertEquals(listOf("a", "c"), ids(TaskQuery(openSubtasksOnly = true)))
        assertEquals(listOf("a"), ids(TaskQuery(courses = setOf("rpl", "Basis Data"), difficulties = setOf(4, 5), deadline = DeadlineRange.TODAY)))
        // Sorting is stable and PRIORITY keeps the scheduler order.
        assertEquals(listOf("a", "b", "c", "d"), ids(TaskQuery()))
        assertEquals(listOf("b", "a", "c", "d"), ids(TaskQuery(sort = TaskSort.DEADLINE)))
        assertEquals(listOf("c", "a", "b", "d"), ids(TaskQuery(sort = TaskSort.DIFFICULTY)))
        assertEquals(listOf("b", "c", "d", "a"), ids(TaskQuery(sort = TaskSort.SHORTEST)))
        // Badge counts chosen chips and options; Reset keeps the typed search.
        val busy = TaskQuery("esai", setOf("RPL", "Jaringan"), setOf(3), DeadlineRange.OVERDUE, DurationRange.LONG, TaskSort.DEADLINE, true)
        assertEquals(7, busy.filterCount)
        assertEquals(TaskQuery("esai"), busy.withoutFilters())
        assertEquals(listOf("RPL" to 2, "Basis Data" to 1).sortedBy { CourseColors.key(it.first) },
            TaskSearch.courseCounts(listOf("RPL", "rpl ", "Basis Data", "")))
    }

    @Test
    fun courseAndDifficultyChipsCombineWithText() {
        assertEquals(listOf("Laporan normalisasi"), search(TaskQuery(courses = setOf("BASIS DATA")), done = false))
        assertEquals(listOf("Kuis SQL"), search(TaskQuery(courses = setOf("Basis Data")), done = true))
        assertEquals(listOf("Proyek React"), search(TaskQuery(difficulties = setOf(5)), done = true))
        assertEquals(emptyList<String>(), search(TaskQuery("erd", difficulties = setOf(4)), done = false))
        assertFalse(TaskQuery().active)
        assertTrue(TaskQuery(difficulties = setOf(1)).active)
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
    fun everySwatchIsReadableOnWhiteBlackAndNeumorphicPages() {
        assertEquals(10, CourseColors.palette.size)
        assertEquals(CourseColors.palette.size, CourseColors.palette.map { it.light.container }.toSet().size)
        for (swatch in CourseColors.palette) {
            // White/black plus the v1.9.0 neumorphic canvas and raised-card surfaces.
            for ((page, tone) in listOf(0xFFFFFFFF to swatch.light, 0xFFEDF2FB to swatch.light, 0xFFF4F7FD to swatch.light,
                0xFF000000 to swatch.dark, 0xFF141923 to swatch.dark, 0xFF1A2130 to swatch.dark)) {
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
