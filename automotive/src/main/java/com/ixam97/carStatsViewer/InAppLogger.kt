package com.ixam97.carStatsViewer

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import com.google.gson.GsonBuilder
import com.ixam97.carStatsViewer.appPreferences.AppPreferences
import com.ixam97.carStatsViewer.dataManager.DataManagers
import kotlinx.android.synthetic.main.activity_log.*
import java.text.SimpleDateFormat
import java.util.*


object InAppLogger {

    var logArray = ArrayList<String>()

    private var lastVHALCallback = ""
    private var numVHALCallbacks = 0

    private var lastUIUpdate = ""
    private var numUIUpdates = 0


    private var lastNotificationUpdate = ""
    private var numNotificationUpdates = 0

    fun log(message: String) {
        var messageTime = SimpleDateFormat("dd.MM.yyyy hh:mm:ss.SSS").format(Date())
        val logMessage = String.format("%s: %s", messageTime, message)
        android.util.Log.d("InAppLogger:", logMessage)
        if (logArray.size > 1_000) logArray.removeAt(0)
        logArray.add(logMessage)
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

        // clipboardString += getVHALLog() + "\n" + getUILog() + "\n" + getNotificationLog()

        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("CarStatsViewerLog", clipboardString)
        clipboardManager.setPrimaryClip(clipData)

        Toast.makeText(context,"Copied log to clipboard", Toast.LENGTH_LONG).show()
    }
}

class LogActivity : Activity() {

    private lateinit var appPreferences: AppPreferences

    fun CharSequence?.isValidEmail() = !isNullOrEmpty() && Patterns.EMAIL_ADDRESS.matcher(this).matches()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        InAppLogger.log("LogActivity.onCreate")

        appPreferences = AppPreferences(applicationContext)

        setContentView(R.layout.activity_log)

        log_switch_deep_log.isChecked = appPreferences.deepLog

        // val logTextView = TextView(this)
        // logTextView.typeface
        log_text_view.text = getLogString()

        log_button_back.setOnClickListener {
            finish()
        }

        log_button_send.setOnClickListener {

            val mailAdr = log_text_target_mail.text.toString()
            val senderName = log_text_sender.text.toString()

            if (!mailAdr.isValidEmail())
                Toast.makeText(this, "Invalid mail address!", Toast.LENGTH_SHORT).show()
            else {
                CoroutineScope(Dispatchers.Default).launch() {
                    try {
                        val sender = MailSender(getString(R.string.email_address), getString(R.string.password))
                        enumValues<DataManagers>().forEach {
                            try {
                                val dir = File(applicationContext.filesDir, "TripData")
                                if (!dir.exists()) {
                                    InAppLogger.log("TRIP DATA: Directory TripData does not exist!")

                                } else {
                                    val gpxFile = File(dir, "${it.dataManager.printableName}.json")
                                    if (!gpxFile.exists() && gpxFile.length() > 0) {
                                        InAppLogger.log("TRIP_DATA File ${it.dataManager.printableName}.json does not exist!")
                                    }
                                    else {
                                        sender.addAttachment(gpxFile)
                                    }
                                }
                            } catch(e: java.lang.Exception) {
                                InAppLogger.log("Can't attach file ${it.dataManager.printableName}")
                            }

                        }
                        sender.sendMail("Debug Log ${Date()} from $senderName", getLogString(), "CarStatsViewer@ixam97.de", mailAdr)
                        runOnUiThread {
                            Toast.makeText(this@LogActivity, "Log and JSON sent to $mailAdr", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: java.lang.Exception) {
                        runOnUiThread {
                            Toast.makeText(this@LogActivity, "Sending E-Mail failed. See log.", Toast.LENGTH_LONG).show()
                        }
                        InAppLogger.log(e.stackTraceToString())
                    }
                }
            }
        }

        log_button_show_json.setOnClickListener {
            val currentText = log_button_show_json.text
            when (currentText) {
                "JSON" -> {
                    log_button_show_json.text = "LOG"
                    val gson = GsonBuilder()
                        .setExclusionStrategies(appPreferences.exclusionStrategy)
                        .setPrettyPrinting()
                        .create()
                    val textValue = "MARKERS: \n" + gson.toJson(DataManagers.CURRENT_TRIP.dataManager.tripData?.markers?: 0) + "\n\nCHARGE CURVE:\n" + gson.toJson(DataManagers.CURRENT_TRIP.dataManager.tripData?.chargePlotLine?: 0)
                    log_text_view.text = textValue
                }
                "LOG" -> {
                    log_button_show_json.text = "JSON"
                    log_text_view.text = getLogString()
                }
            }
        }

        log_button_copy.setOnClickListener {
            copyToClipboard(log_text_view.text.toString())
        }

        log_button_reload.setOnClickListener {
            log_text_view.text = getLogString()
        }

        log_reset_log.setOnClickListener {
            InAppLogger.logArray.clear()
            InAppLogger.log("Cleared log")
            finish()
            startActivity(intent)
        }

        log_switch_deep_log.setOnClickListener {
            appPreferences.deepLog = log_switch_deep_log.isChecked
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        InAppLogger.log("LogActivity.onDestroy")
    }

    private fun getLogString(): String {
        var logString = ""

        for (i in 0 until InAppLogger.logArray.size) {
            logString += "${InAppLogger.logArray[i]}\n"
        }

        // logString += "${InAppLogger.getVHALLog()}\n${InAppLogger.getUILog()}\n${InAppLogger.getNotificationLog()}\n"
        logString += "v${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})"

        return logString
    }

    private fun copyToClipboard(clipboardString: String) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("CarStatsViewerLog", clipboardString)
        clipboardManager.setPrimaryClip(clipData)

        Toast.makeText(this,"Copied to clipboard", Toast.LENGTH_LONG).show()
    }
}