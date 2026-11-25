package com.tazer.taskorganizer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tazer.taskorganizer.data.FixedSchedule
import com.tazer.taskorganizer.data.FixedScheduleKind
import com.tazer.taskorganizer.data.UserProfile
import com.tazer.taskorganizer.data.WorkSession
import com.tazer.taskorganizer.data.WorkSessionState
import com.tazer.taskorganizer.domain.ScheduleRule
import com.tazer.taskorganizer.domain.Scheduler
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle

private data class TimelineRow(
    val id: String, val title: String, val kind: String, val start: Long, val end: Long,
    val session: WorkSession? = null,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TimelineScreen(
    workspace: Workspace, state: TazerState, zone: ZoneId, day: LocalDate, selectDay: (LocalDate) -> Unit,
    openFocus: (WorkSession) -> Unit, defer: () -> Unit, permissions: () -> Unit,
) {
    val today = localDate(state.now, zone)
    val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
    val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val rows = remember(workspace, day, zone) {
        val fixedById = workspace.fixed.associateBy { it.id }
        val tasksById = workspace.tasks.associateBy { it.id }
        val rules = workspace.fixed.map { ScheduleRule(it.id, it.label, it.dayOfWeek, it.startMinute, it.endMinute) }
        val fixedRows = Scheduler.fixedBlocksForDay(rules, day, zone).map { (rule, block) ->
            val kind = if (fixedById.getValue(rule.id).kind == FixedScheduleKind.BREAK) "Istirahat" else "Jadwal tetap"
            TimelineRow("fixed-" + rule.id, rule.label, kind, block.startAt, block.endAt)
        }
        val workRows = workspace.sessions.asSequence().filter {
            it.state == WorkSessionState.PENDING && it.plannedStartAt < end && it.plannedEndAt > start
        }.mapNotNull { session ->
            val task = tasksById[session.taskId] ?: return@mapNotNull null
            TimelineRow("work-" + session.id, task.title, "Sesi fokus", session.plannedStartAt, session.plannedEndAt, session)
        }.toList()
        (fixedRows + workRows).sortedWith(compareBy<TimelineRow> { it.start }.thenBy { it.id })
    }
    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)) {
        item {
            workspace.sessions.firstOrNull { it.state in ActiveFocusStates }?.let { active ->
                TextButton(onClick = { openFocus(active) }) { Text("Buka timer aktif") }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = defer, enabled = !state.busy) { Text("Saya lagi sibuk") }
                TextButton(onClick = permissions) { Text("Izin pengingat") }
            }
            if (state.risks.isNotEmpty()) Text(
                "Risiko lewat deadline pada ${state.risks.size} tugas. Lihat tab Belum Selesai.",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Text(DayFormat.format(day), modifier = Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { selectDay(day.minusDays(1)) }) { Text("Sebelumnya") }
                TextButton(onClick = { selectDay(today) }) { Text("Hari ini") }
                TextButton(onClick = { selectDay(day.plusDays(1)) }) { Text("Berikutnya") }
            }
            if (day > today.plusDays(6)) Text(
                "Sesi fokus direncanakan hingga tujuh hari ke depan.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider()
        }
        if (rows.isEmpty()) item {
            Text("Tidak ada blok jadwal hari ini.", modifier = Modifier.padding(vertical = 20.dp))
        }
        items(rows, key = { it.id }) { row ->
            Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val starts = formatTime(maxOf(row.start, start), zone)
                val ends = if (row.end >= end) "24:00" else formatTime(row.end, zone)
                Text(starts + "–" + ends + " · " + row.kind, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                Text(row.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                if (day == today && row.session != null && row.end > state.now) {
                    TextButton(onClick = { openFocus(row.session) }) { Text("Buka timer") }
                }
            }
            HorizontalDivider()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsScreen(workspace: Workspace, state: TazerState, model: TazerViewModel) {
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    val profile = workspace.profile
    val editing = workspace.fixed.firstOrNull { it.id == editId }
    val deleting = workspace.fixed.firstOrNull { it.id == deleteId }
    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)) {
        item {
            ProfileForm(profile, state.busy, model)
            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))
            Text("Jadwal tetap", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            TextButton(onClick = { editId = "" }, enabled = !state.busy) { Text("Tambah jadwal tetap") }
        }
        items(workspace.fixed.sortedWith(compareBy<FixedSchedule> { it.dayOfWeek }.thenBy { it.startMinute }), key = { it.id }) { block ->
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(block.label, style = MaterialTheme.typography.titleMedium)
                Text(dayName(block.dayOfWeek) + " · " + formatMinute(block.startMinute) + "–" + formatMinute(block.endMinute))
                FlowRow {
                    TextButton(onClick = { editId = block.id }, enabled = !state.busy) { Text("Edit jadwal") }
                    TextButton(onClick = { deleteId = block.id }, enabled = !state.busy) { Text("Hapus jadwal") }
                }
            }
            HorizontalDivider()
        }
        item {
            AccountSettings(state, model, Modifier.padding(top = 24.dp, bottom = 16.dp))
        }
    }
    if (editId != null) key(editId) {
        FixedEditor(editing, state.busy, model) { editId = null }
    }
    if (deleting != null) AlertDialog(
        onDismissRequest = { deleteId = null },
        title = { Text("Hapus jadwal tetap?") },
        text = { Text(deleting.label) },
        confirmButton = { TextButton(onClick = { model.deleteFixed(deleting); deleteId = null }) { Text("Hapus") } },
        dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Batal") } },
    )
}

