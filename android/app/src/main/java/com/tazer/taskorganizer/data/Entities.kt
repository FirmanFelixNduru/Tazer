package com.tazer.taskorganizer.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TaskStatus { PENDING, IN_PROGRESS, DONE }
enum class AnalysisSource { OFFLINE, AI }
enum class FixedScheduleKind { ROUTINE, BREAK }
enum class WorkSessionState { PENDING, RUNNING, OVERTIME, EXTENDED, PAUSED, DONE, CANCELLED }

@Entity
data class UserProfile(
    @PrimaryKey val ownerUid: String,
    val zoneId: String,
    val activeStartMinute: Int,
    val activeEndMinute: Int,
    val busyUntilAt: Long?,
)

@Entity(
    foreignKeys = [ForeignKey(
        entity = UserProfile::class,
        parentColumns = ["ownerUid"],
        childColumns = ["ownerUid"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [
        Index(value = ["ownerUid", "status", "completedAt"]),
        Index(value = ["id", "ownerUid"], unique = true),
    ],
)
data class Task(
    @PrimaryKey val id: String,
    val ownerUid: String,
    val title: String,
    val course: String,
    val description: String,
    val deadlineAt: Long,
    val difficultyScore: Int,
    val initialEstimatedMinutes: Int,
    val remainingMinutes: Int,
    val analysisSource: AnalysisSource,
    val status: TaskStatus,
    val createdAt: Long,
    val completedAt: Long?,
)

@Entity(
    foreignKeys = [ForeignKey(
        entity = UserProfile::class,
        parentColumns = ["ownerUid"],
        childColumns = ["ownerUid"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = ["ownerUid", "dayOfWeek"])],
)
data class FixedSchedule(
    @PrimaryKey val id: String,
    val ownerUid: String,
    val label: String,
    val kind: FixedScheduleKind,
    val dayOfWeek: Int,
    val startMinute: Int,
    val endMinute: Int,
)

@Entity(
    foreignKeys = [ForeignKey(
        entity = Task::class,
        parentColumns = ["id", "ownerUid"],
        childColumns = ["taskId", "ownerUid"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [
        Index(value = ["ownerUid", "state", "plannedStartAt"]),
        Index(value = ["taskId", "ownerUid"]),
    ],
)
data class WorkSession(
    @PrimaryKey val id: String,
    val taskId: String,
    val ownerUid: String,
    val state: WorkSessionState,
    val plannedStartAt: Long,
    val plannedEndAt: Long,
    val plannedMinutes: Int,
    val startedAt: Long?,
    val endedAt: Long?,
    val actualWorkMs: Long?,
    val timerBootId: String?,
    val activeSinceElapsedMs: Long?,
    val cutoffElapsedMs: Long?,
    val lastObservedWorkMs: Long?,
    val revision: Long,
    val pausedRemainingMs: Long? = null,
    val pausedFromState: WorkSessionState? = null,
    val workBeforeSegmentMs: Long? = null,
    val continuousWorkBeforeSegmentMs: Long? = null,
    val lastObservedAt: Long? = null,
)

/** Start and end are inclusive civil dates. A null start minute represents an all-day event. */
@Entity(
    foreignKeys = [ForeignKey(
        entity = UserProfile::class,
        parentColumns = ["ownerUid"],
        childColumns = ["ownerUid"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = ["ownerUid", "startEpochDay"])],
)
data class CalendarEvent(
    @PrimaryKey val id: String,
    val ownerUid: String,
    val title: String,
    val startEpochDay: Long,
    val endEpochDay: Long,
    val startMinute: Int?,
    val description: String,
)
