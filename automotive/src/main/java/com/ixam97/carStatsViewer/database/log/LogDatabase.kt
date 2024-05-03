package com.ixam97.carStatsViewer.database.log

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [LogEntry::class], version = 1)
abstract class LogDatabase: RoomDatabase() {
    abstract fun logDao(): LogDao
}
