package com.tazer.taskorganizer.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tazer.taskorganizer.data.WorkSessionState
import java.time.ZoneId

private val DayColors = lightColorScheme(
    primary = Color(0xFF2A4AB6),
    primaryContainer = Color(0xFFDCE2FF),
    onPrimaryContainer = Color(0xFF102363),
    background = Color(0xFFF9FAFF),
    surface = Color(0xFFF9FAFF),
    onSurface = Color(0xFF1A1B24),
    onSurfaceVariant = Color(0xFF464959),
)

private val NightColors = darkColorScheme(
    primary = Color(0xFFB5C5FF),
    onPrimary = Color(0xFF152A70),
    primaryContainer = Color(0xFF233B89),
    onPrimaryContainer = Color(0xFFDCE2FF),
    background = Color(0xFF151622),
    surface = Color(0xFF151622),
    onSurface = Color(0xFFE4E6F1),
    onSurfaceVariant = Color(0xFFC4C7DA),
)

private val Sections = listOf("Tugas", "Jadwal", "Pengaturan")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TazerApp(model: TazerViewModel, launch: (Intent) -> Unit, requestNotifications: () -> Unit, permissionRefresh: Int) {
    val state by model.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbar.showSnackbar(message)
        model.showMessage(null)
    }
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) NightColors else DayColors) {
        val workspace = state.data
        when {
            state.recovery || state.uid == null -> Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
                LoginScreen(model, state, launch, Modifier.padding(padding))
            }
            workspace == null -> Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.semantics { contentDescription = "Memuat tugas" })
                }
            }
            else -> WorkspaceScreen(state, workspace, model, snackbar, launch, requestNotifications, permissionRefresh)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkspaceScreen(
    state: TazerState, workspace: Workspace, model: TazerViewModel, snackbar: SnackbarHostState,
    launch: (Intent) -> Unit, requestNotifications: () -> Unit, permissionRefresh: Int,
) {
    var section by rememberSaveable(state.uid) { mutableIntStateOf(0) }
    var taskEditorId by rememberSaveable(state.uid) { mutableStateOf<String?>(null) }
    var archivedTab by rememberSaveable(state.uid) { mutableStateOf(false) }
    var viewDay by rememberSaveable(state.uid) { mutableStateOf<String?>(null) }
    var deferDialog by rememberSaveable(state.uid) { mutableStateOf(false) }
    var permissionDialog by rememberSaveable(state.uid) { mutableStateOf(false) }
    val zone = ZoneId.of(workspace.profile.zoneId)
    val focusTarget = state.focusTarget
    val inFocus = focusTarget != null
    val focusedSession = workspace.sessions.firstOrNull { it.id == focusTarget?.id }
    val focusedTask = workspace.tasks.firstOrNull { it.id == focusedSession?.taskId }
    val overrun = workspace.sessions.firstOrNull { it.state == WorkSessionState.OVERTIME }
    val editing = taskEditorId != null && !inFocus
    val editedTask = workspace.tasks.firstOrNull { it.id == taskEditorId }
    val closeEditor = { taskEditorId = null }
    LaunchedEffect(inFocus) { if (inFocus) section = 1 }
    LaunchedEffect(overrun?.id, overrun?.revision) {
        if (overrun != null && focusTarget?.id != overrun.id) model.openFocus(overrun)
    }
    BackHandler(enabled = editing, onBack = closeEditor)
    BackHandler(enabled = inFocus, onBack = model::closeFocus)
    BackHandler(enabled = !inFocus && !editing && section != 0) { section = 0 }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(when {
                        inFocus -> "Timer fokus"
                        editing -> if (editedTask == null) "Tambah tugas" else "Edit tugas"
                        else -> Sections[section]
                    })
                },
                navigationIcon = {
                    if (editing) TextButton(onClick = closeEditor) { Text("Kembali") }
                    if (inFocus) TextButton(onClick = model::closeFocus) { Text("Kembali") }
                },
            )
        },
        bottomBar = {
            if (!inFocus && !editing) NavigationBar {
                Sections.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = section == index,
                        onClick = { section = index },
                        icon = { SectionIcon(index) },
                        label = { Text(label) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (section == 0 && !inFocus && !editing && !archivedTab) {
                FloatingActionButton(
                    onClick = { taskEditorId = "" },
                    modifier = Modifier.semantics { contentDescription = "Tambah tugas" },
                ) { Text("+", style = MaterialTheme.typography.headlineMedium) }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxSize()) {
                if (focusTarget != null) {
                    if (focusedSession != null && focusedTask != null) {
                        FocusScreen(focusedSession, focusedTask, focusTarget, zone, state, model,
                            { permissionDialog = true }, { deferDialog = true })
                    } else {
                        Text("Memuat sesi…", modifier = Modifier.padding(20.dp))
                    }
                } else if (editing) {
                    TaskEditor(editedTask, zone, state.subscription?.tier, state.busy, model, closeEditor)
                } else when (section) {
                    0 -> TasksScreen(workspace, state, zone, archivedTab, { archivedTab = it }, model) {
                        taskEditorId = it.id
                    }
                    1 -> TimelineScreen(
                        workspace, state, zone, viewDay?.let(java.time.LocalDate::parse) ?: localDate(state.now, zone),
                        { viewDay = it.toString() }, model::openFocus, { deferDialog = true }, { permissionDialog = true },
                    )
                    2 -> SettingsScreen(workspace, state, model)
                }
            }
        }
    }
    if (deferDialog) DeferFocusDialog(zone, state.busy, model) { deferDialog = false }
    if (permissionDialog) FocusPermissionsDialog(model.focus, permissionRefresh, launch, requestNotifications) { permissionDialog = false }
}

@Composable
private fun SectionIcon(index: Int) {
    val ink = LocalContentColor.current
    Canvas(Modifier.size(24.dp)) {
        fun point(x: Float, y: Float) = Offset(size.width * x, size.height * y)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
            drawLine(ink, point(x1, y1), point(x2, y2), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
        }
        when (index) {
            0 -> {
                listOf(.28f, .5f, .72f).forEach { y ->
                    drawCircle(ink, radius = 1.05.dp.toPx(), center = point(.18f, y))
                    line(.34f, y, .82f, y)
                }
            }
            1 -> {
                drawRect(ink, point(.16f, .23f), Size(size.width * .68f, size.height * .58f), style = Stroke(1.8.dp.toPx()))
                line(.31f, .15f, .31f, .3f)
                line(.69f, .15f, .69f, .3f)
                line(.16f, .42f, .84f, .42f)
            }
            else -> {
                line(.15f, .29f, .85f, .29f)
                line(.15f, .5f, .85f, .5f)
                line(.15f, .71f, .85f, .71f)
                listOf(.36f to .29f, .66f to .5f, .46f to .71f).forEach { (x, y) ->
                    drawCircle(ink, radius = 2.6.dp.toPx(), center = point(x, y))
                }
            }
        }
    }
}

internal val NumberKeyboard = KeyboardOptions(keyboardType = KeyboardType.Number)
