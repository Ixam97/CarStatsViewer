package com.ixam97.carStatsViewer.fragments

import android.app.AlertDialog
import android.content.*
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColor
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.activities.MainActivity
import com.ixam97.carStatsViewer.dataManager.*
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.database.tripData.TripType
import com.ixam97.carStatsViewer.plot.enums.*
import com.ixam97.carStatsViewer.plot.graphics.PlotLinePaint
import com.ixam97.carStatsViewer.plot.graphics.PlotPaint
import com.ixam97.carStatsViewer.plot.objects.*
import com.ixam97.carStatsViewer.utils.InAppLogger
import com.ixam97.carStatsViewer.utils.StringFormatters
import com.ixam97.carStatsViewer.views.PlotView
import kotlinx.android.synthetic.main.fragment_summary.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

class SummaryFragment(val session: DrivingSession, var fragmentContainerId: Int) : Fragment(R.layout.fragment_summary) {

    val appPreferences = CarStatsViewer.appPreferences
    val applicationContext = CarStatsViewer.appContext
    private var primaryColor = CarStatsViewer.primaryColor

    private var chargePlotLine = PlotLine(
        PlotLineConfiguration(
            PlotRange(0f, 20f, 0f, 160f, 20f),
            PlotLineLabelFormat.FLOAT,
            PlotHighlightMethod.AVG_BY_TIME,
            "kW"
        )
    )

    private lateinit var consumptionPlotLine: PlotLine

    private lateinit var consumptionPlotLinePaint : PlotLinePaint
    private lateinit var chargePlotLinePaint : PlotLinePaint

