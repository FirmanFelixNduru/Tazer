package com.tazer.taskorganizer.domain

import java.io.InputStream
import java.nio.ByteBuffer
import java.time.DateTimeException
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

internal sealed interface IcsItem {
    val uid: String
    val occurrence: String

    data class Event(
        override val uid: String,
        override val occurrence: String,
        val title: String,
        val startDate: LocalDate,
        val endDate: LocalDate,
        val startMinute: Int?,
        val description: String,
        /** Local end time on [endDate] for timed events with a length; 1440 means midnight. */
        val endMinute: Int? = null,
    ) : IcsItem

    data class WeeklyBlock(
        override val uid: String,
        override val occurrence: String,
        val title: String,
        val day: DayOfWeek,
        val startMinute: Int,
        val endMinute: Int,
    ) : IcsItem
}

internal data class IcsCalendar(
    val items: List<IcsItem>,
    val invalid: Int,
    val unsupported: Int,
    val cancelled: Int,
)

internal object IcsParser {
    private const val MAX_BYTES = 2 * 1024 * 1024
    private const val MAX_ITEMS = 2500
    private const val MAX_OCCURRENCES = 512
    private val fold = Regex("\\r?\\n[ \\t]")
    private val dateFormat = DateTimeFormatter.ofPattern("uuuuMMdd", Locale.ROOT).withResolverStyle(ResolverStyle.STRICT)
    private val clockFormat = DateTimeFormatter.ofPattern("HHmmss", Locale.ROOT).withResolverStyle(ResolverStyle.STRICT)
    private val weekdays = DayOfWeek.entries.associateBy { it.name.take(2) }
    private val weeklyKeys = setOf("FREQ", "INTERVAL", "BYDAY", "WKST", "COUNT", "UNTIL")
    private val recurrenceExceptions = setOf("EXDATE", "RDATE", "EXRULE", "RECURRENCE-ID")
    private val durationFormat = Regex("\\+?P(?:(\\d+)W|(?:(\\d+)D)?(?:T(?=\\d)(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?)?)")
    private class UnsupportedEvent : RuntimeException()
    private data class Property(val name: String, val params: Map<String, String>, val value: String)

    // A null clock marks a DATE. A null zone on a DATE-TIME marks floating civil time.
    private data class EventTime(val date: LocalDate, val clock: LocalTime?, val zone: ZoneId?) {
        fun local() = date.atTime(requireNotNull(clock))
        fun at(profileZone: ZoneId): ZonedDateTime = local().atZone(zone ?: profileZone)
        fun key(): String = when {
            clock == null -> date.toString()
            zone == null -> local().toString()
            else -> at(zone).toInstant().toString()
        }
    }

    private data class EventLength(val days: Long, val seconds: Long) {
        fun end(start: ZonedDateTime): ZonedDateTime = start.plusDays(days).plusSeconds(seconds)
    }

    fun parse(input: InputStream, profileZone: ZoneId): IcsCalendar {
        // Unfold bytes before UTF-8 decoding, including folds that split a multibyte character.
        val buffer = ByteArray(MAX_BYTES + 1)
        var size = 0
        while (size < buffer.size) {
            val count = input.read(buffer, size, buffer.size - size)
            if (count < 0) break
            size += count
        }
        require(size <= MAX_BYTES)
        val unfolded = String(buffer, 0, size, Charsets.ISO_8859_1)
            .replace(fold, "")
            .toByteArray(Charsets.ISO_8859_1)
        val text = Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(unfolded)).toString().removePrefix("\uFEFF")

