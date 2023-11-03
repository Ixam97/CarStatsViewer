package com.coderax.carStatsViewer.database.log

import androidx.room.*

@Dao
interface LogDao {

    @Query("SELECT * FROM LogEntries ORDER BY epochTime ASC")
    fun getAll(): List<LogEntry>

    @Query("SELECT * FROM LogEntries WHERE type >= :logLevel ORDER BY epochTime ASC")
    fun getLevel(logLevel: Int): List<LogEntry>

    @Query("SELECT * FROM LogEntries WHERE type >= :logLevel ORDER BY epochTime DESC LIMIT :length")
    // @Query("SELECT * FROM LogEntries WHERE type >= :logLevel LIMIT :length")
    fun getLevelAndLength(logLevel: Int, length: Int): List<LogEntry>

    @Query("SELECT * FROM LogEntries WHERE epochTime BETWEEN :firstTime AND :lastTime ORDER BY epochTime ASC")
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