package com.tazer.taskorganizer.timer

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tazer.taskorganizer.data.WorkSession
import com.tazer.taskorganizer.data.WorkSessionState
import com.tazer.taskorganizer.domain.FocusMoment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FocusNotificationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun privateLinkChecksTokenAndIsReadOnce() {
        val event = FocusEvent("test/uid", "focus id", 4L, FocusEventKind.EXTEND)
        val intent = event.activityIntent(context, LINK_TOKEN)
        assertNull(FocusEvent.readLink(context, intent, "spoofed"))
        assertNull(FocusEvent.readLink(context, Intent(intent).setComponent(null), LINK_TOKEN))
        assertNull(FocusEvent.readLink(context, event.copy(revision = -1L).activityIntent(context, LINK_TOKEN), LINK_TOKEN))
        assertEquals(FocusLink("test/uid", "focus id", 4L, FocusLinkKind.EXTEND), FocusEvent.readLink(context, intent, LINK_TOKEN))
        assertNull(FocusEvent.readLink(context, intent, LINK_TOKEN))
    }

    @Test
    fun pendingIntentIdentityIncludesOwnerSessionRevisionAndEvent() {
        val event = FocusEvent("test-user", "notification-fixture", 4L, FocusEventKind.CUTOFF)
        val variants = listOf(
            event.copy(uid = "another-user"),
            event.copy(id = "another-session"),
            event.copy(revision = 5L),
            event.copy(kind = FocusEventKind.START_ALARM),
            event.copy(kind = FocusEventKind.DONE),
        )
        val operations = (listOf(event) + variants).map {
            val intent = it.receiverIntent(context)
            assertEquals(it, FocusEvent.fromReceiver(intent))
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }
        try {
            assertEquals(operations.size, operations.toSet().size)
            val same = PendingIntent.getBroadcast(context, 0, event.receiverIntent(context), PendingIntent.FLAG_IMMUTABLE)
            assertEquals(operations.first(), same)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) assertTrue(operations.all { it.isImmutable })
        } finally {
            operations.forEach { it.cancel() }
        }
    }

    @Test
    fun chronometerUsesMonotonicCutoffAndOvertimeHasDistinctActions() {
        val prefsName = "focus_notification_test"
        val preferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val notifications = FocusNotifications(context, preferences) { LINK_TOKEN }
        val running = WorkSession(
            id = "test-notification", taskId = "test-task", ownerUid = "test-owner",
            state = WorkSessionState.RUNNING, plannedStartAt = 1_000_000L, plannedEndAt = 2_500_000L,
            plannedMinutes = 25, startedAt = 1_000_000L, endedAt = null, actualWorkMs = null,
            timerBootId = "test-boot", activeSinceElapsedMs = 60_000L, cutoffElapsedMs = 1_560_000L,
            lastObservedWorkMs = 0L, revision = 1L,
        )
        val now = FocusMoment(1_300_000L, 160_000L, "test-boot")
        val countdown = notifications.activeNotification(running, now, alert = false)
        val overtimeSession = running.copy(state = WorkSessionState.OVERTIME, revision = 2L)
        val late = FocusMoment(3_600_000L, 1_660_000L, "test-boot")
        val overtime = notifications.activeNotification(overtimeSession, late, alert = true)
        try {
            assertEquals(2_700_000L, countdown.`when`)
            assertTrue(countdown.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
            assertTrue(countdown.extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN))
            assertTrue(countdown.flags and Notification.FLAG_ONGOING_EVENT != 0)
            assertEquals(3_500_000L, overtime.`when`)
            assertTrue(overtime.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
            assertFalse(overtime.extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN))
            assertEquals(listOf("Sudah Selesai", "Belum Selesai"), overtime.actions.map { it.title.toString() })
            val done = FocusEvent(overtimeSession, FocusEventKind.DONE)
            val extend = FocusEvent(overtimeSession, FocusEventKind.EXTEND)
            val expectedDone = PendingIntent.getBroadcast(
                context, 0, done.receiverIntent(context), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
            val expectedExtend = PendingIntent.getActivity(
                context, 0, extend.activityIntent(context, LINK_TOKEN), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
            assertEquals(expectedDone, overtime.actions[0].actionIntent)
            assertEquals(expectedExtend, overtime.actions[1].actionIntent)
            assertNotEquals(countdown.contentIntent, overtime.contentIntent)
        } finally {
            countdown.contentIntent.cancel()
            overtime.contentIntent.cancel()
            overtime.actions.forEach { it.actionIntent.cancel() }
            context.deleteSharedPreferences(prefsName)
        }
    }

    private companion object {
        const val LINK_TOKEN = "fixture-link-token"
    }
}
