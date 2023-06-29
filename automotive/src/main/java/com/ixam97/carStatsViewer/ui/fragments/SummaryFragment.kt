package com.ixam97.carStatsViewer.ui.fragments

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColor
import androidx.core.view.forEach
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.ui.activities.MainActivity
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.database.tripData.TripType
import com.ixam97.carStatsViewer.ui.plot.graphics.PlotLinePaint
import com.ixam97.carStatsViewer.ui.plot.graphics.PlotPaint
import com.ixam97.carStatsViewer.ui.plot.objects.PlotLine
import com.ixam97.carStatsViewer.ui.plot.objects.PlotLineConfiguration
import com.ixam97.carStatsViewer.ui.plot.objects.PlotRange
import com.ixam97.carStatsViewer.ui.plot.enums.*
import com.ixam97.carStatsViewer.utils.DataConverters
import com.ixam97.carStatsViewer.utils.InAppLogger
import com.ixam97.carStatsViewer.utils.StringFormatters
import com.ixam97.carStatsViewer.ui.views.PlotView
import kotlinx.android.synthetic.main.fragment_summary.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

class SummaryFragment() : Fragment(R.layout.fragment_summary) {

    lateinit var session: DrivingSession

    constructor(session: DrivingSession) :this() {
        this.session = session
    }


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

    private var consumptionPlotLine = PlotLine(
        PlotLineConfiguration(
            PlotRange(-300f, 900f, -300f, 900f, 100f, 0f),
            PlotLineLabelFormat.NUMBER,
            PlotHighlightMethod.AVG_BY_DISTANCE,
            "Wh/km"
        ),
    )

    private val consumptionPlotLinePaint = PlotLinePaint(
    PlotPaint.byColor(applicationContext.getColor(R.color.primary_plot_color), PlotView.textSize),
    PlotPaint.byColor(applicationContext.getColor(R.color.secondary_plot_color), PlotView.textSize),
    PlotPaint.byColor(applicationContext.getColor(R.color.secondary_plot_color_alt), PlotView.textSize)
    ) { appPreferences.consumptionPlotSecondaryColor }

    private val chargePlotLinePaint = PlotLinePaint(
    PlotPaint.byColor(applicationContext.getColor(R.color.charge_plot_color), PlotView.textSize),
    PlotPaint.byColor(applicationContext.getColor(R.color.secondary_plot_color), PlotView.textSize),
    PlotPaint.byColor(applicationContext.getColor(R.color.secondary_plot_color_alt), PlotView.textSize)
    ) { appPreferences.chargePlotSecondaryColor }

