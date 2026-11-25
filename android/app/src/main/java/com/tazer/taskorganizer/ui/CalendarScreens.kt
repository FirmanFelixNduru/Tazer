package com.tazer.taskorganizer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
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
private val calendarHeaderPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)

@Composable
internal fun MonthPicker(month: YearMonth, selectMonth: (YearMonth) -> Unit) {
    MonthPicker(month, Modifier, ButtonDefaults.ContentPadding, selectMonth)
}

@Composable
private fun MonthPicker(month: YearMonth, modifier: Modifier, contentPadding: PaddingValues, selectMonth: (YearMonth) -> Unit) {
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    FilledTonalButton(
        onClick = { pickerOpen = true },
        modifier = modifier.testTag("calendar-month-picker"),
        contentPadding = contentPadding,
    ) {
        Text(monthLabel.format(month), textAlign = TextAlign.Center)
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
                                else OutlinedButton(choose, Modifier.weight(1f)) { Text(text) }
                            }
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { pickerOpen = false }) { Text("Tutup") } },
        )
    }
}

private data class AgendaItem(
    val key: String, val title: String, val kind: String, val time: String,
    val event: CalendarEvent? = null, val session: WorkSession? = null,
)

@OptIn(ExperimentalComposeUiApi::class)
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
    LazyColumn(
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonthPicker(month, Modifier.weight(1f).fillMaxHeight(), calendarHeaderPadding) {
                    selected = it.atDay(1).toEpochDay()
                }
                FilledTonalButton(
                    onClick = { selected = today.toEpochDay() },
                    modifier = Modifier.weight(1f).fillMaxHeight().testTag("calendar-today"),
                    contentPadding = calendarHeaderPadding,
                ) { Text("Hari Ini", textAlign = TextAlign.Center) }
                IconButton(onClick = { selected = month.minusMonths(1).atDay(1).toEpochDay() }) {
                    TazerIcon(TazerGlyph.Previous, "Bulan sebelumnya")
                }
                IconButton(onClick = { selected = month.plusMonths(1).atDay(1).toEpochDay() }) {
                    TazerIcon(TazerGlyph.Next, "Bulan berikutnya")
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                dayLabels.forEach { label ->
                    Text(label, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
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
                headlineContent = { AgendaPill(item.title, item.kind) },
                supportingContent = {
                    val details = item.event?.description?.takeIf(String::isNotBlank)
                    Text(listOfNotNull(item.kind, details).joinToString(" · "))
                },
                leadingContent = { Text(item.time, style = MaterialTheme.typography.labelMedium) },
                trailingContent = when {
                    item.event != null -> ({ IconButton(onClick = { openEvent(item.event) }) { TazerIcon(TazerGlyph.Edit, "Edit " + item.title) } })
                    item.session?.let { it.state in ActiveFocusStates || it.state == WorkSessionState.PENDING && day == today && it.plannedEndAt > state.now } == true ->
                        ({ TimerIconButton("Buka timer") { model.openFocus(requireNotNull(item.session)) } })
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
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().drawBehind {
            val stroke = 1.dp.toPx()
            val inset = (stroke / 2).coerceAtMost(size.width / 2)
            for (column in 0..7) {
                val x = (size.width * column / 7).coerceIn(inset, size.width - inset)
                drawLine(colors.outline, Offset(x, 0f), Offset(x, size.height), stroke)
            }
            val bottom = size.height - stroke / 2
            drawLine(colors.outline, Offset(0f, bottom), Offset(size.width, bottom), stroke)
        },
    ) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            for (offset in 0L..6L) {
                val date = monday.plusDays(offset)
                val active = date == selected
                val current = date == today
                val entries = agenda[date].orEmpty().filter { it.event == null || it.event.startEpochDay == it.event.endEpochDay }
                Column(
                    Modifier.weight(1f).fillMaxHeight().heightIn(min = 64.dp)
                        .testTag("calendar-date-cell")
                        .selectable(selected = active, role = Role.Button, onClick = { select(date) })
                        .semantics { contentDescription = DayFormat.format(date) + if (current) ", hari ini" else "" }
                        .padding(horizontal = 3.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        border = if (current && !active) BorderStroke(1.dp, colors.primary) else null,
                        color = when {
                            active -> colors.primary
                            current -> colors.primaryContainer
                            else -> Color.Transparent
                        },
                        contentColor = when {
                            active -> colors.onPrimary
                            current -> colors.onPrimaryContainer
                            YearMonth.from(date) == month -> colors.onSurface
                            else -> colors.onSurfaceVariant
                        },
                    ) {
                        Box(
                            Modifier.sizeIn(minWidth = 26.dp, minHeight = 26.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.titleSmall,
                                textDecoration = if (current) TextDecoration.Underline else null,
                                maxLines = 1,
                            )
                        }
                    }
                    if (entries.isNotEmpty()) {
                        val first = entries.first()
                        AgendaPill(
                            if (entries.size == 1) first.title else "${entries.size} agenda",
                            first.kind,
                            Modifier.fillMaxWidth(),
                            compact = true,
                        )
                        if (entries.size == 1 && first.time != "Seharian") {
                            Text(
                                first.time,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
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
                        modifier = Modifier.weight((end - start + 1).toFloat()).padding(horizontal = 2.dp).testTag("calendar-event-span"),
                        color = colors.secondaryContainer,
                        contentColor = colors.onSecondaryContainer,
                        shape = RoundedCornerShape(7.dp),
                    ) {
                        Text(event.title, Modifier.padding(horizontal = 5.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    cursor = end + 1
                }
                if (cursor < base + 7) Spacer(Modifier.weight((base + 7 - cursor).toFloat()))
            }
        }
    }
}

@Composable
private fun AgendaPill(title: String, kind: String, modifier: Modifier = Modifier, compact: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val (background, ink) = when (kind) {
        "Deadline" -> colors.errorContainer to colors.onErrorContainer
        "Sesi fokus" -> colors.primaryContainer to colors.onPrimaryContainer
        else -> colors.secondaryContainer to colors.onSecondaryContainer
    }
    Surface(modifier.testTag("calendar-event-pill"), color = background, contentColor = ink, shape = RoundedCornerShape(7.dp)) {
        Text(
            title,
            Modifier.padding(horizontal = if (compact) 3.dp else 8.dp, vertical = if (compact) 1.dp else 3.dp),
            style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
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
            confirmButton = { Button(enabled = !busy, onClick = { model.deleteEvent(event.id, close) }) { Text("Hapus") } },
            dismissButton = { OutlinedButton(onClick = { deleting = false }) { Text("Batal") } },
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
                if (event != null) OutlinedButton(onClick = { deleting = true }, enabled = !busy) { Text("Hapus acara") }
            }
        },
        confirmButton = {
            Button(
                onClick = { model.saveEvent(event?.id, title, from, through, if (allDay) null else minute, description, close) },
                enabled = !busy && title.isNotBlank() && through >= from && (allDay || minute != null),
            ) { Text("Simpan acara") }
        },
        dismissButton = { OutlinedButton(onClick = close, enabled = !busy) { Text("Batal") } },
    )
}

@Composable
private fun EventDate(label: String, epochDay: Long, enabled: Boolean, change: (Long) -> Unit) {
    val date = LocalDate.ofEpochDay(epochDay)
    PickerDateButton(date, enabled, label + " · " + DayFormat.format(date)) { change(it.toEpochDay()) }
}
