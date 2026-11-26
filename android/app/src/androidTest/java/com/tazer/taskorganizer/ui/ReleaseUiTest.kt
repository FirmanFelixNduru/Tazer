package com.tazer.taskorganizer.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import com.tazer.taskorganizer.data.CourseColorStore
import com.tazer.taskorganizer.data.SubtaskDraft
import com.tazer.taskorganizer.data.TazerBackup
import com.tazer.taskorganizer.domain.CourseColors
import com.tazer.taskorganizer.domain.IcsParser
import org.junit.Assert.assertFalse
import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.tazer.taskorganizer.data.AnalysisSource
import com.tazer.taskorganizer.data.SubtaskRepository
import com.tazer.taskorganizer.domain.BriefingKind
import com.tazer.taskorganizer.domain.BriefingSettings
import com.tazer.taskorganizer.integration.AndroidIntegration
import com.tazer.taskorganizer.timer.BriefingPreferences
import com.tazer.taskorganizer.timer.DailyBriefingAlarms
import java.io.FileOutputStream
import java.time.LocalDateTime
import org.junit.Assume.assumeTrue
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
                val control = find { it.contentDescription?.toString()?.startsWith("Pilih jam ") == true }
                control != null && bounds(control).bottom <= context.resources.displayMetrics.heightPixels * .8f || scrollUp()
            }
            val before = requireNotNull(find { it.contentDescription?.toString()?.startsWith("Pilih jam ") == true })
                .contentDescription.toString()
            assertTrue(clickVisible(before))
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
    fun jadwalPlusOffersEventsAndFixedPagesAndFocusSettingsLiveOnlyInSettings() {
        try {
            launch()
            waitFor("Schedule unavailable") { clickText("Jadwal") }
            waitFor("Jadwal + button missing") { find { it.contentDescription?.toString() == "Tambah ke jadwal" } != null }
            assertEquals("Focus settings must not repeat on Jadwal", null, findText("Pengaturan fokus"))
            waitFor("Jadwal + button unavailable") { clickText("Tambah ke jadwal") }
            waitFor("Two add options missing") { findText("Tambah Acara") != null && findText("Jadwal Tetap") != null }
            assertEquals(2, nodes { it.text?.toString() in setOf("Tambah Acara", "Jadwal Tetap") }.size)
            waitFor("Events option unavailable") { clickText("Tambah Acara") }
            waitFor("Events page did not open") { findText("Judul acara") != null }
            assertEquals("The events page is manual only", null, findText("Tambah Jadwal Tetap"))
            assertEquals(null, findText("Impor .csv"))
            waitFor("Back unavailable") { clickText("Kembali") }
            waitFor("Jadwal + button unavailable") { clickText("Tambah ke jadwal") }
            waitFor("Fixed option unavailable") { clickText("Jadwal Tetap") }
            waitFor("Fixed page dropdown missing") { clickText("Tambah Jadwal Tetap") }
            for (option in listOf("Input Manual", "Impor .ics", "Impor .csv", "Impor .xlsx (Excel)", "Foto / Screenshot Jadwal")) {
                waitFor("Dropdown option $option missing") { nodes { hasText(it, option) }.isNotEmpty() }
            }
            for (icon in listOf("Ikon file CSV", "Ikon spreadsheet Excel")) {
                waitFor("Vector icon $icon missing") { nodes { it.contentDescription?.toString()?.contains(icon) == true }.isNotEmpty() }
            }
            assertEquals("CSV and Excel must be drawn icons, not text badges", 0, nodes {
                it.text?.toString() in setOf("CSV", "XLS", "XLSX")
            }.size)
            automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
            waitFor("Back unavailable") { clickText("Kembali") }

            waitFor("Calendar unavailable") { clickText("Kalender") }
            waitFor("Calendar did not open") { findText("Tambah acara") != null }
            assertEquals("Calendar no longer imports weekly schedules", null, findText("Impor .ics"))
            waitFor("Settings unavailable") { clickText("Pengaturan") }
            var focusButtons = 0
            repeat(12) {
                assertEquals("Settings no longer manages weekly schedules", null, findText("Tambah jadwal tetap"))
                assertEquals(null, findText("Jadwal tetap"))
                focusButtons = maxOf(focusButtons, nodes { it.text?.toString() == "Pengaturan fokus" }.size)
                if (findText("Keluar") != null) return@repeat
                gentleScroll()
            }
            assertEquals("Focus settings appear exactly once in Settings", 1, focusButtons)
        } finally {
            finish()
        }
    }

    @Test
    fun eventsPageSavesAManualEventWithEndTimeAndColour() = runBlocking {
        val calendar = CalendarRepository(TazerDatabase.getInstance(context))
        val title = "Acara " + UUID.randomUUID().toString().take(6)
        try {
            launch()
            waitFor("Schedule unavailable") { clickText("Jadwal") }
            waitFor("Jadwal + button unavailable") { clickText("Tambah ke jadwal") }
            waitFor("Events option unavailable") { clickText("Tambah Acara") }
            setText("Judul acara", title)
            waitFor("Colour choice unavailable") { clickVisible("Warna Biru") }
            waitFor("Save unavailable") { clickVisible("Simpan Acara") }
            waitFor("Event was not saved") { runBlocking { calendar.observeEvents(UID).first().any { it.title == title } } }
            val saved = calendar.observeEvents(UID).first().first { it.title == title }
            assertEquals(Triple(9 * 60, 10 * 60, 1), Triple(saved.startMinute, saved.endMinute, saved.colorIndex))
            waitFor("Saved list missing the event") { findText(title) != null || gentleScroll() }
        } finally {
            calendar.observeEvents(UID).first().filter { it.title == title }.forEach { calendar.deleteEvent(UID, it.id) }
            finish()
        }
    }

    @Test
    fun fixedPageManualInputSavesOneBlockPerRepeatingDay() = runBlocking {
        val dao = TazerDatabase.getInstance(context).tazerDao()
        val title = "Manual " + UUID.randomUUID().toString().take(6)
        try {
            openFixedPage()
            waitFor("Dropdown unavailable") { clickText("Tambah Jadwal Tetap") }
            waitFor("Manual option unavailable") { clickText("Input Manual") }
            setText("Nama jadwal / mata kuliah", title)
            waitFor("Wednesday chip unavailable") { clickVisible("Rabu") }
            waitFor("Save unavailable") { clickVisible("Simpan Jadwal") }
            waitFor("Two weekly blocks were not saved") { runBlocking { dao.findFixedSchedules(UID).count { it.label == title } == 2 } }
            assertEquals(setOf(1, 3), dao.findFixedSchedules(UID).filter { it.label == title }.map { it.dayOfWeek }.toSet())
        } finally {
            dao.findFixedSchedules(UID).filter { it.label == title }.forEach { dao.deleteFixedSchedule(UID, it.id) }
            finish()
        }
    }

    @Test
    fun icsCsvAndXlsxImportsShareOnePreviewWithDuplicatesAndClashes() = runBlocking {
        val database = TazerDatabase.getInstance(context)
        val calendar = CalendarRepository(database)
        val dao = database.tazerDao()
        val tag = UUID.randomUUID().toString().take(6)
        val ics = File(context.cacheDir, "fixed-$tag.ics").apply {
            writeText("BEGIN:VCALENDAR\r\nVERSION:2.0\r\nBEGIN:VEVENT\r\nUID:$tag-event\r\nDTSTART;VALUE=DATE:20261126\r\n" +
                "SUMMARY:Ics acara $tag\r\nEND:VEVENT\r\nBEGIN:VEVENT\r\nUID:$tag-weekly\r\nDTSTART:20261126T090000\r\n" +
                "DTEND:20261126T100000\r\nSUMMARY:Ics kuliah $tag\r\nRRULE:FREQ=WEEKLY\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n")
        }
        val csv = File(context.cacheDir, "fixed-$tag.csv").apply {
            writeText("Mata Kuliah;Hari;Jam;Ruang\nCsv basis $tag;Senin;08:00-09:40;B-201\nCsv basis $tag;Senin;08:00-09:40;B-201\n" +
                "Csv algo $tag;Senin;09:00-10:30;Lab 2\n")
        }
        val xlsx = File(context.cacheDir, "fixed-$tag.xlsx").apply {
            java.util.zip.ZipOutputStream(outputStream()).use { zip ->
                zip.putNextEntry(java.util.zip.ZipEntry("xl/worksheets/sheet1.xml"))
                zip.write(("<worksheet><sheetData>" +
                    "<row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>Nama</t></is></c><c r=\"B1\" t=\"inlineStr\"><is><t>Hari</t></is></c>" +
                    "<c r=\"C1\" t=\"inlineStr\"><is><t>Mulai</t></is></c><c r=\"D1\" t=\"inlineStr\"><is><t>Selesai</t></is></c></row>" +
                    "<row r=\"2\"><c r=\"A2\" t=\"inlineStr\"><is><t>Xlsx statistik $tag</t></is></c><c r=\"B2\" t=\"inlineStr\"><is><t>Sabtu</t></is></c>" +
                    "<c r=\"C2\"><v>0.3125</v></c><c r=\"D2\"><v>0.38194444444444442</v></c></row>" +
                    "</sheetData></worksheet>").toByteArray())
                zip.closeEntry()
            }
        }
        fun pick(file: File): Instrumentation.ActivityMonitor = instrumentation.addMonitor(
            IntentFilter(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); addDataType("*/*") },
            Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(Uri.fromFile(file))), true,
        )
        fun importFrom(option: String, file: File, check: () -> Unit, saveLabel: String) {
            val monitor = pick(file)
            try {
                waitFor("Dropdown unavailable") { clickText("Tambah Jadwal Tetap") }
                waitFor("$option unavailable") { clickText(option) }
                waitFor("Picker for $option not requested") { monitor.hits > 0 }
                waitFor("Unified preview for $option missing") { nodes { it.text?.toString()?.startsWith("Pratinjau impor") == true }.isNotEmpty() }
                check()
                waitFor("$saveLabel unavailable") { clickText(saveLabel) }
                waitFor("Preview did not close") { nodes { it.text?.toString()?.startsWith("Pratinjau impor") == true }.isEmpty() }
            } finally {
                instrumentation.removeMonitor(monitor)
            }
        }
        try {
            openFixedPage()
            importFrom("Impor .ics", ics, {
                waitFor("ICS row missing") { findText("Ics kuliah $tag") != null }
                waitFor("Events toggle missing") { findText("Juga impor 1 acara sekali ke Kalender") != null }
            }, "Simpan 1 Jadwal Terpilih")
            waitFor("ICS rows not saved") { runBlocking { dao.findFixedSchedules(UID).any { it.label == "Ics kuliah $tag" } } }
            assertEquals(1, calendar.observeEvents(UID).first().count { it.title == "Ics acara $tag" })

            importFrom("Impor .csv", csv, {
                waitFor("Duplicate summary missing") { nodes { it.text?.toString()?.contains("1 duplikat dilewati") == true }.isNotEmpty() }
                waitFor("Clash warning missing") { nodes { it.text?.toString()?.startsWith("Bentrok dengan") == true }.isNotEmpty() }
                waitFor("Duplicate row label missing") { findText("Sudah tersimpan, dilewati") != null }
            }, "Simpan 2 Jadwal Terpilih")
            waitFor("CSV rows not saved") { runBlocking { dao.findFixedSchedules(UID).count { it.label.startsWith("Csv ") && it.label.endsWith(tag) } == 2 } }

            importFrom("Impor .xlsx (Excel)", xlsx, {
                waitFor("XLSX row missing") { findText("Xlsx statistik $tag") != null && findText("Sabtu · 07:30 – 09:10") != null }
            }, "Simpan 1 Jadwal Terpilih")
            waitFor("XLSX row not saved") { runBlocking { dao.findFixedSchedules(UID).any { it.label == "Xlsx statistik $tag" && it.startMinute == 450 } } }

            importFrom("Impor .csv", csv, {
                waitFor("Re-import should mark every row as saved") { nodes { it.text?.toString()?.contains("3 duplikat dilewati") == true }.isNotEmpty() }
            }, "Batal")
            assertEquals(2, dao.findFixedSchedules(UID).count { it.label.startsWith("Csv ") && it.label.endsWith(tag) })
        } finally {
            dao.findFixedSchedules(UID).filter { it.label.endsWith(tag) }.forEach { dao.deleteFixedSchedule(UID, it.id) }
            calendar.observeEvents(UID).first().filter { it.title.endsWith(tag) }.forEach { calendar.deleteEvent(UID, it.id) }
            listOf(ics, csv, xlsx).forEach(File::delete)
            finish()
        }
    }

    @Test
    fun timetablePhotoFillsTheSamePreviewFromTheMockBackend() = runBlocking {
        assumeTrue("Needs the loopback mock backend", AndroidIntegration(context).mockEnabled)
        val dao = TazerDatabase.getInstance(context).tazerDao()
        val image = File(context.cacheDir, "scan").apply { mkdirs() }.resolve("krs-test.png")
        FileOutputStream(image).use { output ->
            Bitmap.createBitmap(1400, 900, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.WHITE) }
                .compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        val picked = FileProvider.getUriForFile(context, "${context.packageName}.scan", image)
        var monitor: Instrumentation.ActivityMonitor? = null
        try {
            activity = instrumentation.startActivitySync(Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            waitFor("Mock PRO workspace did not open") { findText("Belum Selesai") != null || (clickText("PRO") && false) }
            dao.findFixedSchedules(MOCK_PRO).forEach { dao.deleteFixedSchedule(MOCK_PRO, it.id) }
            waitFor("Schedule unavailable") { clickText("Jadwal") }
            waitFor("Jadwal + button unavailable") { clickText("Tambah ke jadwal") }
            waitFor("Fixed option unavailable") { clickText("Jadwal Tetap") }
            monitor = instrumentation.addMonitor(IntentFilter(MediaStore.ACTION_PICK_IMAGES).apply { addDataType("image/*") },
                Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(picked)), true)
            waitFor("Dropdown unavailable") { clickText("Tambah Jadwal Tetap") }
            waitFor("Photo option unavailable") { clickText("Foto / Screenshot Jadwal") }
            waitFor("Gallery choice unavailable") { clickText("Galeri / Screenshot") }
            waitFor("Photo preview missing") { findText("Pratinjau impor Foto") != null && findText("3 jadwal terbaca") != null }
            waitFor("Save unavailable") { clickText("Simpan 3 Jadwal Terpilih") }
            waitFor("Photo rows not saved") { runBlocking { dao.findFixedSchedules(MOCK_PRO).size == 3 } }
            assertEquals(setOf(1, 3, 4), dao.findFixedSchedules(MOCK_PRO).map { it.dayOfWeek }.toSet())
        } finally {
            monitor?.let(instrumentation::removeMonitor)
            dao.findFixedSchedules(MOCK_PRO).forEach { dao.deleteFixedSchedule(MOCK_PRO, it.id) }
            image.delete()
            finish()
        }
    }

    private fun openFixedPage() {
        launch()
        waitFor("Schedule unavailable") { clickText("Jadwal") }
        waitFor("Jadwal + button unavailable") { clickText("Tambah ke jadwal") }
        waitFor("Fixed option unavailable") { clickText("Jadwal Tetap") }
        waitFor("Fixed page did not open") { findText("Tambah Jadwal Tetap") != null }
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

    @Test
    fun galleryScanPrefillsTheNewTaskFormForReviewBeforeSaving() = runBlocking {
        assumeTrue("Needs the loopback mock backend", AndroidIntegration(context).mockEnabled)
        val title = "Laporan praktikum basis data"
        val tasks = TaskRepository(TazerDatabase.getInstance(context))
        val image = File(context.cacheDir, "scan").apply { mkdirs() }.resolve("ui-test.png")
        FileOutputStream(image).use { output ->
            Bitmap.createBitmap(1600, 900, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.WHITE) }
                .compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        val picked = FileProvider.getUriForFile(context, "${context.packageName}.scan", image)
        // An action-only filter also matches action-less intents, so add the image type and register after launch.
        val filter = IntentFilter(MediaStore.ACTION_PICK_IMAGES).apply { addDataType("image/*") }
        var monitor: Instrumentation.ActivityMonitor? = null
        try {
            activity = instrumentation.startActivitySync(Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            waitFor("Mock PRO workspace did not open") { findText("Belum Selesai") != null || (clickText("PRO") && false) }
            monitor = instrumentation.addMonitor(
                filter, Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(picked)), true,
            )
            waitFor("Add task unavailable") { clickText("Tambah tugas") }
            waitFor("Scan button missing") { clickText("Scan Foto / Screenshot") }
            waitFor("Gallery choice missing") { clickText("Galeri / Screenshot") }
            waitFor("Photo picker was not requested") { requireNotNull(monitor).hits > 0 }
            waitFor("Scan did not prefill the title") { findText(title) != null }
            for (expected in listOf("Basis Data", "Form terisi dari gambar", "• Jam deadline tidak terlihat, diatur ke 23:59.",
                "23:59", "Baca modul normalisasi", "Gambar diagram ERD")) {
                waitFor("Prefilled value missing: $expected") { findText(expected) != null || scrollUp() }
            }
            assertTrue("A scan must not save before review", tasks.observeTasks(MOCK_PRO).first().none { it.title == title })
            waitFor("Save button unavailable") { clickVisible("Simpan tugas") }
            waitFor("Reviewed task was not saved") { runBlocking { tasks.observeTasks(MOCK_PRO).first().any { it.title == title } } }
            val saved = tasks.observeTasks(MOCK_PRO).first().first { it.title == title }
            val zone = ZoneId.of(requireNotNull(tasks.observeProfile(MOCK_PRO).first()).zoneId)
            assertEquals("Basis Data", saved.course)
            assertEquals(LocalDateTime.of(2026, 12, 4, 23, 59).atZone(zone).toInstant().toEpochMilli(), saved.deadlineAt)
            assertEquals(3, saved.difficultyScore)
            assertEquals(AnalysisSource.AI, saved.analysisSource)
            assertEquals(3, SubtaskRepository(TazerDatabase.getInstance(context)).observe(MOCK_PRO).first()
                .count { it.taskId == saved.id })
        } finally {
            monitor?.let(instrumentation::removeMonitor)
            tasks.observeTasks(MOCK_PRO).first().filter { it.title == title }.forEach { tasks.deleteTask(MOCK_PRO, it.id) }
            image.delete()
            finish()
        }
    }

    @Test
    fun scanExplainsWhyItNeedsProOrByokOnOtherAccounts() {
        try {
            launch()
            waitFor("Add task unavailable") { clickText("Tambah tugas") }
            waitFor("Scan availability is not explained") {
                findText("Tersedia untuk akun PRO atau BYOK Gemini. Gambar hanya diproses saat kamu memindainya.") != null
            }
            waitFor("Scan button missing") { clickText("Scan Foto / Screenshot") }
            waitFor("Non-PRO scan message missing") { findText("Scan foto tersedia untuk akun PRO atau BYOK Gemini.") != null }
            assertEquals(null, findText("Galeri / Screenshot"))
        } finally {
            finish()
        }
    }

    @Test
    fun dailyBriefingSettingsUseSpinnersPersistAndScheduleAlarms() = runBlocking {
        val preferences = BriefingPreferences(context)
        val original = preferences.read()
        automation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        preferences.save(BriefingSettings())
        DailyBriefingAlarms.reschedule(context)
        try {
            launch()
            waitFor("Settings unavailable") { clickText("Pengaturan") }
            waitFor("Daily Briefing section missing") { findText("Smart Daily Briefing") != null }
            assertTrue(!DailyBriefingAlarms.scheduled(context, BriefingKind.MORNING))
            waitFor("Morning switch unavailable") { tapSwitch("Briefing pagi") }
            waitFor("Morning briefing was not saved and scheduled") {
                preferences.read().morningEnabled && DailyBriefingAlarms.scheduled(context, BriefingKind.MORNING)
            }
            val exact = FocusRuntime.get(context).canScheduleExactAlarms()
            waitFor("Exact alarm hint must show only without exact access") {
                (findText("Izinkan alarm tepat waktu") != null) == !exact
            }
            waitFor("Morning time button missing") { clickVisible("Pilih jam 07:00") }
            waitFor("Spinner time picker did not open") {
                findText("Pilih jam") != null && nodes { it.className == "android.widget.NumberPicker" }.size == 2
            }
            // The left wheel is the hour; tapping below its centre increments it like a user would.
            val hour = bounds(nodes { it.className?.toString() == "android.widget.NumberPicker" }.minBy { bounds(it).left })
            tap(hour.exactCenterX(), hour.top + hour.height() * .82f)
            SystemClock.sleep(500)
            waitFor("Apply unavailable") { clickText("Terapkan") }
            waitFor("Morning time was not saved") { preferences.read().morningMinute == 8 * 60 }

            finish()
            launch()
            waitFor("Settings unavailable after restart") { clickText("Pengaturan") }
            waitFor("Saved morning time missing after restart") { findText("Pilih jam 08:00") != null }
            assertTrue("Morning switch lost its state", switchChecked("Briefing pagi"))
            waitFor("Evening switch unavailable") { tapSwitch("Review malam") }
            waitFor("Evening review was not scheduled") {
                preferences.read().eveningEnabled && DailyBriefingAlarms.scheduled(context, BriefingKind.EVENING)
            }
            assertEquals(21 * 60, preferences.read().eveningMinute)
            waitFor("Morning switch unavailable") { tapSwitch("Briefing pagi") }
            waitFor("Disabled briefing alarm was not cancelled") {
                !preferences.read().morningEnabled && !DailyBriefingAlarms.scheduled(context, BriefingKind.MORNING)
            }
        } finally {
            finish()
            preferences.save(original)
            DailyBriefingAlarms.reschedule(context)
        }
    }

    @Test
    fun briefingNotificationsUseTheirOwnChannelAndOpenScheduleOrStatistics() = runBlocking {
        val preferences = BriefingPreferences(context)
        val original = preferences.read()
        val manager = context.getSystemService(NotificationManager::class.java)
        automation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        try {
            launch() // Establishes the demo owner used by the background briefing.
            preferences.save(BriefingSettings(morningEnabled = true, eveningEnabled = true))
            for (kind in BriefingKind.entries) DailyBriefingAlarms.deliver(context, kind)
            val posted = manager.activeNotifications.filter { it.id == 4100 || it.id == 4101 }.associateBy { it.id }
            assertEquals(setOf(4100, 4101), posted.keys)
            posted.values.forEach { assertEquals("daily_briefing", it.notification.channelId) }
            val morning = requireNotNull(posted[4100]).notification
            assertTrue(morning.extras.getString(Notification.EXTRA_TITLE).orEmpty().startsWith("Hari ini"))
            for (id in listOf("focus_timer", "focus_reminders_v2", "focus_overtime_v2")) {
                assertTrue("Focus channel $id changed", manager.getNotificationChannel(id) != null)
            }

            requireNotNull(posted[4101]).notification.contentIntent.send()
            waitFor("Evening review did not open Statistics") { navigationSelected("Statistik") }
            SystemClock.sleep(500)
            morning.contentIntent.send()
            waitFor("Morning briefing did not open today's schedule") { navigationSelected("Jadwal") }

            preferences.save(BriefingSettings())
            manager.cancel(4100)
            DailyBriefingAlarms.deliver(context, BriefingKind.MORNING)
            assertTrue("A disabled briefing must not post", manager.activeNotifications.none { it.id == 4100 })
        } finally {
            manager.cancel(4100)
            manager.cancel(4101)
            finish()
            preferences.save(original)
            DailyBriefingAlarms.reschedule(context)
        }
    }

    @Test
    fun searchAndFiltersNarrowBothTabsAndResetClearly() = runBlocking {
        val tasks = TaskRepository(TazerDatabase.getInstance(context))
        val tag = UUID.randomUUID().toString().take(6)
        val now = System.currentTimeMillis()
        val ids = mutableListOf<String>()
        try {
            launch()
            ids += tasks.createTask(UID, "Cari $tag laporan", "Kimia $tag", "Titrasi asam", now + 86_400_000, 4, 60, now).id
            ids += tasks.createTask(UID, "Cari $tag esai", "Sastra $tag", "Puisi lama", now + 86_400_000, 2, 30, now).id
            ids += tasks.createTask(UID, "Cari $tag kuis", "Kimia $tag", "Stoikiometri", now + 86_400_000, 1, 20, now).id
                .also { assertTrue(tasks.completeTask(UID, it, now)) }
            waitFor("Search field missing") { find { it.contentDescription?.toString() == "Cari tugas" } != null }
            setText("Cari tugas", "titrasi $tag")
            waitFor("Search did not narrow the active tab") {
                findText("Cari $tag laporan") != null && findText("Cari $tag esai") == null
            }
            setText("Cari tugas", tag)
            waitFor("Course chip missing") { clickVisible("Sastra $tag") }
            waitFor("Course filter did not apply") { findText("Cari $tag esai") != null && findText("Cari $tag laporan") == null }
            waitFor("Difficulty chip missing") { clickVisible("Kesulitan 5") }
            waitFor("Informative empty state missing") { findText("Tidak ada tugas aktif yang cocok") != null }
            waitFor("Reset unavailable") { clickVisible("Reset filter") }
            waitFor("Reset did not restore the list") { findText("Cari $tag laporan") != null }
            waitFor("Done tab unavailable") { clickText("Selesai") }
            setText("Cari tugas", "stoikiometri $tag")
            waitFor("Search did not work on the done tab") { findText("Cari $tag kuis") != null }
            setText("Cari tugas", "titrasi $tag")
            waitFor("Done tab empty state missing") { findText("Tidak ada tugas selesai yang cocok") != null }
        } finally {
            ids.forEach { tasks.deleteTask(UID, it) }
            finish()
        }
    }

    @Test
    fun courseChipsKeepTheirPaletteColourInLightAndDarkAndHonourOverrides() = runBlocking {
        val tasks = TaskRepository(TazerDatabase.getInstance(context))
        val preference = ThemePreference(context)
        val original = preference.read()
        val store = CourseColorStore(context)
        val originalColors = store.read(UID)
        val course = "Warna " + UUID.randomUUID().toString().take(6)
        val now = System.currentTimeMillis()
        var taskId: String? = null
        try {
            taskId = tasks.createTask(UID, "Uji warna $course", course, "", now + 86_400_000, 3, 30, now).id
            val override = (CourseColors.automaticIndex(course) + 3) % CourseColors.palette.size
            for ((mode, chosen) in listOf(TazerThemeMode.LIGHT to null, TazerThemeMode.DARK to null,
                TazerThemeMode.LIGHT to override, TazerThemeMode.DARK to override)) {
                finish()
                preference.save(mode)
                store.save(UID, if (chosen == null) emptyMap() else mapOf(CourseColors.key(course) to chosen))
                launch()
                val swatch = CourseColors.palette[chosen ?: CourseColors.automaticIndex(course)]
                val expected = swatch.tone(mode == TazerThemeMode.DARK).container.toInt()
                waitFor("Course chip for $course did not show its colour in $mode") {
                    // The task card's chip sits below the white filter chip that carries the same label.
                    val chip = nodes { it.text?.toString() == course }.maxByOrNull { bounds(it).top } ?: return@waitFor scrollUp()
                    val rect = bounds(chip)
                    val shot = automation.takeScreenshot() ?: return@waitFor false
                    try {
                        // The label's start padding sits on the chip container, between the dot and the text.
                    sameColor(expected, shot.getPixel(rect.left + 2, rect.centerY()))
                    } finally {
                        shot.recycle()
                    }
                }
            }
        } finally {
            taskId?.let { tasks.deleteTask(UID, it) }
            store.save(UID, originalColors)
            finish()
            preference.save(original)
        }
    }

    @Test
    fun icsAndBackupExportThroughSafAndRestoreMergesWithPreview() = runBlocking {
        val database = TazerDatabase.getInstance(context)
        val tasks = TaskRepository(database)
        val tag = UUID.randomUUID().toString().take(6)
        val now = System.currentTimeMillis()
        val ics = File(context.cacheDir, "export-$tag.ics")
        val backup = File(context.cacheDir, "backup-$tag.json")
        val created = mutableListOf<String>()
        fun monitor(action: String, type: String, file: File) = instrumentation.addMonitor(
            IntentFilter(action).apply { addCategory(Intent.CATEGORY_OPENABLE); addDataType(type) },
            Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(Uri.fromFile(file))), true,
        )
        var monitors = emptyList<Instrumentation.ActivityMonitor>()
        try {
            launch()
            created += tasks.createTask(UID, "Ekspor $tag", "Backup $tag", "", now + 86_400_000, 2, 30, now).id
            monitors = listOf(
                monitor(Intent.ACTION_CREATE_DOCUMENT, "text/calendar", ics),
                monitor(Intent.ACTION_CREATE_DOCUMENT, "application/json", backup),
                monitor(Intent.ACTION_OPEN_DOCUMENT, "*/*", backup),
            )
            waitFor("Settings unavailable") { clickText("Pengaturan") }
            waitFor("ICS export unavailable") { clickVisible("Ekspor .ics") }
            waitFor("ICS file was not written") { ics.length() > 0 }
            val calendar = ics.readText()
            assertTrue(calendar.startsWith("BEGIN:VCALENDAR\r\n") && calendar.trimEnd().endsWith("END:VCALENDAR"))
            assertEquals(0, IcsParser.parse(ics.inputStream(), ZoneId.systemDefault()).invalid)

            waitFor("Backup export unavailable") { clickVisible("Ekspor Backup JSON") }
            waitFor("Backup file was not written") { backup.length() > 0 && runCatching { TazerBackup.decode(backup.readText()) }.isSuccess }
            val exported = backup.readText()
            assertFalse("backup leaks the account id", exported.contains(UID))
            assertTrue(TazerBackup.decode(exported).tasks.any { it.title == "Ekspor $tag" })

            // A friend's backup: one new task plus the rows this account already has.
            val data = TazerBackup.decode(exported)
            val extra = data.tasks.first { it.title == "Ekspor $tag" }.copy(id = UUID.randomUUID().toString(), title = "Pulih $tag")
            backup.writeText(TazerBackup.encode(data.copy(tasks = data.tasks + extra), now, "test"))
            waitFor("Backup import unavailable") { clickVisible("Impor Backup JSON") }
            waitFor("Backup preview missing") { findText("Pratinjau backup") != null && findText("1 tugas baru") != null }
            waitFor("Restore unavailable") { clickText("Pulihkan") }
            waitFor("Restored task missing") {
                runBlocking { tasks.observeTasks(UID).first().any { it.title == "Pulih $tag" } }
            }
            created += tasks.observeTasks(UID).first().filter { it.title == "Pulih $tag" }.map { it.id }
            waitFor("Backup import unavailable for duplicate check") { clickVisible("Impor Backup JSON") }
            waitFor("Duplicate preview missing") { findText("0 tugas baru") != null }
            waitFor("Close unavailable") { clickText("Tutup") }
            assertEquals(1, tasks.observeTasks(UID).first().count { it.title == "Pulih $tag" })
        } finally {
            monitors.forEach(instrumentation::removeMonitor)
            created.forEach { tasks.deleteTask(UID, it) }
            ics.delete()
            backup.delete()
            finish()
        }
    }

    @Test
    fun sharedTaskPackageImportsWithAllSubtasksAfterPreview() = runBlocking {
        val database = TazerDatabase.getInstance(context)
        val tasks = TaskRepository(database)
        val subtasks = SubtaskRepository(database)
        val tag = UUID.randomUUID().toString().take(6)
        val now = System.currentTimeMillis()
        var shared: String? = null
        val chooser = object : Instrumentation.ActivityMonitor() {
            override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
                if (intent.action != Intent.ACTION_CHOOSER) return null
                @Suppress("DEPRECATION")
                shared = (intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT))?.getStringExtra(Intent.EXTRA_TEXT)
                return Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null)
            }
        }
        var sourceId: String? = null
        try {
            launch()
            sourceId = tasks.createTask(UID, "Paket $tag", "Kelompok $tag", "Bagi tugas presentasi", now + 3 * 86_400_000L, 4, 90, now).id
            subtasks.replace(UID, requireNotNull(sourceId), listOf("Riset materi", "Buat slide", "Latihan presentasi")
                .map { SubtaskDraft(null, it, false) })
            instrumentation.addMonitor(chooser)
            waitFor("Share action missing") { clickVisible("Bagikan tugas Paket $tag") }
            waitFor("Share sheet was not requested") { shared != null }
            val text = requireNotNull(shared)
            assertTrue(text.contains("Mata kuliah: Kelompok $tag") && text.contains("3. Latihan presentasi"))
            assertFalse(text.contains(UID))
            tasks.deleteTask(UID, requireNotNull(sourceId))
            sourceId = null

            val link = Regex("tazer://import-task\\?d=[A-Za-z0-9_-]+").find(text)!!.value
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)).setPackage(context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
            waitFor("Package preview missing") { findText("Pratinjau paket tugas") != null && findText("Paket $tag") != null }
            for (expected in listOf("Kelompok $tag", "Kesulitan 4/5 · Estimasi 90 menit", "Subtask (3)", "2. Buat slide")) {
                waitFor("Preview lacks $expected") { findText(expected) != null }
            }
            assertTrue("Nothing may save before confirmation", tasks.observeTasks(UID).first().none { it.title == "Paket $tag" })
            waitFor("Save unavailable") { clickText("Simpan tugas") }
            waitFor("Imported task missing") { runBlocking { tasks.observeTasks(UID).first().any { it.title == "Paket $tag" } } }
            val imported = tasks.observeTasks(UID).first().first { it.title == "Paket $tag" }
            sourceId = imported.id
            assertEquals(Triple("Kelompok $tag", 4, 90), Triple(imported.course, imported.difficultyScore, imported.initialEstimatedMinutes))
            assertEquals(listOf("Riset materi", "Buat slide", "Latihan presentasi"),
                subtasks.observe(UID).first().filter { it.taskId == imported.id }.sortedBy { it.position }.map { it.title })

            waitFor("Add task unavailable") { clickText("Tambah tugas") }
            waitFor("Package import button missing") { clickVisible("Impor Paket Tugas") }
            waitFor("Paste dialog missing") { findText("Teks atau kode paket") != null }
            setText("Teks atau kode paket", "bukan kode")
            waitFor("Preview button unavailable") { clickText("Pratinjau") }
            waitFor("Invalid code message missing") { findText("Kode paket tugas tidak ditemukan.") != null }
        } finally {
            instrumentation.removeMonitor(chooser)
            sourceId?.let { tasks.deleteTask(UID, it) }
            finish()
        }
    }

    @Test
    fun releaseNotesRenderWithoutMarkdownSymbols() {
        try {
            launch()
            val markdown = "## Tazer **1.7.0**\n\n- **Pencarian.** Cari tugas di dua tab.\n* Warna per mata kuliah\n\nPasang APK di atas v1.6.0."
            instrumentation.runOnMainSync {
                (activity as ComponentActivity).setContent { MaterialTheme { Column { ReleaseNotesView(markdown) } } }
            }
            waitFor("Release notes did not render") { findText("Tazer 1.7.0") != null }
            assertTrue(nodes { it.text?.toString()?.contains("Pencarian. Cari tugas di dua tab.") == true }.isNotEmpty())
            assertTrue(findText("Warna per mata kuliah") != null)
            assertTrue(nodes { node -> listOf("##", "**", "- ", "* ").any { node.text?.toString()?.startsWith(it) == true } ||
                node.text?.toString()?.contains("**") == true }.isEmpty())
        } finally {
            finish()
        }
    }

    private fun setText(label: String, value: String) {
        waitFor("Text field $label missing") {
            val field = find { (it.contentDescription?.toString() == label || hasText(it, label)) && it.isEditable }
                ?: generateSequence(find { it.text?.toString() == label || it.contentDescription?.toString() == label }) { it.parent }
                    .firstOrNull { it.isEditable }
            field?.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, android.os.Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
            }) == true
        }
    }

    /** A slow drag that scrolls about a quarter screen without a fling, so mid-page targets are not skipped. */
    private fun gentleScroll(upward: Boolean = false): Boolean {
        val display = context.resources.displayMetrics
        val x = display.widthPixels / 2f
        val down = SystemClock.uptimeMillis()
        for (i in 0..20) {
            val action = when (i) { 0 -> MotionEvent.ACTION_DOWN; 20 -> MotionEvent.ACTION_UP; else -> MotionEvent.ACTION_MOVE }
            val travel = .25f * minOf(i, 18) / 18
            val y = display.heightPixels * if (upward) .45f + travel else .70f - travel
            val event = MotionEvent.obtain(down, down + i * 40L, action, x, y, 0)
            try { automation.injectInputEvent(event, true) } finally { event.recycle() }
        }
        SystemClock.sleep(300)
        return false
    }

    private fun sameColor(expected: Int, actual: Int) = listOf(16, 8, 0).all { shift ->
        kotlin.math.abs((expected shr shift and 0xFF) - (actual shr shift and 0xFF)) <= 6
    }

    private fun switchNode(label: String) = nodes {
        it.isCheckable && (it.text?.toString()?.contains(label) == true || hasText(it, label))
    }.firstOrNull()
    private fun navigationSelected(label: String): Boolean {
        if (Build.VERSION.SDK_INT >= 33) automation.clearCache() // Selection changes do not always refresh cached nodes.
        return nodes { it.text?.toString() == label }.any { node -> generateSequence(node) { it.parent }.any { it.isSelected } }
    }
    /** Scrolls until the target is on screen, then clicks it through accessibility, independent of position. */
    private fun clickVisible(text: String, upward: Boolean = false): Boolean {
        var node = find { it.text?.toString() == text || it.contentDescription?.toString() == text } ?: return gentleScroll(upward)
        while (!node.isClickable) node = node.parent ?: return false
        return node.isEnabled && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }
    private fun tapSwitch(label: String): Boolean = switchNode(label)?.let(::tapNode) == true
    private fun switchChecked(label: String): Boolean = switchNode(label)?.isChecked == true

    private fun waitFor(message: String, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 12_000L
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition()) return
            SystemClock.sleep(100)
        }
        val visible = runCatching { nodes { !it.text.isNullOrBlank() }.map { it.text.toString() }.take(30) }.getOrDefault(emptyList())
        throw AssertionError("$message | visible: $visible")
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
        const val MOCK_PRO = "demo-pro"
        val DateDescription = Regex("^\\p{L}+, \\d")
    }
}
