package com.coderax.carStatsViewer.ui.activities

import android.app.Activity
import android.app.AlertDialog
import android.car.Car
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import com.coderax.carStatsViewer.utils.InAppLogger
import com.coderax.carStatsViewer.R
import kotlin.system.exitProcess

class PermissionsActivity: Activity() {
    companion object {
        val PERMISSIONS = arrayOf(
            Car.PERMISSION_ENERGY,
            Car.PERMISSION_SPEED,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)
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
        InAppLogger.d("onRequestPermissionResult")

        if (unGrantedPermissions().isEmpty()) {
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
        InAppLogger.i("Checking permissions...")
        val unGrantedPermissions = unGrantedPermissions()
        if (unGrantedPermissions.isNotEmpty()) {
            InAppLogger.i("Requesting missing Permissions...")
            requestPermissions(unGrantedPermissions.toTypedArray(), 0)
            return false
        }
        InAppLogger.i("Permissions already granted.")
        return true
    }

    private fun unGrantedPermissions(): List<String> {
        return PERMISSIONS.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
    }
}
























