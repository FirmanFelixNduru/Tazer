package com.tazer.taskorganizer.domain

/** A run of release-note text; [bold] marks a `**...**` span. */
data class NoteSpan(val text: String, val bold: Boolean = false)

sealed interface NoteBlock {
    data class Heading(val text: String) : NoteBlock
    data class Bullet(val spans: List<NoteSpan>) : NoteBlock
    data class Paragraph(val spans: List<NoteSpan>) : NoteBlock
}

/** The small Markdown subset GitHub release bodies use: headings, bullets, bold, and paragraphs. */
object ReleaseNotes {
    private val heading = Regex("^#{1,6}\\s+(.*)$")
    private val bullet = Regex("^[-*+]\\s+(.*)$")

    fun parse(markdown: String): List<NoteBlock> {
        val blocks = mutableListOf<NoteBlock>()
        val paragraph = StringBuilder()
        fun flush() {
            if (paragraph.isNotBlank()) blocks += NoteBlock.Paragraph(spans(paragraph.toString()))
            paragraph.clear()
        }
        for (raw in markdown.replace("\r\n", "\n").split('\n')) {
            val line = raw.trim()
            val headingMatch = heading.find(line)
            val bulletMatch = bullet.find(line)
            when {
                line.isEmpty() -> flush()
                headingMatch != null -> {
                    flush()
                    blocks += NoteBlock.Heading(spans(headingMatch.groupValues[1].trimEnd('#', ' ')).joinToString("") { it.text })
                }
                bulletMatch != null -> {
                    flush()
                    blocks += NoteBlock.Bullet(spans(bulletMatch.groupValues[1]))
                }
                else -> paragraph.append(if (paragraph.isEmpty()) "" else " ").append(line)
            }
        }
        flush()
        return blocks
    }

    /** Splits on `**` pairs; an unmatched final marker stays as literal text. */
    fun spans(text: String): List<NoteSpan> {
        var parts = text.split("**")
        if (parts.size % 2 == 0) parts = parts.dropLast(2) + (parts[parts.size - 2] + "**" + parts.last())
        return parts.mapIndexedNotNull { index, part ->
            plain(part).takeIf(String::isNotEmpty)?.let { NoteSpan(it, bold = index % 2 == 1) }
        }
    }

    private fun plain(text: String) = text.replace("`", "")
}
