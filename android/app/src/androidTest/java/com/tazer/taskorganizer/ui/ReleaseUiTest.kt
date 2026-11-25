package com.tazer.taskorganizer.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import android.os.SystemClock
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tazer.taskorganizer.MainActivity
import com.tazer.taskorganizer.data.CalendarRepository
import com.tazer.taskorganizer.data.TaskRepository
import com.tazer.taskorganizer.data.TazerDatabase
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek
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

    private companion object { const val UID = "tazer-demo-local" }
}
