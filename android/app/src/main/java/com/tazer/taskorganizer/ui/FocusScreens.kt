package com.tazer.taskorganizer.ui

import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tazer.taskorganizer.data.Subtask
import com.tazer.taskorganizer.data.Task
import com.tazer.taskorganizer.data.TaskStatus
import com.tazer.taskorganizer.data.WorkSession
import com.tazer.taskorganizer.data.WorkSessionState
import com.tazer.taskorganizer.domain.FocusMoment
import com.tazer.taskorganizer.domain.FocusPreset
import com.tazer.taskorganizer.domain.FocusPresetMode
import com.tazer.taskorganizer.timer.FocusRuntime
import com.tazer.taskorganizer.timer.FocusSettings
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val TickingFocusStates = setOf(WorkSessionState.RUNNING, WorkSessionState.OVERTIME, WorkSessionState.EXTENDED)
internal val ActiveFocusStates = TickingFocusStates + WorkSessionState.PAUSED

@Composable
internal fun focusClock(runtime: FocusRuntime, session: WorkSession): FocusMoment = key(session.id, session.revision) {
    val clock by produceState(runtime.now()) {
        while (session.state != WorkSessionState.PAUSED) {
            delay(250)
            value = runtime.now()
        }
    }
    clock
}

internal fun WorkSession.isOvertime(clock: FocusMoment): Boolean =
    state == WorkSessionState.OVERTIME ||
        (state == WorkSessionState.PAUSED && pausedRemainingMs?.let { it <= 0 } == true) ||
        (state in TickingFocusStates && timerBootId == clock.bootId && cutoffElapsedMs?.let { clock.elapsedMs >= it } == true)

