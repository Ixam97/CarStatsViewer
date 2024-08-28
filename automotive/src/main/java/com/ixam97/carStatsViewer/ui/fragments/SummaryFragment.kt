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
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.forEach
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.database.tripData.ChargingSession
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.database.tripData.TripType
import com.ixam97.carStatsViewer.databinding.FragmentSummaryBinding
import com.ixam97.carStatsViewer.emulatorMode
import com.ixam97.carStatsViewer.ui.activities.MainActivity
import com.ixam97.carStatsViewer.ui.plot.enums.PlotDimensionSmoothingType
import com.ixam97.carStatsViewer.ui.plot.enums.PlotDimensionX
import com.ixam97.carStatsViewer.ui.plot.enums.PlotDimensionY
import com.ixam97.carStatsViewer.ui.plot.enums.PlotHighlightMethod
import com.ixam97.carStatsViewer.ui.plot.enums.PlotLineLabelFormat
import com.ixam97.carStatsViewer.ui.plot.enums.PlotMarkerType
import com.ixam97.carStatsViewer.ui.plot.enums.PlotSessionGapRendering
import com.ixam97.carStatsViewer.ui.plot.graphics.PlotLinePaint
import com.ixam97.carStatsViewer.ui.plot.graphics.PlotPaint
import com.ixam97.carStatsViewer.ui.plot.objects.PlotLine
import com.ixam97.carStatsViewer.ui.plot.objects.PlotLineConfiguration
import com.ixam97.carStatsViewer.ui.plot.objects.PlotRange
import com.ixam97.carStatsViewer.utils.DataConverters
import com.ixam97.carStatsViewer.utils.InAppLogger
import com.ixam97.carStatsViewer.utils.StringFormatters
import com.ixam97.carStatsViewer.utils.getColorFromAttribute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt


class SummaryFragment() : Fragment(R.layout.fragment_summary) {

    private var _binding: FragmentSummaryBinding? = null
    private val binding get() = _binding!!

    lateinit var session: DrivingSession
    private var completedChargingSessions = listOf<ChargingSession>()

    constructor(session: DrivingSession) :this() {
        this.session = session.copy() // Using a copy of the session to not have extremely long driving points and charging sessions in the dataProcessor to limit memory usage.
    }

    val appPreferences = CarStatsViewer.appPreferences
    val applicationContext = CarStatsViewer.appContext
    private var primaryColor = CarStatsViewer.primaryColor

