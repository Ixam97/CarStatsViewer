package com.ixam97.carStatsViewer.ui.activities

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ixam97.carStatsViewer.BuildConfig
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.databinding.ActivitySettingsApisBinding
import kotlinx.coroutines.launch

class SettingsApisActivity: FragmentActivity() {

    private lateinit var binding: ActivitySettingsApisBinding

    val appPreferences = CarStatsViewer.appPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (CarStatsViewer.appPreferences.colorTheme > 0) setTheme(R.style.ColorTestTheme)
        binding = ActivitySettingsApisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding){
            settingsApisConnectionSelector.entries =
                ArrayList(CarStatsViewer.liveDataApis.map { getString(it.apiNameStringId) })
            settingsApisConnectionSelector.selectedIndex = appPreferences.mainViewConnectionApi
            settingsApisConnectionSelector.setOnIndexChangedListener {
                appPreferences.mainViewConnectionApi = settingsApisConnectionSelector.selectedIndex
            }

            settingsApisButtonBack.setOnClickListener {
                finish()
                if (BuildConfig.FLAVOR_aaos != "carapp")
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            }

            settingsApisAbrpRow.setOnMainClickListener {
                CarStatsViewer.liveDataApis[0].showSettingsDialog(this@SettingsApisActivity)
            }

            settingsApisHttpRow.setOnMainClickListener {
                CarStatsViewer.liveDataApis[1].showSettingsDialog(this@SettingsApisActivity)
            }

            settingsApisSmtpRow.setOnMainClickListener {
                showSmtpLoginDialog()
            }

            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    CarStatsViewer.watchdog.watchdogStateFlow.collect {
                        settingsApisAbrpRow.connectionStatus =
                            it.apiState[CarStatsViewer.liveDataApis[0].apiIdentifier] ?: 0
                        settingsApisHttpRow.connectionStatus =
                            it.apiState[CarStatsViewer.liveDataApis[1].apiIdentifier] ?: 0
                    }
                }
            }
        }
    }

    private fun showSmtpLoginDialog() {
        val credentialsDialog = AlertDialog.Builder(this@SettingsApisActivity).apply {
            val layout = LayoutInflater.from(this@SettingsApisActivity).inflate(R.layout.dialog_smtp_credentials, null)
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
            setTitle(getString(R.string.settings_apis_smtp))
            setMessage(getString(R.string.smtp_description))
            setCancelable(true)
            create()
        }
        credentialsDialog.show()
    }
}