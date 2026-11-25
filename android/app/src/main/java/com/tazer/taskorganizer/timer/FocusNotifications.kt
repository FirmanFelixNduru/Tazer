package com.tazer.taskorganizer.timer

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.tazer.taskorganizer.MainActivity
import com.tazer.taskorganizer.data.WorkSession
import com.tazer.taskorganizer.data.WorkSessionState
import com.tazer.taskorganizer.domain.FocusMoment

internal class FocusNotifications(
    private val context: Context,
    private val preferences: SharedPreferences,
    private val linkToken: () -> String,
) {
    private val manager = context.getSystemService(NotificationManager::class.java)
    private var displayedActive: FocusEvent? = null

    fun enabled(): Boolean =
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            manager.areNotificationsEnabled()

    fun remindersEnabled(): Boolean = canPost(REMINDERS)

    fun foregroundPlaceholder(): Notification =
        notification(TIMER, "Timer fokus", "Memuat sesi fokus")
            .setSilent(true)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

    fun showActive(session: WorkSession, now: FocusMoment, force: Boolean) {
        if (session.cutoffElapsedMs == null) return
        val event = FocusEvent(session, FocusEventKind.ACTIVE)
        if (!canPost(TIMER)) {
            displayedActive = null
            return
        }
        if (!force && displayedActive == event) return
        val overtime = session.state == WorkSessionState.OVERTIME
        val key = event.uri("event").toString()
        val alert = overtime && preferences.getString(OVERTIME_ALERT, null) != key
        if (post(ACTIVE_ID, activeNotification(session, now, alert))) {
            displayedActive = event
            if (alert) preferences.edit { putString(OVERTIME_ALERT, key) }
        }
    }

    fun activeNotification(session: WorkSession, now: FocusMoment, alert: Boolean): Notification {
        val cutoff = requireNotNull(session.cutoffElapsedMs)
        val overtime = session.state == WorkSessionState.OVERTIME
        // Android's notification chronometer converts wall-clock "when" to a monotonic base.
        val builder = if (overtime) {
            notification(TIMER, "Waktu fokus habis", "Timer tetap menghitung waktu tambahan")
        } else {
            notification(TIMER, "Fokus sedang berjalan", "Ketuk untuk membuka timer")
        }
        builder.setWhen(now.wallMs + (cutoff - now.elapsedMs))
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(!overtime)
            .setOngoing(true)
            .setSilent(!alert)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(open(FocusEvent(session, FocusEventKind.ACTIVE)))
        if (overtime) {
            val done = FocusEvent(session, FocusEventKind.DONE)
            builder.addAction(0, "Sudah Selesai", PendingIntent.getBroadcast(
                context, 0, done.receiverIntent(context), PendingIntent.FLAG_IMMUTABLE,
            ))
            builder.addAction(0, "Belum Selesai", open(FocusEvent(session, FocusEventKind.EXTEND)))
        }
        return builder.build()
    }

    fun showReminder(session: WorkSession, now: Long): Boolean {
        if (!remindersEnabled()) return false
        return post(REMINDER_ID, notification(REMINDERS, "Waktunya fokus", "Buka jadwal untuk mulai")
            .setTimeoutAfter(session.plannedEndAt - now)
            .setContentIntent(open(FocusEvent(session, FocusEventKind.REMINDER)))
            .setAutoCancel(true)
            .build())
    }

    fun showInterrupted() {
        if (!canPost(REMINDERS)) return
        val launch = Intent(context, MainActivity::class.java).setAction(Intent.ACTION_MAIN)
        val open = PendingIntent.getActivity(context, 0, launch, PendingIntent.FLAG_IMMUTABLE)
        post(RECOVERY_ID, notification(REMINDERS, "Sesi fokus terinterupsi",
            "Periksa durasi terakhir yang tersimpan sebelum perangkat dimulai ulang.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build())
    }

    fun cancelActive() {
        displayedActive = null
        manager.cancel(ACTIVE_ID)
    }

    fun cancelReminder() = manager.cancel(REMINDER_ID)

    fun cancelAll() {
        cancelActive()
        cancelReminder()
        manager.cancel(RECOVERY_ID)
        preferences.edit { remove(OVERTIME_ALERT) }
    }

    private fun open(event: FocusEvent): PendingIntent = PendingIntent.getActivity(
        context, 0, event.activityIntent(context, linkToken()), PendingIntent.FLAG_IMMUTABLE,
    )

    private fun notification(channel: String, title: String, body: String): NotificationCompat.Builder {
        ensureChannels()
        return NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentTitle(title)
            .setContentText(body)
    }

    private fun canPost(channel: String): Boolean =
        enabled() && manager.getNotificationChannel(channel)?.importance != NotificationManager.IMPORTANCE_NONE

    private fun post(id: Int, notification: Notification): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return try {
            manager.notify(id, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    private fun ensureChannels() {
        manager.createNotificationChannels(listOf(
            NotificationChannel(TIMER, "Timer fokus", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(REMINDERS, "Pengingat fokus", NotificationManager.IMPORTANCE_DEFAULT),
        ))
    }

    companion object {
        const val ACTIVE_ID = 3100
        const val REMINDER_ID = 3101
        const val RECOVERY_ID = 3102
        private const val TIMER = "focus_timer"
        private const val REMINDERS = "focus_reminders"
        private const val OVERTIME_ALERT = "overtime_alert"
    }
}
