package com.ixam97.carStatsViewer.ui.activities

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ixam97.carStatsViewer.BuildConfig
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.compose.ComposeSettingsActivity
import com.ixam97.carStatsViewer.compose.ComposeTripDetailsActivity
import com.ixam97.carStatsViewer.dataCollector.DataCollector
import com.ixam97.carStatsViewer.dataProcessor.DrivingState
import com.ixam97.carStatsViewer.database.tripData.DrivingPoint
import com.ixam97.carStatsViewer.database.tripData.TripType
import com.ixam97.carStatsViewer.databinding.ActivityMainBinding
import com.ixam97.carStatsViewer.emulatorMode
import com.ixam97.carStatsViewer.emulatorPowerSign
import com.ixam97.carStatsViewer.ui.fragments.SummaryFragment
import com.ixam97.carStatsViewer.ui.plot.enums.PlotDimensionSmoothingType
import com.ixam97.carStatsViewer.ui.plot.enums.PlotDimensionX
import com.ixam97.carStatsViewer.ui.plot.enums.PlotDimensionY
import com.ixam97.carStatsViewer.ui.plot.enums.PlotHighlightMethod
import com.ixam97.carStatsViewer.ui.plot.enums.PlotLineLabelFormat
import com.ixam97.carStatsViewer.ui.plot.enums.PlotSessionGapRendering
import com.ixam97.carStatsViewer.ui.plot.graphics.PlotLinePaint
import com.ixam97.carStatsViewer.ui.plot.graphics.PlotPaint
import com.ixam97.carStatsViewer.ui.plot.objects.PlotGlobalConfiguration
import com.ixam97.carStatsViewer.ui.plot.objects.PlotLine
import com.ixam97.carStatsViewer.ui.plot.objects.PlotLineConfiguration
import com.ixam97.carStatsViewer.ui.plot.objects.PlotRange
import com.ixam97.carStatsViewer.utils.DataConverters
import com.ixam97.carStatsViewer.utils.DistanceUnitEnum
import com.ixam97.carStatsViewer.utils.InAppLogger
import com.ixam97.carStatsViewer.utils.StringFormatters
import com.ixam97.carStatsViewer.utils.Ticker
import com.ixam97.carStatsViewer.utils.WatchdogState
import com.ixam97.carStatsViewer.utils.getColorFromAttribute
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class MainActivity : FragmentActivity() {
    companion object {
        const val DISTANCE_TRIP_DIVIDER = 5_000L
        const val CONSUMPTION_DISTANCE_RESTRICTION = 10_000L
    }

    private var appliedTheme = 0

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
    private lateinit var consumptionPlotLinePaint: PlotLinePaint

    private val chargePlotLine = PlotLine(
        PlotLineConfiguration(
            PlotRange(0f, 20f, 0f, 400f, 20f),
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

        if (appliedTheme != appPreferences.colorTheme) {
            finish()
            startActivity(intent)
        }

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
        main_soc_gage.minValue = 0f
        main_soc_gage.maxValue = 100f
        main_soc_gage.gageName = "State of charge"
        main_soc_gage.gageUnit = "%"
        */

        setGageAndPlotUnits(appPreferences.consumptionUnit, appPreferences.distanceUnit)

        setGageLimits()

        setSecondaryConsumptionPlotDimension(appPreferences.secondaryConsumptionDimension)

        setGageVisibilities(appPreferences.consumptionPlotVisibleGages, appPreferences.consumptionPlotVisibleGages)

        binding.mainSecondaryDimensionIndicator.background = if (appPreferences.consumptionPlotSecondaryColor) {
            getColorFromAttribute(this, R.attr.tertiary_plot_color).toDrawable()
        } else {
            getColorFromAttribute(this, R.attr.secondary_plot_color).toDrawable()
        }

        setSnow(moving)

    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (CarStatsViewer.appPreferences.colorTheme > 0) setTheme(R.style.ColorTestTheme)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        val view = binding.root

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
                        with(binding){
                            if (instCons != null && it.speed!! * 3.6 > 3) {
                                if (appPreferences.consumptionUnit) {
                                    mainConsumptionGage.setValue(
                                        appPreferences.distanceUnit.asUnit(
                                            instCons
                                        ).roundToInt()
                                    )
                                } else {
                                    mainConsumptionGage.setValue(
                                        appPreferences.distanceUnit.asUnit(
                                            instCons
                                        ) / 10
                                    )
                                }
                            } else {
                                mainConsumptionGage.setValue(null as Float?)
                            }

                            mainPowerGage.setValue(it.power!! / 1_000_000f)

                            mainChargeGage.setValue(it.power / -1_000_000f)
                            mainSoCGage.setValue((it.stateOfCharge!! * 100f).roundToInt())

                            mainSpeedGage.setValue(
                                appPreferences.distanceUnit.toUnit(it.speed!! * 3.6).toInt()
                            )
                            CarStatsViewer.dataProcessor.staticVehicleData.batteryCapacity?.let { batteryCapacity ->
                                mainSocGage.setValue((it.batteryLevel!! / (batteryCapacity) * 100).roundToInt())
                            }

                            if (it.speed > .1 && !moving) {
                                setSnow(true)
                                moving = true
                                val summaryFragment =
                                    supportFragmentManager.findFragmentByTag("SummaryFragment")
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
                                // main_button_summary.isEnabled = false
                                mainImageButtonSummary.isEnabled = false
                                mainButtonHistory.isEnabled = false
                                mainButtonHistory.setColorFilter(
                                    getColor(R.color.disabled_tint),
                                    PorterDuff.Mode.SRC_IN
                                )
                                mainImageButtonSummary.setColorFilter(
                                    getColor(R.color.disabled_tint),
                                    PorterDuff.Mode.SRC_IN
                                )
                            } else if (it.speed <= .1 && moving) {
                                setSnow(false)
                                moving = false
                                // main_button_summary.isEnabled = true
                                mainImageButtonSummary.isEnabled = true
                                mainButtonHistory.isEnabled = true
                                mainButtonHistory.setColorFilter(
                                    Color.WHITE,
                                    PorterDuff.Mode.SRC_IN
                                )
                                mainImageButtonSummary.setColorFilter(
                                    Color.WHITE,
                                    PorterDuff.Mode.SRC_IN
                                )
                            }
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
                        with(binding){
                            var sizeDelta = chargingPoints.size - chargePlotLine.getDataPointsSize()

                            // InAppLogger.d("[NEO] Updating charging plot. Size delta: $sizeDelta")

                            chargePlotLine.reset()
                            chargePlotLine.addDataPoints(
                                DataConverters.chargePlotLineFromChargingPoints(
                                    chargingPoints
                                )
                            )
                            mainChargePlot.dimensionRestriction = TimeUnit.MINUTES.toMillis(
                                (TimeUnit.MILLISECONDS.toMinutes(chargingSession.chargeTime) / 5) + 1
                            ) * 5 + 1
                            mainChargePlot.invalidate()

                            if (sizeDelta in 1..9 && chargingPoints.last().point_marker_type == null) {
                                while (sizeDelta > 0) {
                                    val prevChargingPoint =
                                        if (chargePlotLine.getDataPointsSize() > 0) {
                                            chargePlotLine.getDataPoints(PlotDimensionX.TIME).last()
                                        } else null
                                    chargePlotLine.addDataPoint(
                                        DataConverters.chargePlotLineItemFromChargingPoint(
                                            chargingPoints[chargingPoints.size - sizeDelta],
                                            prevChargingPoint
                                        )
                                    )
                                    sizeDelta--
                                }
                                mainChargePlot.dimensionRestriction = TimeUnit.MINUTES.toMillis(
                                    (TimeUnit.MILLISECONDS.toMinutes(chargingSession.chargeTime) / 5) + 1
                                ) * 5 + 1
                                mainChargePlot.invalidate()
                            } else /*if (sizeDelta > 10 || sizeDelta < 0)*/ {
                                /** refresh entire plot for large numbers of new data Points */
                                chargePlotLine.reset()
                                chargePlotLine.addDataPoints(
                                    DataConverters.chargePlotLineFromChargingPoints(
                                        chargingPoints
                                    )
                                )
                                mainChargePlot.dimensionRestriction = TimeUnit.MINUTES.toMillis(
                                    (TimeUnit.MILLISECONDS.toMinutes(chargingSession.chargeTime) / 5) + 1
                                ) * 5 + 1
                                mainChargePlot.invalidate()
                            }
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
                    neoDistance = session?.driven_distance?:0.0
                    neoEnergy = session?.used_energy?:0.0
                    neoTime = session?.drive_time?:0
                    neoUsedStateOfCharge = session?.used_soc?:0.0
                    neoUsedStateOfChargeEnergy = session?.used_soc_energy?:0.0

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

                        // InAppLogger.d("startIndex = $startIndex, lastItemIndex = $lastItemIndex, drivingPoints.size = ${drivingPoints.size}, prevDrivingPoint?.Distance = ${prevDrivingPoint?.Distance}")

                        val selectedDistance = appPreferences.distanceUnit.asUnit(when (appPreferences.mainPrimaryDimensionRestriction) {
                            1 -> 40_000L
                            2 -> 100_000L
                            else -> 20_000L
                        })

                        when {
                            startIndex == 0 && drivingPoints.isEmpty() -> {
                                consumptionPlotLine.reset()
                            }
                            startIndex == 0
                            || (prevDrivingPoint?.Distance?:0f) > (selectedDistance * 2)
                            || lastItemIndex == null
                            || drivingPoints.size - lastItemIndex > 100 -> {
                                InAppLogger.v("Rebuilding consumption plot")
                                refreshConsumptionPlot(drivingPoints)
                            }
                            startIndex > 0 -> {
                                // if (lastItemIndex == null) lastItemIndex = drivingPoints.size
                                // InAppLogger.v("Last plot item index: $lastItemIndex, drivingPoints size: ${drivingPoints.size}")

                                while (lastItemIndex < drivingPoints.size - 1) {
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
        appliedTheme = appPreferences.colorTheme

        consumptionPlotLinePaint  = PlotLinePaint(
            PlotPaint.byColor(getColorFromAttribute(this, R.attr.primary_plot_color), CarStatsViewer.appContext.resources.getDimension(R.dimen.reduced_font_size)),
            PlotPaint.byColor(getColorFromAttribute(this, R.attr.secondary_plot_color), CarStatsViewer.appContext.resources.getDimension(R.dimen.reduced_font_size)),
            PlotPaint.byColor(getColorFromAttribute(this, R.attr.tertiary_plot_color), CarStatsViewer.appContext.resources.getDimension(R.dimen.reduced_font_size))
        ) { appPreferences.consumptionPlotSecondaryColor }

        setupDefaultUi()
        setUiEventListeners()

        with(binding){
            mainButtonPerf.isEnabled = false
            mainButtonPerf.setColorFilter(getColor(R.color.disabled_tint), PorterDuff.Mode.SRC_IN)

            if (appPreferences.versionString != BuildConfig.VERSION_NAME) {

                CarStatsViewer.getChangelogDialog(this@MainActivity).show()
                appPreferences.versionString = BuildConfig.VERSION_NAME
            }

            if (BuildConfig.FLAVOR_aaos == "carapp") {
                mainTitle.visibility = View.GONE
                mainTitleIcon.visibility = View.GONE
                mainTitleDashboard.visibility = View.VISIBLE
                mainButtonBack.visibility = View.VISIBLE
                mainButtonBack.setOnClickListener {
                    finish()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        InAppLogger.d("Main view destroyed")
    }

    /** Private functions */

    private fun setTripTypeIcon(tripType: Int) = with(binding) {
        when (tripType) {
            TripType.MANUAL -> {
                mainTripTypeIcon.setImageDrawable(getDrawable(R.drawable.ic_hand))
                mainGageTripName?.text = resources.getStringArray(R.array.trip_type_names)[1]
            }
            TripType.SINCE_CHARGE -> {
                mainTripTypeIcon.setImageDrawable(getDrawable(R.drawable.ic_charger))
                mainGageTripName.text = resources.getStringArray(R.array.trip_type_names)[2]
            }
            TripType.AUTO -> {
                mainTripTypeIcon.setImageDrawable(getDrawable(R.drawable.ic_day))
                mainGageTripName.text = resources.getStringArray(R.array.trip_type_names)[3]
            }
            TripType.MONTH -> {
                mainTripTypeIcon.setImageDrawable(getDrawable(R.drawable.ic_month))
                mainGageTripName.text = resources.getStringArray(R.array.trip_type_names)[4]
            }
            else -> mainTripTypeIcon.setImageDrawable(null)
        }
        mainButtonReset.visibility = if (tripType == TripType.MANUAL) {
            View.VISIBLE
        } else View.GONE
    }

    private fun setGageAndPlotUnits(consumptionUnit: Boolean, distanceUnit: DistanceUnitEnum) = with(binding) {
        if (consumptionUnit) {
            mainConsumptionGage.gageUnit = "Wh/%s".format(distanceUnit.unit())
            mainConsumptionGage.minValue = distanceUnit.asUnit(-300f)
            mainConsumptionGage.maxValue = distanceUnit.asUnit(600f)
            consumptionPlotLine.Configuration.Unit = "Wh/%s".format(distanceUnit.unit())
            consumptionPlotLine.Configuration.LabelFormat = PlotLineLabelFormat.NUMBER
            consumptionPlotLine.Configuration.Divider = distanceUnit.toFactor() * 1f

        } else {
            mainConsumptionGage.gageUnit = "kWh/100%s".format(distanceUnit.unit())
            mainConsumptionGage.minValue = distanceUnit.asUnit(-30f)
            mainConsumptionGage.maxValue = distanceUnit.asUnit(60f)
            consumptionPlotLine.Configuration.Unit = "kWh/100%s".format(distanceUnit.unit())
            consumptionPlotLine.Configuration.LabelFormat = PlotLineLabelFormat.FLOAT
            consumptionPlotLine.Configuration.Divider = distanceUnit.toFactor() * 10f
        }

        mainDistanceSelector.run {
            val unitString = appPreferences.distanceUnit.unit()
            buttonNames = listOf(
                "20 $unitString",
                "40 $unitString",
                "100 $unitString",
                "",
            )
        }

        PlotGlobalConfiguration.updateDistanceUnit(distanceUnit)
        // main_consumption_plot.dimensionRestriction = distanceUnit.asUnit(
        //     CONSUMPTION_DISTANCE_RESTRICTION
        // )
        setPrimaryConsumptionPlotDimension(appPreferences.mainPrimaryDimensionRestriction)
        mainConsumptionPlot.invalidate()
    }

    private fun setGageLimits() = with(binding) {
        mainPowerGage.minValue = -100f

        mainPowerGage.maxValue = when (appPreferences.performanceUpgrade) {
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

        mainChargeGage.maxValue = when (appPreferences.driveTrain) {
            0 -> 135f
            1, 2 -> {
                if (appPreferences.modelYear <= 2) 155f
                else 205f
            }
            else -> 155f
        }
    }

    fun setPrimaryConsumptionPlotDimension(index: Int) = with(binding) {
        InAppLogger.v("Changing primary plot restriction.")
        val newDistance = when (index) {
            1 -> 40_000L
            2 -> 100_000L
            else -> 20_000L
        }
        mainConsumptionPlot.dimensionRestriction = appPreferences.distanceUnit.asUnit(newDistance)
        refreshConsumptionPlot()
    }
    fun setSecondaryConsumptionPlotDimension(secondaryConsumptionDimension: Int) = with(binding) {

        val constraintSet = ConstraintSet()
        constraintSet.clone(mainSecondarySelectorContainer)

        when (secondaryConsumptionDimension) {
            1 -> {
                // main_button_secondary_dimension.text = getString(R.string.main_secondary_axis, getString(R.string.main_speed))
                constraintSet.connect(R.id.main_secondary_dimension_indicator, ConstraintSet.LEFT, R.id.main_image_button_speed, ConstraintSet.LEFT)
                constraintSet.connect(R.id.main_secondary_dimension_indicator, ConstraintSet.RIGHT, R.id.main_image_button_speed, ConstraintSet.RIGHT)
                mainSecondaryDimensionIndicator.isVisible = true
            }
            2 -> {
                // main_button_secondary_dimension.text = getString(R.string.main_secondary_axis, getString(R.string.main_SoC))
                constraintSet.connect(R.id.main_secondary_dimension_indicator, ConstraintSet.LEFT, R.id.main_image_button_soc, ConstraintSet.LEFT)
                constraintSet.connect(R.id.main_secondary_dimension_indicator, ConstraintSet.RIGHT, R.id.main_image_button_soc, ConstraintSet.RIGHT)
                mainSecondaryDimensionIndicator.isVisible = true
            }
            3 -> {
                // main_button_secondary_dimension.text = getString(R.string.main_secondary_axis, getString(R.string.plot_dimensionY_ALTITUDE))
                constraintSet.connect(R.id.main_secondary_dimension_indicator, ConstraintSet.LEFT, R.id.main_image_button_alt, ConstraintSet.LEFT)
                constraintSet.connect(R.id.main_secondary_dimension_indicator, ConstraintSet.RIGHT, R.id.main_image_button_alt, ConstraintSet.RIGHT)
                mainSecondaryDimensionIndicator.isVisible = true
            }
            else -> {
                // main_button_secondary_dimension.text = getString(R.string.main_secondary_axis, "-")
                constraintSet.connect(R.id.main_secondary_dimension_indicator, ConstraintSet.LEFT, R.id.main_image_button_speed, ConstraintSet.RIGHT)
                constraintSet.connect(R.id.main_secondary_dimension_indicator, ConstraintSet.RIGHT, R.id.main_image_button_speed, ConstraintSet.RIGHT)
                mainSecondaryDimensionIndicator.visibility = View.GONE
            }
        }
        constraintSet.applyTo(mainSecondarySelectorContainer)
        mainConsumptionPlot.dimensionYSecondary = PlotDimensionY.IndexMap[secondaryConsumptionDimension]
        mainConsumptionPlot.invalidate()
    }

    private fun setGageVisibilities(consumptionPlotVisibleGages: Boolean, chargePlotVisibleGages: Boolean) = with(binding) {
        mainPowerGage.barVisibility = consumptionPlotVisibleGages
        mainConsumptionGage.barVisibility = consumptionPlotVisibleGages
        mainSoCGage.barVisibility = chargePlotVisibleGages
        mainChargeGage.barVisibility = chargePlotVisibleGages
    }

    private fun updateActivity() = with(binding) {

        setUiVisibilities()

        mainGageAvgConsumptionTextView.text = StringFormatters.getAvgConsumptionString(neoEnergy.toFloat(), neoDistance.toFloat())
        mainGageDistanceTextView.text = StringFormatters.getTraveledDistanceString(neoDistance.toFloat())
        mainGageUsedPowerTextView.text = StringFormatters.getEnergyString(neoEnergy.toFloat())
        mainGageAvgSpeedTextView.text = StringFormatters.getAvgSpeedString(neoDistance.toFloat(), neoTime)
        mainGageTimeTextView.text = StringFormatters.getElapsedTimeString(neoTime)
        mainGageChargedEnergyTextView.text = StringFormatters.getEnergyString(neoChargedEnergy.toFloat())
        mainGageChargeTimeTextView.text = StringFormatters.getElapsedTimeString(neoChargeTime)
        // main_gage_ambient_temperature_text_view.text = "  %s".format( StringFormatters.getTemperatureString(selectedDataManager.ambientTemperature))

        // val usedEnergyPerSoC = neoUsedStateOfChargeEnergy / neoUsedStateOfCharge / 100
        // val currentStateOfCharge = CarStatsViewer.dataProcessor.realTimeData.stateOfCharge * 100
        // val remainingEnergy = usedEnergyPerSoC * currentStateOfCharge
        // val avgConsumption = neoEnergy / neoDistance * 1000
        // val remainingRange = (remainingEnergy / avgConsumption) * 1000

        // main_gage_remaining_range_text_view.text = "  %s (%.0f %% used)".format(StringFormatters.getRemainingRangeString(remainingRange.toFloat()), neoUsedStateOfCharge * 100)

    }


    private fun setUiVisibilities() = with(binding) {
        if (mainButtonDismissChargePlot.isEnabled == neoChargePortConnected)
            mainButtonDismissChargePlot.isEnabled = !neoChargePortConnected
        if (mainChargeLayout.visibility == View.GONE && neoChargePortConnected) {
            // mainConsumptionLayout.visibility = View.GONE
            // mainChargeLayout.visibility = View.VISIBLE
            crossfade(mainConsumptionLayout, mainChargeLayout)
        } else if (CarStatsViewer.dataProcessor.realTimeData.drivingState == DrivingState.DRIVE && mainChargeLayout.visibility == View.VISIBLE) {
            // mainChargeLayout.visibility = View.GONE
            // mainConsumptionLayout.visibility = View.VISIBLE
            crossfade(mainChargeLayout, mainConsumptionLayout)
        }
    }

    private fun updateConnectionStatusIcon(apiStatus: Map<String, Int>) = with(binding) {
        val selectedApi = appPreferences.mainViewConnectionApi
        if (apiStatus.containsKey(CarStatsViewer.liveDataApis[selectedApi].apiIdentifier)) {

            when (apiStatus[CarStatsViewer.liveDataApis[selectedApi].apiIdentifier]) {
                WatchdogState.DISABLED -> mainIconAbrpStatus.visibility = View.GONE
                WatchdogState.NOMINAL -> {
                    mainIconAbrpStatus.setColorFilter(getColor(R.color.connected_blue))
                    mainIconAbrpStatus.visibility = View.VISIBLE
                }
                WatchdogState.ERROR -> {
                    mainIconAbrpStatus.setColorFilter(getColor(R.color.bad_red))
                    mainIconAbrpStatus.visibility = View.VISIBLE
                }
                WatchdogState.LIMITED -> {
                    mainIconAbrpStatus.setColorFilter(getColor(R.color.limited_yellow))
                    mainIconAbrpStatus.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun updateLocationStatusIcon(status: Int) = with(binding) {
        when(status) {
            WatchdogState.DISABLED -> mainIconLocationStatus.visibility = View.GONE
            WatchdogState.NOMINAL -> {
                mainIconLocationStatus.setImageDrawable(getDrawable(R.drawable.ic_location_on))
                mainIconLocationStatus.visibility = View.GONE
            }
            WatchdogState.ERROR -> {
                mainIconLocationStatus.setImageDrawable(getDrawable(R.drawable.ic_location_error))
                mainIconLocationStatus.visibility = View.VISIBLE
            }
        }
    }

    private fun setupDefaultUi() = with(binding) {

        PlotGlobalConfiguration.updateDistanceUnit(appPreferences.distanceUnit)

        mainConsumptionPlot.reset()
        mainConsumptionPlot.addPlotLine(consumptionPlotLine, consumptionPlotLinePaint)

        mainConsumptionPlot.dimension = PlotDimensionX.DISTANCE
        // mainConsumptionPlot.dimensionRestriction = appPreferences.distanceUnit.asUnit(
        //     CONSUMPTION_DISTANCE_RESTRICTION
        // )
        setPrimaryConsumptionPlotDimension(appPreferences.mainPrimaryDimensionRestriction)

        mainDistanceSelector.run {
            selectedIndex = appPreferences.mainPrimaryDimensionRestriction
                .coerceAtMost(2)
                .coerceAtLeast(0)
        }
        mainConsumptionPlot.dimensionSmoothing = 0.02f
        mainConsumptionPlot.dimensionSmoothingType = PlotDimensionSmoothingType.PERCENTAGE
        mainConsumptionPlot.sessionGapRendering = PlotSessionGapRendering.JOIN
        mainConsumptionPlot.dimensionYSecondary = PlotDimensionY.IndexMap[appPreferences.secondaryConsumptionDimension]

        mainConsumptionPlot.invalidate()

        mainChargePlot.reset()
        mainChargePlot.addPlotLine(chargePlotLine, chargePlotLinePaint)

        mainChargePlot.dimension = PlotDimensionX.TIME
        // mainChargePlot.dimensionRestriction = null
        mainChargePlot.sessionGapRendering = PlotSessionGapRendering.GAP
        mainChargePlot.dimensionYSecondary = PlotDimensionY.STATE_OF_CHARGE
        mainChargePlot.invalidate()

        mainPowerGage.gageName = getString(R.string.main_gage_power)
        mainPowerGage.gageUnit = "kW"
        mainPowerGage.primaryColor = getColor(R.color.polestar_orange)
        mainPowerGage.maxValue = if (appPreferences.consumptionPlotSingleMotor) 170f else 300f
        mainPowerGage.minValue = if (appPreferences.consumptionPlotSingleMotor) -100f else -150f
        mainPowerGage.setValue(0f)

        mainConsumptionGage.gageName = getString(R.string.main_gage_consumption)
        mainConsumptionGage.gageUnit = "kWh/100km"
        mainConsumptionGage.primaryColor = getColor(R.color.polestar_orange) // getColorFromAttribute(this@MainActivity, android.R.attr.colorControlActivated)
        mainConsumptionGage.minValue = -30f
        mainConsumptionGage.maxValue = 60f
        mainConsumptionGage.setValue(0f)

        mainChargeGage.gageName = getString(R.string.main_gage_charging_power)
        mainChargeGage.gageUnit = "kW"
        mainChargeGage.primaryColor = getColor(R.color.charge_plot_color)
        mainChargeGage.minValue = 0f
        mainChargeGage.maxValue = 160f
        mainChargeGage.setValue(0f)

        mainSoCGage.gageName = getString(R.string.main_gage_SoC)
        mainSoCGage.gageUnit = "%"
        mainSoCGage.primaryColor = getColor(R.color.charge_plot_color)
        mainSoCGage.minValue = 0f
        mainSoCGage.maxValue = 100f
        mainSoCGage.setValue(0f)
    }

    private fun setUiEventListeners() = with(binding) {

        fun toggleEmulatorPowerSign() {
            if (emulatorMode) {
                emulatorPowerSign = if (emulatorPowerSign < 0) 1
                else -1
                Toast.makeText(this@MainActivity, "Power sign: ${if(emulatorPowerSign<0) "-" else "+"}", Toast.LENGTH_SHORT).show()
            }
        }

        mainTitle.setOnClickListener {
            toggleEmulatorPowerSign()
        }

        mainTitleDashboard.setOnClickListener {
            toggleEmulatorPowerSign()
        }

        mainButtonSettings.setOnClickListener {
            // startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            startActivity(Intent(this@MainActivity, ComposeSettingsActivity::class.java))
            if (BuildConfig.FLAVOR_aaos != "carapp")
                overridePendingTransition(R.anim.slide_in_right, R.anim.stay_still)
        }
        /*
        main_button_secondary_dimension.setOnClickListener {
            var currentIndex = appPreferences.secondaryConsumptionDimension
            currentIndex++
            if (currentIndex > 3) currentIndex = 0
            appPreferences.secondaryConsumptionDimension = currentIndex

            setSecondaryConsumptionPlotDimension(currentIndex)
        }
        */

        mainImageButtonSpeed.setOnClickListener {
            var currentIndex = appPreferences.secondaryConsumptionDimension
            currentIndex = if (currentIndex == 1) 0 else 1
            setSecondaryConsumptionPlotDimension(currentIndex)
            appPreferences.secondaryConsumptionDimension = currentIndex
        }

        mainImageButtonSoc.setOnClickListener {
            var currentIndex = appPreferences.secondaryConsumptionDimension
            currentIndex = if (currentIndex == 2) 0 else 2
            setSecondaryConsumptionPlotDimension(currentIndex)
            appPreferences.secondaryConsumptionDimension = currentIndex
        }

        mainImageButtonAlt.setOnClickListener {
            var currentIndex = appPreferences.secondaryConsumptionDimension
            currentIndex = if (currentIndex == 3) 0 else 3
            setSecondaryConsumptionPlotDimension(currentIndex)
            appPreferences.secondaryConsumptionDimension = currentIndex
        }

        mainDistanceSelector.setOnIndexChangedListener {
            appPreferences.mainPrimaryDimensionRestriction = mainDistanceSelector.selectedIndex
            setPrimaryConsumptionPlotDimension(appPreferences.mainPrimaryDimensionRestriction)
        }

        /*
        mainButtonSummary.setOnClickListener {
            openSummaryFragment()
        }
        */

        mainImageButtonSummary.setOnClickListener {
            // openSummaryFragment()
            val sessionId = CarStatsViewer.dataProcessor.selectedSessionData?.driving_session_id

            if (sessionId != null) {
                val summaryIntent =
                    Intent(this@MainActivity, ComposeTripDetailsActivity::class.java)
                summaryIntent.putExtra("SessionId", sessionId)
                startActivity(summaryIntent)
            }
        }

        mainButtonSummaryCharge.setOnClickListener {
            // openSummaryFragment()

            val sessionId = CarStatsViewer.dataProcessor.selectedSessionData?.driving_session_id

            if (sessionId != null) {
                val summaryIntent =
                    Intent(this@MainActivity, ComposeTripDetailsActivity::class.java)
                summaryIntent.putExtra("SessionId", sessionId)
                startActivity(summaryIntent)
            }
        }

        mainButtonDismissChargePlot.setOnClickListener {
            // mainChargeLayout.visibility = View.GONE
            // mainConsumptionLayout.visibility = View.VISIBLE
            crossfade(mainChargeLayout, mainConsumptionLayout)
            mainConsumptionPlot.invalidate()
            // DataManager.chargedEnergy = 0f
            // DataManager.chargeTime = 0L
        }

        mainButtonHistory.setOnClickListener {
            startActivity(Intent(this@MainActivity, HistoryActivity::class.java))
            if (BuildConfig.FLAVOR_aaos != "carapp")
                overridePendingTransition(R.anim.slide_in_right, R.anim.stay_still)
        }

        mainButtonReset.setOnClickListener {
            createResetDialog()
        }

        mainTripTypeIcon.setOnClickListener {
            val newTripType = if (appPreferences.mainViewTrip >= 3) 0 else appPreferences.mainViewTrip + 1
            CarStatsViewer.dataProcessor.changeSelectedTrip(newTripType + 1)
            appPreferences.mainViewTrip = newTripType
        }
    }

    private fun openSummaryFragment() = with(binding) {
        CoroutineScope(Dispatchers.IO).launch {
            CarStatsViewer.tripDataSource.getActiveDrivingSessionsIdsMap()[appPreferences.mainViewTrip + 1]?.let {
                val session = CarStatsViewer.dataProcessor.selectedSessionDataFlow.value // CarStatsViewer.tripDataSource.getFullDrivingSession(it)
                if (session != null) runOnUiThread {
                    mainFragmentContainer.visibility = View.VISIBLE
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

    private fun refreshConsumptionPlot(drivingPoints: List<DrivingPoint> = emptyList()) {
        InAppLogger.d("Refreshing entire consumption plot.")
        var localDrivingPoints = drivingPoints
        if (drivingPoints.isEmpty()) {
            localDrivingPoints = CarStatsViewer.dataProcessor.selectedSessionDataFlow.value?.drivingPoints?: emptyList()
        }
        consumptionPlotLine.reset()
        if (localDrivingPoints.isEmpty()) return
        lifecycleScope.launch {
            val newDistance = when (appPreferences.mainPrimaryDimensionRestriction) {
                1 -> 40_000L
                2 -> 100_000L
                else -> 20_000L
            }
            val dataPoints = DataConverters.consumptionPlotLineFromDrivingPoints(localDrivingPoints, appPreferences.distanceUnit.asUnit(newDistance.toFloat()))
            runOnUiThread {
                consumptionPlotLine.addDataPoints(dataPoints)
            }
        }
    }

    private fun crossfade(fromView: View, toView: View) {
        toView.apply {
            alpha = 0f
            isVisible = true
            animate()
                .alpha(1f)
                .setDuration(200L)
                .setListener(null)
        }

        fromView.animate()
            .alpha(0f)
            .setDuration(200L)
            .setListener(object: AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    fromView.isVisible = false
                }
            })
    }

    private fun setSnow(override: Boolean = false) {
        val calendar = Calendar.getInstance()
        if (calendar.get(Calendar.MONTH) == Calendar.DECEMBER && !override) {
            when (calendar.get(Calendar.DAY_OF_MONTH)) {
                22, 23, 24, 25, 26 -> binding.snowflakes?.apply { visibility = View.VISIBLE }
                else -> binding.snowflakes?.apply { visibility = View.GONE }
            }
        } else {
            binding.snowflakes?.apply { visibility = View.GONE }
        }
    }
}
