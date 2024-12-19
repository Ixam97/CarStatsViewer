package com.ixam97.carStatsViewer.compose

import android.os.Build
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.compose.theme.ColorTheme

fun brandSelector(themeSetting: Int): String? = when (CarStatsViewer.appPreferences.colorTheme) {
    ColorTheme.OEM -> {
        if (Build.MODEL == "Polestar 2") Build.MODEL
        else if (CarStatsViewer.dataProcessor.staticVehicleData.modelName == "PS2") "Polestar 2"
        else Build.BRAND
    }
    ColorTheme.ORANGE -> "Orange"
    ColorTheme.BLUE -> "Blue"
    else -> null
}