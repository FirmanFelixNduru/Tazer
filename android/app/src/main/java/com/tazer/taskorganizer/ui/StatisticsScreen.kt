package com.tazer.taskorganizer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tazer.taskorganizer.data.TaskStatus
import com.tazer.taskorganizer.data.WorkSessionState
import com.tazer.taskorganizer.domain.DailyFocus
import com.tazer.taskorganizer.domain.StatisticsSession
import com.tazer.taskorganizer.domain.StatisticsTask
import com.tazer.taskorganizer.domain.weeklyStatistics
import java.text.NumberFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToLong

private val statisticsLocale = Locale.forLanguageTag("id-ID")
private val fullDate = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", statisticsLocale)
private val rangeDate = DateTimeFormatter.ofPattern("d MMM yyyy", statisticsLocale)

@Composable
internal fun StatisticsScreen(workspace: Workspace, state: TazerState, zone: ZoneId) {
    val ownerUid = workspace.profile.ownerUid
    val today = localDate(state.now, zone)
    var weeksAgo by rememberSaveable(ownerUid) { mutableIntStateOf(0) }
    val end = today.minusWeeks(weeksAgo.toLong())
    val tasks = remember(workspace.tasks, ownerUid) {
        workspace.tasks.filter { it.ownerUid == ownerUid }.map { task ->
            StatisticsTask(
                id = task.id,
                course = task.course,
                completedAt = if (task.status == TaskStatus.DONE) task.completedAt else null,
                deadlineAt = task.deadlineAt,
                initialEstimatedMinutes = task.initialEstimatedMinutes,
                analyzerBaselineMinutes = task.analyzerBaselineMinutes,
            )
        }
    }
    val sessions = remember(workspace.sessions, ownerUid) {
        workspace.sessions.filter { it.ownerUid == ownerUid }.map { session ->
            StatisticsSession(
                taskId = session.taskId,
                done = session.state == WorkSessionState.DONE,
                startedAt = session.startedAt,
                endedAt = session.endedAt,
                plannedEndAt = session.plannedEndAt,
                actualWorkMs = session.actualWorkMs,
            )
        }
    }
    val stats = remember(tasks, sessions, today, end, zone) { weeklyStatistics(tasks, sessions, today, end, zone) }
    val integerFormat = remember { NumberFormat.getIntegerInstance(statisticsLocale) }
    val paceFormat = remember {
        NumberFormat.getNumberInstance(statisticsLocale).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 2
        }
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedIconButton(onClick = { weeksAgo++ }) {
                    TazerIcon(TazerGlyph.Previous, "Minggu sebelumnya")
                }
                Text(
                    "${rangeDate.format(stats.days.first().date)} – ${rangeDate.format(end)}",
                    modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                )
                OutlinedIconButton(onClick = { weeksAgo-- }, enabled = weeksAgo > 0) {
                    TazerIcon(TazerGlyph.Next, "Minggu berikutnya")
                }
            }
        }
        item {
            Column(Modifier.padding(top = 16.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatisticsHeading("Fokus harian")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        if (stats.totalFocusMs > 0) "Total ${duration(stats.totalFocusMs, integerFormat)}"
                        else "Belum ada sesi fokus",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${integerFormat.format(stats.completedTaskCount)} tugas selesai",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End,
                    )
                }
                DailyFocusChart(stats.days, integerFormat)
            }
        }
        item {
            val accuracy = stats.accuracy
            HorizontalDivider()
            Column(Modifier.padding(vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Statistic(
                    label = "Akurasi estimasi",
                    value = accuracy?.let { "${it.percent}%" },
                    detail = accuracy?.let {
                        "${integerFormat.format(it.taskCount)} tugas · " +
                            "${duration(it.estimatedMinutes * 60_000, integerFormat)} estimasi · " +
                            "${duration(it.actualWorkMs, integerFormat)} aktual"
                    } ?: "Perlu tugas selesai dengan waktu fokus tercatat.",
                )
                Statistic(
                    label = "Faktor ritme",
                    value = stats.paceFactor?.let { "${paceFormat.format(it)}×" },
                    detail = if (stats.paceFactor == null) "Perlu 3 tugas selesai dengan waktu fokus tercatat."
                        else "Median durasi aktual dibanding estimasi dasar.",
                )
                Statistic(
                    label = "Streak tepat waktu",
                    value = "${integerFormat.format(stats.onTimeStreakDays)} hari",
                    detail = "Hari berurutan dengan setidaknya satu tugas selesai tepat waktu.",
                )
            }
        }
        item {
            HorizontalDivider()
            Column(Modifier.padding(top = 20.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatisticsHeading("Fokus per mata kuliah")
                if (stats.courses.isEmpty()) {
                    Text(
                        "Durasi per mata kuliah muncul setelah sesi fokus tercatat.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        items(stats.courses, key = { it.course }) { course ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp).semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(course.course, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Text(duration(course.workMs, integerFormat), style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.End, modifier = Modifier.weight(0.65f))
            }
        }
    }
}

@Composable
private fun StatisticsHeading(label: String) {
    Text(label, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun Statistic(label: String, value: String?, detail: String) {
    Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        val typography = MaterialTheme.typography
        Text(if (value == null) label else "$label · $value", style = typography.titleSmall)
        Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, style = typography.bodyMedium)
    }
}

@Composable
private fun DailyFocusChart(days: List<DailyFocus>, integerFormat: NumberFormat) {
    val scheme = MaterialTheme.colorScheme
    val peakMs = days.maxOf(DailyFocus::workMs)
    val labelStyle = MaterialTheme.typography.labelSmall
    Canvas(Modifier.fillMaxWidth().height(if (peakMs > 0) 120.dp else 24.dp)) {
        val columnWidth = size.width / days.size
        val barWidth = (columnWidth * 0.58f).coerceAtMost(36.dp.toPx())
        val baseline = size.height - 1.dp.toPx()
        drawLine(scheme.outlineVariant, Offset(0f, baseline), Offset(size.width, baseline), strokeWidth = 1.dp.toPx())
        days.forEachIndexed { index, day ->
            if (day.workMs > 0) {
                val height = (day.workMs.toDouble() / peakMs * baseline).toFloat().coerceAtLeast(2.dp.toPx())
                drawRoundRect(
                    color = scheme.primary,
                    topLeft = Offset(columnWidth * (index + 0.5f) - barWidth / 2, baseline - height),
                    size = Size(barWidth, height),
                    cornerRadius = CornerRadius(minOf(4.dp.toPx(), height / 2)),
                )
            }
        }
    }
    Row(Modifier.fillMaxWidth()) {
        days.forEach { day ->
            val accessibleValue = "${fullDate.format(day.date)}, ${duration(day.workMs, integerFormat)}"
            val minutes = if (day.workMs in 1L..<60_000L) "<1" else integerFormat.format(toMinutes(day.workMs))
            Column(
                Modifier.weight(1f).clearAndSetSemantics { contentDescription = accessibleValue },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, statisticsLocale), style = labelStyle,
                    textAlign = TextAlign.Center)
                Text(minutes, style = labelStyle, textAlign = TextAlign.Center)
            }
        }
    }
    Text("Dalam menit", style = labelStyle, color = scheme.onSurfaceVariant)
}

private fun toMinutes(workMs: Long): Long = (workMs / 60_000.0).roundToLong()

private fun duration(workMs: Long, integerFormat: NumberFormat): String {
    if (workMs in 1L..<60_000L) return "Kurang dari 1 menit"
    val minutes = toMinutes(workMs)
    val hours = minutes / 60
    val remainder = minutes % 60
    return when {
        hours == 0L -> "${integerFormat.format(minutes)} menit"
        remainder == 0L -> "${integerFormat.format(hours)} jam"
        else -> "${integerFormat.format(hours)} jam ${integerFormat.format(remainder)} menit"
    }
}
