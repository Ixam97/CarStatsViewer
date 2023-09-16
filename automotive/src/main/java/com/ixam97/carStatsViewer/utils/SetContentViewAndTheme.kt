package com.ixam97.carStatsViewer.utils

import android.app.Activity
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R

fun setContentViewAndTheme(context: Activity, resId: Int) {
    if (CarStatsViewer.theme) context.setTheme(R.style.ColorTestTheme)
    context.setContentView(resId)
}