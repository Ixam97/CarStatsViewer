package com.ixam97.carStatsViewer.ui.activities

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.drawToBitmap
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ixam97.carStatsViewer.*
import com.ixam97.carStatsViewer.dataCollector.DataCollector
import com.ixam97.carStatsViewer.dataProcessor.DrivingState
import com.ixam97.carStatsViewer.database.tripData.TripType
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
import kotlinx.android.synthetic.main.activity_main.*
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
        PlotPaint.byColor(CarStatsViewer.appContext.getColor(R.color.primary_plot_color), PlotView.textSize),
        PlotPaint.byColor(CarStatsViewer.appContext.getColor(R.color.secondary_plot_color), PlotView.textSize),
        PlotPaint.byColor(CarStatsViewer.appContext.getColor(R.color.secondary_plot_color_alt), PlotView.textSize)
    ) { appPreferences.consumptionPlotSecondaryColor }

    private val chargePlotLine = PlotLine(
        PlotLineConfiguration(
            PlotRange(0f, 20f, 0f, 160f, 20f),
            PlotLineLabelFormat.FLOAT,
            PlotHighlightMethod.AVG_BY_TIME,
            "kW"
        )
    )
    private val chargePlotLinePaint = PlotLinePaint(
        PlotPaint.byColor(CarStatsViewer.appContext.getColor(R.color.charge_plot_color), PlotView.textSize),
        PlotPaint.byColor(CarStatsViewer.appContext.getColor(R.color.secondary_plot_color), PlotView.textSize),
        PlotPaint.byColor(CarStatsViewer.appContext.getColor(R.color.secondary_plot_color_alt), PlotView.textSize)
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

        CarStatsViewer.dataProcessor.changeSelectedTrip(appPreferences.mainViewTrip + 1)

        setTripTypeIcon(appPreferences.mainViewTrip + 1)

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
        main_soc_gage.minValue = 0f
        main_soc_gage.maxValue = 100f
        main_soc_gage.gageName = "State of charge"
        main_soc_gage.gageUnit = "%"

        setGageAndPlotUnits(appPreferences.consumptionUnit, appPreferences.distanceUnit)

        setGageLimits()

        setSecondaryConsumptionPlotDimension(appPreferences.secondaryConsumptionDimension)

        setGageVisibilities(appPreferences.consumptionPlotVisibleGages, appPreferences.consumptionPlotVisibleGages)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                withContext(Dispatchers.Default) {
                    Ticker.tickerFlow(1000).collectLatest {
                        CarStatsViewer.dataProcessor.updateTripDataValuesByTick()
                        runOnUiThread { updateActivity() }
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
                        if (instCons != null && it.speed!! * 3.6 > 3) {
                            if (appPreferences.consumptionUnit) {
                                main_consumption_gage.setValue(appPreferences.distanceUnit.asUnit(instCons).roundToInt())
                            } else {
                                main_consumption_gage.setValue(appPreferences.distanceUnit.asUnit(instCons) / 10)
                            }
                        } else {
                            main_consumption_gage.setValue(null as Float?)
                        }

                        main_power_gage.setValue(it.power!! / 1_000_000f)

                        main_charge_gage.setValue(it.power / -1_000_000f)
                        main_SoC_gage.setValue((it.stateOfCharge!! * 100f).roundToInt())

                        main_speed_gage.setValue(appPreferences.distanceUnit.toUnit(it.speed!!*3.6).toInt())
                        CarStatsViewer.dataProcessor.staticVehicleData.batteryCapacity?.let { batteryCapacity ->
                            main_soc_gage.setValue((it.batteryLevel!! / (batteryCapacity) * 100).roundToInt())
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
                            main_button_summary.isEnabled = false
                            main_button_history.isEnabled = false
                            main_button_history.setColorFilter(getColor(R.color.disabled_tint), PorterDuff.Mode.SRC_IN)
                        } else if (it.speed <= .1 && moving) {
                            moving = false
                            main_button_summary.isEnabled = true
                            main_button_history.isEnabled = true
                            main_button_history.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
                        }

                        setUiVisibilities()
                    }
                }
            }
        }


        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                CarStatsViewer.dataProcessor.currentChargingSessionDataFlow.collectLatest { chargingSession ->
                    // InAppLogger.i("Charging update")
                    chargingSession?.let {
                        neoChargedEnergy = it.charged_energy
                        neoChargeTime = it.chargeTime
                    }
                    chargingSession?.chargingPoints?.let { chargingPoints ->
                        var sizeDelta = chargingPoints.size - chargePlotLine.getDataPointsSize()

                        // InAppLogger.d("[NEO] Updating charging plot. Size delta: $sizeDelta")

                        chargePlotLine.reset()
                        chargePlotLine.addDataPoints(DataConverters.chargePlotLineFromChargingPoints(chargingPoints))
                        main_charge_plot.dimensionRestriction = TimeUnit.MINUTES.toMillis((TimeUnit.MILLISECONDS.toMinutes(chargingSession.chargeTime) / 5) + 1) * 5 + 1
                        main_charge_plot.invalidate()

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
                            main_charge_plot.dimensionRestriction = TimeUnit.MINUTES.toMillis((TimeUnit.MILLISECONDS.toMinutes(chargingSession.chargeTime) / 5) + 1) * 5 + 1
                            main_charge_plot.invalidate()
                        } else /*if (sizeDelta > 10 || sizeDelta < 0)*/ {
                            /** refresh entire plot for large numbers of new data Points */
                            chargePlotLine.reset()
                            chargePlotLine.addDataPoints(DataConverters.chargePlotLineFromChargingPoints(chargingPoints))
                            main_charge_plot.dimensionRestriction = TimeUnit.MINUTES.toMillis((TimeUnit.MILLISECONDS.toMinutes(chargingSession.chargeTime) / 5) + 1) * 5 + 1
                            main_charge_plot.invalidate()
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

                    neoDistance = session?.driven_distance?:0.0
                    neoEnergy = session?.used_energy?:0.0
                    neoTime = session?.drive_time?:0
                    neoUsedStateOfCharge = session?.used_soc?:0.0
                    neoUsedStateOfChargeEnergy = session?.used_soc_energy?:0.0

                    if (session?.drivingPoints == null || session.drivingPoints?.size == 0 || neoSelectedTripId != session.driving_session_id) {
                        consumptionPlotLine.reset()
                        main_consumption_plot.invalidate()
                        setTripTypeIcon(session?.session_type?:0)
                        updateActivity()
                    }

                    neoSelectedTripId = session?.driving_session_id

                    /** Add new plot points */

                    session?.drivingPoints?.let { drivingPoints ->
                        val startIndex = consumptionPlotLine.getDataPointsSize()
                        when {
                            startIndex == 0 && drivingPoints.isEmpty() -> {
                                consumptionPlotLine.reset()
                            }
                            startIndex == 0 -> {
                                consumptionPlotLine.reset()
                                consumptionPlotLine.addDataPoints(DataConverters.consumptionPlotLineFromDrivingPoints(drivingPoints, 10_000f))
                            }
                            startIndex != drivingPoints.size -> {
                                var prevDrivingPoint = consumptionPlotLine.lastItem()

                                for (i in drivingPoints.indices) {
                                    if (i < startIndex) continue

                                    prevDrivingPoint = consumptionPlotLine.addDataPoint(
                                        DataConverters.consumptionPlotLineItemFromDrivingPoint(
                                            drivingPoints[i],
                                            prevDrivingPoint
                                        )
                                    ) ?: prevDrivingPoint
                                }
                            }
                        }

                        main_consumption_plot.invalidate()
                    }

                    /*
                    session?.drivingPoints?.let { drivingPoints ->
                        var sizeDelta = drivingPoints.size - consumptionPlotLine.getDataPointsSize()
                        // InAppLogger.d("Size delta: $sizeDelta (${drivingPoints.size} vs. ${consumptionPlotLine.getDataPointsSize()}, $nonFiniteCounter non-finite)")
                        if (sizeDelta in 1..9) {
                            while (sizeDelta > 0) {
                                val prevDrivingPoint = if (consumptionPlotLine.getDataPointsSize() > 0) {
                                    consumptionPlotLine.getDataPoints(PlotDimensionX.DISTANCE).last()
                                } else null
                                consumptionPlotLine.addDataPoint(
                                    DataConverters.consumptionPlotLineItemFromDrivingPoint(
                                        drivingPoints[drivingPoints.size - sizeDelta],
                                        prevDrivingPoint
                                    )
                                )
                                sizeDelta --
                            }
                            main_consumption_plot.invalidate()
                        } else if (sizeDelta > 10) {
                            /** refresh entire plot for large numbers of new data Points */
                            consumptionPlotLine.reset()
                            consumptionPlotLine.addDataPoints(DataConverters.consumptionPlotLineFromDrivingPoints(drivingPoints, 10_000f))
                            main_consumption_plot.invalidate()
                        }
                    }

                     */
                    // updateActivity()
                }
            }
        }

        appPreferences.altLayout = false

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

        setContentView(R.layout.activity_main)

        CarStatsViewer.typefaceMedium?.let {
            applyTypeface(main_activity)
        }

        setupDefaultUi()
        setUiEventListeners()

        main_button_performance.isEnabled = true
        main_button_performance.setColorFilter(getColor(R.color.disabled_tint), PorterDuff.Mode.SRC_IN)

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
            TripType.MANUAL -> main_trip_type_icon.setImageDrawable(getDrawable(R.drawable.ic_hand))
            TripType.SINCE_CHARGE -> main_trip_type_icon.setImageDrawable(getDrawable(R.drawable.ic_charger_2))
            TripType.AUTO -> main_trip_type_icon.setImageDrawable(getDrawable(R.drawable.ic_day))
            TripType.MONTH -> main_trip_type_icon.setImageDrawable(getDrawable(R.drawable.ic_month))
            else -> main_trip_type_icon.setImageDrawable(null)
        }
        main_button_reset.visibility = if (tripType == TripType.MANUAL) {
            View.VISIBLE
        } else View.GONE
    }

    private fun setGageAndPlotUnits(consumptionUnit: Boolean, distanceUnit: DistanceUnitEnum) {
        if (consumptionUnit) {
            main_consumption_gage.gageUnit = "Wh/%s".format(distanceUnit.unit())
            main_consumption_gage.minValue = distanceUnit.asUnit(-300f)
            main_consumption_gage.maxValue = distanceUnit.asUnit(600f)
            consumptionPlotLine.Configuration.Unit = "Wh/%s".format(distanceUnit.unit())
            consumptionPlotLine.Configuration.LabelFormat = PlotLineLabelFormat.NUMBER
            consumptionPlotLine.Configuration.Divider = distanceUnit.toFactor() * 1f

        } else {
            main_consumption_gage.gageUnit = "kWh/100%s".format(distanceUnit.unit())
            main_consumption_gage.minValue = distanceUnit.asUnit(-30f)
            main_consumption_gage.maxValue = distanceUnit.asUnit(60f)
            consumptionPlotLine.Configuration.Unit = "kWh/100%s".format(distanceUnit.unit())
            consumptionPlotLine.Configuration.LabelFormat = PlotLineLabelFormat.FLOAT
            consumptionPlotLine.Configuration.Divider = distanceUnit.toFactor() * 10f
        }

        PlotGlobalConfiguration.updateDistanceUnit(distanceUnit)
        main_consumption_plot.dimensionRestriction = distanceUnit.asUnit(
            CONSUMPTION_DISTANCE_RESTRICTION
        )
        main_consumption_plot.invalidate()
    }

    private fun setGageLimits() {
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
    }

    fun setSecondaryConsumptionPlotDimension(secondaryConsumptionDimension: Int) {
        main_button_secondary_dimension.text = when (secondaryConsumptionDimension) {
            1 -> getString(R.string.main_secondary_axis, getString(R.string.main_speed))
            2 -> getString(R.string.main_secondary_axis, getString(R.string.main_SoC))
            3 -> getString(R.string.main_secondary_axis, getString(R.string.plot_dimensionY_ALTITUDE))
            else -> getString(R.string.main_secondary_axis, "-")
        }
        main_consumption_plot.dimensionYSecondary = PlotDimensionY.IndexMap[secondaryConsumptionDimension]
        main_consumption_plot.invalidate()
    }

    private fun setGageVisibilities(consumptionPlotVisibleGages: Boolean, chargePlotVisibleGages: Boolean) {
        main_power_gage.barVisibility = consumptionPlotVisibleGages
        main_consumption_gage.barVisibility = consumptionPlotVisibleGages
        main_SoC_gage.barVisibility = chargePlotVisibleGages
        main_charge_gage.barVisibility = chargePlotVisibleGages
    }

    private fun updateActivity() {

        setUiVisibilities()

        main_gage_avg_consumption_text_view.text = StringFormatters.getAvgConsumptionString(neoEnergy.toFloat(), neoDistance.toFloat())
        main_gage_distance_text_view.text = StringFormatters.getTraveledDistanceString(neoDistance.toFloat())
        main_gage_used_power_text_view.text = StringFormatters.getEnergyString(neoEnergy.toFloat())
        main_gage_avg_speed_text_view.text = StringFormatters.getAvgSpeedString(neoDistance.toFloat(), neoTime)
        main_gage_time_text_view.text = StringFormatters.getElapsedTimeString(neoTime)
        main_gage_charged_energy_text_view.text = StringFormatters.getEnergyString(neoChargedEnergy.toFloat())
        main_gage_charge_time_text_view.text = StringFormatters.getElapsedTimeString(neoChargeTime)
        // main_gage_ambient_temperature_text_view.text = "  %s".format( StringFormatters.getTemperatureString(selectedDataManager.ambientTemperature))

        // val usedEnergyPerSoC = neoUsedStateOfChargeEnergy / neoUsedStateOfCharge / 100
        // val currentStateOfCharge = CarStatsViewer.dataProcessor.realTimeData.stateOfCharge * 100
        // val remainingEnergy = usedEnergyPerSoC * currentStateOfCharge
        // val avgConsumption = neoEnergy / neoDistance * 1000
        // val remainingRange = (remainingEnergy / avgConsumption) * 1000

        // main_gage_remaining_range_text_view.text = "  %s (%.0f %% used)".format(StringFormatters.getRemainingRangeString(remainingRange.toFloat()), neoUsedStateOfCharge * 100)

    }


    private fun setUiVisibilities() {
        if (main_button_dismiss_charge_plot.isEnabled == neoChargePortConnected)
            main_button_dismiss_charge_plot.isEnabled = !neoChargePortConnected
        if (main_charge_layout.visibility == View.GONE && neoChargePortConnected) {
            main_consumption_layout.visibility = View.GONE
            main_charge_layout.visibility = View.VISIBLE
        } else if (CarStatsViewer.dataProcessor.realTimeData.drivingState == DrivingState.DRIVE && main_charge_layout.visibility == View.VISIBLE) {
            main_charge_layout.visibility = View.GONE
            main_consumption_layout.visibility = View.VISIBLE
        }
    }

    private fun updateConnectionStatusIcon(apiStatus: Map<String, Int>) {
        val selectedApi = appPreferences.mainViewConnectionApi
        if (apiStatus.containsKey(CarStatsViewer.liveDataApis[selectedApi].apiIdentifier)) {

            when (apiStatus[CarStatsViewer.liveDataApis[selectedApi].apiIdentifier]) {
                WatchdogState.DISABLED -> main_icon_abrp_status.visibility = View.GONE
                WatchdogState.NOMINAL -> {
                    main_icon_abrp_status.setColorFilter(getColor(R.color.connected_blue))
                    main_icon_abrp_status.visibility = View.VISIBLE
                }
                WatchdogState.ERROR -> {
                    main_icon_abrp_status.setColorFilter(getColor(R.color.bad_red))
                    main_icon_abrp_status.visibility = View.VISIBLE
                }
                WatchdogState.LIMITED -> {
                    main_icon_abrp_status.setColorFilter(getColor(R.color.limited_yellow))
                    main_icon_abrp_status.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun updateLocationStatusIcon(status: Int) {
        when(status) {
            WatchdogState.DISABLED -> main_icon_location_status.visibility = View.GONE
            WatchdogState.NOMINAL -> {
                main_icon_location_status.setImageDrawable(getDrawable(R.drawable.ic_location_on))
                main_icon_location_status.visibility = View.GONE
            }
            WatchdogState.ERROR -> {
                main_icon_location_status.setImageDrawable(getDrawable(R.drawable.ic_location_error))
                main_icon_location_status.visibility = View.VISIBLE
            }
        }
    }

    private fun setupDefaultUi() {

        PlotGlobalConfiguration.updateDistanceUnit(appPreferences.distanceUnit)

        main_consumption_plot.reset()
        main_consumption_plot.addPlotLine(consumptionPlotLine, consumptionPlotLinePaint)

        main_consumption_plot.dimension = PlotDimensionX.DISTANCE
        main_consumption_plot.dimensionRestriction = appPreferences.distanceUnit.asUnit(
            CONSUMPTION_DISTANCE_RESTRICTION
        )
        main_consumption_plot.dimensionSmoothing = 0.02f
        main_consumption_plot.dimensionSmoothingType = PlotDimensionSmoothingType.PERCENTAGE
        main_consumption_plot.sessionGapRendering = PlotSessionGapRendering.JOIN
        main_consumption_plot.dimensionYSecondary = PlotDimensionY.IndexMap[appPreferences.secondaryConsumptionDimension]

        main_consumption_plot.invalidate()

        main_charge_plot.reset()
        main_charge_plot.addPlotLine(chargePlotLine, chargePlotLinePaint)

        main_charge_plot.dimension = PlotDimensionX.TIME
        // main_charge_plot.dimensionRestriction = null
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

            setSecondaryConsumptionPlotDimension(currentIndex)
        }

        main_button_summary.setOnClickListener {
            openSummaryFragment()
        }

        main_button_summary_charge.setOnClickListener {
            openSummaryFragment()
        }

        main_button_dismiss_charge_plot.setOnClickListener {
            main_charge_layout.visibility = View.GONE
            main_consumption_layout.visibility = View.VISIBLE
            main_consumption_plot.invalidate()
            // DataManager.chargedEnergy = 0f
            // DataManager.chargeTime = 0L
        }

        main_button_performance.setOnClickListener {
            // throw Exception("Intentional crash")
            InAppLogger.i("Debug")
            lifecycleScope.launch {
                CarStatsViewer.screenshotBitmap = master_layout.drawToBitmap()
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Screenshot taken", Toast.LENGTH_SHORT).show()
                }
            }
        }

        main_button_history.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.stay_still)
        }

        main_button_reset.setOnClickListener {
            createResetDialog()
        }

        main_trip_type_icon.setOnClickListener {
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
                    main_fragment_container.visibility = View.VISIBLE
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
