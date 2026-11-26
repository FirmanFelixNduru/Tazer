package com.tazer.taskorganizer.data

import com.tazer.taskorganizer.domain.ScheduleDraft
import com.tazer.taskorganizer.domain.ScheduleTable
import com.tazer.taskorganizer.integration.IntegrationError
import com.tazer.taskorganizer.integration.IntegrationException
import com.tazer.taskorganizer.integration.parseJson
import com.tazer.taskorganizer.integration.scheduleScan
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.*
import org.junit.Test

class XlsxAndScheduleScanTest {
    private fun xlsx(vararg parts: Pair<String, String>): ByteArray = ByteArrayOutputStream().also { bytes ->
        ZipOutputStream(bytes).use { zip ->
            zip.putNextEntry(ZipEntry("[Content_Types].xml")); zip.write("<Types/>".toByteArray()); zip.closeEntry()
            parts.forEach { (name, xml) -> zip.putNextEntry(ZipEntry(name)); zip.write(xml.toByteArray()); zip.closeEntry() }
        }
    }.toByteArray()

    private val shared = """<?xml version="1.0" encoding="UTF-8"?>
        <sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="7" uniqueCount="7">
          <si><t>Mata Kuliah</t></si><si><t>Hari</t></si><si><t>Jam Mulai</t></si><si><t>Jam Selesai</t></si>
          <si><r><t>Basis </t></r><r><rPr><b/></rPr><t>Data</t></r><rPh><t>ignored</t></rPh></si><si><t>Senin</t></si><si><t>Rabu</t></si>
        </sst>"""
    private val sheet = """<?xml version="1.0" encoding="UTF-8"?>
        <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>
          <row r="1"><c r="A1" t="s"><v>0</v></c><c r="B1" t="s"><v>1</v></c><c r="C1" t="s"><v>2</v></c><c r="D1" t="s"><v>3</v></c><c r="E1" t="inlineStr"><is><t>Ruang</t></is></c></row>
          <row r="2"><c r="A2" t="s"><v>4</v></c><c r="B2" t="s"><v>5</v></c><c r="C2"><v>0.33333333333333331</v></c><c r="D2"><v>0.40277777777777779</v></c><c r="E2" t="inlineStr"><is><t>B-201</t></is></c></row>
          <row r="4"><c r="A4" t="inlineStr"><is><t>Algoritma</t></is></c><c r="B4" t="s"><v>6</v></c><c r="C4" t="str"><f>TEXT(1,"hh:mm")</f><v>10:00</v></c><c r="D4" t="inlineStr"><is><t>11:40</t></is></c></row>
        </sheetData></worksheet>"""

    @Test
    fun readsSharedInlineNumericAndFormulaCellsFromTheFirstSheet() {
        val rows = XlsxReader.read(xlsx("xl/sharedStrings.xml" to shared, "xl/worksheets/sheet1.xml" to sheet).inputStream())
        assertEquals(listOf("Mata Kuliah", "Hari", "Jam Mulai", "Jam Selesai", "Ruang"), rows[0])
        assertEquals("Basis Data", rows[1][0])
        assertEquals("0.33333333333333331", rows[1][2])
        assertEquals(listOf("Algoritma", "Rabu", "10:00", "11:40"), rows[2])
        assertEquals(listOf(ScheduleDraft("Basis Data", 1, 480, 580), ScheduleDraft("Algoritma", 3, 600, 700)),
            ScheduleTable.rows(rows).drafts)
        assertEquals(27, XlsxReader.columnIndex("AB12"))
        assertEquals(0, XlsxReader.columnIndex("A1"))
    }

    @Test
    fun picksTheLowestSheetWhenSheet1IsMissingAndRejectsNonWorkbooks() {
        fun only(text: String) = """<worksheet><sheetData><row r="1"><c r="A1" t="inlineStr"><is><t>$text</t></is></c></row></sheetData></worksheet>"""
        val rows = XlsxReader.read(xlsx("xl/worksheets/sheet10.xml" to only("Sepuluh"), "xl/worksheets/sheet2.xml" to only("Dua")).inputStream())
        assertEquals(listOf(listOf("Dua")), rows)
        assertThrows(IllegalArgumentException::class.java) { XlsxReader.read(xlsx("word/document.xml" to "<w/>").inputStream()) }
        assertThrows(Exception::class.java) { XlsxReader.read("bukan zip".byteInputStream()) }
    }

    @Test
    fun scheduleScanKeepsReadableRowsAndExplainsDroppedOnes() {
        val scan = parseJson("""
            {"schedules":[
              {"title":"Basis Data","day":1,"startTime":"08:00","endTime":"09:40"},
              {"title":"Rusak","day":9,"startTime":"08:00","endTime":"09:00"},
              {"title":"Terbalik","day":2,"startTime":"11:00","endTime":"10:00"},
              {"title":"Malam","day":5,"startTime":"22:00","endTime":"24:00"}
            ],"warnings":["Foto agak miring"]}
        """.trimIndent()).scheduleScan()
        assertEquals(listOf(ScheduleDraft("Basis Data", 1, 480, 580), ScheduleDraft("Malam", 5, 1320, 1440)), scan.drafts)
        assertEquals(listOf("Foto agak miring", "2 baris jadwal tidak terbaca lengkap dan dilewati."), scan.warnings)
        val serverWarned = parseJson("""{"schedules":[],"warnings":["3 baris jadwal tidak terbaca lengkap dan dilewati."]}""").scheduleScan()
        assertEquals(1, serverWarned.warnings.size)
        val failure = assertThrows(IntegrationException::class.java) { parseJson("""{"schedules":[],"leak":1}""").scheduleScan() }
        assertEquals(IntegrationError.INVALID_RESPONSE, failure.code)
    }
}
