package com.ixam97.carStatsViewer.activities

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import com.google.gson.GsonBuilder
import com.ixam97.carStatsViewer.utils.InAppLogger
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.appPreferences.AppPreferences
import com.ixam97.carStatsViewer.dataManager.DataManagers
import com.ixam97.carStatsViewer.mailSender.MailSender
import kotlinx.android.synthetic.main.activity_log.*
import kotlinx.coroutines.*
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.*

class LogActivity : Activity() {

    private lateinit var appPreferences: AppPreferences

    private fun CharSequence?.isValidEmail() = !isNullOrEmpty() && Patterns.EMAIL_ADDRESS.matcher(this).matches()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appPreferences = AppPreferences(applicationContext)

        setContentView(R.layout.activity_log)

        log_text_target_mail.setText(appPreferences.logTargetAddress)
        log_text_sender.setText(appPreferences.logUserName)

        log_progress_bar.visibility = View.VISIBLE

        log_button_back.setOnClickListener {
            finish()
        }

        log_button_send.setOnClickListener {

            val mailAdr = log_text_target_mail.text.toString()
            val senderName = log_text_sender.text.toString()

            var senderMail = ""

            appPreferences.logTargetAddress = mailAdr
            appPreferences.logUserName = senderName

            if (!mailAdr.isValidEmail())
                Toast.makeText(this, "Invalid mail address!", Toast.LENGTH_SHORT).show()
            else {
                log_progress_bar.visibility = View.VISIBLE
                CoroutineScope(Dispatchers.Default).launch() {
                    try {
                        val sender = if (appPreferences.smtpAddress != "" && appPreferences.smtpPassword != "" && appPreferences.smtpServer != "") {
                            senderMail = appPreferences.smtpAddress
                            MailSender(appPreferences.smtpAddress, appPreferences.smtpPassword, appPreferences.smtpServer)
                        } else {
                            if (resources.getIdentifier("logmail_email_address", "string", applicationContext.packageName) != 0) {
                                senderMail = getString(resources.getIdentifier("logmail_email_address", "string", applicationContext.packageName))
                                MailSender(
                                    senderMail,
                                    getString(resources.getIdentifier("logmail_password", "string", applicationContext.packageName)),
                                    getString(resources.getIdentifier("logmail_server", "string", applicationContext.packageName)))
                            } else {
                                runOnUiThread {
                                    Toast.makeText(this@LogActivity, "No SMTP login", Toast.LENGTH_LONG).show()
                                }
                                null
                            }
                        }

                        if (sender == null) return@launch

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
                        sender.sendMail("Debug Log ${Date()} from $senderName", InAppLogger.getLogString(), senderMail, mailAdr)
                        runOnUiThread {
                            log_progress_bar.visibility = View.GONE
                            Toast.makeText(this@LogActivity, "Log and JSON sent to $mailAdr", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: java.lang.Exception) {
                        runOnUiThread {
                            log_progress_bar.visibility = View.GONE
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
                    val textValue = "MARKERS: \n" + gson.toJson(DataManagers.CURRENT_TRIP.dataManager.tripData?.markers?: 0) + "\n\nCHARGE CURVE:\n" + gson.toJson(
                        DataManagers.CURRENT_TRIP.dataManager.tripData?.chargePlotLine?: 0)
                    log_text_view.text = textValue
                }
                "LOG" -> {
                    log_button_show_json.text = "JSON"
                    log_text_view.text = InAppLogger.getLogString()
                }
            }
        }

        log_button_login.setOnClickListener {
            // copyToClipboard(log_text_view.text.toString())
            val credentialsDialog = AlertDialog.Builder(this@LogActivity).apply {
                val layout = LayoutInflater.from(this@LogActivity).inflate(R.layout.dialog_smtp_credentials, null)
                val smtp_dialog_address = layout.findViewById<EditText>(R.id.smtp_dialog_address)
                smtp_dialog_address.setText(appPreferences.smtpAddress)
                val smtp_dialog_password = layout.findViewById<EditText>(R.id.smtp_dialog_password)
                smtp_dialog_password.setText(appPreferences.smtpPassword)
                val smtp_dialog_server = layout.findViewById<EditText>(R.id.smtp_dialog_server)
                smtp_dialog_server.setText(appPreferences.smtpServer)

                setView(layout)

                setPositiveButton("OK") { dialog, _ ->
                    appPreferences.smtpAddress = smtp_dialog_address.text.toString()
                    appPreferences.smtpPassword = smtp_dialog_password.text.toString()
                    appPreferences.smtpServer = smtp_dialog_server.text.toString()
                }
                setTitle("SMTP Login")
                setCancelable(true)
                create()
            }
            credentialsDialog.show()
        }

        log_button_reload.setOnClickListener {
            log_text_view.text = InAppLogger.getLogString()
        }

        log_reset_log.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                withContext(Dispatchers.IO) {
                    InAppLogger.resetLog()
                    runOnUiThread {
                        InAppLogger.log("Cleared log")
                        log_text_view.text = ""
                    }
                }
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            val logString = InAppLogger.getLogString()
            runOnUiThread {
                log_text_view.text = logString
                log_progress_bar.visibility = View.GONE
            }
            delay(100)
            runOnUiThread {
                log_scrollview.fullScroll(View.FOCUS_DOWN)
            }
        }
    }
}