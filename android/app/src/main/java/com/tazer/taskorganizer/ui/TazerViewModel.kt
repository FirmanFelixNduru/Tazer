package com.tazer.taskorganizer.ui

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tazer.taskorganizer.data.AnalysisSource
import com.tazer.taskorganizer.data.CalendarEvent
import com.tazer.taskorganizer.data.CalendarRepository
import com.tazer.taskorganizer.data.FixedSchedule
import com.tazer.taskorganizer.data.FixedScheduleKind
import com.tazer.taskorganizer.data.FocusChange
import com.tazer.taskorganizer.data.Task
import com.tazer.taskorganizer.data.TaskRepository
import com.tazer.taskorganizer.data.UserProfile
import com.tazer.taskorganizer.data.WorkSession
import com.tazer.taskorganizer.data.WorkSessionState
import com.tazer.taskorganizer.domain.DeadlineRisk
import com.tazer.taskorganizer.domain.OfflineAnalyzer
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
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class Workspace(
    val profile: UserProfile,
    val tasks: List<Task>,
    val fixed: List<FixedSchedule>,
    val sessions: List<WorkSession>,
    val events: List<CalendarEvent>,
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
    val busy: Boolean = false,
    val message: String? = null,
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
)

data class AnalysisSuggestion(val difficulty: Int, val minutes: Int, val source: AnalysisSource)

enum class AnalyzerMode { OFFLINE, CLOUD, BYOK }

class TazerViewModel(
    private val repository: TaskRepository,
    private val calendar: CalendarRepository,
    private val integration: AndroidIntegration,
    val focus: FocusRuntime,
    val demoAvailable: Boolean,
) : ViewModel() {
    private val _state = MutableStateFlow(TazerState())
    val state = _state.asStateFlow()
    val authAvailable get() = integration.enabled
    val mockAvailable get() = integration.mockEnabled

    private var ownerJob: Job? = null
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
    fun startGoogle(launch: (Intent) -> Unit) = runAction { launch(integration.googleSignInIntent()) }

    fun handleAuthCallback(uri: Uri?) {
        if (uri != null && integration.isAuthCallback(uri)) runAction {
            when (val callback = integration.handleAuthCallback(uri)) {
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
            val account = integration.restoreAccount() ?: return@runAction
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

    fun pauseFocus(session: WorkSession) = focusAction("Sesi tidak dapat dijeda.", closeWhenDone = true) { uid ->
        focus.pause(uid, session.id, session.revision)
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

    private suspend fun refreshSubscription() {
        val uid = _state.value.uid ?: return
        if (_state.value.demo) return
        val subscription = integration.subscription()
        _state.update { if (it.uid == uid) it.copy(subscription = subscription) else it }
    }

    fun saveTask(task: Task?, input: TaskInput, onSaved: () -> Unit) = change(onSaved) { uid ->
        if (task == null) {
            repository.createTask(
                uid, input.title, input.course, input.description, input.deadlineAt, input.difficulty,
                input.minutes, System.currentTimeMillis(), input.source,
            )
            true
        } else {
            repository.updateTask(
                uid, task.id, input.title, input.course, input.description, input.deadlineAt,
                input.difficulty, input.minutes, input.source,
            )
        }
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
        if (mode == AnalyzerMode.OFFLINE) {
            val result = OfflineAnalyzer.analyze(title, description, deadlineAt, System.currentTimeMillis())
            return AnalysisSuggestion(result.difficultyScore, result.estimatedMinutes, AnalysisSource.OFFLINE)
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
        return AnalysisSuggestion(result.difficultyScore, result.estimatedMinutes, AnalysisSource.AI)
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
        if (repository.observeProfile(uid).first() == null) {
            val zone = ZoneId.systemDefault().id.takeIf { it in ZoneId.getAvailableZoneIds() } ?: "Asia/Jakarta"
            repository.saveProfile(uid, zone, 480, 1320)
            if (demo) ensureDemoTasks(uid)
        }
        _state.update { it.copy(uid = uid, demo = demo, subscription = subscription, data = null, risks = emptyList(), recovery = false) }
        ownerJob = viewModelScope.launch {
            launch {
                combine(
                    repository.observeProfile(uid), repository.observeTasks(uid),
                    repository.observeFixedSchedules(uid), repository.observeWorkSessions(uid), calendar.observeEvents(uid),
                ) { profile, tasks, fixed, sessions, events ->
                    profile?.let { Workspace(it, tasks, fixed, sessions, events) }
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
        }
        focus.activate(uid)
        rebuild(uid)
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

internal fun localDate(timestamp: Long, zone: ZoneId) = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()

private fun IntegrationException.rejectsSession() =
    code == IntegrationError.INVALID_AUTH || code == IntegrationError.EMAIL_UNVERIFIED
