package com.tazer.taskorganizer.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineAnalyzerTest {
    private val now = 1_800_000_000_000L
    private val deadlineIn48Hours = now + 48 * 60 * 60 * 1000L

    @Test
    fun returnsBaselineForUnmatchedTextOutsideDeadlineWindow() {
        assertEquals(
            OfflineAnalysis(1, 25),
            OfflineAnalyzer.analyze("Tugas", "Baca buku", deadlineIn48Hours + 1, now),
        )
    }

    @Test
    fun countsEachWholeKeywordOnceAcrossTitleAndDescription() {
        assertEquals(
            OfflineAnalysis(3, 90),
            OfflineAnalyzer.analyze(
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
            OfflineAnalyzer.analyze(
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
        assertEquals(OfflineAnalysis(1, 25), OfflineAnalyzer.analyze("Tugas", " ${"x".repeat(239)} ", deadline, now))
        assertEquals(OfflineAnalysis(2, 50), OfflineAnalyzer.analyze("Tugas", " ${"x".repeat(240)} ", deadline, now))
    }

    @Test
    fun deadlineBonusIncludesWindowBoundaryAndPastDeadlines() {
        assertEquals(OfflineAnalysis(2, 50), OfflineAnalyzer.analyze("Tugas", "", deadlineIn48Hours, now))
        assertEquals(OfflineAnalysis(2, 50), OfflineAnalyzer.analyze("Tugas", "", now, now))
        assertEquals(OfflineAnalysis(2, 50), OfflineAnalyzer.analyze("Tugas", "", now - 1, now))
    }

    @Test
    fun combinesBonusesAndCapsDifficultyAtFive() {
        val description = "kelompok " + "x".repeat(240)
        assertEquals(OfflineAnalysis(4, 150), OfflineAnalyzer.analyze("Coding", description, deadlineIn48Hours + 1, now))
        assertEquals(OfflineAnalysis(5, 240), OfflineAnalyzer.analyze("Coding makalah presentasi", description, now, now))
    }

    @Test
    fun deadlineComparisonHandlesEpochValuesNearLongMaximum() {
        assertEquals(
            OfflineAnalysis(2, 50),
            OfflineAnalyzer.analyze("Tugas", "", Long.MAX_VALUE, Long.MAX_VALUE - 1),
        )
    }
}
