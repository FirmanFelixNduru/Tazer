package com.tazer.taskorganizer.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tazer.taskorganizer.data.Task
import com.tazer.taskorganizer.data.WorkSession
import com.tazer.taskorganizer.data.WorkSessionState
import com.tazer.taskorganizer.timer.FocusRuntime
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.delay

internal val ActiveFocusStates = setOf(WorkSessionState.RUNNING, WorkSessionState.OVERTIME, WorkSessionState.EXTENDED)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FocusScreen(
    session: WorkSession,
    task: Task,
    target: FocusTarget,
    zone: ZoneId,
    state: TazerState,
    model: TazerViewModel,
    permissions: () -> Unit,
    defer: () -> Unit,
) {
    val clock by produceState(model.focus.now(), session.id) {
        while (true) {
            value = model.focus.now()
            delay(500)
        }
    }
    val cutoff = session.cutoffElapsedMs
    val validClock = session.timerBootId == clock.bootId && cutoff != null
    val active = session.state in ActiveFocusStates
    val overtime = session.state == WorkSessionState.OVERTIME || (active && validClock && clock.elapsedMs >= requireNotNull(cutoff))
    val displayMs = if (!validClock) {
        session.plannedMinutes * 60_000L
    } else if (overtime) {
        (clock.elapsedMs - requireNotNull(cutoff)).coerceAtLeast(0)
    } else {
        (requireNotNull(cutoff) - clock.elapsedMs).coerceAtLeast(0)
    }
    val canStart = session.state == WorkSessionState.PENDING && clock.wallMs in session.plannedStartAt..<session.plannedEndAt
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
        if (active && (session.state != WorkSessionState.OVERTIME && overtime || !validClock)) model.refreshFocus()
    }
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(task.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        Text(task.course, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            when (session.state) {
                WorkSessionState.PENDING -> "Sesi terjadwal"
                WorkSessionState.DONE -> "Sesi ditutup"
                WorkSessionState.CANCELLED -> "Jadwal sesi telah berubah"
                else -> if (overtime) "Lembur" else "Sedang fokus"
            },
            style = MaterialTheme.typography.titleMedium,
            color = if (overtime) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
        if (active || session.state == WorkSessionState.PENDING) {
            Text(
                (if (overtime) "+" else "") + formatTimer(displayMs, overtime),
                style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"),
            )
        }
        Text(formatDate(session.plannedStartAt, zone) + " – " + formatTime(session.plannedEndAt, zone))
        if (session.state == WorkSessionState.PENDING) {
            Button(onClick = { model.startFocus(session) }, enabled = canStart && !state.busy) { Text("Mulai sesi") }
            if (!canStart) Text("Sesi dapat dimulai pada jam yang tertera. Lihat jadwal terbaru bila waktunya sudah lewat.")
        }
        if (active) {
            if (overtime) {
                Text("Waktu rencana habis. Timer tetap menghitung waktu aktual.")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { model.finishFocus(session) }, enabled = !state.busy) { Text("Sudah Selesai") }
                    OutlinedButton(onClick = { decision = "extra" }, enabled = !state.busy && session.state == WorkSessionState.OVERTIME) {
                        Text("Belum Selesai")
                    }
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
        TextButton(onClick = defer, enabled = !state.busy) { Text("Saya lagi sibuk") }
        TextButton(onClick = permissions) { Text("Izin pengingat") }
    }
    if (decision == "decision" && session.state == WorkSessionState.OVERTIME) {
        AlertDialog(
            onDismissRequest = { decision = null },
            title = { Text("Waktu sesi habis") },
            text = { Text("Apakah tugas ini sudah selesai? Timer masih mencatat waktu lembur.") },
            confirmButton = {
                TextButton(onClick = { decision = null; model.finishFocus(session) }, enabled = !state.busy) {
                    Text("Sudah Selesai")
                }
            },
            dismissButton = {
                TextButton(onClick = { decision = "extra" }, enabled = !state.busy) { Text("Belum Selesai") }
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
            TextButton(onClick = { parsed?.let(submit) }, enabled = parsed != null && !busy) { Text("Susun ulang") }
        },
        dismissButton = { TextButton(onClick = close) { Text("Nanti") } },
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
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = close,
        title = { Text("Kapan bisa fokus lagi?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Progres yang sudah dikerjakan disimpan. Sesi berikutnya disusun di sekitar jadwal tetap.")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        DatePickerDialog(context, { _, y, m, d -> date = LocalDate.of(y, m + 1, d).toString() },
                            day.year, day.monthValue - 1, day.dayOfMonth).show()
                    }, enabled = !busy) { Text(DayFormat.format(day)) }
                    OutlinedButton(onClick = {
                        TimePickerDialog(context, { _, h, m -> minute = h * 60 + m }, minute / 60, minute % 60, true).show()
                    }, enabled = !busy) { Text(formatMinute(minute)) }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { model.deferFocus(validTime, close) },
                enabled = !busy && validTime > model.focus.now().wallMs,
            ) { Text("Susun ulang") }
        },
        dismissButton = { TextButton(onClick = close) { Text("Batal") } },
    )
}

@Composable
internal fun FocusPermissionsDialog(
    focus: FocusRuntime, refresh: Int, launch: (Intent) -> Unit, requestNotification: () -> Unit, close: () -> Unit,
) {
    val context = LocalContext.current
    val notifications = remember(refresh) { focus.notificationsEnabled() }
    val exact = remember(refresh) { focus.canScheduleExactAlarms() }
    val battery = remember(refresh) {
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
    }
    AlertDialog(
        onDismissRequest = close,
        title = { Text("Izin pengingat") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (notifications) "Notifikasi aktif" else "Notifikasi belum aktif")
                if (!notifications) {
                    TextButton(onClick = requestNotification) { Text("Izinkan notifikasi") }
                    TextButton(onClick = {
                        launch(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                    }) { Text("Pengaturan notifikasi") }
                }
                Text(if (exact) "Alarm tepat waktu aktif" else "Alarm tepat waktu belum aktif")
                if (!exact) {
                    Text("Pengingat dapat terlambat. Timer tetap menghitung berdasarkan waktu perangkat.")
                    focus.exactAlarmSettingsIntent()?.let { intent ->
                        TextButton(onClick = { launch(intent) }) { Text("Izinkan alarm tepat waktu") }
                    }
                }
                Text(if (battery) "Pembatasan baterai dikecualikan" else "Pembatasan baterai mengikuti pengaturan perangkat")
                if (!battery) {
                    Text("Saat Doze, Android dapat menunda pengingat. Periksa pengaturan baterai jika pengingat sering terlambat.")
                    TextButton(onClick = { launch(focus.batterySettingsIntent()) }) { Text("Pengaturan baterai") }
                }
            }
        },
        confirmButton = { TextButton(onClick = close) { Text("Tutup") } },
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
