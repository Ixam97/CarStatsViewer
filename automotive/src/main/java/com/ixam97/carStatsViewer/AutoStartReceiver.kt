package com.ixam97.carStatsViewer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import com.ixam97.carStatsViewer.activities.PermissionsActivity
import com.ixam97.carStatsViewer.appPreferences.AppPreferences
import com.ixam97.carStatsViewer.dataManager.DataCollector

class AutoStartReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appPreferences = AppPreferences(context.applicationContext)
        if (appPreferences.notifications) {
            Toast.makeText(context, context.resources.getString(R.string.toast_started_in_background), Toast.LENGTH_LONG).show()
            if (PermissionsActivity.PERMISSIONS.filter { context.applicationContext.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }.isEmpty()) {
                context.applicationContext.startForegroundService(Intent(context.applicationContext, DataCollector::class.java))
                InAppLogger.log("Started in background")
            }
        }
    }
}