package com.ixam97.carStatsViewer.carApp.renderer

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import androidx.car.app.CarContext
import androidx.lifecycle.lifecycleScope
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.dataProcessor.RealTimeData
import com.ixam97.carStatsViewer.database.tripData.DrivingPoint
import com.ixam97.carStatsViewer.ui.plot.enums.PlotDimensionX
import com.ixam97.carStatsViewer.ui.plot.enums.PlotDimensionY
import com.ixam97.carStatsViewer.ui.plot.enums.PlotHighlightMethod
import com.ixam97.carStatsViewer.ui.plot.enums.PlotLineLabelFormat
import com.ixam97.carStatsViewer.ui.plot.graphics.PlotLinePaint
import com.ixam97.carStatsViewer.ui.plot.graphics.PlotPaint
import com.ixam97.carStatsViewer.ui.plot.objects.PlotLine
import com.ixam97.carStatsViewer.ui.plot.objects.PlotLineConfiguration
import com.ixam97.carStatsViewer.ui.plot.objects.PlotRange
import com.ixam97.carStatsViewer.ui.views.GageView
import com.ixam97.carStatsViewer.ui.views.PlotView
import com.ixam97.carStatsViewer.utils.DataConverters
import com.ixam97.carStatsViewer.utils.InAppLogger
import com.ixam97.carStatsViewer.utils.getColorFromAttribute
import kotlinx.android.synthetic.main.activity_main.main_consumption_gage
import kotlinx.android.synthetic.main.activity_main.main_consumption_plot
import kotlinx.coroutines.launch
import java.lang.Exception
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.roundToInt

class DefaultRenderer(val carContext: CarContext): Renderer {
    private val TAG = "DefaultRenderer"

    private val HORIZONTAL_TEXT_MARGIN = 10
    private val VERTICAL_TEXT_MARGIN_FROM_TOP = 20
    private val VERTICAL_TEXT_MARGIN_FROM_BOTTOM = 10

    private val mLeftInsetPaint = Paint()
    private val mRightInsetPaint = Paint()
    private val mCenterPaint = Paint()

    private var realTimeData: RealTimeData? = null

    private val mRect = Paint()

    private val powerGage = GageView(carContext)
    private val consGage = GageView(carContext)
    private val diagram = PlotView(carContext)

    private val consumptionPlotLine = PlotLine(
        PlotLineConfiguration(
            PlotRange(-200f, 600f, -200f, 600f, 100f, 0f),
            PlotLineLabelFormat.NUMBER,
            PlotHighlightMethod.AVG_BY_DISTANCE,
            "Wh/km"
        ),
    )
    private var consumptionPlotLinePaint: PlotLinePaint

    private var horizontalOverflowFlag = false

    init {
        mLeftInsetPaint.color = Color.RED
        mLeftInsetPaint.isAntiAlias = true
        mLeftInsetPaint.style = Paint.Style.STROKE
        mRightInsetPaint.color = Color.RED
        mRightInsetPaint.isAntiAlias = true
        mRightInsetPaint.style = Paint.Style.STROKE
        mRightInsetPaint.textAlign = Paint.Align.RIGHT
        mCenterPaint.color = Color.BLUE
        mCenterPaint.isAntiAlias = true
        mCenterPaint.style = Paint.Style.STROKE
        mRect.color = Color.BLACK
        mRect.style = Paint.Style.FILL

        consumptionPlotLinePaint  = PlotLinePaint(
            PlotPaint.byColor(carContext.getColor(R.color.primary_plot_color), carContext.resources.getDimension(R.dimen.reduced_font_size)),
            PlotPaint.byColor(carContext.getColor(R.color.secondary_plot_color), carContext.resources.getDimension(R.dimen.reduced_font_size)),
            PlotPaint.byColor(carContext.getColor(R.color.secondary_plot_color_alt), carContext.resources.getDimension(R.dimen.reduced_font_size))
        ) { CarStatsViewer.appPreferences.consumptionPlotSecondaryColor }

        powerGage.apply {
            gageName = carContext.getString(R.string.main_gage_power)
            gageUnit = "kW"
            maxValue = 300f
            minValue = -150f
            valueTextSize = 115f
            descriptionTextSize = 31f
        }

        consGage.apply {
            valueTextSize = 115f
            descriptionTextSize = 31f
        }

        diagram.apply {
            textSize = carContext.resources.getDimension(R.dimen.reduced_font_size)
            xMargin = carContext.resources.getDimension(R.dimen.plot_x_margin).toInt()
            yMargin = carContext.resources.getDimension(R.dimen.plot_y_margin).toInt()
            dimension = PlotDimensionX.DISTANCE
            addPlotLine(consumptionPlotLine, consumptionPlotLinePaint)
        }
    }

    override fun enable(onChangeListener: Runnable) {
        // Don't need to do anything here since renderFrame doesn't require any setup.
    }

    override fun disable() {
        // Don't need to do anything here since renderFrame doesn't require any setup.
    }

