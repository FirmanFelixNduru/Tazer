package com.tazer.taskorganizer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tazer.taskorganizer.domain.CourseColors
import com.tazer.taskorganizer.domain.DeadlineRange
import com.tazer.taskorganizer.domain.DurationRange
import com.tazer.taskorganizer.domain.TaskQuery
import com.tazer.taskorganizer.domain.TaskSort

/** Keeps the search text and every Filter panel choice across rotation and process death. */
internal val TaskQuerySaver = Saver<TaskQuery, ArrayList<Any>>(
    save = {
        arrayListOf(it.text, ArrayList(it.courses), ArrayList(it.difficulties), it.deadline.name, it.duration.name,
            it.sort.name, it.openSubtasksOnly)
    },
    restore = {
        @Suppress("UNCHECKED_CAST")
        TaskQuery(
            it[0] as String, (it[1] as List<String>).toSet(), (it[2] as List<Int>).toSet(),
            DeadlineRange.valueOf(it[3] as String), DurationRange.valueOf(it[4] as String),
            TaskSort.valueOf(it[5] as String), it[6] as Boolean,
        )
    },
)

/** A slim search pill and the Filter button; the rich filters live in [TaskFilterSheet]. */
@Composable
internal fun TaskFilterBar(
    query: TaskQuery, courses: List<Pair<String, Int>>, countFor: (TaskQuery) -> Int, onQuery: (TaskQuery) -> Unit,
) {
    var panel by rememberSaveable { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        SearchPill(query.text, { onQuery(query.copy(text = it.take(120))) }, Modifier.weight(1f))
        FilterButton(query.filterCount) { panel = true }
    }
    if (panel) TaskFilterSheet(query, courses, countFor, onApply = { onQuery(it); panel = false }) { panel = false }
}

/** 44 dp recessed pill: smaller placeholder, but the typed text keeps the normal body size. */
@Composable
private fun SearchPill(text: String, onText: (String) -> Unit, modifier: Modifier) {
    val neu = neu()
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier.heightIn(min = 44.dp).neumorphicInset(neu, 22.dp)
            .border(if (focused) 2.dp else 1.dp, if (focused) neu.accent else neu.outline, shape)
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides neu.muted) { TazerIcon(TazerGlyph.Search) }
        BasicTextField(
            value = text, onValueChange = onText, singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = neu.ink),
            cursorBrush = SolidColor(neu.accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 10.dp)
                .onFocusChanged { focused = it.isFocused }.semantics { contentDescription = "Cari tugas" },
            decorationBox = { field ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (text.isEmpty()) Text(
                        "Cari judul, matkul, deskripsi", color = neu.muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    field()
                }
            },
        )
        if (text.isNotEmpty()) TextButton(onClick = { onText("") }) { Text("Hapus") }
    }
}

/** Surface pill with a clear border; turns royal blue with a count badge while any filter is on. */
@Composable
private fun FilterButton(count: Int, onClick: () -> Unit) {
    val neu = neu()
    val shape = RoundedCornerShape(22.dp)
    val on = count > 0
    val ink = if (on) neu.onFill else neu.ink
    Row(
        Modifier.heightIn(min = 44.dp).neumorphicRaised(neu, 22.dp, 3.dp, if (on) neu.fill else neu.surface)
            .background(if (on) neu.fill else neu.surface, shape).border(1.dp, if (on) neu.fill else neu.outline, shape)
            .clip(shape).clickable(role = Role.Button, onClick = onClick)
            .semantics { if (on) contentDescription = "Filter, $count aktif" }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CompositionLocalProvider(LocalContentColor provides ink) { TazerIcon(TazerGlyph.FilterList) }
        Text("Filter", color = ink, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
        if (on) Box(
            Modifier.sizeIn(minWidth = 22.dp, minHeight = 22.dp).background(neu.onFill, CircleShape).padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center,
        ) { Text("$count", color = neu.fill, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)) }
    }
}

