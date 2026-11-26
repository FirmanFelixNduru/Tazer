package com.tazer.taskorganizer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface CalendarDao {
    @Query("""
        SELECT * FROM CalendarEvent WHERE ownerUid = :uid
        ORDER BY startEpochDay, startMinute, id
    """)
    fun observeEvents(uid: String): Flow<List<CalendarEvent>>

    @Query("SELECT id FROM CalendarEvent WHERE ownerUid = :uid")
    suspend fun findEventIds(uid: String): List<String>

    @Insert
    suspend fun insertEvent(event: CalendarEvent)

    @Query("""
        UPDATE CalendarEvent
        SET title = :title, startEpochDay = :startEpochDay, endEpochDay = :endEpochDay,
            startMinute = :startMinute, description = :description, endMinute = :endMinute, colorIndex = :colorIndex
        WHERE ownerUid = :uid AND id = :id
    """)
    suspend fun updateEvent(
        uid: String,
        id: String,
        title: String,
        startEpochDay: Long,
        endEpochDay: Long,
        startMinute: Int?,
        description: String,
        endMinute: Int?,
        colorIndex: Int?,
    ): Int

    @Query("DELETE FROM CalendarEvent WHERE ownerUid = :uid AND id = :id")
    suspend fun deleteEvent(uid: String, id: String): Int
}