internal fun WorkSession.timerText(clock: FocusMoment, overtime: Boolean): String {
    val cutoff = cutoffElapsedMs
    val duration = when {
        state == WorkSessionState.PAUSED -> kotlin.math.abs(pausedRemainingMs ?: 0L)
        timerBootId != clock.bootId || cutoff == null -> if (overtime) 0L else plannedMinutes * 60_000L
        overtime -> (clock.elapsedMs - cutoff).coerceAtLeast(0)
        else -> (cutoff - clock.elapsedMs).coerceAtLeast(0)
    }
    return (if (overtime) "+" else "") + formatTimer(duration, overtime)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FocusScreen(
    session: WorkSession,
    task: Task,
    target: FocusTarget,
    zone: ZoneId,
    state: TazerState,
    model: TazerViewModel,
    defer: () -> Unit,
) {
    val clock = focusClock(model.focus, session)
    val validClock = session.timerBootId == clock.bootId && session.cutoffElapsedMs != null
    val active = session.state in ActiveFocusStates
    val overtime = session.isOvertime(clock)
    val canStart = session.state == WorkSessionState.PENDING && clock.wallMs in session.plannedStartAt..<session.plannedEndAt
    val subtasks = state.data?.subtasks
    val checklist = remember(task.id, subtasks) {
        subtasks.orEmpty().filter { it.taskId == task.id }.sortedBy(Subtask::position).map(Subtask::checklistItem)
    }
    var subtaskEditKey by rememberSaveable(task.id) { mutableStateOf<String?>(null) }
    var decision by rememberSaveable(session.id, session.revision, target.requestExtension) {
        mutableStateOf(
            when {
                target.requestExtension -> "extra"
                session.state == WorkSessionState.OVERTIME -> "decision"
                else -> null
            },
        )
    }
    LaunchedEffect(session.id, session.revision, overtime, validClock) {
        if (session.state in TickingFocusStates && (session.state != WorkSessionState.OVERTIME && overtime || !validClock)) {
            model.refreshFocus()
        }
    }
    Column(
        Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(task.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        Text(task.course, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FocusRhythm(model.focus.focusSettings().preset)
        val status = when (session.state) {
                WorkSessionState.PENDING -> "Sesi terjadwal"
                WorkSessionState.PAUSED -> "Dijeda"
                WorkSessionState.DONE -> "Sesi ditutup"
                WorkSessionState.CANCELLED -> "Jadwal sesi telah berubah"
                else -> if (overtime) "Lembur" else "Sedang fokus"
        }
        val colors = MaterialTheme.colorScheme
        if (active || session.state == WorkSessionState.PENDING) {
            val alert = session.state == WorkSessionState.PAUSED || overtime
            NeuCard(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp), radius = 26.dp,
                fill = if (alert) colors.tertiaryContainer else null) {
                CompositionLocalProvider(LocalContentColor provides if (alert) colors.onTertiaryContainer else colors.onSurface) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TazerIcon(TazerGlyph.Timer)
                        Text(status, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(session.timerText(clock, overtime),
                        style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"))
                }
                }
            }
        } else Text(status, style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant)
        Text(formatDate(session.plannedStartAt, zone) + " – " + formatTime(session.plannedEndAt, zone))
        if (session.state == WorkSessionState.PENDING) {
            Button(onClick = { model.startFocus(session) }, enabled = canStart && !state.busy) { Text("Mulai sesi") }
            if (!canStart) Text("Sesi dapat dimulai pada jam yang tertera. Lihat jadwal terbaru bila waktunya sudah lewat.")
        }
        if (active) {
            if (session.state == WorkSessionState.PAUSED) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { model.resumeFocus(session) }, enabled = !state.busy) { Text("Lanjutkan") }
                    OutlinedButton(onClick = { model.finishFocus(session) }, enabled = !state.busy) { Text("Sudah Selesai") }
                    OutlinedButton(onClick = { model.stopFocus(session) }, enabled = !state.busy) { Text("Hentikan sesi") }
                }
            } else if (overtime) {
                Text("Waktu rencana habis. Timer tetap menghitung waktu aktual.")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { model.finishFocus(session) }, enabled = !state.busy) { Text("Sudah Selesai") }
                    OutlinedButton(onClick = { decision = "extra" }, enabled = !state.busy && session.state == WorkSessionState.OVERTIME) {
                        Text("Belum Selesai")
                    }
                    OutlinedButton(onClick = { model.pauseFocus(session) }, enabled = !state.busy) { Text("Jeda") }
                }
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { model.finishFocus(session) }, enabled = !state.busy) { Text("Selesai lebih cepat") }
                    OutlinedButton(onClick = { model.pauseFocus(session) }, enabled = !state.busy) { Text("Jeda") }
                }
                Text("Jeda menyimpan progres dan menjadwalkan sisa kerja.", style = MaterialTheme.typography.bodySmall)
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        if (model.subtasksAvailable && task.status != TaskStatus.DONE) {
            key(task.id) {
                SubtaskEditor(
                    items = checklist,
                    enabled = !state.busy,
                    activeKey = subtaskEditKey?.takeIf { id -> id.isEmpty() || checklist.any { it.key == id } },
                    onEditKey = { subtaskEditKey = it },
                    onChecked = { item, completed -> model.setSubtaskCompleted(task.id, item.key, completed) },
                    onSave = { item, title, onSaved -> model.saveSubtask(task.id, item?.key, title, onSaved) },
                    onDelete = { item -> model.deleteSubtask(task.id, item.key) },
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
        }
        OutlinedButton(onClick = defer, enabled = !state.busy) { Text("Saya lagi sibuk") }
    }
    if (decision == "decision" && session.state == WorkSessionState.OVERTIME) {
        AlertDialog(
            onDismissRequest = { decision = null },
            title = { Text("Waktu sesi habis") },
            text = { Text("Apakah tugas ini sudah selesai? Timer masih mencatat waktu lembur.") },
            confirmButton = {
                OutlinedButton(onClick = { decision = null; model.finishFocus(session) }, enabled = !state.busy) {
                    Text("Sudah Selesai")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { decision = "extra" }, enabled = !state.busy) { Text("Belum Selesai") }
            },
        )
    }
    if (decision == "extra" && session.state == WorkSessionState.OVERTIME) key(session.id, session.revision) {
        ExtensionDialog(state.busy, { decision = null }) { minutes ->
            decision = null
            model.extendFocus(session, minutes)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExtensionDialog(busy: Boolean, close: () -> Unit, submit: (Int) -> Unit) {
    var minutes by rememberSaveable { mutableStateOf("") }
    val parsed = minutes.toIntOrNull()?.takeIf { it in 1..10080 }
    AlertDialog(
        onDismissRequest = close,
        title = { Text("Tambahan waktu") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).imePadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Masukkan perkiraan sisa kerja mulai sekarang.")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(15 to "+15 mnt", 30 to "+30 mnt", 60 to "+1 jam").forEach { (amount, label) ->
                        OutlinedButton(onClick = { minutes = amount.toString() }, enabled = !busy) { Text(label) }
                    }
                }
                OutlinedTextField(minutes, { minutes = it }, label = { Text("Sisa menit") },
                    singleLine = true, enabled = !busy, keyboardOptions = NumberKeyboard, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            OutlinedButton(onClick = { parsed?.let(submit) }, enabled = parsed != null && !busy) { Text("Susun ulang") }
        },
        dismissButton = { OutlinedButton(onClick = close) { Text("Nanti") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DeferFocusDialog(zone: ZoneId, busy: Boolean, model: TazerViewModel, close: () -> Unit) {
    val initial = remember(zone) { Instant.ofEpochMilli(model.focus.now().wallMs).atZone(zone).plusHours(1) }
    var date by rememberSaveable { mutableStateOf(initial.toLocalDate().toString()) }
    var minute by rememberSaveable { mutableIntStateOf(initial.hour * 60 + initial.minute) }
    val day = LocalDate.parse(date)
    val validTime = day.atTime(minute / 60, minute % 60).atZone(zone).toInstant().toEpochMilli()
    AlertDialog(
        onDismissRequest = close,
        title = { Text("Kapan bisa fokus lagi?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Progres yang sudah dikerjakan disimpan. Sesi berikutnya disusun di sekitar jadwal tetap.")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PickerDateButton(day, !busy) { date = it.toString() }
                    PickerTimeButton(minute, !busy) { minute = it }
                }
            }
        },
        confirmButton = {
            OutlinedButton(
                onClick = { model.deferFocus(validTime, close) },
                enabled = !busy && validTime > model.focus.now().wallMs,
            ) { Text("Susun ulang") }
        },
        dismissButton = { OutlinedButton(onClick = close) { Text("Batal") } },
    )
}

@Composable
internal fun FocusPermissionsDialog(
    focus: FocusRuntime,
    refresh: Int,
    launch: (Intent) -> Unit,
    requestNotification: () -> Unit,
    onPresetSaved: () -> Unit = {},
    close: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val initial = remember(focus) { focus.focusSettings() }
    var mode by rememberSaveable { mutableStateOf(initial.preset.mode) }
    var work by rememberSaveable { mutableStateOf(initial.preset.customWorkMinutes.toString()) }
    var rest by rememberSaveable { mutableStateOf(initial.preset.customBreakMinutes.toString()) }
    var dnd by rememberSaveable { mutableStateOf(initial.dndEnabled) }
    var saving by remember { mutableStateOf(false) }
    var failed by rememberSaveable { mutableStateOf(false) }
    val workMinutes = work.toIntOrNull()?.takeIf { it in FocusPreset.WORK_RANGE }
    val breakMinutes = rest.toIntOrNull()?.takeIf { it in FocusPreset.BREAK_RANGE }
    val valid = mode != FocusPresetMode.CUSTOM || workMinutes != null && breakMinutes != null
    val preset = FocusPreset(mode, workMinutes ?: initial.preset.customWorkMinutes,
        breakMinutes ?: initial.preset.customBreakMinutes)
    val dirty = !valid || preset != initial.preset || dnd != initial.dndEnabled
    val dndAccess = remember(refresh) { focus.hasDndAccess() }
    val notifications = remember(refresh) { focus.notificationsEnabled() }
    val exact = remember(refresh) { focus.canScheduleExactAlarms() }
    val battery = remember(refresh) {
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
    }
    AlertDialog(
        onDismissRequest = { if (!saving) close() },
        title = { Text("Pengaturan fokus") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).imePadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Durasi fokus dan jeda", Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleSmall)
                Column(Modifier.selectableGroup()) {
                    for (option in FocusPresetMode.entries) {
                        val label = when (option) {
                            FocusPresetMode.DEEP_WORK -> "Deep Work · 50 mnt fokus, 10 mnt jeda"
                            FocusPresetMode.POMODORO -> "Pomodoro · 25 mnt fokus, 5 mnt jeda"
                            FocusPresetMode.CUSTOM -> "Custom"
                        }
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(
                                selected = mode == option, enabled = !saving, role = Role.RadioButton,
                                onClick = { mode = option },
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            RadioButton(selected = mode == option, enabled = !saving, onClick = null)
                            Text(label)
                        }
                    }
                }
                if (mode == FocusPresetMode.CUSTOM) {
                    OutlinedTextField(work, { work = it }, Modifier.fillMaxWidth(), enabled = !saving,
                        label = { Text("Fokus (menit)") }, supportingText = { Text("15–120 menit") },
                        isError = workMinutes == null, singleLine = true, keyboardOptions = NumberKeyboard)
                    OutlinedTextField(rest, { rest = it }, Modifier.fillMaxWidth(), enabled = !saving,
                        label = { Text("Jeda (menit)") }, supportingText = { Text("1–30 menit") },
                        isError = breakMinutes == null, singleLine = true, keyboardOptions = NumberKeyboard)
                }
                Text("Durasi ini berlaku untuk blok baru. Hitung mundur yang sudah dimulai tetap berjalan.")
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                if (focus.dndSupported()) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(
                            value = dnd, enabled = !saving, role = Role.Switch, onValueChange = { dnd = it },
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Jangan Ganggu saat fokus", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                        Switch(checked = dnd, onCheckedChange = null, enabled = !saving)
                    }
                    Text("Saat Anda mulai atau melanjutkan timer, Tazer membisukan notifikasi dan tetap mengizinkan alarm. " +
                        "Anda mengatur mode dan aksesnya di Android.")
                    if (dnd && !dndAccess) {
                        Text("Tazer memerlukan akses Jangan Ganggu.")
                        OutlinedButton(onClick = { launch(focus.dndSettingsIntent()) }) { Text("Buka akses Jangan Ganggu") }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                }
                Text("Izin pengingat", Modifier.semantics { heading() }, style = MaterialTheme.typography.titleSmall)
                Text(if (notifications) "Notifikasi aktif" else "Notifikasi belum aktif")
                if (!notifications) {
                    OutlinedButton(onClick = requestNotification) { Text("Izinkan notifikasi") }
                    OutlinedButton(onClick = {
                        launch(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                    }) { Text("Pengaturan notifikasi") }
                }
                Text(if (exact) "Alarm tepat waktu aktif" else "Alarm tepat waktu belum aktif")
                if (!exact) {
                    Text("Pengingat dapat terlambat. Timer tetap menghitung berdasarkan waktu perangkat.")
                    focus.exactAlarmSettingsIntent()?.let { intent ->
                        OutlinedButton(onClick = { launch(intent) }) { Text("Izinkan alarm tepat waktu") }
                    }
                }
                Text(if (battery) "Pembatasan baterai dikecualikan" else "Pembatasan baterai mengikuti pengaturan perangkat")
                if (!battery) {
                    Text("Saat Doze, Android dapat menunda pengingat. Periksa pengaturan baterai jika pengingat sering terlambat.")
                    OutlinedButton(onClick = { launch(focus.batterySettingsIntent()) }) { Text("Pengaturan baterai") }
                }
                if (failed) Text("Pengaturan belum tersimpan. Coba simpan lagi.", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { if (dirty) TextButton(onClick = close, enabled = !saving) { Text("Batal") } },
        confirmButton = {
            Button(enabled = valid && !saving, onClick = {
                if (!dirty) {
                    close()
                } else {
                    saving = true
                    failed = false
                    scope.launch {
                        if (focus.saveFocusSettings(FocusSettings(preset, dnd))) {
                            if (preset != initial.preset) onPresetSaved()
                            close()
                        } else {
                            failed = true
                            saving = false
                        }
                    }
                }
            }) { Text(if (dirty) "Simpan" else "Tutup") }
        },
    )
}

@Composable
internal fun FocusRhythm(preset: FocusPreset, modifier: Modifier = Modifier) {
    val name = when (preset.mode) {
        FocusPresetMode.POMODORO -> "Pomodoro"
        FocusPresetMode.DEEP_WORK -> "Deep Work"
        FocusPresetMode.CUSTOM -> "Custom"
    }
    Text(
        "$name · ${preset.workMinutes} mnt fokus · ${preset.breakMinutes} mnt jeda",
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
    )
}

private fun formatTimer(ms: Long, overtime: Boolean): String {
    val seconds = (ms + if (overtime) 0L else 999L) / 1000L
    return if (seconds >= 3600) {
        String.format(Locale.ROOT, "%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
    } else {
        String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60)
    }
}
