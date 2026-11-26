package com.tazer.taskorganizer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineAnalyzerTest {
    private val now = 1_800_000_000_000L
    private val deadlineIn48Hours = now + 48 * 60 * 60 * 1000L

    private fun ratings(title: String, description: String, deadline: Long, instant: Long) =
        OfflineAnalyzer.analyze(title, description, deadline, instant).copy(subtasks = emptyList())

    @Test
    fun returnsBaselineForUnmatchedTextOutsideDeadlineWindow() {
        assertEquals(
            OfflineAnalysis(1, 25),
            ratings("Tugas", "Baca buku", deadlineIn48Hours + 1, now),
        )
    }

    @Test
    fun countsEachWholeKeywordOnceAcrossTitleAndDescription() {
        assertEquals(
            OfflineAnalysis(3, 90),
            ratings(
                "CODING: makalah, coding",
                "Makalah tentang precoding, perkelompokan, dan presentasinya",
                deadlineIn48Hours + 1,
                now,
            ),
        )
    }

    @Test
    fun letterNumberAndUnderscoreKeepWordsTogether() {
        assertEquals(
            OfflineAnalysis(1, 25),
            ratings(
                "écoding makalah_1",
                "presentasi2 1kelompok",
                deadlineIn48Hours + 1,
                now,
            ),
        )
    }

    @Test
    fun lengthBonusUsesTrimmedDescriptionAt240Characters() {
        val deadline = deadlineIn48Hours + 1
        assertEquals(OfflineAnalysis(1, 25), ratings("Tugas", " ${"x".repeat(239)} ", deadline, now))
        assertEquals(OfflineAnalysis(2, 50), ratings("Tugas", " ${"x".repeat(240)} ", deadline, now))
    }

    @Test
    fun deadlineBonusIncludesWindowBoundaryAndPastDeadlines() {
        assertEquals(OfflineAnalysis(2, 50), ratings("Tugas", "", deadlineIn48Hours, now))
        assertEquals(OfflineAnalysis(2, 50), ratings("Tugas", "", now, now))
        assertEquals(OfflineAnalysis(2, 50), ratings("Tugas", "", now - 1, now))
    }

    @Test
    fun combinesBonusesAndCapsDifficultyAtFive() {
        val description = "kelompok " + "x".repeat(240)
        assertEquals(OfflineAnalysis(4, 150), ratings("Coding", description, deadlineIn48Hours + 1, now))
        assertEquals(OfflineAnalysis(5, 240), ratings("Coding makalah presentasi", description, now, now))
    }

    @Test
    fun deadlineComparisonHandlesEpochValuesNearLongMaximum() {
        assertEquals(
            OfflineAnalysis(2, 50),
            ratings("Tugas", "", Long.MAX_VALUE, Long.MAX_VALUE - 1),
        )
    }

    @Test
    fun suggestionsFitTheIdentifiedTaskType() {
        val coding = OfflineAnalyzer.analyze("Coding pengurutan", "", now, now).subtasks
        val paper = OfflineAnalyzer.analyze("Makalah sejarah", "", now, now).subtasks
        assertTrue(coding.size in 3..5 && coding.any { "uji" in it })
        assertTrue(paper.size in 3..5 && paper.any { "referensi" in it })
        assertTrue((coding + paper).all { it.length in 1..120 })
    }
}
