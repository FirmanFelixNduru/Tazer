package com.tazer.taskorganizer.timer

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.tazer.taskorganizer.MainActivity
import com.tazer.taskorganizer.data.WorkSessionState
import com.tazer.taskorganizer.domain.BriefingKind
import com.tazer.taskorganizer.domain.BriefingMessage
import com.tazer.taskorganizer.domain.BriefingSession
import com.tazer.taskorganizer.domain.BriefingSettings
import com.tazer.taskorganizer.domain.BriefingTask
import com.tazer.taskorganizer.domain.DailyBriefing
import com.tazer.taskorganizer.widget.FocusWidget
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/** Device-wide briefing choices. A missing file (fresh install or upgrade from 1.5.0) means both are off. */
internal class BriefingPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("daily_briefing", Context.MODE_PRIVATE)

    fun read() = BriefingSettings(
        preferences.getBoolean("morning_enabled", false),
        preferences.getInt("morning_minute", 7 * 60).takeIf { it in 0 until 1440 } ?: (7 * 60),
        preferences.getBoolean("evening_enabled", false),
        preferences.getInt("evening_minute", 21 * 60).takeIf { it in 0 until 1440 } ?: (21 * 60),
    )

    fun save(settings: BriefingSettings): Boolean = preferences.edit()
        .putBoolean("morning_enabled", settings.morningEnabled)
        .putInt("morning_minute", settings.morningMinute)
        .putBoolean("evening_enabled", settings.eveningEnabled)
        .putInt("evening_minute", settings.eveningMinute)
        .commit()
}

internal object DailyBriefingAlarms {
    const val CHANNEL = "daily_briefing"
    const val ACTION_OPEN_STATISTICS = "com.tazer.taskorganizer.briefing.OPEN_STATISTICS"
    private const val ACTION_PREFIX = "com.tazer.taskorganizer.briefing."
    private const val NOTIFICATION_BASE = 4100
    private const val TAG = "DailyBriefing"

    fun kind(action: String?): BriefingKind? =
        BriefingKind.entries.firstOrNull { action == ACTION_PREFIX + it.name }

    /** Installs or cancels both daily alarms from the saved settings. Safe to call repeatedly. */
    suspend fun reschedule(context: Context) {
        val settings = BriefingPreferences(context).read()
        val manager = context.getSystemService(AlarmManager::class.java)
        val zone = zone(context)
        val now = System.currentTimeMillis()
        for (kind in BriefingKind.entries) {
            if (!settings.enabled(kind)) {
                alarm(context, kind, PendingIntent.FLAG_NO_CREATE)?.let { manager.cancel(it); it.cancel() }
                continue
            }
            val at = DailyBriefing.nextTrigger(now, zone, settings.minute(kind))
            val operation = requireNotNull(alarm(context, kind, PendingIntent.FLAG_UPDATE_CURRENT))
            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()) {
                    manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
                    continue
                }
            } catch (denied: SecurityException) {
                Log.i(TAG, "Exact alarm access changed during scheduling", denied)
            }
            // Without exact access Android may delay the briefing by some minutes.
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
        }
    }

    internal fun scheduled(context: Context, kind: BriefingKind): Boolean =
        alarm(context, kind, PendingIntent.FLAG_NO_CREATE) != null

    /** Builds today's message for the signed-in owner, or null when no account is active. */
    internal suspend fun message(context: Context, kind: BriefingKind, now: Long = System.currentTimeMillis()): BriefingMessage? {
        val runtime = FocusRuntime.get(context)
        val uid = runtime.widgetOwnerUid() ?: return null
        val repository = runtime.taskRepository
        val zone = zone(context)
        val tasks = repository.observeTasks(uid).first().map {
            BriefingTask(it.id, it.title, it.deadlineAt, it.createdAt, it.remainingMinutes, it.difficultyScore, it.completedAt)
        }
        val sessions = repository.observeWorkSessions(uid).first().mapNotNull {
            if (it.state == WorkSessionState.CANCELLED) null
            else BriefingSession(it.plannedStartAt, it.plannedMinutes, it.state == WorkSessionState.DONE,
                it.state != WorkSessionState.DONE)
        }
        return if (kind == BriefingKind.MORNING) DailyBriefing.morning(now, zone, tasks, sessions)
        else DailyBriefing.evening(now, zone, tasks, sessions)
    }

    suspend fun deliver(context: Context, kind: BriefingKind) {
        if (!BriefingPreferences(context).read().enabled(kind) || !canPost(context)) return
        val message = message(context, kind) ?: return
        val open = Intent(context, MainActivity::class.java)
            .setAction(if (kind == BriefingKind.MORNING) FocusWidget.ACTION_OPEN_SCHEDULE else ACTION_OPEN_STATISTICS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentTitle(message.title)
            .setContentText(message.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.body))
            .setContentIntent(PendingIntent.getActivity(context, NOTIFICATION_BASE + kind.ordinal, open, PendingIntent.FLAG_IMMUTABLE))
            .setAutoCancel(true)
            .build()
        try {
            context.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_BASE + kind.ordinal, notification)
        } catch (denied: SecurityException) {
            Log.i(TAG, "Notification permission changed before posting", denied)
        }
    }

    fun ensureChannel(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Smart Daily Briefing", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Ringkasan jadwal pagi dan review malam"
            },
        )
    }

    fun canPost(context: Context): Boolean {
        ensureChannel(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return granted && manager.areNotificationsEnabled() &&
            manager.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE
    }

    private suspend fun zone(context: Context): ZoneId {
        val runtime = FocusRuntime.get(context)
        val zoneId = runtime.widgetOwnerUid()?.let { runtime.taskRepository.observeProfile(it).first()?.zoneId }
        return zoneId?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
    }

    private fun alarm(context: Context, kind: BriefingKind, flags: Int): PendingIntent? = PendingIntent.getBroadcast(
        context, NOTIFICATION_BASE + kind.ordinal,
        Intent(context, DailyBriefingReceiver::class.java).setAction(ACTION_PREFIX + kind.name),
        flags or PendingIntent.FLAG_IMMUTABLE,
    )
}

class DailyBriefingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = DailyBriefingAlarms.kind(intent.action) ?: return
        val pending = goAsync()
        val application = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeout(9_000L) {
                    // Rescheduling first keeps the daily chain alive even if building the message fails.
                    DailyBriefingAlarms.reschedule(application)
                    DailyBriefingAlarms.deliver(application, kind)
                }
            } catch (cancelled: CancellationException) {
                Log.w("DailyBriefing", "Briefing broadcast interrupted", cancelled)
                throw cancelled
            } catch (e: RuntimeException) {
                Log.w("DailyBriefing", "Could not deliver briefing", e)
            } finally {
                pending.finish()
            }
        }
    }
}
