package com.ixam97.carStatsViewer

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.fonts.SystemFonts
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.TextView
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ixam97.carStatsViewer.appPreferences.AppPreferences
import com.ixam97.carStatsViewer.dataProcessor.DataProcessor
import com.ixam97.carStatsViewer.database.log.LogDao
import com.ixam97.carStatsViewer.database.log.LogDatabase
import com.ixam97.carStatsViewer.database.tripData.*
import com.ixam97.carStatsViewer.liveDataApi.LiveDataApi
import com.ixam97.carStatsViewer.liveDataApi.abrpLiveData.AbrpLiveData
import com.ixam97.carStatsViewer.liveDataApi.http.HttpLiveData
import com.ixam97.carStatsViewer.ui.plot.graphics.PlotPaint
import com.ixam97.carStatsViewer.ui.views.MultiButtonWidget
import com.ixam97.carStatsViewer.utils.InAppLogger
import com.ixam97.carStatsViewer.utils.ScreenshotButton
import com.ixam97.carStatsViewer.utils.Watchdog
import com.ixam97.carStatsViewer.utils.applyTypeface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.properties.Delegates
import kotlin.system.exitProcess

var emulatorMode = false
var emulatorPowerSign = -1

object Defines {
    const val PLOT_ENERGY_INTERVAL = 100L
    const val AUTO_RESET_TIME = 14_400_000L // 4h
    const val PLOT_DISTANCE_INTERVAL = 100.0
}

class CarStatsViewer : Application() {

