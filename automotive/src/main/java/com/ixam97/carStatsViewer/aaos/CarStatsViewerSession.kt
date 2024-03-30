package com.ixam97.carStatsViewer.aaos

import android.content.Intent
import android.content.pm.PackageManager
import androidx.car.app.Screen
import androidx.car.app.Session
import com.ixam97.carStatsViewer.dataCollector.DataCollector
import com.ixam97.carStatsViewer.ui.activities.PermissionsActivity

class CarStatsViewerSession : Session() {

    val permissions = PermissionsActivity.PERMISSIONS.toList()
    override fun onCreateScreen(intent: Intent): Screen {
        var neededPermissions = permissions.filter { carContext.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (neededPermissions.isNotEmpty()) {
            carContext.requestPermissions(permissions) {granted,_ ->
                if (granted.containsAll(permissions)) {
                    startService()
                }
            }
        } else {
            startService()
        }
        return CarStatsViewerScreen(carContext)
    }

    private fun startService() {
        carContext.startForegroundService(Intent(carContext, DataCollector::class.java))
    }
}