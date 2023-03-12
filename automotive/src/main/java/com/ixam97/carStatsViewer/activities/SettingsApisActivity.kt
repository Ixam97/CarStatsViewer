package com.ixam97.carStatsViewer.activities

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import com.ixam97.carStatsViewer.CarStatsViewer.Companion.liveDataApis
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.appPreferences.AppPreferences
import kotlinx.android.synthetic.main.activity_settings_apis.*

class SettingsApisActivity: Activity() {

    lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings_apis)

        appPreferences = AppPreferences(this)

        settings_apis_button_back.setOnClickListener {
            finish()
        }

        settings_apis_abrp_token.setOnClickListener {
            liveDataApis[0].showSettingsDialog(this@SettingsApisActivity)
        }

        settings_apis_smtp_login.setOnClickListener {
            showSmtpLoginDialog()
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
            setTitle("SMTP Login")
            setCancelable(true)
            create()
        }
        credentialsDialog.show()
    }
}