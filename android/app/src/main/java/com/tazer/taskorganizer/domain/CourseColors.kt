package com.tazer.taskorganizer.domain

import java.util.Locale
import kotlin.math.pow

/** A chip colour pair: [container] behind [ink] text, as 0xAARRGGBB. */
data class CourseTone(val container: Long, val ink: Long, val accent: Long)

/** One hue with a Light (white page) and a Dark (black page) rendering. */
data class CourseSwatch(val name: String, val light: CourseTone, val dark: CourseTone) {
    fun tone(dark: Boolean) = if (dark) this.dark else light
}

object CourseColors {
    val palette = listOf(
        swatch("Hijau", 0xFFC8EBD9, 0xFF0B3D27, 0xFF1F7A4D, 0xFF173B2B, 0xFFC5EFD8, 0xFF6FD1A0),
        swatch("Biru", 0xFFD3E3FB, 0xFF0D2E5C, 0xFF2D63B8, 0xFF1A2E4D, 0xFFD3E3FB, 0xFF8AB4F8),
        swatch("Ungu", 0xFFE6DAF7, 0xFF35185C, 0xFF7446B3, 0xFF2F2147, 0xFFE6DAF7, 0xFFC2A3F0),
        swatch("Merah muda", 0xFFF8D9E6, 0xFF5C0F31, 0xFFB03A6B, 0xFF4A1C30, 0xFFF8D9E6, 0xFFF29BBE),
        swatch("Jingga", 0xFFFCE0C8, 0xFF5A2A05, 0xFFB35C14, 0xFF4A2C14, 0xFFFCE0C8, 0xFFF5AC6E),
        swatch("Kuning", 0xFFF5EBB8, 0xFF493D00, 0xFF8A7300, 0xFF3D3610, 0xFFF5EBB8, 0xFFE3CC5A),
        swatch("Toska", 0xFFC9ECEC, 0xFF003B3D, 0xFF137A7E, 0xFF113A3B, 0xFFC9ECEC, 0xFF6CD2D4),
        swatch("Nila", 0xFFDCDDF8, 0xFF22245C, 0xFF4E52B8, 0xFF25264D, 0xFFDCDDF8, 0xFFA5A8F5),
        swatch("Cokelat", 0xFFEDDDD0, 0xFF4A2A12, 0xFF8C5A33, 0xFF3D2A1C, 0xFFEDDDD0, 0xFFD4A98A),
        swatch("Abu", 0xFFE1E4E3, 0xFF262B29, 0xFF5E6764, 0xFF2B302E, 0xFFE1E4E3, 0xFFAFB8B4),
    )

    private fun swatch(
        name: String, lightContainer: Long, lightInk: Long, lightAccent: Long,
        darkContainer: Long, darkInk: Long, darkAccent: Long,
    ) = CourseSwatch(
        name, CourseTone(lightContainer, lightInk, lightAccent), CourseTone(darkContainer, darkInk, darkAccent),
    )

    /** Case- and spacing-insensitive key so "Basis  Data" and "basis data" share a colour. */
    fun key(course: String): String = course.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)

    /**
     * Stable palette slot from the course name: FNV-1a over UTF-8, then the MurmurHash3 finaliser so similar names
     * still spread across slots. Pure arithmetic, so it never depends on JVM hashing or the device.
     */
    fun automaticIndex(course: String): Int {
        var hash = 0x811C9DC5.toInt()
        for (byte in key(course).toByteArray(Charsets.UTF_8)) {
            hash = (hash xor (byte.toInt() and 0xFF)) * 0x01000193
        }
        hash = hash xor (hash ushr 16)
        hash *= 0x85EBCA6B.toInt()
        hash = hash xor (hash ushr 13)
        hash *= 0xC2B2AE35.toInt()
        hash = hash xor (hash ushr 16)
        return Integer.remainderUnsigned(hash, palette.size)
    }

    fun index(course: String, overrides: Map<String, Int>): Int =
        overrides[key(course)]?.takeIf { it in palette.indices } ?: automaticIndex(course)

    fun swatch(course: String, overrides: Map<String, Int> = emptyMap()): CourseSwatch = palette[index(course, overrides)]

    /** WCAG 2.x contrast ratio between two opaque colours. */
    fun contrast(first: Long, second: Long): Double {
        val a = luminance(first)
        val b = luminance(second)
        return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
    }

    private fun luminance(color: Long): Double {
        fun channel(shift: Int): Double {
            val value = ((color shr shift) and 0xFF) / 255.0
            return if (value <= 0.03928) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }
}
