package com.tazer.taskorganizer.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tazer.taskorganizer.data.CalendarEvent
import com.tazer.taskorganizer.data.FixedSchedule
import com.tazer.taskorganizer.data.FixedScheduleKind
import com.tazer.taskorganizer.data.IcsImportPreview
import com.tazer.taskorganizer.data.XlsxReader
import com.tazer.taskorganizer.domain.CourseColors
import com.tazer.taskorganizer.domain.PreviewRow
import com.tazer.taskorganizer.domain.ScheduleDraft
import com.tazer.taskorganizer.domain.SchedulePreview
import com.tazer.taskorganizer.domain.ScheduleTable
import com.tazer.taskorganizer.integration.IntegrationError
import com.tazer.taskorganizer.integration.IntegrationException
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val shortDays = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")

/** The single entry point on Jadwal: one + button that offers an event or a weekly schedule. */
@Composable
internal fun ScheduleAddButton(openEvents: () -> Unit, openFixed: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        FloatingActionButton(
            onClick = { open = true },
            modifier = Modifier.semantics { contentDescription = "Tambah ke jadwal" },
        ) { TazerIcon(TazerGlyph.Add) }
        DropdownMenu(open, { open = false }) {
            DropdownMenuItem(
                text = { Text("Tambah Acara") },
                leadingIcon = { TazerIcon(TazerGlyph.Calendar) },
                onClick = { open = false; openEvents() },
            )
            DropdownMenuItem(
                text = { Text("Jadwal Tetap") },
                leadingIcon = { TazerIcon(TazerGlyph.Timeline) },
                onClick = { open = false; openFixed() },
            )
        }
    }
}

/** Shared page frame so Acara and Jadwal Tetap read the same: heading, form card, then saved items. */
@Composable
private fun PageHeading(detail: String) {
    // The top bar already names the page, so the page opens with what it is for.
    Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun FormCard(content: @Composable () -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
    }
}

