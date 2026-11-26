package com.tazer.taskorganizer.ui

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.tazer.taskorganizer.data.IcsImportPreview
import java.io.IOException
import java.nio.charset.CharacterCodingException
import kotlinx.coroutines.CancellationException

@Composable
internal fun ImportIcsButton(
    ownerUid: String,
    busy: Boolean,
    model: TazerViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var document by rememberSaveable { mutableStateOf<Uri?>(null) }
    var preview by remember(document) { mutableStateOf<IcsImportPreview?>(null) }
    var error by remember(document) { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        document = uri
    }
    val selected = document
    LaunchedEffect(selected) {
        if (selected == null) return@LaunchedEffect
        try {
            preview = model.previewIcs(ownerUid) {
                context.contentResolver.openInputStream(selected) ?: throw IOException("No document stream")
            }
        } catch (failure: CancellationException) {
            throw failure
        } catch (_: CharacterCodingException) {
            error = "File .ics harus memakai encoding UTF-8."
        } catch (_: IllegalArgumentException) {
            error = "Pilih file kalender .ics yang valid dengan ukuran maksimal 2 MB dan hingga 2.500 entri impor."
        } catch (_: Exception) {
            error = "File kalender tidak dapat dibaca. Pilih kembali file .ics."
        }
    }
    OutlinedButton(
        onClick = {
            try {
                // Some file providers advertise .ics as text/plain or application/octet-stream.
                picker.launch(arrayOf("*/*"))
            } catch (_: ActivityNotFoundException) {
                model.showMessage("Pemilih file tidak tersedia di perangkat ini.")
            }
        },
        enabled = !busy && selected == null,
        modifier = modifier.testTag("import-ics"),
    ) { Text(if (selected != null && preview == null && error == null) "Membaca .ics…" else "Impor .ics") }

    val ready = preview
    if (ready != null) {
        val counts = ready.counts
        AlertDialog(
            onDismissRequest = { if (!busy) document = null },
            title = { Text("Pratinjau impor .ics") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${counts.events} acara kalender baru")
                    Text("${counts.weeklyBlocks} blok jadwal tetap baru")
                    if (counts.duplicates > 0) Text("${counts.duplicates} duplikat dilewati")
                    if (ready.invalid > 0) Text("${ready.invalid} acara tidak valid")
                    if (ready.unsupported > 0) Text("${ready.unsupported} acara memakai zona waktu atau aturan pengulangan yang belum didukung")
                    if (ready.cancelled > 0) Text("${ready.cancelled} acara berstatus batal dilewati")
                    if (counts.weeklyBlocks > 0) {
                        Text("Blok jadwal tetap memakai zona profil (${ready.zoneId}) dan berlaku setiap minggu tanpa tanggal mulai. Blok menyimpan judul dan jam.")
                    }
                }
            },
            confirmButton = {
                if (counts.totalNew > 0) {
                    Button(
                        onClick = { model.importIcs(ready) { document = null } },
                        enabled = !busy,
                    ) { Text("Impor") }
                }
            },
            dismissButton = {
                TextButton(onClick = { document = null }, enabled = !busy) {
                    Text(if (counts.totalNew > 0) "Batal" else "Tutup")
                }
            },
        )
    } else if (error != null) {
        AlertDialog(
            onDismissRequest = { document = null },
            title = { Text("Impor .ics gagal") },
            text = { Text(requireNotNull(error)) },
            confirmButton = { TextButton(onClick = { document = null }) { Text("Tutup") } },
        )
    }
}
