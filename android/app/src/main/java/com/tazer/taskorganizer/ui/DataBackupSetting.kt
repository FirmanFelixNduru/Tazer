package com.tazer.taskorganizer.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tazer.taskorganizer.data.BackupPreview
import com.tazer.taskorganizer.data.TazerBackup
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private suspend fun writeDocument(context: Context, uri: Uri, text: String) = withContext(Dispatchers.IO) {
    val stream = context.contentResolver.openOutputStream(uri, "wt") ?: throw IOException("No output stream")
    stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
}

private suspend fun readDocument(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    val stream = context.contentResolver.openInputStream(uri) ?: throw IOException("No input stream")
    val output = java.io.ByteArrayOutputStream()
    stream.use { input ->
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            output.write(buffer, 0, count)
            require(output.size() <= TazerBackup.MAX_BYTES) { "Backup lebih dari 8 MB." }
        }
    }
    val bytes = output.toByteArray()
    Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString().removePrefix("\uFEFF")
}

/** Saves the study plan, weekly blocks, and calendar events as an .ics file chosen through SAF. */
@Composable
internal fun ExportIcsButton(model: TazerViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/calendar")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                writeDocument(context, uri, model.exportIcs())
                model.showMessage("Jadwal belajar diekspor ke .ics.")
            } catch (failure: CancellationException) {
                throw failure
            } catch (_: Exception) {
                model.showMessage("File .ics belum tersimpan. Pilih lokasi lain lalu coba lagi.")
            }
        }
    }
    OutlinedButton(onClick = {
        try {
            create.launch("tazer-jadwal-${LocalDate.now()}.ics")
        } catch (_: ActivityNotFoundException) {
            model.showMessage("Pemilih file tidak tersedia di perangkat ini.")
        }
    }, modifier = modifier) { Text("Ekspor .ics") }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DataBackupSetting(busy: Boolean, model: TazerViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var preview by remember { mutableStateOf<BackupPreview?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                writeDocument(context, uri, model.exportBackup())
                model.showMessage("Backup JSON tersimpan. File ini tidak memuat sesi login atau kunci API.")
            } catch (failure: CancellationException) {
                throw failure
            } catch (_: Exception) {
                model.showMessage("Backup belum tersimpan. Pilih lokasi lain lalu coba lagi.")
            }
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                preview = model.previewBackup(readDocument(context, uri))
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: IllegalArgumentException) {
                error = failure.message?.takeIf { it.endsWith('.') } ?: "File ini bukan backup Tazer yang valid."
            } catch (_: Exception) {
                error = "File backup tidak dapat dibaca. Pilih kembali file .json."
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Data & backup", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        Text("Ekspor jadwal ke Google Calendar lewat .ics, atau simpan backup JSON untuk dipulihkan di perangkat lain. " +
            "Backup tidak memuat sesi login, token, maupun kunci API.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ExportIcsButton(model)
            OutlinedButton(onClick = {
                try { create.launch("tazer-backup-${LocalDate.now()}.json") } catch (_: ActivityNotFoundException) {
                    model.showMessage("Pemilih file tidak tersedia di perangkat ini.")
                }
            }, enabled = !busy) { Text("Ekspor Backup JSON") }
            OutlinedButton(onClick = {
                try { open.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) } catch (_: ActivityNotFoundException) {
                    model.showMessage("Pemilih file tidak tersedia di perangkat ini.")
                }
            }, enabled = !busy) { Text("Impor Backup JSON") }
        }
    }
    preview?.let { ready ->
        val counts = ready.counts
        AlertDialog(
            onDismissRequest = { if (!busy) preview = null },
            title = { Text("Pratinjau backup") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${counts.tasks} tugas baru")
                    Text("${counts.subtasks} subtask baru")
                    Text("${counts.fixedSchedules} jadwal tetap baru")
                    Text("${counts.events} acara kalender baru")
                    Text("${counts.sessions} riwayat sesi fokus baru")
                    if (counts.duplicates > 0) Text("${counts.duplicates} item sudah ada dan dilewati")
                    if (ready.data.courseColors.isNotEmpty()) Text("${ready.data.courseColors.size} pilihan warna mata kuliah")
                    if (ready.profileChanges) {
                        val profile = requireNotNull(ready.data.profile)
                        Text("Jam aktif dan zona waktu diganti ke ${formatMinute(profile.activeStartMinute)}–" +
                            "${formatMinute(profile.activeEndMinute)} (${profile.zoneId}).")
                    }
                    Text("Akun yang sedang masuk tetap sama. Item baru ikut Cloud Sync seperti biasa.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                if (counts.totalNew > 0 || ready.profileChanges) {
                    PrimaryButton(onClick = { model.restoreBackup(ready) { preview = null } }, enabled = !busy) { Text("Pulihkan") }
                }
            },
            dismissButton = {
                TextButton(onClick = { preview = null }, enabled = !busy) {
                    Text(if (counts.totalNew > 0 || ready.profileChanges) "Batal" else "Tutup")
                }
            },
        )
    }
    error?.let { message ->
        AlertDialog(
            onDismissRequest = { error = null },
            title = { Text("Impor backup gagal") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { error = null }) { Text("Tutup") } },
        )
    }
}
