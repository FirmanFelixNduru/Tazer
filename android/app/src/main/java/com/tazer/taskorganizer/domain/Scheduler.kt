package com.tazer.taskorganizer.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class SchedulingTask(
    val id: String,
    val deadlineAt: Long,
    val createdAt: Long,
    val remainingMinutes: Int,
    val difficultyScore: Int,
)

data class ScheduleRule(
    val id: String,
    val label: String,
    val dayOfWeek: Int,
    val startMinute: Int,
    val endMinute: Int,
)

data class TimeBlock(val startAt: Long, val endAt: Long)
data class PlannedWork(val taskId: String, val startAt: Long, val endAt: Long, val minutes: Int)
data class DeadlineRisk(val taskId: String, val missingMinutes: Int)
data class SchedulePlan(val sessions: List<PlannedWork>, val risks: List<DeadlineRisk>)

object Scheduler {
    internal const val MINUTE_MS = 60_000L
    private const val HORIZON_DAYS = 7L

    fun priorityScore(task: SchedulingTask, now: Long): Int {
        val untilDeadline = task.deadlineAt - now
        val urgencyPoints = when {
            now >= task.deadlineAt -> 750
            untilDeadline < 0 -> 0 // The positive interval overflowed and rounds to zero urgency.
            else -> {
                val window = untilDeadline.coerceAtLeast(MINUTE_MS)
                val workMs = (task.remainingMinutes * MINUTE_MS).coerceIn(0, window)
                ((750 * workMs + window / 2) / window).toInt()
            }
        }
        return urgencyPoints + 50 * task.difficultyScore
    }

    fun rank(tasks: List<SchedulingTask>, now: Long): List<SchedulingTask> =
        rankedWork(tasks, now).map { it.task }

    /** Remaining estimates already exclude retained work. Reserved intervals include any locked breaks. */
    fun plan(
        tasks: List<SchedulingTask>,
        rules: List<ScheduleRule>,
        zone: ZoneId,
        activeStartMinute: Int,
        activeEndMinute: Int,
        now: Long,
        busyUntilAt: Long? = null,
        reserved: List<TimeBlock> = emptyList(),
        preset: FocusPreset = FocusPreset.DEFAULT,
    ): SchedulePlan {
        val work = rankedWork(tasks, now)
        if (work.isEmpty()) return SchedulePlan(emptyList(), emptyList())

        val startAt = maxOf(now, busyUntilAt ?: now)
        val slots = freeSlots(rules, zone, activeStartMinute, activeEndMinute, startAt, reserved)
        val sessions = allocate(work, slots, preset)
        val risks = work.mapNotNull { item ->
            val missing = if (item.task.deadlineAt <= now) item.task.remainingMinutes else item.remaining
            DeadlineRisk(item.task.id, missing).takeIf { missing > 0 }
        }
        return SchedulePlan(sessions, risks)
    }

    fun fixedBlocksForDay(
        rules: List<ScheduleRule>,
        day: LocalDate,
        zone: ZoneId,
    ): List<Pair<ScheduleRule, TimeBlock>> = rules.asSequence()
        .filter { it.dayOfWeek == day.dayOfWeek.value }
        .mapNotNull { rule -> localBlock(day, zone, rule.startMinute, rule.endMinute)?.let { rule to it } }
        .sortedWith(compareBy({ it.second.startAt }, { it.second.endAt }, { it.first.id }))
        .toList()

    internal fun localBlock(day: LocalDate, zone: ZoneId, start: Int, end: Int): TimeBlock? {
        if (start !in 0..<end || end > 1440) return null
        val midnight = day.atTime(0, 0)
        val startAt = midnight.plusMinutes(start.toLong()).atZone(zone).toInstant().toEpochMilli()
        val endAt = midnight.plusMinutes(end.toLong()).atZone(zone)
            .withLaterOffsetAtOverlap().toInstant().toEpochMilli()
        return TimeBlock(startAt, endAt).takeIf { startAt < endAt }
    }

