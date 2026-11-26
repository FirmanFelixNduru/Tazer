package com.tazer.taskorganizer.ui

import android.content.ClipboardManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tazer.taskorganizer.data.TaskPackage
import java.time.ZoneId

@Composable
internal fun ImportPackageButton(enabled: Boolean, model: TazerViewModel) {
    val context = LocalContext.current
    var open by rememberSaveable { mutableStateOf(false) }
    var text by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    PrimaryButton(onClick = { open = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        TazerIcon(TazerGlyph.Share)
        Text("Impor Paket Tugas", Modifier.padding(start = 10.dp))
    }
    if (!open) return
    AlertDialog(
        onDismissRequest = { open = false },
        title = { Text("Impor paket tugas") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tempel pesan dari teman kelompok. Tazer mencari kode TAZER-TASK-V1 atau tautan tazer://import-task.",
                    style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(text, { text = it.take(48_000); error = null }, Modifier.fillMaxWidth(), minLines = 3, maxLines = 6,
                    label = { Text("Teks atau kode paket") }, isError = error != null,
                    supportingText = error?.let { message -> { Text(message) } })
                TextButton(onClick = {
                    val clip = context.getSystemService(ClipboardManager::class.java).primaryClip
                    clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.let {
                        text = it.toString().take(48_000)
                        error = null
                    }
                }) { Text("Tempel dari clipboard") }
            }
        },
        confirmButton = {
            PrimaryButton(onClick = {
                error = model.reviewPackage(text)
                if (error == null) { open = false; text = "" }
            }, enabled = text.isNotBlank()) {
                Text("Pratinjau")
            }
        },
        dismissButton = { TextButton(onClick = { open = false }) { Text("Batal") } },
    )
}

/** Full preview of a shared task; nothing is saved until the user confirms. */
@Composable
internal fun PackageReviewDialog(pack: TaskPackage, zone: ZoneId, busy: Boolean, onSave: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Pratinjau paket tugas") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(pack.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                CourseChip(pack.course)
                Text("Deadline " + formatDate(pack.deadlineAt, zone))
                Text("Kesulitan ${pack.difficultyScore}/5 · Estimasi ${pack.estimatedMinutes} menit")
                if (pack.description.isNotBlank()) {
                    Text(pack.description, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (pack.subtasks.isNotEmpty()) {
                    Text("Subtask (${pack.subtasks.size})", style = MaterialTheme.typography.titleSmall)
                    pack.subtasks.forEachIndexed { index, step -> Text("${index + 1}. $step") }
                }
                Text("Paket hanya berisi detail tugas. Progres dan data akun teman tidak ikut.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { PrimaryButton(onClick = onSave, enabled = !busy) { Text("Simpan tugas") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Batal") } },
    )
}
