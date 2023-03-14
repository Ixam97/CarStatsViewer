package com.ixam97.carStatsViewer.activities

import android.app.Activity
import android.os.Bundle
import com.ixam97.carStatsViewer.R
import kotlinx.android.synthetic.main.activity_settings_vehicle.*

class SettingsVehicleActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings_vehicle)

        settings_vehicle_button_back.setOnClickListener {
            finish()
        }
    }
}