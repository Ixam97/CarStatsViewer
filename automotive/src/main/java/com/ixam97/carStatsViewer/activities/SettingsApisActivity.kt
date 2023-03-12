package com.ixam97.carStatsViewer.activities

import android.app.Activity
import android.os.Bundle
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
    }

    private fun showSmtpLoginDialog() {

    }
}