package com.tazer.taskorganizer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import java.time.YearMonth
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
    val session: WorkSession? = null, val course: String? = null, val description: String? = null,
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
    val weekStart = day.minusDays((day.dayOfWeek.value - 1).toLong())
    val rows = remember(workspace, day, zone, today) {
        val fixedById = workspace.fixed.associateBy { it.id }
        val tasksById = workspace.tasks.associateBy { it.id }
        val rules = workspace.fixed.map { ScheduleRule(it.id, it.label, it.dayOfWeek, it.startMinute, it.endMinute) }
        val fixedRows = Scheduler.fixedBlocksForDay(rules, day, zone).map { (rule, block) ->
            val kind = if (fixedById.getValue(rule.id).kind == FixedScheduleKind.BREAK) "Istirahat" else "Jadwal tetap"
            TimelineRow("fixed-" + rule.id, rule.label, kind, block.startAt, block.endAt)
        }
        val workRows = workspace.sessions.asSequence().filter {
            (it.state == WorkSessionState.PENDING || it.state in ActiveFocusStates) &&
                (it.plannedStartAt < end && it.plannedEndAt > start || it.state in ActiveFocusStates && day == today)
        }.mapNotNull { session ->
            val task = tasksById[session.taskId] ?: return@mapNotNull null
            val startAt = session.startedAt ?: session.plannedStartAt
            val duration = (session.cutoffElapsedMs ?: 0L) - (session.activeSinceElapsedMs ?: 0L)
            val endAt = if (duration > 0) startAt + duration else session.plannedEndAt
            TimelineRow("work-" + session.id, task.title, "Sesi fokus", startAt, endAt, session, task.course, task.description)
        }.toList()
        (fixedRows + workRows).sortedWith(compareBy<TimelineRow> { it.start }.thenBy { it.id })
    }
    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                MonthPicker(YearMonth.from(day)) { month -> selectDay(month.atDay(day.dayOfMonth.coerceAtMost(month.lengthOfMonth()))) }
                TextButton(onClick = { selectDay(today) }) { Text("Hari Ini") }
            }
            var drag by remember { mutableStateOf(0f) }
            Row(
                Modifier.fillMaxWidth().padding(vertical = 12.dp).pointerInput(weekStart) {
                    detectHorizontalDragGestures(onDragEnd = {
                        if (drag > 60f) selectDay(day.minusWeeks(1)) else if (drag < -60f) selectDay(day.plusWeeks(1))
                        drag = 0f
                    }, onHorizontalDrag = { _, amount -> drag += amount })
                }, horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                (0L..6L).forEach { offset ->
                    val date = weekStart.plusDays(offset)
                    val isSelected = date == day
                    Surface(
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp).semantics {
                            contentDescription = DayFormat.format(date)
                            selected = isSelected
                        }.clickable { selectDay(date) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    ) {
                        Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(date.dayOfWeek.getDisplayName(TextStyle.SHORT, Indonesian), style = MaterialTheme.typography.labelMedium, maxLines = 1)
                            Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { selectDay(day.minusWeeks(1)) }) { Text("Pekan sebelumnya") }
                TextButton(onClick = { selectDay(day.plusWeeks(1)) }) { Text("Pekan berikutnya") }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = defer, enabled = !state.busy) { Text("Saya lagi sibuk") }
                TextButton(onClick = permissions) { Text("Izin pengingat") }
            }
            if (state.risks.isNotEmpty()) Text(
                "Risiko lewat deadline pada ${state.risks.size} tugas. Lihat tab Belum Selesai.",
                color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp),
            )
            if (day > today.plusDays(6)) Text("Sesi fokus direncanakan hingga tujuh hari ke depan.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp)) {
                Text("Jam", Modifier.width(68.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Jadwal", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (rows.isEmpty()) item {
            Text("Tidak ada blok jadwal pada tanggal ini.", modifier = Modifier.padding(vertical = 24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(rows, key = { it.id }) { row ->
            TimelineEntry(row, start, end, state.now, zone, day == today, openFocus)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimelineEntry(row: TimelineRow, start: Long, end: Long, now: Long, zone: ZoneId, isToday: Boolean, openFocus: (WorkSession) -> Unit) {
    val active = row.session?.state in ActiveFocusStates
    val overtime = row.session?.state == WorkSessionState.OVERTIME
    val status = when {
        overtime -> "Waktu rencana habis ? lembur"
        active -> "Sedang fokus"
        row.start > now -> {
            val minutes = ((row.start - now + 59_999) / 60_000)
            if (minutes >= 60) "Dimulai dalam ${minutes / 60} jam ${minutes % 60} menit" else "Dimulai dalam $minutes menit"
        }
        row.end > now -> "Sedang berlangsung"
        row.session != null -> "Waktu sesi terlewat"
        else -> "Jadwal berakhir"
    }
    Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.width(68.dp).padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(formatTime(maxOf(row.start, start), zone), style = MaterialTheme.typography.labelLarge)
            Text(if (row.end >= end) "24:00" else formatTime(row.end, zone), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(Modifier.padding(end = 10.dp).width(1.dp).height(100.dp).background(MaterialTheme.colorScheme.outlineVariant))
        Surface(Modifier.weight(1f), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column {
                val colors = MaterialTheme.colorScheme
                val banner = when {
                    overtime -> colors.tertiaryContainer
                    active -> colors.primaryContainer
                    else -> colors.secondaryContainer
                }
                val bannerText = when {
                    overtime -> colors.onTertiaryContainer
                    active -> colors.onPrimaryContainer
                    else -> colors.onSecondaryContainer
                }
                Text(status, Modifier.fillMaxWidth().background(banner).padding(horizontal = 16.dp, vertical = 10.dp), color = bannerText, style = MaterialTheme.typography.labelLarge)
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(row.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).semantics { heading() })
                        if (row.session != null && (active || isToday && row.end > now)) {
                            IconButton(onClick = { openFocus(row.session) }) {
                                TazerIcon(TazerGlyph.Timer, if (active) "Buka timer aktif" else "Buka timer")
                            }
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ScheduleTag(row.kind)
                        row.course?.takeIf { it.isNotBlank() }?.let { ScheduleTag(it) }
                        ScheduleTag("${((row.end - row.start) / 60_000).coerceAtLeast(1)} menit")
                    }
                    row.description?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleTag(label: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
        Text(label, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium)
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
            Surface(Modifier.fillMaxWidth().padding(vertical = 6.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(block.label, style = MaterialTheme.typography.titleMedium)
                    Text(dayName(block.dayOfWeek) + " · " + formatMinute(block.startMinute) + "–" + formatMinute(block.endMinute), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ScheduleTag(if (block.kind == FixedScheduleKind.BREAK) "Istirahat" else "Rutinitas / kuliah")
                    FlowRow {
                        TextButton(onClick = { editId = block.id }, enabled = !state.busy) { Text("Edit jadwal") }
                        TextButton(onClick = { deleteId = block.id }, enabled = !state.busy) { Text("Hapus jadwal") }
                    }
                }
            }
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
        Text("Atur waktu yang tersedia untuk sesi fokus.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
