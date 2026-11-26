package com.tazer.taskorganizer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tazer.taskorganizer.data.Subtask
import com.tazer.taskorganizer.data.SubtaskDraft
import java.util.UUID

internal const val MaxSubtasks = 40
internal const val MaxSubtaskTitleLength = 120

internal data class ChecklistItem(val key: String, val draft: SubtaskDraft) {
    constructor(title: String) : this(UUID.randomUUID().toString(), SubtaskDraft(null, title, false))
}

internal fun Subtask.checklistItem() = ChecklistItem(id, asDraft())

internal val ChecklistSaver = Saver<List<ChecklistItem>, ArrayList<String>>(
    save = { items ->
        ArrayList(items.flatMap { (key, draft) ->
            listOf(key, draft.id.orEmpty(), draft.title, draft.completed.toString())
        })
    },
    restore = { values ->
        values.chunked(4).map { (key, id, title, completed) ->
            ChecklistItem(key, SubtaskDraft(id.ifEmpty { null }, title, completed.toBoolean()))
        }
    },
)

@Composable
internal fun SubtaskProgress(completed: Int, total: Int) {
    if (total > 0) {
        Text(
            "$completed dari $total subtask selesai",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
internal fun SubtaskEditor(
    items: List<ChecklistItem>,
    enabled: Boolean,
    activeKey: String?,
    onEditKey: (String?) -> Unit,
    onChecked: (ChecklistItem, Boolean) -> Unit,
    onSave: (ChecklistItem?, String, () -> Unit) -> Unit,
    onDelete: (ChecklistItem) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    val closeEditor = {
        onEditKey(null)
        title = ""
    }
    val edit = { item: ChecklistItem? ->
        title = item?.draft?.title.orEmpty()
        onEditKey(item?.key.orEmpty())
    }
    val canEditOther = enabled && activeKey == null
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Subtask", style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
        SubtaskProgress(items.count { it.draft.completed }, items.size)
        items.forEach { item ->
            key(item.key) {
                if (activeKey == item.key) {
                    SubtaskTitleEditor(title, enabled, { title = it }, closeEditor) {
                        onSave(item, it, closeEditor)
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            Modifier.weight(1f).heightIn(min = 48.dp).toggleable(
                                value = item.draft.completed,
                                enabled = enabled,
                                role = Role.Checkbox,
                                onValueChange = { onChecked(item, it) },
                            ),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = item.draft.completed, onCheckedChange = null, enabled = enabled)
                            Text(item.draft.title, style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(enabled = canEditOther, onClick = { edit(item) }) {
                            TazerIcon(TazerGlyph.Edit, "Edit subtask ${item.draft.title}")
                        }
                        IconButton(enabled = canEditOther, onClick = { onDelete(item) }) {
                            TazerIcon(TazerGlyph.Delete, "Hapus subtask ${item.draft.title}")
                        }
                    }
                }
            }
        }
        if (activeKey == "") {
            SubtaskTitleEditor(title, enabled && items.size < MaxSubtasks, { title = it }, closeEditor) {
                onSave(null, it, closeEditor)
            }
        } else if (items.size < MaxSubtasks) {
            OutlinedButton(onClick = { edit(null) }, enabled = canEditOther) { Text("Tambah subtask") }
        } else {
            Text("Maksimal $MaxSubtasks subtask per tugas.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubtaskTitleEditor(
    title: String,
    enabled: Boolean,
    onTitleChange: (String) -> Unit,
    cancel: () -> Unit,
    save: (String) -> Unit,
) {
    val requester = remember { FocusRequester() }
    LaunchedEffect(requester) { requester.requestFocus() }
    val trimmed = title.trim()
    val tooLong = trimmed.length > MaxSubtaskTitleLength
    OutlinedTextField(
        title,
        onTitleChange,
        modifier = Modifier.fillMaxWidth().focusRequester(requester),
        label = { Text("Judul subtask") },
        supportingText = if (tooLong) { { Text("Maksimal $MaxSubtaskTitleLength karakter.") } } else null,
        isError = tooLong,
        singleLine = true,
        enabled = enabled,
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PrimaryButton(onClick = { save(trimmed) }, enabled = enabled && trimmed.isNotEmpty() && !tooLong) {
            Text("Simpan subtask")
        }
        OutlinedButton(onClick = cancel) { Text("Batal") }
    }
}
