package com.tazer.taskorganizer.timer

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.core.content.edit
import com.tazer.taskorganizer.data.FocusChange
import com.tazer.taskorganizer.data.TaskRepository
import com.tazer.taskorganizer.data.TazerDatabase
import com.tazer.taskorganizer.data.WorkSession
import com.tazer.taskorganizer.data.WorkSessionState
import com.tazer.taskorganizer.domain.FocusMoment
import java.lang.ref.WeakReference
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class FocusRuntime private constructor(private val context: Context) {
    private val repository = TaskRepository(TazerDatabase.getInstance(context))
    private val preferences = context.getSharedPreferences("tazer_focus_runtime", Context.MODE_PRIVATE)
    private val lock = Mutex()
    private val bootId by lazy {
        val count = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
        // If an OEM omits the boot count, conservatively recover from the last checkpoint.
        if (count >= 0) "boot-$count" else UUID.randomUUID().toString()
    }
    private val linkToken by lazy {
        preferences.getString(LINK_TOKEN, null) ?: UUID.randomUUID().toString().also {
            preferences.edit { putString(LINK_TOKEN, it) }
        }
    }
    private val alarms = FocusAlarms(context, preferences)
    private val notifications = FocusNotifications(context, preferences) { linkToken }
    @Volatile private var service = WeakReference<FocusService>(null)

    fun now(): FocusMoment = FocusMoment(System.currentTimeMillis(), SystemClock.elapsedRealtime(), bootId)

    fun readLink(intent: Intent): FocusLink? = FocusEvent.readLink(context, intent, linkToken)

    fun canScheduleExactAlarms(): Boolean = alarms.canScheduleExact()

    fun notificationsEnabled(): Boolean = notifications.enabled()

    fun exactAlarmSettingsIntent(): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.fromParts("package", context.packageName, null))
        } else null

    fun batterySettingsIntent(): Intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

    /** Called after the ViewModel establishes the owner through its sign-in flow. */
    suspend fun activate(uid: String) = serialized {
        require(uid.isNotBlank())
        if (owner() != uid) {
            owner()?.let { deactivateLocked(it) }
            saveOwner(uid)
        }
        syncLocked(uid)
        Unit
    }

    suspend fun deactivate(uid: String) = withContext(NonCancellable) {
        serialized {
            if (owner() == uid) deactivateLocked(uid)
        }
    }

    suspend fun sync(uid: String) = serialized {
        if (owner() == uid) syncLocked(uid, forceNotification = true)
        Unit
    }

    /** Called only after an explicit user action while the Activity is visible. */
    suspend fun start(uid: String, id: String, revision: Long): FocusChange? {
        return withContext(NonCancellable) {
            change(uid) { moment ->
                repository.startFocus(uid, id, revision, moment).also { result ->
                    if (result?.active != null) startService()
                }
            }
        }
    }

    suspend fun done(uid: String, id: String, revision: Long): FocusChange? =
        change(uid) { repository.finishFocus(uid, id, revision, it) }

    suspend fun extend(uid: String, id: String, revision: Long, extraMinutes: Int): FocusChange? =
        change(uid) { repository.extendFocus(uid, id, revision, extraMinutes, it) }

    suspend fun pause(uid: String, id: String, revision: Long): FocusChange? =
        change(uid) { repository.pauseFocus(uid, id, revision, it) }

    suspend fun defer(uid: String, resumeAt: Long): FocusChange? =
        change(uid) { repository.deferFocus(uid, resumeAt, it) }

    internal suspend fun onEvent(event: FocusEvent) = serialized {
        if (owner() != event.uid) return@serialized
        val moment = now()
        when (event.kind) {
            FocusEventKind.DONE -> {
                repository.finishFocus(event.uid, event.id, event.revision, moment) ?: return@serialized
            }
            FocusEventKind.CUTOFF -> {
                if (!alarms.consume(event)) return@serialized
                repository.checkFocus(event.uid, event.id, event.revision, moment) ?: return@serialized
            }
            FocusEventKind.START_ALARM -> {
                if (alarms.consume(event)) onStartAlarm(event, moment)
                return@serialized
            }
            else -> return@serialized
        }
        syncLocked(event.uid)
        Unit
    }

    internal suspend fun onSystemChange(action: String) = serialized {
        val uid = owner() ?: return@serialized
        if (action == Intent.ACTION_TIME_CHANGED || action == Intent.ACTION_TIMEZONE_CHANGED) {
            repository.rebuildPlan(uid, now().wallMs)
        }
        syncLocked(uid, forceAlarms = true, forceNotification = true)
        Unit
    }

    internal fun foregroundPlaceholder(): Notification = notifications.foregroundPlaceholder()

    internal fun attach(service: FocusService) {
        this.service = WeakReference(service)
    }

    internal fun detach(service: FocusService) {
        if (this.service.get() === service) this.service.clear()
    }

    internal suspend fun serviceTick(forceNotification: Boolean): Long? = serialized {
        val uid = owner()
        if (uid == null) {
            stopService()
            return@serialized null
        }
        val active = syncLocked(uid, forceNotification = forceNotification) ?: return@serialized null
        if (active.state == WorkSessionState.OVERTIME) return@serialized CHECKPOINT_MS
        val untilCutoff = (active.cutoffElapsedMs ?: return@serialized CHECKPOINT_MS) - now().elapsedMs
        untilCutoff.coerceIn(1L, CHECKPOINT_MS)
    }

    private suspend fun change(uid: String, transition: suspend (FocusMoment) -> FocusChange?): FocusChange? =
        serialized {
            if (owner() != uid) return@serialized null
            val result = transition(now())
            if (result != null) syncLocked(uid)
            result
        }

    private suspend fun deactivateLocked(uid: String) {
        try {
            repository.activeFocus(uid)?.let { repository.pauseFocus(uid, it.id, it.revision, now()) }
        } finally {
            try {
                saveOwner(null)
            } finally {
                stopService()
                notifications.cancelAll()
                preferences.edit { remove(LAST_REMINDER) }
                alarms.reconcile(emptyList())
            }
        }
    }

    private suspend fun syncLocked(
        uid: String,
        forceAlarms: Boolean = false,
        forceNotification: Boolean = false,
    ): WorkSession? {
        val moment = now()
        val before = repository.activeFocus(uid)
        val active = if (before != null) repository.recoverFocus(uid, moment)?.active else null
        if (active == null) {
            stopService()
            if (before != null && before.timerBootId != moment.bootId) notifications.showInterrupted()
            reconcileReminder(uid, moment, forceAlarms)
        } else {
            notifications.cancelReminder()
            notifications.showActive(active, moment, forceNotification)
            val cutoff = active.cutoffElapsedMs
            val wanted = if (active.state != WorkSessionState.OVERTIME && cutoff != null) {
                listOf(FocusAlarm(FocusEvent(active, FocusEventKind.CUTOFF), AlarmManager.ELAPSED_REALTIME_WAKEUP, cutoff))
            } else emptyList()
            alarms.reconcile(wanted, forceAlarms)
        }
        return active
    }

    private suspend fun onStartAlarm(event: FocusEvent, now: FocusMoment) {
        if (repository.activeFocus(event.uid) != null) return
        val session = repository.focusSession(event.uid, event.id)
        if (session?.revision == event.revision && session.state == WorkSessionState.PENDING &&
            now.wallMs in session.plannedStartAt..<session.plannedEndAt
        ) {
            showReminderOnce(session, now)
        }
        reconcileReminder(event.uid, now, forceAlarms = false, showDue = false)
    }

    private suspend fun reconcileReminder(
        uid: String,
        now: FocusMoment,
        forceAlarms: Boolean,
        showDue: Boolean = true,
    ) {
        if (!notifications.remindersEnabled()) {
            notifications.cancelReminder()
            alarms.reconcile(emptyList())
            return
        }
        val pending = repository.pendingFocus(uid).filter { now.wallMs < it.plannedEndAt }
        val due = pending.firstOrNull { it.plannedStartAt <= now.wallMs }
        if (due == null) {
            notifications.cancelReminder()
        } else if (showDue) {
            showReminderOnce(due, now)
        }
        // One next reminder is enough. On delivery the next pending slot is armed.
        val next = pending.firstOrNull { it.plannedStartAt > now.wallMs }
        val wanted = next?.let {
            listOf(FocusAlarm(FocusEvent(it, FocusEventKind.START_ALARM), AlarmManager.RTC_WAKEUP, it.plannedStartAt))
        }.orEmpty()
        alarms.reconcile(wanted, forceAlarms)
    }

    private fun showReminderOnce(session: WorkSession, now: FocusMoment) {
        val key = FocusEvent(session, FocusEventKind.REMINDER).uri("event").toString()
        if (preferences.getString(LAST_REMINDER, null) == key) return
        notifications.cancelReminder()
        if (notifications.showReminder(session, now.wallMs)) {
            preferences.edit { putString(LAST_REMINDER, key) }
        }
    }

    private fun startService() {
        try {
            context.startForegroundService(Intent(context, FocusService::class.java))
        } catch (e: IllegalStateException) {
            Log.i(TAG, "Focus service start restricted; Room and alarms remain active", e)
        } catch (e: SecurityException) {
            Log.w(TAG, "Focus service start denied; Room and alarms remain active", e)
        }
    }

    private fun stopService() {
        service.get()?.stopSelf()
        notifications.cancelActive()
    }

    private fun owner(): String? = preferences.getString(OWNER_UID, null)

    private fun saveOwner(uid: String?) {
        val editor = preferences.edit()
        editor.putString(OWNER_UID, uid)
        check(editor.commit()) { "Could not persist the active focus owner" }
    }

    private suspend fun <T> serialized(block: suspend () -> T): T =
        withContext(Dispatchers.IO) { lock.withLock { block() } }

    companion object {
        private const val TAG = "FocusRuntime"
        private const val OWNER_UID = "active_uid"
        private const val LINK_TOKEN = "link_token"
        private const val LAST_REMINDER = "last_reminder"
        private const val CHECKPOINT_MS = 30_000L
        // get() supplies the application context, which shares the singleton's lifetime.
        @SuppressLint("StaticFieldLeak")
        @Volatile private var instance: FocusRuntime? = null

        fun get(context: Context): FocusRuntime = instance ?: synchronized(this) {
            instance ?: FocusRuntime(context.applicationContext).also { instance = it }
        }
    }
}
