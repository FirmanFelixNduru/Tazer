package com.tazer.taskorganizer.ui

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tazer.taskorganizer.data.AnalysisSource
import com.tazer.taskorganizer.data.CalendarEvent
import com.tazer.taskorganizer.data.CalendarRepository
import com.tazer.taskorganizer.data.CloudSync
import com.tazer.taskorganizer.data.FixedSchedule
import com.tazer.taskorganizer.data.FixedScheduleKind
import com.tazer.taskorganizer.data.FocusChange
import com.tazer.taskorganizer.data.IcsImportPreview
import com.tazer.taskorganizer.data.Subtask
import com.tazer.taskorganizer.data.SubtaskDraft
import com.tazer.taskorganizer.data.SubtaskRepository
import com.tazer.taskorganizer.data.Task
import com.tazer.taskorganizer.data.TaskRepository
import com.tazer.taskorganizer.data.TaskStatus
import com.tazer.taskorganizer.data.UserProfile
import com.tazer.taskorganizer.data.WorkSession
import com.tazer.taskorganizer.data.WorkSessionState
import com.tazer.taskorganizer.domain.DeadlineRisk
import com.tazer.taskorganizer.domain.OfflineAnalyzer
import com.tazer.taskorganizer.domain.PaceCalibration
import com.tazer.taskorganizer.domain.PaceSample
import com.tazer.taskorganizer.integration.Account
import com.tazer.taskorganizer.integration.AndroidIntegration
import com.tazer.taskorganizer.integration.AuthCallback
import com.tazer.taskorganizer.integration.ByokProvider
import com.tazer.taskorganizer.integration.IntegrationError
import com.tazer.taskorganizer.integration.IntegrationException
import com.tazer.taskorganizer.integration.Subscription
import com.tazer.taskorganizer.integration.SubscriptionTier
import com.tazer.taskorganizer.integration.TaskAnalysisInput
import com.tazer.taskorganizer.timer.FocusLink
import com.tazer.taskorganizer.timer.FocusLinkKind
import com.tazer.taskorganizer.timer.FocusRuntime
import java.io.InputStream
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class Workspace(
    val profile: UserProfile,
    val tasks: List<Task>,
    val fixed: List<FixedSchedule>,
    val sessions: List<WorkSession>,
    val events: List<CalendarEvent>,
    val subtasks: List<Subtask> = emptyList(),
)

data class TazerState(
    val uid: String? = null,
    val demo: Boolean = false,
    val subscription: Subscription? = null,
    val data: Workspace? = null,
    val risks: List<DeadlineRisk> = emptyList(),
    val focusTarget: FocusTarget? = null,
    val now: Long = System.currentTimeMillis(),
    val recovery: Boolean = false,
    val googlePending: Boolean = false,
    val busy: Boolean = false,
    val message: String? = null,
    val cloudStatus: String? = null,
    val cloudSyncing: Boolean = false,
)

data class FocusTarget(val id: String, val requestExtension: Boolean = false)

data class TaskInput(
    val title: String,
    val course: String,
    val description: String,
    val deadlineAt: Long,
    val difficulty: Int,
    val minutes: Int,
    val source: AnalysisSource,
    val analyzerBaselineMinutes: Int? = null,
    val checklist: ChecklistChange? = null,
)

data class ChecklistChange(val original: List<SubtaskDraft>, val items: List<SubtaskDraft>)

data class AnalysisSuggestion(
    val difficulty: Int,
    val minutes: Int,
    val source: AnalysisSource,
    val analyzerBaselineMinutes: Int = minutes,
    val calibrationPercent: Int? = null,
    val subtasks: List<String> = emptyList(),
)

enum class AnalyzerMode { OFFLINE, CLOUD, BYOK }

