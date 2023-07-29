package com.ixam97.carStatsViewer.ui.activities

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.drawToBitmap
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ixam97.carStatsViewer.*
import com.ixam97.carStatsViewer.dataCollector.DataCollector
import com.ixam97.carStatsViewer.dataProcessor.DrivingState
import com.ixam97.carStatsViewer.database.tripData.TripType
import com.ixam97.carStatsViewer.databinding.ActivityMainBinding
import com.ixam97.carStatsViewer.events.MainEvent
import com.ixam97.carStatsViewer.events.UiEvent
import com.ixam97.carStatsViewer.ui.fragments.SummaryFragment
import com.ixam97.carStatsViewer.ui.plot.graphics.PlotLinePaint
import com.ixam97.carStatsViewer.ui.plot.graphics.PlotPaint
import com.ixam97.carStatsViewer.ui.plot.objects.PlotGlobalConfiguration
import com.ixam97.carStatsViewer.ui.plot.objects.PlotLine
import com.ixam97.carStatsViewer.ui.plot.objects.PlotLineConfiguration
import com.ixam97.carStatsViewer.ui.plot.objects.PlotRange
import com.ixam97.carStatsViewer.ui.plot.enums.*
import com.ixam97.carStatsViewer.ui.views.GageView
import com.ixam97.carStatsViewer.ui.views.PlotView
import com.ixam97.carStatsViewer.utils.*
import com.ixam97.carStatsViewer.viewModels.MainRealTimeDataState
import com.ixam97.carStatsViewer.viewModels.MainTripDataState
import com.ixam97.carStatsViewer.viewModels.MainViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class MainActivity : FragmentActivity() {
    companion object {
        const val DISTANCE_TRIP_DIVIDER = 5_000L
        const val CONSUMPTION_DISTANCE_RESTRICTION = 10_000L
    }

    /** values and variables */
    private val appPreferences = CarStatsViewer.appPreferences

    private val consumptionPlotLine = PlotLine(
        PlotLineConfiguration(
            PlotRange(-200f, 600f, -200f, 600f, 100f, 0f),
            PlotLineLabelFormat.NUMBER,
            PlotHighlightMethod.AVG_BY_DISTANCE,
            "Wh/km"
        ),
    )
    private val consumptionPlotLinePaint  = PlotLinePaint(
        PlotPaint.byColor(CarStatsViewer.appContext.getColor(R.color.primary_plot_color), CarStatsViewer.appContext.resources.getDimension(R.dimen.reduced_font_size)),
        PlotPaint.byColor(CarStatsViewer.appContext.getColor(R.color.secondary_plot_color), CarStatsViewer.appContext.resources.getDimension(R.dimen.reduced_font_size)),
        PlotPaint.byColor(CarStatsViewer.appContext.getColor(R.color.secondary_plot_color_alt), CarStatsViewer.appContext.resources.getDimension(R.dimen.reduced_font_size))
    ) { appPreferences.consumptionPlotSecondaryColor }

    private val chargePlotLine = PlotLine(
        PlotLineConfiguration(
            PlotRange(0f, 20f, 0f, 240f, 20f),
            PlotLineLabelFormat.FLOAT,
            PlotHighlightMethod.AVG_BY_TIME,
            "kW"
        )
    )
    private val chargePlotLinePaint = PlotLinePaint(
        PlotPaint.byColor(CarStatsViewer.appContext.getColor(R.color.charge_plot_color), CarStatsViewer.appContext.resources.getDimension(R.dimen.reduced_font_size)),
        PlotPaint.byColor(CarStatsViewer.appContext.getColor(R.color.secondary_plot_color), CarStatsViewer.appContext.resources.getDimension(R.dimen.reduced_font_size)),
        PlotPaint.byColor(CarStatsViewer.appContext.getColor(R.color.secondary_plot_color_alt), CarStatsViewer.appContext.resources.getDimension(R.dimen.reduced_font_size))
    ) { appPreferences.chargePlotSecondaryColor }

    private lateinit var context: Context

    private var moving = false

    private var neoDistance: Double = 0.0
    private var neoEnergy: Double = 0.0
    private var neoTime: Long = 0
    private var neoSelectedTripId: Long? = null
    private var neoUsedStateOfCharge: Double = 0.0
    private var neoUsedStateOfChargeEnergy: Double = 0.0
    private var neoChargePortConnected: Boolean = false

    private var neoChargedEnergy: Double = 0.0
    private var neoChargeTime: Long = 0

    /**
     * Overrides
     */

    override fun onResume() {
        super.onResume()

        // CarStatsViewer.dataProcessor.changeSelectedTrip(appPreferences.mainViewTrip + 1)

        // setTripTypeIcon(appPreferences.mainViewTrip + 1)

        /*
        // Temporary
        if (appPreferences.altLayout) {
            main_gage_layout.visibility = View.GONE
            main_alternate_gage_layout.visibility = View.VISIBLE
        } else {
            main_gage_layout.visibility = View.VISIBLE
            main_alternate_gage_layout.visibility = View.GONE
        }

        // Temporary
        main_speed_gage.minValue = 0f
        main_speed_gage.maxValue = appPreferences.distanceUnit.toUnit(205f)
        main_speed_gage.gageUnit = "${appPreferences.distanceUnit.unit()}/h"
        main_speed_gage.gageName = "Speed"
        binding.mainSocGage.minValue = 0f
        binding.mainSocGage.maxValue = 100f
        binding.mainSocGage.gageName = "State of charge"
        binding.mainSocGage.gageUnit = "%"
        */

        setGageAndPlotUnits(appPreferences.consumptionUnit, appPreferences.distanceUnit)

        setGageLimits()

        setSecondaryConsumptionPlotDimension(appPreferences.secondaryConsumptionDimension)

        setGageVisibilities(appPreferences.consumptionPlotVisibleGages, appPreferences.consumptionPlotVisibleGages)
    }

    private lateinit var binding: ActivityMainBinding

    private fun openActivity(intent: Intent) {
        startActivity(intent)
        if (intent.hasExtra("animation")) {
            when (intent.getStringExtra("animation")) {
                "right" -> overridePendingTransition(R.anim.slide_in_right, R.anim.stay_still)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        val view = binding.root

        val viewModel: MainViewModel by viewModels()

        val mainTripDataObserver = Observer<MainTripDataState>{ mainTripDataState ->
            binding.mainGageDistanceTextView.text = mainTripDataState.distanceString
            binding.mainGageUsedPowerTextView.text = mainTripDataState.usedEnergyString
            binding.mainGageAvgConsumptionTextView.text = mainTripDataState.avgConsumptionString
            binding.mainGageTimeTextView.text = mainTripDataState.tripTimeString
            binding.mainGageAvgSpeedTextView.text = mainTripDataState.avgSpeedString
        }

        val realTimeDataObserver = Observer<MainRealTimeDataState>{ mainRealTimeDataState ->
            binding.mainPowerGage.setValue(mainRealTimeDataState.currentPower)
            binding.mainConsumptionGage.setValue(mainRealTimeDataState.currentConsumption)
        }

        viewModel.mainTripDataStateLiveData.observe(this, mainTripDataObserver)
        viewModel.mainRealTimeDataStateLiveData.observe(this, realTimeDataObserver)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEvent.collect { event ->
                    when (event) {
                        is UiEvent.startActivity -> {
                            openActivity(event.intent)
                        }
                        is UiEvent.popBackstack -> {

                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                CarStatsViewer.dataProcessor.realTimeDataFlow.collectLatest {

                    // InAppLogger.v("Real time data: $it")

                    if (it.isInitialized()) {
                        neoChargePortConnected = it.chargePortConnected!!

                        val instCons = it.instConsumption
                        // if (instCons != null && it.speed!! * 3.6 > 3) {
                        //     if (appPreferences.consumptionUnit) {
                        //         binding.mainConsumptionGage.setValue(appPreferences.distanceUnit.asUnit(instCons).roundToInt())
                        //     } else {
                        //         binding.mainConsumptionGage.setValue(appPreferences.distanceUnit.asUnit(instCons) / 10)
                        //     }
                        // } else {
                        //     binding.mainConsumptionGage.setValue(null as Float?)
                        // }

                        // binding.mainPowerGage.setValue(it.power!! / 1_000_000f)

                        binding.mainChargeGage.setValue(it.power!! / -1_000_000f)
                        binding.mainSocGage.setValue((it.stateOfCharge!! * 100f).roundToInt())

                        binding.mainSpeedGage.setValue(appPreferences.distanceUnit.toUnit(it.speed!!*3.6).toInt())
                        CarStatsViewer.dataProcessor.staticVehicleData.batteryCapacity?.let { batteryCapacity ->
                            binding.mainSocGage.setValue((it.batteryLevel!! / (batteryCapacity) * 100).roundToInt())
                        }

                        if (it.speed > .1 && !moving) {
                            moving = true
                            val summaryFragment = supportFragmentManager.findFragmentByTag("SummaryFragment")
                            if (summaryFragment != null) {
                                supportFragmentManager.commit {
                                    setCustomAnimations(
                                        R.anim.slide_in_up,
                                        R.anim.slide_out_down,
                                        R.anim.stay_still,
                                        R.anim.slide_out_down
                                    )
                                    remove(summaryFragment)
                                }
                            }
                            binding.mainButtonSummary.isEnabled = false
                            binding.mainButtonHistory.isEnabled = false
                            binding.mainButtonHistory.setColorFilter(getColor(R.color.disabled_tint), PorterDuff.Mode.SRC_IN)
                        } else if (it.speed <= .1 && moving) {
                            moving = false
                            binding.mainButtonSummary.isEnabled = true
                            binding.mainButtonHistory.isEnabled = true
                            binding.mainButtonHistory.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
                        }

                        setUiVisibilities()
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                CarStatsViewer.dataProcessor.currentChargingSessionDataFlow.collectLatest { chargingSession ->
                    // InAppLogger.i("########## Charging update ##########")
                    chargingSession?.let {
                        neoChargedEnergy = it.charged_energy
                        neoChargeTime = it.chargeTime
                    }
                    chargingSession?.chargingPoints?.let { chargingPoints ->
                        var sizeDelta = chargingPoints.size - chargePlotLine.getDataPointsSize()

                        // InAppLogger.d("[NEO] Updating charging plot. Size delta: $sizeDelta")

                        chargePlotLine.reset()
                        chargePlotLine.addDataPoints(DataConverters.chargePlotLineFromChargingPoints(chargingPoints))
                        binding.mainChargePlot.dimensionRestriction = TimeUnit.MINUTES.toMillis((TimeUnit.MILLISECONDS.toMinutes(chargingSession.chargeTime) / 5) + 1) * 5 + 1
                        binding.mainChargePlot.invalidate()

                        if (sizeDelta in 1..9 && chargingPoints.last().point_marker_type == null) {
                            while (sizeDelta > 0) {
                                val prevChargingPoint = if (chargePlotLine.getDataPointsSize() > 0) {
                                    chargePlotLine.getDataPoints(PlotDimensionX.TIME).last()
                                } else null
                                chargePlotLine.addDataPoint(
                                    DataConverters.chargePlotLineItemFromChargingPoint(
                                        chargingPoints[chargingPoints.size - sizeDelta],
                                        prevChargingPoint
                                    )
                                )
                                sizeDelta --
                            }
                            binding.mainChargePlot.dimensionRestriction = TimeUnit.MINUTES.toMillis((TimeUnit.MILLISECONDS.toMinutes(chargingSession.chargeTime) / 5) + 1) * 5 + 1
                            binding.mainChargePlot.invalidate()
                        } else /*if (sizeDelta > 10 || sizeDelta < 0)*/ {
                            /** refresh entire plot for large numbers of new data Points */
                            chargePlotLine.reset()
                            chargePlotLine.addDataPoints(DataConverters.chargePlotLineFromChargingPoints(chargingPoints))
                            binding.mainChargePlot.dimensionRestriction = TimeUnit.MINUTES.toMillis((TimeUnit.MILLISECONDS.toMinutes(chargingSession.chargeTime) / 5) + 1) * 5 + 1
                            binding.mainChargePlot.invalidate()
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                CarStatsViewer.watchdog.watchdogStateFlow.collectLatest {
                    InAppLogger.d("[Watchdog] State changed: $it}")
                    updateLocationStatusIcon(it.locationState)
                    updateConnectionStatusIcon(it.apiState)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                CarStatsViewer.dataProcessor.selectedSessionDataFlow.collectLatest { session ->
                    // InAppLogger.i("########## Session state changed ##########")
                    // neoDistance = session?.driven_distance?:0.0
                    // neoEnergy = session?.used_energy?:0.0
                    // neoTime = session?.drive_time?:0
                    // neoUsedStateOfCharge = session?.used_soc?:0.0
                    // neoUsedStateOfChargeEnergy = session?.used_soc_energy?:0.0

                    if (session?.drivingPoints == null || session.drivingPoints?.size == 0 || neoSelectedTripId != session.driving_session_id) {
                        consumptionPlotLine.reset()
                        binding.mainConsumptionPlot.invalidate()
                        setTripTypeIcon(session?.session_type?:0)
                        updateActivity()
                    }

                    neoSelectedTripId = session?.driving_session_id

                    /** Add new plot points */

                    session?.drivingPoints?.let { drivingPoints ->
                        val startIndex = consumptionPlotLine.getDataPointsSize()
                        var prevDrivingPoint = consumptionPlotLine.lastItem()
                        var lastItemIndex = drivingPoints.withIndex().find { it.value.driving_point_epoch_time == prevDrivingPoint?.EpochTime }?.index

                        when {
                            startIndex == 0 && drivingPoints.isEmpty() -> {
                                consumptionPlotLine.reset()
                            }
                            startIndex == 0
                            || (prevDrivingPoint?.Distance?:0f) > 20_000
                            || lastItemIndex == null
                            || drivingPoints.size - lastItemIndex > 100 -> {
                                InAppLogger.d("Refreshing entire consumption plot.")
                                consumptionPlotLine.reset()
                                lifecycleScope.launch {
                                    val dataPoints = DataConverters.consumptionPlotLineFromDrivingPoints(drivingPoints, 10_000f)
                                    runOnUiThread {
                                        consumptionPlotLine.addDataPoints(dataPoints)
                                    }
                                }
                            }
                            startIndex > 0 -> {
                                // if (lastItemIndex == null) lastItemIndex = drivingPoints.size
                                // InAppLogger.v("Last plot item index: $lastItemIndex, drivingPoints size: ${drivingPoints.size}")

                                while (lastItemIndex < drivingPoints.size - 1) {
                                    InAppLogger.i("Data point added to plot")
                                    prevDrivingPoint = consumptionPlotLine.addDataPoint(
                                        DataConverters.consumptionPlotLineItemFromDrivingPoint(
                                            drivingPoints[lastItemIndex + 1],
                                            prevDrivingPoint
                                        )
                                    ) ?: prevDrivingPoint
                                    lastItemIndex++
                                }
                            }
                        }
                        binding.mainConsumptionPlot.invalidate()
                    }
                }
            }
        }

        appPreferences.altLayout = false

        startForegroundService(Intent(applicationContext, DataCollector::class.java))

        context = applicationContext
        val displayMetrics = context.resources.displayMetrics
        InAppLogger.d("Display size: ${displayMetrics.widthPixels/displayMetrics.density}x${displayMetrics.heightPixels/displayMetrics.density}")
        InAppLogger.d("Main view created")


        // PlotView.textSize = resources.getDimension(R.dimen.reduced_font_size)
        // InAppLogger.i("Plot text size: ${PlotView.textSize}")
        // PlotView.xMargin = resources.getDimension(R.dimen.plot_x_margin).toInt()
        // PlotView.yMargin = resources.getDimension(R.dimen.plot_y_margin).toInt()
        // GageView.valueTextSize = resources.getDimension(R.dimen.gage_value_text_size)
        // GageView.descriptionTextSize = resources.getDimension(R.dimen.gage_desc_text_size)

        setContentView(view)

        CarStatsViewer.typefaceMedium?.let {
            applyTypeface(binding.mainActivity)
        }

        binding.mainButtonSettings.setOnClickListener { viewModel.onEvent(MainEvent.OnOpenSettings) }
        binding.mainButtonHistory.setOnClickListener {
            viewModel.onEvent(MainEvent.OnOpenHistory)
        }

        setupDefaultUi()
        setUiEventListeners()

        if (BuildConfig.FLAVOR != "dev") binding.mainButtonScreenshot.visibility = View.GONE

        binding.mainButtonPerf.isEnabled = false
        binding.mainButtonPerf.setColorFilter(getColor(R.color.disabled_tint), PorterDuff.Mode.SRC_IN)

        if (appPreferences.versionString != BuildConfig.VERSION_NAME) {

            CarStatsViewer.getChangelogDialog(this).show()
            appPreferences.versionString = BuildConfig.VERSION_NAME
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        InAppLogger.d("Main view destroyed")
    }

    /** Private functions */

    private fun setTripTypeIcon(tripType: Int) {
        when (tripType) {
            TripType.MANUAL -> binding.mainTripTypeIcon.setImageDrawable(getDrawable(R.drawable.ic_hand))
            TripType.SINCE_CHARGE -> binding.mainTripTypeIcon.setImageDrawable(getDrawable(R.drawable.ic_charger))
            TripType.AUTO -> binding.mainTripTypeIcon.setImageDrawable(getDrawable(R.drawable.ic_day))
            TripType.MONTH -> binding.mainTripTypeIcon.setImageDrawable(getDrawable(R.drawable.ic_month))
            else -> binding.mainTripTypeIcon.setImageDrawable(null)
        }
        binding.mainButtonReset.visibility = if (tripType == TripType.MANUAL) {
            View.VISIBLE
        } else View.GONE
    }

    private fun setGageAndPlotUnits(consumptionUnit: Boolean, distanceUnit: DistanceUnitEnum) {
        if (consumptionUnit) {
            binding.mainConsumptionGage.gageUnit = "Wh/%s".format(distanceUnit.unit())
            binding.mainConsumptionGage.minValue = distanceUnit.asUnit(-300f)
            binding.mainConsumptionGage.maxValue = distanceUnit.asUnit(600f)
            consumptionPlotLine.Configuration.Unit = "Wh/%s".format(distanceUnit.unit())
            consumptionPlotLine.Configuration.LabelFormat = PlotLineLabelFormat.NUMBER
            consumptionPlotLine.Configuration.Divider = distanceUnit.toFactor() * 1f

        } else {
            binding.mainConsumptionGage.gageUnit = "kWh/100%s".format(distanceUnit.unit())
            binding.mainConsumptionGage.minValue = distanceUnit.asUnit(-30f)
            binding.mainConsumptionGage.maxValue = distanceUnit.asUnit(60f)
            consumptionPlotLine.Configuration.Unit = "kWh/100%s".format(distanceUnit.unit())
            consumptionPlotLine.Configuration.LabelFormat = PlotLineLabelFormat.FLOAT
            consumptionPlotLine.Configuration.Divider = distanceUnit.toFactor() * 10f
        }

        PlotGlobalConfiguration.updateDistanceUnit(distanceUnit)
        binding.mainConsumptionPlot.dimensionRestriction = distanceUnit.asUnit(
            CONSUMPTION_DISTANCE_RESTRICTION
        )
        binding.mainConsumptionPlot.invalidate()
    }

    private fun setGageLimits() {
        binding.mainPowerGage.minValue = -100f

        binding.mainPowerGage.maxValue = when (appPreferences.performanceUpgrade) {
            true -> 350f
            false -> {
                when (appPreferences.driveTrain) {
                    0 -> {
                        if (appPreferences.modelYear <= 2) 150f
                        else 200f
                    }
                    1 -> {
                        if (appPreferences.modelYear <= 2) 170f
                        else 220f
                    }
                    2 -> {
                        if (appPreferences.modelYear <= 2) 300f
                        else 310f
                    }
                    else -> 300f
                }
            }
        }

        binding.mainChargeGage.maxValue = when (appPreferences.driveTrain) {
            0 -> 135f
            1, 2 -> {
                if (appPreferences.modelYear <= 2) 155f
                else 205f
            }
            else -> 155f
        }
    }

    fun setSecondaryConsumptionPlotDimension(secondaryConsumptionDimension: Int) {
        binding.mainButtonSecondaryDimension.text = when (secondaryConsumptionDimension) {
            1 -> getString(R.string.main_secondary_axis, getString(R.string.main_speed))
            2 -> getString(R.string.main_secondary_axis, getString(R.string.main_SoC))
            3 -> getString(R.string.main_secondary_axis, getString(R.string.plot_dimensionY_ALTITUDE))
            else -> getString(R.string.main_secondary_axis, "-")
        }
        binding.mainConsumptionPlot.dimensionYSecondary = PlotDimensionY.IndexMap[secondaryConsumptionDimension]
        binding.mainConsumptionPlot.invalidate()
    }

    private fun setGageVisibilities(consumptionPlotVisibleGages: Boolean, chargePlotVisibleGages: Boolean) {
        binding.mainPowerGage.barVisibility = consumptionPlotVisibleGages
        binding.mainConsumptionGage.barVisibility = consumptionPlotVisibleGages
        binding.mainSocGage.barVisibility = chargePlotVisibleGages
        binding.mainChargeGage.barVisibility = chargePlotVisibleGages
    }

    private fun updateActivity() {

        setUiVisibilities()

        // binding.mainGageAvgConsumptionTextView.text = StringFormatters.getAvgConsumptionString(neoEnergy.toFloat(), neoDistance.toFloat())
        // binding.mainGageDistanceTextView.text = StringFormatters.getTraveledDistanceString(neoDistance.toFloat())
        // binding.mainGageUsedPowerTextView.text = StringFormatters.getEnergyString(neoEnergy.toFloat())
        // binding.mainGageAvgSpeedTextView.text = StringFormatters.getAvgSpeedString(neoDistance.toFloat(), neoTime)
        // binding.mainGageTimeTextView.text = StringFormatters.getElapsedTimeString(neoTime)
        // binding.mainGageChargedEnergyTextView.text = StringFormatters.getEnergyString(neoChargedEnergy.toFloat())
        // binding.mainGageChargeTimeTextView.text = StringFormatters.getElapsedTimeString(neoChargeTime)
        // main_gage_ambient_temperature_text_view.text = "  %s".format( StringFormatters.getTemperatureString(selectedDataManager.ambientTemperature))

        // val usedEnergyPerSoC = neoUsedStateOfChargeEnergy / neoUsedStateOfCharge / 100
        // val currentStateOfCharge = CarStatsViewer.dataProcessor.realTimeData.stateOfCharge * 100
        // val remainingEnergy = usedEnergyPerSoC * currentStateOfCharge
        // val avgConsumption = neoEnergy / neoDistance * 1000
        // val remainingRange = (remainingEnergy / avgConsumption) * 1000

        // main_gage_remaining_range_text_view.text = "  %s (%.0f %% used)".format(StringFormatters.getRemainingRangeString(remainingRange.toFloat()), neoUsedStateOfCharge * 100)

    }


    private fun setUiVisibilities() {
        if (binding.mainButtonDismissChargePlot.isEnabled == neoChargePortConnected)
            binding.mainButtonDismissChargePlot.isEnabled = !neoChargePortConnected
        if (binding.mainChargeLayout.visibility == View.GONE && neoChargePortConnected) {
            binding.mainConsumptionLayout.visibility = View.GONE
            binding.mainChargeLayout.visibility = View.VISIBLE
        } else if (CarStatsViewer.dataProcessor.realTimeData.drivingState == DrivingState.DRIVE && binding.mainChargeLayout.visibility == View.VISIBLE) {
            binding.mainChargeLayout.visibility = View.GONE
            binding.mainConsumptionLayout.visibility = View.VISIBLE
        }
    }

    private fun updateConnectionStatusIcon(apiStatus: Map<String, Int>) {
        val selectedApi = appPreferences.mainViewConnectionApi
        if (apiStatus.containsKey(CarStatsViewer.liveDataApis[selectedApi].apiIdentifier)) {

            when (apiStatus[CarStatsViewer.liveDataApis[selectedApi].apiIdentifier]) {
                WatchdogState.DISABLED -> binding.mainIconAbrpStatus.visibility = View.GONE
                WatchdogState.NOMINAL -> {
                    binding.mainIconAbrpStatus.setColorFilter(getColor(R.color.connected_blue))
                    binding.mainIconAbrpStatus.visibility = View.VISIBLE
                }
                WatchdogState.ERROR -> {
                    binding.mainIconAbrpStatus.setColorFilter(getColor(R.color.bad_red))
                    binding.mainIconAbrpStatus.visibility = View.VISIBLE
                }
                WatchdogState.LIMITED -> {
                    binding.mainIconAbrpStatus.setColorFilter(getColor(R.color.limited_yellow))
                    binding.mainIconAbrpStatus.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun updateLocationStatusIcon(status: Int) {
        when(status) {
            WatchdogState.DISABLED -> binding.mainIconLocationStatus.visibility = View.GONE
            WatchdogState.NOMINAL -> {
                binding.mainIconLocationStatus.setImageDrawable(getDrawable(R.drawable.ic_location_on))
                binding.mainIconLocationStatus.visibility = View.GONE
            }
            WatchdogState.ERROR -> {
                binding.mainIconLocationStatus.setImageDrawable(getDrawable(R.drawable.ic_location_error))
                binding.mainIconLocationStatus.visibility = View.VISIBLE
            }
        }
    }

    private fun setupDefaultUi() {

        PlotGlobalConfiguration.updateDistanceUnit(appPreferences.distanceUnit)

        binding.mainConsumptionPlot.reset()
        binding.mainConsumptionPlot.addPlotLine(consumptionPlotLine, consumptionPlotLinePaint)

        binding.mainConsumptionPlot.dimension = PlotDimensionX.DISTANCE
        binding.mainConsumptionPlot.dimensionRestriction = appPreferences.distanceUnit.asUnit(
            CONSUMPTION_DISTANCE_RESTRICTION
        )
        binding.mainConsumptionPlot.dimensionSmoothing = 0.02f
        binding.mainConsumptionPlot.dimensionSmoothingType = PlotDimensionSmoothingType.PERCENTAGE
        binding.mainConsumptionPlot.sessionGapRendering = PlotSessionGapRendering.JOIN
        binding.mainConsumptionPlot.dimensionYSecondary = PlotDimensionY.IndexMap[appPreferences.secondaryConsumptionDimension]

        binding.mainConsumptionPlot.invalidate()

        binding.mainChargePlot.reset()
        binding.mainChargePlot.addPlotLine(chargePlotLine, chargePlotLinePaint)

        binding.mainChargePlot.dimension = PlotDimensionX.TIME
        // binding.mainChargePlot.dimensionRestriction = null
        binding.mainChargePlot.sessionGapRendering = PlotSessionGapRendering.GAP
        binding.mainChargePlot.dimensionYSecondary = PlotDimensionY.STATE_OF_CHARGE
        binding.mainChargePlot.invalidate()

        binding.mainPowerGage.gageName = getString(R.string.main_gage_power)
        binding.mainPowerGage.gageUnit = "kW"
        binding.mainPowerGage.primaryColor = getColor(R.color.polestar_orange)
        binding.mainPowerGage.maxValue = if (appPreferences.consumptionPlotSingleMotor) 170f else 300f
        binding.mainPowerGage.minValue = if (appPreferences.consumptionPlotSingleMotor) -100f else -150f
        binding.mainPowerGage.setValue(0f)

        binding.mainConsumptionGage.gageName = getString(R.string.main_gage_consumption)
        binding.mainConsumptionGage.gageUnit = "kWh/100km"
        binding.mainConsumptionGage.primaryColor = getColor(R.color.polestar_orange)
        binding.mainConsumptionGage.minValue = -30f
        binding.mainConsumptionGage.maxValue = 60f
        binding.mainConsumptionGage.setValue(0f)

        binding.mainChargeGage.gageName = getString(R.string.main_gage_charging_power)
        binding.mainChargeGage.gageUnit = "kW"
        binding.mainChargeGage.primaryColor = getColor(R.color.charge_plot_color)
        binding.mainChargeGage.minValue = 0f
        binding.mainChargeGage.maxValue = 160f
        binding.mainChargeGage.setValue(0f)

        binding.mainSocGage.gageName = getString(R.string.main_gage_SoC)
        binding.mainSocGage.gageUnit = "%"
        binding.mainSocGage.primaryColor = getColor(R.color.charge_plot_color)
        binding.mainSocGage.minValue = 0f
        binding.mainSocGage.maxValue = 100f
        binding.mainSocGage.setValue(0f)
    }

    private fun setUiEventListeners() {

        binding.mainTitleIcon.setOnClickListener {
            if (emulatorMode) {
                emulatorPowerSign = if (emulatorPowerSign < 0) 1
                else -1
                Toast.makeText(this, "Power sign: ${if(emulatorPowerSign<0) "-" else "+"}", Toast.LENGTH_SHORT).show()
            }
        }

        // binding.mainButtonSettings.setOnClickListener {
        //     startActivity(Intent(this, SettingsActivity::class.java))
        //     overridePendingTransition(R.anim.slide_in_right, R.anim.stay_still)
        // }
        binding.mainButtonSecondaryDimension.setOnClickListener {
            var currentIndex = appPreferences.secondaryConsumptionDimension
            currentIndex++
            if (currentIndex > 3) currentIndex = 0
            appPreferences.secondaryConsumptionDimension = currentIndex

            setSecondaryConsumptionPlotDimension(currentIndex)
        }

        binding.mainButtonSummary.setOnClickListener {
            openSummaryFragment()
        }

        binding.mainButtonSummaryCharge.setOnClickListener {
            openSummaryFragment()
        }

        binding.mainButtonDismissChargePlot.setOnClickListener {
            binding.mainChargeLayout.visibility = View.GONE
            binding.mainConsumptionLayout.visibility = View.VISIBLE
            binding.mainConsumptionPlot.invalidate()
            // DataManager.chargedEnergy = 0f
            // DataManager.chargeTime = 0L
        }

        binding.mainButtonScreenshot.setOnClickListener {
            // throw Exception("Intentional crash")
            InAppLogger.i("Debug")
            lifecycleScope.launch {
                CarStatsViewer.screenshotBitmap = binding.root.drawToBitmap()
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Screenshot taken", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // binding.mainButtonHistory.setOnClickListener {
        //     startActivity(Intent(this, HistoryActivity::class.java))
        //     overridePendingTransition(R.anim.slide_in_right, R.anim.stay_still)
        // }

        binding.mainButtonReset.setOnClickListener {
            createResetDialog()
        }

        binding.mainTripTypeIcon.setOnClickListener {
            val newTripType = if (appPreferences.mainViewTrip >= 3) 0 else appPreferences.mainViewTrip + 1
            CarStatsViewer.dataProcessor.changeSelectedTrip(newTripType + 1)
            appPreferences.mainViewTrip = newTripType
        }
    }

    private fun openSummaryFragment() {
        CoroutineScope(Dispatchers.IO).launch {
            CarStatsViewer.tripDataSource.getActiveDrivingSessionsIdsMap()[appPreferences.mainViewTrip + 1]?.let {
                val session = CarStatsViewer.dataProcessor.selectedSessionDataFlow.value // CarStatsViewer.tripDataSource.getFullDrivingSession(it)
                if (session != null) runOnUiThread {
                    binding.mainFragmentContainer.visibility = View.VISIBLE
                    supportFragmentManager.commit {
                        setCustomAnimations(
                            R.anim.slide_in_up,
                            R.anim.stay_still,
                            R.anim.stay_still,
                            R.anim.slide_out_down
                        )
                        add(R.id.main_fragment_container, SummaryFragment(session), "SummaryFragment")
                    }
                }
            }
        }
    }

    private fun createResetDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.dialog_reset_title))
            .setMessage(getString(R.string.dialog_reset_message))
            .setCancelable(true)
            .setPositiveButton(getString(R.string.dialog_reset_confirm)) { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    CarStatsViewer.dataProcessor.resetTrip(
                        TripType.MANUAL,
                        CarStatsViewer.dataProcessor.realTimeData.drivingState
                    )
                }
            }
            .setNegativeButton(getString(R.string.dialog_reset_cancel)) { dialog, _ ->
                dialog.cancel()
            }
        val alert = builder.create()
        alert.show()
    }
}
