package com.tazer.taskorganizer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tazer.taskorganizer.domain.CourseColors
import com.tazer.taskorganizer.domain.CourseTone

/** Resolves a course to its colours for the current theme and the account's overrides. */
internal class CoursePalette(private val overrides: Map<String, Int>, private val dark: Boolean) {
    fun tone(course: String): CourseTone = CourseColors.swatch(course, overrides).tone(dark)
    fun index(course: String) = CourseColors.index(course, overrides)
}

internal val LocalCoursePalette = staticCompositionLocalOf { CoursePalette(emptyMap(), false) }

private fun color(argb: Long) = Color(argb.toInt())

/** Course name on its own colour, the same in task cards, schedule rows, and filters. */
@Composable
internal fun CourseChip(course: String, modifier: Modifier = Modifier) {
    val tone = LocalCoursePalette.current.tone(course)
    Surface(modifier, shape = RoundedCornerShape(8.dp), color = color(tone.container), contentColor = color(tone.ink)) {
        Row(Modifier.padding(horizontal = 9.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(color(tone.accent), CircleShape))
            Text(course, Modifier.padding(start = 6.dp), style = MaterialTheme.typography.labelMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun CourseDot(course: String, modifier: Modifier = Modifier, size: Int = 10) {
    Box(modifier.size(size.dp).background(color(LocalCoursePalette.current.tone(course).accent), CircleShape))
}

@Composable
internal fun courseContainer(course: String): Pair<Color, Color> {
    val tone = LocalCoursePalette.current.tone(course)
    return color(tone.container) to color(tone.ink)
}

/** Chip colours for a palette slot chosen directly, such as an event's calendar marker. */
@Composable
internal fun paletteContainer(index: Int): Pair<Color, Color> {
    val tone = CourseColors.palette[index.coerceIn(CourseColors.palette.indices)].tone(LocalTazerDark.current)
    return color(tone.container) to color(tone.ink)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CourseColorSetting(courses: List<String>, choose: (String, Int?) -> Unit) {
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Warna mata kuliah", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        Text("Tiap mata kuliah otomatis mendapat warna tetap di Tugas, Jadwal, Kalender, dan Statistik. Ketuk untuk menggantinya.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (courses.isEmpty()) {
            Text("Warna muncul setelah kamu menambahkan tugas.", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            courses.forEach { course ->
                CourseChip(course, Modifier.clickable(onClickLabel = "Ganti warna $course") { editing = course })
            }
        }
    }
    val course = editing ?: return
    val palette = LocalCoursePalette.current
    val dark = LocalTazerDark.current
    AlertDialog(
        onDismissRequest = { editing = null },
        title = { Text("Warna $course", maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CourseColors.palette.forEachIndexed { index, swatch ->
                    val tone = swatch.tone(dark)
                    val chosen = palette.index(course) == index
                    Box(
                        Modifier.size(44.dp)
                            .background(color(tone.container), CircleShape)
                            .border(if (chosen) 3.dp else 1.dp, if (chosen) color(tone.ink) else MaterialTheme.colorScheme.outline, CircleShape)
                            .clickable(role = Role.RadioButton) { choose(course, index); editing = null }
                            .semantics { contentDescription = swatch.name; selected = chosen },
                        contentAlignment = Alignment.Center,
                    ) { Box(Modifier.size(14.dp).background(color(tone.accent), CircleShape)) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { editing = null }) { Text("Selesai") } },
        dismissButton = {
            TextButton(onClick = { choose(course, null); editing = null }) { Text("Otomatis") }
        },
    )
}
