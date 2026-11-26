package com.tazer.taskorganizer.widget

import android.content.Context
import android.os.Build
import android.text.format.DateUtils
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.room.withTransaction
import com.tazer.taskorganizer.R
import com.tazer.taskorganizer.data.TaskStatus
import com.tazer.taskorganizer.data.TazerDatabase
import com.tazer.taskorganizer.data.WorkSession
import com.tazer.taskorganizer.data.WorkSessionState
import com.tazer.taskorganizer.domain.FocusClock
import com.tazer.taskorganizer.domain.FocusMoment
import com.tazer.taskorganizer.timer.FocusLink
import com.tazer.taskorganizer.timer.FocusLinkKind
import com.tazer.taskorganizer.ui.TazerThemeMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal data class WidgetContent(
    val status: Int = R.string.focus_widget_schedule_hint,
    val title: String? = null,
    val link: FocusLink? = null,
    val startTime: String? = null,
    val pausedRemainingMs: Long? = null,
    val chronometerBase: Long? = null,
    val countdown: Boolean = false,
)

internal data class WidgetSnapshot(
    val content: WidgetContent = WidgetContent(),
    val refreshAtWallMs: Long? = null,
    val refreshAtElapsedMs: Long? = null,
)

internal suspend fun loadWidgetSnapshot(
    database: TazerDatabase,
    uid: String,
    now: FocusMoment,
): WidgetSnapshot = database.withTransaction {
    val dao = database.tazerDao()
    val profile = dao.findProfile(uid) ?: return@withTransaction WidgetSnapshot()
    val empty = WidgetContent(status = R.string.focus_widget_no_session)
    val active = dao.findActiveFocus(uid)
    if (active != null) {
        val task = dao.findTask(uid, active.taskId)?.takeUnless { it.status == TaskStatus.DONE }
        return@withTransaction activeWidget(active, task?.title, now)
    }

    val zone = ZoneId.of(profile.zoneId)
    val today = Instant.ofEpochMilli(now.wallMs).atZone(zone).toLocalDate()
    val dayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
    val tomorrow = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val next = dao.findPendingSessions(uid).firstOrNull {
        it.plannedStartAt >= dayStart && it.plannedEndAt > now.wallMs
    } ?: return@withTransaction WidgetSnapshot(empty)
    if (next.plannedStartAt >= tomorrow) {
        val nextDay = Instant.ofEpochMilli(next.plannedStartAt).atZone(zone).toLocalDate()
        return@withTransaction WidgetSnapshot(empty, nextDay.atStartOfDay(zone).toInstant().toEpochMilli())
    }
    val task = dao.findTask(uid, next.taskId)?.takeUnless { it.status == TaskStatus.DONE }
        ?: return@withTransaction WidgetSnapshot(empty)
    val start = Instant.ofEpochMilli(next.plannedStartAt).atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm"))
    WidgetSnapshot(
        WidgetContent(R.string.focus_widget_next, task.title, next.link(FocusLinkKind.REMINDER), startTime = start),
        refreshAtWallMs = minOf(next.plannedEndAt, tomorrow),
    )
}

private fun activeWidget(session: WorkSession, title: String?, now: FocusMoment): WidgetSnapshot {
    if (title == null) return WidgetSnapshot()
    val link = session.link(FocusLinkKind.ACTIVE)
    if (session.state == WorkSessionState.PAUSED) {
        val remaining = session.pausedRemainingMs ?: return WidgetSnapshot()
        return WidgetSnapshot(WidgetContent(R.string.focus_widget_paused, title, link, pausedRemainingMs = remaining))
    }
    val clock = FocusClock(session.timerBootId, session.activeSinceElapsedMs, session.cutoffElapsedMs, session.lastObservedWorkMs)
    val remaining = clock.remainingAt(now) ?: return WidgetSnapshot()
    val cutoff = requireNotNull(session.cutoffElapsedMs)
    val countdown = session.state != WorkSessionState.OVERTIME && remaining > 0L
    return WidgetSnapshot(
        WidgetContent(
            if (countdown) R.string.focus_widget_running else R.string.focus_widget_overtime,
            title, link, chronometerBase = cutoff, countdown = countdown,
        ),
        refreshAtElapsedMs = cutoff.takeIf { countdown },
    )
}

private fun WorkSession.link(kind: FocusLinkKind) = FocusLink(ownerUid, id, revision, kind)

internal fun widgetViews(context: Context, content: WidgetContent, theme: TazerThemeMode): RemoteViews =
    RemoteViews(context.packageName, R.layout.focus_widget).apply {
        setTextViewText(R.id.focus_widget_title, content.title ?: context.getString(R.string.focus_widget_open_schedule))
        setTextViewText(R.id.focus_widget_status, context.getString(content.status))
        val overtimeFormat = context.getString(R.string.focus_widget_overtime_format)
        val time = content.pausedRemainingMs?.let { remaining ->
            val seconds = if (remaining > 0L) (remaining + 999L) / 1000L else -(remaining / 1000L)
            val elapsed = DateUtils.formatElapsedTime(seconds)
            if (remaining < 0L) overtimeFormat.format(elapsed) else elapsed
        } ?: content.startTime
        setTextViewText(R.id.focus_widget_time, time)
        setViewVisibility(R.id.focus_widget_time, if (time == null) View.GONE else View.VISIBLE)
        val base = content.chronometerBase
        setViewVisibility(R.id.focus_widget_timer, if (base == null) View.GONE else View.VISIBLE)
        setChronometerCountDown(R.id.focus_widget_timer, content.countdown)
        setChronometer(R.id.focus_widget_timer, base ?: 0L, overtimeFormat.takeIf { !content.countdown }, base != null)

        val (background, primary, secondary) = when (theme) {
            TazerThemeMode.LIGHT -> Triple(R.drawable.focus_widget_background_light, R.color.focus_widget_primary_light, R.color.focus_widget_secondary_light)
            TazerThemeMode.DARK -> Triple(R.drawable.focus_widget_background_dark, R.color.focus_widget_primary_dark, R.color.focus_widget_secondary_dark)
            TazerThemeMode.SYSTEM -> Triple(R.drawable.focus_widget_background, R.color.focus_widget_primary, R.color.focus_widget_secondary)
        }
        setInt(android.R.id.background, "setBackgroundResource", background)
        // Resource-backed colors resolve against the launcher's current configuration on Android 12+.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setColor(R.id.focus_widget_title, "setTextColor", primary)
            setColor(R.id.focus_widget_time, "setTextColor", primary)
            setColor(R.id.focus_widget_timer, "setTextColor", primary)
            setColor(R.id.focus_widget_status, "setTextColor", secondary)
        } else {
            setTextColor(R.id.focus_widget_title, ContextCompat.getColor(context, primary))
            setTextColor(R.id.focus_widget_time, ContextCompat.getColor(context, primary))
            setTextColor(R.id.focus_widget_timer, ContextCompat.getColor(context, primary))
            setTextColor(R.id.focus_widget_status, ContextCompat.getColor(context, secondary))
        }
    }
