package com.ixam97.carStatsViewer.utils

import android.app.Activity
import android.view.View
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R

fun setContentViewAndTheme(context: Activity, view: View) {
    if (CarStatsViewer.appPreferences.colorTheme > 0) context.setTheme(R.style.ColorTestTheme)
    context.setContentView(view)
}