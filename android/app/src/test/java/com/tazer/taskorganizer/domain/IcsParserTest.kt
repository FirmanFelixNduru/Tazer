package com.tazer.taskorganizer.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class IcsParserTest {
    private val jakarta = ZoneId.of("Asia/Jakarta")

    @Test
    fun readsInclusiveAllDayDatesAndFoldedEscapes() {
        val result = parse(event("""
            UID:holiday
            DTSTART;VALUE=DATE:20261126
            DTEND;VALUE=DATE:20261129
            SUMMARY:Libur\, hari
              raya
            DESCRIPTION:Baris satu\nBaris dua\; ini\\itu
            LOCATION;ALTREP="https://example.org/a;b":Jakarta\, Indonesia
            BEGIN:VALARM
            DESCRIPTION:Ignore nested description
            END:VALARM
        """))
        val holiday = result.items.single() as IcsItem.Event
        assertEquals("Libur, hari raya", holiday.title)
        assertEquals("Lokasi: Jakarta, Indonesia\nBaris satu\nBaris dua; ini\\itu", holiday.description)
        assertEquals(LocalDate.parse("2026-11-26"), holiday.startDate)
        assertEquals(LocalDate.parse("2026-11-28"), holiday.endDate)
        assertNull(holiday.startMinute)
    }

    @Test
    fun unfoldsInsideUtf8CharacterBeforeDecoding() {
        val bytes = calendar(event("UID:fold\nDTSTART;VALUE=DATE:20261126\nSUMMARY:Café"))
            .replace("\n", "\r\n").toByteArray()
        val cut = bytes.indexOf(0xA9.toByte())
        val folded = bytes.copyOfRange(0, cut) + "\r\n\t".toByteArray() + bytes.copyOfRange(cut, bytes.size)
        val result = IcsParser.parse(folded.inputStream(), jakarta)
        assertEquals("Café", (result.items.single() as IcsItem.Event).title)
    }

    @Test
    fun resolvesUtcTzidAndFloatingTimesInProfileZoneWithoutAddingAnExclusiveMidnight() {
        val result = parse(
            event("UID:utc\nDTSTART:20261126T163000Z\nDTEND:20261126T170000Z"),
            event("UID:ny\nDTSTART;TZID=\"America/New_York\":20261126T160000"),
            event("UID:float\nDTSTART:20261126T090000\nDURATION:P1DT1H"),
            event("UID:single\nDTSTART;VALUE=DATE:20261126"),
        )
        val events = result.items.filterIsInstance<IcsItem.Event>()
        assertEquals(4, events.size)
        assertEquals(23 * 60 + 30, events[0].startMinute)
        assertEquals(LocalDate.parse("2026-11-26"), events[0].endDate)
        assertEquals(LocalDate.parse("2026-11-27"), events[1].startDate)
        assertEquals(4 * 60, events[1].startMinute)
        assertEquals(9 * 60, events[2].startMinute)
        assertEquals(LocalDate.parse("2026-11-27"), events[2].endDate)
        assertEquals(events[3].startDate, events[3].endDate)
    }

    @Test
    fun expandsCountAndWeekStartWithoutCreatingPermanentBlocks() {
        val result = parse(event("""
            UID:labs
            DTSTART;VALUE=DATE:20261004
            DURATION:P2D
            RRULE:FREQ=WEEKLY;INTERVAL=2;BYDAY=SU,MO;WKST=SU;COUNT=4
        """))
        val events = result.items.filterIsInstance<IcsItem.Event>()
        assertEquals(listOf("2026-10-04", "2026-10-05", "2026-10-18", "2026-10-19"),
            events.map { it.startDate.toString() })
        assertEquals(4, events.map { it.occurrence }.toSet().size)
        assertTrue(events.all { it.startDate.plusDays(1) == it.endDate })
    }

    @Test
    fun boundedWeeklyUntilIsInclusiveAndPreservesSourceWallTimeAcrossDst() {
        val result = parse(event("""
            UID:dst
            DTSTART;TZID=America/New_York:20261026T090000
            DTEND;TZID=America/New_York:20261026T100000
            RRULE:FREQ=WEEKLY;UNTIL=20261102T140000Z
        """))
        assertEquals(listOf(20 * 60, 21 * 60), result.items.filterIsInstance<IcsItem.Event>().map { it.startMinute })
    }

    @Test
    fun weeklyBlocksConvertToProfileDayAndSplitAtMidnight() {
        val result = parse(event("""
            UID:lesson
            DTSTART:20261126T160000Z
            DTEND:20261126T180000Z
            RRULE:FREQ=WEEKLY;BYDAY=TH,SA
        """))
        val blocks = result.items.filterIsInstance<IcsItem.WeeklyBlock>()
        assertEquals(listOf(DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
            blocks.map { it.day })
        assertEquals(listOf(1380 to 1440, 0 to 60, 1380 to 1440, 0 to 60),
            blocks.map { it.startMinute to it.endMinute })
        assertEquals(4, blocks.map { it.occurrence }.toSet().size)
    }

    @Test
    fun skipsWholeUnrepresentableSeriesRatherThanImportingFirstOccurrence() {
        val start = "DTSTART:20261126T160000Z\nDTEND:20261126T180000Z"
        val result = parse(
            event("UID:every-other-week\n$start\nRRULE:FREQ=WEEKLY;INTERVAL=2"),
            event("UID:daily\n$start\nRRULE:FREQ=DAILY;COUNT=4"),
            event("UID:bysetpos\n$start\nRRULE:FREQ=WEEKLY;BYSETPOS=1"),
            event("UID:except\n$start\nRRULE:FREQ=WEEKLY\nEXDATE:20261203T160000Z"),
            event("UID:edited\n$start\nRRULE:FREQ=WEEKLY;COUNT=3"),
            event("UID:edited\nRECURRENCE-ID:20261203T160000Z\n$start"),
            event("UID:oversize\n$start\nRRULE:FREQ=WEEKLY;COUNT=513"),
            event("UID:oversize-until\n$start\nRRULE:FREQ=WEEKLY;UNTIL=20991126T160000Z"),
            event("UID:unknown-zone\nDTSTART;TZID=Custom/School:20261126T160000"),
            event("UID:shifting-zone\nDTSTART;TZID=America/New_York:20261126T160000\nDURATION:PT2H\nRRULE:FREQ=WEEKLY"),
            event("UID:cancelled\nSTATUS:CANCELLED"),
            event("UID:bad-date\nDTSTART;VALUE=DATE:20260230"),
            event("UID:ok\n$start"),
        )
        assertEquals(10, result.unsupported)
        assertEquals(1, result.invalid)
        assertEquals(1, result.cancelled)
        assertEquals("ok", result.items.single().uid)
    }

    @Test
    fun rejectsBrokenEnvelopesAndOversizeDocuments() {
        assertThrows(IllegalArgumentException::class.java) {
            IcsParser.parse("BEGIN:VCALENDAR\nBEGIN:VEVENT\nEND:VCALENDAR".byteInputStream(), jakarta)
        }
        assertThrows(IllegalArgumentException::class.java) {
            IcsParser.parse(ByteArray(2 * 1024 * 1024 + 1).inputStream(), jakarta)
        }
    }

    private fun parse(vararg events: String) = IcsParser.parse(calendar(*events).byteInputStream(), jakarta)
    private fun event(body: String) = "BEGIN:VEVENT\n" + body.trimIndent() + "\nEND:VEVENT"
    private fun calendar(vararg events: String) =
        "BEGIN:VCALENDAR\nVERSION:2.0\n" + events.joinToString("\n") + "\nEND:VCALENDAR"
}
