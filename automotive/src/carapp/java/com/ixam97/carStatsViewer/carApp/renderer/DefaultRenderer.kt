package com.ixam97.carStatsViewer.carApp.renderer

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import androidx.car.app.CarContext
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.dataProcessor.RealTimeData
import com.ixam97.carStatsViewer.database.tripData.DrivingPoint
import com.ixam97.carStatsViewer.ui.plot.enums.PlotDimensionSmoothingType
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
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.roundToInt

class DefaultRenderer(val carContext: CarContext): Renderer {

    companion object {
        // val polestarPS2Rect = Rect(0, 340, 999, 1183)
        val polestarPS2Rect = Rect(0, 297, 999, 1359) // Different rect in real car
        val volvoEX40Rect = Rect(0, 84, 670, 884)

        val layoutList = mapOf(
            "PS2" to polestarPS2Rect,
            "EX40" to volvoEX40Rect
        )
    }

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
    var debugFlag = false

    var useFixedSizes: Boolean = false
    var drawBoundingBoxes: Boolean = false
    var overrideLayout: String? = null

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
            valueTextSize = 110f
            descriptionTextSize = 28f
        }

        diagram.apply {
            mTextSize = carContext.resources.getDimension(R.dimen.reduced_font_size)
            mXMargin = carContext.resources.getDimension(R.dimen.plot_x_margin).toInt()
            mYMargin = carContext.resources.getDimension(R.dimen.plot_y_margin).toInt()
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
            InAppLogger.d("[$TAG] Visible area not available.")
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
            InAppLogger.d("[$TAG] Stable area not available.")
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
        val topCorrection = -120

        val isLandscape = (canvas.width > canvas.height)

        // InAppLogger.d("[$TAG] Rendering Frame")
        val density = CarStatsViewer.appContext.resources.displayMetrics.density
        // InAppLogger.d("[$TAG] Density: $density")
        // canvas.scale(density, density)

        var correctedArea = if (visibleArea != null) {
            if (!useFixedSizes) { visibleArea
                /*
                Rect(
                    visibleArea.left,
                    visibleArea.top,// + if (!isLandscape) topCorrection else 0,
                    visibleArea.right,
                    visibleArea.bottom
                )
                */
            } else {
                val layoutModel = overrideLayout?:CarStatsViewer.dataProcessor.staticVehicleData.modelName
                when (layoutModel) {
                    "PS2" -> polestarPS2Rect
                    "EX40", "XC40", "C40" -> volvoEX40Rect
                    "Speedy Model" -> polestarPS2Rect // volvoEX40Rect
                    else -> { visibleArea
                        /*
                        Rect(
                            visibleArea.left,
                            visibleArea.top,// + if (!isLandscape) topCorrection else 0,
                            visibleArea.right,
                            visibleArea.bottom
                        )
                         */
                    }
                }
            }//.applyDensity(density)
        } else null

        if (correctedArea != null) {
            if (drawBoundingBoxes) drawBoundingBox(canvas, correctedArea, correctedArea)

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
                dimensionSmoothing = 0.01f
                dimensionSmoothingType = PlotDimensionSmoothingType.PERCENTAGE
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
            correctedArea = moveDrawingArea(canvas, dx = 15f, dy = 15f, stableArea = correctedArea)

            var powerRect = powerGage.drawGage(canvas)
            var consRect = consGage.drawGage(canvas, xOffset = if (horizontalOverflowFlag || isLandscape) 0 else 320, yOffset = if (horizontalOverflowFlag || isLandscape) powerRect.bottom + 15 else 0)

            if ((consRect.right) > (correctedArea?.right?:0) && !horizontalOverflowFlag) {
                InAppLogger.w("[$TAG] Overflow detected: ${consRect.right}, ${(correctedArea?.right?:0)}. Setting flag for rearrangement")
                horizontalOverflowFlag = true
                canvas.drawColor(carContext.getColor(R.color.slideup_activity_background))
                powerRect = powerGage.drawGage(canvas)
                consRect = consGage.drawGage(canvas, xOffset = if (horizontalOverflowFlag || isLandscape) 0 else 320, yOffset = if (horizontalOverflowFlag || isLandscape) powerRect.bottom + 15 else 0)
            } else if ((consRect.right + powerRect.right) < (correctedArea?.right?:0) && horizontalOverflowFlag) {
                horizontalOverflowFlag = false
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

            // InAppLogger.d("[$TAG] Canvas dimensions: ${canvas.width} x ${canvas.height}")
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
            InAppLogger.w("[$TAG] Invalid drawing area movement!")
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

    fun refreshConsumptionPlot(drivingPoints: List<DrivingPoint> = emptyList()) {
        val appPreferences = CarStatsViewer.appPreferences
        InAppLogger.d("[$TAG] Refreshing entire consumption plot.")
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

    fun Rect.applyDensity(density: Float): Rect {
        return Rect(
            (this.left / density).toInt(),
            (this.top / density).toInt(),
            (this.right / density).toInt(),
            (this.bottom / density).toInt()
        )
    }
}