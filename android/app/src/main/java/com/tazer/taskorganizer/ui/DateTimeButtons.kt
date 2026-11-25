package com.tazer.taskorganizer.ui

import android.app.DatePickerDialog
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.widget.NumberPicker
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.time.LocalDate

private fun platformTheme(dark: Boolean) =
    if (dark) android.R.style.Theme_Material_Dialog_Alert else android.R.style.Theme_Material_Light_Dialog_Alert

@Composable
internal fun PickerDateButton(
    day: LocalDate, enabled: Boolean, label: String = DayFormat.format(day), onSelected: (LocalDate) -> Unit,
) {
    val context = LocalContext.current
    val dark = LocalTazerDark.current
    val colors = MaterialTheme.colorScheme
    OutlinedButton(enabled = enabled, onClick = {
        DatePickerDialog(ContextThemeWrapper(context, platformTheme(dark)), { _, year, month, date ->
            onSelected(LocalDate.of(year, month + 1, date))
        }, day.year, day.monthValue - 1, day.dayOfMonth).apply {
            setOnShowListener {
                val density = context.resources.displayMetrics.density
                getButton(DialogInterface.BUTTON_POSITIVE).apply {
                    setTextColor(colors.onPrimary.toArgb())
                    background = RippleDrawable(
                        ColorStateList.valueOf(colors.onPrimary.copy(alpha = .16f).toArgb()),
                        GradientDrawable().apply { setColor(colors.primary.toArgb()); cornerRadius = 24 * density },
                        null,
                    )
                }
                getButton(DialogInterface.BUTTON_NEGATIVE).apply {
                    setTextColor(colors.primary.toArgb())
                    background = RippleDrawable(
                        ColorStateList.valueOf(colors.primary.copy(alpha = .12f).toArgb()),
                        GradientDrawable().apply {
                            setColor(colors.surface.toArgb())
                            setStroke(density.toInt().coerceAtLeast(1), colors.outline.toArgb())
                            cornerRadius = 24 * density
                        },
                        null,
                    )
                }
            }
            show()
        }
    }) { Text(label) }
}

@Composable
internal fun PickerTimeButton(minute: Int, enabled: Boolean, onSelected: (Int) -> Unit) {
    var selecting by rememberSaveable { mutableStateOf(false) }
    OutlinedButton(
        onClick = { selecting = true }, enabled = enabled,
        modifier = Modifier.semantics { contentDescription = "Pilih jam " + formatMinute(minute) },
    ) { Text(formatMinute(minute)) }
    if (selecting) TimeSpinner(minute, { selecting = false }) {
        onSelected(it)
        selecting = false
    }
}

@Composable
private fun TimeSpinner(minute: Int, close: () -> Unit, onSelected: (Int) -> Unit) {
    var hour by rememberSaveable { mutableIntStateOf(minute / 60) }
    var minutes by rememberSaveable { mutableIntStateOf(minute % 60) }
    AlertDialog(
        onDismissRequest = close,
        title = { Text("Pilih jam") },
        text = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally)) {
                NumberWheel("Jam", hour, 23) { hour = it }
                NumberWheel("Menit", minutes, 59) { minutes = it }
            }
        },
        confirmButton = { Button(onClick = { onSelected(hour * 60 + minutes) }) { Text("Terapkan") } },
        dismissButton = { OutlinedButton(onClick = close) { Text("Batal") } },
    )
}

@Composable
private fun NumberWheel(label: String, selected: Int, maximum: Int, change: (Int) -> Unit) {
    val dark = LocalTazerDark.current
    val ink = MaterialTheme.colorScheme.onSurface.toArgb()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        AndroidView(
            factory = { context ->
                NumberPicker(ContextThemeWrapper(context, platformTheme(dark))).apply {
                    minValue = 0
                    maxValue = maximum
                    displayedValues = Array(maximum + 1) { it.toString().padStart(2, '0') }
                    wrapSelectorWheel = true
                    descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                    contentDescription = label
                    value = selected
                    setOnValueChangedListener { _, _, value -> change(value) }
                    if (android.os.Build.VERSION.SDK_INT >= 29) textColor = ink
                }
            },
            update = { picker -> if (picker.value != selected) picker.value = selected },
        )
    }
}
