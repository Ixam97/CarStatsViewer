package com.ixam97.carStatsViewer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.ixam97.carStatsViewer.activities.PermissionsActivity
import com.ixam97.carStatsViewer.appPreferences.AppPreferences
import com.ixam97.carStatsViewer.dataManager.DataCollector
import com.ixam97.carStatsViewer.utils.InAppLogger

class AutoStartReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appPreferences = AppPreferences(context.applicationContext)
        val serviceIntent = Intent(context.applicationContext, DataCollector::class.java)
        serviceIntent.putExtra(
            "reason",
            when (intent.action) {
                Intent.ACTION_BOOT_COMPLETED -> "reboot"
                Intent.ACTION_MY_PACKAGE_REPLACED -> "update"
                else -> ""
            }
        )
        if (PermissionsActivity.PERMISSIONS.none {
                context.applicationContext.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
            }) {
            context.applicationContext.startForegroundService(serviceIntent)
            InAppLogger.log("Started in background")
        }
    }
}