    private var chargePlotLine = PlotLine(
        PlotLineConfiguration(
            PlotRange(0f, 20f, 0f, 400f, 20f),
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = with(binding) {
        super.onViewCreated(view, savedInstanceState)

        consumptionPlotLinePaint = PlotLinePaint(
            PlotPaint.byColor(getColorFromAttribute(requireContext(), R.attr.primary_plot_color), CarStatsViewer.appContext.resources.getDimension(R.dimen.reduced_font_size)),
            PlotPaint.byColor(getColorFromAttribute(requireContext(), R.attr.secondary_plot_color), CarStatsViewer.appContext.resources.getDimension(R.dimen.reduced_font_size)),
            PlotPaint.byColor(getColorFromAttribute(requireContext(), R.attr.tertiary_plot_color), CarStatsViewer.appContext.resources.getDimension(R.dimen.reduced_font_size))
        ) { appPreferences.consumptionPlotSecondaryColor }

        setupPlots()
        setSecondaryConsumptionPlotDimension(appPreferences.secondaryConsumptionDimension)
        setupListeners()

        summaryViewSelector.buttonList[2].isEnabled = false

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

        summaryConsumptionPlot.setOnTouchListener { v, event ->
            disallowIntercept(v, event)
            false
        }
        summaryChargePlotView.setOnTouchListener { v, event ->
            disallowIntercept(v, event)
            false
        }

        // check if a session has been provided, else close fragment
        if (this@SummaryFragment::session.isInitialized) {
            applySession(session)
        } else {
            requireActivity().supportFragmentManager.commit { remove(this@SummaryFragment) }
        }

        summarySecondaryDimensionIndicator.background = if (appPreferences.consumptionPlotSecondaryColor) {
            getColorFromAttribute(requireContext(), R.attr.tertiary_plot_color).toDrawable()
        } else {
            getColorFromAttribute(requireContext(), R.attr.secondary_plot_color).toDrawable()
        }

        // summary_button_dist_all.text = "${appPreferences.distanceUnit.toUnit(((session.driven_distance.toLong() / 5_000) + 1) * 5)} ${appPreferences.distanceUnit.unit()}"
    }

    private fun setupListeners() = with(binding) {
        setupHeader()

        summaryButtonReset.setOnClickListener {
            createResetDialog()
        }

        summaryViewSelector.setOnIndexChangedListener{
            when (summaryViewSelector.selectedIndex) {
                0 -> switchToConsumptionLayout()
                1 -> switchToChargeLayout()
            }
        }

        summaryButtonBack.setOnClickListener {
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

        summaryButtonDist20.setOnClickListener {
            summaryConsumptionPlot.dimensionRestriction = appPreferences.distanceUnit.asUnit(20_000) + 1
            summaryConsumptionPlot.dimensionShift = 0
        }
        summaryButtonDist40.setOnClickListener {
            summaryConsumptionPlot.dimensionRestriction = appPreferences.distanceUnit.asUnit(40_000) + 1
            summaryConsumptionPlot.dimensionShift = 0
        }
        summaryButtonDist100.setOnClickListener {
            summaryConsumptionPlot.dimensionRestriction = appPreferences.distanceUnit.asUnit(100_000) + 1
            summaryConsumptionPlot.dimensionShift = 0
        }
        summaryButtonDistAll.setOnClickListener {
            summaryConsumptionPlot.dimensionRestriction = appPreferences.distanceUnit.asUnit(((session.driven_distance.toLong() / 5_000) + 1) * 5_000) + 1
            summaryConsumptionPlot.dimensionShift = 0
        }

        summaryImageButtonSpeed.setOnClickListener {
            var currentIndex = appPreferences.secondaryConsumptionDimension
            currentIndex = if (currentIndex == 1) 0 else 1
            setSecondaryConsumptionPlotDimension(currentIndex)
            appPreferences.secondaryConsumptionDimension = currentIndex
        }

        summaryImageButtonSoc.setOnClickListener {
            var currentIndex = appPreferences.secondaryConsumptionDimension
            currentIndex = if (currentIndex == 2) 0 else 2
            setSecondaryConsumptionPlotDimension(currentIndex)
            appPreferences.secondaryConsumptionDimension = currentIndex
        }

        summaryImageButtonAlt.setOnClickListener {
            var currentIndex = appPreferences.secondaryConsumptionDimension
            currentIndex = if (currentIndex == 3) 0 else 3
            setSecondaryConsumptionPlotDimension(currentIndex)
            appPreferences.secondaryConsumptionDimension = currentIndex
        }
    }

    private fun setupHeader() = with(binding) {
        summaryButtonTripPrev.setOnClickListener {
            var tripIndex = appPreferences.mainViewTrip
            tripIndex--
            if (tripIndex < 0) tripIndex = 3
            appPreferences.mainViewTrip = tripIndex
            changeSelectedTrip(tripIndex)
        }

        summaryButtonTripNext.setOnClickListener {
            var tripIndex = appPreferences.mainViewTrip
            tripIndex++
            if (tripIndex > 3) tripIndex = 0
            appPreferences.mainViewTrip = tripIndex
            changeSelectedTrip(tripIndex)
        }

        summarySelectorTitle.setOnClickListener {
            var tripIndex = appPreferences.mainViewTrip
            tripIndex++
            if (tripIndex > 3) tripIndex = 0
            appPreferences.mainViewTrip = tripIndex
            changeSelectedTrip(tripIndex)
        }
    }

    private fun setupPlots() = with(binding) {
        if (appPreferences.consumptionUnit) {
            consumptionPlotLine.Configuration.Unit = "Wh/%s".format(appPreferences.distanceUnit.unit())
            consumptionPlotLine.Configuration.LabelFormat = PlotLineLabelFormat.NUMBER
            consumptionPlotLine.Configuration.Divider = appPreferences.distanceUnit.toFactor() * 1f
        } else {
            consumptionPlotLine.Configuration.Unit = "kWh/100%s".format(appPreferences.distanceUnit.unit())
            consumptionPlotLine.Configuration.LabelFormat = PlotLineLabelFormat.FLOAT
            consumptionPlotLine.Configuration.Divider = appPreferences.distanceUnit.toFactor() * 10f
        }

        summaryConsumptionPlot.dimension = PlotDimensionX.DISTANCE
        summaryConsumptionPlot.dimensionRestrictionMin = appPreferences.distanceUnit.asUnit(
            MainActivity.DISTANCE_TRIP_DIVIDER)
        summaryConsumptionPlot.dimensionSmoothing = 0.02f
        summaryConsumptionPlot.dimensionSmoothingType = PlotDimensionSmoothingType.PERCENTAGE
        summaryConsumptionPlot.visibleMarkerTypes.add(PlotMarkerType.CHARGE)
        summaryConsumptionPlot.visibleMarkerTypes.add(PlotMarkerType.PARK)

        summaryConsumptionPlot.addPlotLine(consumptionPlotLine, consumptionPlotLinePaint)
        summaryConsumptionPlot.sessionGapRendering = PlotSessionGapRendering.JOIN

        summaryChargePlotView.dimension = PlotDimensionX.TIME
        summaryChargePlotView.dimensionRestrictionMin = TimeUnit.MINUTES.toMillis(5)
        summaryChargePlotView.dimensionSmoothing = 0.01f
        summaryChargePlotView.dimensionSmoothingType = PlotDimensionSmoothingType.PERCENTAGE
        summaryChargePlotView.dimensionYSecondary = PlotDimensionY.STATE_OF_CHARGE

        summaryChargePlotView.addPlotLine(chargePlotLine, chargePlotLinePaint)
        summaryChargePlotView.sessionGapRendering = PlotSessionGapRendering.GAP
    }

    private fun applySession(session: DrivingSession) = with(binding) {
        switchToConsumptionLayout()
        summaryViewSelector.buttonList[1].isEnabled = false

        if (session.session_type != TripType.MANUAL) {
            summaryButtonReset.isEnabled = false
            summaryButtonReset.setColorFilter(applicationContext.getColor(R.color.disabled_tint), PorterDuff.Mode.SRC_IN)
        } else {
            summaryButtonReset.isEnabled = true
            summaryButtonReset.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
        }

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                requireActivity().runOnUiThread {
                    summaryConsumptionPlot.visibility = View.GONE
                    summaryProgressBar.visibility = View.VISIBLE
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
                        summaryAltitudeWidget.topText = StringFormatters.getAltitudeString(altUp, altDown)
                        consumptionPlotLine.reset()
                        consumptionPlotLine.addDataPoints(plotPoints)
                        summaryConsumptionPlot.setPlotMarkers(plotMarkers)
                        summaryConsumptionPlot.dimensionRestriction = appPreferences.distanceUnit.asUnit(((appPreferences.distanceUnit.toUnit(session.driven_distance.toFloat()) / MainActivity.DISTANCE_TRIP_DIVIDER).toInt() + 1) * MainActivity.DISTANCE_TRIP_DIVIDER) + 1
                        summaryConsumptionPlot.invalidate()
                        summaryConsumptionPlot.visibility = View.VISIBLE
                        summaryProgressBar.visibility = View.GONE
                    }
                }

                session.chargingSessions?.let {  chargingSessions ->
                    completedChargingSessions = if (chargingSessions.isNotEmpty()) {
                        chargingSessions.filter { it.end_epoch_time != null }
                    } else {
                        listOf()
                    }
                    requireActivity().runOnUiThread {

                        summaryViewSelector.buttonList[1].text = "${getString(R.string.summary_charging_sessions)}: ${completedChargingSessions.size}"
                        summaryViewSelector.buttonList[1].isEnabled = completedChargingSessions.isNotEmpty()

                        summaryChargePlotSeekBar.max = (completedChargingSessions.size - 1).coerceAtLeast(0)
                        summaryChargePlotSeekBar.progress = (completedChargingSessions.size - 1).coerceAtLeast(0)
                        summaryChargePlotSeekBar.setOnSeekBarChangeListener(seekBarChangeListener)

                        summaryChargePlotButtonNext.setOnClickListener {
                            val newProgress = summaryChargePlotSeekBar.progress + 1
                            if (newProgress <= (completedChargingSessions.size - 1)) {
                                summaryChargePlotSeekBar.progress = newProgress
                            }
                            summaryChargePlotView.dimensionShift = 0L
                        }

                        summaryChargePlotButtonPrev.setOnClickListener {
                            val newProgress = summaryChargePlotSeekBar.progress - 1
                            if (newProgress >= 0) {
                                summaryChargePlotSeekBar.progress = newProgress
                            }
                            summaryChargePlotView.dimensionShift = 0L
                        }

                        setVisibleChargeCurve(completedChargingSessions.size - 1)
                    }
                }
            }
        }



        if ((session.end_epoch_time?:0) > 0 ) {
            when (session.session_type) {
                TripType.MANUAL -> summaryTitle.setCompoundDrawables(applicationContext.getDrawable(R.drawable.ic_hand),null,null, null)
                TripType.SINCE_CHARGE -> summaryTitle.setCompoundDrawables(applicationContext.getDrawable(R.drawable.ic_charger),null,null, null)
                TripType.AUTO -> summaryTitle.setCompoundDrawables(applicationContext.getDrawable(R.drawable.ic_day),null,null, null)
                TripType.MONTH -> summaryTitle.setCompoundDrawables(applicationContext.getDrawable(R.drawable.ic_month),null,null, null)
            }
            summaryTitle.text = "${StringFormatters.getDateString(Date(session.start_epoch_time))}, ${resources.getStringArray(R.array.trip_type_names)[session.session_type]}"
        } else {
            when (session.session_type) {
                TripType.MANUAL -> summaryTypeIcon.setImageDrawable(applicationContext.getDrawable(R.drawable.ic_hand))
                TripType.SINCE_CHARGE -> summaryTypeIcon.setImageDrawable(applicationContext.getDrawable(R.drawable.ic_charger))
                TripType.AUTO -> summaryTypeIcon.setImageDrawable(applicationContext.getDrawable(R.drawable.ic_day))
                TripType.MONTH -> summaryTypeIcon.setImageDrawable(applicationContext.getDrawable(R.drawable.ic_month))
            }
            summaryTitle.visibility = View.GONE
            summaryTripSelector.visibility = View.VISIBLE
            summarySelectorTitle.text = resources.getStringArray(R.array.trip_type_names)[session.session_type]
            summarySelectedTripBar.forEach { bar ->
                bar.background = getColorFromAttribute(requireContext(), R.attr.widget_background).toDrawable()
                // bar.background = applicationContext.getColor(R.color.club_night_variant).toDrawable()
            }
            // summary_selected_trip_bar[appPreferences.mainViewTrip].background = applicationContext.getDrawable(R.drawable.bg_button_selected)
            summarySelectedTripBar[appPreferences.mainViewTrip].background = getColorFromAttribute(requireContext(), android.R.attr.colorControlActivated).toDrawable()
        }

        summaryTripDateText.text = getString(R.string.summary_trip_start_date).format(StringFormatters.getDateString(Date(session.start_epoch_time)))
        summaryDistanceWidget.topText = StringFormatters.getTraveledDistanceString(session.driven_distance.toFloat())
        summaryAltitudeWidget.topText = StringFormatters.getAltitudeString(0f, 0f)
        summaryEnergyWidget.topText = StringFormatters.getEnergyString(session.used_energy.toFloat())
        summaryConsumptionWidget.topText = StringFormatters.getAvgConsumptionString(session.used_energy.toFloat(), session.driven_distance.toFloat())
        summaryTimeWidget.topText = StringFormatters.getElapsedTimeString(session.drive_time)
        summarySpeedWidget.topText = StringFormatters.getAvgSpeedString(session.driven_distance.toFloat(), session.drive_time)

        summaryViewSelector.selectedIndex = 0
    }

