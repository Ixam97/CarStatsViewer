package com.ixam97.carStatsViewer.ui.views

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getColor
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.appPreferences.AppPreferences
import com.ixam97.carStatsViewer.ui.plot.enums.*
import com.ixam97.carStatsViewer.ui.plot.graphics.PlotLinePaint
import com.ixam97.carStatsViewer.ui.plot.graphics.PlotMarkerPaint
import com.ixam97.carStatsViewer.ui.plot.graphics.PlotPaint
import com.ixam97.carStatsViewer.ui.plot.objects.*
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class PlotView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    // companion object {
    //     var textSize = 26f
    //     var xMargin = 0
    //     var yMargin = 0
    // }

    var textSize: Float
    var xMargin: Int
    var yMargin: Int

    /*var xMargin: Int = 100
        set(value) {
            val diff = value != field
            if (value > 0) {
                field = value
                if (diff) invalidate()
            }
        }*/

    var xLineCount: Int = 6
        set(value) {
            val diff = value != field
            if (value > 1) {
                field = value
                if (diff) invalidate()
            }
        }

    /*var yMargin: Int = 60
        set(value) {
            val diff = value != field
            if (value > 0) {
                field = value
                if (diff) invalidate()
            }
        }*/

    var yLineCount: Int = 5
        set(value) {
            val diff = value != field
            if (value > 1) {
                field = value
                if (diff) invalidate()
            }
        }

    var dimension: PlotDimensionX = PlotDimensionX.INDEX
        set(value) {
            val diff = value != field
            field = value
            if (diff) invalidate()
        }

    var dimensionYPrimary: PlotDimensionY? = null
        set(value) {
            val diff = value != field
            field = value
            if (diff) invalidate()
        }

    var dimensionYSecondary: PlotDimensionY? = null
        set(value) {
            val diff = value != field
            field = value
            if (diff) invalidate()
        }

    var dimensionYAdditional: ArrayList<PlotDimensionY?>? = null

    var dimensionRestrictionMin: Long? = null
    var dimensionRestriction: Long? = null
        set(value) {
            val diff = value != field
            field = value
            if (diff) invalidate()
        }

    var dimensionShift: Long? = null
        set(value) {
            val diff = value != field
            field = value
            if (diff) invalidate()
        }

    var dimensionSmoothing: Float? = null
        set(value) {
            val diff = value != field
            field = value
            if (diff) invalidate()
        }

    var dimensionSmoothingType: PlotDimensionSmoothingType? = null
        set(value) {
            val diff = value != field
            field = value
            if (diff) invalidate()
        }

    private var dimensionHighlightAt : Float? = null
        set(value) {
            val coerce = value
                ?.coerceAtLeast(xMargin.toFloat())
                ?.coerceAtMost((width - xMargin).toFloat())

            val diff = coerce != field
            field = coerce
            if (diff) {
                dimensionHighlightAtPercentage = when (coerce) {
                    null -> null
                    else -> (1f / (width.toFloat() - 2 * xMargin) * (coerce - xMargin))
                        .coerceAtLeast(0f)
                        .coerceAtMost(1f)
                }
                invalidate()
            }
        }

    private var dimensionHighlightAtPercentage : Float? = null

    private var dimensionHighlightValue : HashMap<PlotLine, HashMap<PlotDimensionY?, Float?>> = HashMap()

    var visibleMarkerTypes: HashSet<PlotMarkerType> = HashSet()

    var sessionGapRendering : PlotSessionGapRendering = PlotSessionGapRendering.JOIN
        set(value) {
            val diff = value != field
            field = value
            if (diff) invalidate()
        }

    private val plotLines = ArrayList<Pair<PlotLine, PlotLinePaint>>()
    private var plotMarkers : PlotMarkers? = null

    private lateinit var labelPaint: Paint
    private lateinit var labelLinePaint: Paint
    private lateinit var borderLinePaint: Paint
    private lateinit var baseLinePaint: Paint
    private lateinit var backgroundPaint: Paint
    private lateinit var dimensionHighlightLinePaint: Paint

    private var markerPaint = HashMap<PlotMarkerType, PlotMarkerPaint>()
    private var markerIcon = HashMap<PlotMarkerType, Bitmap?>()

    private val appPreferences : AppPreferences

    init {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.PlotView)
        try {
            textSize = attributes.getDimension(R.styleable.PlotView_baseTextSize, 26f)
            xMargin = attributes.getDimension(R.styleable.PlotView_xMargin, 0f).toInt()
            yMargin = attributes.getDimension(R.styleable.PlotView_yMargin, 0f).toInt()
        } finally {
            attributes.recycle()
        }
        setupPaint()
        appPreferences = AppPreferences(context)
    }

    // Setup paint with color and stroke styles
    private fun setupPaint() {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorControlActivated, typedValue, true)

        val basePaint = PlotPaint.basePaint(textSize)

        labelLinePaint = Paint(basePaint)
        labelLinePaint.color = getColor(context, R.color.grid_line_color)

        borderLinePaint = Paint(basePaint)
        borderLinePaint.color = Color.GRAY

        labelPaint = Paint(borderLinePaint)
        labelPaint.style = Paint.Style.FILL

        baseLinePaint = Paint(borderLinePaint)
        baseLinePaint.color = Color.LTGRAY

        backgroundPaint = Paint(basePaint)
        backgroundPaint.color = Color.BLACK
        backgroundPaint.style = Paint.Style.FILL

        dimensionHighlightLinePaint = Paint(borderLinePaint)
        dimensionHighlightLinePaint.strokeWidth = 3f
        dimensionHighlightLinePaint.pathEffect = DashPathEffect(floatArrayOf(5f, 10f), 0f)

        for (type in PlotMarkerType.values()) {
            when (type) {
                PlotMarkerType.CHARGE -> {
                    markerPaint[type] = PlotMarkerPaint.byColor(getColor(context, R.color.charge_marker), textSize)
                    markerIcon[type] = getVectorBitmap(context, R.drawable.ic_marker_charge)
                }
                PlotMarkerType.PARK -> {
                    markerPaint[type] = PlotMarkerPaint.byColor(getColor(context, R.color.park_marker), textSize)
                    markerIcon[type] = getVectorBitmap(context, R.drawable.ic_marker_park)
                }
            }
        }
    }

    private fun getVectorBitmap(context: Context, drawableId: Int): Bitmap? {
        when (val drawable = ContextCompat.getDrawable(context, drawableId)) {
            is BitmapDrawable -> {
                return drawable.bitmap
            }

            is VectorDrawable -> {
                val bitmap = Bitmap.createBitmap(
                    drawable.intrinsicWidth,
                    drawable.intrinsicHeight,
                    Bitmap.Config.ARGB_8888
                )

                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)

                val factor = textSize / canvas.width

                return Bitmap.createScaledBitmap(
                    bitmap,
                    (canvas.width * factor).roundToInt(),
                    (canvas.height * factor).roundToInt(),
                    true
                )
            }
        }

        return null
    }

    fun reset() {
        for (item in plotLines) {
            item.first.reset()
        }

        plotMarkers?.markers?.clear()

        invalidate()
    }

    fun addPlotLine(plotLine: PlotLine, plotLinePaint: PlotLinePaint) {
        plotLines.add(Pair(plotLine, plotLinePaint))
        invalidate()
    }

    fun removePlotLine(plotLine: PlotLine?) {
        plotLines.removeAll { it.first == plotLine }
        invalidate()
    }

    fun removeAllPlotLine() {
        plotLines.clear()
        invalidate()
    }

    fun setPlotMarkers(plotMarkers: PlotMarkers) {
        this.plotMarkers = plotMarkers
        invalidate()
    }

    private fun x(index: Any?, min: Any, max: Any, maxX: Float): Float? {
        if (min is Float) {
            return x(index as Float?, min, max as Float, maxX)
        }

        if (min is Long) {
            return x(index as Long?, min, max as Long, maxX)
        }

        return null
    }

    private fun x(index: Long?, min: Long, max: Long, maxX: Float): Float? {
        return x(PlotLineItem.cord(index, min, max), maxX)
    }

    private fun x(index: Float?, min: Float, max: Float, maxX: Float): Float? {
        return x(PlotLineItem.cord(index, min, max), maxX)
    }

    private fun x(value: Float?, maxX: Float): Float? {
        return when (value) {
            null -> null
            else -> xMargin + (maxX - (2 * xMargin)) * value
        }
    }

    private fun y(index: Float?, min: Float, max: Float, maxY: Float): Float? {
        return y(PlotLineItem.cord(index, min, max), maxY)
    }

    private fun y(value: Float?, maxY: Float): Float? {
        return when (value) {
            null -> null
            else -> {
                val px = maxY - (2 * yMargin)
                yMargin + px - px * value
            }
        }
    }

    private val mScaleGestureDetector = object : SimpleOnScaleGestureListener() {
        private var lastSpanX: Float = 0f
        private var lastRestriction: Long? = 0L

        override fun onScaleBegin(scaleGestureDetector: ScaleGestureDetector): Boolean {
            lastSpanX = scaleGestureDetector.currentSpanX
            lastRestriction = dimensionRestriction
            return true
        }

        override fun onScale(scaleGestureDetector: ScaleGestureDetector): Boolean {
            val spanX: Float = scaleGestureDetector.currentSpanX
            val restrictionMin = dimensionRestrictionMin ?: return true

            val center = (dimensionShift?: 0L) + (dimensionRestriction?: 0L) / 2L

            val scalingFraction = when (dimensionRestriction) {
                in 0L..24_999L -> 100L
                in 25_000..99_999L -> 1_000L
                else -> 5_000L
            }

            if (lastRestriction == null || (lastSpanX/spanX).isInfinite()) return true

            val targetDimensionRestriction = ((lastRestriction!!.toFloat() * (lastSpanX / spanX)).toLong())
                .coerceAtMost(touchDimensionMax)
                .coerceAtLeast(restrictionMin)

            dimensionRestriction = (((targetDimensionRestriction / scalingFraction) * scalingFraction) / 100L) * 100L

            val shift = center - (dimensionRestriction?: center) / 2L // dimensionShift ?: return true

            Log.d("PLOT", "Shift: $shift")
            Log.d("PLOT", "dimension restriction: $dimensionRestriction")

            dimensionShift = shift
                .coerceAtMost(touchDimensionMax - dimensionRestriction!!)
                .coerceAtLeast(0L)

            return true
        }
    }

    private val mScrollGestureListener = object : GestureDetector.SimpleOnGestureListener() {
        private val shiftingFraction = 50L

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            touchDimensionShiftDistance += distanceX * touchDistanceMultiplier

            val targetDimensionShift = (((touchDimensionShift + touchDimensionShiftDistance * touchDimensionShiftByPixel) / ((dimensionRestriction?:shiftingFraction) / shiftingFraction)).toLong() * ((dimensionRestriction?:shiftingFraction) / shiftingFraction))
                .coerceAtMost(touchDimensionMax - (dimensionRestriction ?: 0L))
                .coerceAtLeast(0L)

            dimensionShift = (targetDimensionShift / 100L) * 100L

            Log.d("PLOT", "Shift: $dimensionShift")

            return true
        }
    }

    private val mTapGestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (dimensionHighlightAt != null) {
                dimensionHighlightAt = e.x
            }
            return super.onSingleTapConfirmed(e)
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            dimensionHighlightAt = when (dimensionHighlightAt) {
                null -> e.x
                else -> null
            }
            return super.onDoubleTap(e)
        }
    }

    private val mScrollDetector = GestureDetector(context, mScrollGestureListener)
    private val mTapDetector = GestureDetector(context, mTapGestureListener)
    private val mScaleDetector = ScaleGestureDetector(context, mScaleGestureDetector)

    // invert direction
    private var touchDistanceMultiplier : Float = -1f
    private var touchDimensionShift : Long = 0L
    private var touchDimensionShiftDistance : Float = 0f
    private var touchDimensionShiftByPixel : Float = 0f
    private var touchDimensionMax : Long = 0L
    private var touchGesture : Boolean = false

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val restriction = dimensionRestriction ?: return true
        val min = dimensionRestrictionMin ?: return true

        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                touchDistanceMultiplier = when (dimension.toPlotDirection()) {
                    PlotDirection.LEFT_TO_RIGHT -> 1f
                    PlotDirection.RIGHT_TO_LEFT -> -1f
                }
                touchDimensionShift = dimensionShift ?: 0L
                touchDimensionShiftDistance = 0f
                touchDimensionShiftByPixel = restriction.toFloat() / width.toFloat()

                val dimensionMax = plotLines.mapNotNull { it.first.distanceDimension(dimension) }.maxOfOrNull { it }?.toLong() ?: return true
                touchDimensionMax = dimensionMax + (min - dimensionMax % min)

                touchGesture = true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                touchGesture = false
            }
        }

        mTapDetector.onTouchEvent(ev)

        if (touchGesture) {
            mScrollDetector.onTouchEvent(ev)
            mScaleDetector.onTouchEvent(ev)
        }

        return true
    }

    override fun onDraw(canvas: Canvas) {
        Log.d("PLOT", "onDraw, dimensionRestriction: $dimensionRestriction, dimensionShift:$dimensionShift")
        super.onDraw(canvas)
        dataPointMap.clear()
        alignZero()
        drawBackground(canvas)
        drawXLines(canvas)
        drawYBaseLines(canvas)
        drawPlot(canvas)
        drawYLines(canvas)
    }

    private var dataPointMap : HashMap<PlotLine, List<PlotLineItem>> = HashMap()
    private fun dataPoints(plotLine: PlotLine?) : List<PlotLineItem>? {
        if (plotLine == null) return null
        Log.d("PLOT", "dataPointMap.containsKey = ${dataPointMap.containsKey(plotLine)}")
        if (!dataPointMap.containsKey(plotLine)) {
            Log.i("PLOT", "Getting data points")
            dataPointMap[plotLine] = plotLine.getDataPoints(dimension, dimensionRestriction, dimensionShift)
        }
        return dataPointMap[plotLine]
    }

    private fun alignZero() {
        if (plotLines.none { it.first.alignZero }) return

        var zeroAt : Float? = null
        for (index in plotLines.indices) {
            val pair = plotLines[index]
            val line = pair?.first ?: continue

            if (index == 0) {
                if (line.isEmpty() || !line.Visible) return

                val dataPoints = dataPoints(line)
                if (dataPoints?.isEmpty() != false) continue

                val minValue = line.minValue(dataPoints)!!
                val maxValue = line.maxValue(dataPoints)!!

                zeroAt = PlotLineItem.cord(0f, minValue, maxValue)
                continue
            }

            if (line.alignZero) {
                line.zeroAt = zeroAt
            }
        }
    }

    private fun drawBackground(canvas: Canvas) {
        val maxX = width.toFloat()
        val maxY = height.toFloat()

        canvas.drawRect(xMargin.toFloat(), yMargin.toFloat(), maxX - xMargin, maxY - yMargin, backgroundPaint)
    }

    private fun drawXLines(canvas: Canvas) {
        val maxX = width.toFloat()
        val maxY = height.toFloat()

        val distanceDimension = when {
            dimensionRestriction != null -> dimensionRestriction!!.toFloat()
            else -> plotLines.mapNotNull { it.first.distanceDimension(dimension, dimensionRestriction, dimensionShift) }.maxOfOrNull { it }
        } ?: return

        val sectionLength = distanceDimension / (xLineCount - 1)
        val baseShift = dimensionShift ?: 0L
        val sectionShift = baseShift % sectionLength

        for (i in -1 until xLineCount + 1) {
            val between = i in 0 until xLineCount

            val relativeShift = when {
                between -> sectionShift % sectionLength
                else -> 0f
            }

            val leftToRight = (sectionLength * i.coerceAtLeast(0)) + baseShift - relativeShift
            val rightToLeft = distanceDimension - leftToRight + (2 * (baseShift - relativeShift))

            val corXFactor = when (dimension.toPlotDirection()) {
                PlotDirection.LEFT_TO_RIGHT -> -1
                PlotDirection.RIGHT_TO_LEFT -> 1
            }

            val xPos = (sectionLength * i) + (relativeShift * corXFactor)
            if (xPos < 0f || xPos > distanceDimension) continue

            val cordX = x(xPos, 0f, distanceDimension, maxX)!!
            val cordY = maxY - yMargin

            drawXLine(canvas, cordX, maxY, labelLinePaint)

            val label = when (dimension) {
                PlotDimensionX.INDEX -> label(leftToRight, PlotLineLabelFormat.NUMBER)
                PlotDimensionX.DISTANCE -> label(rightToLeft, PlotLineLabelFormat.DISTANCE)
                PlotDimensionX.TIME -> label(leftToRight, PlotLineLabelFormat.TIME)
                PlotDimensionX.STATE_OF_CHARGE -> label(leftToRight, PlotLineLabelFormat.NUMBER)
            }

            val bounds = Rect()
            labelPaint.getTextBounds(label, 0, label.length, bounds)

            if (between) {
                canvas.drawText(
                    label,
                    cordX - bounds.width() / 2,
                    cordY + yMargin / 2 + bounds.height() / 2,
                    labelPaint
                )
            }
        }

        drawXLine(canvas, x(0f, 0f, 1f, maxX), maxY, borderLinePaint)
        drawXLine(canvas, x(1f, 0f, 1f, maxX), maxY, borderLinePaint)

        val dimensionHighlight = dimensionHighlightAt
        if (dimensionHighlight != null) {
            drawXLine(canvas, dimensionHighlight, maxY, dimensionHighlightLinePaint)
        }
    }

    private fun drawXLine(canvas: Canvas, cord: Float?, maxY: Float, paint: Paint?) {
        if (cord == null) return
        val path = Path()
        path.moveTo(cord, yMargin.toFloat())
        path.lineTo(cord, maxY - yMargin)
        canvas.drawPath(path, paint ?: labelLinePaint)
    }

    private fun drawYBaseLines(canvas: Canvas) {
        val maxX = width.toFloat()
        val maxY = height.toFloat()

        for (i in 0 until yLineCount) {
            val cordY = y(i.toFloat(), 0f, yLineCount.toFloat() - 1, maxY)!!

            if (i in 1 until yLineCount - 1) {
                drawYLine(canvas, cordY, maxX, labelLinePaint)
            } else {
                drawYLine(canvas, cordY, maxX, borderLinePaint)
            }
        }
    }

    private fun drawPlot(canvas: Canvas) {
        val maxX = width.toFloat()
        val maxY = height.toFloat()

        val dimensionYArrayList = arrayListOf(dimensionYPrimary, dimensionYSecondary)
        if (dimensionYAdditional?.isEmpty() == false) dimensionYArrayList.addAll(dimensionYAdditional!!)

        for (pair in plotLines.filter { it.first.Visible }) {
            val plotLine = pair.first
            val plotPaint = pair.second ?: continue

            if (!dimensionHighlightValue.containsKey(plotLine)) dimensionHighlightValue[plotLine] = HashMap()

            for (drawBackground in listOf(true, false)) {
                for (dimensionY in dimensionYArrayList.distinct().reversed()) {
                    dimensionHighlightValue[plotLine]?.set(dimensionY, null)

                    if (plotLine.isEmpty()) continue

                    val configuration = when {
                        dimensionY != null -> PlotGlobalConfiguration.DimensionYConfiguration[dimensionY]
                        else -> plotLine.Configuration
                    } ?: continue

                    if (drawBackground && configuration.Range.backgroundZero == null) continue

                    val dataPoints = dataPoints(plotLine)
                    if (dataPoints?.isEmpty() != false) continue

                    val minDimension = plotLine.minDimension(dimension, dimensionRestriction, dimensionShift) ?: continue
                    val maxDimension = plotLine.maxDimension(dimension, dimensionRestriction, dimensionShift) ?: continue
                    val minMaxDimension = plotLine.distanceDimensionMinMax(dimension, minDimension, maxDimension) ?: 0f

                    val paint = plotPaint.bySecondaryDimension(dimensionY) ?: continue

                    val minValue = plotLine.minValue(dataPoints, dimensionY) ?: continue
                    val maxValue = plotLine.maxValue(dataPoints, dimensionY) ?: continue

                    val smoothing = when (val dimensionSmoothingByConfig = configuration.DimensionSmoothing ?: dimensionSmoothing){
                        null -> null
                        else -> when (configuration.DimensionSmoothingType ?: dimensionSmoothingType) {
                            PlotDimensionSmoothingType.VALUE -> configuration.DimensionSmoothing ?: dimensionSmoothingByConfig
                            PlotDimensionSmoothingType.PERCENTAGE -> minMaxDimension * dimensionSmoothingByConfig
                            PlotDimensionSmoothingType.PIXEL -> minMaxDimension * (1f / (maxX - (2 * xMargin)) * dimensionSmoothingByConfig)
                            else -> null
                        }
                    }

                    val smoothingPercentage = when {
                        smoothing == null || minMaxDimension == 0f -> null
                        else -> smoothing / minMaxDimension
                    }

                    val zeroCord = y(configuration.Range.backgroundZero, minValue, maxValue, maxY)

                    val plotPointCollection = toPlotPointCollection(configuration, plotLine, dimensionY, minValue, maxValue, minDimension, maxDimension, maxX, maxY, smoothing, smoothingPercentage)

                    drawPlotPointCollection(canvas, configuration, plotPointCollection, maxX, maxY, paint, drawBackground, zeroCord)

                    if (!drawBackground && dimensionY == null && plotMarkers?.markers?.isNotEmpty() == true) {
                        drawMarker(canvas, minDimension, maxDimension, maxX, maxY)
                    }
                }
            }
        }
    }

    private fun toPlotPointCollection(configuration: PlotLineConfiguration, line: PlotLine, dimensionY: PlotDimensionY?, minValue: Float, maxValue: Float, minDimension: Any, maxDimension: Any, maxX: Float, maxY: Float, smoothing: Float?, smoothingPercentage: Float?): ArrayList<ArrayList<PointF>> {
        // val dataPoints = line.getDataPoints(dimension, dimensionRestriction, dimensionShift, true)
        val dataPoints = dataPoints(line)!!
        val plotLineItemPointCollection = line.toPlotLineItemPointCollection(dataPoints, dimension, smoothing, minDimension, maxDimension)

        val plotPointCollection = ArrayList<ArrayList<PointF>>()
        for (collection in plotLineItemPointCollection) {
            if (collection.isEmpty()) continue

            val dimensionHighlight = dimensionHighlightAtPercentage
            if (dimensionHighlight != null && dimensionHighlight in collection.minOf { it.x } .. collection.maxOf { it.x }) {
                val point = collection.minByOrNull { (it.x - dimensionHighlight).absoluteValue }
                if (point != null) {
                    val points = collection.filter { (it.x - point.x).absoluteValue <= (smoothingPercentage ?: 0f) }
                    val value = line.byHighlightMethod(configuration.DimensionSmoothingHighlightMethod ?: PlotHighlightMethod.AVG_BY_DIMENSION, points.map { it.y }, dimension, dimensionY)

                    dimensionHighlightValue[line]?.set(dimensionY, value)
                }
             }

            val plotPoints = ArrayList<PointF>()

            for (group in collection.groupBy { it.group }) {
                val plotPoint = when {
                    group.value.size <= 1 -> {
                        val point = group.value.first()

                        val value = point.y.byDimensionY(dimensionY)
                        val x = x(point.x, 0f, 1f, maxX) ?: continue
                        val y = y(value, minValue, maxValue, maxY) ?: continue

                        PointF(x, y)
                    }
                    else -> {
                        val xGroup = when (plotPoints.size) {
                            0 -> group.value.minOfOrNull { it.x }
                            else -> group.value.maxOfOrNull { it.x }
                        } ?: continue

                        val value = line.averageValue(group.value.map { it.y }, dimension, dimensionY)
                        val x = x(xGroup, 0f, 1f, maxX) ?: continue
                        val y = y(value, minValue, maxValue, maxY) ?: continue

                        PointF(x, y)
                    }
                }

                plotPoints.add(plotPoint)
            }

            if (plotPoints.isNotEmpty()) plotPointCollection.add(plotPoints)
        }

        return plotPointCollection
    }

    private fun drawPlotPointCollection(canvas: Canvas, configuration: PlotLineConfiguration, plotPointCollection: ArrayList<ArrayList<PointF>>, maxX: Float, maxY: Float, paint: PlotPaint, drawBackground: Boolean, zeroCord: Float?) {

        restrictCanvas(canvas, maxX, maxY)

        val joinedPlotPoints = ArrayList<PointF>()

        for (plotPointIndex in plotPointCollection.indices) {
            val plotPoints = plotPointCollection[plotPointIndex]

            when (configuration.SessionGapRendering ?: sessionGapRendering) {
                PlotSessionGapRendering.JOIN -> joinedPlotPoints.addAll(plotPoints)
                else -> drawPlotPoints(canvas, configuration, plotPoints, paint, drawBackground, zeroCord, plotPointIndex == plotPointCollection.size - 1)
            }
        }

        if (joinedPlotPoints.isNotEmpty()) {
            drawPlotPoints(canvas, configuration, joinedPlotPoints, paint, drawBackground, zeroCord)
        }

        if ((configuration.SessionGapRendering ?: sessionGapRendering) == PlotSessionGapRendering.GAP) {
            var last: PointF? = null
            for (collection in plotPointCollection) {
                if (last != null) {
                    val first = collection.first()
                    drawLine(canvas, last.x, last.y, first.x, first.y, paint.PlotGapSecondary)
                }
                last = collection.last()
            }
        }

        canvas.restore()
    }

    private fun drawPlotPoints(canvas: Canvas, configuration: PlotLineConfiguration, plotPoints : ArrayList<PointF>, plotPaint: PlotPaint, drawBackground: Boolean, zeroCord: Float?, lastPlot: Boolean = true) {
        val linePaint = when (lastPlot) {
            true -> when (drawBackground) {
                true -> plotPaint.PlotBackground
                else -> plotPaint.Plot
            }
            else -> when (drawBackground) {
                true -> plotPaint.PlotBackgroundSecondary
                else -> plotPaint.PlotSecondary
            }
        }
        drawPlotLine(canvas, configuration, linePaint, plotPaint.TransparentColor, plotPoints, drawBackground, zeroCord)
    }

    private fun drawPlotLine(canvas : Canvas, configuration: PlotLineConfiguration, paint : Paint, transparentColor: Int, plotPoints : ArrayList<PointF>, drawBackground: Boolean, zeroCord: Float?) {
        if (plotPoints.isEmpty()) return
        if (drawBackground && zeroCord == null) return

        val path = Path()

        var firstPoint: PointF? = null
        var prevPoint: PointF? = null

        for (i in plotPoints.indices) {
            val point = plotPoints[i]

            when {
                prevPoint === null -> {
                    firstPoint = point
                    path.moveTo(point.x, point.y)
                }
                else -> {
                    val midX = (prevPoint.x + point.x) / 2
                    val midY = (prevPoint.y + point.y) / 2

                    if (i == 1) {
                        path.lineTo(midX, midY)
                    } else {
                        path.quadTo(prevPoint.x, prevPoint.y, midX, midY)
                    }
                }
            }

            prevPoint = point
        }

        path.lineTo(prevPoint!!.x, prevPoint.y)

        if (drawBackground && zeroCord != null) {
            path.lineTo(prevPoint.x, zeroCord)
            path.lineTo(firstPoint!!.x, zeroCord)

            paint.shader = LinearGradient(0f, 0f, 0f, height.toFloat(), paint.color, transparentColor, Shader.TileMode.MIRROR)
        }

        if (!drawBackground && (configuration.SessionGapRendering ?: sessionGapRendering) == PlotSessionGapRendering.CIRCLE){
            drawPlotLineMarker(canvas, firstPoint, paint)
            drawPlotLineMarker(canvas, prevPoint, paint)
        }

        canvas.drawPath(path, paint)
    }

    private fun drawPlotLineMarker(canvas: Canvas, point: PointF?, paint: Paint) {
        if (point == null) return
        canvas.drawCircle(point.x, point.y, 3f, paint)
    }

    private fun drawMarker(canvas: Canvas, minDimension: Any, maxDimension: Any, maxX: Float, maxY: Float) {

        val markers = plotMarkers?.markers?.filter {
            val isNotOnEdge = when (dimension) {
                PlotDimensionX.DISTANCE -> (minDimension as Float) < it.StartDistance && it.StartDistance < (maxDimension as Float)
                PlotDimensionX.TIME -> false // (minDimension as Long) < it.StartTime && it.StartTime < (maxDimension as Long)
                else -> false
            }
            visibleMarkerTypes.contains(it.MarkerType) && isNotOnEdge
        } ?: return

        var markerXLimit = 0f
        val markerTimes = HashMap<PlotMarkerType, Long>()

        restrictCanvas(canvas, maxX, maxY, yArea = false)

        var prevMarkerEndX: Float? = null

        for (markerGroup in markers.groupBy { it.group(dimension, dimensionSmoothing) }) {
            if (markerGroup.key == null) continue

            val markerType = markerGroup.value.minOfOrNull { it.MarkerType } ?: continue

            val startX = markerGroup.value.mapNotNull { x(it.startByDimension(dimension), minDimension, maxDimension, maxX) }.minOfOrNull { it } ?: continue
            val endX = markerGroup.value.mapNotNull { x(it.endByDimension(dimension), minDimension, maxDimension, maxX) }.maxOfOrNull { it }

            if (startX < xMargin) continue

            if (markerXLimit == 0f) {
                markerXLimit = startX
            }

            markerXLimit = drawMarkerLabel(canvas, markerTimes, startX, markerXLimit)

            if (prevMarkerEndX == null || startX - prevMarkerEndX > 25 || markerType == PlotMarkerType.CHARGE) {
                prevMarkerEndX = endX

                drawMarkerLine(canvas, markerType, startX, -1)
                drawMarkerLine(canvas, markerType, endX, 1)
            }

            for (markerDimensionGroup in markerGroup.value.groupBy { it.group(dimension) }) {
                val markerDimensionType = markerDimensionGroup.value.minOfOrNull { it.MarkerType } ?: continue

                for (marker in markerDimensionGroup.value) {
                    markerTimes[markerDimensionType] = (markerTimes[markerDimensionType] ?: 0L) + ((marker.EndTime ?: marker.StartTime) - marker.StartTime)
                }
            }
        }

        drawMarkerLabel(canvas, markerTimes, Float.MAX_VALUE, markerXLimit)

        canvas.restore()
    }

    private fun drawMarkerLabel(canvas: Canvas, markerTimes: HashMap<PlotMarkerType, Long>, xCurrent: Float, xLimit: Float): Float {
        if (markerTimes.isEmpty()) return xLimit

        val marker = markerTimes.minByOrNull { it.key } ?: return xLimit
        val icon = markerIcon[marker.key] ?: return xLimit
        val paint = markerPaint[marker.key]?.Label ?: return xLimit

        val labels = markerTimes.map { it }.sortedBy { it.key }.associateBy({ it.key }, { timeLabel(it.value) })

        val padding = 8f
        val spaceNeeded = icon.width + labels.maxOf { paint.measureText(it.value).roundToInt() }  + 2 * padding

        if (xCurrent <= xLimit + spaceNeeded) return xLimit

        canvas.drawBitmap(
            icon,
            xLimit - (labelPaint.textSize / 2f),
            (yMargin / 3f),
            paint
        )

        var yStart = when (labels.size) {
            1 -> yMargin - labelPaint.textSize + padding
            else -> labelPaint.textSize + padding / 2
        }

        for (label in labels) {
            val labelPaint = markerPaint[label.key]?.Label ?: continue

            canvas.drawText(
                label.value,
                xLimit + (labelPaint.textSize / 2f) + padding,
                yStart,
                labelPaint
            )

            yStart += labelPaint.textSize
        }

        markerTimes.clear()

        return xCurrent
    }

    private fun drawMarkerLine(canvas: Canvas, markerType: PlotMarkerType, x: Float?, multiplier: Int) {
        if (x == null) return

        val center = x.roundToInt().toFloat() + multiplier

        val top = yMargin.toFloat() + borderLinePaint.strokeWidth
        val bottom = canvas.height - yMargin.toFloat() - borderLinePaint.strokeWidth - 1

        val points = arrayOf(
            PointF(center, top + 15f),
            PointF(center + (multiplier * 10f), top),
            PointF(center, top),
            PointF(center, bottom),
            PointF(center, top + 15f)
        )

        if (multiplier > 0) points.reverse()

        val trianglePath = Path()

        trianglePath.moveTo(center, top + 15f)

        points.forEach {
            trianglePath.lineTo(it.x, it.y)
        }

        canvas.drawPath(trianglePath, markerPaint[markerType]!!.Mark)
    }
    
    private fun drawYLines(canvas: Canvas) {
        val maxX = width.toFloat()
        val maxY = height.toFloat()

        val bounds = Rect()
        labelPaint.getTextBounds("Dummy", 0, "Dummy".length, bounds)

        val dimensionYArrayList = arrayListOf(dimensionYPrimary, dimensionYSecondary)
        if (dimensionYAdditional?.isEmpty() == false) dimensionYArrayList.addAll(dimensionYAdditional!!)

        for (drawHighlightLabelOnly in listOf(false, true)) {
            var index = 0
            for (pair in plotLines.filter { it.first.Visible }) {
                val line = pair.first
                val plotPaint = pair.second

                for (dimensionY in dimensionYArrayList.distinct()) {
                    val labelPosition = when (dimensionYArrayList.indexOf(dimensionY)) {
                        0 -> PlotLabelPosition.LEFT
                        1 -> PlotLabelPosition.RIGHT
                        else -> PlotLabelPosition.NONE
                    }

                    if (dimensionY != null && index++ > 0) continue

                    val dataPoints = dataPoints(line)
                    if (dataPoints?.isEmpty() != false) continue

                    val configuration = when {
                        dimensionY != null -> PlotGlobalConfiguration.DimensionYConfiguration[dimensionY]
                        else -> line.Configuration
                    } ?: continue

                    val paint = plotPaint.bySecondaryDimension(dimensionY) ?: continue

                    val minValue = line.minValue(dataPoints, dimensionY) ?: continue
                    val maxValue = line.maxValue(dataPoints, dimensionY) ?: continue

                    val highlight = when (dimensionHighlightAt) {
                        null -> line.byHighlightMethod(dataPoints, dimension, dimensionY)
                        else -> dimensionHighlightValue[line]?.get(dimensionY)
                    }

                    val highlightMethod = when (dimensionHighlightAt) {
                        null -> configuration.HighlightMethod
                        else -> PlotHighlightMethod.RAW
                    }

                    val labelShiftY = (bounds.height() / 2).toFloat()
                    val valueShiftY = (maxValue - minValue) / (yLineCount - 1)
                    val valueCorrectionY = when (dimensionY) {
                        PlotDimensionY.TIME -> line.minValue(dataPoints, dimensionY, false)!!
                        else -> 0f
                    }

                    val labelUnitXOffset = when (labelPosition) {
                        PlotLabelPosition.LEFT -> 0f
                        PlotLabelPosition.RIGHT -> -labelPaint.measureText(configuration.Unit) / 2
                        else -> 0f
                    }

                    val labelCordX = when (labelPosition) {
                        PlotLabelPosition.LEFT -> 0f // textSize
                        PlotLabelPosition.RIGHT -> maxX - xMargin + textSize / 2
                        else -> null
                    }

                    val highlightCordY = y(highlight, minValue, maxValue, maxY)

                    if (!drawHighlightLabelOnly) {
                        if (labelPosition !== PlotLabelPosition.NONE && labelCordX != null) {
                            if (configuration.Unit.isNotEmpty()) {
                                canvas.drawText(configuration.Unit, labelCordX + labelUnitXOffset,
                                    yMargin - (yMargin / 3f), labelPaint)
                            }

                            for (i in 0 until yLineCount) {
                                val valueY = maxValue - i * valueShiftY
                                val cordY = y(valueY, minValue, maxValue, maxY)!!
                                val label = label((valueY - valueCorrectionY) / configuration.Divider, configuration.LabelFormat)

                                canvas.drawText(label, labelCordX, cordY + labelShiftY, labelPaint)
                            }
                        }

                        if (highlightCordY != null && highlightCordY in yMargin.toFloat() .. maxY - yMargin) {
                            when (highlightMethod) {
                                PlotHighlightMethod.AVG_BY_DIMENSION,
                                PlotHighlightMethod.AVG_BY_INDEX,
                                PlotHighlightMethod.AVG_BY_DISTANCE,
                                PlotHighlightMethod.AVG_BY_STATE_OF_CHARGE,
                                PlotHighlightMethod.AVG_BY_TIME,
                                PlotHighlightMethod.RAW -> drawYLine(canvas, highlightCordY, maxX, paint.HighlightLabelLine)
                                else -> {
                                    // Don't draw
                                }
                            }
                        }

                        for (baseLineAt in line.baseLineAt) {
                            drawYLine(canvas, y(baseLineAt, minValue, maxValue, maxY), maxX, baseLinePaint)
                        }
                    } else {
                        if (labelCordX != null && highlightCordY != null) {
                            val highlightCordYLimited = highlightCordY
                                .coerceAtLeast(yMargin.toFloat())
                                .coerceAtMost(maxY - yMargin)

                            val label = label((highlight!! - valueCorrectionY) / configuration.Divider, configuration.LabelFormat, highlightMethod)
                            paint.HighlightLabel.textSize = textSize * 1.25f
                            val labelWidth = paint.HighlightLabel.measureText(label)
                            val labelHeight = paint.HighlightLabel.textSize
                            val textBoxMargin = paint.HighlightLabel.textSize / 3.5f



                            val adjustX = if (labelPosition == PlotLabelPosition.RIGHT) {
                                if (labelWidth + 2 * textBoxMargin > xMargin + paint.Plot.strokeWidth * 4) {
                                    maxX - (labelWidth + textBoxMargin + paint.Plot.strokeWidth * 2)
                                } else {
                                    maxX - xMargin - textBoxMargin - paint.Plot.strokeWidth * 2
                                }
                                //labelCordX + xMargin - (labelWidth + paint.Plot.strokeWidth * 4 - labelUnitXOffset)//  + labelUnitXOffset

                            } else {
                                if (labelWidth + 2 * textBoxMargin > xMargin + paint.Plot.strokeWidth * 4) {
                                    labelCordX + textBoxMargin + paint.Plot.strokeWidth * 2
                                } else {
                                    xMargin - labelWidth + textBoxMargin + paint.Plot.strokeWidth * 2
                                }
                            }

                            canvas.drawRect(
                                adjustX - textBoxMargin,
                                highlightCordYLimited - labelHeight + labelShiftY,
                                adjustX + labelWidth + textBoxMargin,
                                highlightCordYLimited + labelShiftY + textBoxMargin,
                                backgroundPaint
                            )

                            canvas.drawRect(
                                adjustX - textBoxMargin,
                                highlightCordYLimited - labelHeight + labelShiftY,
                                adjustX + labelWidth + textBoxMargin,
                                highlightCordYLimited + labelShiftY + textBoxMargin,
                                paint.Plot
                            )

                            canvas.drawText(label, adjustX, highlightCordYLimited + labelShiftY, paint.HighlightLabel)
                        }
                    }
                }
            }
        }
    }

    private fun drawYLine(canvas: Canvas, cord: Float?, maxX: Float, paint: Paint?) {
        if (cord == null) return
        drawLine(canvas, xMargin.toFloat(), cord, maxX - xMargin, cord, paint ?: labelLinePaint)
    }

    private fun drawLine(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, paint: Paint) {
        val path = Path()
        path.moveTo(x1, y1)
        path.lineTo(x2, y2)
        canvas.drawPath(path, paint)
    }

    private fun restrictCanvas(canvas: Canvas, maxX: Float, maxY: Float, xArea: Boolean = true, yArea: Boolean = true) {
        // restrict canvas drawing region
        canvas.save()
        if (yArea) {
            canvas.clipOutRect(0f, 0f, maxX, yMargin.toFloat()) // TOP
            canvas.clipOutRect(0f, maxY - yMargin, maxX, maxY)  // BOTTOM
        }

        if (xArea) {
            canvas.clipOutRect(0f, 0f, xMargin.toFloat(), maxY) // LEFT
            canvas.clipOutRect(maxX - xMargin, 0f, maxX, maxY)  // RIGHT
        }
    }

    private fun timeLabel(time: Long): String {
        // return when {
        //     TimeUnit.HOURS.convert(time, TimeUnit.NANOSECONDS) > 12 -> String.format("%02d:%02d", TimeUnit.DAYS.convert(time, TimeUnit.NANOSECONDS), TimeUnit.HOURS.convert(time, TimeUnit.NANOSECONDS) % 24)
        //     TimeUnit.MINUTES.convert(time, TimeUnit.NANOSECONDS) > 30 -> String.format("%02d:%02d'", TimeUnit.HOURS.convert(time, TimeUnit.NANOSECONDS), TimeUnit.MINUTES.convert(time, TimeUnit.NANOSECONDS) % 60)
        //     else -> String.format("%02d'%02d''", TimeUnit.MINUTES.convert(time, TimeUnit.NANOSECONDS), TimeUnit.SECONDS.convert(time, TimeUnit.NANOSECONDS) % 60)
        // }
        return String.format("%02d:%02d",
            TimeUnit.MILLISECONDS.toHours(time),
            (TimeUnit.MILLISECONDS.toSeconds(time) / 60f).roundToInt() % TimeUnit.HOURS.toMinutes(1))
    }

    private fun label(value: Float, plotLineLabelFormat: PlotLineLabelFormat, plotHighlightMethod: PlotHighlightMethod? = null): String {
        if (plotHighlightMethod == PlotHighlightMethod.NONE) return ""

        // val suffix = when (plotLineLabelFormat) {
        //     PlotLineLabelFormat.PERCENTAGE -> " %"
        //     else -> ""
        // }

        val suffix = ""

        return when (plotLineLabelFormat) {
            PlotLineLabelFormat.NUMBER, PlotLineLabelFormat.PERCENTAGE -> when (plotHighlightMethod) {
                PlotHighlightMethod.AVG_BY_INDEX, PlotHighlightMethod.AVG_BY_DISTANCE, PlotHighlightMethod.AVG_BY_TIME, PlotHighlightMethod.AVG_BY_STATE_OF_CHARGE  -> String.format("Ø %.0f", value)
                else -> String.format("%.0f%s", value, suffix)
            }
            PlotLineLabelFormat.FLOAT -> when (plotHighlightMethod) {
                PlotHighlightMethod.AVG_BY_INDEX, PlotHighlightMethod.AVG_BY_DISTANCE, PlotHighlightMethod.AVG_BY_TIME, PlotHighlightMethod.AVG_BY_STATE_OF_CHARGE  -> String.format("Ø %.1f", value)
                else -> String.format("%.1f", value)
            }
            PlotLineLabelFormat.TIME -> timeLabel(value.toLong())
            PlotLineLabelFormat.DISTANCE -> when {
                appPreferences.distanceUnit.toUnit(value) % 1000 > 100 -> String.format("%.1f %s", appPreferences.distanceUnit.toUnit(value) / 1000, appPreferences.distanceUnit.unit())
                else -> String.format("%d %s", (appPreferences.distanceUnit.toUnit(value) / 1000).roundToInt(), appPreferences.distanceUnit.unit())
            }
            PlotLineLabelFormat.ALTITUDE -> String.format("%d", appPreferences.distanceUnit.toSubUnit(value).roundToInt(), appPreferences.distanceUnit.subUnit())
        }
    }
}
