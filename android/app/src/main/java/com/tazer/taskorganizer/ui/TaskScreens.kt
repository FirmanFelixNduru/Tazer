package com.tazer.taskorganizer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tazer.taskorganizer.data.AnalysisSource
import com.tazer.taskorganizer.data.Task
import com.tazer.taskorganizer.data.TaskStatus
import com.tazer.taskorganizer.data.WorkSessionState
import com.tazer.taskorganizer.domain.Scheduler
import com.tazer.taskorganizer.domain.SchedulingTask
import com.tazer.taskorganizer.integration.IntegrationException
import com.tazer.taskorganizer.integration.SubscriptionTier
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

internal val Indonesian = Locale.forLanguageTag("id-ID")
internal val DayFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Indonesian)
internal val ClockFormat = DateTimeFormatter.ofPattern("HH:mm", Indonesian)
private val FullDateFormat = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Indonesian)

internal fun formatDate(timestamp: Long, zone: ZoneId): String =
    FullDateFormat.format(Instant.ofEpochMilli(timestamp).atZone(zone))

internal fun formatTime(timestamp: Long, zone: ZoneId): String =
    ClockFormat.format(Instant.ofEpochMilli(timestamp).atZone(zone))

internal fun formatMinute(minute: Int): String =
    if (minute == 1440) "24:00" else ClockFormat.format(LocalTime.of(minute / 60, minute % 60))

