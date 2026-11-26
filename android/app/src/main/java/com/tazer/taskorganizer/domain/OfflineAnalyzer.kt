package com.tazer.taskorganizer.domain

import java.util.Locale

data class OfflineAnalysis(val difficultyScore: Int, val estimatedMinutes: Int, val subtasks: List<String> = emptyList())

object OfflineAnalyzer {
    private const val DEADLINE_WINDOW_MS = 48 * 60 * 60 * 1000L
    private val keyword = Regex(
        "(?<![\\p{L}\\p{M}\\p{N}_])(coding|makalah|presentasi|kelompok)(?![\\p{L}\\p{M}\\p{N}_])",
    )
    private val minutesByScore = listOf(25, 50, 90, 150, 240)

    fun analyze(title: String, description: String, deadlineAt: Long, now: Long): OfflineAnalysis {
        val text = "$title\n$description".lowercase(Locale.ROOT)
        val topics = keyword.findAll(text).map { it.value }.toSet()
        val lengthBonus = if (description.trim().length >= 240) 1 else 0
        val deadlineBonus = if (now >= Long.MAX_VALUE - DEADLINE_WINDOW_MS ||
            deadlineAt <= now + DEADLINE_WINDOW_MS
        ) 1 else 0
        val difficulty = (1 + topics.size + lengthBonus + deadlineBonus).coerceAtMost(5)
        val steps = when {
            "coding" in topics -> listOf("Catat input, output, dan aturan program", "Buat contoh uji untuk tiap aturan",
                "Implementasikan fungsi inti", "Jalankan pengujian dan perbaiki kasus yang gagal")
            "makalah" in topics -> listOf("Tentukan pertanyaan dan ruang lingkup makalah", "Kumpulkan referensi dan catat sumbernya",
                "Susun kerangka lalu tulis draf", "Periksa argumen, kutipan, dan format akhir")
            "presentasi" in topics -> listOf("Tentukan pesan utama dan urutan penjelasan", "Buat slide untuk tiap poin utama",
                "Latihan presentasi sambil mengukur durasi", "Periksa keterbacaan slide dan format berkas")
            "kelompok" in topics -> listOf("Sepakati hasil akhir dan tenggat internal", "Bagi bagian kerja dan tetapkan penanggung jawab",
                "Gabungkan bagian tiap anggota", "Periksa hasil bersama sebelum dikumpulkan")
            else -> listOf("Ringkas instruksi dan kriteria tugas", "Siapkan bahan dan kerjakan bagian utama",
                "Periksa hasil terhadap kriteria pengumpulan")
        }
        return OfflineAnalysis(difficulty, minutesByScore[difficulty - 1], steps)
    }
}
