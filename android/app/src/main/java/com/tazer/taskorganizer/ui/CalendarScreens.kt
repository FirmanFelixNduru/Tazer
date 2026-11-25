package com.tazer.taskorganizer.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tazer.taskorganizer.data.CalendarEvent
import com.tazer.taskorganizer.data.WorkSession
import com.tazer.taskorganizer.data.WorkSessionState
import com.tazer.taskorganizer.domain.ScheduleRule
import com.tazer.taskorganizer.domain.Scheduler
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val monthLabel = DateTimeFormatter.ofPattern("MMMM yyyy", Indonesian)
private val dayLabels = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")

@Composable
internal fun MonthPicker(month: YearMonth, selectMonth: (YearMonth) -> Unit) {
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = { pickerOpen = true }) {
        Text(monthLabel.format(month), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(6.dp))
        TazerIcon(TazerGlyph.Down)
    }
    if (pickerOpen) {
        var year by rememberSaveable(month) { mutableIntStateOf(month.year) }
        AlertDialog(
            onDismissRequest = { pickerOpen = false },
            title = { Text("Pilih bulan") },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { year-- }) { TazerIcon(TazerGlyph.Previous, "Tahun sebelumnya") }
                        Text(year.toString(), Modifier.weight(1f), textAlign = TextAlign.Center)
                        IconButton(onClick = { year++ }) { TazerIcon(TazerGlyph.Next, "Tahun berikutnya") }
                    }
                    (1..12).chunked(3).forEach { months ->
                        Row(Modifier.fillMaxWidth()) {
                            months.forEach { number ->
                                val choice = YearMonth.of(year, number)
                                val text = DateTimeFormatter.ofPattern("MMM", Indonesian).format(choice)
                                val choose = {
                                    selectMonth(choice)
                                    pickerOpen = false
                                }
                                if (choice == month) FilledTonalButton(choose, Modifier.weight(1f)) { Text(text) }
                                else TextButton(choose, Modifier.weight(1f)) { Text(text) }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pickerOpen = false }) { Text("Tutup") } },
        )
    }
}

private data class AgendaItem(
    val key: String, val title: String, val kind: String, val time: String,
    val event: CalendarEvent? = null, val session: WorkSession? = null,
)

