package com.ixam97.carStatsViewer

import kotlinx.coroutines.*
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object InAppLogger {

    fun log(message: String) {
        val messageTime = SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS").format(Date())
        val logMessage = String.format("%s: %s", messageTime, message)
        android.util.Log.d("InAppLogger:", logMessage)

        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.IO) {
                val logFile = File(CarStatsViewer.appContext.filesDir,"InAppLogger.txt")
                if (!logFile.exists()) {
                    try {
                        logFile.createNewFile()
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }
                }
                try {
                    BufferedWriter(FileWriter(logFile, true)).apply {
                        append(logMessage)
                        newLine()
                        close()
                    }
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun getLogString(): String {
        val logFile = File(CarStatsViewer.appContext.filesDir, "InAppLogger.txt")
        return "${logFile.readText()} v${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})"
    }
}