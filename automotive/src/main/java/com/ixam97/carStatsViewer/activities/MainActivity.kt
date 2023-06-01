package com.ixam97.carStatsViewer.activities

import android.app.AlertDialog
import android.app.PendingIntent
import android.car.Car
import android.car.VehicleGear
import android.car.VehiclePropertyIds
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ixam97.carStatsViewer.*
import com.ixam97.carStatsViewer.appPreferences.AppPreferences
import com.ixam97.carStatsViewer.dataManager.*
import com.ixam97.carStatsViewer.fragments.SummaryFragment
import com.ixam97.carStatsViewer.liveData.LiveDataApi
import com.ixam97.carStatsViewer.plot.enums.*
import com.ixam97.carStatsViewer.plot.graphics.PlotLinePaint
import com.ixam97.carStatsViewer.plot.graphics.PlotPaint
import com.ixam97.carStatsViewer.plot.objects.PlotGlobalConfiguration
import com.ixam97.carStatsViewer.utils.InAppLogger
import com.ixam97.carStatsViewer.utils.StringFormatters
import com.ixam97.carStatsViewer.views.GageView
import com.ixam97.carStatsViewer.views.PlotView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class MainActivity : FragmentActivity(), SummaryFragment.OnSelectedTripChangedListener {
    companion object {
        private const val UI_UPDATE_INTERVAL = 1000L
        const val DISTANCE_TRIP_DIVIDER = 5_000L
        const val CONSUMPTION_DISTANCE_RESTRICTION = 10_000L
    }

    /** values and variables */
    private lateinit var appPreferences: AppPreferences
    private lateinit var consumptionPlotLinePaint : PlotLinePaint
    private lateinit var chargePlotLinePaint : PlotLinePaint

    private lateinit var timerHandler: Handler
    private lateinit var context: Context

    private var selectedDataManager = DataManagers.CURRENT_TRIP.dataManager

    private var updateUi = false
    private var lastPlotUpdate: Long = 0L

    private val updateActivityTask = object : Runnable {
        override fun run() {
            updateActivity()
            if (updateUi) timerHandler.postDelayed(this, UI_UPDATE_INTERVAL)
        }
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                getString(R.string.ui_update_plot_broadcast) -> updatePlots()
                getString(R.string.ui_update_gages_broadcast) -> updateGages()
                CarStatsViewer.liveDataApis[0].broadcastAction -> updateAbrpStatus(LiveDataApi.ConnectionStatus.fromInt(intent.getIntExtra("status", 0)))
                CarStatsViewer.liveDataApis[1].broadcastAction -> updateAbrpStatus(LiveDataApi.ConnectionStatus.fromInt(intent.getIntExtra("status", 0)))
            }
        }
    }

    override fun onSelectedTripChanged() {
        onResume()
    }

    /** Overrides */

    override fun onResume() {
        super.onResume()

        updateAbrpStatus(CarStatsViewer.liveDataApis[0].connectionStatus)

        val preferenceDataManager = DataManagers.values()[appPreferences.mainViewTrip].dataManager
        if (selectedDataManager != preferenceDataManager) {
            // Delay refresh for 400ms to ensure transition animation has finished
            CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
                delay(400)
                runOnUiThread {
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    finish()
                    overridePendingTransition(0, 0)
                }
            }
        }

        PlotGlobalConfiguration.updateDistanceUnit(appPreferences.distanceUnit)

        main_consumption_plot.dimensionRestriction = appPreferences.distanceUnit.asUnit(CONSUMPTION_DISTANCE_RESTRICTION)

        for (manager in DataManagers.values()) {
            manager.dataManager.consumptionPlotLine.Configuration.UnitFactor = appPreferences.distanceUnit.toFactor()

            if (appPreferences.consumptionUnit) {
                manager.dataManager.consumptionPlotLine.Configuration.Unit = "Wh/%s".format(appPreferences.distanceUnit.unit())
                manager.dataManager.consumptionPlotLine.Configuration.LabelFormat = PlotLineLabelFormat.NUMBER
                manager.dataManager.consumptionPlotLine.Configuration.Divider = appPreferences.distanceUnit.toFactor() * 1f
            } else {
                manager.dataManager.consumptionPlotLine.Configuration.Unit = "kWh/100%s".format(appPreferences.distanceUnit.unit())
                manager.dataManager.consumptionPlotLine.Configuration.LabelFormat = PlotLineLabelFormat.FLOAT
                manager.dataManager.consumptionPlotLine.Configuration.Divider = appPreferences.distanceUnit.toFactor() * 10f
            }
        }

        if (appPreferences.bstEdition) {
            main_power_gage.maxValue = 350f
            main_power_gage.minValue = -175f
        } else if (appPreferences.driveTrain == 2) {
            main_power_gage.maxValue = 300f
            main_power_gage.minValue = -150f
        } else {
            main_power_gage.maxValue = 170f
            main_power_gage.minValue = -100f
        }

        main_button_secondary_dimension.text = when (appPreferences.secondaryConsumptionDimension) {
            1 -> getString(R.string.main_secondary_axis, getString(R.string.main_speed))
            2 -> getString(R.string.main_secondary_axis, getString(R.string.main_SoC))
            3 -> getString(R.string.main_secondary_axis, getString(R.string.plot_dimensionY_ALTITUDE))
            else -> getString(R.string.main_secondary_axis, "-")
        }
        main_consumption_plot.dimensionYSecondary = PlotDimensionY.IndexMap[appPreferences.secondaryConsumptionDimension]


        main_power_gage.barVisibility = appPreferences.consumptionPlotVisibleGages
        main_consumption_gage.barVisibility = appPreferences.consumptionPlotVisibleGages
        main_SoC_gage.barVisibility = appPreferences.chargePlotVisibleGages
        main_charge_gage.barVisibility = appPreferences.chargePlotVisibleGages

        main_checkbox_speed.isChecked = appPreferences.plotSpeed

        main_consumption_plot.invalidate()

        enableUiUpdates()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val carStatsViewer = applicationContext as CarStatsViewer
                carStatsViewer.dataProcessor.realTimeDataFlow.collectLatest {
                    // Do stuff with live data
                    InAppLogger.v("State flow value: $it")
                }
            }
        }
         */

        startForegroundService(Intent(applicationContext, DataCollector::class.java))

        context = applicationContext
        val displayMetrics = context.resources.displayMetrics
        InAppLogger.d("Display size: ${displayMetrics.widthPixels/displayMetrics.density}x${displayMetrics.heightPixels/displayMetrics.density}")
        InAppLogger.d("Main view created")

        PlotView.textSize = resources.getDimension(R.dimen.reduced_font_size)
        PlotView.xMargin = resources.getDimension(R.dimen.plot_x_margin).toInt()
        PlotView.yMargin = resources.getDimension(R.dimen.plot_y_margin).toInt()
        GageView.valueTextSize = resources.getDimension(R.dimen.gage_value_text_size)
        GageView.descriptionTextSize = resources.getDimension(R.dimen.gage_desc_text_size)

        appPreferences = AppPreferences(context)

        consumptionPlotLinePaint = PlotLinePaint(
            PlotPaint.byColor(getColor(R.color.primary_plot_color), PlotView.textSize),
            PlotPaint.byColor(getColor(R.color.secondary_plot_color), PlotView.textSize),
            PlotPaint.byColor(getColor(R.color.secondary_plot_color_alt), PlotView.textSize)
        ) { appPreferences.consumptionPlotSecondaryColor }

        chargePlotLinePaint = PlotLinePaint(
            PlotPaint.byColor(getColor(R.color.charge_plot_color), PlotView.textSize),
            PlotPaint.byColor(getColor(R.color.secondary_plot_color), PlotView.textSize),
            PlotPaint.byColor(getColor(R.color.secondary_plot_color_alt), PlotView.textSize)
        ) { appPreferences.chargePlotSecondaryColor }

        selectedDataManager = DataManagers.values()[appPreferences.mainViewTrip].dataManager
        InAppLogger.i("selected Trip: ${selectedDataManager.printableName}")

        PlotGlobalConfiguration.updateDistanceUnit(appPreferences.distanceUnit)

        DataCollector.mainActivityPendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        setContentView(R.layout.activity_main)

        setupDefaultUi()
        setUiEventListeners()

        timerHandler = Handler(Looper.getMainLooper())

        registerReceiver(
            broadcastReceiver,
            IntentFilter(getString(R.string.ui_update_plot_broadcast))
        )
        registerReceiver(
            broadcastReceiver,
            IntentFilter(getString(R.string.ui_update_gages_broadcast))
        )
        registerReceiver(
            broadcastReceiver,
            IntentFilter(CarStatsViewer.liveDataApis[0].broadcastAction)
        )

        main_button_performance.isEnabled = false
        main_button_performance.colorFilter = PorterDuffColorFilter(getColor(R.color.disabled_tint), PorterDuff.Mode.SRC_IN)
        main_button_history.isEnabled = false
        main_button_history.colorFilter = PorterDuffColorFilter(getColor(R.color.disabled_tint), PorterDuff.Mode.SRC_IN)

        enableUiUpdates()

        if (appPreferences.versionString != BuildConfig.VERSION_NAME) {
            val changelogDialog = AlertDialog.Builder(this).apply {
                setPositiveButton(getString(R.string.dialog_close)) { dialog, _ ->
                    dialog.cancel()
                }
                setTitle(getString(R.string.main_changelog_dialog_title, BuildConfig.VERSION_NAME.dropLast(5)))
                val changesArray = resources.getStringArray(R.array.changes_0_24_1)
                var changelog = ""
                for ((index, change) in changesArray.withIndex()) {
                    changelog += "• $change"
                    if (index < changesArray.size - 1) changelog += "\n\n"
                }
                setMessage(changelog)
                setCancelable(true)
                create()
            }
            changelogDialog.show()
            appPreferences.versionString = BuildConfig.VERSION_NAME
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disableUiUpdates()
        unregisterReceiver(broadcastReceiver)
        InAppLogger.d("Main view destroyed")
    }

    override fun onPause() {
        super.onPause()
        disableUiUpdates()
    }

    /** Private functions */

    private fun updateActivity() {
        /** Use data from DataManager to Update MainActivity text */

        DataCollector.gagePowerValue = selectedDataManager.currentPower
        DataCollector.gageConsValue = ((selectedDataManager.currentPower / 1000)/(selectedDataManager.currentSpeed * 3.6f)).let {
            if (it.isFinite()) it
            else 0F
        }

        setUiVisibilities()
        updateGages()

        main_gage_avg_consumption_text_view.text = "  Ø %s".format(StringFormatters.getAvgConsumptionString(selectedDataManager.usedEnergy, selectedDataManager.traveledDistance))
        main_gage_distance_text_view.text = "  %s".format(StringFormatters.getTraveledDistanceString(selectedDataManager.traveledDistance))
        main_gage_used_power_text_view.text = "  %s".format(StringFormatters.getEnergyString(selectedDataManager.usedEnergy))
        main_gage_time_text_view.text = "  %s".format(StringFormatters.getElapsedTimeString(selectedDataManager.travelTime))
        main_gage_charged_energy_text_view.text = "  %s".format(StringFormatters.getEnergyString(DataManagers.CURRENT_TRIP.dataManager.chargedEnergy))
        main_gage_charge_time_text_view.text = "  %s".format(StringFormatters.getElapsedTimeString(DataManagers.CURRENT_TRIP.dataManager.chargeTime))
        main_gage_remaining_range_text_view.text = "  -/-  %s".format(appPreferences.distanceUnit.unit())
        main_gage_ambient_temperature_text_view.text = "  %s".format( StringFormatters.getTemperatureString(selectedDataManager.ambientTemperature))
    }

    private fun setUiVisibilities() {

        if (main_button_dismiss_charge_plot.isEnabled == selectedDataManager.chargePortConnected)
            main_button_dismiss_charge_plot.isEnabled = !selectedDataManager.chargePortConnected
        if (main_charge_layout.visibility == View.GONE && selectedDataManager.chargePortConnected) {
            main_consumption_layout.visibility = View.GONE
            main_charge_layout.visibility = View.VISIBLE
        }
    }

    private fun updateGages() {
        if ((selectedDataManager.currentPower / 1_000_000).absoluteValue >= 100 && true) { // Add Setting!
            main_power_gage.setValue((DataCollector.gagePowerValue / 1_000_000).toInt())
            main_charge_gage.setValue((-DataCollector.gagePowerValue / 1_000_000).toInt())
        } else {
            main_power_gage.setValue(DataCollector.gagePowerValue / 1_000_000)
            main_charge_gage.setValue(-DataCollector.gagePowerValue / 1_000_000)
        }
        main_SoC_gage.setValue(selectedDataManager.stateOfCharge)

        val nullValue: Float? = null

        if (appPreferences.consumptionUnit) {
            main_consumption_gage.gageUnit = "Wh/%s".format(appPreferences.distanceUnit.unit())
            main_consumption_gage.minValue = appPreferences.distanceUnit.asUnit(-300f)
            main_consumption_gage.maxValue = appPreferences.distanceUnit.asUnit(600f)

            if (selectedDataManager.currentSpeed * 3.6 > 3) {
                main_consumption_gage.setValue(appPreferences.distanceUnit.asUnit(DataCollector.gageConsValue).roundToInt())
            } else {
                main_consumption_gage.setValue(nullValue)
            }

        } else {
            main_consumption_gage.gageUnit = "kWh/100%s".format(appPreferences.distanceUnit.unit())
            main_consumption_gage.minValue = appPreferences.distanceUnit.asUnit(-30f)
            main_consumption_gage.maxValue = appPreferences.distanceUnit.asUnit(60f)

            if (selectedDataManager.currentSpeed * 3.6 > 3) {
                main_consumption_gage.setValue(appPreferences.distanceUnit.asUnit(DataCollector.gageConsValue) / 10)
            } else {
                main_consumption_gage.setValue(nullValue)
            }
        }
        main_consumption_gage.invalidate()
        main_power_gage.invalidate()
        main_charge_gage.invalidate()
        main_SoC_gage.invalidate()
    }

    private fun updatePlots(){
        main_charge_plot.dimensionRestriction = TimeUnit.MINUTES.toMillis((TimeUnit.MILLISECONDS.toMinutes(selectedDataManager.chargeTime) / 5) + 1) * 5 + 1

        if (SystemClock.elapsedRealtime() - lastPlotUpdate > 1_000L) {
            if (main_consumption_layout.visibility == View.VISIBLE) {
                main_consumption_plot.invalidate()
            }

            if (main_charge_layout.visibility == View.VISIBLE) {
                main_charge_plot.invalidate()
            }

            lastPlotUpdate = SystemClock.elapsedRealtime()
        }
    }

    private fun updateAbrpStatus(status: LiveDataApi.ConnectionStatus) {
        when (status) {
            LiveDataApi.ConnectionStatus.CONNECTED -> {
                main_icon_abrp_status.setColorFilter(Color.parseColor("#2595FF"))
                main_icon_abrp_status.visibility = View.VISIBLE
            }
            LiveDataApi.ConnectionStatus.ERROR -> {
                main_icon_abrp_status.setColorFilter(getColor(R.color.bad_red))
                main_icon_abrp_status.visibility = View.VISIBLE
            }
            else -> main_icon_abrp_status.visibility = View.GONE
        }
    }

    private fun setupDefaultUi() {

        PlotGlobalConfiguration.updateDistanceUnit(appPreferences.distanceUnit)

        main_consumption_plot.reset()
        main_consumption_plot.addPlotLine(selectedDataManager.consumptionPlotLine, consumptionPlotLinePaint)

        main_consumption_plot.dimension = PlotDimensionX.DISTANCE
        main_consumption_plot.dimensionRestriction = appPreferences.distanceUnit.asUnit(CONSUMPTION_DISTANCE_RESTRICTION)
        main_consumption_plot.dimensionSmoothing = 0.02f
        main_consumption_plot.dimensionSmoothingType = PlotDimensionSmoothingType.PERCENTAGE
        main_consumption_plot.sessionGapRendering = PlotSessionGapRendering.JOIN
        main_consumption_plot.dimensionYSecondary = PlotDimensionY.IndexMap[appPreferences.secondaryConsumptionDimension]

        main_consumption_plot.invalidate()

        main_charge_plot.reset()
        main_charge_plot.addPlotLine(DataManagers.CURRENT_TRIP.dataManager.chargePlotLine, chargePlotLinePaint)

        main_charge_plot.dimension = PlotDimensionX.TIME
        main_charge_plot.dimensionRestriction = null
        main_charge_plot.sessionGapRendering = PlotSessionGapRendering.GAP
        main_charge_plot.dimensionYSecondary = PlotDimensionY.STATE_OF_CHARGE
        main_charge_plot.invalidate()

        main_power_gage.gageName = getString(R.string.main_gage_power)
        main_power_gage.gageUnit = "kW"
        main_power_gage.primaryColor = getColor(R.color.polestar_orange)
        main_power_gage.maxValue = if (appPreferences.consumptionPlotSingleMotor) 170f else 300f
        main_power_gage.minValue = if (appPreferences.consumptionPlotSingleMotor) -100f else -150f
        main_power_gage.setValue(0f)

        main_consumption_gage.gageName = getString(R.string.main_gage_consumption)
        main_consumption_gage.gageUnit = "kWh/100km"
        main_consumption_gage.primaryColor = getColor(R.color.polestar_orange)
        main_consumption_gage.minValue = -30f
        main_consumption_gage.maxValue = 60f
        main_consumption_gage.setValue(0f)

        main_charge_gage.gageName = getString(R.string.main_gage_charging_power)
        main_charge_gage.gageUnit = "kW"
        main_charge_gage.primaryColor = getColor(R.color.charge_plot_color)
        main_charge_gage.minValue = 0f
        main_charge_gage.maxValue = 160f
        main_charge_gage.setValue(0f)

        main_SoC_gage.gageName = getString(R.string.main_gage_SoC)
        main_SoC_gage.gageUnit = "%"
        main_SoC_gage.primaryColor = getColor(R.color.charge_plot_color)
        main_SoC_gage.minValue = 0f
        main_SoC_gage.maxValue = 100f
        main_SoC_gage.setValue(0f)
    }

    private fun setUiEventListeners() {

        main_title.setOnClickListener {
            if (emulatorMode) {
                val gearSimulationIntent = Intent(getString(R.string.VHAL_emulator_broadcast)).apply {
                    putExtra(
                        EmulatorIntentExtras.PROPERTY_ID,
                        VehiclePropertyIds.GEAR_SELECTION
                    )
                    putExtra(EmulatorIntentExtras.TYPE, EmulatorIntentExtras.TYPE_INT)
                    if (selectedDataManager.currentGear == VehicleGear.GEAR_PARK) {
                        putExtra(
                            EmulatorIntentExtras.VALUE,
                            VehicleGear.GEAR_DRIVE
                        )
                        Toast.makeText(applicationContext, "Drive", Toast.LENGTH_SHORT).show()
                    }
                    else {
                        putExtra(EmulatorIntentExtras.VALUE, VehicleGear.GEAR_PARK)
                        Toast.makeText(applicationContext, "Park", Toast.LENGTH_SHORT).show()
                    }
                }
                sendBroadcast(gearSimulationIntent)
                sendBroadcast(Intent(getString(R.string.save_trip_data_broadcast)))
            }
        }
        main_title_icon.setOnClickListener {
            if (emulatorMode) {
                emulatorPowerSign = if (emulatorPowerSign < 0) 1
                else -1
                Toast.makeText(this, "Power sign: ${if(emulatorPowerSign<0) "-" else "+"}", Toast.LENGTH_SHORT).show()
            }
        }

        main_button_settings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.stay_still)
        }
        main_button_secondary_dimension.setOnClickListener {
            var currentIndex = appPreferences.secondaryConsumptionDimension
            currentIndex++
            if (currentIndex > 3) currentIndex = 0
            appPreferences.secondaryConsumptionDimension = currentIndex
            main_consumption_plot.dimensionYSecondary = when (appPreferences.secondaryConsumptionDimension) {
                1 -> {
                    main_button_secondary_dimension.text =
                        getString(R.string.main_secondary_axis, getString(R.string.main_speed))
                    PlotDimensionY.SPEED
                }
                2 -> {
                    main_button_secondary_dimension.text =
                        getString(R.string.main_secondary_axis, getString(R.string.main_SoC))
                    PlotDimensionY.STATE_OF_CHARGE
                }
                3 -> {
                    main_button_secondary_dimension.text =
                        getString(R.string.main_secondary_axis, getString(R.string.plot_dimensionY_ALTITUDE))
                    PlotDimensionY.ALTITUDE
                }
                else -> {
                    main_button_secondary_dimension.text =
                        getString(R.string.main_secondary_axis, "-")
                    null
                }
            }
        }
        /*
        main_dimension_y_secondary.entries = arrayListOf<String>().apply {
            PlotDimensionY.IndexMap.forEach {
                val id = resources.getIdentifier("plot_dimensionY_" + (it.value?.name?:"CONSUMPTION"), "string", packageName)
                add(
                    when (id != 0) {
                        true -> getString(id)
                        else -> (it.value?.name?:"-")
                    }
                )
            }
        }
        main_dimension_y_secondary.selectedIndex = appPreferences.secondaryConsumptionDimension
        main_dimension_y_secondary.setOnIndexChangedListener {
            appPreferences.secondaryConsumptionDimension = main_dimension_y_secondary.selectedIndex
            main_consumption_plot.dimensionYSecondary = PlotDimensionY.IndexMap[main_dimension_y_secondary.selectedIndex]
        }

         */

        main_button_summary.setOnClickListener {
            //val summaryIntent = Intent(this, SummaryActivity::class.java)
            //// summaryIntent.putExtra("dataManager", DataManagers.values().indexOf(DataManagers.CURRENT_TRIP))
            //summaryIntent.putExtra("dataManager", appPreferences.mainViewTrip)
            //startActivity(summaryIntent)
            //overridePendingTransition(R.anim.slide_in_up, R.anim.stay_still)
            main_fragment_container.visibility = View.VISIBLE
            supportFragmentManager.commit {
                setCustomAnimations(
                    R.anim.slide_in_up,
                    R.anim.stay_still,
                    R.anim.stay_still,
                    R.anim.slide_out_down
                )
                val summaryDataManager = DataManagers.values()[appPreferences.mainViewTrip].dataManager
                CarStatsViewer.dataManager = summaryDataManager
                CarStatsViewer.tripData = summaryDataManager.tripData
                add(R.id.main_fragment_container, SummaryFragment())
            }

        }

        main_button_summary_charge.setOnClickListener {
            val summaryIntent = Intent(this, SummaryActivity::class.java)
            // summaryIntent.putExtra("dataManager", DataManagers.values().indexOf(DataManagers.CURRENT_TRIP))
            summaryIntent.putExtra("dataManager", appPreferences.mainViewTrip)
            startActivity(summaryIntent)
            overridePendingTransition(R.anim.slide_in_up, R.anim.stay_still)
        }

        main_button_dismiss_charge_plot.setOnClickListener {
            main_charge_layout.visibility = View.GONE
            main_consumption_layout.visibility = View.VISIBLE
            main_consumption_plot.invalidate()
            // DataManager.chargedEnergy = 0f
            // DataManager.chargeTime = 0L
        }

        main_button_performance.setOnClickListener {
            throw IOException()
        }
    }

    private fun enableUiUpdates() {
        updateUi = true
        if (this::timerHandler.isInitialized) {
            timerHandler.removeCallbacks(updateActivityTask)
            timerHandler.post(updateActivityTask)
        }

    }

    private fun disableUiUpdates() {
        updateUi = false
        if (this::timerHandler.isInitialized) {
            timerHandler.removeCallbacks(updateActivityTask)
        }
    }

}
