package com.tazer.taskorganizer.timer

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.SystemClock
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tazer.taskorganizer.MainActivity
import com.tazer.taskorganizer.data.FixedScheduleKind
import com.tazer.taskorganizer.data.TaskRepository
import com.tazer.taskorganizer.data.TaskStatus
import com.tazer.taskorganizer.data.TazerDatabase
import com.tazer.taskorganizer.data.WorkSessionState
import com.tazer.taskorganizer.domain.FocusMoment
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FocusStartTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: TazerDatabase
    private lateinit var repository: TaskRepository

    @Before
    fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(context, TazerDatabase::class.java).build()
        repository = TaskRepository(database)
        repository.saveProfile(UID, "Asia/Jakarta", 480, 720)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun reminderPayloadStartsFullTimerAndReplansAfterActualEndAndBreak() = runBlocking {
        val task = repository.createTask(UID, "Tugas", "Kuliah", "", wall("11:30"), 3, 50, wall("08:00"))
        val pending = requireNotNull(repository.addPlannedSession(UID, task.id, wall("09:00"), 25))
        val next = requireNotNull(repository.addPlannedSession(UID, task.id, wall("09:35"), 25))
        val event = FocusEvent(pending, FocusEventKind.REMINDER)
        val preferences = context.getSharedPreferences(PREFERENCES, 0)
        val notifications = FocusNotifications(context, preferences) { LINK_TOKEN }
        val now = FocusMoment(wall("09:01") + 15_000L, minutes(60), "boot")
        val contentIntent = requireNotNull(notifications.reminderNotification(pending, now.wallMs).contentIntent)
        try {
            val payload = event.activityIntent(context, LINK_TOKEN)
            val expected = PendingIntent.getActivity(
                context, 0, payload, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
            assertEquals(ComponentName(context, MainActivity::class.java), payload.component)
            assertEquals(expected, contentIntent)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) assertTrue(contentIntent.isImmutable)
            val link = requireNotNull(FocusEvent.readLink(context, payload, LINK_TOKEN))
            assertEquals(FocusLink(UID, pending.id, pending.revision, FocusLinkKind.REMINDER), link)
            assertNull(FocusEvent.readLink(context, payload, LINK_TOKEN))
            assertNull(repository.activeFocus(UID))

            val result = requireNotNull(repository.startFocus(link.uid, link.id, link.revision, now))
            val running = requireNotNull(result.active)
            val cutoff = now.elapsedMs + minutes(25)
            assertEquals(WorkSessionState.RUNNING, running.state)
            assertEquals(now.wallMs, running.startedAt)
            assertEquals(pending.plannedEndAt, running.plannedEndAt)
            assertEquals(cutoff, running.cutoffElapsedMs)
            assertEquals(running, repository.activeFocus(UID))
            assertNull(repository.startFocus(link.uid, link.id, link.revision, now))
            assertEquals(WorkSessionState.CANCELLED, repository.focusSession(UID, next.id)?.state)
            val replacement = repository.pendingFocus(UID).single()
            assertEquals(25, replacement.plannedMinutes)
            assertEquals(wall("09:37"), replacement.plannedStartAt)
            assertEquals(replacement.plannedStartAt, requireNotNull(result.plan).sessions.single().startAt)

            val before = FocusMoment(wall("10:59"), cutoff - 1L, now.bootId)
            assertEquals(WorkSessionState.RUNNING, repository.checkFocus(UID, running.id, running.revision, before)?.active?.state)
            val atCutoff = before.copy(elapsedMs = cutoff)
            val overtime = repository.checkFocus(UID, running.id, running.revision, atCutoff)?.active
            assertEquals(WorkSessionState.OVERTIME, overtime?.state)
            assertEquals(minutes(25), overtime?.lastObservedWorkMs)
        } finally {
            contentIntent.cancel()
            context.deleteSharedPreferences(PREFERENCES)
        }
    }

    @Test
    fun startThatWouldCrossFixedBlockReplansWithoutStartingOrReducingEstimate() = runBlocking {
        val task = repository.createTask(UID, "Tugas", "Kuliah", "", wall("11:25"), 3, 25, wall("08:00"))
        repository.addFixedSchedule(UID, "Kuliah", FixedScheduleKind.ROUTINE, 1, 600, 660)
        val pending = requireNotNull(repository.addPlannedSession(UID, task.id, wall("09:35"), 25))
        val now = FocusMoment(pending.plannedStartAt + 1L, minutes(60), "boot")

        val result = requireNotNull(repository.startFocus(UID, pending.id, pending.revision, now))

        assertNull(result.active)
        assertNull(repository.activeFocus(UID))
        assertNull(repository.startFocus(UID, pending.id, pending.revision, now))
        assertEquals(WorkSessionState.CANCELLED, repository.focusSession(UID, pending.id)?.state)
        assertEquals(TaskStatus.PENDING, repository.observeTasks(UID).first().single().status)
        assertEquals(25, repository.observeTasks(UID).first().single().remainingMinutes)
        val replacement = repository.pendingFocus(UID).single()
        assertEquals(wall("11:00"), replacement.plannedStartAt)
        assertEquals(25, replacement.plannedMinutes)
        assertEquals(replacement.plannedStartAt, requireNotNull(result.plan).sessions.single().startAt)
    }

    @Test
    fun tappedReminderOpensTimerAndStartKeepsFullTwentyFiveMinutes() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.uiAutomation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            automation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        val runtime = FocusRuntime.get(context)
        val room = TaskRepository(TazerDatabase.getInstance(context))
        val now = runtime.now()
        val zone = ZoneId.getAvailableZoneIds().asSequence().filter { it.startsWith("Etc/GMT") }
            .first { Instant.ofEpochMilli(now.wallMs).atZone(ZoneId.of(it)).hour in 10..16 }
        val uid = "tazer-demo-local"
        room.saveProfile(uid, zone, 0, 1440)
        val activity = instrumentation.startActivitySync(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        var taskId: String? = null
        fun find(root: AccessibilityNodeInfo?, text: String): AccessibilityNodeInfo? {
            if (root == null || !root.isVisibleToUser) return null
            if (root.text?.toString() == text || root.contentDescription?.toString() == text) return root
            for (index in 0 until root.childCount) find(root.getChild(index), text)?.let { return it }
            return null
        }
        fun node(text: String) = find(automation.rootInActiveWindow, text)
        fun click(text: String): Boolean {
            var target = node(text)
            while (target != null && !target.isClickable) target = target.parent
            if (target?.isEnabled != true) return false
            val bounds = Rect().also { target.getBoundsInScreen(it) }
            val now = SystemClock.uptimeMillis()
            for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
                val event = MotionEvent.obtain(now, SystemClock.uptimeMillis(), action,
                    bounds.exactCenterX(), bounds.exactCenterY(), 0)
                try { automation.injectInputEvent(event, true) } finally { event.recycle() }
            }
            return true
        }
        suspend fun waitFor(description: String, check: () -> Boolean) {
            val until = SystemClock.elapsedRealtime() + 12_000L
            while (SystemClock.elapsedRealtime() < until) {
                if (check()) return
                delay(100)
            }
            throw AssertionError(description)
        }
        try {
            waitFor("Demo login could not be tapped") { click("Masuk sebagai demo") }
            waitFor("Workspace did not load") { node("Belum Selesai") != null }
            val moment = runtime.now()
            val task = room.createTask(uid, "Verifikasi timer notifikasi", "QA", "", moment.wallMs + minutes(90),
                3, 25, moment.wallMs)
            taskId = task.id
            val pending = requireNotNull(room.addPlannedSession(uid, task.id, moment.wallMs - 11_000L, 25))
            runtime.sync(uid)
            val manager = context.getSystemService(NotificationManager::class.java)
            fun reminder() = manager.activeNotifications.firstOrNull { it.id == FocusNotifications.REMINDER_ID }
            waitFor("Scheduled notification was not posted") { reminder() != null }
            requireNotNull(reminder()?.notification?.contentIntent).send()
            waitFor("Reminder did not open the focus screen") { node("Timer fokus") != null && node("25:00") != null }
            waitFor("Start button from notification was not tappable") { click("Mulai sesi") }
            waitFor("Focus timer did not start") { node("Sedang fokus") != null }
            val running = requireNotNull(room.activeFocus(uid))
            assertEquals(pending.id, running.id)
            assertTrue(requireNotNull(running.startedAt) - pending.plannedStartAt >= 11_000L)
            assertEquals(minutes(25), requireNotNull(running.cutoffElapsedMs) - requireNotNull(running.activeSinceElapsedMs))
        } finally {
            room.activeFocus(uid)?.takeIf { it.taskId == taskId }?.let { runtime.done(uid, it.id, it.revision) }
            taskId?.let { room.deleteTask(uid, it) }
            runtime.deactivate(uid)
            instrumentation.runOnMainSync { activity.finish() }
        }
    }

    private fun minutes(value: Int) = value * 60_000L

    private fun wall(time: String) = Instant.parse("2026-11-23T$time:00+07:00").toEpochMilli()

    private companion object {
        const val UID = "focus-start-test"
        const val LINK_TOKEN = "fixture-link-token"
        const val PREFERENCES = "focus_start_test"
    }
}
