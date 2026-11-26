package com.tazer.taskorganizer.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.tazer.taskorganizer.domain.CourseColors
import com.tazer.taskorganizer.domain.TaskQuery

@Composable
internal fun TaskFilterBar(query: TaskQuery, courses: List<String>, onQuery: (TaskQuery) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query.text,
            onValueChange = { onQuery(query.copy(text = it.take(120))) },
            modifier = Modifier.fillMaxWidth().neumorphicInset(neu(), 26.dp).semantics { contentDescription = "Cari tugas" },
            shape = RoundedCornerShape(26.dp),
            placeholder = { Text("Cari judul, mata kuliah, atau deskripsi") },
            leadingIcon = { TazerIcon(TazerGlyph.Search) },
            trailingIcon = if (query.text.isNotEmpty()) {
                { TextButton(onClick = { onQuery(query.copy(text = "")) }) { Text("Hapus") } }
            } else null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        )
        if (courses.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                courses.forEach { course ->
                    val selected = query.course?.let(CourseColors::key) == CourseColors.key(course)
                    FilterChip(
                        selected = selected,
                        onClick = { onQuery(query.copy(course = if (selected) null else course)) },
                        label = { Text(course) },
                        leadingIcon = { CourseDot(course) },
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Kesulitan", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            (1..5).forEach { level ->
                val selected = query.difficulty == level
                FilterChip(
                    selected = selected,
                    onClick = { onQuery(query.copy(difficulty = if (selected) null else level)) },
                    label = { Text("$level") },
                    modifier = Modifier.semantics { contentDescription = "Kesulitan $level" },
                )
            }
            if (query.active) {
                OutlinedButton(onClick = { onQuery(TaskQuery()) }) { Text("Reset filter") }
            }
        }
    }
}

/** Empty state that says why nothing shows and offers the way back. */
@Composable
internal fun FilteredEmpty(archived: Boolean, reset: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Tidak ada tugas ${if (archived) "selesai" else "aktif"} yang cocok",
            style = MaterialTheme.typography.titleMedium)
        Text("Coba kata kunci lain atau lepas filter mata kuliah dan kesulitan.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = reset) { Text("Reset filter") }
    }
}
