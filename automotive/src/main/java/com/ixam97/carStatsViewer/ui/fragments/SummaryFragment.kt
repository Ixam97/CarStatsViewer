package com.ixam97.carStatsViewer.ui.fragments

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.SeekBar
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColor
import androidx.core.view.forEach
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.database.tripData.ChargingSession
import com.ixam97.carStatsViewer.ui.activities.MainActivity
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.database.tripData.TripType
import com.ixam97.carStatsViewer.emulatorMode
import com.ixam97.carStatsViewer.ui.plot.graphics.PlotLinePaint
import com.ixam97.carStatsViewer.ui.plot.graphics.PlotPaint
import com.ixam97.carStatsViewer.ui.plot.objects.PlotLine
import com.ixam97.carStatsViewer.ui.plot.objects.PlotLineConfiguration
import com.ixam97.carStatsViewer.ui.plot.objects.PlotRange
import com.ixam97.carStatsViewer.ui.plot.enums.*
import com.ixam97.carStatsViewer.utils.DataConverters
import com.ixam97.carStatsViewer.utils.StringFormatters
import com.ixam97.carStatsViewer.ui.views.PlotView
import com.ixam97.carStatsViewer.utils.InAppLogger
import com.ixam97.carStatsViewer.utils.applyTypeface
import com.ixam97.carStatsViewer.utils.getColorFromAttribute
import kotlinx.android.synthetic.main.activity_main.main_consumption_plot
import kotlinx.android.synthetic.main.activity_main.main_image_button_alt
import kotlinx.android.synthetic.main.activity_main.main_image_button_soc
import kotlinx.android.synthetic.main.activity_main.main_image_button_speed
import kotlinx.android.synthetic.main.activity_main.main_secondary_dimension_indicator
import kotlinx.android.synthetic.main.activity_main.main_secondary_selector_container
import kotlinx.android.synthetic.main.fragment_summary.*
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class SummaryFragment() : Fragment(R.layout.fragment_summary) {

    lateinit var session: DrivingSession
    private var completedChargingSessions = listOf<ChargingSession>()

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
            PlotRange(-200f, 600f, -200f, 600f, 100f, 0f),
            PlotLineLabelFormat.NUMBER,
            PlotHighlightMethod.AVG_BY_DISTANCE,
            "Wh/km"
        ),
    )

    private lateinit var consumptionPlotLinePaint: PlotLinePaint

    private val chargePlotLinePaint = PlotLinePaint(
    PlotPaint.byColor(applicationContext.getColor(R.color.charge_plot_color), CarStatsViewer.appContext.resources.getDimension(R.dimen.reduced_font_size)),
    PlotPaint.byColor(applicationContext.getColor(R.color.secondary_plot_color), CarStatsViewer.appContext.resources.getDimension(R.dimen.reduced_font_size)),
    PlotPaint.byColor(applicationContext.getColor(R.color.secondary_plot_color_alt), CarStatsViewer.appContext.resources.getDimension(R.dimen.reduced_font_size))
    ) { appPreferences.chargePlotSecondaryColor }

    private val seekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            setVisibleChargeCurve(progress)
        }
        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    }

    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        return if (CarStatsViewer.appPreferences.colorTheme > 0) {
            val inflater = super.onGetLayoutInflater(savedInstanceState)
            val contextThemeWrapper: Context = ContextThemeWrapper(requireContext(), R.style.ColorTestTheme)
            inflater.cloneInContext(contextThemeWrapper)
        } else {
            super.onGetLayoutInflater(savedInstanceState)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        CarStatsViewer.typefaceRegular?.let {
            applyTypeface(view)
        }

        consumptionPlotLinePaint = PlotLinePaint(
            PlotPaint.byColor(getColorFromAttribute(requireContext(), R.attr.primary_plot_color), CarStatsViewer.appContext.resources.getDimension(R.dimen.reduced_font_size)),
            PlotPaint.byColor(getColorFromAttribute(requireContext(), R.attr.secondary_plot_color), CarStatsViewer.appContext.resources.getDimension(R.dimen.reduced_font_size)),
            PlotPaint.byColor(getColorFromAttribute(requireContext(), R.attr.tertiary_plot_color), CarStatsViewer.appContext.resources.getDimension(R.dimen.reduced_font_size))
        ) { appPreferences.consumptionPlotSecondaryColor }

        setupPlots()
        setSecondaryConsumptionPlotDimension(appPreferences.secondaryConsumptionDimension)
        setupListeners()

        summary_view_selector.buttonList[2].isEnabled = false

        // Don't allow the scroll view to scroll when interacting with plots
        fun disallowIntercept(v: View, event: MotionEvent) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.parent.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_UP -> {
                    v.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
        }

        summary_consumption_plot.setOnTouchListener { v, event ->
            disallowIntercept(v, event)
            false
        }
        summary_charge_plot_view.setOnTouchListener { v, event ->
            disallowIntercept(v, event)
            false
        }

        // check if a session has been provided, else close fragment
        if (this::session.isInitialized) {
            applySession(session)
        } else {
            requireActivity().supportFragmentManager.commit { remove(this@SummaryFragment) }
        }

        summary_secondary_dimension_indicator.background = if (appPreferences.consumptionPlotSecondaryColor) {
            getColorFromAttribute(requireContext(), R.attr.tertiary_plot_color).toDrawable()
        } else {
            getColorFromAttribute(requireContext(), R.attr.secondary_plot_color).toDrawable()
        }

        // summary_button_dist_all.text = "${appPreferences.distanceUnit.toUnit(((session.driven_distance.toLong() / 5_000) + 1) * 5)} ${appPreferences.distanceUnit.unit()}"
    }

    private fun setupListeners() {
        setupHeader()

        summary_button_reset.setOnClickListener {
            createResetDialog()
        }

        summary_view_selector.setOnIndexChangedListener{
            when (summary_view_selector.selectedIndex) {
                0 -> switchToConsumptionLayout()
                1 -> switchToChargeLayout()
            }
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
        summary_button_dist_all.setOnClickListener {
            summary_consumption_plot.dimensionRestriction = appPreferences.distanceUnit.asUnit(((session.driven_distance.toLong() / 5_000) + 1) * 5_000) + 1
            summary_consumption_plot.dimensionShift = 0
        }

        summary_image_button_speed.setOnClickListener {
            var currentIndex = appPreferences.secondaryConsumptionDimension
            currentIndex = if (currentIndex == 1) 0 else 1
            setSecondaryConsumptionPlotDimension(currentIndex)
            appPreferences.secondaryConsumptionDimension = currentIndex
        }

        summary_image_button_soc.setOnClickListener {
            var currentIndex = appPreferences.secondaryConsumptionDimension
            currentIndex = if (currentIndex == 2) 0 else 2
            setSecondaryConsumptionPlotDimension(currentIndex)
            appPreferences.secondaryConsumptionDimension = currentIndex
        }

        summary_image_button_alt.setOnClickListener {
            var currentIndex = appPreferences.secondaryConsumptionDimension
            currentIndex = if (currentIndex == 3) 0 else 3
            setSecondaryConsumptionPlotDimension(currentIndex)
            appPreferences.secondaryConsumptionDimension = currentIndex
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
        summary_charge_plot_view.dimensionRestrictionMin = TimeUnit.MINUTES.toMillis(5)
        summary_charge_plot_view.dimensionSmoothing = 0.01f
        summary_charge_plot_view.dimensionSmoothingType = PlotDimensionSmoothingType.PERCENTAGE
        summary_charge_plot_view.dimensionYSecondary = PlotDimensionY.STATE_OF_CHARGE

        summary_charge_plot_view.addPlotLine(chargePlotLine, chargePlotLinePaint)
        summary_charge_plot_view.sessionGapRendering = PlotSessionGapRendering.GAP
    }

    private fun applySession(session: DrivingSession) {
        switchToConsumptionLayout()
        summary_view_selector.buttonList[1].isEnabled = false

        if (session.session_type != TripType.MANUAL) {
            summary_button_reset.isEnabled = false
            summary_button_reset.setColorFilter(applicationContext.getColor(R.color.disabled_tint), PorterDuff.Mode.SRC_IN)
        } else {
            summary_button_reset.isEnabled = true
            summary_button_reset.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
        }

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                requireActivity().runOnUiThread {
                    summary_consumption_plot.visibility = View.GONE
                    summary_progress_bar.visibility = View.VISIBLE
                }
                if (session.drivingPoints == null || session.chargingSessions == null) {
                    val fullSession = CarStatsViewer.tripDataSource.getFullDrivingSession(sessionId = session.driving_session_id)
                    InAppLogger.d("[SUM] Loading driving points and charging sessions")
                    session.drivingPoints = fullSession.drivingPoints
                    session.chargingSessions = fullSession.chargingSessions

                    if (emulatorMode) delay(1_000)
                }

                var altUp = 0f
                var altDown = 0f

                session.drivingPoints?.let { drivingPoints ->
                    val plotPoints = DataConverters.consumptionPlotLineFromDrivingPoints(drivingPoints)
                    val plotMarkers = DataConverters.plotMarkersFromSession(session)

                    var prevAlt = 0f
                    val altList = drivingPoints.mapNotNull { it.alt }
                    altList.forEachIndexed { index, alt ->
                        if (index == 0) prevAlt = alt
                        else {
                            when {
                                alt > prevAlt -> altUp += alt - prevAlt
                                alt < prevAlt -> altDown += prevAlt - alt
                            }
                        }
                        prevAlt = alt
                    }

                    delay(500)
                    requireActivity().runOnUiThread {
                        summary_altitude_widget.topText = StringFormatters.getAltitudeString(altUp, altDown)
                        consumptionPlotLine.reset()
                        consumptionPlotLine.addDataPoints(plotPoints)
                        summary_consumption_plot.setPlotMarkers(plotMarkers)
                        summary_consumption_plot.dimensionRestriction = appPreferences.distanceUnit.asUnit(((appPreferences.distanceUnit.toUnit(session.driven_distance.toFloat()) / MainActivity.DISTANCE_TRIP_DIVIDER).toInt() + 1) * MainActivity.DISTANCE_TRIP_DIVIDER) + 1
                        summary_consumption_plot.invalidate()
                        summary_consumption_plot.visibility = View.VISIBLE
                        summary_progress_bar.visibility = View.GONE
                    }
                }

                session.chargingSessions?.let {  chargingSessions ->
                    completedChargingSessions = if (chargingSessions.isNotEmpty()) {
                        chargingSessions.filter { it.end_epoch_time != null }
                    } else {
                        listOf()
                    }
                    requireActivity().runOnUiThread {

                        summary_view_selector.buttonList[1].text = "${getString(R.string.summary_charging_sessions)}: ${completedChargingSessions.size}"
                        summary_view_selector.buttonList[1].isEnabled = completedChargingSessions.isNotEmpty()

                        summary_charge_plot_seek_bar.max = (completedChargingSessions.size - 1).coerceAtLeast(0)
                        summary_charge_plot_seek_bar.progress = (completedChargingSessions.size - 1).coerceAtLeast(0)
                        summary_charge_plot_seek_bar.setOnSeekBarChangeListener(seekBarChangeListener)

                        summary_charge_plot_button_next.setOnClickListener {
                            val newProgress = summary_charge_plot_seek_bar.progress + 1
                            if (newProgress <= (completedChargingSessions.size - 1)) {
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

                        setVisibleChargeCurve(completedChargingSessions.size - 1)
                    }
                }
            }
        }



        if ((session.end_epoch_time?:0) > 0 ) {
            when (session.session_type) {
                TripType.MANUAL -> summary_title.setCompoundDrawables(applicationContext.getDrawable(R.drawable.ic_hand),null,null, null)
                TripType.SINCE_CHARGE -> summary_title.setCompoundDrawables(applicationContext.getDrawable(R.drawable.ic_charger),null,null, null)
                TripType.AUTO -> summary_title.setCompoundDrawables(applicationContext.getDrawable(R.drawable.ic_day),null,null, null)
                TripType.MONTH -> summary_title.setCompoundDrawables(applicationContext.getDrawable(R.drawable.ic_month),null,null, null)
            }
            summary_title.text = "${StringFormatters.getDateString(Date(session.start_epoch_time))}, ${resources.getStringArray(R.array.trip_type_names)[session.session_type]}"
        } else {
            when (session.session_type) {
                TripType.MANUAL -> summary_type_icon.setImageDrawable(applicationContext.getDrawable(R.drawable.ic_hand))
                TripType.SINCE_CHARGE -> summary_type_icon.setImageDrawable(applicationContext.getDrawable(R.drawable.ic_charger))
                TripType.AUTO -> summary_type_icon.setImageDrawable(applicationContext.getDrawable(R.drawable.ic_day))
                TripType.MONTH -> summary_type_icon.setImageDrawable(applicationContext.getDrawable(R.drawable.ic_month))
            }
            summary_title.visibility = View.GONE
            summary_trip_selector.visibility = View.VISIBLE
            summary_selector_title.text = resources.getStringArray(R.array.trip_type_names)[session.session_type]
            summary_selected_trip_bar.forEach { bar ->
                bar.background = getColorFromAttribute(requireContext(), R.attr.widget_background).toDrawable()
                // bar.background = applicationContext.getColor(R.color.club_night_variant).toDrawable()
            }
            // summary_selected_trip_bar[appPreferences.mainViewTrip].background = applicationContext.getDrawable(R.drawable.bg_button_selected)
            summary_selected_trip_bar[appPreferences.mainViewTrip].background = getColorFromAttribute(requireContext(), android.R.attr.colorControlActivated).toDrawable()
        }

        summary_trip_date_text.text = getString(R.string.summary_trip_start_date).format(StringFormatters.getDateString(Date(session.start_epoch_time)))
        summary_distance_widget.topText = StringFormatters.getTraveledDistanceString(session.driven_distance.toFloat())
        summary_altitude_widget.topText = StringFormatters.getAltitudeString(0f, 0f)
        summary_energy_widget.topText = StringFormatters.getEnergyString(session.used_energy.toFloat())
        summary_consumption_widget.topText = StringFormatters.getAvgConsumptionString(session.used_energy.toFloat(), session.driven_distance.toFloat())
        summary_time_widget.topText = StringFormatters.getElapsedTimeString(session.drive_time)
        summary_speed_widget.topText = StringFormatters.getAvgSpeedString(session.driven_distance.toFloat(), session.drive_time)

        summary_view_selector.selectedIndex = 0
    }

    private fun setSecondaryConsumptionPlotDimension(secondaryConsumptionDimension: Int) {
        if(requireActivity() is MainActivity) {
            (requireActivity() as MainActivity).setSecondaryConsumptionPlotDimension(secondaryConsumptionDimension)
        }

        val constraintSet = ConstraintSet()
        constraintSet.clone(summary_secondary_selector_container)

        when (secondaryConsumptionDimension) {
            1 -> {
                constraintSet.connect(R.id.summary_secondary_dimension_indicator, ConstraintSet.LEFT, R.id.summary_image_button_speed, ConstraintSet.LEFT)
                constraintSet.connect(R.id.summary_secondary_dimension_indicator, ConstraintSet.RIGHT, R.id.summary_image_button_speed, ConstraintSet.RIGHT)
                summary_secondary_dimension_indicator.isVisible = true
            }
            2 -> {
                constraintSet.connect(R.id.summary_secondary_dimension_indicator, ConstraintSet.LEFT, R.id.summary_image_button_soc, ConstraintSet.LEFT)
                constraintSet.connect(R.id.summary_secondary_dimension_indicator, ConstraintSet.RIGHT, R.id.summary_image_button_soc, ConstraintSet.RIGHT)
                summary_secondary_dimension_indicator.isVisible = true
            }
            3 -> {
                constraintSet.connect(R.id.summary_secondary_dimension_indicator, ConstraintSet.LEFT, R.id.summary_image_button_alt, ConstraintSet.LEFT)
                constraintSet.connect(R.id.summary_secondary_dimension_indicator, ConstraintSet.RIGHT, R.id.summary_image_button_alt, ConstraintSet.RIGHT)
                summary_secondary_dimension_indicator.isVisible = true
            }
            else -> {
                constraintSet.connect(R.id.summary_secondary_dimension_indicator, ConstraintSet.LEFT, R.id.summary_image_button_speed, ConstraintSet.RIGHT)
                constraintSet.connect(R.id.summary_secondary_dimension_indicator, ConstraintSet.RIGHT, R.id.summary_image_button_speed, ConstraintSet.RIGHT)
                summary_secondary_dimension_indicator.visibility = View.GONE
            }
        }
        constraintSet.applyTo(summary_secondary_selector_container)
        summary_consumption_plot.dimensionYSecondary = PlotDimensionY.IndexMap[secondaryConsumptionDimension]
        summary_consumption_plot.invalidate()
    }

    private fun setVisibleChargeCurve(progress: Int) {
        summary_charge_plot_view.reset()
        chargePlotLine.reset()

        if (completedChargingSessions.isEmpty() || progress >= completedChargingSessions.size || progress < 0 || completedChargingSessions[progress].chargingPoints?.isEmpty() == true) {
            summary_charge_plot_sub_title_curve.text = "%s (0/0)".format(
                getString(R.string.settings_sub_title_last_charge_plot))

            summary_charge_plot_button_next.isEnabled = false
            summary_charge_plot_button_next.alpha = CarStatsViewer.disabledAlpha
            summary_charge_plot_button_prev.isEnabled = false
            summary_charge_plot_button_prev.alpha = CarStatsViewer.disabledAlpha

            summary_charged_widget.topText = "-/-"
            summary_charge_time_widget.topText = "-/-"
            summary_temperature_widget.topText = "-/-"

            return
        }

        summary_charge_plot_sub_title_curve.text = "%s (%d/%d, %s)".format(
            getString(R.string.settings_sub_title_last_charge_plot),
            completedChargingSessions.size,
            completedChargingSessions.size,
            StringFormatters.getDateString(Date(completedChargingSessions.last().start_epoch_time))
        )

        if (completedChargingSessions.size - 1 != 0) {
            summary_charge_plot_sub_title_curve.text = "%s (%d/%d, %s)".format(
                getString(R.string.settings_sub_title_last_charge_plot),
                progress + 1,
                completedChargingSessions.size,
                StringFormatters.getDateString(Date(completedChargingSessions[progress].start_epoch_time)))

            when (progress) {
                0 -> {
                    summary_charge_plot_button_prev.isEnabled = false
                    summary_charge_plot_button_prev.alpha = CarStatsViewer.disabledAlpha
                    summary_charge_plot_button_next.isEnabled = true
                    summary_charge_plot_button_next.alpha = 1f
                }
                completedChargingSessions.size - 1 -> {
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

        if  ((completedChargingSessions[progress].chargingPoints?.filter { it.point_marker_type == 2}?.size?:0) > 1) {
            summary_charged_energy_warning_text.visibility = View.VISIBLE
        } else {
            summary_charged_energy_warning_text.visibility = View.GONE
        }

        summary_charged_widget.topText = String.format(
            "%s, %d%%  →  %d%%",
            StringFormatters.getEnergyString(completedChargingSessions[progress].charged_energy.toFloat()),
            ((completedChargingSessions[progress].chargingPoints?.first()?.state_of_charge?:0f)*100f).roundToInt(),
            ((completedChargingSessions[progress].chargingPoints?.last()?.state_of_charge?:0f)*100f).roundToInt()
        )
        summary_charge_time_widget.topText = StringFormatters.getElapsedTimeString((completedChargingSessions[progress].end_epoch_time?:0) - completedChargingSessions[progress].start_epoch_time)
        summary_temperature_widget.topText = StringFormatters.getTemperatureString(completedChargingSessions[progress].outside_temp)

        chargePlotLine.reset()
        completedChargingSessions[progress].chargingPoints?.let {
            if (it.isNotEmpty()) chargePlotLine.addDataPoints(DataConverters.chargePlotLineFromChargingPoints(it))
        }

        summary_charge_plot_view.dimensionRestriction = TimeUnit.MINUTES.toMillis((TimeUnit.MILLISECONDS.toMinutes((completedChargingSessions[progress].end_epoch_time?:0) - completedChargingSessions[progress].start_epoch_time) / 5) + 1) * 5 + 1
        summary_charge_plot_view.invalidate()
    }

    private fun switchToConsumptionLayout() {
        summary_consumption_container.visibility = View.VISIBLE
        summary_charge_container.visibility = View.GONE
        summary_consumption_container.scrollTo(0,0)
    }

    private fun switchToChargeLayout() {
        summary_consumption_container.visibility = View.GONE
        summary_charge_container.visibility = View.VISIBLE
        summary_charge_container.scrollTo(0,0)
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
                InAppLogger.d("[SUM] Changing trip")
                session = CarStatsViewer.tripDataSource.getFullDrivingSession(it)
                InAppLogger.d("Charging sessions: ${session.chargingSessions}")
                requireActivity().runOnUiThread {
                    applySession(session)
                }
            }
        }
    }
}