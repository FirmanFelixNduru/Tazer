package com.tazer.taskorganizer.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

data class ExportEvent(
    val id: String, val title: String, val startEpochDay: Long, val endEpochDay: Long,
    val startMinute: Int?, val description: String,
)

data class ExportBlock(val id: String, val label: String, val dayOfWeek: Int, val startMinute: Int, val endMinute: Int)

data class ExportSession(val id: String, val title: String, val course: String, val startAt: Long, val endAt: Long)

/**
 * RFC 5545 calendar of study sessions, weekly fixed blocks, and calendar events.
 * Local times are floating DATE-TIMEs in the profile zone (named in X-WR-TIMEZONE), so no VTIMEZONE is needed
 * and the file imports back into Tazer unchanged.
 */
object IcsWriter {
    private val utc = DateTimeFormatter.ofPattern("uuuuMMdd'T'HHmmss'Z'", Locale.ROOT).withZone(ZoneOffset.UTC)
    private val local = DateTimeFormatter.ofPattern("uuuuMMdd'T'HHmmss", Locale.ROOT)
    private val date = DateTimeFormatter.ofPattern("uuuuMMdd", Locale.ROOT)

    fun write(
        zone: ZoneId, now: Long, events: List<ExportEvent>, blocks: List<ExportBlock>, sessions: List<ExportSession>,
    ): String {
        val stamp = utc.format(Instant.ofEpochMilli(now))
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val lines = mutableListOf(
            "BEGIN:VCALENDAR", "VERSION:2.0", "PRODID:-//Tazer//Task Organizer//ID", "CALSCALE:GREGORIAN",
            "METHOD:PUBLISH", "X-WR-CALNAME:Tazer", "X-WR-TIMEZONE:${zone.id}",
        )
        fun vevent(uid: String, vararg fields: String) {
            lines += listOf("BEGIN:VEVENT", "UID:$uid", "DTSTAMP:$stamp") + fields + "END:VEVENT"
        }
        for (session in sessions.sortedBy(ExportSession::startAt)) {
            vevent(
                "session-${session.id}@tazer",
                "DTSTART:${utc.format(Instant.ofEpochMilli(session.startAt))}",
                "DTEND:${utc.format(Instant.ofEpochMilli(session.endAt))}",
                "SUMMARY:${text("Fokus: ${session.title}")}",
                "DESCRIPTION:${text(session.course)}",
                "CATEGORIES:${text("Sesi fokus")}",
            )
        }
        for (block in blocks.sortedWith(compareBy(ExportBlock::dayOfWeek, ExportBlock::startMinute))) {
            val day = DayOfWeek.of(block.dayOfWeek)
            val first = today.with(TemporalAdjusters.nextOrSame(day))
            vevent(
                "fixed-${block.id}@tazer",
                "DTSTART:${local.format(at(first, block.startMinute))}",
                "DTEND:${local.format(at(first, block.endMinute))}",
                "RRULE:FREQ=WEEKLY;BYDAY=${day.name.take(2)}",
                "SUMMARY:${text(block.label)}",
                "CATEGORIES:${text("Jadwal tetap")}",
            )
        }
        for (event in events.sortedWith(compareBy(ExportEvent::startEpochDay, ExportEvent::title))) {
            val start = LocalDate.ofEpochDay(event.startEpochDay)
            val end = LocalDate.ofEpochDay(event.endEpochDay)
            val minute = event.startMinute
            val time = if (minute == null) {
                // DTEND of an all-day event is exclusive.
                arrayOf("DTSTART;VALUE=DATE:${date.format(start)}", "DTEND;VALUE=DATE:${date.format(end.plusDays(1))}")
            } else if (end > start) {
                arrayOf("DTSTART:${local.format(at(start, minute))}", "DTEND:${local.format(at(end, minute))}")
            } else {
                arrayOf("DTSTART:${local.format(at(start, minute))}")
            }
            val description = if (event.description.isBlank()) emptyArray() else arrayOf("DESCRIPTION:${text(event.description)}")
            vevent("event-${event.id}@tazer", *time, "SUMMARY:${text(event.title)}", *description)
        }
        lines += "END:VCALENDAR"
        return lines.joinToString("") { fold(it) + "\r\n" }
    }

    private fun at(day: LocalDate, minute: Int): LocalDateTime =
        day.atStartOfDay().plusMinutes(minute.toLong())

    /** TEXT escaping from RFC 5545 section 3.3.11. */
    internal fun text(value: String): String = buildString {
        for (char in value.replace("\r\n", "\n").replace('\r', '\n')) {
            when (char) {
                '\\' -> append("\\\\")
                ';' -> append("\\;")
                ',' -> append("\\,")
                '\n' -> append("\\n")
                else -> if (char >= ' ' || char == '\t') append(char)
            }
        }
    }

    /** Folds at 75 octets without splitting a UTF-8 sequence or a surrogate pair. */
    internal fun fold(line: String): String {
        val out = StringBuilder()
        var octets = 0
        var index = 0
        while (index < line.length) {
            val codePoint = line.codePointAt(index)
            val width = String(Character.toChars(codePoint)).toByteArray(Charsets.UTF_8).size
            if (octets + width > 75) {
                out.append("\r\n ")
                octets = 1
            }
            out.appendCodePoint(codePoint)
            octets += width
            index += Character.charCount(codePoint)
        }
        return out.toString()
    }
}
