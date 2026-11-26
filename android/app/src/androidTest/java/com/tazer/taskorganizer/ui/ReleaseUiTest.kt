package com.tazer.taskorganizer.ui

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tazer.taskorganizer.MainActivity
import com.tazer.taskorganizer.BuildConfig
import com.tazer.taskorganizer.data.CalendarRepository
import com.tazer.taskorganizer.data.TaskRepository
import com.tazer.taskorganizer.data.TazerDatabase
import java.io.File
import com.tazer.taskorganizer.data.WorkSession
import com.tazer.taskorganizer.data.WorkSessionState
import com.tazer.taskorganizer.domain.FocusPresetMode
import com.tazer.taskorganizer.timer.FocusRuntime
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReleaseUiTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val automation = instrumentation.uiAutomation
    private var activity: Activity? = null

    private fun launch() {
        activity = instrumentation.startActivitySync(Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        waitFor("Demo workspace did not open") {
            findText("Belum Selesai") != null || (clickText("Masuk sebagai demo") && false)
        }
    }

    private fun finish() {
        activity?.let { current -> instrumentation.runOnMainSync { current.finish() } }
        activity = null
    }

    @Test
    fun settingsShowsInstalledVersionAndUpdateCheckReturnsAStatus() {
        try {
            launch()
            waitFor("Settings unavailable") { clickText("Pengaturan") }
            waitFor("Installed version missing") { findText("Versi terpasang ${BuildConfig.VERSION_NAME}") != null }
            waitFor("Update check could not be tapped") { clickText("Cek Update") }
            waitFor("Update check did not return a result") {
                if (Build.VERSION.SDK_INT >= 33) automation.clearCache()
                nodes {
                    val value = it.text?.toString().orEmpty()
                    value.startsWith("Pembaruan belum bisa diperiksa.") || value == "Rilis publik belum tersedia." ||
                        value == "Aplikasi sudah versi terbaru." || value == "Batas permintaan GitHub tercapai. Coba lagi nanti." ||
                        value.startsWith("Versi ") && value.endsWith(" tersedia")
                }.isNotEmpty()
            }
        } finally {
            finish()
        }
    }

    @Test
    fun googleSignInIsReachableOnLoginScreen() {
        if (BuildConfig.SUPABASE_URL.isEmpty()) return
        try {
            activity = instrumentation.startActivitySync(Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            waitFor("Google sign-in is missing or disabled") {
                var node = findText("Masuk dengan Google")
                while (node != null && !node.isClickable) node = node.parent
                node?.isEnabled == true
            }
        } finally {
            finish()
        }
    }

    @Test
    fun themeChoicesPersistAcrossActivityRestart() {
        val preference = ThemePreference(context)
        val original = preference.read()
        try {
            launch()
            for (mode in TazerThemeMode.entries) {
                waitFor("Settings unavailable") { clickText("Pengaturan") }
                waitFor("Theme choice ${mode.label} unavailable") { clickText(mode.label) }
                waitFor("Theme choice was not saved") { ThemePreference(context).read() == mode }
                finish()
                launch()
                waitFor("Settings unavailable after restart") { clickText("Pengaturan") }
                waitFor("${mode.label} was not selected after restart") { isChoiceSelected(mode.label) }
                assertEquals(mode, ThemePreference(context).read())
            }
        } finally {
            finish()
            preference.save(original)
        }
    }

    @Test
    fun statisticsUsesRoomHistoryAndRendersInLightAndDark() = runBlocking {
        val database = TazerDatabase.getInstance(context)
        val tasks = TaskRepository(database)
        val preference = ThemePreference(context)
        val original = preference.read()
        val course = "Statistik QA " + UUID.randomUUID().toString().take(8)
        var taskId: String? = null
        try {
            launch() // Establishes the existing demo owner and profile.
            val now = System.currentTimeMillis()
            val ended = now - 5 * 60_000L
            val started = ended - 40 * 60_000L
            val task = tasks.createTask(UID, course, course, "", now + 60_000L, 2, 30, started)
            taskId = task.id
            database.tazerDao().insertSession(WorkSession(
                UUID.randomUUID().toString(), task.id, UID, WorkSessionState.DONE, started, ended, 40,
                started, ended, ended - started, null, null, null, null, 0L,
            ))
            assertTrue(tasks.completeTask(UID, task.id, ended))
            for (mode in listOf(TazerThemeMode.LIGHT, TazerThemeMode.DARK)) {
                finish()
                preference.save(mode)
                launch()
                waitFor("Statistics navigation unavailable") { clickText("Statistik") }
                waitFor("Weekly chart did not open") {
                    if (Build.VERSION.SDK_INT >= 33) automation.clearCache()
                    findText("Fokus harian") != null
                }
                val screenshot = requireNotNull(automation.takeScreenshot())
                try {
                    assertEquals(if (mode == TazerThemeMode.LIGHT) android.graphics.Color.WHITE else android.graphics.Color.BLACK,
                        screenshot.getPixel(4, screenshot.height / 8))
                } finally { screenshot.recycle() }
                waitFor("Estimate accuracy for the Room fixture is missing") {
                    find { it.text?.toString()?.startsWith("Akurasi estimasi") == true } != null || scrollUp()
                }
                waitFor("Room focus course is missing from statistics") { findText(course) != null || scrollUp() }
            }
        } finally {
            taskId?.let { tasks.deleteTask(UID, it) }
            finish()
            preference.save(original)
        }
    }

    @Test
    fun focusPresetPersistsWithDndPermissionFallback() = runBlocking {
        launch()
        val focus = FocusRuntime.get(context)
        val previous = focus.focusSettings()
        val policyAccess = focus.hasDndAccess()
        if (policyAccess) setDndTestAccess(false)
        try {
            waitFor("Settings unavailable") { clickText("Pengaturan") }
            waitFor("Focus settings button unavailable") { clickText("Pengaturan fokus") }
            waitFor("Focus settings dialog did not open") { findText("Durasi fokus dan jeda") != null }
            val pomodoro = "Pomodoro · 25 mnt fokus, 5 mnt jeda"
            waitFor("Pomodoro could not be selected") { clickText(pomodoro) }
            if (findText("Buka akses Jangan Ganggu") == null) {
                waitFor("DND opt-in unavailable") { clickText("Jangan Ganggu saat fokus") || scrollUp() }
            }
            waitFor("DND permission explanation missing") {
                if (Build.VERSION.SDK_INT >= 33) automation.clearCache()
                findText("Buka akses Jangan Ganggu") != null || scrollUp()
            }
            assertEquals(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS, focus.dndSettingsIntent().action)
            waitFor("Focus preference could not be saved") { clickText("Simpan") }
            waitFor("Focus preferences were not applied") {
                focus.focusSettings().preset.mode == FocusPresetMode.POMODORO && focus.focusSettings().dndEnabled
            }
            finish()
            launch()
            assertEquals(25, focus.focusSettings().preset.workMinutes)
            assertEquals(5, focus.focusSettings().preset.breakMinutes)
            assertTrue(focus.focusSettings().dndEnabled)
            waitFor("Schedule navigation unavailable") { clickText("Jadwal") }
            waitFor("Schedule did not show the selected rhythm") {
                if (Build.VERSION.SDK_INT >= 33) automation.clearCache()
                findText("Pomodoro · 25 mnt fokus · 5 mnt jeda") != null
            }
        } finally {
            focus.saveFocusSettings(previous)
            if (policyAccess) setDndTestAccess(true)
            finish()
        }
    }

    private fun setDndTestAccess(granted: Boolean) {
        val command = if (granted) "allow_dnd" else "disallow_dnd"
        android.os.ParcelFileDescriptor.AutoCloseInputStream(
            automation.executeShellCommand("cmd notification $command ${context.packageName}"),
        ).use { it.readBytes() }
    }

    @Test
    fun taskDeadlineSpinnerChangesAndAppliesTime() {
        try {
            launch()
            waitFor("Add task unavailable") { clickText("Tambah tugas") }
            waitFor("Deadline time control unavailable") {
                find { it.contentDescription?.toString()?.startsWith("Pilih jam ") == true } != null || scrollUp()
            }
            val before = requireNotNull(find { it.contentDescription?.toString()?.startsWith("Pilih jam ") == true })
                .contentDescription.toString()
            assertTrue(tapNode(requireNotNull(find { it.contentDescription?.toString() == before })))
            waitFor("Time dialog missing") { findText("Pilih jam") != null }
            waitFor("Native hour and minute wheels missing") { nodes { it.className?.toString() == "android.widget.NumberPicker" }.size == 2 }
            val wheels = nodes { it.className?.toString() == "android.widget.NumberPicker" }.sortedBy { bounds(it).left }
            // Tap below each wheel's center, the native increment region.
            wheels.forEach { wheel ->
                val rect = bounds(wheel)
                tap(rect.exactCenterX(), rect.top + rect.height() * .82f)
                SystemClock.sleep(500)
            }
            waitFor("Apply time unavailable") { clickText("Terapkan") }
            waitFor("Updated deadline time was not applied") {
                val after = find { it.contentDescription?.toString()?.startsWith("Pilih jam ") == true }?.contentDescription?.toString()
                after != null && after != before
            }
            assertNotEquals(before, requireNotNull(find { it.contentDescription?.toString()?.startsWith("Pilih jam ") == true }).contentDescription.toString())
        } finally { finish() }
    }

    @Test
    fun calendarRendersSingleAndMultiDayEventsAsDistinctPills() = runBlocking {
        val database = TazerDatabase.getInstance(context)
        val tasks = TaskRepository(database)
        val calendar = CalendarRepository(database)
        val token = UUID.randomUUID().toString().take(8)
        val singleTitle = "Single $token"
        val spanTitle = "Span $token"
        var fixtureIds: List<String> = emptyList()
        try {
            launch()
            val profile = requireNotNull(tasks.observeProfile(UID).first())
            val today = LocalDate.now(ZoneId.of(profile.zoneId))
            val monday = today.withDayOfMonth(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
            assertTrue(calendar.saveEvent(UID, null, singleTitle, monday.toEpochDay(), monday.toEpochDay(), null, ""))
            assertTrue(calendar.saveEvent(UID, null, spanTitle, monday.plusDays(1).toEpochDay(), monday.plusDays(3).toEpochDay(), null, ""))
            fixtureIds = calendar.observeEvents(UID).first().filter { it.title == singleTitle || it.title == spanTitle }.map { it.id }
            assertEquals(2, fixtureIds.size)
            waitFor("Calendar unavailable") { clickText("Kalender") }
            waitFor("Single and spanning event pills missing") {
                taggedWithText("calendar-event-pill", singleTitle) != null && taggedWithText("calendar-event-span", spanTitle) != null
            }
            val single = requireNotNull(taggedWithText("calendar-event-pill", singleTitle))
            val span = requireNotNull(taggedWithText("calendar-event-span", spanTitle))
            assertTrue("Multi-day event should span multiple day columns", bounds(span).width() > bounds(single).width())
            assertTrue("Multi-day pill should open its event", tapNode(span))
            waitFor("Event editor did not open") { findText("Edit acara") != null }
        } finally {
            finish()
            // Include partially-created fixtures if an assertion fails during setup.
            calendar.observeEvents(UID).first().filter { it.id in fixtureIds || it.title == singleTitle || it.title == spanTitle }
                .forEach { calendar.deleteEvent(UID, it.id) }
        }
    }

    @Test
    fun calendarAndSettingsPreviewTheSamePickedIcsAndOnlySaveOnConfirm() = runBlocking {
        val database = TazerDatabase.getInstance(context)
        val calendar = CalendarRepository(database)
        val dao = database.tazerDao()
        val title = "Impor uji " + UUID.randomUUID().toString().take(8)
        val document = File.createTempFile("tazer-ics-", ".ics", context.cacheDir)
        document.writeText("""
            BEGIN:VCALENDAR
            VERSION:2.0
            BEGIN:VEVENT
            UID:$title-event
            DTSTART;VALUE=DATE:20261126
            SUMMARY:$title
            END:VEVENT
            BEGIN:VEVENT
            UID:$title-weekly
            DTSTART:20261126T090000
            DTEND:20261126T100000
            SUMMARY:$title
            RRULE:FREQ=WEEKLY
            END:VEVENT
            END:VCALENDAR
        """.trimIndent())
        val filter = IntentFilter(Intent.ACTION_OPEN_DOCUMENT).apply {
            addDataType("*/*")
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        val monitor = instrumentation.addMonitor(
            filter, Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(Uri.fromFile(document))), true,
        )
        try {
            launch()
            for (section in listOf("Kalender", "Pengaturan")) {
                waitFor("$section unavailable") { clickText(section) }
                waitFor("File picker button missing in $section") { clickText("Impor .ics") || scrollUp() }
                waitFor("Preview counts missing in $section") {
                    findText("1 acara kalender baru") != null && findText("1 blok jadwal tetap baru") != null
                }
                assertTrue(calendar.observeEvents(UID).first().none { it.title == title })
                assertTrue(dao.findFixedSchedules(UID).none { it.label == title })
                val action = if (section == "Kalender") "Batal" else "Impor"
                waitFor("Preview action $action unavailable") { clickText(action) }
                waitFor("Preview did not close") { findText("Pratinjau impor .ics") == null }
            }
            assertEquals(2, monitor.hits)
            assertEquals(1, calendar.observeEvents(UID).first().count { it.title == title })
            assertEquals(1, dao.findFixedSchedules(UID).count { it.label == title })
        } finally {
            instrumentation.removeMonitor(monitor)
            finish()
            document.delete()
            calendar.observeEvents(UID).first().filter { it.title == title }.forEach { calendar.deleteEvent(UID, it.id) }
            dao.findFixedSchedules(UID).filter { it.label == title }.forEach { dao.deleteFixedSchedule(UID, it.id) }
        }
    }

    @Test
    fun scheduleSwipeOnlySelectsDatesOnTheStrip() {
        try {
            launch()
            waitFor("Schedule unavailable") { clickText("Jadwal") }
            waitFor("Date strip and timeline did not load") {
                scheduleDates().size >= 3 && findText("Jam") != null && centeredScheduleDate() != null
            }
            assertTrue(findText("Pekan sebelumnya") == null && findText("Pekan berikutnya") == null)
            val initial = requireNotNull(centeredScheduleDate())
            val dateBounds = scheduleDates().map(::bounds)
            val strip = Rect(dateBounds.first()).apply { dateBounds.drop(1).forEach { union(it) } }
            val inset = strip.width() * .12f
            val timelineHeader = bounds(requireNotNull(findText("Jam")))
            val outside = timelineHeader.bottom.toFloat() + context.resources.displayMetrics.density * 40
            swipe(strip.right - inset, outside, strip.left + inset, outside)
            automation.waitForIdle(350, 5_000)
            var afterTimelineSwipe: String? = null
            waitFor("Date strip did not return after the timeline swipe") {
                afterTimelineSwipe = centeredScheduleDate()
                afterTimelineSwipe != null
            }
            assertEquals(initial, afterTimelineSwipe)
            val y = strip.exactCenterY()
            swipe(strip.right - inset, y, strip.left + inset, y)
            automation.waitForIdle(350, 5_000)
            waitFor("Date strip did not settle on a new day") {
                centeredScheduleDate()?.let { it != initial } == true && findText("Jam") != null
            }
            val updatedHeader = bounds(requireNotNull(findText("Jam")))
            assertEquals(timelineHeader.left, updatedHeader.left)
            assertEquals(timelineHeader.right, updatedHeader.right)
            assertTrue(clickText("Hari Ini"))
            waitFor("Hari Ini did not restore the initial date") { centeredScheduleDate() == initial }
        } finally { finish() }
    }

    @Test
    fun calendarHeaderButtonsAlignAndDateCellsShareGridEdges() = runBlocking {
        try {
            launch()
            val profile = requireNotNull(TaskRepository(TazerDatabase.getInstance(context)).observeProfile(UID).first())
            val date = LocalDate.now(ZoneId.of(profile.zoneId))
            val month = DateTimeFormatter.ofPattern("MMMM yyyy", Indonesian).format(date)
            waitFor("Calendar unavailable") { clickText("Kalender") }
            waitFor("Calendar header did not load") { findText(month) != null && findText("Hari Ini") != null }
            val monthButton = bounds(requireNotNull(actionFor(month)))
            val todayButton = bounds(requireNotNull(actionFor("Hari Ini")))
            assertEquals(todayButton.height(), monthButton.height())
            assertEquals(todayButton.centerY(), monthButton.centerY())
            val cells = nodes { it.viewIdResourceName == "calendar-date-cell" }.map(::bounds)
            val row = cells.groupBy { it.top }.values.first { it.size == 7 }.sortedBy { it.left }
            assertTrue(row.all { it.height() == row.first().height() })
            assertTrue(row.zipWithNext().all { (left, right) -> kotlin.math.abs(left.right - right.left) <= 2 })
        } finally { finish() }
    }

    @Test
    fun taskCardIconsAreAccessibleAndKeepTheirActions() = runBlocking {
        val tasks = TaskRepository(TazerDatabase.getInstance(context))
        val title = "Aksi tugas QA " + UUID.randomUUID().toString().take(8)
        var taskId: String? = null
        try {
            launch()
            val now = System.currentTimeMillis()
            taskId = tasks.createTask(UID, title, "QA", "", now + 172_800_000L, 3, 30, now).id
            val labels = listOf("Edit tugas $title", "Tandai selesai $title", "Hapus tugas $title")
            waitFor("Task icon labels missing") { labels.all { actionFor(it) != null } }
            val minimum = 48 * context.resources.displayMetrics.density
            assertTrue(labels.map { bounds(requireNotNull(actionFor(it))) }.all {
                it.width() >= minimum - 1 && it.height() >= minimum - 1
            })
            assertTrue("Edit action was rejected",
                requireNotNull(actionFor(labels[0])).performAction(AccessibilityNodeInfo.ACTION_CLICK))
            waitFor("Editor did not open for $title") {
                // UiAutomation can retain the old Compose task list after navigation on API 37.
                if (Build.VERSION.SDK_INT >= 33) automation.clearCache()
                findText("Edit tugas") != null && find { it.isEditable && it.text?.toString() == title } != null
            }
            waitFor("Edit back button did not accept a tap") { clickText("Kembali") }
            waitFor("Complete action not available") { clickText(labels[1]) }
            waitFor("Archive tab did not open") { clickText("Selesai") }
            waitFor("Completed task or archive delete action missing") {
                findText("Buka kembali") != null && clickText(labels[2])
            }
            waitFor("Archive delete confirmation did not open") { findText("Hapus tugas?") != null }
            waitFor("Archive delete confirmation did not accept a tap") { clickText("Hapus") }
            waitFor("Task stayed in archive after delete") {
                findText("Belum Selesai") != null && findText(title) == null && actionFor(labels[2]) == null
            }
        } finally {
            taskId?.let { tasks.deleteTask(UID, it) }
            finish()
        }
    }

    private fun scheduleDates() = nodes {
        it.contentDescription?.let(DateDescription::containsMatchIn) == true
    }

    private fun centeredScheduleDate(): String? {
        val screenCenter = context.resources.displayMetrics.widthPixels / 2
        return scheduleDates().minByOrNull { kotlin.math.abs(bounds(it).centerX() - screenCenter) }
            ?.contentDescription?.toString()
    }

    private fun actionFor(text: String): AccessibilityNodeInfo? {
        var node = findText(text)
        while (node != null && !node.isClickable) node = node.parent
        return node
    }

    private fun swipe(x1: Float, y1: Float, x2: Float, y2: Float) {
        val coordinates = listOf(x1, y1, x2, y2).joinToString(" ") { it.toInt().toString() }
        android.os.ParcelFileDescriptor.AutoCloseInputStream(
            automation.executeShellCommand("input swipe $coordinates 360"),
        ).use { it.readBytes() }
    }

    private fun waitFor(message: String, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 12_000L
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition()) return
            SystemClock.sleep(100)
        }
        throw AssertionError(message)
    }

    private fun nodes(predicate: (AccessibilityNodeInfo) -> Boolean): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        fun visit(node: AccessibilityNodeInfo?) {
            if (node == null || !node.isVisibleToUser) return
            if (predicate(node)) result.add(node)
            for (i in 0 until node.childCount) visit(node.getChild(i))
        }
        visit(automation.rootInActiveWindow)
        return result
    }

    private fun find(predicate: (AccessibilityNodeInfo) -> Boolean) = nodes(predicate).firstOrNull()
    private fun findText(text: String) = find { it.text?.toString() == text || it.contentDescription?.toString() == text }
    private fun clickText(text: String): Boolean =
        nodes { it.text?.toString() == text || it.contentDescription?.toString() == text }.any(::tapNode)
    private fun isChoiceSelected(label: String): Boolean {
        var node = findText(label)
        while (node != null) {
            if (node.isChecked || node.isSelected) return true
            node = node.parent
        }
        return false
    }
    private fun taggedWithText(tag: String, text: String): AccessibilityNodeInfo? = nodes {
        it.viewIdResourceName == tag && hasText(it, text)
    }.firstOrNull()
    private fun hasText(node: AccessibilityNodeInfo, text: String): Boolean {
        if (node.text?.toString() == text || node.contentDescription?.toString()?.contains(text) == true) return true
        return (0 until node.childCount).any { node.getChild(it)?.let { child -> hasText(child, text) } == true }
    }
    private fun bounds(node: AccessibilityNodeInfo) = Rect().also { node.getBoundsInScreen(it) }
    private fun tapNode(node: AccessibilityNodeInfo): Boolean {
        var target: AccessibilityNodeInfo? = node
        while (target != null && !target.isClickable) target = target.parent
        if (target?.isEnabled != true) return false
        val rect = bounds(target)
        tap(rect.exactCenterX(), rect.exactCenterY())
        return true
    }
    private fun tap(x: Float, y: Float) {
        val down = SystemClock.uptimeMillis()
        for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
            val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, x, y, 0)
            try { automation.injectInputEvent(event, true) } finally { event.recycle() }
        }
    }
    private fun scrollUp(): Boolean {
        val display = context.resources.displayMetrics
        val x = display.widthPixels / 2f
        val down = SystemClock.uptimeMillis()
        for (i in 0..10) {
            val action = when (i) { 0 -> MotionEvent.ACTION_DOWN; 10 -> MotionEvent.ACTION_UP; else -> MotionEvent.ACTION_MOVE }
            val event = MotionEvent.obtain(down, down + i * 30L, action, x, display.heightPixels * (.75f - .4f * i / 10), 0)
            try { automation.injectInputEvent(event, true) } finally { event.recycle() }
        }
        return false
    }

    private companion object {
        const val UID = "tazer-demo-local"
        val DateDescription = Regex("^\\p{L}+, \\d")
    }
}