    private fun setSecondaryConsumptionPlotDimension(secondaryConsumptionDimension: Int) = with(binding) {
        if(requireActivity() is MainActivity) {
            (requireActivity() as MainActivity).setSecondaryConsumptionPlotDimension(secondaryConsumptionDimension)
        }

        val constraintSet = ConstraintSet()
        constraintSet.clone(summarySecondarySelectorContainer)

        when (secondaryConsumptionDimension) {
            1 -> {
                constraintSet.connect(R.id.summary_secondary_dimension_indicator, ConstraintSet.LEFT, R.id.summary_image_button_speed, ConstraintSet.LEFT)
                constraintSet.connect(R.id.summary_secondary_dimension_indicator, ConstraintSet.RIGHT, R.id.summary_image_button_speed, ConstraintSet.RIGHT)
                summarySecondaryDimensionIndicator.isVisible = true
            }
            2 -> {
                constraintSet.connect(R.id.summary_secondary_dimension_indicator, ConstraintSet.LEFT, R.id.summary_image_button_soc, ConstraintSet.LEFT)
                constraintSet.connect(R.id.summary_secondary_dimension_indicator, ConstraintSet.RIGHT, R.id.summary_image_button_soc, ConstraintSet.RIGHT)
                summarySecondaryDimensionIndicator.isVisible = true
            }
            3 -> {
                constraintSet.connect(R.id.summary_secondary_dimension_indicator, ConstraintSet.LEFT, R.id.summary_image_button_alt, ConstraintSet.LEFT)
                constraintSet.connect(R.id.summary_secondary_dimension_indicator, ConstraintSet.RIGHT, R.id.summary_image_button_alt, ConstraintSet.RIGHT)
                summarySecondaryDimensionIndicator.isVisible = true
            }
            else -> {
                constraintSet.connect(R.id.summary_secondary_dimension_indicator, ConstraintSet.LEFT, R.id.summary_image_button_speed, ConstraintSet.RIGHT)
                constraintSet.connect(R.id.summary_secondary_dimension_indicator, ConstraintSet.RIGHT, R.id.summary_image_button_speed, ConstraintSet.RIGHT)
                summarySecondaryDimensionIndicator.visibility = View.GONE
            }
        }
        constraintSet.applyTo(summarySecondarySelectorContainer)
        summaryConsumptionPlot.dimensionYSecondary = PlotDimensionY.IndexMap[secondaryConsumptionDimension]
        summaryConsumptionPlot.invalidate()
    }

