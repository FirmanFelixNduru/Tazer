package com.tazer.taskorganizer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tazer.taskorganizer.data.Task
import com.tazer.taskorganizer.data.TaskStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DayColors = lightColorScheme(
    primary = Color(0xFF2A4AB6),
    primaryContainer = Color(0xFFDCE2FF),
    onPrimaryContainer = Color(0xFF102363),
    surface = Color(0xFFF9FAFF),
    onSurface = Color(0xFF1A1B24),
    onSurfaceVariant = Color(0xFF464959),
)

private val NightColors = darkColorScheme(
    primary = Color(0xFFB5C5FF),
    onPrimary = Color(0xFF152A70),
    primaryContainer = Color(0xFF233B89),
    onPrimaryContainer = Color(0xFFDCE2FF),
    surface = Color(0xFF151622),
    onSurface = Color(0xFFE4E6F1),
    onSurfaceVariant = Color(0xFFC4C7DA),
)

private val DateFormat = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID"))

@Composable
fun TazerApp(model: TazerViewModel) {
    val screen by model.screen.collectAsStateWithLifecycle()
    BackHandler(enabled = screen != TazerScreen.Login, onBack = model::signOut)
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) NightColors else DayColors) {
        when (val current = screen) {
            TazerScreen.Login -> LoginScreen(model.demoAvailable, model::openDemo)
            TazerScreen.Loading -> Scaffold { padding ->
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            TazerScreen.ReadError -> Scaffold { padding ->
                Column(
                    Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("Daftar tugas gagal dibuka.")
                    TextButton(onClick = model::openDemo) { Text("Coba lagi") }
                    TextButton(onClick = model::signOut) { Text("Kembali ke login") }
                }
            }
            is TazerScreen.Tasks -> TaskListing(current.items, model::signOut)
        }
    }
}

@Composable
private fun LoginScreen(demoAvailable: Boolean, openDemo: () -> Unit) {
    Scaffold { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Column(
                Modifier.widthIn(max = 440.dp).fillMaxWidth()
                    .verticalScroll(rememberScrollState()).padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    "Tazer",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { heading() },
                )
                Text("Atur tugas kuliah", style = MaterialTheme.typography.headlineSmall)
                if (demoAvailable) {
                    Text(
                        "Buka daftar tugas dengan akun demo lokal.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Button(onClick = openDemo, modifier = Modifier.fillMaxWidth()) {
                        Text("Masuk sebagai demo")
                    }
                } else {
                    Text("Login akun belum tersedia pada build ini.")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskListing(tasks: List<Task>, signOut: () -> Unit) {
    var activeTab by rememberSaveable { mutableIntStateOf(0) }
    val completed = activeTab == 1
    val visibleTasks = if (completed) {
        tasks.filter { it.status == TaskStatus.DONE }
            .sortedWith(compareByDescending<Task> { it.completedAt }.thenBy { it.id })
    } else {
        tasks.filter { it.status != TaskStatus.DONE }
            .sortedWith(compareBy<Task> { it.deadlineAt }.thenBy { it.id })
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Tugas") }, actions = {
            TextButton(onClick = signOut) { Text("Keluar") }
        })
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            TaskTabs(activeTab, { activeTab = it }, visibleTasks)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskTabs(activeTab: Int, selectTab: (Int) -> Unit, visibleTasks: List<Task>) {
    val completed = activeTab == 1
    Column(Modifier.widthIn(max = 680.dp).fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = activeTab) {
            Tab(selected = !completed, onClick = { selectTab(0) }, text = { Text("Belum Selesai") })
            Tab(selected = completed, onClick = { selectTab(1) }, text = { Text("Selesai") })
        }
        if (visibleTasks.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    if (completed) "Belum ada tugas selesai." else "Belum ada tugas aktif.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 24.dp)) {
                items(visibleTasks, key = { it.id }) { task ->
                    TaskRow(task, completed)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun TaskRow(task: Task, completed: Boolean) {
    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(task.title, style = MaterialTheme.typography.titleMedium)
        Text(task.course, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Kesulitan ${task.difficultyScore}/5 · Estimasi awal ${task.initialEstimatedMinutes} mnt")
        if (completed) {
            task.completedAt?.let { Text("Selesai ${formatDate(it)}") }
        } else {
            Text("Sisa ${task.remainingMinutes} mnt · Deadline ${formatDate(task.deadlineAt)}")
        }
    }
}

private fun formatDate(timestamp: Long): String =
    DateFormat.format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()))
