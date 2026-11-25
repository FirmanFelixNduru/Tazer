package com.tazer.taskorganizer.domain

import com.tazer.taskorganizer.domain.Scheduler.BREAK_MS
import com.tazer.taskorganizer.domain.Scheduler.MAX_FOCUS
import com.tazer.taskorganizer.domain.Scheduler.MINUTE_MS
import java.time.Instant
import java.time.ZoneId

data class FocusMoment(val wallMs: Long, val elapsedMs: Long, val bootId: String)

internal data class FocusClock(
    val bootId: String?,
    val startElapsedMs: Long?,
    val cutoffElapsedMs: Long?,
    val checkpointMs: Long?,
) {
    fun isSameBoot(now: FocusMoment): Boolean =
        bootId == now.bootId && startElapsedMs != null && now.elapsedMs >= startElapsedMs

    fun workAt(now: FocusMoment): Long {
        val checkpoint = checkpointMs?.coerceAtLeast(0L) ?: 0L
        if (!isSameBoot(now)) return checkpoint
        return maxOf(checkpoint, now.elapsedMs - requireNotNull(startElapsedMs))
    }

    fun cutoffReached(now: FocusMoment): Boolean =
        isSameBoot(now) && cutoffElapsedMs != null && now.elapsedMs >= cutoffElapsedMs
}

internal fun remainingAfterInterruption(remainingMinutes: Int, workMs: Long): Int =
    (remainingMinutes - workMs / MINUTE_MS).coerceAtLeast(1L).toInt()

/** Previous segments are from the same boot, ordered by elapsed start descending. */
internal fun continuousWorkMs(
    startElapsedMs: Long,
    workMs: Long,
    previous: Sequence<TimeBlock>,
): Long {
    var start = startElapsedMs
    var total = workMs
    for (segment in previous) {
        if (total >= MAX_FOCUS * MINUTE_MS) break
        if (start - segment.endAt >= BREAK_MS) break
        total += (minOf(start, segment.endAt) - segment.startAt).coerceAtLeast(0L)
        start = segment.startAt
    }
    return total
}

internal fun focusDurationMs(
    now: Long,
    task: SchedulingTask,
    rules: List<ScheduleRule>,
    zone: ZoneId,
    activeStart: Int,
    activeEnd: Int,
    slotEnd: Long? = null,
    continuousWorkMs: Long = 0L,
): Long {
    val day = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val days = listOf(day.minusDays(1), day, day.plusDays(1))
    val activeEndAt = days.mapNotNull { Scheduler.localBlock(it, zone, activeStart, activeEnd) }
        .filter { now >= it.startAt && now < it.endAt }
        .maxOfOrNull { it.endAt } ?: return 0L
    val nextFixedAt = days.asSequence().flatMap { Scheduler.fixedBlocksForDay(rules, it, zone).asSequence() }
        .map { it.second }
        .filter { it.endAt > now }
        .minOfOrNull { it.startAt } ?: Long.MAX_VALUE
    val deadline = task.deadlineAt.takeIf { it > now } ?: Long.MAX_VALUE
    val endAt = minOf(activeEndAt, nextFixedAt, slotEnd ?: Long.MAX_VALUE, deadline)
    if (endAt <= now || continuousWorkMs >= MAX_FOCUS * MINUTE_MS) return 0L
    val availableMs = minOf(endAt - now, MAX_FOCUS * MINUTE_MS - continuousWorkMs)
    // Starting an existing slot tolerates up to one partial minute without moving its cutoff.
    val availableMinutes = if (slotEnd != null) (availableMs + MINUTE_MS - 1) / MINUTE_MS else availableMs / MINUTE_MS
    val legalMinutes = Scheduler.chunkMinutes(task.remainingMinutes, availableMinutes.toInt())
    return minOf(legalMinutes * MINUTE_MS, availableMs)
}
