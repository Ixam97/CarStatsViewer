package com.coderax.carStatsViewer.utils

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children
import com.coderax.carStatsViewer.CarStatsViewer

fun applyTypeface(view: View?) {
    if (view == null) return
    try {
        if (view is ViewGroup) {
            view.children.forEach { child ->
                applyTypeface(child)
            }
        }
        if (view is TextView) {
            when (view.tag) {
                "bold" -> view.typeface = CarStatsViewer.typefaceMedium
                else -> view.typeface = CarStatsViewer.typefaceRegular
            }
            if (CarStatsViewer.isPolestarTypeface) view.letterSpacing = -0.025f
        }
    } catch (e: Exception) {
        InAppLogger.e(e.stackTraceToString())
    }
}