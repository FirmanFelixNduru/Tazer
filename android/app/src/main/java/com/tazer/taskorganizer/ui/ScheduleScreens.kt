package com.tazer.taskorganizer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tazer.taskorganizer.data.FixedScheduleKind
import com.tazer.taskorganizer.data.UserProfile
import com.tazer.taskorganizer.data.WorkSession
import com.tazer.taskorganizer.data.WorkSessionState
import com.tazer.taskorganizer.domain.ScheduleRule
import com.tazer.taskorganizer.domain.FocusPreset
import com.tazer.taskorganizer.domain.Scheduler
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import kotlin.math.abs
import kotlinx.coroutines.coroutineScope
import com.tazer.taskorganizer.domain.TaskSearch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

private data class TimelineRow(
    val id: String, val title: String, val kind: String, val start: Long, val end: Long,
    val session: WorkSession? = null, val course: String? = null, val description: String? = null,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TimelineScreen(
    workspace: Workspace, state: TazerState, zone: ZoneId, day: LocalDate, selectDay: (LocalDate) -> Unit,
    openFocus: (WorkSession) -> Unit, defer: () -> Unit,
    focusPreset: FocusPreset,
) {
    val today = localDate(state.now, zone)
    val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
    val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val rows = remember(workspace, day, zone, today) {
        val fixedById = workspace.fixed.associateBy { it.id }
        val tasksById = workspace.tasks.associateBy { it.id }
        val rules = workspace.fixed.map { ScheduleRule(it.id, it.label, it.dayOfWeek, it.startMinute, it.endMinute) }
        val fixedRows = Scheduler.fixedBlocksForDay(rules, day, zone).map { (rule, block) ->
            val kind = if (fixedById.getValue(rule.id).kind == FixedScheduleKind.BREAK) "Istirahat" else "Jadwal tetap"
            TimelineRow("fixed-" + rule.id, rule.label, kind, block.startAt, block.endAt)
        }
        val workRows = workspace.sessions.asSequence().filter {
            (it.state == WorkSessionState.PENDING || it.state in ActiveFocusStates) &&
                (it.plannedStartAt < end && it.plannedEndAt > start || it.state in ActiveFocusStates && day == today)
        }.mapNotNull { session ->
            val task = tasksById[session.taskId] ?: return@mapNotNull null
            val startAt = session.startedAt ?: session.plannedStartAt
            val duration = (session.cutoffElapsedMs ?: 0L) - (session.activeSinceElapsedMs ?: 0L)
            val endAt = if (duration > 0) startAt + duration else session.plannedEndAt
            TimelineRow("work-" + session.id, task.title, "Sesi fokus", startAt, endAt, session, task.course, task.description)
        }.toList()
        (fixedRows + workRows).sortedWith(compareBy<TimelineRow> { it.start }.thenBy { it.id })
    }
    Column(Modifier.padding(top = 12.dp)) {
        DateSpinner(day, today, selectDay, Modifier.padding(horizontal = 20.dp))
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 12.dp)) {
            item {
                OutlinedButton(onClick = defer, enabled = !state.busy) { Text("Saya lagi sibuk") }
                FocusRhythm(focusPreset, Modifier.padding(top = 6.dp))
                if (state.risks.isNotEmpty()) Text(
                    "Risiko lewat deadline pada ${state.risks.size} tugas. Lihat tab Belum Selesai.",
                    color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp),
                )
                Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp)) {
                    Text("Jam", Modifier.width(68.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Jadwal", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (day > today.plusDays(6)) Text("Sesi fokus direncanakan hingga tujuh hari ke depan.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (rows.isEmpty()) item {
                Text("Tidak ada blok jadwal pada tanggal ini.", modifier = Modifier.padding(vertical = 24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(rows, key = { it.id }) { row ->
                TimelineEntry(row, start, end, state.now, zone, day == today, openFocus)
            }
        }
    }
}

private val SpinnerDateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Indonesian)

@Composable
private fun DateSpinner(day: LocalDate, today: LocalDate, selectDay: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    val startEpoch = rememberSaveable { day.toEpochDay() - 700_000 }
    val dayCount = 1_400_001
    val selectedIndex = (day.toEpochDay() - startEpoch).coerceIn(0, dayCount - 1L).toInt()
    val dates = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val latestSelectDay by rememberUpdatedState(selectDay)
    var navigationRequest by remember { mutableIntStateOf(0) }
    val request = navigationRequest

    fun navigateTo(date: LocalDate) {
        navigationRequest++
        selectDay(date)
    }

    LaunchedEffect(dates, selectedIndex, request) {
        val layout = dates.layoutInfo
        val viewportCenter = (layout.viewportStartOffset + layout.viewportEndOffset) / 2f
        val target = layout.visibleItemsInfo.firstOrNull { it.index == selectedIndex }
        coroutineScope {
            // A new drag can cancel the centering animation while the observer stays active.
            launch {
                when {
                    target == null -> dates.scrollToItem(selectedIndex)
                    abs(target.offset + target.size / 2f - viewportCenter) > 1f -> dates.animateScrollToItem(selectedIndex)
                }
            }
        }
        snapshotFlow {
            if (navigationRequest != request || dates.isScrollInProgress) null
            else {
                val settled = dates.layoutInfo
                val center = (settled.viewportStartOffset + settled.viewportEndOffset) / 2f
                settled.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2f - center) }?.index
            }
        }.filterNotNull().collect { index ->
            if (index != selectedIndex) latestSelectDay(LocalDate.ofEpochDay(startEpoch + index))
        }
    }

    val colors = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            MonthPicker(YearMonth.from(day)) { month -> navigateTo(month.atDay(day.dayOfMonth.coerceAtMost(month.lengthOfMonth()))) }
            FilledTonalButton(onClick = { navigateTo(today) }, border = BorderStroke(1.dp, colors.outline)) { Text("Hari Ini") }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            val cellWidth = 56.dp.coerceAtMost(maxWidth)
            LazyRow(
                modifier = Modifier.fillMaxWidth().testTag("schedule-date-spinner").selectableGroup()
                    .semantics { contentDescription = "Pilih tanggal" }
                    .drawWithCache {
                        val fade = 24.dp.toPx().coerceAtMost(size.width / 6f)
                        val surface = colors.surface
                        val clearSurface = surface.copy(alpha = 0f)
                        val left = Brush.horizontalGradient(listOf(surface, clearSurface), endX = fade)
                        val right = Brush.horizontalGradient(listOf(clearSurface, surface), startX = size.width - fade, endX = size.width)
                        onDrawWithContent {
                            drawContent()
                            if (fade > 0f) {
                                val edgeSize = Size(fade, size.height)
                                drawRect(left, size = edgeSize)
                                drawRect(right, topLeft = Offset(size.width - fade, 0f), size = edgeSize)
                            }
                        }
                    },
                state = dates,
                contentPadding = PaddingValues(horizontal = (maxWidth - cellWidth) / 2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                flingBehavior = rememberSnapFlingBehavior(dates, SnapPosition.Center),
            ) {
                items(dayCount, key = { startEpoch + it }) { index ->
                    val date = LocalDate.ofEpochDay(startEpoch + index)
                    val isSelected = date == day
                    Surface(
                        modifier = Modifier.width(cellWidth).heightIn(min = 48.dp).testTag("schedule-date-$date")
                            .selectable(selected = isSelected, role = Role.Button, onClick = { navigateTo(date) })
                            .semantics {
                                contentDescription = SpinnerDateFormat.format(date) + if (date == today) ", hari ini" else ""
                            },
                        shape = RoundedCornerShape(14.dp),
                        border = if (isSelected) null else BorderStroke(1.dp, colors.outline),
                        color = if (isSelected) colors.primary else Color.Transparent,
                        contentColor = if (isSelected) colors.onPrimary else colors.onSurface,
                    ) {
                        Column(
                            Modifier.clearAndSetSemantics { }.padding(vertical = 10.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(date.dayOfWeek.getDisplayName(TextStyle.SHORT, Indonesian), style = MaterialTheme.typography.labelMedium, maxLines = 1)
                            Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimelineEntry(row: TimelineRow, start: Long, end: Long, now: Long, zone: ZoneId, isToday: Boolean, openFocus: (WorkSession) -> Unit) {
    val active = row.session?.state in ActiveFocusStates
    val overtime = row.session?.state == WorkSessionState.OVERTIME
    val status = when {
        row.session?.state == WorkSessionState.PAUSED -> "Dijeda"
        overtime -> "Lewat waktu · Lembur"
        active -> "Sedang fokus"
        row.start > now -> {
            val minutes = ((row.start - now + 59_999) / 60_000)
            if (minutes >= 60) "Dimulai dalam ${minutes / 60} jam ${minutes % 60} menit" else "Dimulai dalam $minutes menit"
        }
        row.end > now -> "Sedang berlangsung"
        row.session != null -> "Waktu sesi terlewat"
        else -> "Jadwal berakhir"
    }
    Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.width(68.dp).padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(formatTime(maxOf(row.start, start), zone), style = MaterialTheme.typography.labelLarge)
            Text(if (row.end >= end) "24:00" else formatTime(row.end, zone), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(Modifier.padding(end = 10.dp).width(1.dp).height(100.dp).background(MaterialTheme.colorScheme.outlineVariant))
        NeuCard(Modifier.weight(1f), radius = 18.dp) {
            Column {
                val colors = MaterialTheme.colorScheme
                val banner = when {
                    overtime -> colors.tertiaryContainer
                    active -> colors.primaryContainer
                    else -> colors.secondaryContainer
                }
                val bannerText = when {
                    overtime -> colors.onTertiaryContainer
                    active -> colors.onPrimaryContainer
                    else -> colors.onSecondaryContainer
                }
                Text(status, Modifier.fillMaxWidth().background(banner).padding(horizontal = 16.dp, vertical = 10.dp), color = bannerText, style = MaterialTheme.typography.labelLarge)
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(row.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).semantics { heading() })
                        if (row.session != null && (active || isToday && row.end > now)) {
                            TimerIconButton(if (active) "Buka timer aktif" else "Buka timer") { openFocus(row.session) }
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ScheduleTag(row.kind)
                        row.course?.takeIf { it.isNotBlank() }?.let { CourseChip(it) }
                        ScheduleTag("${((row.end - row.start) / 60_000).coerceAtLeast(1)} menit")
                    }
                    row.description?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleTag(label: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
        Text(label, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsScreen(
    workspace: Workspace, state: TazerState, model: TazerViewModel, theme: TazerThemeMode,
    setTheme: (TazerThemeMode) -> Unit, permissionRefresh: Int, requestNotifications: () -> Unit,
    openFocusSettings: () -> Unit,
) {
    val profile = workspace.profile
    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)) {
        // Each settings group is one raised card on the canvas, in the same order as before.
        item {
            SettingsCard { ThemeSetting(theme, setTheme) }
            SettingsCard { DailyBriefingSetting(permissionRefresh, requestNotifications) }
            SettingsCard { ReleaseUpdateSetting() }
            SettingsCard { OutlinedButton(onClick = openFocusSettings) { Text("Pengaturan fokus") } }
            state.cloudStatus?.let { status ->
                SettingsCard {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Sinkronisasi cloud", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                        Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        PrimaryButton(onClick = model::syncNow, enabled = !state.cloudSyncing) { Text("Sinkronkan sekarang") }
                    }
                }
            }
            SettingsCard { ProfileForm(profile, state.busy, model) }
        }
        item {
            SettingsCard {
                CourseColorSetting(remember(workspace.tasks) { TaskSearch.courses(workspace.tasks.map { it.course }) },
                    model::chooseCourseColor)
            }
            SettingsCard { DataBackupSetting(state.busy, model) }
            SettingsCard { AccountSettings(state, model) }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    NeuCard(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(Modifier.padding(18.dp)) { content() }
    }
}

@Composable
private fun ProfileForm(profile: UserProfile, busy: Boolean, model: TazerViewModel) {
    var start by rememberSaveable(profile.ownerUid, profile.activeStartMinute) { mutableStateOf(formatMinute(profile.activeStartMinute)) }
    var end by rememberSaveable(profile.ownerUid, profile.activeEndMinute) { mutableStateOf(formatMinute(profile.activeEndMinute)) }
    var zone by rememberSaveable(profile.ownerUid, profile.zoneId) { mutableStateOf(profile.zoneId) }
    val parsedStart = parseMinute(start)
    val parsedEnd = parseMinute(end)
    val zoneId = remember(zone) { runCatching { ZoneId.of(zone.trim()) }.getOrNull()?.takeIf { it.id in ZoneId.getAvailableZoneIds() } }
    val validRange = parsedStart != null && parsedEnd != null && parsedStart < parsedEnd
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Jam aktif", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        Text("Atur waktu yang tersedia untuk sesi fokus.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TimeField(start, { start = it }, "Mulai", busy)
        TimeField(end, { end = it }, "Selesai", busy)
        OutlinedTextField(zone, { zone = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Zona waktu") },
            singleLine = true, enabled = !busy, isError = zoneId == null)
        if (!validRange) Text("Jam selesai harus setelah jam mulai.", color = MaterialTheme.colorScheme.error)
        PrimaryButton(
            onClick = {
                model.saveProfile(requireNotNull(zoneId), requireNotNull(parsedStart), requireNotNull(parsedEnd)) {
                    model.showMessage("Jam aktif tersimpan.")
                }
            }, enabled = !busy && validRange && zoneId != null,
        ) { Text("Simpan jam aktif") }
    }
}

@Composable
private fun TimeField(text: String, update: (String) -> Unit, label: String, busy: Boolean) {
    OutlinedTextField(text, update, label = { Text(label + " (HH:mm)") }, singleLine = true, enabled = !busy,
        isError = parseMinute(text) == null, modifier = Modifier.fillMaxWidth())
}

internal fun dayName(day: Int) = DayOfWeek.of(day).getDisplayName(TextStyle.FULL, Indonesian)
