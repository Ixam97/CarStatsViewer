package com.ixam97.carStatsViewer.ui.activities

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.utils.applyTypeface
import com.ixam97.carStatsViewer.utils.setContentViewAndTheme
import kotlinx.android.synthetic.main.activity_settings_apis.*
import kotlinx.coroutines.launch

class SettingsApisActivity: FragmentActivity() {

    val appPreferences = CarStatsViewer.appPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentViewAndTheme(this, R.layout.activity_settings_apis)

        CarStatsViewer.typefaceMedium?.let {
            applyTypeface(settings_apis_activity)
        }

        settings_apis_connection_selector.entries = ArrayList(CarStatsViewer.liveDataApis.map { getString(it.apiNameStringId) })
        settings_apis_connection_selector.selectedIndex = appPreferences.mainViewConnectionApi
        settings_apis_connection_selector.setOnIndexChangedListener {
            appPreferences.mainViewConnectionApi = settings_apis_connection_selector.selectedIndex
        }

        settings_apis_button_back.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        settings_apis_abrp_row.setOnMainClickListener {
            CarStatsViewer.liveDataApis[0].showSettingsDialog(this@SettingsApisActivity)
        }

        settings_apis_http_row.setOnMainClickListener {
            CarStatsViewer.liveDataApis[1].showSettingsDialog(this@SettingsApisActivity)
        }

        settings_apis_smtp_row.setOnMainClickListener {
            showSmtpLoginDialog()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                CarStatsViewer.watchdog.watchdogStateFlow.collect {
                    settings_apis_abrp_row.connectionStatus = it.apiState[CarStatsViewer.liveDataApis[0].apiIdentifier]?:0
                    settings_apis_http_row.connectionStatus = it.apiState[CarStatsViewer.liveDataApis[1].apiIdentifier]?:0
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