    fun setData(realTimeData: RealTimeData) {
        this.realTimeData = realTimeData
    }

    fun drawBoundingBox(
        canvas: Canvas,
        visibleArea: Rect?,
        stableArea: Rect?) {
        if (visibleArea != null) {
            if (visibleArea.isEmpty) {
                // No inset set. The entire area is considered safe to draw.
                visibleArea[0, 0, canvas.width - 1] = canvas.height - 1
            }
            canvas.drawRect(visibleArea, mLeftInsetPaint)
            canvas.drawLine(
                visibleArea.left.toFloat(),
                visibleArea.top.toFloat(),
                visibleArea.right.toFloat(),
                visibleArea.bottom.toFloat(),
                mLeftInsetPaint
            )
            canvas.drawLine(
                visibleArea.right.toFloat(),
                visibleArea.top.toFloat(),
                visibleArea.left.toFloat(),
                visibleArea.bottom.toFloat(),
                mLeftInsetPaint
            )
            canvas.drawText(
                "(" + visibleArea.left + " , " + visibleArea.top + ")",
                (
                        visibleArea.left + HORIZONTAL_TEXT_MARGIN).toFloat(),
                (
                        visibleArea.top + VERTICAL_TEXT_MARGIN_FROM_TOP).toFloat(),
                mLeftInsetPaint
            )
            canvas.drawText(
                "(" + visibleArea.right + " , " + visibleArea.bottom + ")",
                (
                        visibleArea.right - HORIZONTAL_TEXT_MARGIN).toFloat(),
                (
                        visibleArea.bottom - VERTICAL_TEXT_MARGIN_FROM_BOTTOM).toFloat(),
                mRightInsetPaint
            )
        } else {
            Log.d(TAG, "Visible area not available.")
        }
        if (stableArea != null) {
            // Draw a cross-hairs at the stable center.
            val lengthPx = 15
            val centerX = stableArea.centerX()
            val centerY = stableArea.centerY()
            canvas.drawLine(
                (centerX - lengthPx).toFloat(),
                centerY.toFloat(),
                (centerX + lengthPx).toFloat(),
                centerY.toFloat(),
                mCenterPaint
            )
            canvas.drawLine(
                centerX.toFloat(),
                (centerY - lengthPx).toFloat(),
                centerX.toFloat(),
                (centerY + lengthPx).toFloat(),
                mCenterPaint
            )
            canvas.drawText(
                "($centerX, $centerY)",
                (
                        centerX + HORIZONTAL_TEXT_MARGIN).toFloat(),
                centerY.toFloat(),
                mCenterPaint
            )
        } else {
            Log.d(TAG, "Stable area not available.")
        }
    }

