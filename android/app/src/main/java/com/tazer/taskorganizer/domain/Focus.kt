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
    val workBeforeSegmentMs: Long = 0L,
    val paused: Boolean = false,
) {
    fun isSameBoot(now: FocusMoment): Boolean =
        bootId == now.bootId && startElapsedMs != null && now.elapsedMs >= startElapsedMs

    fun workAt(now: FocusMoment): Long {
        val checkpoint = checkpointMs?.coerceAtLeast(0L) ?: 0L
        if (paused || !isSameBoot(now)) return checkpoint
        return maxOf(checkpoint, workBeforeSegmentMs + now.elapsedMs - requireNotNull(startElapsedMs))
    }

    fun segmentWorkAt(now: FocusMoment): Long = (workAt(now) - workBeforeSegmentMs).coerceAtLeast(0L)

    fun remainingAt(now: FocusMoment): Long? =
        cutoffElapsedMs?.takeIf { !paused && isSameBoot(now) }?.minus(now.elapsedMs)

    fun cutoffReached(now: FocusMoment): Boolean =
        remainingAt(now)?.let { it <= 0L } ?: false

    fun breakTaken(now: FocusMoment): Boolean =
        !isSameBoot(now) || now.elapsedMs - requireNotNull(startElapsedMs) - segmentWorkAt(now) >= BREAK_MS
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
    plannedMinutes: Int? = null,
    continuousWorkMs: Long = 0L,
): Long {
    val availableMs = focusAvailableMs(now, task.deadlineAt, rules, zone, activeStart, activeEnd, continuousWorkMs)
    if (plannedMinutes != null) {
        val plannedMs = plannedMinutes * MINUTE_MS
        return if (plannedMinutes in 1..minOf(MAX_FOCUS, task.remainingMinutes) && plannedMs <= availableMs) {
            plannedMs
        } else 0L
    }
    return Scheduler.chunkMinutes(task.remainingMinutes, (availableMs / MINUTE_MS).toInt()) * MINUTE_MS
}

internal fun focusAvailableMs(
    now: Long,
    deadlineAt: Long,
    rules: List<ScheduleRule>,
    zone: ZoneId,
    activeStart: Int,
    activeEnd: Int,
    continuousWorkMs: Long,
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
    val deadline = deadlineAt.takeIf { it > now } ?: Long.MAX_VALUE
    val endAt = minOf(activeEndAt, nextFixedAt, deadline)
    if (endAt <= now || continuousWorkMs >= MAX_FOCUS * MINUTE_MS) return 0L
    return minOf(endAt - now, MAX_FOCUS * MINUTE_MS - continuousWorkMs)
}
