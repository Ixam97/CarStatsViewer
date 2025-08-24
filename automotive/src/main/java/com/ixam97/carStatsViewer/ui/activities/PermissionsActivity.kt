package com.ixam97.carStatsViewer.ui.activities

import android.app.Activity
import android.app.AlertDialog
import android.car.Car
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import com.ixam97.carStatsViewer.BuildConfig
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.databinding.ActivityPermissionsBinding
import com.ixam97.carStatsViewer.utils.InAppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

class PermissionsActivity: Activity() {
    companion object {
        val PERMISSIONS_BY_SDK = arrayOf<Pair<Int, String>>(
            Build.VERSION_CODES.BASE to Car.PERMISSION_ENERGY,
            Build.VERSION_CODES.BASE to Car.PERMISSION_SPEED,
            Build.VERSION_CODES.BASE to android.Manifest.permission.ACCESS_FINE_LOCATION,
            Build.VERSION_CODES.BASE to android.Manifest.permission.ACCESS_COARSE_LOCATION,
            Build.VERSION_CODES.BASE to android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Build.VERSION_CODES.TIRAMISU to android.Manifest.permission.POST_NOTIFICATIONS
        )
    }

    private lateinit var binding: ActivityPermissionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (CarStatsViewer.appPreferences.colorTheme > 0) setTheme(R.style.ColorTestTheme)
        binding = ActivityPermissionsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.permissionsVersion.text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})"

        CoroutineScope(Dispatchers.Default).launch {
            while (!CarStatsViewer.fontsLoaded) {
                // Wait for Fonts to be loaded
            }
            runOnUiThread {
                if (checkPermissions()){
                    if (checkSelfPermission(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        showBackgroundLocationPermissionDialog()
                    } else {
                        finish()
                        startActivity(Intent(applicationContext, MainActivity::class.java))
                    }
                } else {
                    showBasicPermissionsDialog()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        InAppLogger.d("onRequestPermissionResult -> ${unGrantedPermissions().toString()}")

        if (unGrantedPermissions().isEmpty() && checkSelfPermission(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            finish()
            startActivity(Intent(applicationContext, MainActivity::class.java))
        } else if (checkSelfPermission(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            showBackgroundLocationPermissionDialog()
        } else {
            showBasicPermissionsDialog()
        }
    }

    private fun checkPermissions(): Boolean {
        InAppLogger.i("Checking permissions...")
        val unGrantedPermissions = unGrantedPermissions()
        if (unGrantedPermissions.isNotEmpty()) {
            InAppLogger.i("Requesting missing Permissions...")
            return false
        }
        // if (checkSelfPermission(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        //     requestPermissions(arrayOf(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION), 0)
        //     return false
        // }
        InAppLogger.i("Permissions already granted.")
        return true
    }

    private fun unGrantedPermissions(): List<String> {
        return PERMISSIONS_BY_SDK.filter {
            it.first <= Build.VERSION.SDK_INT
                    && checkSelfPermission(it.second) != PackageManager.PERMISSION_GRANTED
                    && it.second != android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
        }.map { it.second }
    }

    private fun showBasicPermissionsDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.permissions_dialog_title))
            .setMessage(getString(R.string.permissions_dialog_text))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.permissions_dialog_grant)) { dialog, id ->
                val unGrantedPermissions = unGrantedPermissions()
                requestPermissions(unGrantedPermissions.toTypedArray(), 0)

            }
            .setNegativeButton(getString(R.string.permissions_dialog_quit)) { dialog, id ->
                exitProcess(0)
            }
        val alert = builder.create()
        alert.show()
    }

    private fun showBackgroundLocationPermissionDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.permissions_dialog_title))
            .setMessage(getString(R.string.permissions_dialog_background_location_text))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.permissions_dialog_grant_singular)) { dialog, id ->
                requestPermissions(arrayOf(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION), 0)
            }
            .setNegativeButton(getString(R.string.permissions_dialog_deny)) { dialog, id ->
                finish()
                startActivity(Intent(applicationContext, MainActivity::class.java))
            }
        val alert = builder.create()
        alert.show()
    }
}
