    private fun freeSlots(
        rules: List<ScheduleRule>,
        zone: ZoneId,
        activeStart: Int,
        activeEnd: Int,
        startAt: Long,
        reserved: List<TimeBlock>,
    ): List<TimeBlock> {
        val firstDay = Instant.ofEpochMilli(startAt).atZone(zone).toLocalDate()
        val active = union((0L until HORIZON_DAYS).mapNotNull { offset ->
            localBlock(firstDay.plusDays(offset), zone, activeStart, activeEnd)
        })
        // Adjacent dates can overlap the horizon when an offset transition crosses midnight.
        val fixed = (-1L..HORIZON_DAYS).flatMap { offset ->
            fixedBlocksForDay(rules, firstDay.plusDays(offset), zone).map { it.second }
        }
        val blocked = union(fixed + reserved)

        return buildList {
            var blockerIndex = 0
            for (interval in active) {
                var cursor = maxOf(startAt, interval.startAt)
                while (cursor < interval.endAt && blockerIndex < blocked.size) {
                    val blocker = blocked[blockerIndex]
                    if (blocker.startAt >= interval.endAt) break
                    if (blocker.endAt > cursor) {
                        addAlignedSlot(cursor, blocker.startAt)
                        cursor = blocker.endAt
                    }
                    // A blocker ending after this interval may also block the next one.
                    if (blocker.endAt > interval.endAt) break
                    blockerIndex++
                }
                addAlignedSlot(cursor, interval.endAt)
            }
        }
    }

    private fun union(blocks: List<TimeBlock>): List<TimeBlock> {
        val sorted = blocks.filter { it.startAt < it.endAt }.sortedBy { it.startAt }
        return buildList {
            for (block in sorted) {
                val last = lastOrNull()
                if (last != null && block.startAt <= last.endAt) {
                    this[lastIndex] = TimeBlock(last.startAt, maxOf(last.endAt, block.endAt))
                } else {
                    add(block)
                }
            }
        }
    }

    private fun MutableList<TimeBlock>.addAlignedSlot(startAt: Long, endAt: Long) {
        if (startAt >= endAt) return
        val start = Math.floorDiv(startAt, MINUTE_MS) + if (startAt % MINUTE_MS == 0L) 0 else 1
        val end = Math.floorDiv(endAt, MINUTE_MS)
        if (start < end) add(TimeBlock(start * MINUTE_MS, end * MINUTE_MS))
    }

    private fun allocate(work: List<Work>, slots: List<TimeBlock>, preset: FocusPreset): List<PlannedWork> = buildList {
        assignQuotas(work, slots.sumOf { (it.endAt - it.startAt) / MINUTE_MS })
        var nextFocusAt = Long.MIN_VALUE
        var recalculate = false
        for ((index, slot) in slots.withIndex()) {
            val remainingSlots = slots.subList(index, slots.size)
            var cursor = maxOf(slot.startAt, nextFocusAt)
            while (cursor < slot.endAt) {
                val options = work.mapNotNull { item ->
                    val available = availableMinutes(item, cursor, slot.endAt)
                    val minutes = chunkMinutes(item.remaining, available, preset)
                    Option(item, available, minutes).takeIf { minutes > 0 }
                }
                if (options.isEmpty()) break

                if (recalculate) redistribute(work, remainingSlots, cursor, preset)
                var preferred = withinQuota(options, remainingSlots, cursor, preset)
                if (preferred == null && !recalculate) {
                    redistribute(work, remainingSlots, cursor, preset)
                    preferred = withinQuota(options, remainingSlots, cursor, preset)
                }
                val chosen = preservePriority(options, preferred ?: options.first(), remainingSlots)
                val item = chosen.work
                val endAt = cursor + chosen.minutes * MINUTE_MS
                add(PlannedWork(item.task.id, cursor, endAt, chosen.minutes))
                // Preserve usable quotas when priority takes precedence over an affordable lower-ranked chunk.
                recalculate = preferred == null
                item.quota = (item.quota - chosen.minutes).coerceAtLeast(0)
                item.remaining -= chosen.minutes
                if (work.all { it.remaining == 0 }) return@buildList

                nextFocusAt = endAt + preset.breakMs
                cursor = nextFocusAt
            }
        }
    }

    private fun withinQuota(options: List<Option>, slots: List<TimeBlock>, cursor: Long, preset: FocusPreset): Option? {
        val affordable = options.mapNotNull { option ->
            val item = option.work
            val minutes = chunkMinutes(item.remaining, minOf(option.available, item.quota), preset)
            if (minutes > 0) option.copy(minutes = minutes) else null
        }
        val rankedFirst = affordable.firstOrNull() ?: return null
        val nextFocusAt = cursor + rankedFirst.minutes * MINUTE_MS + preset.breakMs
        return affordable.filter { option ->
            val item = option.work
            val minimum = minOf(preset.minimumMinutes, item.remaining) * MINUTE_MS
            val next = slots.asSequence().drop(1).firstOrNull { it.endAt - maxOf(it.startAt, nextFocusAt) >= minimum }
            if (next != null) item.cutoff < maxOf(next.startAt, nextFocusAt) + minimum
            else item.cutoff <= slots.last().endAt
        }.minByOrNull { it.work.cutoff } ?: rankedFirst
    }

