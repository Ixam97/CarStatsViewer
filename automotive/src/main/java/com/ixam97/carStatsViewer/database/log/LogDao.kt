package com.ixam97.carStatsViewer.database.log

import androidx.room.*

@Dao
interface LogDao {

    @Query("SELECT * FROM LogEntries")
    fun getAll(): List<LogEntry>

    @Query("SELECT * FROM LogEntries WHERE type >= :logLevel")
    fun getLevel(logLevel: Int): List<LogEntry>

    @Query("SELECT * FROM LogEntries WHERE type >= :logLevel ORDER BY id DESC LIMIT :length")
    // @Query("SELECT * FROM LogEntries WHERE type >= :logLevel LIMIT :length")
    fun getLevelAndLength(logLevel: Int, length: Int): List<LogEntry>

    @Query("SELECT * FROM LogEntries WHERE epochTime BETWEEN :firstTime AND :lastTime")
    fun getTimeSpan(firstTime: Long, lastTime: Long): List<LogEntry>

    fun getTimeSpan(timeSpan: Long): List<LogEntry> {
        val lastTime = System.currentTimeMillis()
        val firstTime = lastTime - timeSpan
        return getTimeSpan(firstTime, lastTime)
    }

    @Insert
    fun insert(logEntry: LogEntry)

    @Delete
    fun delete(logEntry: LogEntry)

    @Query("DELETE FROM LogEntries")
    fun clear()
}