@Composable
private fun SavedCard(title: String, detail: String, marker: Color?, tag: String?, onEdit: () -> Unit, onDelete: () -> Unit, enabled: Boolean) {
    Surface(Modifier.fillMaxWidth().padding(vertical = 5.dp), shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(Modifier.padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (marker != null) Box(Modifier.size(12.dp).background(marker, CircleShape))
            Column(Modifier.weight(1f).padding(start = if (marker != null) 12.dp else 0.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (tag != null) Text(tag, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onEdit, enabled = enabled) { TazerIcon(TazerGlyph.Edit, "Edit $title") }
            IconButton(onClick = onDelete, enabled = enabled) { TazerIcon(TazerGlyph.Delete, "Hapus $title") }
        }
    }
}

@Composable
private fun DeleteDialog(title: String, detail: String, confirm: () -> Unit, close: () -> Unit) = AlertDialog(
    onDismissRequest = close,
    title = { Text(title) },
    text = { Text(detail) },
    confirmButton = { Button(onClick = { confirm(); close() }) { Text("Hapus") } },
    dismissButton = { TextButton(onClick = close) { Text("Batal") } },
)

private fun eventMarker(index: Int?, dark: Boolean) = index?.let { Color(CourseColors.palette[it].tone(dark).accent.toInt()) }

// ---------------------------------------------------------------- Acara

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EventsPage(
    workspace: Workspace, state: TazerState, zone: ZoneId, model: TazerViewModel, editId: String?, setEditId: (String?) -> Unit,
) {
    val editing = workspace.events.firstOrNull { it.id == editId }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    val dark = LocalTazerDark.current
    val today = localDate(state.now, zone)
    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageHeading("Acara sekali atau beberapa hari tampil di Kalender dan Jadwal, lalu ikut Cloud Sync.") }
        item(key = editing?.id ?: "new") { EventForm(editing, today, state.busy, model) { setEditId(null) } }
        item {
            Text("Acara tersimpan", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            if (workspace.events.isEmpty()) Text("Belum ada acara.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(workspace.events.sortedWith(compareBy({ it.startEpochDay }, { it.startMinute ?: -1 }, { it.title })), key = { it.id }) { event ->
            SavedCard(event.title, eventSummary(event), eventMarker(event.colorIndex, dark) ?: MaterialTheme.colorScheme.secondary,
                event.description.takeIf(String::isNotBlank), { setEditId(event.id) }, { deleteId = event.id }, !state.busy)
        }
    }
    workspace.events.firstOrNull { it.id == deleteId }?.let { event ->
        DeleteDialog("Hapus acara?", event.title, { model.deleteEvent(event.id) { if (editId == event.id) setEditId(null) } }) { deleteId = null }
    }
}

private fun eventSummary(event: CalendarEvent): String {
    val start = LocalDate.ofEpochDay(event.startEpochDay)
    val end = LocalDate.ofEpochDay(event.endEpochDay)
    val days = if (start == end) DayFormat.format(start) else DayFormat.format(start) + " – " + DayFormat.format(end)
    val time = event.startMinute?.let { from -> formatMinute(from) + (event.endMinute?.let { " – " + formatMinute(it) } ?: "") } ?: "Seharian"
    return "$days · $time"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EventForm(event: CalendarEvent?, today: LocalDate, busy: Boolean, model: TazerViewModel, done: () -> Unit) {
    var title by rememberSaveable { mutableStateOf(event?.title.orEmpty()) }
    var from by rememberSaveable { mutableStateOf(event?.startEpochDay ?: today.toEpochDay()) }
    var through by rememberSaveable { mutableStateOf(event?.endEpochDay ?: today.toEpochDay()) }
    var allDay by rememberSaveable { mutableStateOf(event != null && event.startMinute == null) }
    var start by rememberSaveable { mutableIntStateOf(event?.startMinute ?: 9 * 60) }
    var end by rememberSaveable { mutableIntStateOf(event?.endMinute ?: ((event?.startMinute ?: 9 * 60) + 60).coerceAtMost(1439)) }
    var color by rememberSaveable { mutableStateOf(event?.colorIndex) }
    var notes by rememberSaveable { mutableStateOf(event?.description.orEmpty()) }
    val dark = LocalTazerDark.current
    val timeValid = allDay || through > from || end > start
    val valid = title.trim().length in 1..500 && from <= through && timeValid
    FormCard {
        Text(if (event == null) "Tambah acara" else "Edit acara", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Judul acara") }, singleLine = true, enabled = !busy)
        Text("Tanggal", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PickerDateButton(LocalDate.ofEpochDay(from), !busy, "Mulai " + DayFormat.format(LocalDate.ofEpochDay(from))) {
                from = it.toEpochDay()
                if (through < from) through = from
            }
            PickerDateButton(LocalDate.ofEpochDay(through), !busy, "Selesai " + DayFormat.format(LocalDate.ofEpochDay(through))) {
                through = maxOf(from, it.toEpochDay())
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Seharian", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Switch(allDay, { allDay = it }, enabled = !busy, modifier = Modifier.semantics { contentDescription = "Seharian" })
        }
        if (!allDay) {
            Text("Jam", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically) {
                PickerTimeButton(start, !busy) { start = it }
                Text("sampai", color = MaterialTheme.colorScheme.onSurfaceVariant)
                PickerTimeButton(end, !busy) { end = it }
            }
            if (!timeValid) Text("Jam selesai harus setelah jam mulai.", color = MaterialTheme.colorScheme.error)
        }
        Text("Warna penanda", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ColorChoice(color == null, MaterialTheme.colorScheme.secondaryContainer, "bawaan") { color = null }
            CourseColors.palette.forEachIndexed { index, swatch ->
                ColorChoice(color == index, Color(swatch.tone(dark).accent.toInt()), swatch.name) { color = index }
            }
        }
        OutlinedTextField(notes, { notes = it.take(10_000) }, Modifier.fillMaxWidth(), label = { Text("Catatan (opsional)") },
            minLines = 2, enabled = !busy)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                model.saveEvent(event?.id, title, from, through, if (allDay) null else start, notes,
                    if (allDay) null else end, color) {
                    model.showMessage(if (event == null) "Acara tersimpan." else "Acara diperbarui.")
                    title = ""; notes = ""; color = null; allDay = false
                    done()
                }
            }, enabled = !busy && valid) { Text("Simpan Acara") }
            if (event != null) OutlinedButton(onClick = done, enabled = !busy) { Text("Batal edit") }
        }
    }
}

@Composable
private fun ColorChoice(chosen: Boolean, fill: Color, name: String, choose: () -> Unit) {
    Box(
        Modifier.size(40.dp)
            .border(if (chosen) 3.dp else 1.dp, if (chosen) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline, CircleShape)
            .padding(5.dp)
            .background(fill, CircleShape)
            .clickable(role = Role.RadioButton, onClick = choose)
            .semantics { contentDescription = "Warna $name"; selected = chosen },
    )
}

// ---------------------------------------------------------------- Jadwal Tetap

private enum class ImportSource(val label: String, val tag: String, val glyph: TazerGlyph, val iconLabel: String) {
    MANUAL("Input Manual", "Offline", TazerGlyph.Edit, "Ikon input manual"),
    ICS("Impor .ics", "Offline", TazerGlyph.Calendar, "Ikon kalender .ics"),
    CSV("Impor .csv", "Offline", TazerGlyph.CsvFile, "Ikon file CSV"),
    XLSX("Impor .xlsx (Excel)", "Offline", TazerGlyph.ExcelFile, "Ikon spreadsheet Excel"),
    PHOTO("Foto / Screenshot Jadwal", "Gemini AI", TazerGlyph.Camera, "Ikon foto jadwal"),
}

/** Rows waiting in the unified preview, whatever file or photo they came from. */
internal class ImportBatch(val source: String, val drafts: List<ScheduleDraft>, val skipped: Int, val warnings: List<String>, val ics: IcsImportPreview?)

private const val MAX_IMPORT_BYTES = 2 * 1024 * 1024

private suspend fun readBytes(context: Context, uri: Uri): ByteArray = withContext(Dispatchers.IO) {
    val stream = context.contentResolver.openInputStream(uri) ?: throw IOException("No input stream")
    stream.use { input ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(32 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            output.write(buffer, 0, count)
            require(output.size() <= MAX_IMPORT_BYTES) { "File melebihi 2 MB." }
        }
        output.toByteArray()
    }
}

/** UTF-8 first; spreadsheets exported on Windows often use Windows-1252 instead. */
private fun decodeText(bytes: ByteArray): String = try {
    Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
} catch (_: java.nio.charset.CharacterCodingException) {
    String(bytes, charset("windows-1252"))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FixedSchedulesPage(workspace: Workspace, state: TazerState, zone: ZoneId, model: TazerViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var menu by remember { mutableStateOf(false) }
    var manual by rememberSaveable { mutableStateOf(false) }
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var photoChoice by remember { mutableStateOf(false) }
    var reading by remember { mutableStateOf<String?>(null) }
    var batch by remember { mutableStateOf<ImportBatch?>(null) }
    val uid = workspace.profile.ownerUid
    val tier = state.subscription?.tier
    val aiAvailable = !state.demo && (tier == com.tazer.taskorganizer.integration.SubscriptionTier.PRO ||
        tier == com.tazer.taskorganizer.integration.SubscriptionTier.BYOK)

    fun load(label: String, work: suspend () -> ImportBatch) {
        reading = label
        scope.launch {
            try {
                batch = work()
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: IntegrationException) {
                scanFailureMessage(failure.code)?.let(model::showMessage) ?: model.reportIntegrationFailure(failure)
            } catch (failure: IllegalArgumentException) {
                model.showMessage(failure.message?.takeIf { it.endsWith('.') } ?: "File tidak dapat dibaca sebagai jadwal.")
            } catch (_: Exception) {
                model.showMessage("File tidak dapat dibaca. Pilih kembali filenya.")
            } finally {
                reading = null
            }
        }
    }
    fun table(label: String, uri: Uri, rows: (ByteArray) -> List<List<String>>) = load(label) {
        val parsed = withContext(Dispatchers.Default) { ScheduleTable.rows(rows(readBytes(context, uri))) }
        ImportBatch(label, parsed.drafts, parsed.skipped, emptyList(), null)
    }
    val icsPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        load(".ics") {
            val preview = model.previewIcs(uid) { context.contentResolver.openInputStream(uri) ?: throw IOException("No stream") }
            val drafts = preview.weeklyBlocks.map { ScheduleDraft(it.label, it.dayOfWeek, it.startMinute, it.endMinute) }
            ImportBatch(".ics", drafts, preview.invalid + preview.unsupported, emptyList(), preview.takeIf { it.events.isNotEmpty() })
        }
    }
    val csvPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { table(".csv", it) { bytes -> ScheduleTable.parseCsv(decodeText(bytes)) } }
    }
    val xlsxPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { table(".xlsx", it) { bytes -> XlsxReader.read(bytes.inputStream()) } }
    }
    val images = rememberImageSource({ uri ->
        load("Foto") {
            val scan = model.scanSchedule(context.contentResolver, uri, zone)
            ImportBatch("Foto", scan.drafts, 0, scan.warnings, null)
        }
    }, model::showMessage)
    fun launch(source: ImportSource) {
        menu = false
        try {
            when (source) {
                ImportSource.MANUAL -> { editId = null; manual = true }
                ImportSource.ICS -> icsPicker.launch(arrayOf("text/calendar", "text/plain", "application/octet-stream"))
                ImportSource.CSV -> csvPicker.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain",
                    "application/csv", "application/vnd.ms-excel", "application/octet-stream"))
                ImportSource.XLSX -> xlsxPicker.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/octet-stream", "application/zip"))
                ImportSource.PHOTO -> if (aiAvailable) photoChoice = true else model.showMessage(IntegrationError.SCAN_REQUIRES_AI.message)
            }
        } catch (_: ActivityNotFoundException) {
            model.showMessage("Pemilih file tidak tersedia di perangkat ini.")
        }
    }

    val editing = workspace.fixed.firstOrNull { it.id == editId }
    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageHeading("Kuliah dan rutinitas mingguan. Scheduler tidak menaruh sesi fokus di jam ini.") }
        item {
            Box {
                FilledTonalButton(onClick = { menu = true }, enabled = !state.busy && reading == null,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    if (reading != null) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("Membaca ${reading}…", Modifier.padding(start = 12.dp))
                    } else {
                        TazerIcon(TazerGlyph.Add)
                        Text("Tambah Jadwal Tetap", Modifier.weight(1f).padding(start = 10.dp), style = MaterialTheme.typography.titleSmall)
                        TazerIcon(TazerGlyph.Down)
                    }
                }
                DropdownMenu(menu, { menu = false }) {
                    ImportSource.entries.forEach { source ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(source.label, style = MaterialTheme.typography.bodyLarge)
                                    Text(source.tag, style = MaterialTheme.typography.labelMedium,
                                        color = if (source == ImportSource.PHOTO) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            leadingIcon = { SourceIcon(source) },
                            onClick = { launch(source) },
                        )
                    }
                }
            }
        }
        if (manual || editing != null) item(key = "form-" + (editing?.id ?: "new")) {
            FixedForm(editing, state.busy, model) { manual = false; editId = null }
        }
        item {
            Text("Jadwal tersimpan", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            if (workspace.fixed.isEmpty()) Text("Belum ada jadwal tetap. Tambahkan secara manual atau impor dari .ics, .csv, .xlsx, maupun foto.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(workspace.fixed.sortedWith(compareBy<FixedSchedule>({ it.dayOfWeek }, { it.startMinute })), key = { it.id }) { block ->
            val tone = LocalCoursePalette.current.tone(block.label)
            SavedCard(block.label, dayName(block.dayOfWeek) + " · " + formatMinute(block.startMinute) + " – " + formatMinute(block.endMinute),
                Color(tone.accent.toInt()), if (block.kind == FixedScheduleKind.BREAK) "Istirahat" else "Kuliah / rutinitas",
                { manual = false; editId = block.id }, { deleteId = block.id }, !state.busy)
        }
    }
    if (photoChoice) AlertDialog(
        onDismissRequest = { photoChoice = false },
        title = { Text("Foto / Screenshot Jadwal") },
        text = { Text("AI membaca nama mata kuliah, hari, dan jam dari foto KRS atau jadwal. Ruang dan dosen tidak disimpan.") },
        confirmButton = { Button(onClick = { photoChoice = false; images.pickGallery() }) { Text("Galeri / Screenshot") } },
        dismissButton = { OutlinedButton(onClick = { photoChoice = false; images.takePhoto() }) { Text("Ambil Foto") } },
    )
    workspace.fixed.firstOrNull { it.id == deleteId }?.let { block ->
        DeleteDialog("Hapus jadwal tetap?", block.label, { model.deleteFixed(block) }) { deleteId = null }
    }
    batch?.let { ready ->
        val existing = workspace.fixed.map { ScheduleDraft(it.label, it.dayOfWeek, it.startMinute, it.endMinute) }
        SchedulePreviewSheet(ready, existing, state.busy, close = { batch = null }) { drafts, events ->
            model.saveScheduleDrafts(drafts, events) { batch = null }
        }
    }
}

