package com.coderax.carStatsViewer.database.log

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "LogEntries")
data class LogEntry (
    @PrimaryKey(autoGenerate = true) var id: Int? = null,
    val epochTime: Long,
    val type: Int,
    val message: String
)
