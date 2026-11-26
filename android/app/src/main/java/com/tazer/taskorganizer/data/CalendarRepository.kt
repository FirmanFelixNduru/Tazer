package com.tazer.taskorganizer.data

import androidx.room.withTransaction
import com.tazer.taskorganizer.domain.IcsItem
import com.tazer.taskorganizer.domain.IcsParser
import java.io.InputStream
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

internal data class IcsImportCounts(val events: Int, val weeklyBlocks: Int, val duplicates: Int) {
    val totalNew get() = events + weeklyBlocks
}

internal data class IcsImportPreview(
    val ownerUid: String,
    val zoneId: String,
    val events: List<CalendarEvent>,
    val weeklyBlocks: List<FixedSchedule>,
    val counts: IcsImportCounts,
    val invalid: Int,
    val unsupported: Int,
    val cancelled: Int,
)

class CalendarRepository(private val database: TazerDatabase) {
    private val dao = database.calendarDao()
    private val schedules = database.tazerDao()

    fun observeEvents(uid: String): Flow<List<CalendarEvent>> {
        require(uid.isNotBlank())
        return dao.observeEvents(uid)
    }

    suspend fun saveEvent(
        uid: String,
        id: String?,
        title: String,
        startEpochDay: Long,
        endEpochDay: Long,
        startMinute: Int?,
        description: String,
        endMinute: Int? = null,
        colorIndex: Int? = null,
    ): Boolean {
        val cleanTitle = title.trim()
        val cleanDescription = description.trim()
        require(uid.isNotBlank())
        require(cleanTitle.isNotEmpty())
        require(startEpochDay <= endEpochDay)
        require(startEpochDay >= LocalDate.MIN.toEpochDay() && endEpochDay <= LocalDate.MAX.toEpochDay())
        require(startMinute == null || startMinute in 0..1439)
        // An end time needs a start time, and a same-day event must end after it starts.
        require(endMinute == null || (startMinute != null && endMinute in 1..1440 &&
            (endEpochDay > startEpochDay || endMinute > startMinute)))
        require(colorIndex == null || colorIndex in 0..9)

        if (id != null) {
            return dao.updateEvent(uid, id, cleanTitle, startEpochDay, endEpochDay, startMinute, cleanDescription,
                endMinute, colorIndex) == 1
        }

        dao.insertEvent(CalendarEvent(
            id = UUID.randomUUID().toString(),
            ownerUid = uid,
            title = cleanTitle,
            startEpochDay = startEpochDay,
            endEpochDay = endEpochDay,
            startMinute = startMinute,
            description = cleanDescription,
            endMinute = endMinute,
            colorIndex = colorIndex,
        ))
        return true
    }

    suspend fun deleteEvent(uid: String, id: String): Boolean {
        require(uid.isNotBlank())
        return dao.deleteEvent(uid, id) == 1
    }

    internal suspend fun previewIcs(uid: String, open: () -> InputStream): IcsImportPreview {
        val profile = requireNotNull(schedules.findProfile(uid))
        val parsed = withContext(Dispatchers.IO) { open().use { IcsParser.parse(it, ZoneId.of(profile.zoneId)) } }
        val events = mutableListOf<CalendarEvent>()
        val blocks = mutableListOf<FixedSchedule>()
        for (item in parsed.items) {
            when (item) {
                is IcsItem.Event -> events += CalendarEvent(
                    importId(uid, "event", item), uid, item.title, item.startDate.toEpochDay(),
                    item.endDate.toEpochDay(), item.startMinute, item.description, item.endMinute,
                )
                is IcsItem.WeeklyBlock -> blocks += FixedSchedule(
                    importId(uid, "weekly", item), uid, item.title, FixedScheduleKind.ROUTINE,
                    item.day.value, item.startMinute, item.endMinute,
                )
            }
        }
        return database.withTransaction {
            val (newEvents, newBlocks) = newImportRows(uid, events, blocks)
            IcsImportPreview(
                uid, profile.zoneId, events, blocks,
                IcsImportCounts(newEvents.size, newBlocks.size, events.size + blocks.size - newEvents.size - newBlocks.size),
                parsed.invalid, parsed.unsupported, parsed.cancelled,
            )
        }
    }

    internal suspend fun importIcs(uid: String, preview: IcsImportPreview): IcsImportCounts =
        database.withTransaction {
            require(uid == preview.ownerUid && schedules.findProfile(uid)?.zoneId == preview.zoneId)
            val (events, blocks) = newImportRows(uid, preview.events, preview.weeklyBlocks)
            // Ordinary Room inserts also populate the existing local sync journal.
            for (event in events) dao.insertEvent(event)
            for (block in blocks) schedules.insertFixedSchedule(block)
            IcsImportCounts(events.size, blocks.size, preview.events.size + preview.weeklyBlocks.size - events.size - blocks.size)
        }

    private suspend fun newImportRows(
        uid: String, events: List<CalendarEvent>, blocks: List<FixedSchedule>,
    ): Pair<List<CalendarEvent>, List<FixedSchedule>> {
        val eventIds = if (events.isEmpty()) emptySet() else dao.findEventIds(uid).toMutableSet()
        val blockIds = if (blocks.isEmpty()) emptySet() else schedules.findFixedSchedules(uid).mapTo(mutableSetOf()) { it.id }
        return events.distinctBy { it.id }.filterNot { it.id in eventIds } to
            blocks.distinctBy { it.id }.filterNot { it.id in blockIds }
    }

    private fun importId(owner: String, kind: String, item: IcsItem): String {
        val key = "tazer-ics:$kind:${owner.length}:$owner:${item.uid.length}:${item.uid}:${item.occurrence}"
        return UUID.nameUUIDFromBytes(key.toByteArray(Charsets.UTF_8)).toString()
    }
}
