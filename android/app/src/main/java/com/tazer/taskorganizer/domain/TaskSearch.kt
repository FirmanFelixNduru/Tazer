package com.tazer.taskorganizer.domain

import java.text.Normalizer
import java.util.Locale

/** Search text plus optional course and difficulty chips; empty fields match everything. */
data class TaskQuery(val text: String = "", val course: String? = null, val difficulty: Int? = null) {
    val active get() = text.isNotBlank() || course != null || difficulty != null
}

object TaskSearch {
    private val marks = Regex("\\p{M}+")

    /** Lower-case, accent-free, single-spaced text so "Basis Dátá" finds "basis data". */
    fun normalize(value: String): String =
        marks.replace(Normalizer.normalize(value, Normalizer.Form.NFD), "")
            .lowercase(Locale.ROOT).replace(Regex("\\s+"), " ").trim()

    /** Every search word must appear in the title, course, or description. */
    fun matches(query: TaskQuery, title: String, course: String, description: String, difficulty: Int): Boolean {
        if (query.course != null && CourseColors.key(course) != CourseColors.key(query.course)) return false
        if (query.difficulty != null && difficulty != query.difficulty) return false
        val words = normalize(query.text).split(' ').filter(String::isNotEmpty)
        if (words.isEmpty()) return true
        val haystack = normalize("$title $course $description")
        return words.all(haystack::contains)
    }

    /** Distinct course names for the chips, keeping the first spelling and sorting by key. */
    fun courses(names: Iterable<String>): List<String> =
        names.filter(String::isNotBlank).distinctBy(CourseColors::key).sortedBy(CourseColors::key)
}
