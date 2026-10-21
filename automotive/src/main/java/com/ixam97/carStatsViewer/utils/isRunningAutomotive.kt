package com.ixam97.carStatsViewer.utils

import android.content.Context
import android.content.pm.PackageManager

fun isRunningOnAAOS(context: Context): Boolean {
    val packageManager: PackageManager = context.packageManager
    return packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)
}