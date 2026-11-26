package com.tazer.taskorganizer.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.tazer.taskorganizer.domain.CourseColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** WCAG floor for the v1.9.0 blue-white neumorphic tokens (docs/UI_NEUMORPHIC_SPEC.md). */
class NeumorphicTokensTest {
    private fun ratio(a: Color, b: Color) = CourseColors.contrast(a.toArgb().toLong() and 0xFFFFFFFFL, b.toArgb().toLong() and 0xFFFFFFFFL)

    @Test
    fun canvasesMatchTheOwnerTokens() {
        assertEquals(0xFFEDF2FB.toInt(), Neu.Light.canvas.toArgb())
        assertEquals(0xFF141923.toInt(), Neu.Dark.canvas.toArgb())
        assertEquals(0xFF222B3D.toInt(), Neu.Dark.highlight.toArgb())
        assertEquals(0xFF090C12.toInt(), Neu.Dark.shade.toArgb())
        assertEquals(0xFF4F94FF.toInt(), Neu.Dark.accent.toArgb())
    }

    @Test
    fun textKeepsFourAndAHalfAndBoundariesThreeToOneInBothThemes() {
        for (neu in listOf(Neu.Light, Neu.Dark)) {
            for (page in listOf(neu.canvas, neu.canvasTop, neu.canvasBottom, neu.surface)) {
                assertTrue("ink", ratio(neu.ink, page) >= 4.5)
                assertTrue("muted", ratio(neu.muted, page) >= 4.5)
                assertTrue("accent text", ratio(neu.accent, page) >= 4.5)
                assertTrue("outline boundary", ratio(neu.outline, page) >= 3.0)
            }
            assertTrue("ink on recessed fields", ratio(neu.ink, neu.recessed) >= 4.5)
            assertTrue("muted placeholder on recessed fields", ratio(neu.muted, neu.recessed) >= 4.5)
            assertTrue("label on accent", ratio(neu.onAccent, neu.accent) >= 4.5)
        }
        // The Sign In label sits on the #2254E6 stop of the gradient.
        assertTrue(ratio(Color.White, Color(0xFF2254E6)) >= 4.5)
    }
}
