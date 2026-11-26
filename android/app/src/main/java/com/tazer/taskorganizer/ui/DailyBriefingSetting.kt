package com.tazer.taskorganizer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tazer.taskorganizer.domain.BriefingKind
import com.tazer.taskorganizer.domain.BriefingSettings
import com.tazer.taskorganizer.timer.BriefingPreferences
import com.tazer.taskorganizer.timer.DailyBriefingAlarms
import com.tazer.taskorganizer.timer.FocusRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
internal fun DailyBriefingSetting(permissionRefresh: Int, requestNotifications: () -> Unit) {
    val activity = LocalContext.current
    val context = activity.applicationContext
    val focus = remember(context) { FocusRuntime.get(context) }
    val preferences = remember(context) { BriefingPreferences(context) }
    var settings by remember(preferences) { mutableStateOf(preferences.read()) }
    val canPost = remember(permissionRefresh, settings) { DailyBriefingAlarms.canPost(context) }
    val exact = remember(permissionRefresh) { focus.canScheduleExactAlarms() }
    val scope = rememberCoroutineScope()
    fun update(next: BriefingSettings) {
        if (!preferences.save(next)) return
        val turnedOn = BriefingKind.entries.any { next.enabled(it) && !settings.enabled(it) }
        settings = next
        scope.launch(Dispatchers.IO) { DailyBriefingAlarms.reschedule(context) }
        // Ask for notification access in context, right when a briefing is switched on.
        if (turnedOn && !DailyBriefingAlarms.canPost(context)) requestNotifications()
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Smart Daily Briefing", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        Text("Ringkasan jadwal setiap pagi dan evaluasi setiap malam lewat notifikasi.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        BriefingRow(
            "Briefing pagi", "Sesi fokus hari ini, total jam kerja, dan prioritas #1.",
            settings.morningEnabled, settings.morningMinute,
            onToggle = { update(settings.copy(morningEnabled = it)) },
            onTime = { update(settings.copy(morningMinute = it)) },
        )
        BriefingRow(
            "Review malam", "Sesi selesai dan yang tersisa, untuk menyusun jadwal besok.",
            settings.eveningEnabled, settings.eveningMinute,
            onToggle = { update(settings.copy(eveningEnabled = it)) },
            onTime = { update(settings.copy(eveningMinute = it)) },
        )
        if ((settings.morningEnabled || settings.eveningEnabled) && !canPost) {
            Text("Notifikasi Tazer sedang nonaktif, jadi briefing belum bisa muncul.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = requestNotifications) { Text("Izinkan notifikasi") }
        } else if ((settings.morningEnabled || settings.eveningEnabled) && !exact) {
            Text("Tanpa izin alarm tepat waktu, Android dapat menunda briefing hingga sekitar satu jam.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            focus.exactAlarmSettingsIntent()?.let { intent ->
                OutlinedButton(onClick = { activity.startActivity(intent) }) { Text("Izinkan alarm tepat waktu") }
            }
        }
    }
}

@Composable
private fun BriefingRow(
    title: String, detail: String, enabled: Boolean, minute: Int,
    onToggle: (Boolean) -> Unit, onTime: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            Modifier.fillMaxWidth().toggleable(value = enabled, role = Role.Switch, onValueChange = onToggle),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = enabled, onCheckedChange = null)
        }
        PickerTimeButton(minute, true, onTime)
    }
}
