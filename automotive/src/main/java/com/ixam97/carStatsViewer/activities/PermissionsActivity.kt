package com.ixam97.carStatsViewer.activities

import android.app.Activity
import android.app.AlertDialog
import android.car.Car
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import com.ixam97.carStatsViewer.InAppLogger
import com.ixam97.carStatsViewer.R
import kotlin.system.exitProcess

class PermissionsActivity: Activity() {
    companion object {
        private val PERMISSIONS = arrayOf(Car.PERMISSION_ENERGY, Car.PERMISSION_SPEED,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (checkPermissions()){
            finish()
            startActivity(Intent(applicationContext, MainActivity::class.java))
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        InAppLogger.log("onRequestPermissionResult")
        if(grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED)
        {
            finish()
            startActivity(Intent(applicationContext, MainActivity::class.java))
        } else {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.permissions_dialog_title))
                .setMessage(getString(R.string.permissions_dialog_text))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.permissions_dialog_grant)) { dialog, id ->
                    finish()
                    startActivity(intent)
                }
                .setNegativeButton(getString(R.string.permissions_dialog_quit)) { dialog, id ->
                    exitProcess(0)
                }
            val alert = builder.create()
            alert.show()
        }
    }

    private fun checkPermissions(): Boolean {
        InAppLogger.log("Checking permissions...")
        if (checkSelfPermission(Car.PERMISSION_ENERGY) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Car.PERMISSION_SPEED) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            InAppLogger.log("Requesting missing Permissions...")
            requestPermissions(PERMISSIONS, 0)
            return false
        }
        InAppLogger.log("Permissions already granted.")
        return true
    }
}