    override fun renderFrame(
        canvas: Canvas,
        visibleArea: Rect?,
        stableArea: Rect?
    ) {
        /**
         * Can be used to offset compensate for the top Button row. Generally 90 compensates
         * the button row. However, on Landscape devices the buttons may be located at the left,
         * making this area unsafe to draw in.
         */
        val topCorrection = -90

        val isLandscape = (canvas.width > canvas.height)

        Log.d(TAG, "Rendering Frame")
        val density = CarStatsViewer.appContext.resources.displayMetrics.density
        Log.d(TAG, "Density: $density")
        canvas.scale(density, density)

        var correctedArea = if (stableArea != null) {
            Rect(
                (stableArea.left / density).toInt(),
                (stableArea.top / density).toInt() + if (!isLandscape) topCorrection else 0,
                (stableArea.right / density).toInt(),
                (stableArea.bottom / density).toInt())
        } else null

        if (visibleArea != null) {

            val consumptionUnit = CarStatsViewer.appPreferences.consumptionUnit
            val distanceUnit = CarStatsViewer.appPreferences.distanceUnit



            consGage.apply {
                gageName = carContext.getString(R.string.main_gage_consumption)
                if (consumptionUnit) {
                    gageUnit = "Wh/%s".format(distanceUnit.unit())
                    minValue = distanceUnit.asUnit(-300f)
                    maxValue = distanceUnit.asUnit(600f)
                } else {
                    gageUnit = "kWh/100%s".format(distanceUnit.unit())
                    minValue = distanceUnit.asUnit(-30f)
                    maxValue = distanceUnit.asUnit(60f)
                }
            }

            consumptionPlotLine.apply {
                if (consumptionUnit) {
                    Configuration.Unit = "Wh/%s".format(distanceUnit.unit())
                    Configuration.LabelFormat = PlotLineLabelFormat.NUMBER
                    Configuration.Divider = distanceUnit.toFactor() * 1f
                } else {
                    Configuration.Unit = "kWh/100%s".format(distanceUnit.unit())
                    Configuration.LabelFormat = PlotLineLabelFormat.FLOAT
                    Configuration.Divider = distanceUnit.toFactor() * 10f
                }
            }

            diagram.apply {
                val newDistance = when (CarStatsViewer.appPreferences.mainPrimaryDimensionRestriction) {
                    1 -> 40_000L
                    2 -> 100_000L
                    else -> 20_000L
                }
                dimensionRestriction = CarStatsViewer.appPreferences.distanceUnit.asUnit(newDistance)
                dimensionYSecondary = PlotDimensionY.IndexMap[CarStatsViewer.appPreferences.secondaryConsumptionDimension]
            }

            val powerInKw = (realTimeData?.power?:0f) / 1_000_000f
            if (powerInKw.absoluteValue >= 100) {
                powerGage.setValue(powerInKw.toInt())
            } else {
                powerGage.setValue(powerInKw)
            }

            val instCons = realTimeData?.instConsumption
            if (instCons != null && realTimeData?.speed!! * 3.6 > 3) {
                if (consumptionUnit) {
                    consGage.setValue(distanceUnit.asUnit(instCons).roundToInt())
                } else {
                    consGage.setValue(distanceUnit.asUnit(instCons) / 10)
                }
            } else {
                consGage.setValue(null as Float?)
            }

            correctedArea = moveDrawingArea(canvas, stableArea = correctedArea)
            correctedArea = moveDrawingArea(canvas, dx = 15f, dy = 0f, stableArea = correctedArea)

            var powerRect = powerGage.drawGage(canvas)
            // drawBoundingBox(canvas, powerRect, powerRect)

            var consRect = consGage.drawGage(canvas, xOffset = if (horizontalOverflowFlag || isLandscape) 0 else 320, yOffset = if (horizontalOverflowFlag || isLandscape) powerRect.bottom + 15 else 0)
            // drawBoundingBox(canvas, consRect, consRect)

            if (consRect.right > (correctedArea?.right?:0) && !horizontalOverflowFlag) {
                Log.w(TAG, "Overflow detected. Setting flag for rearrangement")
                horizontalOverflowFlag = true
                canvas.drawColor(carContext.getColor(R.color.slideup_activity_background))
                powerRect = powerGage.drawGage(canvas)
                consRect = consGage.drawGage(canvas, xOffset = if (horizontalOverflowFlag || isLandscape) 0 else 320, yOffset = if (horizontalOverflowFlag || isLandscape) powerRect.bottom + 15 else 0)
            }

            val plotRect = Rect(
                if (isLandscape) max(powerRect.right, consRect.right) + 25 else 0,
                if (isLandscape) topCorrection else (max(powerRect.bottom, consRect.bottom) + 25),
                correctedArea?.right?:0,
                correctedArea?.bottom?:0
            )

            correctedArea = moveDrawingArea(canvas, drawBox = false, stableArea = plotRect)

            Log.d(TAG, "Canvas dimensions: ${canvas.width} x ${canvas.height}")
            val diagramBounds: Rect? = if (correctedArea != null) {
                Rect(correctedArea.left,correctedArea.top, correctedArea.right, correctedArea.bottom - 15)
            } else null
            diagram.drawDiagram(canvas, diagramBounds)
        }

        // drawBoundingBox(canvas, correctedArea, correctedArea)

    }

    private fun moveDrawingArea(canvas: Canvas, dx: Float? = null, dy: Float? = null, stableArea: Rect? = null, drawBox: Boolean = false): Rect? {
        val newStableArea = if (dx != null && dy!= null) {
            canvas.translate(dx, dy)
            if (stableArea != null) {
                Rect(0, 0, stableArea.right - dx.toInt(), stableArea.bottom - dy.toInt())
            } else null
        } else if (stableArea != null) {
            val mDx = stableArea.left
            val mDy = stableArea.top
            canvas.translate(mDx.toFloat(), mDy.toFloat())
            Rect(0, 0, stableArea.right - mDx, stableArea.bottom - mDy)
        } else {
            Log.w(TAG, "Invalid drawing area movement!")
            null
        }

        if (drawBox) drawBoundingBox(canvas, newStableArea, newStableArea)
        return newStableArea
    }

    fun updateSession() {
        val session = CarStatsViewer.dataProcessor.selectedSessionData
        val appPreferences = CarStatsViewer.appPreferences

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
        }
    }

    private fun refreshConsumptionPlot(drivingPoints: List<DrivingPoint> = emptyList()) {
        val appPreferences = CarStatsViewer.appPreferences
        InAppLogger.d("Refreshing entire consumption plot.")
        var localDrivingPoints = drivingPoints
        if (drivingPoints.isEmpty()) {
            localDrivingPoints = CarStatsViewer.dataProcessor.selectedSessionDataFlow.value?.drivingPoints?: emptyList()
        }
        consumptionPlotLine.reset()
        if (localDrivingPoints.isEmpty()) return
        val newDistance = when (appPreferences.mainPrimaryDimensionRestriction) {
            1 -> 40_000L
            2 -> 100_000L
            else -> 20_000L
        }
        val dataPoints = DataConverters.consumptionPlotLineFromDrivingPoints(localDrivingPoints, appPreferences.distanceUnit.asUnit(newDistance.toFloat()))
        consumptionPlotLine.addDataPoints(dataPoints)
    }
}