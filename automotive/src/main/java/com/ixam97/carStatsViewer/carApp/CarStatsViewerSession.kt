package com.ixam97.carStatsViewer.carApp

import android.content.Intent
import android.content.pm.PackageManager
import androidx.annotation.OptIn
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.lifecycle.DefaultLifecycleObserver
import com.ixam97.carStatsViewer.carApp.renderer.Renderer
import com.ixam97.carStatsViewer.dataCollector.DataCollector
import com.ixam97.carStatsViewer.ui.activities.PermissionsActivity

@OptIn(ExperimentalCarApi::class)
class CarStatsViewerSession : Session(), DefaultLifecycleObserver {

    private val permissions = PermissionsActivity.PERMISSIONS.toList()

    override fun onCreateScreen(intent: Intent): Screen {

        val mLifecycle = lifecycle
        mLifecycle.addObserver(this)

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
        return CarStatsViewerScreen(carContext, this)
    }

    private fun startService() {
        carContext.startForegroundService(Intent(carContext, DataCollector::class.java))
    }
}