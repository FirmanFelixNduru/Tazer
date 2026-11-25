package com.tazer.taskorganizer.timer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.tazer.taskorganizer.data.WorkSession
import com.tazer.taskorganizer.data.WorkSessionState
import com.tazer.taskorganizer.domain.FocusMoment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class FocusNotificationSoundTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val manager = context.getSystemService(NotificationManager::class.java)

    @Test
    fun upgradesChannelsAndDecodesTheRegisteredBell() {
        val legacyIds = listOf("focus_reminders", "focus_overtime")
        val prefsName = "focus_notification_sound_test"
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        try {
            legacyIds.forEach { id ->
                manager.createNotificationChannel(NotificationChannel(id, id, NotificationManager.IMPORTANCE_DEFAULT))
                assertNotNull(manager.getNotificationChannel(id))
            }

            val notifications = FocusNotifications(context, prefs) { "test-link-token" }
            legacyIds.forEach { assertNull(manager.getNotificationChannel(it)) }

            val session = WorkSession(
                id = "sound-test", taskId = "test-task", ownerUid = "test-owner",
                state = WorkSessionState.OVERTIME, plannedStartAt = 0L, plannedEndAt = 60_000L,
                plannedMinutes = 1, startedAt = 0L, endedAt = null, actualWorkMs = null,
                timerBootId = "test-boot", activeSinceElapsedMs = 0L, cutoffElapsedMs = 60_000L,
                lastObservedWorkMs = 60_000L, revision = 1L,
            )
            val reminder = notifications.reminderNotification(session, now = 0L)
            val now = FocusMoment(60_000L, 60_000L, "test-boot")
            val overtime = notifications.activeNotification(session, now, alert = true)
            try {
                assertEquals("focus_reminders_v2", reminder.channelId)
                assertEquals("focus_overtime_v2", overtime.channelId)
                assertEquals(NotificationCompat.GROUP_ALERT_ALL, overtime.groupAlertBehavior)
                val refreshed = notifications.activeNotification(session, now, alert = false)
                assertEquals(NotificationCompat.GROUP_ALERT_SUMMARY, refreshed.groupAlertBehavior)
                val foreground = notifications.foregroundPlaceholder()
                assertEquals("focus_timer", foreground.channelId)
                assertEquals(NotificationCompat.GROUP_ALERT_SUMMARY, foreground.groupAlertBehavior)

                val expected = Uri.parse("android.resource://${context.packageName}/raw/tazer_ringbell")
                val bell = requireNotNull(manager.getNotificationChannel(reminder.channelId).sound)
                assertEquals(expected, bell)
                val overtimeChannel = manager.getNotificationChannel(overtime.channelId)
                assertEquals(bell, overtimeChannel.sound)
                assertEquals(AudioAttributes.USAGE_NOTIFICATION, overtimeChannel.audioAttributes.usage)

                val player = MediaPlayer()
                try {
                    player.setDataSource(context, bell)
                    player.prepare()
                    assertEquals(3_000, player.duration)
                } finally {
                    player.release()
                }
            } finally {
                reminder.contentIntent.cancel()
                overtime.contentIntent.cancel()
                overtime.actions.forEach { it.actionIntent.cancel() }
            }
        } finally {
            legacyIds.forEach { manager.deleteNotificationChannel(it) }
            context.deleteSharedPreferences(prefsName)
        }
    }
}
