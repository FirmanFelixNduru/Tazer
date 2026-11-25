package com.tazer.taskorganizer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tazer.taskorganizer.data.Task
import com.tazer.taskorganizer.data.TaskRepository
import com.tazer.taskorganizer.data.TaskStatus
import com.tazer.taskorganizer.domain.OfflineAnalyzer
import java.time.Duration
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface TazerScreen {
    data object Login : TazerScreen
    data object Loading : TazerScreen
    data object ReadError : TazerScreen
    data class Tasks(val items: List<Task>) : TazerScreen
}

class TazerViewModel(private val repository: TaskRepository, val demoAvailable: Boolean) : ViewModel() {
    private val _screen = MutableStateFlow<TazerScreen>(TazerScreen.Login)
    val screen = _screen.asStateFlow()
    private var taskObserver: Job? = null
    private val demoSetup = Mutex()

    fun openDemo() {
        if (!demoAvailable) return
        taskObserver?.cancel()
        _screen.value = TazerScreen.Loading
        taskObserver = viewModelScope.launch {
            flow {
                demoSetup.withLock { ensureDemoTasks() }
                emitAll(repository.observeTasks(DEMO_UID))
            }
                .catch { _screen.value = TazerScreen.ReadError }
                .collect { _screen.value = TazerScreen.Tasks(it) }
        }
    }

    private suspend fun ensureDemoTasks() {
        if (repository.observeProfile(DEMO_UID).first() == null) {
            repository.saveProfile(DEMO_UID, "Asia/Jakarta", 480, 1320)
        }
        val existing = repository.observeTasks(DEMO_UID).first()
        val now = System.currentTimeMillis()
        if (existing.none { it.title == "Makalah sistem operasi" }) {
            createDemoTask("Makalah sistem operasi", "Sistem Operasi", now, Duration.ofDays(2))
        }
        val completed = existing.firstOrNull { it.title == "Presentasi basis data" }
            ?: createDemoTask("Presentasi basis data", "Basis Data", now, Duration.ofDays(1))
        if (completed.status != TaskStatus.DONE) repository.completeTask(DEMO_UID, completed.id, now)
    }

    private suspend fun createDemoTask(title: String, course: String, now: Long, dueIn: Duration): Task {
        val deadline = now + dueIn.toMillis()
        val analysis = OfflineAnalyzer.analyze(title, "", deadline, now)
        return repository.createTask(
            DEMO_UID, title, course, "", deadline, analysis.difficultyScore, analysis.estimatedMinutes, now,
        )
    }

    fun signOut() {
        taskObserver?.cancel()
        taskObserver = null
        _screen.value = TazerScreen.Login
    }

    private companion object {
        const val DEMO_UID = "tazer-demo-local"
    }
}
