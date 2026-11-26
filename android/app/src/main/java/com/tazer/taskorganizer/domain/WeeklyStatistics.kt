package com.tazer.taskorganizer.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToInt

internal data class StatisticsTask(
    val id: String,
    val course: String,
    val completedAt: Long?,
    val deadlineAt: Long,
    val initialEstimatedMinutes: Int,
    val analyzerBaselineMinutes: Int?,
)

internal data class StatisticsSession(
    val taskId: String,
    val done: Boolean,
    val startedAt: Long?,
    val endedAt: Long?,
    val plannedEndAt: Long,
    val actualWorkMs: Long?,
)

internal data class DailyFocus(val date: LocalDate, val workMs: Long)
internal data class CourseFocus(val course: String, val workMs: Long)

internal data class EstimateAccuracy(
    val taskCount: Int,
    val estimatedMinutes: Long,
    val actualWorkMs: Long,
    val percent: Int,
)

internal data class WeeklyStatistics(
    val days: List<DailyFocus>,
    val courses: List<CourseFocus>,
    val completedTaskCount: Int,
    val accuracy: EstimateAccuracy?,
    val paceFactor: Double?,
    val onTimeStreakDays: Int,
) {
    val totalFocusMs: Long get() = days.sumOf(DailyFocus::workMs)
}

/**
 * Inputs belong to one profile. [weekEnd] and the six preceding civil dates make up the window.
 * A completed task's estimate uses all its logged work, including sessions before the window or
 * before a reopen. Accuracy is 100% minus the mean absolute error relative to the initial estimate,
 * floored at zero. Pace uses the analyzer baseline when present, as it does during task analysis.
 */
internal fun weeklyStatistics(
    tasks: List<StatisticsTask>,
    sessions: List<StatisticsSession>,
    today: LocalDate,
    weekEnd: LocalDate,
    zone: ZoneId,
): WeeklyStatistics {
    val weekStart = weekEnd.minusDays(6)
    val tasksById = tasks.associateBy(StatisticsTask::id)
    val focusByDay = mutableMapOf<LocalDate, Long>()
    val focusByCourse = mutableMapOf<String, Long>()
    val actualByTask = mutableMapOf<String, Long>()
    sessions.forEach { session ->
        val actual = session.actualWorkMs ?: return@forEach
        if (!session.done || session.startedAt == null || actual <= 0) return@forEach
        val task = tasksById[session.taskId] ?: return@forEach
        actualByTask.merge(task.id, actual, Long::plus)
        val day = (session.endedAt ?: session.plannedEndAt).civilDate(zone)
        if (day in weekStart..weekEnd) {
            focusByDay.merge(day, actual, Long::plus)
            focusByCourse.merge(task.course, actual, Long::plus)
        }
    }
    val onTimeDates = mutableSetOf<LocalDate>()
    val estimates = mutableListOf<PaceSample>()
    val paceSamples = mutableListOf<PaceSample>()
    var completedTaskCount = 0
    tasks.forEach { task ->
        val completedAt = task.completedAt ?: return@forEach
        val day = completedAt.civilDate(zone)
        if (day <= weekEnd && completedAt <= task.deadlineAt) onTimeDates += day
        if (day !in weekStart..weekEnd) return@forEach
        completedTaskCount++
        val actual = actualByTask[task.id] ?: return@forEach
        val initial = task.initialEstimatedMinutes
        if (initial in 1..10_080) estimates += PaceSample(task.course, initial, actual)
        paceSamples += PaceSample(task.course, task.analyzerBaselineMinutes ?: initial, actual)
    }
    val accuracy = estimates.takeIf { it.isNotEmpty() }?.let { samples ->
        val meanError = samples.sumOf { abs(it.actualWorkMs / (it.baselineMinutes * 60_000.0) - 1) } / samples.size
        EstimateAccuracy(
            taskCount = samples.size,
            estimatedMinutes = samples.sumOf { it.baselineMinutes.toLong() },
            actualWorkMs = samples.sumOf(PaceSample::actualWorkMs),
            percent = ((1 - meanError).coerceAtLeast(0.0) * 100).roundToInt(),
        )
    }
    return WeeklyStatistics(
        days = List(7) { offset ->
            val day = weekStart.plusDays(offset.toLong())
            DailyFocus(day, focusByDay[day] ?: 0)
        },
        courses = focusByCourse.map { (course, actual) -> CourseFocus(course, actual) }
            .sortedWith(compareByDescending<CourseFocus> { it.workMs }.thenBy { it.course }),
        completedTaskCount = completedTaskCount,
        accuracy = accuracy,
        paceFactor = PaceCalibration.factor("", paceSamples),
        onTimeStreakDays = onTimeStreak(onTimeDates, today, weekEnd),
    )
}

/**
 * A date qualifies when at least one task finished at or before its deadline, even if another
 * task was late. Count consecutive completion dates backwards from the selected week end,
 * crossing earlier weeks if needed. If that end is today and today has yet to qualify, start
 * from yesterday. Any other date without an on-time completion breaks the streak.
 */
private fun onTimeStreak(onTimeDates: Set<LocalDate>, today: LocalDate, weekEnd: LocalDate): Int {
    var date = if (weekEnd == today && weekEnd !in onTimeDates) weekEnd.minusDays(1) else weekEnd
    var count = 0
    while (date in onTimeDates) {
        count++
        date = date.minusDays(1)
    }
    return count
}

private fun Long.civilDate(zone: ZoneId): LocalDate =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDate()
