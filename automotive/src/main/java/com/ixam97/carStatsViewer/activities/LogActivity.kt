package com.ixam97.carStatsViewer.activities

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.utils.InAppLogger
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.appPreferences.AppPreferences
import com.ixam97.carStatsViewer.dataManager.DataManagers
import com.ixam97.carStatsViewer.mailSender.MailSender
import kotlinx.android.synthetic.main.activity_log.*
import kotlinx.coroutines.*
import java.io.File
import java.util.*

class LogActivity : Activity() {

    private lateinit var appPreferences: AppPreferences

    private fun CharSequence?.isValidEmail() = !isNullOrEmpty() && Patterns.EMAIL_ADDRESS.matcher(this).matches()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
/*
        CoroutineScope(Dispatchers.IO).launch {
            val drivingPointsList = CarStatsViewer.tripDao.getAllDrivingPoints()
            Log.i("TripDataDatabase","Size: ${drivingPointsList.size}")
            drivingPointsList.forEach {
                Log.i("TripDataDatabase", it.toString())
            }
        }

 */

        appPreferences = AppPreferences(applicationContext)

        setContentView(R.layout.activity_log)

        log_text_target_mail.setText(appPreferences.logTargetAddress)
        log_text_sender.setText(appPreferences.logUserName)

        log_progress_bar.visibility = View.VISIBLE

        log_button_back.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.stay_still, R.anim.slide_out_down)
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
                                    InAppLogger.w("TRIP DATA: Directory TripData does not exist!")

                                } else {
                                    val gpxFile = File(dir, "${it.dataManager.printableName}.json")
                                    if (!gpxFile.exists() && gpxFile.length() > 0) {
                                        InAppLogger.w("TRIP_DATA File ${it.dataManager.printableName}.json does not exist!")
                                    }
                                    else {
                                        sender.addAttachment(gpxFile)
                                    }
                                }
                            } catch(e: java.lang.Exception) {
                                InAppLogger.e("Can't attach file ${it.dataManager.printableName}")
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
                        InAppLogger.e(e.stackTraceToString())
                    }
                }
            }
        }

        log_button_reload.setOnClickListener {
            loadLog()
        }

        log_reset_log.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                withContext(Dispatchers.IO) {
                    InAppLogger.resetLog()
                    runOnUiThread {
                        InAppLogger.i("Cleared log")
                        log_text_view.text = ""
                    }
                }
            }
        }

        loadLog()
    }

    private fun loadLog() {
        CoroutineScope(Dispatchers.IO).launch {
            runOnUiThread {
                log_progress_bar.visibility = View.VISIBLE
            }
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