@Composable
private fun SourceIcon(source: ImportSource) {
    Box(
        Modifier.size(40.dp).border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) { TazerIcon(source.glyph, source.iconLabel) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FixedForm(block: FixedSchedule?, busy: Boolean, model: TazerViewModel, done: () -> Unit) {
    var label by rememberSaveable { mutableStateOf(block?.label.orEmpty()) }
    var days by rememberSaveable { mutableStateOf(listOfNotNull(block?.dayOfWeek).ifEmpty { listOf(1) }) }
    var breakTime by rememberSaveable { mutableStateOf(block?.kind == FixedScheduleKind.BREAK) }
    var start by rememberSaveable { mutableIntStateOf(block?.startMinute ?: 8 * 60) }
    var end by rememberSaveable { mutableIntStateOf(block?.endMinute?.coerceAtMost(1439) ?: 9 * 60 + 40) }
    val valid = label.trim().length in 1..120 && days.isNotEmpty() && start != end
    FormCard {
        Text(if (block == null) "Input manual" else "Edit jadwal tetap", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(label, { label = it }, Modifier.fillMaxWidth(), label = { Text("Nama jadwal / mata kuliah") }, singleLine = true, enabled = !busy)
        Text(if (block == null) "Hari berulang" else "Hari", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            shortDays.forEachIndexed { index, name ->
                val day = index + 1
                FilterChip(
                    selected = day in days,
                    onClick = {
                        // An edited block is one row, so it keeps exactly one day.
                        days = if (block != null) listOf(day) else if (day in days) days - day else (days + day).sorted()
                    },
                    label = { Text(name) },
                    enabled = !busy,
                    modifier = Modifier.semantics { contentDescription = dayName(day) },
                )
            }
        }
        Text("Jam", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically) {
            PickerTimeButton(start, !busy) { start = it }
            Text("sampai", color = MaterialTheme.colorScheme.onSurfaceVariant)
            PickerTimeButton(end, !busy) { end = it }
        }
        if (end < start) Text("Jadwal berlanjut ke hari berikutnya.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Jam istirahat", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Switch(breakTime, { breakTime = it }, enabled = !busy, modifier = Modifier.semantics { contentDescription = "Jam istirahat" })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                model.saveFixed(block, label, if (breakTime) FixedScheduleKind.BREAK else FixedScheduleKind.ROUTINE, days, start, end) {
                    model.showMessage(if (block == null) "${days.size} jadwal tetap tersimpan." else "Jadwal tetap diperbarui.")
                    done()
                }
            }, enabled = !busy && valid) { Text(if (block == null) "Simpan Jadwal" else "Simpan Perubahan") }
            OutlinedButton(onClick = done, enabled = !busy) { Text("Tutup") }
        }
    }
}

// ---------------------------------------------------------------- Unified preview

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SchedulePreviewSheet(
    batch: ImportBatch, existing: List<ScheduleDraft>, busy: Boolean, close: () -> Unit,
    save: (List<ScheduleDraft>, IcsImportPreview?) -> Unit,
) {
    val drafts = remember(batch) { mutableStateListOf(*batch.drafts.toTypedArray()) }
    val analysis = SchedulePreview.analyze(drafts, existing)
    val ticked = remember(batch) { mutableStateListOf(*analysis.map { !it.duplicate && it.draft.valid }.toTypedArray()) }
    var editIndex by remember { mutableStateOf<Int?>(null) }
    var withEvents by remember(batch) { mutableStateOf(batch.ics != null) }
    val selected = drafts.filterIndexed { index, draft -> ticked.getOrElse(index) { false } && !analysis[index].duplicate && draft.valid }
    val duplicates = analysis.count { it.duplicate }
    val clashes = SchedulePreview.clashes(selected, existing)
    ModalBottomSheet(onDismissRequest = { if (!busy) close() }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Pratinjau impor ${batch.source}", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            Text(buildString {
                append("${drafts.size} jadwal terbaca")
                if (duplicates > 0) append(" · $duplicates duplikat dilewati")
                if (batch.skipped > 0) append(" · ${batch.skipped} baris tidak lengkap")
            }, color = MaterialTheme.colorScheme.onSurfaceVariant)
            batch.warnings.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary) }
            if (clashes > 0) Text("$clashes jadwal terpilih bentrok jam. Periksa sebelum menyimpan.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            if (drafts.isEmpty()) Text("Tidak ada baris jadwal yang bisa diimpor dari sumber ini.")
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                analysis.forEachIndexed { index, row ->
                    PreviewLine(row, ticked.getOrElse(index) { false }, busy,
                        onTick = { ticked[index] = it }, onEdit = { editIndex = index })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
            batch.ics?.let { ics ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Juga impor ${ics.events.size} acara sekali ke Kalender", Modifier.weight(1f))
                    Switch(withEvents, { withEvents = it }, enabled = !busy)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = close, enabled = !busy) { Text("Batal") }
                Button(
                    onClick = { save(selected, batch.ics?.takeIf { withEvents }) },
                    enabled = !busy && (selected.isNotEmpty() || (withEvents && batch.ics != null)),
                    modifier = Modifier.weight(1f),
                ) { Text("Simpan ${selected.size} Jadwal Terpilih") }
            }
        }
    }
    editIndex?.let { index ->
        DraftEditor(drafts[index], close = { editIndex = null }) { updated ->
            drafts[index] = updated
            ticked[index] = true
            editIndex = null
        }
    }
}

