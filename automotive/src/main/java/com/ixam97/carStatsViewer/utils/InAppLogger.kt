package com.ixam97.carStatsViewer.utils

import android.util.Log
import androidx.room.Room
import com.ixam97.carStatsViewer.BuildConfig
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.database.log.LogDatabase
import com.ixam97.carStatsViewer.database.log.LogEntry
import kotlinx.coroutines.*
import java.text.SimpleDateFormat

object InAppLogger {

    private fun typeSymbol(type: Int): String = when (type) {
        Log.VERBOSE -> "V"
        Log.DEBUG -> "D"
        Log.INFO -> "I"
        Log.WARN -> "W"
        Log.ERROR -> "E"
        else -> "?"
    }

    private val logDatabase = Room.databaseBuilder(
        CarStatsViewer.appContext,
        LogDatabase::class.java,
        "LogDatabase"
    ).build()
    private val logDao = logDatabase.logDao()

    fun v(message: String) = log(message, Log.VERBOSE)
    fun d(message: String) = log(message, Log.DEBUG)
    fun i(message: String) = log(message, Log.INFO)
    fun w(message: String) = log(message, Log.WARN)
    fun e(message: String) = log(message, Log.ERROR)

    fun log(message: String, type: Int = Log.INFO) {
        val logTime = System.currentTimeMillis()
        val messageTime = SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS").format(logTime)
        Log.println(type,"InAppLogger:", "$messageTime ${typeSymbol(type)}: $message")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                logDao.insert(LogEntry(
                    epochTime = logTime,
                    type = type,
                    message = message))
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getLogString(): String {
        var logString = ""
        try {
            val startTime = System.currentTimeMillis()
            val logEntries: List<LogEntry> = logDao.getAll()
            logEntries.forEach {
                if (it.message.contains("Car Stats Viewer")) logString += "------------------------------------------------------------\n"
                logString += "${SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS").format(it.epochTime)} ${typeSymbol(it.type)}: ${it.message}\n"
            }
            logString += "------------------------------------------------------------\n" +
                "Loaded ${logEntries.size} log entries in ${System.currentTimeMillis() - startTime} ms\n" +
                "V${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})"
        } catch (e: java.lang.Exception) {
            resetLog()
            e("Loading Log failed. It has been reset.\n${e.stackTraceToString()}")
        }
        return logString
    }

    fun resetLog() {
        try {
            logDao.clear()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}