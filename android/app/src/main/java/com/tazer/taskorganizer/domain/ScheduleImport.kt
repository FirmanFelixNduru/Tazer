package com.tazer.taskorganizer.domain

import kotlin.math.roundToInt

/** One weekly class: ISO day 1 (Monday) to 7, local minutes with [startMinute] < [endMinute] <= 1440. */
data class ScheduleDraft(val title: String, val day: Int, val startMinute: Int, val endMinute: Int) {
    val valid get() = title.isNotBlank() && title.length <= 120 && day in 1..7 && startMinute in 0..1439 &&
        endMinute in 1..1440 && startMinute < endMinute
}

data class TableImport(val drafts: List<ScheduleDraft>, val skipped: Int)

/** Reads timetables from CSV text or spreadsheet cells. Only name, day, and times are kept. */
object ScheduleTable {
    private const val MAX_ROWS = 500
    private val days = mapOf(
        "senin" to 1, "sen" to 1, "monday" to 1, "mon" to 1,
        "selasa" to 2, "sel" to 2, "tuesday" to 2, "tue" to 2, "tues" to 2,
        "rabu" to 3, "rab" to 3, "wednesday" to 3, "wed" to 3,
        "kamis" to 4, "kam" to 4, "thursday" to 4, "thu" to 4, "thur" to 4, "thurs" to 4,
        "jumat" to 5, "jum'at" to 5, "jum" to 5, "friday" to 5, "fri" to 5,
        "sabtu" to 6, "sab" to 6, "saturday" to 6, "sat" to 6,
        "minggu" to 7, "min" to 7, "ahad" to 7, "sunday" to 7, "sun" to 7,
    )
    private val titleHeaders = setOf("nama", "mata kuliah", "matkul", "mk", "nama mata kuliah", "kegiatan", "jadwal",
        "nama jadwal", "course", "subject", "name", "title", "class")
    private val dayHeaders = setOf("hari", "day", "weekday")
    private val startHeaders = setOf("jam mulai", "mulai", "waktu mulai", "start", "start time", "begin", "dari")
    private val endHeaders = setOf("jam selesai", "selesai", "waktu selesai", "end", "end time", "finish", "sampai", "berakhir")
    private val rangeHeaders = setOf("jam", "waktu", "pukul", "time", "jam kuliah", "slot")
    private val clock = Regex("""^([01]?\d|2[0-4])\s*[:.]\s*([0-5]\d)(?:\s*[:.]\s*[0-5]\d)?$""")
    private val range = Regex("""^(.+?)\s*(?:-|–|—|s/d|sd|to|sampai)\s*(.+)$""", RegexOption.IGNORE_CASE)

    /** RFC 4180 records; the delimiter is whichever of ',' or ';' appears more often outside quotes in line one. */
    fun parseCsv(text: String): List<List<String>> {
        val source = text.removePrefix("\uFEFF")
        val delimiter = detectDelimiter(source)
        val records = mutableListOf<List<String>>()
        var field = StringBuilder()
        var record = mutableListOf<String>()
        var quoted = false
        var index = 0
        fun endField() { record += field.toString(); field = StringBuilder() }
        fun endRecord() {
            endField()
            if (record.any(String::isNotBlank)) records += record
            record = mutableListOf()
            require(records.size <= MAX_ROWS + 1) { "File melebihi $MAX_ROWS baris." }
        }
        while (index < source.length) {
            val char = source[index]
            when {
                quoted && char == '"' && source.getOrNull(index + 1) == '"' -> { field.append('"'); index++ }
                char == '"' && (quoted || field.isEmpty()) -> quoted = !quoted
                quoted -> field.append(char)
                char == delimiter -> endField()
                char == '\r' -> if (source.getOrNull(index + 1) != '\n') endRecord()
                char == '\n' -> endRecord()
                else -> field.append(char)
            }
            index++
        }
        if (field.isNotEmpty() || record.isNotEmpty()) endRecord()
        return records
    }

    private fun detectDelimiter(text: String): Char {
        var commas = 0
        var semicolons = 0
        var quoted = false
        for (char in text) {
            when {
                char == '"' -> quoted = !quoted
                quoted -> continue
                char == '\n' -> break
                char == ',' -> commas++
                char == ';' -> semicolons++
            }
        }
        return if (semicolons > commas) ';' else ','
    }

    fun day(value: String): Int? {
        val key = TaskSearch.normalize(value).trimEnd('.')
        return days[key] ?: key.toIntOrNull()?.takeIf { it in 1..7 }
    }

    /** Several days in one cell, such as "Senin, Rabu" or "Sen & Kam". */
    fun dayList(value: String): List<Int>? {
        val parts = value.split(',', '/', '&', '+').flatMap { it.split(Regex("\\s+dan\\s+", RegexOption.IGNORE_CASE)) }
            .map(String::trim).filter(String::isNotEmpty)
        val parsed = parts.map { day(it) ?: return null }
        return parsed.distinct().takeIf { it.isNotEmpty() }
    }

    /** "08:25", "8.25", "08:25:00", or a spreadsheet day fraction such as 0.35. */
    fun time(value: String, allowMidnight: Boolean = false): Int? {
        val text = value.trim()
        val match = clock.matchEntire(text)
        val minute = if (match != null) {
            match.groupValues[1].toInt() * 60 + match.groupValues[2].toInt()
        } else {
            // Spreadsheets store times as a fraction of a day; whole numbers are not times.
            val number = text.toDoubleOrNull()?.takeIf { it in 0.0..1.0 && '.' in text || text == "0" } ?: return null
            (number * 1440).roundToInt()
        }
        return minute.takeIf { it < 1440 || (allowMidnight && it == 1440) }
    }