    companion object {
        const val RESTART_CHANNEL_ID = "RestartChannel"
        const val RESTART_NOTIFICATION_ID = 1
        const val FOREGROUND_CHANNEL_ID = "ForegroundChannel"
        const val FOREGROUND_NOTIFICATION_ID = 2

        var screenshotBitmap = arrayListOf<Bitmap>()

        var fontsLoaded = false

        lateinit var appContext: Context
        lateinit var liveDataApis: ArrayList<LiveDataApi>
        lateinit var appPreferences: AppPreferences
        lateinit var notificationManager: NotificationManager
        var primaryColor by Delegates.notNull<Int>()
        var disabledAlpha by Delegates.notNull<Float>()

        var foregroundServiceStarted = false
        var restartNotificationDismissed = false
        var restartNotificationShown = false
        var restartReason: String? = null

        lateinit var tripDatabase: TripDataDatabase
        lateinit var tripDataSource: LocalTripDataSource
        lateinit var dataProcessor: DataProcessor
        lateinit var watchdog: Watchdog

        lateinit var logDao: LogDao

        var typefaceRegular: Typeface? = null
        var typefaceMedium: Typeface? = null
        var isPolestarTypeface = false

        val appContextIsInitialized: Boolean get() = this::appContext.isInitialized

        fun setupRestartAlarm(context: Context, reason: String, delay: Long, cancel: Boolean = false, extendedLogging: Boolean = false) {
            val serviceIntent = Intent(context, AutoStartReceiver::class.java)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            serviceIntent.action = "com.ixam97.carStatsViewer.RestartAction"
            serviceIntent.putExtra("reason", reason)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                serviceIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            alarmManager.cancel(pendingIntent)
            if (cancel) return
            if (delay < 10_000) {
                alarmManager.set(
                    AlarmManager.RTC,
                    System.currentTimeMillis() + delay,
                    pendingIntent
                )
                InAppLogger.i("[ASR] Setup single shot alarm")
            } else {
                alarmManager.setRepeating(
                    AlarmManager.RTC,
                    System.currentTimeMillis() + delay,
                    delay,
                    pendingIntent
                )
                if (extendedLogging) InAppLogger.i("[ASR] Setup repeating alarm")
            }
        }

        fun getChangelogDialog(context: Context): AlertDialog.Builder {
            return AlertDialog.Builder(context).apply {
                setPositiveButton(context.getString(R.string.dialog_close)) { dialog, _ ->
                    dialog.cancel()
                }
                // setTitle(context.getString(R.string.main_changelog_dialog_title, BuildConfig.VERSION_NAME.dropLast(5)))

                val layout = LayoutInflater.from(context).inflate(R.layout.dialog_changelog, null)

                val changelog4Title = layout.findViewById<TextView>(R.id.changes_0_26_0_title)
                val changelog4TextView = layout.findViewById<TextView>(R.id.changes_0_26_0)
                val changelog3Title = layout.findViewById<TextView>(R.id.changes_0_25_2_title)
                val changelog3TextView = layout.findViewById<TextView>(R.id.changes_0_25_2)
                val changelog2Title = layout.findViewById<TextView>(R.id.changes_0_25_1_title)
                val changelog2TextView = layout.findViewById<TextView>(R.id.changes_0_25_1)
                val changelog1Title = layout.findViewById<TextView>(R.id.changes_0_25_0_title)
                val changelog1TextView = layout.findViewById<TextView>(R.id.changes_0_25_0)

                changelog4Title.text = context.getString(R.string.main_changelog_dialog_title, "0.26.0")
                changelog3Title.text = context.getString(R.string.main_changelog_dialog_title, "0.25.2")
                changelog2Title.text = context.getString(R.string.main_changelog_dialog_title, "0.25.1")
                changelog1Title.text = context.getString(R.string.main_changelog_dialog_title, "0.25.0")

                val changesArray4 = context.resources.getStringArray(R.array.changes_0_26_0)
                var changelog4 = ""
                changesArray4.forEachIndexed { index, change ->
                    changelog4 += "• $change"
                    if (index < changesArray4.size - 1) changelog4 += "\n\n"
                }

                val changesArray3 = context.resources.getStringArray(R.array.changes_0_25_2)
                var changelog3 = ""
                changesArray3.forEachIndexed { index, change ->
                    changelog3 += "• $change"
                    if (index < changesArray3.size - 1) changelog3 += "\n\n"
                }

                val changesArray2 = context.resources.getStringArray(R.array.changes_0_25_1)
                var changelog2 = ""
                changesArray2.forEachIndexed { index, change ->
                    changelog2 += "• $change"
                    if (index < changesArray2.size - 1) changelog2 += "\n\n"
                }

                val changesArray1 = context.resources.getStringArray(R.array.changes_0_25_0)
                var changelog1 = ""
                changesArray1.forEachIndexed { index, change ->
                    changelog1 += "• $change"
                    if (index < changesArray1.size - 1) changelog1 += "\n\n"
                }

                changelog4TextView.text = changelog4
                changelog3TextView.text = changelog3
                changelog2TextView.text = changelog2
                changelog1TextView.text = changelog1

                applyTypeface(layout)

                setView(layout)

                setCancelable(true)
                create()
            }
        }
    }



