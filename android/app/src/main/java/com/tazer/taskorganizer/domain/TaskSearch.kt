package com.tazer.taskorganizer.domain

import java.text.Normalizer
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** Deadline windows measured from `now` in the profile zone; OVERDUE means the deadline has passed. */
enum class DeadlineRange(val label: String) {
    ALL("Semua"), TODAY("Hari Ini"), THREE_DAYS("3 Hari"), THIS_WEEK("Minggu Ini"), OVERDUE("Terlambat"),
}

/** Estimated minutes: remaining work for active tasks, the confirmed estimate for finished ones. */
enum class DurationRange(val label: String) {
    ALL("Semua"), SHORT("≤ 30 mnt"), MEDIUM("30–90 mnt"), LONG("> 90 mnt");

    fun matches(minutes: Int) = when (this) {
        ALL -> true
        SHORT -> minutes <= 30
        MEDIUM -> minutes in 31..90
        LONG -> minutes > 90
    }
}

/** PRIORITY keeps the order the list arrived in (scheduler ranking, or newest first on Selesai). */
enum class TaskSort(val label: String) {
    PRIORITY("Prioritas Tertinggi"), DEADLINE("Deadline Terdekat"), DIFFICULTY("Kesulitan Tertinggi"), SHORTEST("Durasi Terpendek"),
}

/** Search text plus the Filter panel; empty sets and ALL match everything. Course entries compare by key. */
data class TaskQuery(
    val text: String = "",
    val courses: Set<String> = emptySet(),
    val difficulties: Set<Int> = emptySet(),
    val deadline: DeadlineRange = DeadlineRange.ALL,
    val duration: DurationRange = DurationRange.ALL,
    val sort: TaskSort = TaskSort.PRIORITY,
    val openSubtasksOnly: Boolean = false,
) {
    /** Badge count on the Filter button: every chosen chip or option, text search excluded. */
    val filterCount get() = courses.size + difficulties.size + listOf(
        deadline != DeadlineRange.ALL, duration != DurationRange.ALL, sort != TaskSort.PRIORITY, openSubtasksOnly,
    ).count { it }
    val active get() = text.isNotBlank() || filterCount > 0
    fun withoutFilters() = TaskQuery(text = text)
}

/** What the filter needs from one task, independent of Room entities. */
data class TaskFacts(
    val title: String, val course: String, val description: String, val difficulty: Int,
    val deadlineAt: Long, val minutes: Int, val openSubtasks: Int = 0,
)

object TaskSearch {
    private val marks = Regex("\\p{M}+")

    /** Lower-case, accent-free, single-spaced text so "Basis Dátá" finds "basis data". */
    fun normalize(value: String): String =
        marks.replace(Normalizer.normalize(value, Normalizer.Form.NFD), "")
            .lowercase(Locale.ROOT).replace(Regex("\\s+"), " ").trim()

    /** Every search word must appear in the title, course, or description, and every chosen filter must hold. */
    fun matches(query: TaskQuery, task: TaskFacts, now: Long, zone: ZoneId): Boolean {
        if (query.courses.isNotEmpty() && query.courses.none { CourseColors.key(it) == CourseColors.key(task.course) }) return false
        if (query.difficulties.isNotEmpty() && task.difficulty !in query.difficulties) return false
        if (!inRange(query.deadline, task.deadlineAt, now, zone)) return false
        if (!query.duration.matches(task.minutes)) return false
        if (query.openSubtasksOnly && task.openSubtasks == 0) return false
        val words = normalize(query.text).split(' ').filter(String::isNotEmpty)
        if (words.isEmpty()) return true
        val haystack = normalize("${task.title} ${task.course} ${task.description}")
        return words.all(haystack::contains)
    }

    /** Filters, then sorts stably so ties keep the incoming (ranked) order. */
    fun <T> apply(query: TaskQuery, items: List<T>, now: Long, zone: ZoneId, facts: (T) -> TaskFacts): List<T> {
        val kept = items.map { it to facts(it) }.filter { (_, task) -> matches(query, task, now, zone) }
        val sorted = when (query.sort) {
            TaskSort.PRIORITY -> kept
            TaskSort.DEADLINE -> kept.sortedBy { it.second.deadlineAt }
            TaskSort.DIFFICULTY -> kept.sortedByDescending { it.second.difficulty }
            TaskSort.SHORTEST -> kept.sortedBy { it.second.minutes }
        }
        return sorted.map { it.first }
    }

    fun inRange(range: DeadlineRange, deadlineAt: Long, now: Long, zone: ZoneId): Boolean {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        fun startOf(day: java.time.LocalDate) = day.atStartOfDay(zone).toInstant().toEpochMilli()
        return when (range) {
            DeadlineRange.ALL -> true
            DeadlineRange.OVERDUE -> deadlineAt < now
            DeadlineRange.TODAY -> deadlineAt >= now && deadlineAt < startOf(today.plusDays(1))
            DeadlineRange.THREE_DAYS -> deadlineAt >= now && deadlineAt < startOf(today.plusDays(3))
            DeadlineRange.THIS_WEEK -> deadlineAt >= now &&
                deadlineAt < startOf(today.with(TemporalAdjusters.next(DayOfWeek.MONDAY)))
        }
    }

    /** Distinct course names for the chips, keeping the first spelling and sorting by key. */
    fun courses(names: Iterable<String>): List<String> =
        names.filter(String::isNotBlank).distinctBy(CourseColors::key).sortedBy(CourseColors::key)

    /** Course chips with how many tasks in the current tab carry each course, e.g. "RPL (3)". */
    fun courseCounts(names: Iterable<String>): List<Pair<String, Int>> {
        val counts = names.filter(String::isNotBlank).groupingBy(CourseColors::key).eachCount()
        return courses(names).map { it to counts.getValue(CourseColors.key(it)) }
    }
}
