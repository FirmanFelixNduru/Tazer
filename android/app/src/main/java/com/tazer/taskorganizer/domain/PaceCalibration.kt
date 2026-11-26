package com.tazer.taskorganizer.domain

import java.util.Locale
import kotlin.math.roundToInt

internal data class PaceSample(val course: String, val baselineMinutes: Int, val actualWorkMs: Long)

internal object PaceCalibration {
    private val courseWhitespace = Regex("[\\s\\p{Z}]+")
    private val estimateRange = 1..10_080
    private const val MIN_SAMPLES = 3

    fun factor(course: String, samples: List<PaceSample>): Double? {
        val normalizedCourse = normalizeCourse(course)
        val valid = samples.filter { it.baselineMinutes in estimateRange && it.actualWorkMs > 0 }
        val matching = valid.filter { normalizedCourse.isNotEmpty() && normalizeCourse(it.course) == normalizedCourse }
        val selected = if (matching.size >= MIN_SAMPLES) matching else valid
        if (selected.size < MIN_SAMPLES) return null

        val ratios = selected.map { it.actualWorkMs / (it.baselineMinutes * 60_000.0) }.sorted()
        val middle = ratios.size / 2
        val median = if (ratios.size % 2 == 0) (ratios[middle - 1] + ratios[middle]) / 2 else ratios[middle]
        return median.coerceIn(0.75, 1.8)
    }

    fun adjustedMinutes(baselineMinutes: Int, factor: Double?): Int =
        (baselineMinutes * (factor ?: 1.0)).roundToInt().coerceIn(estimateRange)

    private fun normalizeCourse(course: String): String =
        course.trim().replace(courseWhitespace, " ").lowercase(Locale.ROOT)
}
