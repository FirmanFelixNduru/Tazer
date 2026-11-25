package com.tazer.taskorganizer.data

import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class CalendarRepository(database: TazerDatabase) {
    private val dao = database.calendarDao()

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
    ): Boolean {
        val cleanTitle = title.trim()
        val cleanDescription = description.trim()
        require(uid.isNotBlank())
        require(cleanTitle.isNotEmpty())
        require(startEpochDay <= endEpochDay)
        require(startEpochDay >= LocalDate.MIN.toEpochDay() && endEpochDay <= LocalDate.MAX.toEpochDay())
        require(startMinute == null || startMinute in 0..1439)

        if (id != null) {
            return dao.updateEvent(uid, id, cleanTitle, startEpochDay, endEpochDay, startMinute, cleanDescription) == 1
        }

        dao.insertEvent(CalendarEvent(
            id = UUID.randomUUID().toString(),
            ownerUid = uid,
            title = cleanTitle,
            startEpochDay = startEpochDay,
            endEpochDay = endEpochDay,
            startMinute = startMinute,
            description = cleanDescription,
        ))
        return true
    }

    suspend fun deleteEvent(uid: String, id: String): Boolean {
        require(uid.isNotBlank())
        return dao.deleteEvent(uid, id) == 1
    }
}
