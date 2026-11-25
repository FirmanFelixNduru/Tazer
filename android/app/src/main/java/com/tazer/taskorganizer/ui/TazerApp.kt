package com.tazer.taskorganizer.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tazer.taskorganizer.data.WorkSession
import com.tazer.taskorganizer.data.WorkSessionState
import com.tazer.taskorganizer.timer.FocusRuntime
import java.time.ZoneId

private val DayColors = lightColorScheme(
    primary = Color(0xFF00695C),
    primaryContainer = Color(0xFFBCEBDD),
    onPrimaryContainer = Color(0xFF00382F),
    secondary = Color(0xFF455F57),
    secondaryContainer = Color(0xFFDAEAE2),
    onSecondaryContainer = Color(0xFF203D33),
    tertiary = Color(0xFF875324),
    tertiaryContainer = Color(0xFFFFE0BB),
    onTertiaryContainer = Color(0xFF55300F),
    background = Color.White,
    surface = Color.White,
    outline = Color(0xFF687B70),
    surfaceContainer = Color(0xFFEBF2ED),
    surfaceContainerLow = Color(0xFFF1F6F2),
    surfaceContainerHigh = Color(0xFFE5EEE7),
    onSurface = Color(0xFF17221D),
    onSurfaceVariant = Color(0xFF485A51),
    outlineVariant = Color(0xFFC4D3C9),
)

private val NightColors = darkColorScheme(
    primary = Color(0xFF96DBCB),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF005044),
    onPrimaryContainer = Color(0xFFBCEBDD),
    secondary = Color(0xFFADCBBE),
    secondaryContainer = Color(0xFF2D463E),
    onSecondaryContainer = Color(0xFFCFE7DB),
    tertiary = Color(0xFFF0BA84),
    tertiaryContainer = Color(0xFF593C20),
    onTertiaryContainer = Color(0xFFFFE0BB),
    background = Color.Black,
    surface = Color.Black,
    surfaceContainer = Color(0xFF141F19),
    surfaceContainerLow = Color(0xFF101813),
    surfaceContainerHigh = Color(0xFF25372E),
    onSurface = Color(0xFFDEE9E0),
    onSurfaceVariant = Color(0xFFB6C9BD),
    outline = Color(0xFF829B8C),
    outlineVariant = Color(0xFF40564A),
)

private val Sections = listOf("Tugas", "Jadwal", "Kalender", "Pengaturan")
private val SectionGlyphs = listOf(TazerGlyph.Tasks, TazerGlyph.Timeline, TazerGlyph.Calendar, TazerGlyph.Settings)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TazerApp(model: TazerViewModel, launch: (Intent) -> Unit, requestNotifications: () -> Unit, permissionRefresh: Int) {
    val state by model.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val themePreference = remember(context) { ThemePreference(context.applicationContext) }
    var theme by remember(themePreference) { mutableStateOf(themePreference.read()) }
    val dark = theme.isDark()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbar.showSnackbar(message)
        model.showMessage(null)
    }
    ThemeSystemBars(dark)
    MaterialTheme(colorScheme = if (dark) NightColors else DayColors) {
        CompositionLocalProvider(LocalTazerDark provides dark) {
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
            else -> WorkspaceScreen(state, workspace, model, snackbar, launch, requestNotifications, permissionRefresh,
                theme) { selected -> themePreference.save(selected); theme = selected }
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkspaceScreen(
    state: TazerState, workspace: Workspace, model: TazerViewModel, snackbar: SnackbarHostState,
    launch: (Intent) -> Unit, requestNotifications: () -> Unit, permissionRefresh: Int,
    theme: TazerThemeMode, setTheme: (TazerThemeMode) -> Unit,
) {
    var section by rememberSaveable(state.uid) { mutableIntStateOf(0) }
    var taskEditorId by rememberSaveable(state.uid) { mutableStateOf<String?>(null) }
    var archivedTab by rememberSaveable(state.uid) { mutableStateOf(false) }
    var viewDay by rememberSaveable(state.uid) { mutableStateOf<String?>(null) }
    var deferDialog by rememberSaveable(state.uid) { mutableStateOf(false) }
    var permissionDialog by rememberSaveable(state.uid) { mutableStateOf(false) }
    val sectionState = rememberSaveableStateHolder()
    val zone = ZoneId.of(workspace.profile.zoneId)
    val focusTarget = state.focusTarget
    val inFocus = focusTarget != null
    val focusedSession = workspace.sessions.firstOrNull { it.id == focusTarget?.id }
    val focusedTask = workspace.tasks.firstOrNull { it.id == focusedSession?.taskId }
    val activeSession = workspace.sessions.firstOrNull { it.state in ActiveFocusStates }
    val activeTitle = workspace.tasks.firstOrNull { it.id == activeSession?.taskId }?.title
    val editing = taskEditorId != null && !inFocus
    val editedTask = workspace.tasks.firstOrNull { it.id == taskEditorId }
    val closeEditor = { taskEditorId = null }
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
                    if (editing || inFocus) OutlinedButton(
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp),
                        onClick = if (inFocus) model::closeFocus else closeEditor,
                    ) { Text("Kembali") }
                },
            )
        },
        bottomBar = {
            if (!inFocus && !editing) Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (activeSession != null && activeTitle != null) {
                    FloatingTimerDeck(activeSession, activeTitle, model.focus) { model.openFocus(activeSession) }
                }
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    Sections.forEachIndexed { index, label ->
                        NavigationBarItem(
                            selected = section == index,
                            onClick = { section = index },
                            icon = { TazerIcon(SectionGlyphs[index]) },
                            label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer),
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (section == 0 && !inFocus && !editing && !archivedTab) {
                FloatingActionButton(
                    onClick = { taskEditorId = "" },
                    modifier = Modifier.semantics { contentDescription = "Tambah tugas" },
                ) { TazerIcon(TazerGlyph.Add) }
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
                    2 -> sectionState.SaveableStateProvider("calendar") { CalendarScreen(workspace, state, zone, model) }
                    3 -> SettingsScreen(workspace, state, model, theme, setTheme)
                }
            }
        }
    }
    if (deferDialog) DeferFocusDialog(zone, state.busy, model) { deferDialog = false }
    if (permissionDialog) FocusPermissionsDialog(model.focus, permissionRefresh, launch, requestNotifications) { permissionDialog = false }
}