    private fun preservePriority(options: List<Option>, preferred: Option, slots: List<TimeBlock>): Option {
        val projectedTotal = preferred.work.placed + preferred.minutes
        return options.firstOrNull { option ->
            val higher = option.work
            higher.weight > preferred.work.weight && higher.placed < projectedTotal &&
                higher.task.remainingMinutes >= projectedTotal &&
                sameDeadlineWindow(higher, preferred.work, slots)
        } ?: preferred
    }

    private fun sameDeadlineWindow(first: Work, second: Work, slots: List<TimeBlock>): Boolean {
        val earlier = Math.floorDiv(minOf(first.cutoff, second.cutoff), MINUTE_MS)
        val later = Math.floorDiv(maxOf(first.cutoff, second.cutoff), MINUTE_MS)
        // Deadlines in the same gap do not change which working minutes are available.
        return earlier == later || slots.none {
            it.startAt / MINUTE_MS < later && it.endAt / MINUTE_MS > earlier
        }
    }

    private fun redistribute(work: List<Work>, slots: List<TimeBlock>, cursor: Long, preset: FocusPreset) {
        for (item in work) item.quota = 0
        val eligible = work.filter { item ->
            slots.any { slot ->
                chunkMinutes(item.remaining, availableMinutes(item, maxOf(cursor, slot.startAt), slot.endAt), preset) > 0
            }
        }
        val capacity = slots.sumOf { slot ->
            val startAt = maxOf(cursor, slot.startAt)
            // Count the shared window once, up to the latest usable deadline in this slot.
            eligible.maxOfOrNull { item ->
                val available = availableMinutes(item, startAt, slot.endAt)
                if (chunkMinutes(item.remaining, available, preset) > 0) available.toLong() else 0L
            } ?: 0L
        }
        assignQuotas(eligible, capacity)
    }

    private fun assignQuotas(work: List<Work>, capacity: Long) {
        var left = minOf(capacity, work.sumOf { it.remaining.toLong() })
        var totalWeight = work.sumOf { if (it.remaining > 0) it.weight.toLong() else 0L }
        val open = work.withIndex().filter { it.value.remaining > 0 }.sortedWith { first, second ->
            val a = first.value
            val b = second.value
            (a.remaining.toLong() * b.weight).compareTo(b.remaining.toLong() * a.weight)
        }
        // Caps encountered in ascending need/weight order can be resolved in one pass.
        var cappedCount = 0
        for ((_, item) in open) {
            if (item.remaining * totalWeight > left * item.weight) break
            item.quota = item.remaining
            left -= item.remaining
            totalWeight -= item.weight
            cappedCount++
        }
        val uncapped = open.subList(cappedCount, open.size)
        if (uncapped.isEmpty()) return

        var extra = left
        for ((_, item) in uncapped) {
            item.quota = (left * item.weight / totalWeight).toInt()
            extra -= item.quota
        }
        uncapped.sortedWith(compareByDescending<IndexedValue<Work>> { left * it.value.weight % totalWeight }
            .thenBy { it.index }).take(extra.toInt()).forEach { it.value.quota++ }
    }

    private fun availableMinutes(work: Work, startAt: Long, endAt: Long): Int {
        val cutoff = minOf(endAt, work.cutoff)
        return if (startAt < cutoff) ((cutoff - startAt) / MINUTE_MS).toInt() else 0
    }

    internal fun chunkMinutes(remaining: Int, available: Int, preset: FocusPreset = FocusPreset.DEFAULT): Int {
        val longest = minOf(remaining, available, preset.workMinutes)
        val minimum = preset.minimumMinutes
        if (longest < minOf(minimum, remaining)) return 0
        if (longest == remaining || remaining - longest >= minimum) return longest
        val withoutShortTail = remaining - minimum
        return if (withoutShortTail >= minimum) withoutShortTail else longest
    }

    private fun rankedWork(tasks: List<SchedulingTask>, now: Long): List<Work> = tasks.asSequence()
        .filter { it.remainingMinutes > 0 }
        .map { Work(it, priorityScore(it, now), if (it.deadlineAt <= now) Long.MAX_VALUE else it.deadlineAt) }
        .sortedWith(compareByDescending<Work> { it.weight }
            .thenBy { it.task.deadlineAt }.thenBy { it.task.createdAt }.thenBy { it.task.id })
        .toList()

    private class Work(val task: SchedulingTask, val weight: Int, val cutoff: Long) {
        var remaining = task.remainingMinutes
        var quota = 0
        val placed: Int get() = task.remainingMinutes - remaining
    }

    private data class Option(val work: Work, val available: Int, val minutes: Int)
}