internal fun parseMinute(value: String): Int? {
    if (value.trim() == "24:00") return 1440
    if (!Regex("""[0-9]{1,2}:[0-9]{2}""").matches(value.trim())) return null
    val (h, m) = value.trim().split(':').map(String::toInt)
    return if (h in 0..23 && m in 0..59) h * 60 + m else null
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun TasksScreen(
    workspace: Workspace,
    state: TazerState,
    zone: ZoneId,
    archived: Boolean,
    onTab: (Boolean) -> Unit,
    model: TazerViewModel,
    onEdit: (Task) -> Unit,
) {
    val active = workspace.tasks.filter { it.status != TaskStatus.DONE }
    val ranked = remember(active, state.now) {
        val byId = active.associateBy { it.id }
        Scheduler.rank(active.map(Task::schedulingTask), state.now).map { byId.getValue(it.id) }
    }
    val tasks = if (archived) {
        workspace.tasks.filter { it.status == TaskStatus.DONE }
            .sortedWith(compareByDescending<Task> { it.completedAt }.thenBy { it.id })
    } else ranked
    val taskSessions = remember(workspace.sessions) { workspace.sessions.groupBy { it.taskId } }
    val risks = remember(state.risks) { state.risks.associateBy { it.taskId } }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var reopenId by rememberSaveable { mutableStateOf<String?>(null) }
    val taskToDelete = tasks.firstOrNull { it.id == deleteId }
    val taskToReopen = tasks.firstOrNull { it.id == reopenId }
    PrimaryTabRow(selectedTabIndex = if (archived) 1 else 0) {
        Tab(selected = !archived, onClick = { onTab(false) }, text = { Text("Belum Selesai") })
        Tab(selected = archived, onClick = { onTab(true) }, text = { Text("Selesai") })
    }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (tasks.isEmpty()) item {
            Text(
                if (archived) "Belum ada tugas selesai." else "Belum ada tugas aktif. Ketuk Tambah tugas untuk memulai.",
                modifier = Modifier.padding(vertical = 24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(tasks, key = { it.id }) { task ->
            val sessions = taskSessions[task.id].orEmpty()
            Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(task.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                Text(task.course, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (archived) {
                    val actual = sessions.filter { it.state == WorkSessionState.DONE && it.startedAt != null }
                    val actualMinutes = (actual.sumOf { it.actualWorkMs ?: 0L } + 30_000L) / 60_000L
                    Text("Estimasi awal " + task.initialEstimatedMinutes + " mnt · Aktual " + actualMinutes + " mnt")
                    Text(actual.size.toString() + " sesi")
                    task.completedAt?.let { Text("Selesai " + formatDate(it, zone)) }
                } else {
                    if (task.deadlineAt <= state.now) {
                        Text("Lewat deadline", color = MaterialTheme.colorScheme.error)
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        TaskTag("Prioritas " + Scheduler.priorityScore(task.schedulingTask(), state.now), task.deadlineAt <= state.now)
                        TaskTag("Kesulitan " + task.difficultyScore + "/5", false)
                    }
                    Text("Estimasi awal " + task.initialEstimatedMinutes + " mnt · Sisa " + task.remainingMinutes + " mnt")
                    Text("Deadline " + formatDate(task.deadlineAt, zone))
                    val next = sessions.asSequence().filter {
                        it.state == WorkSessionState.PENDING && it.plannedEndAt > state.now
                    }.minByOrNull { it.plannedStartAt }
                    next?.let { Text("Sesi berikutnya " + formatDate(it.plannedStartAt, zone)) }
                    risks[task.id]?.let {
                        Text("Kurang " + it.missingMinutes + " mnt dalam rencana", color = MaterialTheme.colorScheme.error)
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)) {
                    if (archived) {
                        OutlinedButton(onClick = { reopenId = task.id }, enabled = !state.busy) { Text("Buka kembali") }
                    } else {
                        TaskAction(TazerGlyph.Edit, "Edit tugas ${task.title}", MaterialTheme.colorScheme.onSurfaceVariant,
                            !state.busy) { onEdit(task) }
                        val success = if (LocalTazerDark.current) Color(0xFF84D39A) else Color(0xFF146634)
                        TaskAction(TazerGlyph.Check, "Tandai selesai ${task.title}", success, !state.busy) { model.finishTask(task) }
                    }
                    TaskAction(TazerGlyph.Delete, "Hapus tugas ${task.title}", MaterialTheme.colorScheme.error,
                        !state.busy) { deleteId = task.id }
                }
            }
            }
        }
    }
    if (taskToDelete != null) AlertDialog(
        onDismissRequest = { deleteId = null },
        title = { Text("Hapus tugas?") },
        text = { Text("“" + taskToDelete.title + "” beserta riwayat sesinya akan dihapus.") },
        confirmButton = { Button(onClick = { model.deleteTask(taskToDelete); deleteId = null }) { Text("Hapus") } },
        dismissButton = { OutlinedButton(onClick = { deleteId = null }) { Text("Batal") } },
    )
    if (taskToReopen != null) key(taskToReopen.id) {
        var minutes by rememberSaveable { mutableStateOf(taskToReopen.initialEstimatedMinutes.toString()) }
        val parsed = minutes.toIntOrNull()?.takeIf { it in 1..10080 }
        AlertDialog(
            onDismissRequest = { reopenId = null },
            title = { Text("Buka kembali tugas") },
            text = {
                OutlinedTextField(
                    minutes, { minutes = it }, label = { Text("Sisa menit") },
                    keyboardOptions = NumberKeyboard, singleLine = true,
                )
            },
            confirmButton = {
                OutlinedButton(onClick = { parsed?.let { model.reopenTask(taskToReopen, it) }; reopenId = null }, enabled = parsed != null) {
                    Text("Buka kembali")
                }
            },
            dismissButton = { OutlinedButton(onClick = { reopenId = null }) { Text("Batal") } },
        )
    }
}

@Composable
private fun TaskAction(glyph: TazerGlyph, label: String, tint: Color, enabled: Boolean, onClick: () -> Unit) {
    OutlinedIconButton(
        modifier = Modifier.size(48.dp),
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.outlinedIconButtonColors(contentColor = tint),
    ) { TazerIcon(glyph, label) }
}

@Composable
private fun TaskTag(label: String, overdue: Boolean) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (overdue) colors.errorContainer else colors.secondaryContainer,
        contentColor = if (overdue) colors.onErrorContainer else colors.onSecondaryContainer,
    ) {
        Text(label, Modifier.padding(horizontal = 9.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TaskEditor(task: Task?, zone: ZoneId, tier: SubscriptionTier?, busy: Boolean, model: TazerViewModel, close: () -> Unit) {
    val base = remember(task?.id, zone) {
        (task?.deadlineAt?.let(Instant::ofEpochMilli) ?: Instant.now().plusSeconds(86_400)).atZone(zone)
    }
    var title by rememberSaveable(task?.id) { mutableStateOf(task?.title.orEmpty()) }
    var course by rememberSaveable(task?.id) { mutableStateOf(task?.course.orEmpty()) }
    var description by rememberSaveable(task?.id) { mutableStateOf(task?.description.orEmpty()) }
    var date by rememberSaveable(task?.id) { mutableStateOf(base.toLocalDate().toString()) }
    var minute by rememberSaveable(task?.id) { mutableIntStateOf(base.hour * 60 + base.minute) }
    var difficulty by rememberSaveable(task?.id) { mutableStateOf(task?.difficultyScore?.toString().orEmpty()) }
    var minutes by rememberSaveable(task?.id) { mutableStateOf(task?.remainingMinutes?.toString().orEmpty()) }
    var source by rememberSaveable(task?.id) { mutableStateOf(task?.analysisSource?.name ?: AnalysisSource.OFFLINE.name) }
    var analysisMode by remember(task?.id) { mutableStateOf<AnalyzerMode?>(null) }
    val scope = rememberCoroutineScope()
    val day = LocalDate.parse(date)
    val unchangedDeadline = task != null && day == base.toLocalDate() && minute == base.hour * 60 + base.minute
    val deadline = if (unchangedDeadline) requireNotNull(task).deadlineAt
        else day.atTime(minute / 60, minute % 60).atZone(zone).toInstant().toEpochMilli()
    val difficultyValue = difficulty.toIntOrNull()?.takeIf { it in 1..5 }
    val minutesValue = minutes.toIntOrNull()?.takeIf { it in 1..10080 }
    val editable = analysisMode == null && !busy
    val validText = title.trim().length in 1..120 && course.trim().length in 1..120 && description.length <= 4000
    val analyze: (AnalyzerMode) -> Unit = { mode ->
        analysisMode = mode
        scope.launch {
            try {
                val result = model.analyze(mode, title, course, description, deadline)
                difficulty = result.difficulty.toString()
                minutes = result.minutes.toString()
                source = result.source.name
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: IntegrationException) {
                model.reportIntegrationFailure(failure)
            } catch (failure: Exception) {
                model.showMessage(failure.message ?: "Analisis AI tidak tersedia. Gunakan analisis offline.")
            } finally {
                analysisMode = null
            }
        }
    }
    Column(
        Modifier.verticalScroll(rememberScrollState()).imePadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(title, { title = it }, label = { Text("Judul") }, singleLine = true, enabled = editable, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(course, { course = it }, label = { Text("Mata kuliah") }, singleLine = true, enabled = editable, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(description, { description = it }, label = { Text("Deskripsi") }, minLines = 3, enabled = editable, modifier = Modifier.fillMaxWidth())
        Text("Deadline", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PickerDateButton(day, editable) { date = it.toString() }
            PickerTimeButton(minute, editable) { minute = it }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { analyze(AnalyzerMode.OFFLINE) }, enabled = editable && title.isNotBlank()) {
                Text("Analisis offline")
            }
            if (tier == SubscriptionTier.PRO || tier == SubscriptionTier.BYOK) {
                OutlinedButton(
                    onClick = { analyze(if (tier == SubscriptionTier.PRO) AnalyzerMode.CLOUD else AnalyzerMode.BYOK) },
                    enabled = editable && validText,
                ) { Text(if (tier == SubscriptionTier.PRO) "Analisis AI" else "Analisis dengan BYOK") }
            }
        }
        if (tier == SubscriptionTier.PRO) {
            Text("Analisis AI mengirim isi tugas ke provider. Analisis offline tetap tersedia.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        if (analysisMode != null) Text("Menganalisis tugas…")
        OutlinedTextField(
            difficulty, { difficulty = it }, label = { Text("Kesulitan (1–5)") }, singleLine = true,
            keyboardOptions = NumberKeyboard, enabled = editable, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            minutes, { minutes = it }, label = { Text(if (task == null) "Estimasi (menit)" else "Sisa kerja (menit)") },
            singleLine = true, keyboardOptions = NumberKeyboard, enabled = editable, modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                val score = difficultyValue ?: return@Button
                val duration = minutesValue ?: return@Button
                model.saveTask(task, TaskInput(title, course, description, deadline, score, duration, AnalysisSource.valueOf(source)), close)
            },
            enabled = editable && validText && difficultyValue != null && minutesValue != null,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Simpan tugas") }
    }
}

private fun Task.schedulingTask() = SchedulingTask(id, deadlineAt, createdAt, remainingMinutes, difficultyScore)
