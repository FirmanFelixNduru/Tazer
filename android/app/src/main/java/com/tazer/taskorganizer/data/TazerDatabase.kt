package com.tazer.taskorganizer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [UserProfile::class, Task::class, FixedSchedule::class, WorkSession::class, CalendarEvent::class],
    version = 3,
    exportSchema = true,
)
abstract class TazerDatabase : RoomDatabase() {
    internal abstract fun tazerDao(): TazerDao
    internal abstract fun calendarDao(): CalendarDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `CalendarEvent` (
                        `id` TEXT NOT NULL,
                        `ownerUid` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `startEpochDay` INTEGER NOT NULL,
                        `endEpochDay` INTEGER NOT NULL,
                        `startMinute` INTEGER,
                        `description` TEXT NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`ownerUid`) REFERENCES `UserProfile`(`ownerUid`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_CalendarEvent_ownerUid_startEpochDay`
                    ON `CalendarEvent` (`ownerUid`, `startEpochDay`)
                """.trimIndent())
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE WorkSession ADD COLUMN pausedRemainingMs INTEGER")
                db.execSQL("ALTER TABLE WorkSession ADD COLUMN pausedFromState TEXT")
                db.execSQL("ALTER TABLE WorkSession ADD COLUMN workBeforeSegmentMs INTEGER")
                db.execSQL("ALTER TABLE WorkSession ADD COLUMN continuousWorkBeforeSegmentMs INTEGER")
                db.execSQL("ALTER TABLE WorkSession ADD COLUMN lastObservedAt INTEGER")
            }
        }

        @Volatile
        private var instance: TazerDatabase? = null

        fun getInstance(context: Context): TazerDatabase =
            instance ?: synchronized(this) {
                instance ?: open(context, "tazer.db").also { instance = it }
            }

        internal fun open(context: Context, name: String): TazerDatabase =
            Room.databaseBuilder(context.applicationContext, TazerDatabase::class.java, name)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
    }
}