class TazerViewModel internal constructor(
    private val repository: TaskRepository,
    private val calendar: CalendarRepository,
    private val integration: AndroidIntegration,
    val focus: FocusRuntime,
    val demoAvailable: Boolean,
    private val subtaskRepository: SubtaskRepository? = null,
    private val cloud: CloudSync? = null,
    private val network: Flow<Boolean> = flowOf(true),
) : ViewModel() {
    private val _state = MutableStateFlow(TazerState())
    val state = _state.asStateFlow()
    val authAvailable get() = integration.enabled
    val mockAvailable get() = integration.mockEnabled
    val subtasksAvailable get() = subtaskRepository != null

    private var ownerJob: Job? = null
    private var syncJob: Job? = null
    private var online = false
    private var restored = false
    private var pendingFocusLink: FocusLink? = null

    fun saveEvent(
        id: String?, title: String, from: Long, through: Long, minute: Int?, description: String,
        onSaved: () -> Unit,
    ) = runAction {
        val uid = _state.value.uid ?: return@runAction
        if (calendar.saveEvent(uid, id, title, from, through, minute, description)) onSaved()
        else showMessage("Acara sudah berubah. Buka ulang kalender.")
    }

    fun deleteEvent(id: String, onDeleted: () -> Unit) = runAction {
        val uid = _state.value.uid ?: return@runAction
        if (!calendar.deleteEvent(uid, id)) showMessage("Acara sudah dihapus.")
        onDeleted()
    }

    fun openDemo() = runAction { enter(DEMO_UID, null, true) }
    fun signIn(email: String, password: String) = runAction { enter(integration.signIn(email, password)) }
    fun signInMock(tier: SubscriptionTier) = runAction { enter(integration.signInMock(tier)) }
    fun signUp(email: String, password: String) = runAction {
        integration.signUp(email, password)
        showMessage("Periksa email untuk mengonfirmasi akun.")
    }
    fun resetPassword(email: String) = runAction {
        integration.resetPassword(email)
        showMessage("Periksa email untuk melanjutkan pemulihan kata sandi.")
    }
    fun startGoogle(launch: (Intent) -> Unit) = runAction {
        val intent = integration.googleSignInIntent()
        try {
            launch(intent)
            _state.update { it.copy(googlePending = true) }
        } catch (_: android.content.ActivityNotFoundException) {
            integration.cancelGoogleSignIn()
            throw IntegrationException(IntegrationError.BROWSER_UNAVAILABLE)
        }
    }

    fun cancelGoogle() = runAction {
        integration.cancelGoogleSignIn()
        _state.update { it.copy(googlePending = false) }
        showMessage(IntegrationError.GOOGLE_CANCELLED.message)
    }

    fun handleAuthCallback(uri: Uri?) {
        if (uri != null && integration.isAuthCallback(uri)) runAction {
            val callback = try {
                integration.handleAuthCallback(uri)
            } catch (error: IntegrationException) {
                if (error.code != IntegrationError.INVALID_CALLBACK) {
                    _state.update { it.copy(googlePending = false) }
                }
                throw error
            }
            _state.update { it.copy(googlePending = false) }
            when (callback) {
                is AuthCallback.SignedIn -> enter(callback.account)
                AuthCallback.PasswordRecovery -> _state.update { it.copy(recovery = true) }
            }
        }
    }

    fun updatePassword(password: String) = runAction { enter(integration.updatePassword(password)) }

    fun restoreSession() {
        if (restored) return
        restored = true
        runAction {
            val account = integration.restoreAccount()
            if (account == null) {
                if (_state.value.uid == null) focus.widgetOwnerUid()?.let { focus.deactivate(it) }
                return@runAction
            }
            enter(account)
            viewModelScope.launch {
                try {
                    refreshSubscription()
                } catch (failure: CancellationException) {
                    throw failure
                } catch (failure: IntegrationException) {
                    if (failure.rejectsSession() && _state.value.uid == account.uid.value) {
                        reportIntegrationFailure(failure)
                    }
                }
            }
        }
    }

    fun signOut() = runAction {
        ownerJob?.cancel()
        syncJob?.cancel()
        try {
            _state.value.uid?.let { focus.deactivate(it) }
        } finally {
            try {
                integration.signOut()
            } finally {
                pendingFocusLink = null
                _state.value = TazerState()
            }
        }
    }

    fun handleFocusLink(link: FocusLink) {
        pendingFocusLink = link
        val uid = _state.value.uid
        when {
            uid != null -> viewModelScope.launch { consumeFocusLink(uid) }
            demoAvailable && link.uid == DEMO_UID -> openDemo()
            else -> restoreSession()
        }
    }

    private suspend fun consumeFocusLink(uid: String) {
        val link = pendingFocusLink ?: return
        pendingFocusLink = null
        if (link.uid != uid || _state.value.uid != uid) return
        val session = repository.focusSession(uid, link.id) ?: return
        if (session.revision != link.revision || session.state in setOf(WorkSessionState.DONE, WorkSessionState.CANCELLED)) return
        _state.update { if (it.uid == uid) it.copy(focusTarget = FocusTarget(session.id, link.kind == FocusLinkKind.EXTEND)) else it }
    }

    fun openFocus(session: WorkSession) {
        _state.update { if (it.uid == session.ownerUid) it.copy(focusTarget = FocusTarget(session.id)) else it }
    }

    fun closeFocus() = _state.update { it.copy(focusTarget = null) }

    fun refreshFocus() {
        val uid = _state.value.uid ?: return
        viewModelScope.launch {
            try {
                focus.sync(uid)
            } catch (failure: CancellationException) {
                throw failure
            } catch (_: Exception) {
                if (_state.value.uid == uid) showMessage("Pengingat sesi belum dapat diselaraskan.")
            }
        }
    }

    fun refreshPlan() {
        val uid = _state.value.uid ?: return
        viewModelScope.launch { rebuild(uid) }
    }

    fun startFocus(session: WorkSession) = focusAction("Mulai sesi tidak tersedia. Periksa jadwal terbaru.") { uid ->
        focus.start(uid, session.id, session.revision)
            .also { if (it?.active == null && it?.plan != null) showMessage("Sesi dipindahkan. Buka Jadwal untuk melihat waktu baru.") }
    }

    fun finishFocus(session: WorkSession) = focusAction("Sesi sudah berubah. Periksa status terbaru.", closeWhenDone = true) { uid ->
        focus.done(uid, session.id, session.revision)
    }

    fun extendFocus(session: WorkSession, minutes: Int) = focusAction("Tambahan waktu belum dapat disimpan.", closeWhenDone = true) { uid ->
        focus.extend(uid, session.id, session.revision, minutes)
    }

    fun pauseFocus(session: WorkSession) = focusAction("Sesi tidak dapat dijeda.") { uid ->
        focus.pause(uid, session.id, session.revision)
    }

    fun resumeFocus(session: WorkSession) = focusAction("Waktu sesi belum muat sebelum batas jadwal berikutnya.") { uid ->
        focus.resume(uid, session.id, session.revision)
    }

    fun stopFocus(session: WorkSession) = focusAction("Sesi tidak dapat dihentikan.", closeWhenDone = true) { uid ->
        focus.stop(uid, session.id, session.revision)
    }

    fun deferFocus(resumeAt: Long, onSaved: () -> Unit) = focusAction(
        "Jadwal belum dapat digeser. Pilih waktu mulai yang akan datang.", closeWhenDone = true, onSaved = onSaved,
    ) { uid -> focus.defer(uid, resumeAt) }

    private fun focusAction(
        rejected: String,
        closeWhenDone: Boolean = false,
        onSaved: () -> Unit = {},
        action: suspend (String) -> FocusChange?,
    ) = runAction {
        val uid = _state.value.uid ?: return@runAction
        val result = action(uid)
        if (result == null) {
            showMessage(rejected)
            focus.sync(uid)
            return@runAction
        }
        _state.update { state ->
            if (state.uid != uid) state else state.copy(
                now = focus.now().wallMs,
                risks = result.plan?.risks ?: state.risks,
                focusTarget = result.active?.let { FocusTarget(it.id) } ?: state.focusTarget.takeUnless { closeWhenDone },
            )
        }
        onSaved()
    }

    fun showMessage(message: String?) = _state.update { it.copy(message = message) }

    fun refreshTier() = runAction { refreshSubscription() }

    fun syncNow() {
        val uid = _state.value.uid?.takeIf(CloudSync::uuid) ?: return
        val engine = cloud ?: return
        if (syncJob?.isActive == true) return
        if (!online) {
            _state.update { it.copy(cloudStatus = "Offline. Perubahan tersimpan di perangkat.") }
            return
        }
        syncJob = viewModelScope.launch {
            _state.update { if (it.uid == uid) it.copy(cloudSyncing = true, cloudStatus = "Menyinkronkan…") else it }
            try {
                val result = engine.sync(uid)
                if (_state.value.uid != uid) return@launch
                if (result.replan) rebuild(uid) else if (result.changed) focus.sync(uid)
                val text = if (result.postponed) "Sinkronisasi dilanjutkan setelah sesi fokus selesai."
                    else "Sinkron dengan cloud."
                _state.update { if (it.uid == uid && online) it.copy(cloudStatus = text) else it }
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: IntegrationException) {
                if (_state.value.uid != uid) return@launch
                if (failure.rejectsSession()) reportIntegrationFailure(failure)
                else _state.update { it.copy(cloudStatus = "Sinkronisasi tertunda. Perubahan tetap tersimpan.") }
            } catch (_: Exception) {
                _state.update { if (it.uid == uid) it.copy(cloudStatus = "Sinkronisasi tertunda. Perubahan tetap tersimpan.") else it }
            } finally {
                _state.update { if (it.uid == uid) it.copy(cloudSyncing = false) else it }
            }
        }
    }

    private suspend fun refreshSubscription() {
        val uid = _state.value.uid ?: return
        if (_state.value.demo) return
        val subscription = integration.subscription()
        _state.update { if (it.uid == uid) it.copy(subscription = subscription) else it }
    }

    fun saveTask(task: Task?, input: TaskInput, onSaved: () -> Unit) = change(onSaved) { uid ->
        val checklist = input.checklist
        if (checklist == null) return@change persistTask(uid, task, input) != null

        val subtasks = requireNotNull(subtaskRepository)
        subtasks.withTransaction {
            if (task != null) {
                val current = subtasks.observe(uid).first().filter { it.taskId == task.id }
                    .sortedBy(Subtask::position).map(Subtask::asDraft)
                if (current != checklist.original) return@withTransaction false
            }
            val taskId = persistTask(uid, task, input) ?: return@withTransaction false
            subtasks.replace(uid, taskId, checklist.items)
            true
        }
    }

    private suspend fun persistTask(uid: String, task: Task?, input: TaskInput): String? {
        if (task == null) {
            return repository.createTask(
                uid, input.title, input.course, input.description, input.deadlineAt, input.difficulty,
                input.minutes, System.currentTimeMillis(), input.source,
                analyzerBaselineMinutes = input.analyzerBaselineMinutes,
            ).id
        }
        val updated = repository.updateTask(
            uid, task.id, input.title, input.course, input.description, input.deadlineAt,
            input.difficulty, input.minutes, input.source,
        )
        return task.id.takeIf { updated }
    }

    fun setSubtaskCompleted(taskId: String, id: String, completed: Boolean) = editSubtasks { repo, uid ->
        repo.setCompleted(uid, taskId, id, completed)
    }

    fun saveSubtask(taskId: String, id: String?, title: String, onSaved: () -> Unit) = editSubtasks(onSaved) { repo, uid ->
        repo.save(uid, taskId, id, title)
    }

    fun deleteSubtask(taskId: String, id: String) = editSubtasks { repo, uid ->
        repo.delete(uid, taskId, id)
    }

    private fun editSubtasks(
        onSaved: () -> Unit = {},
        operation: suspend (SubtaskRepository, String) -> Boolean,
    ) = runAction {
        val uid = _state.value.uid ?: return@runAction
        val subtasks = subtaskRepository ?: return@runAction
        if (operation(subtasks, uid)) onSaved()
        else showMessage("Subtask tidak dapat disimpan. Periksa daftar terbaru.")
    }

    fun finishTask(task: Task) = change { uid ->
        repository.completeTask(uid, task.id, System.currentTimeMillis())
    }

    fun reopenTask(task: Task, minutes: Int) = change { uid -> repository.reopenTask(uid, task.id, minutes) }

    fun deleteTask(task: Task) = change { uid -> repository.deleteTask(uid, task.id) }

    fun saveProfile(zone: ZoneId, start: Int, end: Int, onSaved: () -> Unit) = change(onSaved) { uid ->
        repository.saveProfile(uid, zone.id, start, end, _state.value.data?.profile?.busyUntilAt)
        true
    }

    fun saveFixed(
        schedule: FixedSchedule?, label: String, kind: FixedScheduleKind, day: Int, start: Int, end: Int,
        onSaved: () -> Unit,
    ) = change(onSaved) { uid ->
        repository.saveFixedScheduleRange(uid, schedule?.id, label, kind, day, start, end)
    }

    fun deleteFixed(schedule: FixedSchedule) = change { uid -> repository.deleteFixedSchedule(uid, schedule.id) }

    internal suspend fun previewIcs(ownerUid: String, open: () -> InputStream): IcsImportPreview {
        require(_state.value.uid == ownerUid)
        return calendar.previewIcs(ownerUid, open)
    }

    internal fun importIcs(preview: IcsImportPreview, close: () -> Unit) = runAction {
        val uid = _state.value.uid
        val counts = try {
            require(uid == preview.ownerUid)
            calendar.importIcs(preview.ownerUid, preview)
        } catch (failure: CancellationException) {
            throw failure
        } catch (_: IllegalArgumentException) {
            close()
            showMessage("Akun atau zona waktu telah berubah. Pilih kembali file .ics.")
            return@runAction
        } catch (_: Exception) {
            close()
            showMessage("Impor .ics belum tersimpan. Coba lagi.")
            return@runAction
        }
        close()
        showMessage("${counts.events} acara dan ${counts.weeklyBlocks} blok jadwal tetap diimpor. ${counts.duplicates} duplikat dilewati.")
        if (counts.weeklyBlocks > 0) rebuild(preview.ownerUid)
    }

    fun saveByok(provider: ByokProvider, model: String, key: String, onSaved: () -> Unit) = runAction {
        integration.saveByok(provider, model, key)
        onSaved()
    }

    fun deleteByok(onSaved: () -> Unit) = runAction {
        integration.deleteByok()
        onSaved()
    }

    suspend fun readByokSettings() = integration.byokSettings()

    fun reportIntegrationFailure(failure: IntegrationException) {
        val message = failure.message ?: "Layanan sementara tidak tersedia."
        if (failure.rejectsSession() && _state.value.uid != null && !_state.value.demo) {
            val uid = requireNotNull(_state.value.uid)
            ownerJob?.cancel()
            _state.value = TazerState(message = message)
            viewModelScope.launch {
                try {
                    focus.deactivate(uid)
                } catch (failure: CancellationException) {
                    throw failure
                } catch (_: Exception) {
                    // Session access is already cleared in the UI.
                }
            }
        } else {
            showMessage(message)
        }
    }

    suspend fun analyze(
        mode: AnalyzerMode, title: String, course: String, description: String, deadlineAt: Long,
    ): AnalysisSuggestion {
        val factor = _state.value.data?.let { PaceCalibration.factor(course, it.paceSamples()) }
        fun suggestion(difficulty: Int, minutes: Int, source: AnalysisSource, subtasks: List<String>) =
            AnalysisSuggestion(
                difficulty, PaceCalibration.adjustedMinutes(minutes, factor), source, minutes,
                factor?.let { (it * 100).roundToInt() }, subtasks,
            )

        if (mode == AnalyzerMode.OFFLINE) {
            val result = OfflineAnalyzer.analyze(title, description, deadlineAt, System.currentTimeMillis())
            return suggestion(result.difficultyScore, result.estimatedMinutes, AnalysisSource.OFFLINE, result.subtasks)
        }
        val input = TaskAnalysisInput(title, course, description, Instant.ofEpochMilli(deadlineAt))
        val result = if (mode == AnalyzerMode.CLOUD) integration.analyzeCloud(input) else integration.analyzeByok(input)
        if (mode == AnalyzerMode.CLOUD) {
            try {
                refreshSubscription()
            } catch (failure: CancellationException) {
                throw failure
            } catch (_: IntegrationException) {
                // Keep the valid result when refreshing the displayed quota is unavailable.
            }
        }
        return suggestion(result.difficultyScore, result.estimatedMinutes, AnalysisSource.AI, result.subtasks)
    }

    private fun change(onSaved: () -> Unit = {}, operation: suspend (String) -> Boolean) = runAction {
        val uid = _state.value.uid ?: return@runAction
        if (!operation(uid)) {
            showMessage("Perubahan tidak dapat disimpan. Buka ulang tugas atau jadwal.")
            return@runAction
        }
        onSaved()
        rebuild(uid)
    }

    private suspend fun enter(account: Account) = enter(account.uid.value, account.subscription, false)

    private suspend fun enter(uid: String, subscription: Subscription?, demo: Boolean) {
        ownerJob?.cancel()
        syncJob?.cancel()
        online = false
        if (repository.observeProfile(uid).first() == null) {
            val zone = ZoneId.systemDefault().id.takeIf { it in ZoneId.getAvailableZoneIds() } ?: "Asia/Jakarta"
            repository.createDefaultProfile(uid, zone)
            if (demo) ensureDemoTasks(uid)
        }
        val cloudEnabled = cloud != null && CloudSync.uuid(uid)
        _state.update { it.copy(uid = uid, demo = demo, subscription = subscription, data = null, risks = emptyList(),
            recovery = false, cloudSyncing = false, cloudStatus = if (cloudEnabled) "Menunggu koneksi." else null) }
        focus.activate(uid)
        rebuild(uid)
        ownerJob = viewModelScope.launch {
            launch {
                combine(
                    combine(repository.observeProfile(uid), subtaskRepository?.observe(uid) ?: flowOf(emptyList())) { profile, subtasks ->
                        profile to subtasks
                    },
                    repository.observeTasks(uid),
                    repository.observeFixedSchedules(uid), repository.observeWorkSessions(uid), calendar.observeEvents(uid),
                ) { (profile, subtasks), tasks, fixed, sessions, events ->
                    profile?.let { Workspace(it, tasks, fixed, sessions, events, subtasks) }
                }.collect { snapshot ->
                    _state.update { if (it.uid == uid) it.copy(data = snapshot) else it }
                }
            }
            launch {
                while (true) {
                    val now = System.currentTimeMillis()
                    delay(60_000 - now % 60_000)
                    val refreshed = System.currentTimeMillis()
                    val current = _state.value
                    val zone = current.data?.profile?.let { ZoneId.of(it.zoneId) } ?: continue
                    _state.update { if (it.uid == uid) it.copy(now = refreshed) else it }
                    if (localDate(current.now, zone) != localDate(refreshed, zone)) rebuild(uid)
                }
            }
            if (cloudEnabled) {
                launch {
                    network.collect { connected ->
                        online = connected
                        if (connected) syncNow()
                        else _state.update { if (it.uid == uid) it.copy(cloudStatus = "Offline. Perubahan tersimpan di perangkat.") else it }
                    }
                }
                launch {
                    cloud.queuedChanges(uid).collect { dirty ->
                        if (dirty) {
                            delay(1_500)
                            if (_state.value.uid == uid && online) syncNow()
                        }
                    }
                }
                launch {
                    while (true) {
                        delay(5 * 60_000L)
                        if (_state.value.uid == uid && online) syncNow()
                    }
                }
            }
        }
        consumeFocusLink(uid)
    }

    private suspend fun rebuild(uid: String) {
        val now = System.currentTimeMillis()
        try {
            val plan = repository.rebuildPlan(uid, now)
            _state.update { if (it.uid == uid) it.copy(risks = plan.risks, now = now) else it }
            focus.sync(uid)
        } catch (failure: CancellationException) {
            throw failure
        } catch (_: Exception) {
            showMessage("Jadwal belum dapat dihitung ulang.")
        }
    }

    private suspend fun ensureDemoTasks(uid: String) {
        val tasks = repository.observeTasks(uid).first()
        if (tasks.isNotEmpty()) return
        val now = System.currentTimeMillis()
        val pending = "Makalah sistem operasi"
        val completed = "Presentasi basis data"
        for ((title, course) in listOf(pending to "Sistem Operasi", completed to "Basis Data")) {
            val deadline = now + Duration.ofDays(2).toMillis()
            val result = OfflineAnalyzer.analyze(title, "", deadline, now)
            val task = repository.createTask(uid, title, course, "", deadline, result.difficultyScore, result.estimatedMinutes, now)
            if (title == completed) repository.completeTask(uid, task.id, now)
        }
    }

    private fun runAction(action: suspend () -> Unit) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            try {
                action()
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: IntegrationException) {
                reportIntegrationFailure(failure)
            } catch (_: IllegalArgumentException) {
                showMessage("Periksa isian sebelum menyimpan.")
            } catch (_: Exception) {
                showMessage("Terjadi masalah saat membaca atau menyimpan data.")
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    private companion object {
        const val DEMO_UID = "tazer-demo-local"
    }
}

internal fun Subtask.asDraft() = SubtaskDraft(id, title, completed)

private fun Workspace.paceSamples(): List<PaceSample> {
    val actualByTask = sessions.filter { it.state == WorkSessionState.DONE && it.startedAt != null }
        .groupingBy(WorkSession::taskId)
        .fold(0L) { total, session -> total + (session.actualWorkMs ?: 0L).coerceAtLeast(0L) }
    return tasks.filter { it.status == TaskStatus.DONE }.mapNotNull { task ->
        val actual = actualByTask[task.id] ?: return@mapNotNull null
        PaceSample(task.course, task.analyzerBaselineMinutes ?: task.initialEstimatedMinutes, actual)
    }
}

internal fun localDate(timestamp: Long, zone: ZoneId) = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()

private fun IntegrationException.rejectsSession() =
    code == IntegrationError.INVALID_AUTH || code == IntegrationError.EMAIL_UNVERIFIED
