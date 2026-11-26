package com.tazer.taskorganizer.widget

import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.Chronometer
import android.widget.FrameLayout
import android.widget.TextView
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tazer.taskorganizer.R
import com.tazer.taskorganizer.data.TaskRepository
import com.tazer.taskorganizer.data.TazerDatabase
import com.tazer.taskorganizer.data.WorkSession
import com.tazer.taskorganizer.data.WorkSessionState
import com.tazer.taskorganizer.domain.FocusMoment
import com.tazer.taskorganizer.timer.FocusLinkKind
import com.tazer.taskorganizer.timer.FocusRuntime
import com.tazer.taskorganizer.ui.TazerThemeMode
import java.time.Instant
import java.util.concurrent.FutureTask
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FocusWidgetTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val now = FocusMoment(wall("2026-11-26T02:00:00Z"), SystemClock.elapsedRealtime(), "widget-boot")
    private lateinit var database: TazerDatabase
    private lateinit var repository: TaskRepository

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(context, TazerDatabase::class.java).build()
        repository = TaskRepository(database)
    }

    @After
    fun close() = database.close()

    @Test
    fun pendingUsesTodayInProfileZoneAndNeverBorrowsAnotherOwnersSession() = runBlocking {
        addSession("other", "Other account", "2026-11-26T01:00:00Z")
        addSession("selected", "Expired", "2026-11-26T01:00:00Z")
        val next = addSession("selected", "Today's next", "2026-11-26T03:00:00Z")
        addSession("selected", "Tomorrow", "2026-11-26T18:00:00Z")

        val pending = loadWidgetSnapshot(database, "selected", now)
        assertEquals("Today's next", pending.content.title)
        assertEquals("10:00", pending.content.startTime)
        assertEquals(next.id, pending.content.link?.id)
        assertEquals(FocusLinkKind.REMINDER, pending.content.link?.kind)
        assertEquals(next.plannedEndAt, pending.refreshAtWallMs)
        assertNull(loadWidgetSnapshot(database, "unknown", now).content.link)

        val after = loadWidgetSnapshot(database, "selected", now.copy(wallMs = next.plannedEndAt))
        assertEquals(R.string.focus_widget_no_session, after.content.status)
        assertNull(after.content.link)
        assertEquals(wall("2026-11-26T17:00:00Z"), after.refreshAtWallMs)
        val views = applyViews(after.content, TazerThemeMode.DARK)
        assertEquals(context.getString(R.string.focus_widget_open_schedule), views.text(R.id.focus_widget_title))
        assertEquals(View.GONE, views.findViewById<View>(R.id.focus_widget_timer).visibility)
    }

    @Test
    fun activeClockUsesMonotonicTimeAndPausedValueStaysFrozenAcrossBoots() = runBlocking {
        val active = addSession("selected", "Active focus", "2026-11-26T01:00:00Z") {
            it.copy(state = WorkSessionState.RUNNING, timerBootId = now.bootId,
                activeSinceElapsedMs = now.elapsedMs, cutoffElapsedMs = now.elapsedMs + 90_000L)
        }
        addSession("selected", "Upcoming focus", "2026-11-26T03:00:00Z")
        val running = loadWidgetSnapshot(database, "selected", now.copy(wallMs = now.wallMs + 2_000_000L))
        assertEquals(R.string.focus_widget_running, running.content.status)
        assertTrue(running.content.countdown)
        assertEquals(active.cutoffElapsedMs, running.content.chronometerBase)
        assertEquals(active.cutoffElapsedMs, running.refreshAtElapsedMs)
        val runningView = applyViews(running.content, TazerThemeMode.LIGHT).findViewById<Chronometer>(R.id.focus_widget_timer)
        assertEquals(View.VISIBLE, runningView.visibility)
        assertEquals(requireNotNull(active.cutoffElapsedMs), runningView.base)
        assertTrue(runningView.isCountDown)

        val overtime = loadWidgetSnapshot(database, "selected", now.copy(elapsedMs = now.elapsedMs + 91_000L))
        assertEquals(R.string.focus_widget_overtime, overtime.content.status)
        assertFalse(overtime.content.countdown)
        val overtimeView = applyViews(overtime.content, TazerThemeMode.SYSTEM).findViewById<Chronometer>(R.id.focus_widget_timer)
        assertEquals(requireNotNull(active.cutoffElapsedMs), overtimeView.base)
        assertFalse(overtimeView.isCountDown)
        assertEquals(context.getString(R.string.focus_widget_overtime_format, "%s"), overtimeView.format)
        assertNull(loadWidgetSnapshot(database, "selected", now.copy(bootId = "reboot")).content.link)

        val atCutoff = now.copy(elapsedMs = now.elapsedMs + 90_000L)
        val overtimeSession = requireNotNull(repository.checkFocus("selected", active.id, active.revision, atCutoff)?.active)
        assertEquals(WorkSessionState.OVERTIME, overtimeSession.state)
        val pauseAt = atCutoff.copy(elapsedMs = atCutoff.elapsedMs + 5_000L)
        val paused = repository.pauseFocus("selected", overtimeSession.id, overtimeSession.revision, pauseAt)?.active
        assertEquals(WorkSessionState.PAUSED, paused?.state)
        val frozen = loadWidgetSnapshot(database, "selected", now.copy(bootId = "reboot"))
        assertEquals(-5_000L, frozen.content.pausedRemainingMs)
        assertEquals(R.string.focus_widget_paused, frozen.content.status)
        assertEquals(FocusLinkKind.ACTIVE, frozen.content.link?.kind)
        val pausedView = applyViews(frozen.content, TazerThemeMode.DARK)
        assertTrue(pausedView.text(R.id.focus_widget_time).startsWith("+"))
        assertEquals(View.GONE, pausedView.findViewById<View>(R.id.focus_widget_timer).visibility)

        val focus = FocusRuntime.get(context)
        val link = requireNotNull(frozen.content.link)
        val intent = focus.widgetFocusIntent(link)
        assertNull(focus.readLink(Intent(intent.action, intent.data).setComponent(intent.component)))
        assertEquals(link, focus.readLink(intent))
        assertNull(focus.readLink(intent))
    }

    private suspend fun addSession(
        uid: String,
        title: String,
        start: String,
        change: (WorkSession) -> WorkSession = { it },
    ): WorkSession {
        val dao = database.tazerDao()
        if (dao.findProfile(uid) == null) repository.saveProfile(uid, "Asia/Jakarta", 480, 1320)
        val task = repository.createTask(uid, title, "Course", "", now.wallMs + 86_400_000L, 3, 25, now.wallMs)
        val startAt = wall(start)
        val session = change(WorkSession(
            id = "$uid-$start", taskId = task.id, ownerUid = uid, state = WorkSessionState.PENDING,
            plannedStartAt = startAt, plannedEndAt = startAt + 25 * 60_000L, plannedMinutes = 25,
            startedAt = null, endedAt = null, actualWorkMs = null, timerBootId = null,
            activeSinceElapsedMs = null, cutoffElapsedMs = null, lastObservedWorkMs = null, revision = 0L,
        ))
        dao.insertSession(session)
        return session
    }

    private fun applyViews(content: WidgetContent, theme: TazerThemeMode): View {
        val result = FutureTask { widgetViews(context, content, theme).apply(context, FrameLayout(context)) }
        instrumentation.runOnMainSync(result)
        return result.get()
    }

    private fun View.text(id: Int) = findViewById<TextView>(id).text.toString()

    private fun wall(iso: String) = Instant.parse(iso).toEpochMilli()
}
