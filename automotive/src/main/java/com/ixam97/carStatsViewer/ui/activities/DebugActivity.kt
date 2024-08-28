package com.ixam97.carStatsViewer.ui.activities

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.google.gson.GsonBuilder
import com.ixam97.carStatsViewer.BuildConfig
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.adapters.LogAdapter
import com.ixam97.carStatsViewer.appPreferences.AppPreferences
import com.ixam97.carStatsViewer.compose.ComposeSettingsActivity
import com.ixam97.carStatsViewer.database.log.LogEntry
import com.ixam97.carStatsViewer.database.tripData.TripType
import com.ixam97.carStatsViewer.databinding.ActivityDebugBinding
import com.ixam97.carStatsViewer.mailSender.MailSender
import com.ixam97.carStatsViewer.ui.views.MultiSelectWidget
import com.ixam97.carStatsViewer.ui.views.SnackbarWidget
import com.ixam97.carStatsViewer.utils.DistanceUnitEnum
import com.ixam97.carStatsViewer.utils.InAppLogger
import com.ixam97.carStatsViewer.utils.logLength
import com.ixam97.carStatsViewer.utils.logLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class DebugActivity : FragmentActivity() {

    private lateinit var appPreferences: AppPreferences
    private lateinit var binding: ActivityDebugBinding
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val logList = mutableListOf<LogEntry>()
    private val logAdapter = LogAdapter(logList)

    private fun CharSequence?.isValidEmail() = !isNullOrEmpty() && Patterns.EMAIL_ADDRESS.matcher(this).matches()

    private val logLengths = arrayOf(0, 500, 1000, 2000, 5000, 10_000)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (CarStatsViewer.appPreferences.colorTheme > 0) setTheme(R.style.ColorTestTheme)
        binding = ActivityDebugBinding.inflate(layoutInflater)

        val view = binding.root

        with(binding) {

            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    InAppLogger.realTimeLog.collectLatest {
                        runOnUiThread {
                            if (logLiveLog.isChecked) {
                                it?.let { logEntry ->
                                    if (logEntry.type >= appPreferences.logLevel + 2) {
                                        logList.add(
                                            0,
                                            it
                                        ) //"${SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS").format(logEntry.epochTime)} ${InAppLogger.typeSymbol(logEntry.type)}: ${logEntry.message}")
                                        logAdapter.notifyDataSetChanged()
                                        logRecyclerview.scrollToPosition(0)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            appPreferences = AppPreferences(applicationContext)

            setContentView(view)

            logLiveLog.isChecked = true

            logRecyclerview.adapter = logAdapter
            logRecyclerview.layoutManager = LinearLayoutManager(this@DebugActivity).apply {
                reverseLayout = true
            }

            logTextTargetMail.setText(appPreferences.logTargetAddress)
            logTextSender.setText(appPreferences.logUserName)

            logProgressBar.visibility = View.VISIBLE

            debugButtonBack.setOnClickListener {
                finish()
                if (BuildConfig.FLAVOR_aaos != "carapp")
                    overridePendingTransition(R.anim.stay_still, R.anim.slide_out_down)
            }

            if(BuildConfig.FLAVOR_version == "dev") {
                debugCompose.setOnRowClickListener {
                    startActivity(Intent(this@DebugActivity, ComposeSettingsActivity::class.java))
                }
            } else {
                debugCompose.visibility = View.GONE
            }

            logButtonSend.setOnClickListener {

                val mailAdr = logTextTargetMail.text.toString()
                val senderName = logTextSender.text.toString()

                var senderMail = ""

                appPreferences.logTargetAddress = mailAdr
                appPreferences.logUserName = senderName

                if (!mailAdr.isValidEmail())
                    Toast.makeText(this@DebugActivity, "Invalid mail address!", Toast.LENGTH_SHORT).show()
                else {
                    logProgressBar.visibility = View.VISIBLE
                    CoroutineScope(Dispatchers.Default).launch() {
                        try {
                            // throw Exception("Test")
                            val sender =
                                if (appPreferences.smtpAddress != "" && appPreferences.smtpPassword != "" && appPreferences.smtpServer != "") {
                                    senderMail = appPreferences.smtpAddress
                                    MailSender(
                                        appPreferences.smtpAddress,
                                        appPreferences.smtpPassword,
                                        appPreferences.smtpServer
                                    )
                                } else {
                                    if (resources.getIdentifier(
                                            "logmail_email_address",
                                            "string",
                                            applicationContext.packageName
                                        ) != 0
                                    ) {
                                        senderMail = getString(
                                            resources.getIdentifier(
                                                "logmail_email_address",
                                                "string",
                                                applicationContext.packageName
                                            )
                                        )
                                        MailSender(
                                            senderMail,
                                            getString(
                                                resources.getIdentifier(
                                                    "logmail_password",
                                                    "string",
                                                    applicationContext.packageName
                                                )
                                            ),
                                            getString(
                                                resources.getIdentifier(
                                                    "logmail_server",
                                                    "string",
                                                    applicationContext.packageName
                                                )
                                            )
                                        )
                                    } else {
                                        runOnUiThread {
                                            Toast.makeText(
                                                this@DebugActivity,
                                                "No SMTP login",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                        null
                                    }
                                }

                            if (sender == null) return@launch

                            sender.addAttachment(
                                content = InAppLogger.getLogString(
                                    appPreferences.logLevel + 2,
                                    logLengths[appPreferences.logLength]
                                ),
                                fileName = "log_${System.currentTimeMillis()}.txt"
                            )

                            CarStatsViewer.screenshotBitmap.forEachIndexed { index, bitmap ->
                                sender.addAttachment(bitmap, "Screenshot_$index")
                            }

                            if (checkboxSendCurrentTrips.isChecked) {
                                for (activeDrivingSessionsId in CarStatsViewer.tripDataSource.getActiveDrivingSessionsIds()) {
                                    val trip = CarStatsViewer.tripDataSource.getFullDrivingSession(
                                        activeDrivingSessionsId
                                    )
                                    sender.addAttachment(
                                        content = GsonBuilder().setPrettyPrinting().create()
                                            .toJson(trip),
                                        fileName = "${TripType.tripTypesNameMap[trip.session_type] ?: "Unknown"}_current.json"
                                    )
                                }
                            }

                            if (checkboxSendPastTrips.isChecked) {
                                for (pastDrivingSessionId in CarStatsViewer.tripDataSource.getPastDrivingSessionIds()) {
                                    val trip = CarStatsViewer.tripDataSource.getFullDrivingSession(
                                        pastDrivingSessionId
                                    )
                                    sender.addAttachment(
                                        content = GsonBuilder().setPrettyPrinting().create()
                                            .toJson(trip),
                                        fileName = "${TripType.tripTypesNameMap[trip.session_type] ?: "Unknown"}_${trip.start_epoch_time}.json"
                                    )
                                }
                            }

                            sender.sendMail(
                                "Debug Log ${Date()} from $senderName",
                                "See attachments.",
                                senderMail,
                                mailAdr
                            )

                            CarStatsViewer.screenshotBitmap.clear()

                            runOnUiThread {
                                logProgressBar.visibility = View.GONE
                                SnackbarWidget.Builder(this@DebugActivity, "Log sent to $mailAdr")
                                    .setDuration(3_000)
                                    .setStartDrawable(R.drawable.ic_mail)
                                    .show()
                                // Toast.makeText(this@DebugActivity, "Log sent to $mailAdr", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: java.lang.Exception) {
                            runOnUiThread {
                                logProgressBar.visibility = View.GONE
                                SnackbarWidget.Builder(
                                    this@DebugActivity,
                                    "Sending E-Mail failed. See log."
                                )
                                    .setDuration(3_000)
                                    .setIsError(true)
                                    .show()
                                // Toast.makeText(this@DebugActivity, "Sending E-Mail failed. See log.", Toast.LENGTH_LONG).show()
                            }
                            InAppLogger.e(e.stackTraceToString())
                        }
                    }
                }
            }

            // log_text_view.setOnClickListener {
            //     log_scrollview.fullScroll(View.FOCUS_DOWN)
            // }

            logButtonReload.setOnClickListener {
                activityScope.launch {
                    withContext(Dispatchers.IO) {
                        loadLog()
                    }
                }
            }

            logLiveLog.setOnClickListener {
                activityScope.launch {
                    withContext(Dispatchers.IO) {
                        loadLog()
                    }
                }
            }

            logResetLog.setOnClickListener {
                activityScope.launch {
                    withContext(Dispatchers.IO) {
                        InAppLogger.resetLog()
                        InAppLogger.i("Cleared log")
                        loadLog()
                    }
                }
            }

            debugKill.setOnClickListener {
                try {
                    throw RuntimeException("Debug Exception")
                } catch (e: Exception) {
                    if (getString(R.string.useFirebase) == "true") {
                        Firebase.crashlytics.log("This is a logging test")
                        Firebase.crashlytics.recordException(e)
                    } else {
                        InAppLogger.e(e.toString())
                    }
                }
            }

            debugSettings.setOnClickListener {
                val settingsDialog =
                    AlertDialog.Builder(ContextThemeWrapper(this@DebugActivity, R.style.redTextEdit)).apply {
                        val layout = LayoutInflater.from(context)
                            .inflate(R.layout.dialog_debug_settings, null)

                        val debug_miles_checkbox =
                            layout.findViewById<CheckBox>(R.id.debug_miles_switch)
                        val debug_screenshot_checkbox =
                            layout.findViewById<CheckBox>(R.id.debug_screenshot_switch)
                        val log_level_multiselect =
                            layout.findViewById<MultiSelectWidget>(R.id.debug_level_multiselect)
                        val log_length_multiselect =
                            layout.findViewById<MultiSelectWidget>(R.id.debug_length_multiselect)

                        debug_miles_checkbox.isChecked =
                            appPreferences.distanceUnit == DistanceUnitEnum.MILES
                        debug_screenshot_checkbox.isChecked = appPreferences.showScreenshotButton

                        log_level_multiselect.entries = arrayListOf<String>(
                            "Verbose",
                            "Debug",
                            "Info",
                            "Warning",
                            "Error"
                        )

                        log_length_multiselect.entries = arrayListOf(
                            "all",
                            "500",
                            "1 000",
                            "2 000",
                            "5 000",
                            "10 000"
                        )

                        log_level_multiselect.selectedIndex = appPreferences.logLevel
                        log_length_multiselect.selectedIndex = appPreferences.logLength

                        log_level_multiselect.setOnIndexChangedListener {
                            appPreferences.logLevel = log_level_multiselect.selectedIndex
                        }
                        log_length_multiselect.setOnIndexChangedListener {
                            appPreferences.logLength = log_length_multiselect.selectedIndex
                        }

                        debug_miles_checkbox.setOnClickListener {
                            appPreferences.distanceUnit = if (debug_miles_checkbox.isChecked) {
                                DistanceUnitEnum.MILES
                            } else {
                                DistanceUnitEnum.KM
                            }
                        }

                        debug_screenshot_checkbox.setOnClickListener {
                            appPreferences.showScreenshotButton =
                                debug_screenshot_checkbox.isChecked
                        }

                        setView(layout)

                        setPositiveButton("OK") { dialog, _ ->
                            activityScope.launch {
                                withContext(Dispatchers.IO) {
                                    loadLog()
                                }
                            }
                        }
                        setTitle("Debug settings")
                        setCancelable(true)
                        create()
                    }
                settingsDialog.show()
            }

            activityScope.launch {
                withContext(Dispatchers.IO) {
                    loadLog()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
    }

    private suspend fun loadLog() {
        // val startTime = System.currentTimeMillis()
        with(binding){
            runOnUiThread {
                logProgressBar.visibility = View.VISIBLE
            }
            // val logString = InAppLogger.getLogString(appPreferences.logLevel + 2, logLengths[appPreferences.logLength])
            // val logLines = logString.split("[\n]+".toRegex()).toTypedArray()
            // val logLines = InAppLogger.getLogArray(appPreferences.logLevel + 2, logLengths[appPreferences.logLength])

            logList.clear()
            logList.addAll(
                InAppLogger.getLogEntries(
                    appPreferences.logLevel + 2,
                    logLengths[appPreferences.logLength]
                ).reversed()
            )

            runOnUiThread {
                // logAdapter.notifyDataSetChanged()
                // logList.add(0,"Log displayed in ${System.currentTimeMillis() - startTime} ms")
                // logList.add(0, "------------------------------------------------------------")
                logAdapter.notifyDataSetChanged()
                if (logList.size > 0)
                    logRecyclerview.scrollToPosition(0)
                logProgressBar.visibility = View.GONE
            }
        }
    }
}