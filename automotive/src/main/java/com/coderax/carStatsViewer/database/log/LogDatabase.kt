package com.coderax.carStatsViewer.database.log

import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase

@Database(entities = [LogEntry::class], version = 1)
abstract class LogDatabase: RoomDatabase() {
    abstract fun logDao(): LogDao
}
