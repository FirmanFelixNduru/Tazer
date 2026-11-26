package com.tazer.taskorganizer.timer

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.tazer.taskorganizer.data.FocusChange
import com.tazer.taskorganizer.data.TaskRepository
import com.tazer.taskorganizer.data.TazerDatabase
import com.tazer.taskorganizer.data.WorkSession
import com.tazer.taskorganizer.data.WorkSessionState
import com.tazer.taskorganizer.domain.FocusMoment
import com.tazer.taskorganizer.domain.FocusPreset
import com.tazer.taskorganizer.domain.FocusPresetMode
import com.tazer.taskorganizer.widget.FocusWidget
import java.lang.ref.WeakReference
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal data class FocusSettings(val preset: FocusPreset = FocusPreset.DEFAULT, val dndEnabled: Boolean = false)

class FocusRuntime private constructor(private val context: Context) {
    private val preferences = context.getSharedPreferences("tazer_focus_runtime", Context.MODE_PRIVATE)
    val taskRepository = TaskRepository(TazerDatabase.getInstance(context)) { settingsFor(it).preset }
    private val lock = Mutex()
    private val scope = CoroutineScope(Dispatchers.IO)
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
    private val dnd = FocusDnd(context, preferences)
    private var pendingDndSegment: String? = null
    @Volatile private var service = WeakReference<FocusService>(null)

    init {
        if (dnd.supported()) {
            ContextCompat.registerReceiver(
                context, FocusReceiver(), IntentFilter(NotificationManager.ACTION_NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        }
        FocusWidget.refresh(context)
    }

    fun now(): FocusMoment = FocusMoment(System.currentTimeMillis(), SystemClock.elapsedRealtime(), bootId)

    fun readLink(intent: Intent): FocusLink? = FocusEvent.readLink(context, intent, linkToken)

    internal fun widgetOwnerUid(): String? = owner()

    internal fun widgetFocusIntent(link: FocusLink): Intent {
        val kind = when (link.kind) {
            FocusLinkKind.REMINDER -> FocusEventKind.REMINDER
            FocusLinkKind.ACTIVE -> FocusEventKind.ACTIVE
            FocusLinkKind.EXTEND -> error("Widget links only open a session")
        }
        return FocusEvent(link.uid, link.id, link.revision, kind).activityIntent(context, linkToken)
    }


    fun canScheduleExactAlarms(): Boolean = alarms.canScheduleExact()

    fun notificationsEnabled(): Boolean = notifications.enabled()

    fun exactAlarmSettingsIntent(): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.fromParts("package", context.packageName, null))
        } else null

    fun batterySettingsIntent(): Intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

    internal fun focusSettings(): FocusSettings = owner()?.let(::settingsFor) ?: FocusSettings()

    internal fun dndSupported(): Boolean = dnd.supported()

    internal fun hasDndAccess(): Boolean = dnd.hasAccess()

