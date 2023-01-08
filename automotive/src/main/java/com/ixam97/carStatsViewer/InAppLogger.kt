package com.ixam97.carStatsViewer

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_log.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import com.ixam97.carStatsViewer.objects.AppPreferences

object InAppLogger {

    var logArray = ArrayList<String>()

    private var lastVHALCallback = ""
    private var numVHALCallbacks = 0

    private var lastUIUpdate = ""
    private var numUIUpdates = 0


    private var lastNotificationUpdate = ""
    private var numNotificationUpdates = 0

    fun log(message: String) {
        var messageTime = SimpleDateFormat("dd.MM.yyyy hh:mm:ss").format(Date())
        val logMessage = String.format("%s: %s", messageTime, message)
        android.util.Log.d("InAppLogger:", logMessage)
        if (logArray.size > 10000) logArray.removeAt(0)
        logArray.add(logMessage)
    }

    fun deepLog(message: String) {
        if (AppPreferences.deepLog) log("DEEP LOG: $message")
    }

    fun logVHALCallback() {
        numVHALCallbacks++
        lastVHALCallback = SimpleDateFormat("dd.MM.yyyy hh:mm:ss").format(Date())
    }

    fun getVHALLog(): String {
        return String.format("Total of %d VHAL callbacks, last at %s", numVHALCallbacks, lastVHALCallback)
    }

    fun logUIUpdate(){
        numUIUpdates++
        lastUIUpdate = SimpleDateFormat("dd.MM.yyyy hh:mm:ss").format(Date())
    }

    fun getUILog(): String {
        return String.format("Total of %d UI updates, last at %s", numUIUpdates, lastUIUpdate)
    }

    fun logNotificationUpdate() {
        numNotificationUpdates++
        lastNotificationUpdate = SimpleDateFormat("dd.MM.yyyy hh:mm:ss").format(Date())
    }

    fun getNotificationLog(): String {
        return String.format("Total of %d notification updates, last at %s", numNotificationUpdates, lastNotificationUpdate)
    }

    fun logAllToConsole() {
        for (i in 0 until logArray.size) {
            android.util.Log.d("InAppLogger:", logArray[i])
        }
    }

    fun copyToClipboard(context: Context) {
        var clipboardString = ""

        for (i in 0 until logArray.size) {
            clipboardString += (logArray[i] + "\n")
        }

        clipboardString += getVHALLog() + "\n" + getUILog() + "\n" + getNotificationLog()

        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("CarStatsViewerLog", clipboardString)
        clipboardManager.setPrimaryClip(clipData)

        Toast.makeText(context,"Copied log to clipboard", Toast.LENGTH_LONG).show()
    }
}

class LogActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        InAppLogger.log("LogActivity.onCreate")

        val sharedPref = applicationContext.getSharedPreferences(
            getString(R.string.preferences_file_key), Context.MODE_PRIVATE
        )

        setContentView(R.layout.activity_log)

        log_switch_deep_log.isChecked = AppPreferences.deepLog

        for (i in 0 until InAppLogger.logArray.size) {
            val logTextView = TextView(this)
            logTextView.text = InAppLogger.logArray[i]
            log_log.addView(logTextView)
        }

        val logVHALTextView = TextView(this)
        val logUIUpdatesTextView = TextView(this)
        val logNotificationUpdatesTextView = TextView(this)
        logVHALTextView.text = InAppLogger.getVHALLog()
        logUIUpdatesTextView.text = InAppLogger.getUILog()
        logNotificationUpdatesTextView.text = InAppLogger.getNotificationLog()
        log_log.addView(logVHALTextView)
        log_log.addView(logUIUpdatesTextView)
        log_log.addView(logNotificationUpdatesTextView)

        log_button_back.setOnClickListener {
            finish()
        }

        log_button_copy.setOnClickListener {
            InAppLogger.copyToClipboard(this)
        }

        log_button_reload.setOnClickListener {
            finish()
            startActivity(intent)
        }

        log_reset_log.setOnClickListener {
            InAppLogger.logArray.clear()
            InAppLogger.log("Cleared log")
            finish()
            startActivity(intent)
        }

        log_switch_deep_log.setOnClickListener {
            sharedPref.edit()
                .putBoolean(getString(R.string.preferences_deep_log_key), log_switch_deep_log.isChecked)
                .apply()
            AppPreferences.deepLog = log_switch_deep_log.isChecked
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        InAppLogger.log("LogActivity.onDestroy")
    }
}