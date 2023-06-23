package com.ixam97.carStatsViewer.ui.activities

import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.liveDataApi.LiveDataApi
import kotlinx.android.synthetic.main.activity_settings_apis.*

class SettingsApisActivity: Activity() {

    val appPreferences = CarStatsViewer.appPreferences

    lateinit var abrpIcon: ImageView

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                CarStatsViewer.liveDataApis[0].broadcastAction -> {
                    updateStatus(abrpIcon, CarStatsViewer.liveDataApis[0].connectionStatus)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus(abrpIcon, CarStatsViewer.liveDataApis[0].connectionStatus)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings_apis)

        abrpIcon = findViewById(R.id.abrp_connection)

        settings_apis_button_back.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        settings_apis_abrp_token.setOnClickListener {
            CarStatsViewer.liveDataApis[0].showSettingsDialog(this@SettingsApisActivity)
        }

        settings_apis_http_live_data.setOnClickListener {
            CarStatsViewer.liveDataApis[1].showSettingsDialog(this@SettingsApisActivity)
        }

        settings_apis_smtp_login.setOnClickListener {
            showSmtpLoginDialog()
        }

        registerReceiver(broadcastReceiver, IntentFilter(CarStatsViewer.liveDataApis[0].broadcastAction))
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

    private fun updateStatus(icon: ImageView, status: LiveDataApi.ConnectionStatus) {
        when (status) {
            LiveDataApi.ConnectionStatus.CONNECTED -> {
                icon.setColorFilter(getColor(R.color.connected_blue))
                icon.alpha = 1f
            }
            LiveDataApi.ConnectionStatus.ERROR -> {
                icon.setColorFilter(getColor(R.color.bad_red))
                icon.alpha = 1f
            }
            else -> {
                icon.setColorFilter(Color.WHITE)
                icon.alpha = CarStatsViewer.disabledAlpha
            }
        }
    }
}