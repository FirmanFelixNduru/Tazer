package com.tazer.taskorganizer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [UserProfile::class, Task::class, FixedSchedule::class, WorkSession::class],
    version = 1,
    exportSchema = true,
)
abstract class TazerDatabase : RoomDatabase() {
    internal abstract fun tazerDao(): TazerDao

    companion object {
        @Volatile
        private var instance: TazerDatabase? = null

        fun getInstance(context: Context): TazerDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TazerDatabase::class.java,
                    "tazer.db",
                ).build().also { instance = it }
            }
    }
}
