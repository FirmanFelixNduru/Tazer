package com.tazer.taskorganizer.domain

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusTest {
    private val zone = ZoneId.of("Asia/Jakarta")
    private val start = wall("09:25")
    private val task = SchedulingTask("task", wall("11:30"), start, 30, 3)

    @Test
    fun sameBootCountsMonotonicWorkAcrossWallClockChangesAndOvertime() {
        val clock = FocusClock("boot", minutes(10), minutes(35), minutes(5))
        val lastSecond = FocusMoment(wall("10:59"), minutes(35) - 1, "boot")
        val atCutoff = FocusMoment(wall("08:01"), minutes(35), "boot")
        val late = atCutoff.copy(wallMs = wall("07:55"), elapsedMs = minutes(38))
        assertFalse(clock.cutoffReached(lastSecond))
        assertEquals(minutes(25) - 1, clock.workAt(lastSecond))
        assertTrue(clock.cutoffReached(atCutoff))
        assertEquals(minutes(25), clock.workAt(atCutoff))
        assertTrue(clock.cutoffReached(late))
        assertEquals(minutes(28), clock.workAt(late))
    }

    @Test
    fun restartUsesCheckpointAndOutOfOrderSamplesDoNotEraseIt() {
        val clock = FocusClock("old", minutes(50), minutes(75), minutes(12) + 500)
        val newBoot = FocusMoment(wall("10:30"), minutes(80), "new")
        val earlierSample = newBoot.copy(bootId = "old", elapsedMs = minutes(59))
        assertFalse(clock.isSameBoot(newBoot))
        assertFalse(clock.cutoffReached(newBoot))
        assertEquals(minutes(12) + 500, clock.workAt(newBoot))
        assertEquals(minutes(12) + 500, clock.workAt(earlierSample))
        assertEquals(0L, FocusClock(null, null, null, null).workAt(newBoot))
    }

    @Test
    fun interruptionDeductsWholeMinutesAndLeavesTaskOpen() {
        assertEquals(30, remainingAfterInterruption(30, minutes(1) - 1))
        assertEquals(18, remainingAfterInterruption(30, minutes(12) + 999))
        assertEquals(1, remainingAfterInterruption(30, minutes(55)))
    }

    @Test
    fun extensionsCountEarlierSegmentsAndOnlyFullBreakRestoresContinuousBudget() {
        val previous = sequenceOf(TimeBlock(minutes(20), minutes(45)), TimeBlock(0L, minutes(10)))
        val work = continuousWorkMs(minutes(45), minutes(10), previous)
        assertEquals(minutes(35), work)
        assertEquals(minutes(15), focusDurationMs(wall("10:00"), task.copy(remainingMinutes = 15), emptyList(), zone, 480, 720,
            continuousWorkMs = work))
        assertEquals(0L, focusDurationMs(wall("10:00"), task, emptyList(), zone, 480, 720, continuousWorkMs = work))
        val shortGap = sequenceOf(TimeBlock(0L, minutes(20)))
        assertEquals(minutes(25), continuousWorkMs(minutes(30) - 1, minutes(5), shortGap))
        assertEquals(minutes(5), continuousWorkMs(minutes(30), minutes(5), shortGap))
    }

    @Test
    fun slotDeadlineAndFixedBlocksAreHardBounds() {
        val classBlock = ScheduleRule("class", "Kuliah", 1, 600, 660)
        assertEquals(0L, focusDurationMs(wall("09:52"), task, listOf(classBlock), zone, 480, 720,
            continuousWorkMs = minutes(27)))
        assertEquals(0L, focusDurationMs(wall("10:01"), task, listOf(classBlock), zone, 480, 720))
        assertEquals(minutes(30), focusDurationMs(wall("11:00"), task, listOf(classBlock), zone, 480, 720))
        assertEquals(0L, focusDurationMs(wall("11:45"), task, emptyList(), zone, 480, 720))
        assertEquals(minutes(25), focusDurationMs(start, task.copy(deadlineAt = wall("09:50")), emptyList(), zone, 480, 720))
        assertEquals(minutes(30), focusDurationMs(start, task.copy(deadlineAt = start), emptyList(), zone, 480, 720))
    }

    @Test
    fun lateStartsUseTheWholePlannedDurationIncludingShortTails() {
        assertEquals(minutes(25), focusDurationMs(start + 1, task, emptyList(), zone, 480, 720, plannedMinutes = 25))
        assertEquals(minutes(25), focusDurationMs(start + minutes(2), task, emptyList(), zone, 480, 720, plannedMinutes = 25))
        val shortTail = task.copy(remainingMinutes = 1)
        assertEquals(minutes(1), focusDurationMs(start + 1, shortTail, emptyList(), zone, 480, 720, plannedMinutes = 1))
        assertEquals(0L, focusDurationMs(start, shortTail, emptyList(), zone, 480, 720, plannedMinutes = 2))
        assertEquals(minutes(25), focusDurationMs(start, task, emptyList(), zone, 480, 720, plannedMinutes = 25))
    }

    @Test
    fun plannedStartRequiresTheEntireDurationBeforeEveryHardBoundary() {
        val classBlock = ScheduleRule("class", "Kuliah", 1, 590, 660)
        val dueTask = task.copy(deadlineAt = wall("09:50"))
        assertEquals(minutes(25), focusDurationMs(start, task, listOf(classBlock), zone, 480, 720, plannedMinutes = 25))
        assertEquals(0L, focusDurationMs(start + 1, task, listOf(classBlock), zone, 480, 720, plannedMinutes = 25))
        assertEquals(0L, focusDurationMs(start + 1, task, emptyList(), zone, 480, 590, plannedMinutes = 25))
        assertEquals(0L, focusDurationMs(start + 1, dueTask, emptyList(), zone, 480, 720, plannedMinutes = 25))
        assertEquals(minutes(25), focusDurationMs(dueTask.deadlineAt, dueTask, emptyList(), zone, 480, 720,
            plannedMinutes = 25))
        assertEquals(0L, focusDurationMs(start, task.copy(remainingMinutes = 60), emptyList(), zone, 480, 720,
            plannedMinutes = 51))
    }

    @Test
    fun narrowWindowOnlyAllowsWholeShortTailAndHonorsMillisecondCap() {
        val twenty = task.copy(remainingMinutes = 20)
        assertEquals(minutes(20), focusDurationMs(start, twenty, emptyList(), zone, 480, 720, continuousWorkMs = minutes(30)))
        assertEquals(0L, focusDurationMs(start, twenty, emptyList(), zone, 480, 720, continuousWorkMs = minutes(30) + 1))
        assertEquals(minutes(30), focusDurationMs(start, task.copy(remainingMinutes = 55), emptyList(), zone, 480, 605))
    }

    private fun wall(time: String) = Instant.parse("2026-11-23T$time:00+07:00").toEpochMilli()
    private fun minutes(value: Int) = value * 60_000L
}