@Composable
private fun PreviewLine(row: PreviewRow, ticked: Boolean, busy: Boolean, onTick: (Boolean) -> Unit, onEdit: () -> Unit) {
    val draft = row.draft
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(ticked && !row.duplicate, onTick, enabled = !busy && !row.duplicate && draft.valid,
            modifier = Modifier.semantics { contentDescription = "Pilih ${draft.title} ${dayName(draft.day)}" })
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(draft.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(dayName(draft.day) + " · " + formatMinute(draft.startMinute) + " – " + formatMinute(draft.endMinute),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            when {
                row.duplicate -> Text("Sudah tersimpan, dilewati", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                row.clashesWith.isNotEmpty() -> Text("Bentrok dengan " + row.clashesWith.joinToString(), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error)
            }
        }
        if (!row.duplicate) IconButton(onClick = onEdit, enabled = !busy) { TazerIcon(TazerGlyph.Edit, "Edit ${draft.title}") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DraftEditor(draft: ScheduleDraft, close: () -> Unit, save: (ScheduleDraft) -> Unit) {
    var title by remember { mutableStateOf(draft.title) }
    var day by remember { mutableIntStateOf(draft.day) }
    var start by remember { mutableIntStateOf(draft.startMinute) }
    var end by remember { mutableIntStateOf(draft.endMinute.coerceAtMost(1439)) }
    val updated = ScheduleDraft(title.trim(), day, start, if (draft.endMinute == 1440 && end == 1439) 1440 else end)
    AlertDialog(
        onDismissRequest = close,
        title = { Text("Edit baris jadwal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Nama") }, singleLine = true)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    shortDays.forEachIndexed { index, name ->
                        FilterChip(day == index + 1, { day = index + 1 }, label = { Text(name) })
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                    PickerTimeButton(start, true) { start = it }
                    Text("sampai")
                    PickerTimeButton(end, true) { end = it }
                }
                if (!updated.valid) Text("Isi nama dan pastikan jam selesai setelah jam mulai.", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { Button(onClick = { save(updated) }, enabled = updated.valid) { Text("Terapkan") } },
        dismissButton = { TextButton(onClick = close) { Text("Batal") } },
    )
}
