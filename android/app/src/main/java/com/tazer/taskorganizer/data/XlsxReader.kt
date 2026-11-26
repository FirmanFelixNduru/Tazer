package com.tazer.taskorganizer.data

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/**
 * Reads the first worksheet of an .xlsx file as text cells, straight from its OpenXML parts with java.util.zip and
 * XmlPullParser, so no spreadsheet library is bundled. Formulas use their cached values.
 */
object XlsxReader {
    private const val MAX_PART_BYTES = 8 * 1024 * 1024
    private const val MAX_TOTAL_BYTES = 24 * 1024 * 1024
    private const val MAX_ROWS = 1000
    private const val MAX_COLUMNS = 40

    fun read(input: InputStream): List<List<String>> {
        val parts = mutableMapOf<String, ByteArray>()
        var total = 0L
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name
                if (name == "xl/sharedStrings.xml" || name.matches(Regex("xl/worksheets/sheet\\d+\\.xml"))) {
                    val bytes = ByteArrayOutputStream()
                    val buffer = ByteArray(32 * 1024)
                    while (true) {
                        val count = zip.read(buffer)
                        if (count < 0) break
                        bytes.write(buffer, 0, count)
                        total += count
                        // Guards against zip bombs: limits apply to inflated bytes, not entry headers.
                        require(bytes.size() <= MAX_PART_BYTES && total <= MAX_TOTAL_BYTES) { "File .xlsx terlalu besar." }
                    }
                    parts[name] = bytes.toByteArray()
                }
            }
        }
        val sheet = parts["xl/worksheets/sheet1.xml"]
            ?: parts.keys.filter { it.startsWith("xl/worksheets/") }.minByOrNull { it.filter(Char::isDigit).toInt() }?.let(parts::get)
        requireNotNull(sheet) { "Lembar kerja pertama tidak ditemukan. Pastikan file berformat .xlsx." }
        val shared = parts["xl/sharedStrings.xml"]?.let(::sharedStrings).orEmpty()
        return cells(sheet, shared)
    }

    private fun parser(bytes: ByteArray): XmlPullParser = XmlPullParserFactory.newInstance().apply {
        isNamespaceAware = false
    }.newPullParser().apply { setInput(ByteArrayInputStream(bytes), "UTF-8") }

    /** Each <si> may hold one <t> or several rich-text runs; phonetic hints (<rPh>) are skipped. */
    internal fun sharedStrings(bytes: ByteArray): List<String> {
        val parser = parser(bytes)
        val strings = mutableListOf<String>()
        var current: StringBuilder? = null
        var phonetic = 0
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "si" -> current = StringBuilder()
                    "rPh" -> phonetic++
                    "t" -> if (phonetic == 0) current?.append(parser.nextText())
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "si" -> { strings += current.toString(); current = null }
                    "rPh" -> phonetic--
                }
            }
        }
        return strings
    }

    internal fun cells(bytes: ByteArray, shared: List<String>): List<List<String>> {
        val parser = parser(bytes)
        val rows = sortedMapOf<Int, MutableMap<Int, String>>()
        var rowIndex = -1
        var column = -1
        var type: String? = null
        var value: String? = null
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "row" -> rowIndex = parser.getAttributeValue(null, "r")?.toIntOrNull()?.minus(1) ?: (rowIndex + 1)
                    "c" -> {
                        column = parser.getAttributeValue(null, "r")?.let(::columnIndex) ?: (column + 1)
                        type = parser.getAttributeValue(null, "t")
                        value = null
                    }
                    "v" -> value = parser.nextText()
                    "t" -> if (type == "inlineStr") value = (value ?: "") + parser.nextText()
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "c" -> {
                        val text = when (type) {
                            "s" -> value?.toIntOrNull()?.let(shared::getOrNull)
                            "b" -> if (value == "1") "TRUE" else "FALSE"
                            else -> value
                        }
                        if (!text.isNullOrBlank() && rowIndex in 0 until MAX_ROWS && column in 0 until MAX_COLUMNS) {
                            rows.getOrPut(rowIndex) { mutableMapOf() }[column] = text
                        }
                    }
                    "row" -> column = -1
                }
            }
        }
        return rows.values.map { row -> List((row.keys.maxOrNull() ?: -1) + 1) { row[it].orEmpty() } }
    }

    /** "B12" -> 1, "AA3" -> 26. */
    internal fun columnIndex(reference: String): Int? {
        val letters = reference.takeWhile(Char::isLetter).uppercase()
        if (letters.isEmpty() || letters.length > 3) return null
        return letters.fold(0) { total, char -> total * 26 + (char - 'A' + 1) } - 1
    }
}
