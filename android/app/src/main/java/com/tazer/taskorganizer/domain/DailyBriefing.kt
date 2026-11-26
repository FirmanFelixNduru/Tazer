package com.tazer.taskorganizer.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class BriefingKind { MORNING, EVENING }

data class BriefingSettings(
    val morningEnabled: Boolean = false,
    val morningMinute: Int = 7 * 60,
    val eveningEnabled: Boolean = false,
    val eveningMinute: Int = 21 * 60,
) {
    fun enabled(kind: BriefingKind) = if (kind == BriefingKind.MORNING) morningEnabled else eveningEnabled
    fun minute(kind: BriefingKind) = if (kind == BriefingKind.MORNING) morningMinute else eveningMinute
}

data class BriefingTask(
    val id: String,
    val title: String,
    val deadlineAt: Long,
    val createdAt: Long,
    val remainingMinutes: Int,
    val difficultyScore: Int,
    val completedAt: Long?,
)

/** [open] covers sessions still pending or running; [done] covers sessions closed as done. */
data class BriefingSession(val startAt: Long, val minutes: Int, val done: Boolean, val open: Boolean)

data class BriefingMessage(val title: String, val body: String)

object DailyBriefing {
    private val deadlineFormat = DateTimeFormatter.ofPattern("EEE, d MMM HH:mm", Locale.forLanguageTag("id-ID"))

    /** Next wall-clock occurrence of [minute] strictly after [now]; DST gaps move forward. */
    fun nextTrigger(now: Long, zone: ZoneId, minute: Int): Long {
        require(minute in 0 until 1440)
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        fun at(day: LocalDate) = day.atTime(minute / 60, minute % 60).atZone(zone).toInstant().toEpochMilli()
        return at(today).takeIf { it > now } ?: at(today.plusDays(1))
    }

    fun morning(now: Long, zone: ZoneId, tasks: List<BriefingTask>, sessions: List<BriefingSession>): BriefingMessage {
        val today = sessions.today(now, zone).filter { it.open || it.done }
        val minutes = today.sumOf { it.minutes }
        val open = tasks.filter { it.completedAt == null }
        val top = Scheduler.rank(
            open.map { SchedulingTask(it.id, it.deadlineAt, it.createdAt, it.remainingMinutes, it.difficultyScore) }, now,
        ).firstOrNull()?.let { ranked -> open.first { it.id == ranked.id } }
        val title = if (today.isEmpty()) "Hari ini belum ada sesi fokus"
            else "Hari ini: ${today.size} sesi fokus · ${duration(minutes)}"
        val body = if (top == null) {
            "Belum ada tugas aktif. Tambahkan tugas agar Tazer bisa menyusun jadwalmu."
        } else {
            val deadline = deadlineFormat.format(Instant.ofEpochMilli(top.deadlineAt).atZone(zone))
            "Prioritas #1: ${top.title} (deadline $deadline). Ketuk untuk membuka jadwal hari ini."
        }
        return BriefingMessage(title, body)
    }

    fun evening(now: Long, zone: ZoneId, tasks: List<BriefingTask>, sessions: List<BriefingSession>): BriefingMessage {
        val today = sessions.today(now, zone)
        val done = today.count { it.done }
        val left = today.count { it.open }
        val (start, end) = window(now, zone)
        val finished = tasks.count { it.completedAt != null && it.completedAt in start until end }
        val tasksLine = if (finished > 0) " dan $finished tugas" else ""
        return when {
            done + left == 0 && finished == 0 -> BriefingMessage(
                "Review malam: hari ini tanpa sesi fokus",
                "Ketuk untuk melihat statistik dan menyiapkan jadwal besok.",
            )
            left == 0 -> BriefingMessage(
                "Semua target hari ini beres",
                "$done sesi fokus$tasksLine selesai. Istirahat yang cukup, lalu lanjutkan besok.",
            )
            else -> BriefingMessage(
                "Review malam: $done dari ${done + left} sesi selesai",
                (if (finished > 0) "$finished tugas selesai hari ini. " else "") +
                    "$left sesi masih tersisa. Ketuk untuk evaluasi di Statistik dan susun ulang jadwal besok.",
            )
        }
    }

    private fun window(now: Long, zone: ZoneId): Pair<Long, Long> {
        val day = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return day.atStartOfDay(zone).toInstant().toEpochMilli() to day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    }

    private fun List<BriefingSession>.today(now: Long, zone: ZoneId): List<BriefingSession> {
        val (start, end) = window(now, zone)
        return filter { it.startAt in start until end }
    }

    internal fun duration(minutes: Int): String = when {
        minutes < 60 -> "$minutes mnt"
        minutes % 60 == 0 -> "${minutes / 60} jam"
        else -> "${minutes / 60} jam ${minutes % 60} mnt"
    }
}