    override fun onCreate() {
        super.onCreate()

        appContext = applicationContext
        appPreferences = AppPreferences(applicationContext)
        watchdog = Watchdog()

        val logDatabase = Room.databaseBuilder(
            applicationContext,
            LogDatabase::class.java,
            "LogDatabase"
        ).build()
        logDao = logDatabase.logDao()

        Thread.setDefaultUncaughtExceptionHandler { t, e ->

            try {
                setupRestartAlarm(applicationContext, "crash", 2_000, extendedLogging = true)
                InAppLogger.i("setup crash alarm")
            } catch (e: Exception) {
                InAppLogger.e(e.stackTraceToString())
            }

            InAppLogger.e("[NEO] Car Stats Viewer has crashed!\n ${e.stackTraceToString()}")
            val crashTime = System.nanoTime()
            while (System.nanoTime() < crashTime + 500_000_000) {
                // Give the logger some time
            }
            InAppLogger.e("exit")
            exitProcess(0)
        }

        InAppLogger.i("${appContext.getString(R.string.app_name)} v${BuildConfig.VERSION_NAME} started")

        InAppLogger.d("Screen width: ${resources.configuration.screenWidthDp}dp")
/*
        CoroutineScope(Dispatchers.IO).launch {
            InAppLogger.i("Available OEM fonts:")

            val systemFonts = SystemFonts.getAvailableFonts()
            systemFonts.filter{ it.file?.name?.contains("volvo", true) == true }.forEach {
                InAppLogger.i("    ${it.file?.name}")
                when {
                    it.file?.name?.contains("light", true) == true -> typefaceRegular = Typeface.Builder(it.file!!).build()
                    it.file?.name?.contains("medium", true) == true -> typefaceMedium = Typeface.Builder(it.file!!).build()
                }
            }
            systemFonts.filter{ it.file?.name?.contains("polestar", true) == true }.forEach {
                InAppLogger.i("    ${it.file?.name}")
                isPolestarTypeface = true
                when {
                    it.file?.name?.contains("regular", true) == true -> typefaceRegular = Typeface.Builder(it.file!!).build()
                    it.file?.name?.contains("medium", true) == true -> typefaceMedium = Typeface.Builder(it.file!!).build()
                }
            }
            systemFonts.filter { it.file?.name?.contains("honda", true) == true }.forEach {
                InAppLogger.i("    ${it.file?.name}")
                when {
                    it.file?.name?.contains("regular", true) == true -> {
                        typefaceRegular = Typeface.Builder(it.file!!).build()
                        typefaceMedium = Typeface.Builder(it.file!!).build()
                    }
                }
            }


            MultiButtonWidget.isPolestar = isPolestarTypeface

            typefaceRegular?.let {
                PlotPaint.typeface = it
                PlotPaint.letterSpacing = -0.025f
            }

            fontsLoaded = true
        }

 */
        fontsLoaded = true
        MultiButtonWidget.isPolestar = true

        // while (!fontsLoaded) {
        //     // Wait for fonts to be loaded before initializing trip database
        // }



        val MIGRATION_5_6 = object: Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE DrivingSession ADD COLUMN last_edited_epoch_time INTEGER NOT NULL DEFAULT 0")
            }
        }

        tripDatabase = Room.databaseBuilder(
            applicationContext,
            TripDataDatabase::class.java,
            "TripDatabase"
        )
            //.fallbackToDestructiveMigration()
            .addMigrations(MIGRATION_5_6)
            .build()

        tripDataSource = LocalTripDataSource(tripDatabase.tripDao())

        CoroutineScope(Dispatchers.IO).launch {

            // tripDatabase.clearAllTables()

            val drivingSessionIds = tripDataSource.getActiveDrivingSessionsIdsMap()
            InAppLogger.d("Trip Database: $drivingSessionIds")
        }

        dataProcessor = DataProcessor()

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

        val abrpApiKey = if (resources.getIdentifier("abrp_api_key", "string", applicationContext.packageName) != 0) {
            getString(resources.getIdentifier("abrp_api_key", "string", applicationContext.packageName))
        } else ""

        liveDataApis = arrayListOf(
            AbrpLiveData(abrpApiKey),
            HttpLiveData()
        )

        notificationManager = createNotificationManager()

        registerActivityLifecycleCallbacks(object: ActivityLifecycleCallbacks{
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) {
                ScreenshotButton.init(activity)
            }
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })

    }

    private fun createNotificationManager(): NotificationManager {
        val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val restartChannel = NotificationChannel(
            RESTART_CHANNEL_ID,
            RESTART_CHANNEL_ID,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = RESTART_CHANNEL_ID
        }

        val foregroundChannel = NotificationChannel(
            FOREGROUND_CHANNEL_ID,
            FOREGROUND_CHANNEL_ID,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = FOREGROUND_CHANNEL_ID
        }

        notificationManager.createNotificationChannels(listOf(
            restartChannel,
            foregroundChannel
        ))
        return notificationManager
    }
}