    private fun setVisibleChargeCurve(progress: Int) = with(binding) {
        summaryChargePlotView.reset()
        chargePlotLine.reset()

        if (completedChargingSessions.isEmpty() || progress >= completedChargingSessions.size || progress < 0 || completedChargingSessions[progress].chargingPoints?.isEmpty() == true) {
            summaryChargePlotSubTitleCurve.text = "%s (0/0)".format(
                getString(R.string.settings_sub_title_last_charge_plot))

            summaryChargePlotButtonNext.isEnabled = false
            summaryChargePlotButtonNext.alpha = CarStatsViewer.disabledAlpha
            summaryChargePlotButtonPrev.isEnabled = false
            summaryChargePlotButtonPrev.alpha = CarStatsViewer.disabledAlpha

            summaryChargedWidget.topText = "-/-"
            summaryChargeTimeWidget.topText = "-/-"
            summaryTemperatureWidget.topText = "-/-"

            return
        }

        summaryChargePlotSubTitleCurve.text = "${getString(R.string.settings_sub_title_last_charge_plot)} (${completedChargingSessions.size}/${completedChargingSessions.size}, ${StringFormatters.getDateString(Date(completedChargingSessions.last().start_epoch_time))}"

        if (completedChargingSessions.size - 1 != 0) {
            summaryChargePlotSubTitleCurve.text = "${getString(R.string.settings_sub_title_last_charge_plot)} (${progress + 1}/${completedChargingSessions.size}, ${StringFormatters.getDateString(Date(completedChargingSessions.last().start_epoch_time))}"

            when (progress) {
                0 -> {
                    summaryChargePlotButtonPrev.isEnabled = false
                    summaryChargePlotButtonPrev.alpha = CarStatsViewer.disabledAlpha
                    summaryChargePlotButtonNext.isEnabled = true
                    summaryChargePlotButtonNext.alpha = 1f
                }
                completedChargingSessions.size - 1 -> {
                    summaryChargePlotButtonNext.isEnabled = false
                    summaryChargePlotButtonNext.alpha = CarStatsViewer.disabledAlpha
                    summaryChargePlotButtonPrev.isEnabled = true
                    summaryChargePlotButtonPrev.alpha = 1f
                }
                else -> {
                    summaryChargePlotButtonNext.isEnabled = true
                    summaryChargePlotButtonNext.alpha = 1f
                    summaryChargePlotButtonPrev.isEnabled = true
                    summaryChargePlotButtonPrev.alpha = 1f
                }
            }
        }

        if  ((completedChargingSessions[progress].chargingPoints?.filter { it.point_marker_type == 2}?.size?:0) > 1) {
            summaryChargedEnergyWarningText.visibility = View.VISIBLE
        } else {
            summaryChargedEnergyWarningText.visibility = View.GONE
        }

        summaryChargedWidget.topText = String.format(
            "%s, %d%%  →  %d%%",
            StringFormatters.getEnergyString(completedChargingSessions[progress].charged_energy.toFloat()),
            ((completedChargingSessions[progress].chargingPoints?.first()?.state_of_charge?:0f)*100f).roundToInt(),
            ((completedChargingSessions[progress].chargingPoints?.last()?.state_of_charge?:0f)*100f).roundToInt()
        )
        summaryChargeTimeWidget.topText = StringFormatters.getElapsedTimeString((completedChargingSessions[progress].end_epoch_time?:0) - completedChargingSessions[progress].start_epoch_time)
        summaryTemperatureWidget.topText = StringFormatters.getTemperatureString(completedChargingSessions[progress].outside_temp)

        chargePlotLine.reset()
        completedChargingSessions[progress].chargingPoints?.let {
            if (it.isNotEmpty()) chargePlotLine.addDataPoints(DataConverters.chargePlotLineFromChargingPoints(it))
        }

        summaryChargePlotView.dimensionRestriction = TimeUnit.MINUTES.toMillis((TimeUnit.MILLISECONDS.toMinutes((completedChargingSessions[progress].end_epoch_time?:0) - completedChargingSessions[progress].start_epoch_time) / 5) + 1) * 5 + 1
        summaryChargePlotView.invalidate()
    }

    private fun switchToConsumptionLayout() = with(binding) {
        summaryConsumptionContainer.visibility = View.VISIBLE
        summaryChargeContainer.visibility = View.GONE
        summaryConsumptionContainer.scrollTo(0,0)
    }

    private fun switchToChargeLayout() = with(binding) {
        summaryConsumptionContainer.visibility = View.GONE
        summaryChargeContainer.visibility = View.VISIBLE
        summaryChargeContainer.scrollTo(0,0)
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