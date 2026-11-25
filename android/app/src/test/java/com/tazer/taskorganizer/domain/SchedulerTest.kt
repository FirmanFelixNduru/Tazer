package com.tazer.taskorganizer.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SchedulerTest {
    private val utc = ZoneId.of("UTC")
    private val monday = LocalDate.parse("2026-11-23")
    private val minute = 60_000L
    private val now = at(0)

    @Test
    fun priorityUsesCalendarMinutesHalfUpAndTimestampTieBreaks() {
        val exactHalf = task("half", 1, deadline = at(20), difficulty = 1)
        assertEquals(88, Scheduler.priorityScore(exactHalf, now))
        assertEquals(800, Scheduler.priorityScore(exactHalf.copy(deadlineAt = now), now))
        assertEquals(800, Scheduler.priorityScore(exactHalf.copy(deadlineAt = now + 1), now))
        val same = task("z", 25, deadline = now)
        val earlierDeadline = same.copy(id = "deadline", deadlineAt = now - 1)
        val earlierCreation = same.copy(id = "creation", createdAt = now - 1)
        val earlierId = same.copy(id = "a")
        val done = same.copy(id = "done", remainingMinutes = 0)
        assertEquals(
            listOf(earlierDeadline, earlierCreation, earlierId, same),
            Scheduler.rank(listOf(done, same, earlierId, earlierCreation, earlierDeadline), now),
        )
    }

    @Test
    fun weightedSharesUseRankAndRespectCaps() {
        val deadline = at(13_500)
        val high = task("high", 9_000, deadline, difficulty = 5)
        val low = task("low", 3_600, deadline, difficulty = 1)
        assertEquals(750, Scheduler.priorityScore(high, now))
        assertEquals(250, Scheduler.priorityScore(low, now))
        val weighted = dayPlan(listOf(low, high), 0, 140, reserved = listOf(block(50, 60), block(100, 110)))
        assertEquals(listOf("high" to 50, "high" to 40, "low" to 30), weighted.allocations())

        val capped = dayPlan(
            listOf(low, high.copy(remainingMinutes = 30)),
            0, 140,
            reserved = listOf(block(30, 40), block(90, 100)),
        )
        assertEquals(listOf("high" to 30, "low" to 50, "low" to 40), capped.allocations())
    }

    @Test
    fun equalFractionalSharesGrantTheExtraMinuteByRank() {
        val tasks = listOf(task("b", 120), task("a", 120))
        val plan = dayPlan(tasks, 0, 61, reserved = listOf(block(26, 36)))
        assertEquals(listOf("a" to 26, "b" to 25), plan.allocations())
        assertEquals(plan, dayPlan(tasks.reversed(), 0, 61, reserved = listOf(block(26, 36))))
    }

    @Test
    fun blocksBelowBothQuotasFallBackToOneLegalFocusBlock() {
        val plan = dayPlan(listOf(task("b", 90), task("a", 90)), 0, 48)
        assertEquals(listOf("a" to 48), plan.allocations())
        assertEquals(listOf(DeadlineRisk("a", 42), DeadlineRisk("b", 90)), plan.risks)
    }

    @Test
    fun equalWindowsProtectHigherWeightWhenAnAvailableBlockWouldReverseTheTotal() {
        val high = task("high", 110)
        val low = task("low", 90)
        val plan = dayPlan(listOf(low, high), 0, 130, reserved = listOf(block(35, 45), block(95, 105)))
        assertEquals(listOf("high" to 35, "high" to 50, "low" to 25), plan.allocations())
    }

    @Test
    fun redistributesQuotasFromTasksWithNoEligibleDeadlineWindow() {
        val impossible = task("impossible", 120, deadline = at(24), difficulty = 5)
        val flexible = task("flexible", 90, difficulty = 1)
        val plan = dayPlan(listOf(impossible, flexible), 0, 100)
        assertEquals(listOf("flexible" to 50, "flexible" to 40), plan.allocations())
        assertEquals(listOf(DeadlineRisk("impossible", 120)), plan.risks)
    }

    @Test
    fun splitsFocusWithoutCreatingAnAvoidableShortTail() {
        val plan = dayPlan(listOf(task("split", 61)), 0, 71)
        assertEquals(listOf("split" to 36, "split" to 25), plan.allocations())
        assertEquals(listOf(at(0), at(46)), plan.sessions.map { it.startAt })
        assertEquals(emptyList<DeadlineRisk>(), plan.risks)
    }

    @Test
    fun canUseShortSlotOnlyForAnEntireSmallRemainder() {
        val plan = dayPlan(listOf(task("large", 25), task("small", 24)), 0, 24)
        assertEquals(listOf("small" to 24), plan.allocations())
        assertEquals(listOf(DeadlineRisk("large", 25)), plan.risks)
        val unavoidableTail = dayPlan(listOf(task("tail", 26)), 0, 36, reserved = listOf(block(25, 35)))
        assertEquals(listOf("tail" to 25, "tail" to 1), unavoidableTail.allocations())
    }

    @Test
    fun mandatoryBreakExtendsAcrossAFixedIntervalAndCanEndAfterActiveHours() {
        val tasks = listOf(task("a", 25), task("b", 25))
        val plan = dayPlan(tasks, 0, 60, rules = listOf(rule("fixed", 25, 30)))
        assertEquals(
            listOf(PlannedWork("a", at(0), at(25), 25), PlannedWork("b", at(35), at(60), 25)),
            plan.sessions,
        )
        assertTrue(plan.risks.isEmpty())
    }

    @Test
    fun subtractsTheUnionOfOverlappingRulesAndReservationsBeforeRounding() {
        val rules = listOf(rule("two", 40, 65), rule("one", 30, 50), rule("tuesday", 0, 100, day = 2))
        val plan = dayPlan(
            listOf(task("task", 90)),
            0, 110,
            rules = rules,
            reserved = listOf(TimeBlock(at(60), at(80) + 1)),
            busyUntilAt = now + 1,
        )
        assertEquals(listOf(block(1, 30), block(81, 110)), plan.sessions.map { TimeBlock(it.startAt, it.endAt) })
        assertEquals(listOf(DeadlineRisk("task", 32)), plan.risks)
        assertEquals(listOf("one", "two"), Scheduler.fixedBlocksForDay(rules, monday, utc).map { it.first.id })
    }

    @Test
    fun roundingBeforeEpochUsesAbsoluteMinuteBoundaries() {
        val task = SchedulingTask("epoch", 30 * minute, -minute, 25, 1)
        val plan = Scheduler.plan(listOf(task), emptyList(), utc, 0, 1440, -1)
        assertEquals(listOf(PlannedWork("epoch", 0, 25 * minute, 25)), plan.sessions)
    }

    @Test
    fun deadlineInCurrentSlotGetsPreferenceWhenBothQuotasFit() {
        val overdue = task("overdue", 30, deadline = now, difficulty = 5)
        val due = task("due", 30, deadline = at(30), difficulty = 1)
        val plan = dayPlan(listOf(overdue, due), 0, 70)
        assertEquals(listOf("due" to 30, "overdue" to 30), plan.allocations())
        assertEquals(at(30), plan.sessions.first().endAt)
        assertEquals(listOf(DeadlineRisk("overdue", 30)), plan.risks)
    }

    @Test
    fun anExpiringTaskWithNoLegalQuotaDoesNotDisplaceAFeasibleQuota() {
        val overdue = task("overdue", 120, deadline = now, difficulty = 5)
        val due = task("due", 25, deadline = at(25), difficulty = 1)
        val plan = dayPlan(listOf(due, overdue), 0, 48)
        assertEquals(listOf("overdue" to 27), plan.allocations())
        assertEquals(listOf(DeadlineRisk("overdue", 120), DeadlineRisk("due", 25)), plan.risks)
    }

    @Test
    fun considersTheMandatoryBreakWhenCheckingTheNextDeadlineOpportunity() {
        val high = task("high", 30, deadline = now, difficulty = 5)
        val due = task("due", 25, deadline = at(60), difficulty = 1)
        val plan = dayPlan(listOf(high, due), 0, 71, reserved = listOf(block(30, 31)))
        assertEquals(listOf("due" to 25, "high" to 30), plan.allocations())
        assertEquals(at(35), plan.sessions.last().startAt)
    }

    @Test
    fun deadlineAtEndOfNextFeasibleBlockAllowsRankToWinCurrentSlot() {
        val high = task("high", 25, difficulty = 5)
        val lower = task("lower", 25, deadline = at(5_050), difficulty = 1)
        val nextSlot = block(5_025, 5_050)
        val result = Scheduler.plan(
            listOf(lower, high), emptyList(), utc, 0, 1440, now,
            reserved = listOf(block(25, 5_025), block(5_050, 10_080)),
        )
        assertEquals(listOf("high" to 25, "lower" to 25), result.allocations())
        assertEquals(nextSlot.endAt, result.sessions.last().endAt)

        val earlier = Scheduler.plan(
            listOf(lower.copy(deadlineAt = nextSlot.endAt - 1), high), emptyList(), utc, 0, 1440, now,
            reserved = listOf(block(25, 5_025), block(5_050, 10_080)),
        )
        assertEquals(listOf("lower" to 25, "high" to 25), earlier.allocations())
    }

    @Test
    fun futureDeadlineIsFlooredToWholeMinutesAndDoesNotSpillIntoLaterSlots() {
        val due = task("due", 60, deadline = at(31) + 59_999)
        val plan = Scheduler.plan(listOf(due), emptyList(), utc, 0, 90, now)
        assertEquals(listOf(PlannedWork("due", at(0), at(31), 31)), plan.sessions)
        assertEquals(listOf(DeadlineRisk("due", 29)), plan.risks)
    }

    @Test
    fun busyUntilMovesTheSevenDateHorizonAndClipsTheFirstDay() {
        val finish = at(1440 * 2 + 30)
        val plan = Scheduler.plan(listOf(task("late", 300)), emptyList(), utc, 0, 60, now, busyUntilAt = finish)
        assertEquals((2L..8L).map(monday::plusDays), plan.sessions.map {
            Instant.ofEpochMilli(it.startAt).atZone(utc).toLocalDate()
        }.distinct())
        assertEquals(finish, plan.sessions.first().startAt)
        assertTrue(plan.sessions.all { it.endAt <= at(8 * 1440 + 60) })
    }

    @Test
    fun dstGapShiftsEndpointsAndOverlapUsesBothOccurrences() {
        val ny = ZoneId.of("America/New_York")
        val spring = LocalDate.parse("2026-03-08")
        val fall = LocalDate.parse("2026-11-01")
        val gap = rule("gap", 130, 220, day = 7)
        val inverted = rule("inverted", 150, 195, day = 7)
        assertEquals(
            listOf(gap to TimeBlock(epoch("2026-03-08T07:10:00Z"), epoch("2026-03-08T07:40:00Z"))),
            Scheduler.fixedBlocksForDay(listOf(gap, inverted), spring, ny),
        )
        val overlap = rule("overlap", 75, 105, day = 7)
        assertEquals(
            listOf(overlap to TimeBlock(epoch("2026-11-01T05:15:00Z"), epoch("2026-11-01T06:45:00Z"))),
            Scheduler.fixedBlocksForDay(listOf(overlap), fall, ny),
        )
        val task = task("focus", 80, deadline = epoch("2026-11-01T07:00:00Z"))
        val plan = Scheduler.plan(listOf(task), emptyList(), ny, 75, 105, epoch("2026-11-01T04:00:00Z"))
        assertEquals(listOf("focus" to 50, "focus" to 30), plan.allocations())
        assertEquals(epoch("2026-11-01T06:45:00Z"), plan.sessions.last().endAt)
    }

    @Test
    fun aReservationSpanningSeveralActiveIntervalsRemainsBlocked() {
        val reservation = block(0, 1440 * 4 + 15)
        val plan = Scheduler.plan(listOf(task("task", 100)), emptyList(), utc, 0, 40, now, reserved = listOf(reservation))
        assertEquals(reservation.endAt, plan.sessions.first().startAt)
        assertTrue(plan.sessions.all { it.startAt >= reservation.endAt })
        assertEquals(100, plan.sessions.sumOf { it.minutes })
    }

    @Test
    fun generatedPlansRespectInvariantsAndInputOrderAcrossReplans() {
        val random = Random(681)
        repeat(60) {
            val tasks = List(12) { index ->
                task("task-$index", random.nextInt(1, 180), at(random.nextInt(-35, 12_000)), random.nextInt(1, 6))
            }
            val rules = List(18) { index ->
                val start = random.nextInt(450, 1_000)
                rule("fixed-$index", start, start + random.nextInt(1, 150), day = random.nextInt(1, 8))
            }
            val reserved = List(8) {
                val start = random.nextInt(0, 10_000)
                TimeBlock(at(start) + 1, at(start + random.nextInt(1, 150)) + 1)
            }
            val plan = Scheduler.plan(tasks, rules, utc, 480, 1_080, now, reserved = reserved)
            assertEquals(
                plan,
                Scheduler.plan(tasks.shuffled(random), rules.shuffled(random), utc, 480, 1_080, now, reserved = reserved.reversed()),
            )
            assertTrue(plan.sessions.zipWithNext().all { (a, b) -> b.startAt - a.endAt >= 10 * minute })
            val remaining = tasks.associate { it.id to it.remainingMinutes }.toMutableMap()
            for (session in plan.sessions) {
                val date = Instant.ofEpochMilli(session.startAt).atZone(utc).toLocalDate()
                val startOfDay = date.atStartOfDay(utc).toInstant().toEpochMilli()
                val blocked = reserved + Scheduler.fixedBlocksForDay(rules, date, utc).map { it.second }
                val need = remaining.getValue(session.taskId)
                val task = tasks.single { it.id == session.taskId }
                assertTrue(date >= monday && date < monday.plusDays(7))
                assertTrue(session.startAt >= startOfDay + 480 * minute && session.endAt <= startOfDay + 1_080 * minute)
                assertTrue(blocked.none { session.startAt < it.endAt && session.endAt > it.startAt })
                assertTrue(session.startAt % minute == 0L && session.endAt - session.startAt == session.minutes * minute)
                assertTrue(session.minutes in 25..minOf(50, need) || (need < 25 && session.minutes == need))
                assertTrue(task.deadlineAt <= now || session.endAt <= task.deadlineAt)
                remaining[task.id] = need - session.minutes
            }
            assertEquals(
                tasks.associate { it.id to if (it.deadlineAt <= now) it.remainingMinutes else remaining.getValue(it.id) }
                    .filterValues { it > 0 },
                plan.risks.associate { it.taskId to it.missingMinutes },
            )
        }
    }

    private fun SchedulePlan.allocations() = sessions.map { it.taskId to it.minutes }

    private fun dayPlan(
        tasks: List<SchedulingTask>,
        activeStart: Int,
        activeEnd: Int,
        rules: List<ScheduleRule> = emptyList(),
        reserved: List<TimeBlock> = emptyList(),
        busyUntilAt: Long? = null,
    ): SchedulePlan = Scheduler.plan(
        tasks, rules, utc, activeStart, activeEnd, now, busyUntilAt,
        reserved + block(1440, 10_080),
    )

    private fun task(id: String, minutes: Int, deadline: Long = at(13_000), difficulty: Int = 3) =
        SchedulingTask(id, deadline, now, minutes, difficulty)

    private fun rule(id: String, start: Int, end: Int, day: Int = 1) = ScheduleRule(id, id, day, start, end)
    private fun block(start: Int, end: Int) = TimeBlock(at(start), at(end))
    private fun at(minutes: Int) = monday.atStartOfDay(utc).toInstant().toEpochMilli() + minutes * minute
    private fun epoch(instant: String) = Instant.parse(instant).toEpochMilli()
}
