package com.tazer.taskorganizer.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeeklyStatisticsTest {
    private val zone = ZoneId.of("America/New_York")
    private val today = LocalDate.parse("2026-03-09")

    @Test
    fun focusUsesSevenCivilDaysAndAttributesTheEntireSessionToItsEnd() {
        val tasks = listOf(task("a", "Basis Data"), task("b", "Basis Data"), task("c", "Aljabar"))
        val sessions = listOf(
            session("a", "2026-03-03T05:00:00Z", 1).copy(
                startedAt = at("2026-03-03T04:59:00Z"),
                plannedEndAt = at("2026-03-02T23:00:00Z"),
            ),
            session("b", "2026-03-09T03:59:59Z", 8).copy(endedAt = null),
            session("c", "2026-03-09T04:00:00Z", 3),
            session("c", "2026-03-03T04:59:59Z", 5),
            session("c", "2026-03-10T04:00:00Z", 6),
        )

        val week = report(tasks, sessions)
        assertEquals((6L downTo 0).map(today::minusDays), week.days.map(DailyFocus::date))
        assertEquals(listOf(1, 0, 0, 0, 0, 8, 3).map { it * 60_000L }, week.days.map(DailyFocus::workMs))
        assertEquals(720_000L, week.totalFocusMs)
        assertEquals(listOf(CourseFocus("Basis Data", 540_000), CourseFocus("Aljabar", 180_000)), week.courses)

        val priorWeek = report(tasks, sessions, today.minusWeeks(1))
        assertEquals(300_000L, priorWeek.totalFocusMs)
        assertEquals(listOf(CourseFocus("Aljabar", 300_000)), priorWeek.courses)
    }

    @Test
    fun ignoresNonFinalUnstartedMissingZeroAndNegativeSessions() {
        val logged = session("a", "2026-03-09T12:00:00Z", 5)
        val week = report(
            listOf(task("a", completed = "2026-03-09T13:00:00Z")),
            listOf(
                logged.copy(done = false),
                logged.copy(startedAt = null),
                logged.copy(actualWorkMs = null),
                logged.copy(actualWorkMs = 0),
                logged.copy(actualWorkMs = -1),
                logged.copy(taskId = "removed"),
            ),
        )

        assertEquals(0L, week.totalFocusMs)
        assertEquals(1, week.completedTaskCount)
        assertEquals(emptyList<CourseFocus>(), week.courses)
        assertNull(week.accuracy)
        assertNull(week.paceFactor)
        assertEquals(1, week.onTimeStreakDays)
    }

    @Test
    fun accuracyUsesEachCompletedTaskTotalIncludingSessionsBeforeTheWindow() {
        val tasks = listOf(
            task("a", completed = "2026-03-07T14:00:00Z", estimate = 10, analyzerEstimate = 30),
            task("b", completed = "2026-03-08T14:00:00Z", estimate = 20),
            task("unfinished"),
            task("older", completed = "2026-03-02T14:00:00Z"),
            task("no-log", completed = "2026-03-09T14:00:00Z"),
        )
        val sessions = listOf(
            session("a", "2026-02-28T12:00:00Z", 4),
            session("a", "2026-03-06T12:00:00Z", 1),
            session("b", "2026-03-08T12:00:00Z", 25),
            session("unfinished", "2026-03-09T12:00:00Z", 10),
            session("older", "2026-03-02T12:00:00Z", 60),
        )

        val week = report(tasks, sessions)
        assertEquals(3, week.completedTaskCount)
        assertEquals(EstimateAccuracy(2, 30, 1_800_000L, 63), week.accuracy)
        assertEquals(2_160_000L, week.totalFocusMs)
        assertNull(week.paceFactor)
    }

    @Test
    fun paceUsesTheMedianAndRequiresThreeTasksInTheSelectedWeek() {
        val date = "2026-03-08T12:00:00Z"
        val tasks = listOf(
            task("a", completed = date, estimate = 20),
            task("b", completed = date, estimate = 15, analyzerEstimate = 20),
            task("c", completed = date, estimate = 20),
            task("older", completed = "2026-03-02T12:00:00Z", estimate = 20),
        )
        val sessions = listOf(
            session("a", date, 10),
            session("b", date, 25),
            session("c", date, 40),
            session("older", "2026-03-02T11:00:00Z", 10),
        )

        assertEquals(1.25, requireNotNull(report(tasks, sessions).paceFactor), 0.0)
        assertNull(report(tasks.filterNot { it.id == "c" }, sessions).paceFactor)
        assertEquals(0, report(listOf(tasks[2].copy(initialEstimatedMinutes = 10)), sessions).accuracy?.percent)
    }

    @Test
    fun onTimeStreakAllowsTodayToBeInProgressAndBreaksAtMissedPastDates() {
        val tasks = listOf(
            task("day7", completed = "2026-03-08T04:30:00Z"),
            task("day8", completed = "2026-03-08T14:00:00Z", deadline = "2026-03-08T14:00:00Z"),
            task("duplicate", completed = "2026-03-08T15:00:00Z"),
            task("late", completed = "2026-03-08T16:00:00Z", deadline = "2026-03-08T15:00:00Z"),
            task("day5", completed = "2026-03-05T12:00:00Z"),
        )

        assertEquals(2, report(tasks).onTimeStreakDays)
        assertEquals(0, report(tasks, weekEnd = today.minusDays(3)).onTimeStreakDays)
        val finishedToday = task("day9", completed = "2026-03-09T04:00:00Z")
        assertEquals(3, report(tasks + finishedToday).onTimeStreakDays)
    }

    @Test
    fun streakContinuesAcrossTheSelectedWeekBoundary() {
        val tasks = (0L..8).map { offset ->
            task("day$offset", completed = today.minusDays(offset).atTime(12, 0).atZone(zone).toInstant().toString())
        }
        assertEquals(9, report(tasks).onTimeStreakDays)
        assertEquals(2, report(tasks, weekEnd = today.minusWeeks(1)).onTimeStreakDays)
    }

    private fun report(
        tasks: List<StatisticsTask>,
        sessions: List<StatisticsSession> = emptyList(),
        weekEnd: LocalDate = today,
    ) = weeklyStatistics(tasks, sessions, today, weekEnd, zone)

    private fun task(
        id: String,
        course: String = "Basis Data",
        completed: String? = null,
        deadline: String = "2026-03-09T23:00:00Z",
        estimate: Int = 20,
        analyzerEstimate: Int? = null,
    ) = StatisticsTask(id, course, completed?.let(::at), at(deadline), estimate, analyzerEstimate)

    private fun session(taskId: String, end: String, minutes: Int): StatisticsSession {
        val endAt = at(end)
        val actual = minutes * 60_000L
        return StatisticsSession(taskId, true, endAt - actual, endAt, endAt, actual)
    }

    private fun at(instant: String): Long = Instant.parse(instant).toEpochMilli()
}
