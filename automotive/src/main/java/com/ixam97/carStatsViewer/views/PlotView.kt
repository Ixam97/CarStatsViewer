package com.ixam97.carStatsViewer.views

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getColor
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.plot.enums.*
import com.ixam97.carStatsViewer.plot.graphics.*
import com.ixam97.carStatsViewer.plot.objects.*
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class PlotView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    companion object {
        const val textSize = 26f
    }

    var xMargin: Int = 100
        set(value) {
            val diff = value != field
            if (value > 0) {
                field = value
                if (diff) invalidate()
            }
        }

    var xLineCount: Int = 6
        set(value) {
            val diff = value != field
            if (value > 1) {
                field = value
                if (diff) invalidate()
            }
        }

    var yMargin: Int = 60
        set(value) {
            val diff = value != field
            if (value > 0) {
                field = value
                if (diff) invalidate()
            }
        }

    var yLineCount: Int = 5
        set(value) {
            val diff = value != field
            if (value > 1) {
                field = value
                if (diff) invalidate()
            }
        }

    var dimension: PlotDimension = PlotDimension.INDEX
        set(value) {
            val diff = value != field
            field = value
            if (diff) invalidate()
        }

    var secondaryDimension: PlotSecondaryDimension? = null
        set(value) {
            val diff = value != field
            field = value
            if (diff) invalidate()
        }

    var dimensionRestrictionTouchInterval: Long? = null
    var dimensionRestriction: Long? = null
        set(value) {
            val diff = value != field
            field = value
            if (diff) invalidate()
        }

    var dimensionShiftTouchEnabled: Boolean = false
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

    var dimensionSmoothingPercentage: Float? = null
        set(value) {
            val diff = value != field
            field = value
            if (diff) invalidate()
        }

    var visibleMarkerTypes: HashSet<PlotMarkerType> = HashSet()

    var sessionGapRendering : PlotSessionGapRendering = PlotSessionGapRendering.JOIN
        set(value) {
            val diff = value != field
            field = value
            if (diff) invalidate()
        }

    private val plotLines = ArrayList<PlotLine>()
    private var plotMarkers : PlotMarkers? = null

    private val plotPaint = ArrayList<PlotPaint>()

    private lateinit var labelPaint: Paint
    private lateinit var labelLinePaint: Paint
    private lateinit var borderLinePaint: Paint
    private lateinit var baseLinePaint: Paint
    private lateinit var backgroundPaint: Paint

    private var markerPaint = HashMap<PlotMarkerType, PlotMarkerPaint>()
    private var markerIcon = HashMap<PlotMarkerType, Bitmap?>()

    init {
        setupPaint()
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

        val plotColors = listOf(null, Color.CYAN, Color.BLUE, Color.RED)

        for (color in plotColors) {
            plotPaint.add(PlotPaint.byColor(color ?: typedValue.data, textSize))
        }

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
            item.reset()
        }

        plotMarkers?.markers?.clear()

        invalidate()
    }

    fun addPlotLine(plotLine: PlotLine) {
        if (plotLine.plotPaint == null) {
            plotLine.plotPaint = plotPaint[plotLines.size]
        }

        if (plotLine.secondaryPlotPaint == null) {
            plotLine.secondaryPlotPaint = PlotPaint.byColor(Color.GREEN, textSize)
        }

        plotLines.add(plotLine)
        invalidate()
    }

    fun removePlotLine(plotLine: PlotLine?) {
        plotLines.remove(plotLine)
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

            val restrictionInterval = dimensionRestrictionTouchInterval
            if (restrictionInterval != null && lastRestriction != null && !(lastSpanX/spanX).isInfinite()) {
                dimensionRestriction = (((lastRestriction!!.toFloat() * (lastSpanX / spanX)).toLong()/restrictionInterval) * restrictionInterval)
                    .coerceAtMost(touchDimensionMax + (restrictionInterval - touchDimensionMax % restrictionInterval))
                    .coerceAtLeast(restrictionInterval)
            }

            return true
        }
    }

    private val mGestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            if (dimensionShiftTouchEnabled) {
                touchDimensionShiftDistance += - distanceX
                dimensionShift = (touchDimensionShift + touchDimensionShiftDistance * touchDimensionShiftByPixel).toLong()
                    .coerceAtMost(touchDimensionMax - (dimensionRestriction ?: 0L))
                    .coerceAtLeast(0L)
            }
            return true
        }
    }

    private val mScrollDetector = GestureDetector(context, mGestureListener)
    private val mScaleDetector = ScaleGestureDetector(context!!, mScaleGestureDetector)

    private var touchDimensionShift : Long = 0L
    private var touchDimensionShiftDistance : Float = 0f
    private var touchDimensionShiftByPixel : Float = 0f
    private var touchDimensionMax : Long = 0L

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val restriction = dimensionRestriction ?: return true

        when (ev.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                touchDimensionShift = dimensionShift ?: 0L
                touchDimensionShiftDistance = 0f
                touchDimensionShiftByPixel = restriction.toFloat() / width.toFloat()
                touchDimensionMax = (plotLines.mapNotNull { it.distanceDimension(dimension) }.max() ?: 0f).toLong()
            }
        }

        mScrollDetector.onTouchEvent(ev)
        mScaleDetector.onTouchEvent(ev)
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        alignZero()
        drawBackground(canvas)
        drawXLines(canvas)
        drawYBaseLines(canvas)
        drawPlot(canvas)
        drawYLines(canvas)
    }

    private fun alignZero() {
        if (plotLines.none { it.alignZero }) return

        var zeroAt : Float? = null
        for (index in plotLines.indices) {
            val line = plotLines[index]

            if (index == 0) {
                if (line.isEmpty() || !line.Visible) return

                val dataPoints = line.getDataPoints(dimension, dimensionRestriction, dimensionShift)
                if (dataPoints.isEmpty()) return

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
            else -> plotLines.mapNotNull { it.distanceDimension(dimension, dimensionRestriction) }.max()?:0f
        }

        val sectionLength = distanceDimension / (xLineCount - 1)
        val baseShift = dimensionShift ?: 0L
        val sectionShift = baseShift % sectionLength

        val from = if (sectionShift == 0f) 0 else -1

        for (i in from until xLineCount) {
            val between = i in from + 1 until xLineCount - 1

            val relativeShift = when {
                between -> sectionShift % sectionLength
                else -> 0f
            }

            val leftZero = (sectionLength * i.coerceAtLeast(0)) + baseShift - relativeShift
            val rightZero = distanceDimension - leftZero + (2 * (baseShift - relativeShift))

            val cordX = x((sectionLength * i.coerceAtLeast(0)) + relativeShift, 0f, distanceDimension, maxX)!!
            val cordY = maxY - yMargin

            if (between) {
                drawXLine(canvas, cordX, maxY, labelLinePaint)
            } else {
                drawXLine(canvas, cordX, maxY, borderLinePaint)
            }

            val label = when (dimension) {
                PlotDimension.INDEX -> label(leftZero, PlotLineLabelFormat.NUMBER)
                PlotDimension.DISTANCE -> label(rightZero, PlotLineLabelFormat.DISTANCE)
                PlotDimension.TIME -> label(leftZero, PlotLineLabelFormat.TIME)
                PlotDimension.STATE_OF_CHARGE -> label(leftZero, PlotLineLabelFormat.NUMBER)
            }

            val bounds = Rect()
            labelPaint.getTextBounds(label, 0, label.length, bounds)

            if (between || from == 0) {
                canvas.drawText(
                    label,
                    cordX - bounds.width() / 2,
                    cordY + yMargin / 2 + bounds.height() / 2,
                    labelPaint
                )
            }
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

        var index = 0
        for (line in plotLines.filter { it.Visible }) {
            for (secondaryDimension in arrayListOf(null, secondaryDimension).distinct()) {
                if (line.isEmpty()) continue

                val dataPoints = line.getDataPoints(dimension, dimensionRestriction, dimensionShift)
                if (dataPoints.isEmpty()) continue

                val configuration = when {
                    secondaryDimension != null -> PlotGlobalConfiguration.SecondaryDimensionConfiguration[secondaryDimension]
                    else -> line.Configuration
                } ?: continue

                val paint = when {
                    secondaryDimension != null -> line.secondaryPlotPaint ?: line.plotPaint
                    else -> line.plotPaint
                } ?: continue

                val minValue = line.minValue(dataPoints, secondaryDimension)!!
                val maxValue = line.maxValue(dataPoints, secondaryDimension)!!

                val minDimension = line.minDimension(dataPoints, dimension, dimensionRestriction)
                val maxDimension = line.maxDimension(dataPoints, dimension, dimensionRestriction)

                val smoothing = when {
                    dimensionSmoothing != null -> dimensionSmoothing
                    dimensionSmoothingPercentage != null -> line.distanceDimension(dataPoints, dimension, dimensionRestriction) * dimensionSmoothingPercentage!!
                    else -> null
                }

                val zeroCord = y(configuration.Range.backgroundZero, minValue, maxValue, maxY)

                val plotPointCollection = toPlotPointCollection(line, secondaryDimension, minValue, maxValue, minDimension, maxDimension, maxX, maxY, smoothing)

                drawPlotPointCollection(canvas, plotPointCollection, maxX, maxY, paint, zeroCord)

                if (index == 0 && plotMarkers?.markers?.isNotEmpty() ?: false) {
                    drawMarker(canvas, dataPoints, line, minDimension, maxDimension, maxX)
                }

                index++
            }
        }
    }

    private fun toPlotPointCollection(line: PlotLine, secondaryDimension: PlotSecondaryDimension?, minValue: Float, maxValue: Float, minDimension: Any, maxDimension: Any, maxX: Float, maxY: Float, smoothing: Float?): ArrayList<ArrayList<PlotPoint>> {
        val dataPointsUnrestricted = line.getDataPoints(dimension)
        val plotLineItemPointCollection = line.toPlotLineItemPointCollection(dataPointsUnrestricted, dimension, smoothing, minDimension, maxDimension)

        val plotPointCollection = ArrayList<ArrayList<PlotPoint>>()
        for (collection in plotLineItemPointCollection) {
            if (collection.isEmpty()) continue

            val plotPoints = ArrayList<PlotPoint>()

            for (group in collection.groupBy { it.group }) {
                val plotPoint = when {
                    group.value.size <= 1 -> {
                        val point = group.value.first()
                        PlotPoint(
                            x(point.x, 0f, 1f, maxX)!!,
                            y(point.y.bySecondaryDimension(secondaryDimension), minValue, maxValue, maxY)!!
                        )
                    }
                    else -> {
                        val x = when (plotPoints.size) {
                            0 -> group.value.minBy { it.x }.x
                            else -> group.value.maxBy { it.x }.x
                        }

                        PlotPoint(
                            x(x, 0f, 1f, maxX)!!,
                            y(line.averageValue(group.value.map { it.y }, dimension, secondaryDimension), minValue, maxValue, maxY)!!
                        )
                    }
                }

                plotPoints.add(plotPoint)
            }

            plotPointCollection.add(plotPoints)
        }

        return plotPointCollection
    }

    private fun drawPlotPointCollection(canvas: Canvas, plotPointCollection: ArrayList<ArrayList<PlotPoint>>, maxX: Float, maxY: Float, paint: PlotPaint, zeroCord: Float?) {
        // restrict canvas drawing region
        canvas.save()
        canvas.clipOutRect(0f, 0f, maxX, yMargin.toFloat()) // TOP
        canvas.clipOutRect(0f, maxY - yMargin, maxX, maxY)  // BOTTOM
        canvas.clipOutRect(0f, 0f, xMargin.toFloat(), maxY) // LEFT
        canvas.clipOutRect(maxX - xMargin, 0f, maxX, maxY)  // RIGHT

        val joinedPlotPoints = ArrayList<PlotPoint>()

        for (plotPointIndex in plotPointCollection.indices) {
            val plotPoints = plotPointCollection[plotPointIndex]

            when (sessionGapRendering) {
                PlotSessionGapRendering.JOIN -> joinedPlotPoints.addAll(plotPoints)
                else -> drawPlotPoints(canvas, plotPoints, paint, zeroCord, plotPointIndex == plotPointCollection.size - 1)
            }
        }

        if (joinedPlotPoints.isNotEmpty()) {
            drawPlotPoints(canvas, joinedPlotPoints, paint, zeroCord)
        }

        if (sessionGapRendering == PlotSessionGapRendering.GAP) {
            var last: PlotPoint? = null
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

    private fun drawPlotPoints(canvas: Canvas, plotPoints : ArrayList<PlotPoint>, plotPaint: PlotPaint, zeroCord: Float?, lastPlot: Boolean = true) {
        if (zeroCord != null) {
            val backgroundPaint = when (lastPlot) {
                true -> plotPaint.PlotBackground
                else -> plotPaint.PlotBackgroundSecondary
            }
            drawPlotLine(canvas, backgroundPaint, plotPoints, zeroCord, true)
        }

        val plotPaint = when (lastPlot) {
            true -> plotPaint.Plot
            else -> plotPaint.PlotSecondary
        }
        drawPlotLine(canvas, plotPaint, plotPoints, zeroCord)
    }

    private fun drawPlotLine(canvas : Canvas, paint : Paint, plotPoints : ArrayList<PlotPoint>, zeroCord: Float?, background: Boolean = false) {
        if (plotPoints.isEmpty()) return

        val path = Path()

        var firstPoint: PlotPoint? = null
        var prevPoint: PlotPoint? = null

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

        if (background && zeroCord != null) {
            path.lineTo(prevPoint.x, zeroCord)
            path.lineTo(firstPoint!!.x, zeroCord)
        }

        if (!background && sessionGapRendering == PlotSessionGapRendering.CIRCLE){
            drawPlotLineMarker(canvas, firstPoint, paint)
            drawPlotLineMarker(canvas, prevPoint, paint)
        }

        canvas.drawPath(path, paint)
    }

    private fun drawPlotLineMarker(canvas: Canvas, point: PlotPoint?, paint: Paint) {
        if (point == null) return
        canvas.drawCircle(point.x, point.y, 3f, paint)
    }

    private fun drawMarker(canvas: Canvas, dataPoints: List<PlotLineItem>, line: PlotLine, minDimension: Any, maxDimension: Any, maxX: Float) {
        val markers = plotMarkers!!.markers.filter { visibleMarkerTypes.contains(it.MarkerType) }

        var markerXLimit = 0f
        val markerTimes = HashMap<PlotMarkerType, Long>()

        for (markerGroup in markers.groupBy { line.x(dataPoints, it.StartTime, PlotDimension.TIME, dimension, minDimension, maxDimension) }) {
            if (markerGroup.key == null) continue

            val markerSorted = markerGroup.value.sortedBy { it.MarkerType }
            val markerTypeGroup = markerSorted.groupBy { it.MarkerType }
            val markerType = markerSorted.first().MarkerType

            val markers = markerTypeGroup[markerType]!!

            val x1 = x(markerGroup.key, 0f, 1f, maxX)
            val x2 = when (dimension) {
                PlotDimension.DISTANCE -> x1
                else -> x(line.x( dataPoints, markers.last().EndTime, PlotDimension.TIME, dimension, minDimension, maxDimension), 0f, 1f, maxX)
            }

            if (x1 != null && markers.none { it.EndTime == null }) {
                if (markerXLimit == 0f) {
                    markerXLimit = x1
                }

                markerXLimit = drawMarkerLabel(canvas, markerTimes, markerXLimit, x1)

                drawMarkerLine(canvas, markerType, x1, -1)
                drawMarkerLine(canvas, markerType, x2, 1)

                val diff = markers.sumOf { (it.EndTime ?: it.StartTime) - it.StartTime }

                markerTimes[markerType] = (markerTimes[markerType] ?: 0L) + diff
            }
        }

        drawMarkerLabel(canvas, markerTimes, markerXLimit, maxX)
    }

    private fun drawMarkerLabel(canvas: Canvas, markerTimes: HashMap<PlotMarkerType, Long>, xMin: Float, xCurrent: Float): Float {
        if (markerTimes.isNotEmpty() && xCurrent > xMin + markerTimes.map { 36 + labelPaint.measureText(timeLabel(it.value)) }.sum()) {
            var shift = 0
            for (item in markerTimes) {
                val label = timeLabel(item.value)
                val labelPaint = markerPaint[item.key]!!.Label

                canvas.drawBitmap(
                    markerIcon[item.key]!!,
                    xMin - (textSize / 2f) + shift,
                    (yMargin / 3f),
                    labelPaint
                )

                canvas.drawText(
                    label,
                    xMin + labelPaint.textSize + shift,
                    yMargin - labelPaint.textSize + 2f,
                    labelPaint
                )

                shift += 36 + labelPaint.measureText(timeLabel(item.value)).roundToInt()
            }

            markerTimes.clear()

            return xCurrent
        }

        return xMin
    }

    private fun drawMarkerLine(canvas: Canvas, markerType: PlotMarkerType, x: Float?, multiplier: Int) {
        if (x == null) return

        val top = yMargin.toFloat() + 1
        val bottom = canvas.height - yMargin.toFloat() - 1

        drawLine(canvas, x, top, x, bottom, markerPaint[markerType]!!.Line)

        val trianglePath = Path()
        trianglePath.moveTo(x, top)
        trianglePath.lineTo(x, yMargin.toFloat() + 15f)
        trianglePath.lineTo(x + (multiplier * 12f), top)
        trianglePath.lineTo(x, top)
        canvas.drawPath(trianglePath, markerPaint[markerType]!!.Mark)
    }
    
    private fun drawYLines(canvas: Canvas) {
        val maxX = width.toFloat()
        val maxY = height.toFloat()

        val bounds = Rect()
        labelPaint.getTextBounds("Dummy", 0, "Dummy".length, bounds)

        for (drawHighlightLabelOnly in listOf(false, true)) {
            var index = 0
            for (line in plotLines.filter { it.Visible }) {
                for (secondaryDimension in arrayListOf(null, secondaryDimension).distinct()) {
                    // only draw one secondary
                    if (secondaryDimension != null && index++ > 0) continue

                    val dataPoints = line.getDataPoints(dimension, dimensionRestriction, dimensionShift)
                    val configuration = when {
                        secondaryDimension != null -> PlotGlobalConfiguration.SecondaryDimensionConfiguration[secondaryDimension]
                        else -> line.Configuration
                    } ?: continue

                    val paint = when {
                        secondaryDimension != null -> line.secondaryPlotPaint ?: line.plotPaint
                        else -> line.plotPaint
                    } ?: continue

                    val minValue = line.minValue(dataPoints, secondaryDimension)!!
                    val maxValue = line.maxValue(dataPoints, secondaryDimension)!!
                    val highlight = line.byHighlightMethod(dataPoints, secondaryDimension)

                    val labelShiftY = (bounds.height() / 2).toFloat()
                    val valueShiftY = (maxValue - minValue) / (yLineCount - 1)
                    val valueCorrectionY = when (secondaryDimension) {
                        PlotSecondaryDimension.TIME -> line.minValue(dataPoints, secondaryDimension, false)!!
                        else -> 0f
                    }

                    val labelUnitXOffset = when (configuration.LabelPosition) {
                        PlotLabelPosition.LEFT -> 0f
                        PlotLabelPosition.RIGHT -> -labelPaint.measureText(configuration.Unit) / 2
                        else -> 0f
                    }

                    val labelCordX = when (configuration.LabelPosition) {
                        PlotLabelPosition.LEFT -> textSize
                        PlotLabelPosition.RIGHT -> maxX - xMargin + textSize / 2
                        else -> null
                    }

                    val highlightCordY = y(highlight, minValue, maxValue, maxY)

                    if (!drawHighlightLabelOnly) {
                        if (configuration.LabelPosition !== PlotLabelPosition.NONE && labelCordX != null) {
                            if (configuration.Unit.isNotEmpty()) {
                                canvas.drawText(configuration.Unit, labelCordX + labelUnitXOffset,yMargin - (yMargin / 3f), labelPaint)
                            }

                            for (i in 0 until yLineCount) {
                                val valueY = maxValue - i * valueShiftY
                                val cordY = y(valueY, minValue, maxValue, maxY)!!
                                val label = label((valueY - valueCorrectionY) / configuration.Divider, configuration.LabelFormat)

                                canvas.drawText(label, labelCordX, cordY + labelShiftY, labelPaint)
                            }
                        }

                        when (configuration.HighlightMethod) {
                            PlotHighlightMethod.AVG_BY_INDEX -> drawYLine(canvas, highlightCordY, maxX, paint.HighlightLabelLine)
                            PlotHighlightMethod.AVG_BY_DISTANCE -> drawYLine(canvas, highlightCordY, maxX, paint.HighlightLabelLine)
                            PlotHighlightMethod.AVG_BY_TIME -> drawYLine(canvas, highlightCordY, maxX, paint.HighlightLabelLine)
                            else -> {
                                // Don't draw
                            }
                        }

                        for (baseLineAt in line.baseLineAt) {
                            drawYLine(canvas, y(baseLineAt, minValue, maxValue, maxY), maxX, baseLinePaint)
                        }
                    } else {
                        if (labelCordX != null && highlightCordY != null) {
                            val label = label((highlight!! - valueCorrectionY) / configuration.Divider, configuration.LabelFormat, configuration.HighlightMethod)
                            paint.HighlightLabel.textSize = 35f
                            val labelWidth = paint.HighlightLabel.measureText(label)
                            val labelHeight = paint.HighlightLabel.textSize
                            val textBoxMargin = paint.HighlightLabel.textSize / 3.5f

                            canvas.drawRect(
                                labelCordX + labelUnitXOffset - textBoxMargin,
                                highlightCordY - labelHeight + labelShiftY,
                                labelCordX + labelUnitXOffset + labelWidth + textBoxMargin,
                                highlightCordY + labelShiftY + textBoxMargin,
                                backgroundPaint
                            )

                            canvas.drawRect(
                                labelCordX + labelUnitXOffset - textBoxMargin,
                                highlightCordY - labelHeight + labelShiftY,
                                labelCordX + labelUnitXOffset + labelWidth + textBoxMargin,
                                highlightCordY + labelShiftY + textBoxMargin,
                                paint.Plot
                            )

                            canvas.drawText(label, labelCordX + labelUnitXOffset, highlightCordY + labelShiftY, paint.HighlightLabel)
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

    private fun timeLabel(time: Long): String {
        return when {
            TimeUnit.HOURS.convert(time, TimeUnit.NANOSECONDS) > 12 -> String.format("%02d:%02d", TimeUnit.DAYS.convert(time, TimeUnit.NANOSECONDS), TimeUnit.HOURS.convert(time, TimeUnit.NANOSECONDS) % 24)
            TimeUnit.MINUTES.convert(time, TimeUnit.NANOSECONDS) > 30 -> String.format("%02d:%02d'", TimeUnit.HOURS.convert(time, TimeUnit.NANOSECONDS), TimeUnit.MINUTES.convert(time, TimeUnit.NANOSECONDS) % 60)
            else -> String.format("%02d'%02d''", TimeUnit.MINUTES.convert(time, TimeUnit.NANOSECONDS), TimeUnit.SECONDS.convert(time, TimeUnit.NANOSECONDS) % 60)
        }
    }

    private fun label(value: Float, plotLineLabelFormat: PlotLineLabelFormat, plotHighlightMethod: PlotHighlightMethod? = null): String {
        if (plotHighlightMethod == PlotHighlightMethod.NONE) return ""

        val suffix = when (plotLineLabelFormat) {
            PlotLineLabelFormat.PERCENTAGE -> " %"
            else -> ""
        }

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
                value % 1000 > 100 -> String.format("%.1f km", value / 1000)
                else -> String.format("%d km", (value / 1000).toInt())
            }
        }
    }
}