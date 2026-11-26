package com.tazer.taskorganizer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserProfile::class, Task::class, FixedSchedule::class, WorkSession::class, CalendarEvent::class,
        Subtask::class, SyncEntry::class, SyncCursor::class, SyncDevice::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class TazerDatabase : RoomDatabase() {
    internal abstract fun tazerDao(): TazerDao
    internal abstract fun calendarDao(): CalendarDao
    internal abstract fun subtaskDao(): SubtaskDao

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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Task ADD COLUMN analyzerBaselineMinutes INTEGER")
                db.execSQL("""
                    CREATE TABLE Subtask (
                        id TEXT NOT NULL PRIMARY KEY, ownerUid TEXT NOT NULL, taskId TEXT NOT NULL,
                        title TEXT NOT NULL, completed INTEGER NOT NULL, position INTEGER NOT NULL,
                        FOREIGN KEY(taskId, ownerUid) REFERENCES Task(id, ownerUid) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX index_Subtask_taskId_ownerUid ON Subtask(taskId, ownerUid)")
                db.execSQL("""
                    CREATE TABLE SyncEntry (
                        ownerUid TEXT NOT NULL, kind TEXT NOT NULL, rowId TEXT NOT NULL,
                        version INTEGER NOT NULL, writer TEXT NOT NULL, tombstone INTEGER NOT NULL,
                        dirty INTEGER NOT NULL, PRIMARY KEY(ownerUid, kind, rowId)
                    )
                """.trimIndent())
                db.execSQL("CREATE TABLE SyncCursor (ownerUid TEXT NOT NULL PRIMARY KEY, changeId INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE SyncDevice (id INTEGER NOT NULL PRIMARY KEY, writer TEXT NOT NULL, applying INTEGER NOT NULL)")
                SyncJournal.install(db, seed = true)
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) = SyncJournal.install(db, seed = false)
                })
                .build()
    }
}