    fun timeRange(value: String): Pair<Int, Int>? {
        val match = range.matchEntire(value.trim()) ?: return null
        val start = time(match.groupValues[1]) ?: return null
        val end = time(match.groupValues[2], allowMidnight = true) ?: return null
        return (start to end).takeIf { start < end }
    }

    private class Columns(val title: Int, val day: Int, val start: Int?, val end: Int?, val range: Int?)

    /** Maps a table with a header row, or guesses the columns from their contents. */
    fun rows(table: List<List<String>>): TableImport {
        if (table.isEmpty()) return TableImport(emptyList(), 0)
        val headerIndex = table.take(5).indexOfFirst { headerColumns(it) != null }
        val columns = if (headerIndex >= 0) headerColumns(table[headerIndex]) else guessColumns(table)
        requireNotNull(columns) { "Kolom nama, hari, dan jam tidak ditemukan." }
        val body = if (headerIndex >= 0) table.drop(headerIndex + 1) else table
        val drafts = mutableListOf<ScheduleDraft>()
        var skipped = 0
        for (row in body.take(MAX_ROWS)) {
            fun cell(index: Int?) = index?.let { row.getOrNull(it)?.trim() }.orEmpty()
            val title = cell(columns.title).replace(Regex("\\s+"), " ")
            val dayValues = dayList(cell(columns.day))
            val times = columns.range?.let { timeRange(cell(it)) }
                ?: (time(cell(columns.start)) to time(cell(columns.end), allowMidnight = true))
                    .takeIf { (start, end) -> start != null && end != null && start < end }
                    ?.let { (start, end) -> start!! to end!! }
            if (title.isEmpty() || title.length > 120 || dayValues == null || times == null) {
                skipped++
                continue
            }
            dayValues.forEach { drafts += ScheduleDraft(title, it, times.first, times.second) }
        }
        skipped += (body.size - MAX_ROWS).coerceAtLeast(0)
        return TableImport(drafts, skipped)
    }

    private fun headerColumns(row: List<String>): Columns? {
        val names = row.map { TaskSearch.normalize(it).trimEnd(':') }
        fun find(options: Set<String>) = names.indexOfFirst { it in options }.takeIf { it >= 0 }
        val title = find(titleHeaders) ?: return null
        val day = find(dayHeaders) ?: return null
        val start = find(startHeaders)
        val end = find(endHeaders)
        val range = find(rangeHeaders)
        return when {
            start != null && end != null -> Columns(title, day, start, end, null)
            range != null -> Columns(title, day, null, null, range)
            else -> null
        }
    }

    private fun guessColumns(table: List<List<String>>): Columns? {
        val sample = table.take(20)
        val width = sample.maxOf { it.size }
        fun share(column: Int, test: (String) -> Boolean) =
            sample.count { row -> row.getOrNull(column)?.let(test) == true }.toDouble() / sample.size
        val day = (0 until width).firstOrNull { share(it) { value -> dayList(value) != null } >= .6 } ?: return null
        val range = (0 until width).firstOrNull { it != day && share(it) { value -> timeRange(value) != null } >= .6 }
        val times = (0 until width).filter { it != day && it != range && share(it) { value -> time(value, true) != null } >= .6 }
        val used = setOfNotNull(day, range) + times.take(2)
        val title = (0 until width).firstOrNull { it !in used && share(it) { value -> value.any(Char::isLetter) } >= .6 }
            ?: return null
        return when {
            range != null -> Columns(title, day, null, null, range)
            times.size >= 2 -> Columns(title, day, times[0], times[1], null)
            else -> null
        }
    }
}

/** A preview row: duplicates are skipped by default and clashes are warnings, never blockers. */
data class PreviewRow(val draft: ScheduleDraft, val duplicate: Boolean, val clashesWith: List<String>)

object SchedulePreview {
    fun same(a: ScheduleDraft, b: ScheduleDraft) = CourseColors.key(a.title) == CourseColors.key(b.title) &&
        a.day == b.day && a.startMinute == b.startMinute && a.endMinute == b.endMinute

    fun overlaps(a: ScheduleDraft, b: ScheduleDraft) =
        a.day == b.day && a.startMinute < b.endMinute && b.startMinute < a.endMinute

    /** Marks rows already saved (or repeated in the file) and lists what each row overlaps with. */
    fun analyze(drafts: List<ScheduleDraft>, existing: List<ScheduleDraft>): List<PreviewRow> {
        val kept = mutableListOf<ScheduleDraft>()
        return drafts.map { draft ->
            val duplicate = existing.any { same(it, draft) } || kept.any { same(it, draft) }
            if (!duplicate) kept += draft
            PreviewRow(draft, duplicate, emptyList())
        }.let { rows ->
            rows.map { row ->
                if (row.duplicate) return@map row
                val others = existing + rows.filter { it !== row && !it.duplicate }.map { it.draft }
                row.copy(clashesWith = others.filter { overlaps(it, row.draft) && !same(it, row.draft) }.map { it.title }.distinct())
            }
        }
    }

    /** Clashes among the rows the user still has ticked, plus the saved schedule. */
    fun clashes(selected: List<ScheduleDraft>, existing: List<ScheduleDraft>): Int =
        selected.count { draft -> (existing + selected).any { it !== draft && overlaps(it, draft) && !same(it, draft) } }
}
