package com.tazer.taskorganizer.ui

import androidx.compose.foundation.layout.padding
import android.app.Activity
import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

internal enum class TazerThemeMode(val label: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System"),
}

internal val LocalTazerDark = staticCompositionLocalOf { false }

internal class ThemePreference(context: Context) {
    private val preferences = context.getSharedPreferences("display_preferences", Context.MODE_PRIVATE)

    fun read(): TazerThemeMode = TazerThemeMode.entries.firstOrNull {
        it.name == preferences.getString("theme", null)
    } ?: TazerThemeMode.SYSTEM

    fun save(mode: TazerThemeMode) = preferences.edit().putString("theme", mode.name).apply()
}

@Composable
internal fun TazerThemeMode.isDark(): Boolean = when (this) {
    TazerThemeMode.LIGHT -> false
    TazerThemeMode.DARK -> true
    TazerThemeMode.SYSTEM -> isSystemInDarkTheme()
}

@Composable
internal fun ThemeSystemBars(dark: Boolean) {
    val view = LocalView.current
    val activity = view.context as? Activity ?: return
    SideEffect {
        val controller = WindowCompat.getInsetsController(activity.window, view)
        controller.isAppearanceLightStatusBars = !dark
        controller.isAppearanceLightNavigationBars = !dark
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ThemeSetting(theme: TazerThemeMode, setTheme: (TazerThemeMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Tema", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        val modes = TazerThemeMode.entries
        NeuSegmented(modes.map { it.label }, modes.indexOf(theme), { setTheme(modes[it]) }, Modifier.padding(top = 6.dp))
    }
}