@Composable
private fun ProfileForm(profile: UserProfile, busy: Boolean, model: TazerViewModel) {
    var start by rememberSaveable(profile.ownerUid, profile.activeStartMinute) { mutableStateOf(formatMinute(profile.activeStartMinute)) }
    var end by rememberSaveable(profile.ownerUid, profile.activeEndMinute) { mutableStateOf(formatMinute(profile.activeEndMinute)) }
    var zone by rememberSaveable(profile.ownerUid, profile.zoneId) { mutableStateOf(profile.zoneId) }
    val parsedStart = parseMinute(start)
    val parsedEnd = parseMinute(end)
    val zoneId = remember(zone) { runCatching { ZoneId.of(zone.trim()) }.getOrNull()?.takeIf { it.id in ZoneId.getAvailableZoneIds() } }
    val validRange = parsedStart != null && parsedEnd != null && parsedStart < parsedEnd
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Jam aktif", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        TimeField(start, { start = it }, "Mulai", busy)
        TimeField(end, { end = it }, "Selesai", busy)
        OutlinedTextField(zone, { zone = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Zona waktu") },
            singleLine = true, enabled = !busy, isError = zoneId == null)
        if (!validRange) Text("Jam selesai harus setelah jam mulai.", color = MaterialTheme.colorScheme.error)
        Button(
            onClick = {
                model.saveProfile(requireNotNull(zoneId), requireNotNull(parsedStart), requireNotNull(parsedEnd)) {
                    model.showMessage("Jam aktif tersimpan.")
                }
            }, enabled = !busy && validRange && zoneId != null,
        ) { Text("Simpan jam aktif") }
    }
}

@Composable
private fun FixedEditor(block: FixedSchedule?, busy: Boolean, model: TazerViewModel, close: () -> Unit) {
    var label by rememberSaveable { mutableStateOf(block?.label.orEmpty()) }
    var day by rememberSaveable { mutableIntStateOf(block?.dayOfWeek ?: 1) }
    var kind by rememberSaveable { mutableStateOf((block?.kind ?: FixedScheduleKind.ROUTINE).name) }
    var start by rememberSaveable { mutableStateOf(formatMinute(block?.startMinute ?: 480)) }
    var end by rememberSaveable { mutableStateOf(formatMinute(block?.endMinute ?: 600)) }
    var dayMenu by remember { mutableStateOf(false) }
    var kindMenu by remember { mutableStateOf(false) }
    val from = parseMinute(start)
    val to = parseMinute(end)
    val valid = from != null && to != null && from < 1440 && to != from
    AlertDialog(
        onDismissRequest = close,
        title = { Text(if (block == null) "Tambah jadwal tetap" else "Edit jadwal tetap") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).imePadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(label, { label = it }, label = { Text("Nama jadwal") }, singleLine = true, enabled = !busy)
                androidx.compose.foundation.layout.Box {
                    TextButton(onClick = { dayMenu = true }, enabled = !busy) { Text(dayName(day)) }
                    DropdownMenu(dayMenu, { dayMenu = false }) {
                        (1..7).forEach { choice ->
                            DropdownMenuItem(text = { Text(dayName(choice)) }, onClick = { day = choice; dayMenu = false })
                        }
                    }
                }
                androidx.compose.foundation.layout.Box {
                    TextButton(onClick = { kindMenu = true }, enabled = !busy) {
                        Text(if (kind == FixedScheduleKind.ROUTINE.name) "Rutinitas / kuliah" else "Istirahat")
                    }
                    DropdownMenu(kindMenu, { kindMenu = false }) {
                        FixedScheduleKind.entries.forEach { choice ->
                            DropdownMenuItem(
                                text = { Text(if (choice == FixedScheduleKind.ROUTINE) "Rutinitas / kuliah" else "Istirahat") },
                                onClick = { kind = choice.name; kindMenu = false },
                            )
                        }
                    }
                }
                TimeField(start, { start = it }, "Mulai", busy)
                TimeField(end, { end = it }, "Selesai", busy)
                if (valid && requireNotNull(to) < requireNotNull(from)) {
                    Text("Jadwal berlanjut ke hari berikutnya.")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                model.saveFixed(block, label, FixedScheduleKind.valueOf(kind), day, requireNotNull(from), requireNotNull(to), close)
            }, enabled = !busy && valid && label.trim().length in 1..120) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = close) { Text("Batal") } },
    )
}

@Composable
private fun TimeField(text: String, update: (String) -> Unit, label: String, busy: Boolean) {
    OutlinedTextField(text, update, label = { Text(label + " (HH:mm)") }, singleLine = true, enabled = !busy,
        isError = parseMinute(text) == null, modifier = Modifier.fillMaxWidth())
}

private fun dayName(day: Int) = DayOfWeek.of(day).getDisplayName(TextStyle.FULL, Indonesian)