    internal fun dndSettingsIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)

    internal suspend fun saveFocusSettings(settings: FocusSettings): Boolean = withContext(NonCancellable) {
        serialized {
            val uid = owner() ?: return@serialized false
            val saved = preferences.edit()
                .putString("preset_mode_$uid", settings.preset.mode.name)
                .putInt("preset_work_$uid", settings.preset.customWorkMinutes)
                .putInt("preset_break_$uid", settings.preset.customBreakMinutes)
                .putBoolean("dnd_opt_in_$uid", settings.dndEnabled)
                .commit()
            if (saved && !settings.dndEnabled) reconcileDnd(null)
            saved
        }
    }

    private fun settingsFor(uid: String): FocusSettings {
        val snapshot = preferences.all
        val mode = snapshot["preset_mode_$uid"]
        return FocusSettings(
            FocusPreset(
                FocusPresetMode.entries.firstOrNull { it.name == mode } ?: FocusPresetMode.DEEP_WORK,
                ((snapshot["preset_work_$uid"] as? Int) ?: 50).coerceIn(FocusPreset.WORK_RANGE),
                ((snapshot["preset_break_$uid"] as? Int) ?: 10).coerceIn(FocusPreset.BREAK_RANGE),
            ),
            snapshot["dnd_opt_in_$uid"] == true,
        )
    }

    /** Called after the ViewModel establishes the owner through its sign-in flow. */
    suspend fun activate(uid: String) = serialized {
        require(uid.isNotBlank())
        if (owner() != uid) {
            owner()?.let { deactivateLocked(it) }
            FocusWidget.beginOwnerChange(context)
            try {
                saveOwner(uid)
            } finally {
                FocusWidget.endOwnerChange(context)
            }
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
    suspend fun start(uid: String, id: String, revision: Long): FocusChange? =
        change(uid) { moment ->
            taskRepository.startFocus(uid, id, revision, moment).also { result ->
                if (result?.active != null) startService()
            }
        }

    suspend fun done(uid: String, id: String, revision: Long): FocusChange? =
        change(uid) { taskRepository.finishFocus(uid, id, revision, it) }

    suspend fun extend(uid: String, id: String, revision: Long, extraMinutes: Int): FocusChange? =
        change(uid) { taskRepository.extendFocus(uid, id, revision, extraMinutes, it) }

    suspend fun pause(uid: String, id: String, revision: Long): FocusChange? =
        change(uid) { taskRepository.pauseFocus(uid, id, revision, it) }

    /** Called after an explicit user action while the Activity is visible. */
    suspend fun resume(uid: String, id: String, revision: Long): FocusChange? =
        change(uid) { moment ->
            taskRepository.resumeFocus(uid, id, revision, moment).also { result ->
                if (result?.active != null) startService()
            }
        }

    suspend fun stop(uid: String, id: String, revision: Long): FocusChange? =
        change(uid) { taskRepository.stopFocus(uid, id, revision, it) }

    suspend fun defer(uid: String, resumeAt: Long): FocusChange? =
        change(uid) { taskRepository.deferFocus(uid, resumeAt, it) }

    internal suspend fun onEvent(event: FocusEvent) = serialized {
        if (owner() != event.uid) return@serialized
        val moment = now()
        when (event.kind) {
            FocusEventKind.DONE -> {
                taskRepository.finishFocus(event.uid, event.id, event.revision, moment) ?: return@serialized
            }
            FocusEventKind.CUTOFF -> {
                if (!alarms.consume(event)) return@serialized
                // Room might have committed a pause before the process could cancel this alarm.
                taskRepository.checkFocus(event.uid, event.id, event.revision, moment)
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
        val uid = owner()
        if (uid == null) {
            reconcileDnd(null)
            return@serialized
        }
        if (action == NotificationManager.ACTION_NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED) {
            reconcileDnd(taskRepository.activeFocus(uid))
            return@serialized
        }
        if (action == Intent.ACTION_TIME_CHANGED || action == Intent.ACTION_TIMEZONE_CHANGED) {
            taskRepository.rebuildPlan(uid, now().wallMs)
        }
        syncLocked(uid, forceAlarms = true, forceNotification = true)
        Unit
    }

    internal fun foregroundPlaceholder(): Notification = notifications.foregroundPlaceholder()

    internal fun attach(service: FocusService) {
        this.service = WeakReference(service)
    }

    internal fun detach(service: FocusService) {
        if (this.service.get() !== service) return
        this.service.clear()
        scope.launch {
            serialized {
                // A replacement sticky service may already have attached.
                if (this@FocusRuntime.service.get() == null) dnd.reconcile(null)
            }
        }
    }

    internal suspend fun serviceTick(forceNotification: Boolean): Long? = serialized {
        val uid = owner()
        if (uid == null) {
            reconcileDnd(null)
            stopService()
            return@serialized null
        }
        val active = syncLocked(uid, forceNotification = forceNotification) ?: return@serialized null
        if (active.state == WorkSessionState.PAUSED) return@serialized null
        if (active.state == WorkSessionState.OVERTIME) return@serialized CHECKPOINT_MS
        val untilCutoff = (active.cutoffElapsedMs ?: return@serialized CHECKPOINT_MS) - now().elapsedMs
        untilCutoff.coerceIn(1L, CHECKPOINT_MS)
    }

    private suspend fun change(uid: String, transition: suspend (FocusMoment) -> FocusChange?): FocusChange? =
        withContext(NonCancellable) {
            serialized {
                if (owner() != uid) return@serialized null
                val result = transition(now()) ?: return@serialized null
                result.active?.countdownSegment()?.let {
                    if (settingsFor(uid).dndEnabled) pendingDndSegment = it
                }
                val current = syncLocked(uid)
                if (result.active != null) result.copy(active = current) else result
            }
        }

    private suspend fun deactivateLocked(uid: String) {
        FocusWidget.beginOwnerChange(context)
        try {
            taskRepository.interruptRunningFocus(uid, now())
        } finally {
            try {
                saveOwner(null)
            } finally {
                FocusWidget.endOwnerChange(context)
                reconcileDnd(null)
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
        val before = taskRepository.activeFocus(uid)
        val active = if (before != null) taskRepository.recoverFocus(uid, moment)?.active else null
        reconcileDnd(active)
        if (active == null) {
            stopService()
            if (before != null && before.timerBootId != moment.bootId) notifications.showInterrupted()
            reconcileReminder(uid, moment, forceAlarms)
        } else if (active.state == WorkSessionState.PAUSED) {
            stopService()
            notifications.cancelReminder()
            alarms.reconcile(emptyList())
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

    private fun WorkSession.countdownSegment(): String? =
        if (state == WorkSessionState.RUNNING || state == WorkSessionState.EXTENDED) {
            "$ownerUid:$id:$revision:$timerBootId"
        } else null

    private fun reconcileDnd(active: WorkSession?) {
        val segment = active?.takeIf { settingsFor(it.ownerUid).dndEnabled }?.countdownSegment()
        if (pendingDndSegment != segment) pendingDndSegment = null
        val foregroundSegment = segment?.takeIf { service.get() != null }
        val newlyStarted = foregroundSegment != null && pendingDndSegment == foregroundSegment
        if (newlyStarted) pendingDndSegment = null
        dnd.reconcile(foregroundSegment, newlyStarted)
    }

    private suspend fun onStartAlarm(event: FocusEvent, now: FocusMoment) {
        if (taskRepository.activeFocus(event.uid) != null) return
        val session = taskRepository.focusSession(event.uid, event.id)
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
        val pending = taskRepository.pendingFocus(uid).filter { now.wallMs < it.plannedEndAt }
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
