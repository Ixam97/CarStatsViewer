package com.ixam97.carStatsViewer

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.TypedValue
import com.ixam97.carStatsViewer.activities.PermissionsActivity
import com.ixam97.carStatsViewer.appPreferences.AppPreferences
import com.ixam97.carStatsViewer.dataManager.DataCollector
import com.ixam97.carStatsViewer.liveData.LiveDataApi
import com.ixam97.carStatsViewer.liveData.abrpLiveData.AbrpLiveData
import com.ixam97.carStatsViewer.liveData.http.HttpLiveData
import kotlin.properties.Delegates


class CarStatsViewer : Application() {

    companion object {
        const val CHANNEL_ID = "TestChannel"
        lateinit var appContext: Context
        // lateinit var abrpLiveData: LiveDataApiInterface

        lateinit var liveDataApis: ArrayList<LiveDataApi>
        lateinit var appPreferences: AppPreferences
        var primaryColor by Delegates.notNull<Int>()
        var disabledAlpha by Delegates.notNull<Float>()
    }

    override fun onCreate() {
        super.onCreate()

        val typedValue = TypedValue()
        applicationContext.theme.resolveAttribute(android.R.attr.colorControlActivated, typedValue, true)
        primaryColor = typedValue.data

        applicationContext.theme.resolveAttribute(android.R.attr.disabledAlpha, typedValue, true)
        disabledAlpha = typedValue.float

        // StrictMode.setVmPolicy(
        //     VmPolicy.Builder(StrictMode.getVmPolicy())
        //         .detectLeakedClosableObjects()
        //         .build()
        // )

        appContext = applicationContext
        appPreferences = AppPreferences(applicationContext)

        val abrpApiKey = if (resources.getIdentifier("abrp_api_key", "string", applicationContext.packageName) != 0) {
            getString(resources.getIdentifier("abrp_api_key", "string", applicationContext.packageName))
        } else ""

        /*
        Add live data APIs here
         */
        liveDataApis = arrayListOf(
            AbrpLiveData(abrpApiKey),
            HttpLiveData()
        )
        // abrpLiveData = AbrpLiveData(abrpApiKey)

        createNotificationChannel()

        if (PermissionsActivity.PERMISSIONS.none {
                applicationContext.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
            }
        ) {
            startForegroundService(Intent(applicationContext, DataCollector::class.java))
        }
    }

    fun createNotificationChannel() {
        val name = "TestChannel"
        val descriptionText = "TestChannel"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}