        val events = mutableListOf<List<Property>>()
        val stack = mutableListOf<String>()
        var calendarCount = 0
        var cancelCalendar = false
        var current = mutableListOf<Property>()
        text.lineSequence().filter(String::isNotBlank).forEach { line ->
            val field = property(line)
            when (field?.name) {
                "BEGIN" -> {
                    val component = field.value.uppercase(Locale.ROOT)
                    require(if (stack.isEmpty()) component == "VCALENDAR" else component != "VCALENDAR")
                    if (stack.isEmpty()) {
                        calendarCount++
                        cancelCalendar = false
                    }
                    stack += component
                    if (stack.size == 2 && component == "VEVENT") current = mutableListOf()
                }
                "END" -> {
                    require(stack.lastOrNull() == field.value.uppercase(Locale.ROOT))
                    if (stack.size == 2 && stack.last() == "VEVENT") events += current
                    if (stack.size == 1 && cancelCalendar) throw IllegalArgumentException("METHOD:CANCEL")
                    stack.removeAt(stack.lastIndex)
                }
                else -> {
                    if (stack.size == 1 && field?.name == "METHOD") {
                        cancelCalendar = field.value.equals("CANCEL", ignoreCase = true)
                    }
                    if (stack.size == 2 && stack.last() == "VEVENT") {
                        current += field ?: Property("", emptyMap(), "")
                    }
                }
            }
        }
        require(stack.isEmpty() && calendarCount > 0)
        val overriddenUids = events.asSequence().filter { fields -> fields.any { it.name == "RECURRENCE-ID" } }
            .mapNotNull { fields -> fields.firstOrNull { it.name == "UID" }?.value?.let(::unescape) }.toSet()
        val items = mutableListOf<IcsItem>()
        var invalid = 0
        var unsupported = 0
        var cancelled = 0
        for (fields in events) {
            val byName = fields.groupBy(Property::name)
            fun field(name: String): Property? = byName[name]?.let { require(it.size == 1); it.single() }
            try {
                require("" !in byName)
                val uid = unescape(requireNotNull(field("UID")).value)
                require(uid.isNotBlank())
                if (uid in overriddenUids || byName.keys.any(recurrenceExceptions::contains)) {
                    throw UnsupportedEvent()
                }
                if (field("STATUS")?.value.equals("CANCELLED", ignoreCase = true)) {
                    cancelled++
                    continue
                }
                val imported = readEvent(uid, profileZone, ::field)
                items += imported
            } catch (_: UnsupportedEvent) {
                unsupported++
            } catch (_: DateTimeException) {
                invalid++
            } catch (_: ArithmeticException) {
                invalid++
            } catch (_: IllegalArgumentException) {
                invalid++
            }
            require(items.size <= MAX_ITEMS)
        }
        return IcsCalendar(items, invalid, unsupported, cancelled)
    }

    private fun readEvent(uid: String, zone: ZoneId, field: (String) -> Property?): List<IcsItem> {
        val start = time(requireNotNull(field("DTSTART")))
        val end = field("DTEND")?.let(::time)
        val duration = field("DURATION")?.value
        require(end == null || duration == null)
        val length = length(start, end, duration, zone)
        val title = field("SUMMARY")?.value?.let(::unescape)?.trim().orEmpty().ifBlank { "Acara tanpa judul" }
        val description = listOfNotNull(
            field("LOCATION")?.value?.let(::unescape)?.trim()?.takeIf(String::isNotEmpty)?.let { "Lokasi: $it" },
            field("DESCRIPTION")?.value?.let(::unescape)?.trim()?.takeIf(String::isNotEmpty),
        ).joinToString("\n")
        // Leave room for JSON escaping and the sync envelope within the existing row size limit.
        require(title.length + description.length <= 10_000 && '\u0000' !in title && '\u0000' !in description)
        val rule = field("RRULE") ?: return listOf(event(uid, "single", title, description, start, length, zone))

        val parts = mutableMapOf<String, String>()
        for (part in rule.value.uppercase(Locale.ROOT).split(';')) {
            val pair = part.split('=', limit = 2)
            require(pair.size == 2 && pair[1].isNotEmpty() && parts.put(pair[0], pair[1]) == null)
        }
        if (parts["FREQ"] != "WEEKLY" || !weeklyKeys.containsAll(parts.keys)) throw UnsupportedEvent()
        val interval = parts["INTERVAL"]?.let(::positiveInt) ?: 1
        val weekStart = parts["WKST"]?.let { weekdays[it] ?: throw UnsupportedEvent() } ?: DayOfWeek.MONDAY
        val days = parts["BYDAY"]?.split(',')?.map { weekdays[it] ?: throw UnsupportedEvent() }?.distinct()
            ?: listOf(start.date.dayOfWeek)
        if (start.date.dayOfWeek !in days) throw UnsupportedEvent()
        val count = parts["COUNT"]?.let(::positiveInt)
        val until = parts["UNTIL"]?.let {
            time(Property("UNTIL", if (start.clock == null) mapOf("VALUE" to "DATE") else emptyMap(), it))
        }
        require(count == null || until == null)
        if (count == null && until == null) {
            if (interval != 1 || field("TRANSP")?.value.equals("TRANSPARENT", ignoreCase = true)) throw UnsupportedEvent()
            return weeklyBlocks(uid, title, start, length, days, zone)
        }
        if (count != null && count > MAX_OCCURRENCES) throw UnsupportedEvent()
        if (until != null) {
            require((start.clock == null) == (until.clock == null) && (start.zone == null) == (until.zone == null))
            require(!pastUntil(start, until, zone))
        }

        val occurrences = mutableListOf<IcsItem>()
        val offsets = days.map { (it.value - weekStart.value + 7) % 7 }.sorted()
        var week = start.date.with(TemporalAdjusters.previousOrSame(weekStart))
        repeat(MAX_OCCURRENCES * 2) {
            for (offset in offsets) {
                val date = week.plusDays(offset.toLong())
                if (date < start.date) continue
                val occurrence = start.copy(date = date)
                if (until != null && pastUntil(occurrence, until, zone)) return occurrences
                if (date != start.date && occurrence.clock != null &&
                    (occurrence.zone ?: zone).rules.getValidOffsets(occurrence.local()).isEmpty()
                ) continue
                if (occurrences.size == MAX_OCCURRENCES) throw UnsupportedEvent()
                occurrences += event(uid, "at:" + occurrence.key(), title, description, occurrence, length, zone)
                if (occurrences.size == count) return occurrences
            }
            week = week.plusWeeks(interval.toLong())
        }
        throw UnsupportedEvent()
    }

    private fun event(
        uid: String,
        key: String,
        title: String,
        description: String,
        start: EventTime,
        length: EventLength,
        zone: ZoneId,
    ): IcsItem.Event {
        if (start.clock == null) {
            return IcsItem.Event(uid, key, title, start.date, start.date.plusDays(length.days - 1), null, description)
        }
        val source = start.at(zone)
        val local = source.withZoneSameInstant(zone)
        val end = length.end(source).withZoneSameInstant(zone)
        val hasLength = end.toInstant() > local.toInstant()
        val endsAtMidnight = hasLength && end.toLocalTime() == LocalTime.MIDNIGHT
        val lastDate = if (endsAtMidnight) end.toLocalDate().minusDays(1) else end.toLocalDate()
        val endMinute = when {
            !hasLength -> null
            endsAtMidnight -> 1440
            else -> minute(end.toLocalTime())
        }
        return IcsItem.Event(uid, key, title, local.toLocalDate(), maxOf(local.toLocalDate(), lastDate),
            minute(local.toLocalTime()), description, endMinute)
    }

    private fun weeklyBlocks(
        uid: String, title: String, start: EventTime, length: EventLength,
        days: List<DayOfWeek>, zone: ZoneId,
    ): List<IcsItem.WeeklyBlock> {
        if (start.clock == null || length.days != 0L || length.seconds !in 1..86_400 ||
            start.clock.second != 0 || length.seconds % 60 != 0L
        ) throw UnsupportedEvent()
        val sourceRules = (start.zone ?: zone).rules
        val instant = start.at(zone).toInstant()
        if (sourceRules != zone.rules &&
            (sourceRules.nextTransition(instant) != null || zone.rules.nextTransition(instant) != null)
        ) throw UnsupportedEvent()
        val offsetSeconds = zone.rules.getOffset(instant).totalSeconds - sourceRules.getOffset(instant).totalSeconds
        if (offsetSeconds % 60 != 0) throw UnsupportedEvent()
        val totalStart = minute(start.clock) + offsetSeconds / 60
        val dayShift = Math.floorDiv(totalStart, 1440).toLong()
        val from = Math.floorMod(totalStart, 1440)
        val to = from + (length.seconds / 60).toInt()
        return days.sorted().flatMap { sourceDay ->
            val day = sourceDay.plus(dayShift)
            val first = IcsItem.WeeklyBlock(uid, "weekly:$sourceDay", title, day, from, minOf(1440, to))
            if (to <= 1440) listOf(first) else listOf(first, IcsItem.WeeklyBlock(
                uid, "weekly:$sourceDay:next", title, day.plus(1), 0, to - 1440,
            ))
        }
    }

    private fun length(start: EventTime, end: EventTime?, duration: String?, zone: ZoneId): EventLength {
        if (end != null) {
            require((start.clock == null) == (end.clock == null))
            if (start.clock == null) {
                val days = ChronoUnit.DAYS.between(start.date, end.date)
                require(days > 0)
                return EventLength(days, 0)
            }
            val seconds = Duration.between(start.at(zone), end.at(zone)).seconds
            require(seconds > 0)
            return EventLength(0, seconds)
        }
        if (duration == null) return EventLength(if (start.clock == null) 1 else 0, 0)
        val parts = requireNotNull(durationFormat.matchEntire(duration.uppercase(Locale.ROOT))).groupValues
        fun part(index: Int) = parts[index].takeIf(String::isNotEmpty)?.toLong() ?: 0
        val days = Math.addExact(Math.multiplyExact(part(1), 7), part(2))
        val seconds = Duration.ofHours(part(3)).plusMinutes(part(4)).plusSeconds(part(5)).seconds
        require((days > 0 || seconds > 0) && (start.clock != null || parts.drop(3).all(String::isEmpty)))
        return EventLength(days, seconds)
    }

    private fun time(field: Property): EventTime {
        val zoneName = field.params["TZID"]
        val type = field.params["VALUE"]?.uppercase(Locale.ROOT)
        if (type == "DATE") {
            require(zoneName == null)
            return EventTime(LocalDate.parse(field.value, dateFormat), null, null)
        }
        require(type == null || type == "DATE-TIME")
        val utc = field.value.endsWith("Z", ignoreCase = true)
        require(!utc || zoneName == null)
        val value = field.value.dropLast(if (utc) 1 else 0)
        require(value.length == 15 && value[8].equals('T', ignoreCase = true))
        val zone = if (utc) ZoneOffset.UTC else zoneName?.let {
            try {
                ZoneId.of(it)
            } catch (_: DateTimeException) {
                throw UnsupportedEvent()
            }
        }
        return EventTime(LocalDate.parse(value.take(8), dateFormat), LocalTime.parse(value.drop(9), clockFormat), zone)
    }

    private fun pastUntil(value: EventTime, until: EventTime, zone: ZoneId): Boolean = when {
        value.clock == null -> value.date > until.date
        value.zone == null -> value.local() > until.local()
        else -> value.at(zone).toInstant() > until.at(zone).toInstant()
    }

    private fun minute(clock: LocalTime) = clock.hour * 60 + clock.minute
    private fun positiveInt(raw: String): Int = raw.toInt().also { require(it > 0) }

    private fun property(line: String): Property? {
        var quoted = false
        var start = 0
        val header = mutableListOf<String>()
        for ((index, char) in line.withIndex()) {
            if (char == '"') quoted = !quoted
            if (quoted || char !in ";:") continue
            header += line.substring(start, index)
            start = index + 1
            if (char == ':') {
                val params = mutableMapOf<String, String>()
                for (param in header.drop(1)) {
                    val pair = param.split('=', limit = 2)
                    if (pair.size != 2 || params.put(pair[0].uppercase(Locale.ROOT), pair[1].removeSurrounding("\"")) != null) return null
                }
                return Property(header.first().uppercase(Locale.ROOT), params, line.substring(start))
            }
        }
        return null
    }

    private fun unescape(value: String): String = buildString(value.length) {
        var index = 0
        while (index < value.length) {
            val char = value[index++]
            if (char == '\\' && index < value.length) {
                when (val escaped = value[index++]) {
                    'n', 'N' -> append('\n')
                    '\\', ',', ';' -> append(escaped)
                    else -> append('\\').append(escaped)
                }
            } else append(char)
        }
    }
}