    private val seekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            setVisibleChargeCurve(progress)
        }
        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            InAppLogger.d(intent.action?: "")
            when (intent.action) {
                getString(R.string.distraction_optimization_broadcast) -> {
                    updateDistractionOptimization()
                }
                else -> {}
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        consumptionPlotLinePaint = PlotLinePaint(
            PlotPaint.byColor(applicationContext.getColor(R.color.primary_plot_color), PlotView.textSize),
            PlotPaint.byColor(applicationContext.getColor(R.color.secondary_plot_color), PlotView.textSize),
            PlotPaint.byColor(applicationContext.getColor(R.color.secondary_plot_color_alt), PlotView.textSize)
        ) { appPreferences.consumptionPlotSecondaryColor }

        chargePlotLinePaint = PlotLinePaint(
            PlotPaint.byColor(applicationContext.getColor(R.color.charge_plot_color), PlotView.textSize),
            PlotPaint.byColor(applicationContext.getColor(R.color.secondary_plot_color), PlotView.textSize),
            PlotPaint.byColor(applicationContext.getColor(R.color.secondary_plot_color_alt), PlotView.textSize)
        ) { appPreferences.chargePlotSecondaryColor }

        // if (dataManager == null || (dataManager?.printableName != DataManagers.CURRENT_TRIP.dataManager.printableName)) {
        //     summary_button_reset.isEnabled = false
        //     summary_button_reset.setColorFilter(applicationContext.getColor(R.color.disabled_tint))
        // }

        if (session.session_type != TripType.MANUAL) {
            summary_button_reset.isEnabled = false
            summary_button_reset.colorFilter = PorterDuffColorFilter(applicationContext.getColor(R.color.disabled_tint), PorterDuff.Mode.SRC_IN)
        }

        // tripData = dataManager.tripData!!
        consumptionPlotLine = PlotLine(
            PlotLineConfiguration(
                PlotRange(-300f, 900f, -300f, 900f, 100f, 0f),
                PlotLineLabelFormat.NUMBER,
                PlotHighlightMethod.AVG_BY_DISTANCE,
                "Wh/km"
            ),
        )
        // consumptionPlotLine.addDataPoints(tripData.consumptionPlotLine)

        summary_selected_trip_bar[appPreferences.mainViewTrip].background = primaryColor.toColor().toDrawable()

        summary_button_trip_prev.setOnClickListener {
            var tripIndex = appPreferences.mainViewTrip
            tripIndex--
            if (tripIndex < 0) tripIndex = 3
            appPreferences.mainViewTrip = tripIndex
            refreshActivity(tripIndex)
        }

        summary_button_trip_next.setOnClickListener {
            var tripIndex = appPreferences.mainViewTrip
            tripIndex++
            if (tripIndex > 3) tripIndex = 0
            appPreferences.mainViewTrip = tripIndex
            refreshActivity(tripIndex)
        }

        summary_title.setOnClickListener {
                var tripIndex = appPreferences.mainViewTrip
                tripIndex++
                if (tripIndex > 3) tripIndex = 0
                appPreferences.mainViewTrip = tripIndex
                refreshActivity(tripIndex)
        }

        if ((session.end_epoch_time?:0) > 0 ) {
            summary_title.text = "${StringFormatters.getDateString(Date(session.start_epoch_time))}, ${resources.getStringArray(R.array.trip_type_names)[session.session_type]}"
        } else {
            summary_title.visibility = View.GONE
            summary_trip_selector.visibility = View.VISIBLE
            summary_selector_title.text = resources.getStringArray(R.array.trip_type_names)[session.session_type]
        }

        // dataManager?.let {
        //     summary_selector_title.text = getString(resources.getIdentifier(
        //         it.printableName,
        //         "string", applicationContext.packageName
        //     ))
        //     if(it.printableName != DataManagers.CURRENT_TRIP.dataManager.printableName) {
        //         summary_button_reset.isEnabled = false;
        //         summary_button_reset.alpha = CarStatsViewer.disabledAlpha
        //     }
        // }

        summary_button_reset.setOnClickListener {
            createResetDialog()
        }

        summary_trip_date_text.text = getString(R.string.summary_trip_start_date).format(
            StringFormatters.getDateString(Date(session.start_epoch_time)))

        summary_button_show_consumption_container.isSelected = true

        updateDistractionOptimization()

        summary_button_show_consumption_container.setOnClickListener {
            switchToConsumptionLayout()
        }

        summary_button_show_charge_container.setOnClickListener {
            switchToChargeLayout()
        }

        setupConsumptionLayout()
        setupChargeLayout()

        applicationContext.registerReceiver(broadcastReceiver, IntentFilter(getString(R.string.distraction_optimization_broadcast)))

        summary_button_back.setOnClickListener {
            requireActivity().supportFragmentManager.commit {
                setCustomAnimations(
                    R.anim.slide_in_up,
                    R.anim.slide_out_down,
                    R.anim.stay_still,
                    R.anim.slide_out_down
                )
                remove(this@SummaryFragment)
            }
            // mListener?.onSelectedTripChanged()
        }
    }

    override fun onDetach() {
        super.onDetach()
        applicationContext.unregisterReceiver(broadcastReceiver)
    }

    private fun setupConsumptionLayout() {
        val plotMarkers = PlotMarkers()
        // plotMarkers.addMarkers(tripData.markers)
        summary_consumption_plot.addPlotLine(consumptionPlotLine, consumptionPlotLinePaint)
        summary_consumption_plot.sessionGapRendering = PlotSessionGapRendering.JOIN

        if (appPreferences.consumptionUnit) {
            consumptionPlotLine.Configuration.Unit = "Wh/%s".format(appPreferences.distanceUnit.unit())
            consumptionPlotLine.Configuration.LabelFormat = PlotLineLabelFormat.NUMBER
            consumptionPlotLine.Configuration.Divider = appPreferences.distanceUnit.toFactor() * 1f
        } else {
            consumptionPlotLine.Configuration.Unit = "kWh/100%s".format(appPreferences.distanceUnit.unit())
            consumptionPlotLine.Configuration.LabelFormat = PlotLineLabelFormat.FLOAT
            consumptionPlotLine.Configuration.Divider = appPreferences.distanceUnit.toFactor() * 10f
        }

        summary_button_secondary_dimension.text = when (appPreferences.secondaryConsumptionDimension) {
            1 -> getString(R.string.main_secondary_axis, getString(R.string.main_speed))
            2 -> getString(R.string.main_secondary_axis, getString(R.string.main_SoC))
            3 -> getString(R.string.main_secondary_axis, getString(R.string.plot_dimensionY_ALTITUDE))
            else -> getString(R.string.main_secondary_axis, "-")
        }
        summary_consumption_plot.dimensionYSecondary = PlotDimensionY.IndexMap[appPreferences.secondaryConsumptionDimension]

        summary_consumption_plot.dimension = PlotDimensionX.DISTANCE
        summary_consumption_plot.dimensionRestriction = appPreferences.distanceUnit.asUnit(((appPreferences.distanceUnit.toUnit(session.driven_distance.toFloat()) / MainActivity.DISTANCE_TRIP_DIVIDER).toInt() + 1) * MainActivity.DISTANCE_TRIP_DIVIDER) + 1
        summary_consumption_plot.dimensionRestrictionMin = appPreferences.distanceUnit.asUnit(MainActivity.DISTANCE_TRIP_DIVIDER)
        summary_consumption_plot.dimensionSmoothing = 0.02f
        summary_consumption_plot.dimensionSmoothingType = PlotDimensionSmoothingType.PERCENTAGE
        summary_consumption_plot.setPlotMarkers(plotMarkers)
        summary_consumption_plot.visibleMarkerTypes.add(PlotMarkerType.CHARGE)
        summary_consumption_plot.visibleMarkerTypes.add(PlotMarkerType.PARK)

        summary_consumption_plot.invalidate()

        summary_distance_value_text.text = StringFormatters.getTraveledDistanceString(session.driven_distance.toFloat())

        // val altList = tripData.consumptionPlotLine.mapNotNull { it.Altitude }
        // val altFirst = if (altList.isEmpty()) altList.first() else null
        // val altMax = altList.maxOrNull()
        // val altMin = altList.minOrNull()
        // var altLast = if (altList.isEmpty()) altList.last() else null

        val altUp = session.drivingPoints?.mapNotNull { it.alt }?.filter { it > 0 }?.sum()
        val altDown = session.drivingPoints?.mapNotNull { it.alt }?.filter { it < 0 }?.sum()?.absoluteValue

        summary_altitude_value_text.text = StringFormatters.getAltitudeString(altUp, altDown)
        summary_used_energy_value_text.text = StringFormatters.getEnergyString(session.used_energy.toFloat())
        summary_avg_consumption_value_text.text = StringFormatters.getAvgConsumptionString(session.used_energy.toFloat(), session.driven_distance.toFloat())
        summary_travel_time_value_text.text = StringFormatters.getElapsedTimeString(session.drive_time)

        summary_button_secondary_dimension.setOnClickListener {
            var currentIndex = appPreferences.secondaryConsumptionDimension
            currentIndex++
            if (currentIndex > 3) currentIndex = 0
            appPreferences.secondaryConsumptionDimension = currentIndex
            summary_consumption_plot.dimensionYSecondary = when (appPreferences.secondaryConsumptionDimension) {
                1 -> {
                    summary_button_secondary_dimension.text =
                        getString(R.string.main_secondary_axis, getString(R.string.main_speed))
                    PlotDimensionY.SPEED
                }
                2 -> {
                    summary_button_secondary_dimension.text =
                        getString(R.string.main_secondary_axis, getString(R.string.main_SoC))
                    PlotDimensionY.STATE_OF_CHARGE
                }
                3 -> {
                    summary_button_secondary_dimension.text =
                        getString(R.string.main_secondary_axis, getString(R.string.plot_dimensionY_ALTITUDE))
                    PlotDimensionY.ALTITUDE
                }
                else -> {
                    summary_button_secondary_dimension.text = getString(R.string.main_secondary_axis, "-")
                    null
                }
            }
        }
    }

    private fun setupChargeLayout() {
        /*
        summary_charge_plot_sub_title_curve.text = "%s (%d/%d, %s)".format(
            getString(R.string.settings_sub_title_last_charge_plot),
            tripData.chargeCurves.size,
            tripData.chargeCurves.size,
            StringFormatters.getDateString(
                if (tripData.chargeCurves.isNotEmpty()) tripData.chargeCurves.last().chargeStartDate
                else null
            )
        )

        chargePlotLine.reset()
        if (tripData.chargeCurves.isNotEmpty()) {
            chargePlotLine.addDataPoints(tripData.chargeCurves.last().chargePlotLine)
            summary_charge_plot_button_next.isEnabled = false
            summary_charge_plot_button_next.alpha = CarStatsViewer.disabledAlpha
            summary_charge_plot_button_prev.isEnabled = true
            summary_charge_plot_button_prev.alpha = 1f

            if (tripData.chargeCurves.last().chargePlotLine.filter { it.Marker == PlotLineMarkerType.END_SESSION }.size > 1)
            // Charge has been interrupted
                summary_charged_energy_warning_text.visibility = View.VISIBLE
            else summary_charged_energy_warning_text.visibility = View.GONE

            summary_charged_energy_value_text.text = chargedEnergy(tripData.chargeCurves.last())
            summary_charge_time_value_text.text = StringFormatters.getElapsedTimeString(tripData.chargeCurves.last().chargeTime)
            summary_charge_ambient_temp.text = StringFormatters.getTemperatureString(tripData.chargeCurves.last().ambientTemperature)
            summary_charge_plot_view.dimensionRestriction = TimeUnit.MINUTES.toMillis((TimeUnit.MILLISECONDS.toMinutes(tripData.chargeCurves.last().chargeTime) / 5) + 1) * 5 + 1
            summary_charge_plot_view.dimensionRestrictionMin = TimeUnit.MINUTES.toMillis(5L)
        }
        if (tripData.chargeCurves.size < 2){
            summary_charge_plot_button_next.isEnabled = false
            summary_charge_plot_button_next.alpha = CarStatsViewer.disabledAlpha
            summary_charge_plot_button_prev.isEnabled = false
            summary_charge_plot_button_prev.alpha = CarStatsViewer.disabledAlpha
        }
        summary_charge_plot_view.addPlotLine(chargePlotLine, chargePlotLinePaint)

        summary_charge_plot_view.dimension = PlotDimensionX.TIME
        // summary_charge_plot_view.dimensionSmoothingPercentage = 0.01f
        summary_charge_plot_view.sessionGapRendering = PlotSessionGapRendering.GAP
        summary_charge_plot_view.dimensionYPrimary = PlotDimensionY.STATE_OF_CHARGE
        summary_charge_plot_view.invalidate()

        summary_charge_plot_seek_bar.max = (tripData.chargeCurves.size - 1).coerceAtLeast(0)
        summary_charge_plot_seek_bar.progress = (tripData.chargeCurves.size - 1).coerceAtLeast(0)
        summary_charge_plot_seek_bar.setOnSeekBarChangeListener(seekBarChangeListener)

        summary_charge_plot_button_next.setOnClickListener {
            val newProgress = summary_charge_plot_seek_bar.progress + 1
            if (newProgress <= (tripData.chargeCurves.size - 1)) {
                summary_charge_plot_seek_bar.progress = newProgress
            }
            summary_charge_plot_view.dimensionShift = 0L
        }

        summary_charge_plot_button_prev.setOnClickListener {
            val newProgress = summary_charge_plot_seek_bar.progress - 1
            if (newProgress >= 0) {
                summary_charge_plot_seek_bar.progress = newProgress
            }
            summary_charge_plot_view.dimensionShift = 0L
        }
        */
    }

    private fun setVisibleChargeCurve(progress: Int) {
        /*
        summary_charge_plot_sub_title_curve.text = "%s (%d/%d, %s)".format(
            getString(R.string.settings_sub_title_last_charge_plot),
            tripData.chargeCurves.size,
            tripData.chargeCurves.size,
            StringFormatters.getDateString(tripData.chargeCurves.last().chargeStartDate))

        if (tripData.chargeCurves.size - 1 == 0) {
            summary_charge_plot_sub_title_curve.text = "%s (0/0)".format(
                getString(R.string.settings_sub_title_last_charge_plot))

            summary_charge_plot_button_next.isEnabled = false
            summary_charge_plot_button_next.alpha = CarStatsViewer.disabledAlpha
            summary_charge_plot_button_prev.isEnabled = false
            summary_charge_plot_button_prev.alpha = CarStatsViewer.disabledAlpha

        } else {
            summary_charge_plot_sub_title_curve.text = "%s (%d/%d, %s)".format(
                getString(R.string.settings_sub_title_last_charge_plot),
                progress + 1,
                tripData.chargeCurves.size,
                StringFormatters.getDateString(tripData.chargeCurves[progress].chargeStartDate))

            when (progress) {
                0 -> {
                    summary_charge_plot_button_prev.isEnabled = false
                    summary_charge_plot_button_prev.alpha = CarStatsViewer.disabledAlpha
                    summary_charge_plot_button_next.isEnabled = true
                    summary_charge_plot_button_next.alpha = 1f
                }
                tripData.chargeCurves.size - 1 -> {
                    summary_charge_plot_button_next.isEnabled = false
                    summary_charge_plot_button_next.alpha = CarStatsViewer.disabledAlpha
                    summary_charge_plot_button_prev.isEnabled = true
                    summary_charge_plot_button_prev.alpha = 1f
                }
                else -> {
                    summary_charge_plot_button_next.isEnabled = true
                    summary_charge_plot_button_next.alpha = 1f
                    summary_charge_plot_button_prev.isEnabled = true
                    summary_charge_plot_button_prev.alpha = 1f
                }
            }
        }
        if (tripData.chargeCurves[progress].chargePlotLine.filter { it.Marker == PlotLineMarkerType.END_SESSION }.size > 1)
        // Charge has been interrupted
            summary_charged_energy_warning_text.visibility = View.VISIBLE
        else summary_charged_energy_warning_text.visibility = View.GONE

        summary_charged_energy_value_text.text = chargedEnergy(tripData.chargeCurves[progress])
        summary_charge_time_value_text.text = StringFormatters.getElapsedTimeString(tripData.chargeCurves[progress].chargeTime)
        summary_charge_ambient_temp.text = StringFormatters.getTemperatureString(tripData.chargeCurves[progress].ambientTemperature)

        chargePlotLine.reset()
        chargePlotLine.addDataPoints(tripData.chargeCurves[progress].chargePlotLine)
        summary_charge_plot_view.dimensionRestriction = TimeUnit.MINUTES.toMillis((TimeUnit.MILLISECONDS.toMinutes(tripData.chargeCurves[progress].chargeTime) / 5) + 1) * 5 + 1
        summary_charge_plot_view.invalidate()

         */
    }

    private fun switchToConsumptionLayout() {
        summary_consumption_container.visibility = View.VISIBLE
        summary_charge_container.visibility = View.GONE
        summary_consumption_container.scrollTo(0,0)
        summary_button_show_charge_container.isSelected = false
        summary_button_show_consumption_container.isSelected = true

    }

    private fun switchToChargeLayout() {
        summary_consumption_container.visibility = View.GONE
        summary_charge_container.visibility = View.VISIBLE
        summary_charge_container.scrollTo(0,0)
        summary_button_show_consumption_container.isSelected = false
        summary_button_show_charge_container.isSelected = true
    }

    private fun createResetDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.dialog_reset_title))
            .setMessage(getString(R.string.dialog_reset_message))
            .setCancelable(true)
            .setPositiveButton(getString(R.string.dialog_reset_confirm)) { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    CarStatsViewer.dataProcessor.resetTrip(
                        TripType.MANUAL,
                        CarStatsViewer.dataProcessor.realTimeData.drivingState
                    )
                    refreshActivity(session.session_type - 1)
                }
            }
            .setNegativeButton(getString(R.string.dialog_reset_cancel)) { dialog, _ ->
                dialog.cancel()
            }
        val alert = builder.create()
        alert.show()
    }

    private fun updateDistractionOptimization() {
        InAppLogger.d("Distraction optimization: ${appPreferences.doDistractionOptimization}")
        summary_parked_warning.visibility =
            if (appPreferences.doDistractionOptimization) View.VISIBLE
            else View.GONE
        summary_content_container.visibility =
            if (appPreferences.doDistractionOptimization) View.GONE
            else View.VISIBLE
    }

    private fun chargedEnergy(chargeCurve: ChargeCurve): String {
        return "%s (%d%%)".format(
            StringFormatters.getEnergyString(chargeCurve.chargedEnergy),
            (chargeCurve.chargePlotLine.last().StateOfCharge - chargeCurve.chargePlotLine.first().StateOfCharge).toInt()
        )
    }

    private fun refreshActivity(index: Int) {
        InAppLogger.i("Trip index: ${appPreferences.mainViewTrip}")

        CoroutineScope(Dispatchers.IO).launch {
            CarStatsViewer.dataProcessor.changeSelectedTrip(index + 1)
            CarStatsViewer.tripDataSource.getActiveDrivingSessionsIdsMap()[index + 1]?.let {
                val session = CarStatsViewer.tripDataSource.getFullDrivingSession(it)
                requireActivity().runOnUiThread {
                    requireActivity().supportFragmentManager.commit {
                        setCustomAnimations(0, 0, 0, 0)
                        replace(fragmentContainerId, SummaryFragment(session, fragmentContainerId))
                    }
                }
            }
        }
    }
}