    private val seekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            setVisibleChargeCurve(progress)
        }
        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPlots()
        setSecondaryConsumptionPlotDimension(appPreferences.secondaryConsumptionDimension)
        setupListeners()

        // check if a session has been provided, else close fragment
        if (this::session.isInitialized) {
            applySession(session)
        } else {
            // val backupSession = CarStatsViewer.dataProcessor.selectedSessionDataFlow.value
            // if (backupSession != null) {
            //     CoroutineScope(Dispatchers.IO).launch {
            //         session = CarStatsViewer.tripDataSource.getFullDrivingSession(backupSession.driving_session_id)
            //         requireActivity().runOnUiThread {
            //             applySession(session)
            //         }
            //     }
            // } else {
                requireActivity().supportFragmentManager.commit { remove(this@SummaryFragment) }
            // }
        }
    }

    private fun setupListeners() {
        setupHeader()

        summary_button_reset.setOnClickListener {
            createResetDialog()
        }

        summary_button_show_consumption_container.setOnClickListener {
            switchToConsumptionLayout()
        }

        summary_button_show_charge_container.setOnClickListener {
            switchToChargeLayout()
        }

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
        }

        summary_button_secondary_dimension.setOnClickListener {
            var currentIndex = appPreferences.secondaryConsumptionDimension
            currentIndex++
            if (currentIndex > 3) currentIndex = 0
            appPreferences.secondaryConsumptionDimension = currentIndex
            setSecondaryConsumptionPlotDimension(currentIndex)
        }

        summary_button_dist_20.setOnClickListener {
            summary_consumption_plot.dimensionRestriction = appPreferences.distanceUnit.asUnit(20_000) + 1
            summary_consumption_plot.dimensionShift = 0
        }
        summary_button_dist_40.setOnClickListener {
            summary_consumption_plot.dimensionRestriction = appPreferences.distanceUnit.asUnit(40_000) + 1
            summary_consumption_plot.dimensionShift = 0
        }
        summary_button_dist_100.setOnClickListener {
            summary_consumption_plot.dimensionRestriction = appPreferences.distanceUnit.asUnit(100_000) + 1
            summary_consumption_plot.dimensionShift = 0
        }
    }

    private fun setupHeader() {
        summary_button_trip_prev.setOnClickListener {
            var tripIndex = appPreferences.mainViewTrip
            tripIndex--
            if (tripIndex < 0) tripIndex = 3
            appPreferences.mainViewTrip = tripIndex
            changeSelectedTrip(tripIndex)
        }

        summary_button_trip_next.setOnClickListener {
            var tripIndex = appPreferences.mainViewTrip
            tripIndex++
            if (tripIndex > 3) tripIndex = 0
            appPreferences.mainViewTrip = tripIndex
            changeSelectedTrip(tripIndex)
        }

        summary_selector_title.setOnClickListener {
            var tripIndex = appPreferences.mainViewTrip
            tripIndex++
            if (tripIndex > 3) tripIndex = 0
            appPreferences.mainViewTrip = tripIndex
            changeSelectedTrip(tripIndex)
        }
    }

    private fun setupPlots() {

        if (appPreferences.consumptionUnit) {
            consumptionPlotLine.Configuration.Unit = "Wh/%s".format(appPreferences.distanceUnit.unit())
            consumptionPlotLine.Configuration.LabelFormat = PlotLineLabelFormat.NUMBER
            consumptionPlotLine.Configuration.Divider = appPreferences.distanceUnit.toFactor() * 1f
        } else {
            consumptionPlotLine.Configuration.Unit = "kWh/100%s".format(appPreferences.distanceUnit.unit())
            consumptionPlotLine.Configuration.LabelFormat = PlotLineLabelFormat.FLOAT
            consumptionPlotLine.Configuration.Divider = appPreferences.distanceUnit.toFactor() * 10f
        }

        summary_consumption_plot.dimension = PlotDimensionX.DISTANCE
        summary_consumption_plot.dimensionRestrictionMin = appPreferences.distanceUnit.asUnit(
            MainActivity.DISTANCE_TRIP_DIVIDER)
        summary_consumption_plot.dimensionSmoothing = 0.02f
        summary_consumption_plot.dimensionSmoothingType = PlotDimensionSmoothingType.PERCENTAGE
        summary_consumption_plot.visibleMarkerTypes.add(PlotMarkerType.CHARGE)
        summary_consumption_plot.visibleMarkerTypes.add(PlotMarkerType.PARK)

        summary_consumption_plot.addPlotLine(consumptionPlotLine, consumptionPlotLinePaint)
        summary_consumption_plot.sessionGapRendering = PlotSessionGapRendering.JOIN

        summary_charge_plot_view.dimension = PlotDimensionX.TIME
        // summary_charge_plot_view.dimensionSmoothing = 0.01f
        // summary_charge_plot_view.dimensionSmoothingType = PlotDimensionSmoothingType.PERCENTAGE
        summary_charge_plot_view.dimensionYSecondary = PlotDimensionY.STATE_OF_CHARGE

        summary_charge_plot_view.addPlotLine(chargePlotLine, chargePlotLinePaint)
        summary_charge_plot_view.sessionGapRendering = PlotSessionGapRendering.GAP
    }

    private fun applySession(session: DrivingSession) {

        switchToConsumptionLayout()

        summary_button_show_charge_container.isEnabled =
            !(session.chargingSessions == null || session.chargingSessions?.isEmpty() == true)

        if (session.session_type != TripType.MANUAL) {
            summary_button_reset.isEnabled = false
            summary_button_reset.setColorFilter(applicationContext.getColor(R.color.disabled_tint), PorterDuff.Mode.SRC_IN)
        } else {
            summary_button_reset.isEnabled = true
            summary_button_reset.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
        }

        session.drivingPoints?.let {
            consumptionPlotLine.reset()
            consumptionPlotLine.addDataPoints(DataConverters.consumptionPlotLineFromDrivingPoints(it))
            summary_consumption_plot.setPlotMarkers(DataConverters.plotMarkersFromSession(session))
            summary_consumption_plot.dimensionRestriction = appPreferences.distanceUnit.asUnit(((appPreferences.distanceUnit.toUnit(session.driven_distance.toFloat()) / MainActivity.DISTANCE_TRIP_DIVIDER).toInt() + 1) * MainActivity.DISTANCE_TRIP_DIVIDER) + 1
            summary_consumption_plot.invalidate()
        }

        session.chargingSessions?.let { chargingSessions ->
            // Setup charge layout


            summary_charge_plot_seek_bar.max = (chargingSessions.size - 1).coerceAtLeast(0)
            summary_charge_plot_seek_bar.progress = (chargingSessions.size - 1).coerceAtLeast(0)
            summary_charge_plot_seek_bar.setOnSeekBarChangeListener(seekBarChangeListener)

            summary_charge_plot_button_next.setOnClickListener {
                val newProgress = summary_charge_plot_seek_bar.progress + 1
                if (newProgress <= (chargingSessions.size - 1)) {
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

            setVisibleChargeCurve(chargingSessions.size - 1)
        }

        when (session.session_type) {
            TripType.MANUAL -> summary_type_icon.setImageDrawable(applicationContext.getDrawable(R.drawable.ic_hand))
            TripType.SINCE_CHARGE -> summary_type_icon.setImageDrawable(applicationContext.getDrawable(R.drawable.ic_charger_2))
            TripType.AUTO -> summary_type_icon.setImageDrawable(applicationContext.getDrawable(R.drawable.ic_day))
            TripType.MONTH -> summary_type_icon.setImageDrawable(applicationContext.getDrawable(R.drawable.ic_month))
        }


        if ((session.end_epoch_time?:0) > 0 ) {
            summary_title.text = "${StringFormatters.getDateString(Date(session.start_epoch_time))}, ${resources.getStringArray(R.array.trip_type_names)[session.session_type]}"
        } else {
            summary_title.visibility = View.GONE
            summary_trip_selector.visibility = View.VISIBLE
            summary_selector_title.text = resources.getStringArray(R.array.trip_type_names)[session.session_type]
            summary_selected_trip_bar.forEach { bar ->
                bar.background = applicationContext.getColor(R.color.disable_background).toDrawable()
            }
            summary_selected_trip_bar[appPreferences.mainViewTrip].background = primaryColor.toColor().toDrawable()
        }

        var altUp = 0f
        var altDown = 0f

        session.drivingPoints?.let { drivingPoint ->
            val altList = drivingPoint.map { it.alt }

            altList.forEachIndexed { index, alt ->
                if (index > 0) {
                    val prevAlt = altList[index - 1]
                    if (alt != null && prevAlt != null) {
                        if (alt > prevAlt) altUp += alt - prevAlt
                        if (alt < prevAlt) altDown += prevAlt - alt
                    }
                }
            }
        }

        summary_trip_date_text.text = getString(R.string.summary_trip_start_date).format(StringFormatters.getDateString(Date(session.start_epoch_time)))
        summary_distance_value_text.text = StringFormatters.getTraveledDistanceString(session.driven_distance.toFloat())
        summary_altitude_value_text.text = StringFormatters.getAltitudeString(altUp, altDown)
        summary_used_energy_value_text.text = StringFormatters.getEnergyString(session.used_energy.toFloat())
        summary_avg_consumption_value_text.text = StringFormatters.getAvgConsumptionString(session.used_energy.toFloat(), session.driven_distance.toFloat())
        summary_travel_time_value_text.text = StringFormatters.getElapsedTimeString(session.drive_time)
        summary_avg_speed_value_text.text = StringFormatters.getAvgSpeedString(session.driven_distance.toFloat(), session.drive_time)

        summary_button_show_consumption_container.isSelected = true
    }

    private fun setSecondaryConsumptionPlotDimension(secondaryConsumptionDimension: Int) {
        if(requireActivity() is MainActivity) {
            (requireActivity() as MainActivity).setSecondaryConsumptionPlotDimension(secondaryConsumptionDimension)
        }

        summary_button_secondary_dimension.text = when (secondaryConsumptionDimension) {
            1 -> getString(R.string.main_secondary_axis, getString(R.string.main_speed))
            2 -> getString(R.string.main_secondary_axis, getString(R.string.main_SoC))
            3 -> getString(R.string.main_secondary_axis, getString(R.string.plot_dimensionY_ALTITUDE))
            else -> getString(R.string.main_secondary_axis, "-")
        }
        summary_consumption_plot.dimensionYSecondary = PlotDimensionY.IndexMap[secondaryConsumptionDimension]
        summary_consumption_plot.invalidate()
    }

    private fun setVisibleChargeCurve(progress: Int) {

        val chargingSessions = session.chargingSessions

        summary_charge_plot_view.reset()
        chargePlotLine.reset()

        // InAppLogger.d("Charging sessions size: ${chargingSessions?.size}, selected index: $progress")

        if (chargingSessions == null || chargingSessions.isEmpty() || progress >= chargingSessions.size || progress < 0) {
            summary_charge_plot_sub_title_curve.text = "%s (0/0)".format(
                getString(R.string.settings_sub_title_last_charge_plot))

            summary_charge_plot_button_next.isEnabled = false
            summary_charge_plot_button_next.alpha = CarStatsViewer.disabledAlpha
            summary_charge_plot_button_prev.isEnabled = false
            summary_charge_plot_button_prev.alpha = CarStatsViewer.disabledAlpha

            summary_charged_energy_value_text.text = "-/-"
            summary_charge_time_value_text.text = "-/-"
            summary_charge_ambient_temp.text = "-/-"

            return
        }

        summary_charge_plot_sub_title_curve.text = "%s (%d/%d, %s)".format(
            getString(R.string.settings_sub_title_last_charge_plot),
            chargingSessions.size,
            chargingSessions.size,
            StringFormatters.getDateString(Date(chargingSessions.last().start_epoch_time))
        )

        if (chargingSessions.size - 1 != 0) {
            summary_charge_plot_sub_title_curve.text = "%s (%d/%d, %s)".format(
                getString(R.string.settings_sub_title_last_charge_plot),
                progress + 1,
                chargingSessions.size,
                StringFormatters.getDateString(Date(chargingSessions[progress].start_epoch_time)))

            when (progress) {
                0 -> {
                    summary_charge_plot_button_prev.isEnabled = false
                    summary_charge_plot_button_prev.alpha = CarStatsViewer.disabledAlpha
                    summary_charge_plot_button_next.isEnabled = true
                    summary_charge_plot_button_next.alpha = 1f
                }
                chargingSessions.size - 1 -> {
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

        summary_charged_energy_value_text.text = StringFormatters.getEnergyString(chargingSessions[progress].charged_energy.toFloat())
        summary_charge_time_value_text.text = StringFormatters.getElapsedTimeString((chargingSessions[progress].end_epoch_time?:0) - chargingSessions[progress].start_epoch_time)
        summary_charge_ambient_temp.text = StringFormatters.getTemperatureString(chargingSessions[progress].outside_temp)

        chargePlotLine.addDataPoints(DataConverters.chargePlotLineFromChargingPoints(chargingSessions[progress].chargingPoints!!))

        summary_charge_plot_view.dimensionRestriction = TimeUnit.MINUTES.toMillis((TimeUnit.MILLISECONDS.toMinutes((chargingSessions[progress].end_epoch_time?:0) - chargingSessions[progress].start_epoch_time) / 5) + 1) * 5 + 1
        summary_charge_plot_view.invalidate()


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
                    changeSelectedTrip(session.session_type - 1)
                }
            }
            .setNegativeButton(getString(R.string.dialog_reset_cancel)) { dialog, _ ->
                dialog.cancel()
            }
        val alert = builder.create()
        alert.show()
    }

    private fun changeSelectedTrip(index: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            CarStatsViewer.dataProcessor.changeSelectedTrip(index + 1)
            CarStatsViewer.tripDataSource.getActiveDrivingSessionsIdsMap()[index + 1]?.let {
                session = CarStatsViewer.tripDataSource.getFullDrivingSession(it)
                requireActivity().runOnUiThread {
                    applySession(session)
                }
            }
        }
    }
}