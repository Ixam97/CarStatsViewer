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

    private const val VERBOSE = 1
    private const val DEBUG = 2
    private const val INFO = 3
    private const val WARN = 4
    private const val ERROR = 5

    private val typeMap = mapOf(
        0 to "?",
        1 to "V",
        2 to "D",
        3 to "I",
        4 to "W",
        5 to "E"
    )

    private val logDatabase = Room.databaseBuilder(
        CarStatsViewer.appContext,
        LogDatabase::class.java,
        "LogDatabase"
    ).build()
    private val logDao = logDatabase.logDao()

    fun v(message: String) = log(message, VERBOSE)
    fun d(message: String) = log(message, DEBUG)
    fun i(message: String) = log(message, INFO)
    fun w(message: String) = log(message, WARN)
    fun e(message: String) = log(message, ERROR)

    fun log(message: String, type: Int = INFO) {
        val logTime = System.currentTimeMillis()
        val messageTime = SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS").format(logTime)
        Log.d("InAppLogger:", "$messageTime: $message")

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
                logString += "${SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS").format(it.epochTime)} ${typeMap[it.type]}: ${it.message}\n"
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