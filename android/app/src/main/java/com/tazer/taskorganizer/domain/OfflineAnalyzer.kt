package com.tazer.taskorganizer.domain

import java.util.Locale

data class OfflineAnalysis(val difficultyScore: Int, val estimatedMinutes: Int)

object OfflineAnalyzer {
    private const val DEADLINE_WINDOW_MS = 48 * 60 * 60 * 1000L
    private val keyword = Regex(
        "(?<![\\p{L}\\p{M}\\p{N}_])(coding|makalah|presentasi|kelompok)(?![\\p{L}\\p{M}\\p{N}_])",
    )
    private val minutesByScore = listOf(25, 50, 90, 150, 240)

    fun analyze(title: String, description: String, deadlineAt: Long, now: Long): OfflineAnalysis {
        val text = "$title\n$description".lowercase(Locale.ROOT)
        val keywordCount = keyword.findAll(text).map { it.value }.toSet().size
        val lengthBonus = if (description.trim().length >= 240) 1 else 0
        val deadlineBonus = if (now >= Long.MAX_VALUE - DEADLINE_WINDOW_MS ||
            deadlineAt <= now + DEADLINE_WINDOW_MS
        ) 1 else 0
        val difficulty = (1 + keywordCount + lengthBonus + deadlineBonus).coerceAtMost(5)
        return OfflineAnalysis(difficulty, minutesByScore[difficulty - 1])
    }
}