@Composable
private fun FloatingTimerDeck(session: WorkSession, title: String, runtime: FocusRuntime, open: () -> Unit) {
    val clock = focusClock(runtime, session)
    val overtime = session.isOvertime(clock)
    val paused = session.state == WorkSessionState.PAUSED
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = open,
        modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            .semantics { contentDescription = "Buka timer aktif" },
        shape = RoundedCornerShape(18.dp),
        color = if (paused || overtime) colors.tertiaryContainer else colors.primaryContainer,
        contentColor = if (paused || overtime) colors.onTertiaryContainer else colors.onPrimaryContainer,
        tonalElevation = 3.dp,
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TazerIcon(TazerGlyph.Timer)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(when {
                    paused -> "Dijeda"
                    overtime -> "Lembur"
                    else -> "Sedang fokus"
                }, style = MaterialTheme.typography.labelMedium)
            }
            Text(session.timerText(clock, overtime), style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"))
            TazerIcon(TazerGlyph.Next)
        }
    }
}

@Composable
internal fun TimerIconButton(label: String, onClick: () -> Unit) {
    OutlinedIconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) { TazerIcon(TazerGlyph.Timer, label) }
}

internal enum class TazerGlyph { Tasks, Timeline, Calendar, Settings, Timer, Previous, Next, Down, Edit, Delete, Check, Add }

@Composable
internal fun TazerIcon(glyph: TazerGlyph, description: String? = null) {
    val ink = LocalContentColor.current
    val accessibility = if (description == null) Modifier else Modifier.semantics { contentDescription = description }
    Canvas(Modifier.size(22.dp).then(accessibility)) {
        fun point(x: Float, y: Float) = Offset(size.width * x, size.height * y)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
            drawLine(ink, point(x1, y1), point(x2, y2), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
        }
        when (glyph) {
            TazerGlyph.Tasks, TazerGlyph.Timeline -> {
                if (glyph == TazerGlyph.Timeline) line(.22f, .23f, .22f, .77f)
                listOf(.28f, .5f, .72f).forEach { y ->
                    drawCircle(ink, radius = 1.05.dp.toPx(), center = point(.22f, y))
                    line(.34f, y, .82f, y)
                }
            }
            TazerGlyph.Calendar -> {
                drawRect(ink, point(.16f, .23f), Size(size.width * .68f, size.height * .58f), style = Stroke(1.8.dp.toPx()))
                line(.31f, .15f, .31f, .3f)
                line(.69f, .15f, .69f, .3f)
                line(.16f, .42f, .84f, .42f)
            }
            TazerGlyph.Settings -> {
                line(.15f, .29f, .85f, .29f)
                line(.15f, .5f, .85f, .5f)
                line(.15f, .71f, .85f, .71f)
                listOf(.36f to .29f, .66f to .5f, .46f to .71f).forEach { (x, y) ->
                    drawCircle(ink, radius = 2.6.dp.toPx(), center = point(x, y))
                }
            }
            TazerGlyph.Timer -> {
                drawCircle(ink, size.width * .31f, point(.5f, .57f), style = Stroke(1.8.dp.toPx()))
                line(.37f, .13f, .63f, .13f)
                line(.5f, .25f, .5f, .13f)
                line(.5f, .57f, .5f, .40f)
                line(.5f, .57f, .62f, .63f)
            }
            TazerGlyph.Previous -> { line(.6f, .24f, .34f, .5f); line(.34f, .5f, .6f, .76f) }
            TazerGlyph.Next -> { line(.4f, .24f, .66f, .5f); line(.66f, .5f, .4f, .76f) }
            TazerGlyph.Down -> { line(.24f, .37f, .5f, .63f); line(.5f, .63f, .76f, .37f) }
            TazerGlyph.Edit -> {
                val w = size.width
                val h = size.height
                drawPath(Path().apply {
                    moveTo(w * .10f, h * .90f)
                    lineTo(w * .15f, h * .65f)
                    lineTo(w * .61f, h * .19f)
                    lineTo(w * .81f, h * .39f)
                    lineTo(w * .35f, h * .85f)
                    close()
                    moveTo(w * .67f, h * .13f)
                    quadraticTo(w * .75f, h * .05f, w * .83f, h * .13f)
                    lineTo(w * .87f, h * .17f)
                    quadraticTo(w * .95f, h * .25f, w * .87f, h * .33f)
                    close()
                }, ink)
            }
            TazerGlyph.Delete -> {
                line(.20f, .30f, .80f, .30f)
                line(.39f, .19f, .61f, .19f)
                line(.28f, .30f, .32f, .81f)
                line(.32f, .81f, .68f, .81f)
                line(.68f, .81f, .72f, .30f)
                line(.43f, .43f, .44f, .68f)
                line(.57f, .43f, .56f, .68f)
            }
            TazerGlyph.Check -> { line(.20f, .51f, .42f, .72f); line(.42f, .72f, .80f, .29f) }
            TazerGlyph.Add -> { line(.22f, .5f, .78f, .5f); line(.5f, .22f, .5f, .78f) }
        }
    }
}

internal val NumberKeyboard = KeyboardOptions(keyboardType = KeyboardType.Number)