@Composable
internal fun CalendarScreen(workspace: Workspace, state: TazerState, zone: ZoneId, model: TazerViewModel) {
    val today = localDate(state.now, zone)
    var selected by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    val day = LocalDate.ofEpochDay(selected)
    val month = YearMonth.from(day)
    val first = month.atDay(1).let { it.minusDays((it.dayOfWeek.value - 1).toLong()) }
    val weeks = List((month.lengthOfMonth() + month.atDay(1).dayOfWeek.value + 5) / 7) { first.plusWeeks(it.toLong()) }
    val agenda = remember(workspace, month, zone) {
        val byId = workspace.tasks.associateBy { it.id }
        val rules = workspace.fixed.map { ScheduleRule(it.id, it.label, it.dayOfWeek, it.startMinute, it.endMinute) }
        weeks.flatMap { monday -> (0L..6L).map(monday::plusDays) }.associateWith { date ->
            val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            buildList {
                workspace.events.filter { date.toEpochDay() in it.startEpochDay..it.endEpochDay }.forEach {
                    add(AgendaItem("event-" + it.id, it.title, "Acara",
                        it.startMinute?.takeIf { _ -> date.toEpochDay() == it.startEpochDay }?.let(::formatMinute) ?: "Seharian", event = it))
                }
                workspace.tasks.filter { localDate(it.deadlineAt, zone) == date }.forEach {
                    add(AgendaItem("task-" + it.id, it.title, "Deadline", formatTime(it.deadlineAt, zone)))
                }
                workspace.sessions.filter { it.state != WorkSessionState.CANCELLED && it.plannedStartAt < dayEnd && it.plannedEndAt > dayStart }.forEach {
                    val task = byId[it.taskId] ?: return@forEach
                    add(AgendaItem("session-" + it.id, task.title, "Sesi fokus", formatTime(maxOf(dayStart, it.plannedStartAt), zone), session = it))
                }
                Scheduler.fixedBlocksForDay(rules, date, zone).forEach { (rule, block) ->
                    add(AgendaItem("fixed-" + rule.id, rule.label, "Jadwal tetap", formatTime(maxOf(dayStart, block.startAt), zone)))
                }
            }.sortedWith(compareBy({ it.time != "Seharian" }, { it.time }, { it.title }))
        }
    }
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    val openEvent: (CalendarEvent) -> Unit = { editId = it.id }
    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { MonthPicker(month) { selected = it.atDay(1).toEpochDay() } }
                TextButton(onClick = { selected = today.toEpochDay() }) { Text("Hari Ini") }
                IconButton(onClick = { selected = month.minusMonths(1).atDay(1).toEpochDay() }) {
                    TazerIcon(TazerGlyph.Previous, "Bulan sebelumnya")
                }
                IconButton(onClick = { selected = month.plusMonths(1).atDay(1).toEpochDay() }) {
                    TazerIcon(TazerGlyph.Next, "Bulan berikutnya")
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                dayLabels.forEach { label ->
                    Text(label, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        items(weeks, key = LocalDate::toEpochDay) { monday ->
            CalendarWeek(monday, month, today, day, agenda, workspace.events, { selected = it.toEpochDay() }, openEvent)
        }
        item {
            Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(DayFormat.format(day), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                FilledTonalButton(onClick = { editId = "" }, enabled = !state.busy) { Text("Tambah acara") }
            }
            if (agenda[day].isNullOrEmpty()) {
                Text("Belum ada agenda pada tanggal ini.", Modifier.padding(vertical = 14.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(agenda[day].orEmpty(), key = AgendaItem::key) { item ->
            ListItem(
                headlineContent = { Text(item.title) },
                supportingContent = {
                    val details = item.event?.description?.takeIf(String::isNotBlank)
                    Text(listOfNotNull(item.kind, details).joinToString(" · "))
                },
                leadingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        AgendaDot(item.kind)
                        Text(item.time, style = MaterialTheme.typography.labelMedium)
                    }
                },
                trailingContent = when {
                    item.event != null -> ({ IconButton(onClick = { openEvent(item.event) }) { TazerIcon(TazerGlyph.Edit, "Edit " + item.title) } })
                    item.session?.let { it.state in ActiveFocusStates || it.state == WorkSessionState.PENDING && day == today && it.plannedEndAt > state.now } == true ->
                        ({ IconButton(onClick = { model.openFocus(requireNotNull(item.session)) }) { TazerIcon(TazerGlyph.Timer, "Buka timer") } })
                    else -> null
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
    val id = editId
    if (id != null) {
        val event = workspace.events.firstOrNull { it.id == id }
        if (id.isEmpty() || event != null) key(id) {
            EventEditor(event, day, state.busy, model) { editId = null }
        }
    }
}

@Composable
private fun CalendarWeek(
    monday: LocalDate, month: YearMonth, today: LocalDate, selected: LocalDate,
    agenda: Map<LocalDate, List<AgendaItem>>, events: List<CalendarEvent>,
    select: (LocalDate) -> Unit, edit: (CalendarEvent) -> Unit,
) {
    val base = monday.toEpochDay()
    val lanes = remember(base, events) {
        val rows = mutableListOf<MutableList<CalendarEvent>>()
        events.filter { it.startEpochDay < it.endEpochDay && it.startEpochDay <= base + 6 && it.endEpochDay >= base }
            .sortedWith(compareBy({ it.startEpochDay }, { it.endEpochDay }, { it.id })).forEach { event ->
                val lane = rows.firstOrNull { it.last().endEpochDay < event.startEpochDay }
                if (lane == null) rows.add(mutableListOf(event)) else lane.add(event)
            }
        rows
    }
    Row(Modifier.fillMaxWidth()) {
        for (offset in 0L..6L) {
            val date = monday.plusDays(offset)
            val active = date == selected
            val current = date == today
            val entries = agenda[date].orEmpty().filter { it.event == null || it.event.startEpochDay == it.event.endEpochDay }
            Column(
                Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                    .selectable(selected = active, role = Role.Button, onClick = { select(date) })
                    .semantics { contentDescription = DayFormat.format(date) + if (current) ", hari ini" else "" },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    shape = CircleShape,
                    color = when {
                        active -> MaterialTheme.colorScheme.primary
                        current -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surface
                    },
                    contentColor = when {
                        active -> MaterialTheme.colorScheme.onPrimary
                        current -> MaterialTheme.colorScheme.onPrimaryContainer
                        YearMonth.from(date) == month -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                ) {
                    Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                        Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.labelLarge)
                    }
                }
                if (entries.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        AgendaDot(entries.first().kind)
                        Text(if (entries.size == 1) entries.first().time else "+${entries.size}",
                            style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (entries.size == 1) Text(entries.first().title, Modifier.padding(horizontal = 2.dp),
                        style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
    lanes.forEach { lane ->
        Row(Modifier.fillMaxWidth().padding(bottom = 3.dp)) {
            var cursor = base
            lane.forEach { event ->
                val start = maxOf(base, event.startEpochDay)
                val end = minOf(base + 6, event.endEpochDay)
                if (start > cursor) Spacer(Modifier.weight((start - cursor).toFloat()))
                Surface(
                    onClick = { edit(event) },
                    modifier = Modifier.weight((end - start + 1).toFloat()).padding(horizontal = 1.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = RoundedCornerShape(7.dp),
                ) {
                    Text(event.title, Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                cursor = end + 1
            }
            if (cursor < base + 7) Spacer(Modifier.weight((base + 7 - cursor).toFloat()))
        }
    }
    HorizontalDivider(Modifier.padding(bottom = 3.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun AgendaDot(kind: String) {
    val color = when (kind) {
        "Deadline" -> MaterialTheme.colorScheme.error
        "Sesi fokus" -> MaterialTheme.colorScheme.primary
        "Acara" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.secondary
    }
    Box(Modifier.size(5.dp).background(color, CircleShape))
}

@Composable
private fun EventEditor(event: CalendarEvent?, selected: LocalDate, busy: Boolean, model: TazerViewModel, close: () -> Unit) {
    var title by rememberSaveable { mutableStateOf(event?.title.orEmpty()) }
    var description by rememberSaveable { mutableStateOf(event?.description.orEmpty()) }
    var from by rememberSaveable { mutableLongStateOf(event?.startEpochDay ?: selected.toEpochDay()) }
    var through by rememberSaveable { mutableLongStateOf(event?.endEpochDay ?: selected.toEpochDay()) }
    var allDay by rememberSaveable { mutableStateOf(event?.startMinute == null) }
    var time by rememberSaveable { mutableStateOf(formatMinute(event?.startMinute ?: 540)) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    val minute = parseMinute(time)?.takeIf { it < 1440 }
    if (deleting && event != null) {
        AlertDialog(
            onDismissRequest = { deleting = false }, title = { Text("Hapus acara?") }, text = { Text(event.title) },
            confirmButton = { TextButton(enabled = !busy, onClick = { model.deleteEvent(event.id, close) }) { Text("Hapus") } },
            dismissButton = { TextButton(onClick = { deleting = false }) { Text("Batal") } },
        )
        return
    }
    AlertDialog(
        onDismissRequest = { if (!busy) close() },
        title = { Text(if (event == null) "Tambah acara" else "Edit acara") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 390.dp)
                .imePadding().verticalScroll(rememberScrollState())) {
                OutlinedTextField(title, { title = it }, label = { Text("Judul acara") }, singleLine = true, enabled = !busy)
                EventDate("Mulai", from, !busy) { from = it; through = maxOf(through, it) }
                EventDate("Sampai", through, !busy) { through = it }
                if (through < from) Text("Tanggal akhir harus sama atau setelah tanggal mulai.", color = MaterialTheme.colorScheme.error)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Switch(checked = allDay, onCheckedChange = { allDay = it }, enabled = !busy,
                        modifier = Modifier.semantics { contentDescription = "Seharian" })
                    Text("Seharian")
                }
                if (!allDay) OutlinedTextField(time, { time = it }, label = { Text("Jam mulai (HH:mm)") }, singleLine = true,
                    enabled = !busy, isError = minute == null)
                OutlinedTextField(description, { description = it }, label = { Text("Keterangan (opsional)") }, enabled = !busy)
                if (event != null) TextButton(onClick = { deleting = true }, enabled = !busy) { Text("Hapus acara") }
            }
        },
        confirmButton = {
            Button(
                onClick = { model.saveEvent(event?.id, title, from, through, if (allDay) null else minute, description, close) },
                enabled = !busy && title.isNotBlank() && through >= from && (allDay || minute != null),
            ) { Text("Simpan acara") }
        },
        dismissButton = { TextButton(onClick = close, enabled = !busy) { Text("Batal") } },
    )
}

@Composable
private fun EventDate(label: String, epochDay: Long, enabled: Boolean, change: (Long) -> Unit) {
    val context = LocalContext.current
    val date = LocalDate.ofEpochDay(epochDay)
    OutlinedButton(onClick = {
        DatePickerDialog(context, { _, year, month, day ->
            change(LocalDate.of(year, month + 1, day).toEpochDay())
        }, date.year, date.monthValue - 1, date.dayOfMonth).show()
    }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Text(label + " · " + DayFormat.format(date))
    }
}