/**
 * The Filter panel (docs/references/task-filter-dropdown-reference.jpg in blue-white neumorphic): a draft the
 * user edits freely, a live result count on the primary button, and Reset that keeps the typed search.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TaskFilterSheet(
    applied: TaskQuery, courses: List<Pair<String, Int>>, countFor: (TaskQuery) -> Int,
    onApply: (TaskQuery) -> Unit, close: () -> Unit,
) {
    val neu = neu()
    var draft by rememberSaveable(stateSaver = TaskQuerySaver) { mutableStateOf(applied) }
    val results = countFor(draft)
    ModalBottomSheet(close, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = neu.surface) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp)) {
            Text("Filter tugas", style = MaterialTheme.typography.titleLarge, color = neu.ink, modifier = Modifier.semantics { heading() })
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(top = 6.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (courses.isNotEmpty()) {
                    SheetHeading("Mata Kuliah")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        courses.forEach { (course, count) ->
                            val key = CourseColors.key(course)
                            val chosen = draft.courses.any { CourseColors.key(it) == key }
                            FilterPill("$course ($count)", chosen, "Mata kuliah $course", multi = true, dot = course) {
                                draft = draft.copy(courses = if (chosen) draft.courses.filterNot { CourseColors.key(it) == key }.toSet()
                                    else draft.courses + course)
                            }
                        }
                    }
                }
                SheetHeading("Tingkat Kesulitan")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..5).forEach { level ->
                        val chosen = level in draft.difficulties
                        FilterPill("$level", chosen, "Kesulitan $level", multi = true, modifier = Modifier.weight(1f)) {
                            draft = draft.copy(difficulties = if (chosen) draft.difficulties - level else draft.difficulties + level)
                        }
                    }
                }
                SheetHeading("Rentang Deadline")
                FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DeadlineRange.entries.forEach { range ->
                        FilterPill(range.label, draft.deadline == range, "Deadline ${range.label}") { draft = draft.copy(deadline = range) }
                    }
                }
                SheetHeading("Estimasi Durasi")
                FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DurationRange.entries.forEach { range ->
                        FilterPill(range.label, draft.duration == range, "Durasi ${range.label}") { draft = draft.copy(duration = range) }
                    }
                }
                SheetHeading("Urutkan")
                Column(Modifier.selectableGroup()) {
                    TaskSort.entries.forEach { sort ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 44.dp)
                                .selectable(draft.sort == sort, role = Role.RadioButton) { draft = draft.copy(sort = sort) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(draft.sort == sort, onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = neu.fill, unselectedColor = neu.outline))
                            Text(sort.label, Modifier.padding(start = 12.dp), color = neu.ink)
                        }
                    }
                }
                SheetHeading("Opsi Tambahan")
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 44.dp)
                        .toggleable(draft.openSubtasksOnly, role = Role.Checkbox) { draft = draft.copy(openSubtasksOnly = it) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(draft.openSubtasksOnly, onCheckedChange = null,
                        colors = CheckboxDefaults.colors(checkedColor = neu.fill, checkmarkColor = neu.onFill, uncheckedColor = neu.outline))
                    Text("Hanya yang punya sub-tugas belum selesai", Modifier.padding(start = 12.dp), color = neu.ink)
                }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryButton(onClick = { onApply(draft) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Text("Tampilkan $results Tugas")
                }
                OutlinedButton(onClick = { draft = draft.withoutFilters() }, enabled = draft.filterCount > 0,
                    modifier = Modifier.heightIn(min = 48.dp)) { Text("Reset") }
            }
        }
    }
}

@Composable
private fun SheetHeading(label: String) {
    Spacer(Modifier.height(6.dp))
    Text(label, style = MaterialTheme.typography.titleSmall, color = neu().ink, modifier = Modifier.semantics { heading() })
}

/** Neumorphic pill: royal-blue fill with white text when chosen, outlined surface otherwise. */
@Composable
private fun FilterPill(
    label: String, selected: Boolean, description: String, modifier: Modifier = Modifier, multi: Boolean = false,
    dot: String? = null, onClick: () -> Unit,
) {
    val neu = neu()
    val shape = RoundedCornerShape(20.dp)
    val choose = if (multi) Modifier.toggleable(selected, role = Role.Checkbox) { onClick() }
        else Modifier.selectable(selected, role = Role.RadioButton, onClick = onClick)
    Row(
        modifier.heightIn(min = 40.dp)
            .then(if (selected) Modifier.neumorphicRaised(neu, 20.dp, 2.dp, neu.fill) else Modifier)
            .background(if (selected) neu.fill else neu.surface, shape)
            .border(1.dp, if (selected) neu.fill else neu.outline, shape).clip(shape).then(choose)
            .semantics { contentDescription = description }.padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        if (dot != null) {
            CourseDot(dot)
            Spacer(Modifier.width(8.dp))
        }
        Text(label, color = if (selected) neu.onFill else neu.ink, maxLines = 1,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium))
    }
}

/** Empty state that says why nothing shows and offers the way back. */
@Composable
internal fun FilteredEmpty(archived: Boolean, reset: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Tidak ada tugas ${if (archived) "selesai" else "aktif"} yang cocok",
            style = MaterialTheme.typography.titleMedium)
        Text("Coba kata kunci lain atau lepas sebagian filter.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = reset) { Text("Reset filter") }
    }
}
