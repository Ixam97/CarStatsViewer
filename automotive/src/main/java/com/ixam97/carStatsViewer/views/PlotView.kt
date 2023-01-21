package com.ixam97.carStatsViewer.views

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getColor
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.plot.*
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

    var dimensionRestrictionTouchInterval: Long? = null
    var dimensionRestriction: Long? = null
        set(value) {
            val diff = value != field
            field = value
            if (diff) invalidate()
        }

    var dimensionShiftTouchInterval: Long? = null
    var dimensionShift: Long? = null
        set(value) {
            val diff = value != field
            field = value
            if (diff) invalidate()
        }

    var dimensionSmoothing: Long? = null
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
        labelLinePaint.color = Color.GRAY

        labelPaint = Paint(labelLinePaint)
        labelPaint.style = Paint.Style.FILL

        baseLinePaint = Paint(labelLinePaint)
        baseLinePaint.color = Color.LTGRAY

        backgroundPaint = Paint(basePaint)
        backgroundPaint.color = Color.BLACK
        backgroundPaint.style = Paint.Style.FILL

        val plotColors = listOf(
            null,
            //Color.parseColor("#00BF00"), // Green
            Color.GREEN,
            Color.CYAN,
            Color.BLUE,
            Color.RED
        )

        for (color in plotColors) {
            plotPaint.add(PlotPaint.byColor(color ?: typedValue.data, textSize))
        }

        // markerPaint[PlotMarkerType.CHARGE] = PlotMarkerPaint.byColor(Color.rgb(237, 218, 75), textSize)
        markerPaint[PlotMarkerType.CHARGE] = PlotMarkerPaint.byColor(getColor(context, R.color.charge_marker), textSize)
        markerIcon[PlotMarkerType.CHARGE] = getVectorBitmap(context, R.drawable.ic_marker_charge)

        // markerPaint[PlotMarkerType.PARK] = PlotMarkerPaint.byColor(Color.rgb(93, 110,204), textSize)
        markerPaint[PlotMarkerType.PARK] = PlotMarkerPaint.byColor(getColor(context, R.color.park_marker), textSize)
        //TODO: ParkingIcon
        markerIcon[PlotMarkerType.PARK] = getVectorBitmap(context, R.drawable.ic_marker_park)
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
                    false
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

    private val mGestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            val shiftInterval = dimensionShiftTouchInterval
            if (shiftInterval != null) {
                touchDimensionShiftDistance += - distanceX
                dimensionShift = (touchDimensionShift + (touchDimensionShiftDistance / touchActionDistance.coerceAtLeast(1L)).toLong() * shiftInterval)
                    .coerceAtMost(touchDimensionMax + (shiftInterval - touchDimensionMax % shiftInterval) - (dimensionRestriction!! - 1))
                    .coerceAtLeast(0L)
            }

            val restrictionInterval = dimensionRestrictionTouchInterval
            if (restrictionInterval != null) {
                touchDimensionRestrictionDistance += - distanceY
                dimensionRestriction = (touchDimensionRestriction + (touchDimensionRestrictionDistance / touchActionDistance.coerceAtLeast(1L)).toLong() * restrictionInterval)
                    .coerceAtMost(touchDimensionMax + (restrictionInterval - touchDimensionMax % restrictionInterval))
                    .coerceAtLeast(restrictionInterval)
            }

            return true
        }
    }

    private val mScaleDetector = GestureDetector(context, mGestureListener)

    private var touchDimensionShift : Long = 0L
    private var touchDimensionShiftDistance : Float = 0f

    private var touchDimensionRestriction : Long = 0L
    private var touchDimensionRestrictionDistance : Float = 0f

    private var touchActionDistance : Long = 1L

    private var touchDimensionMax : Long = 0L

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (dimensionRestriction == null) return true
        if (dimensionShiftTouchInterval == null && dimensionRestrictionTouchInterval == null) return true

        when (ev.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                touchDimensionShiftDistance = 0f
                touchDimensionRestrictionDistance = 0f

                touchDimensionShift = dimensionShift ?: 0L
                touchDimensionRestriction = dimensionRestriction ?: 0L

                touchDimensionMax = (plotLines.mapNotNull { it.distanceDimension(dimension) }.max() ?: 0f).toLong()
            }
        }

        mScaleDetector.onTouchEvent(ev)
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        touchActionDistance = (((canvas.width - 2 * xMargin).toFloat() / xLineCount.toFloat()) * 0.75f).toLong()

        alignZero()
        drawBackground(canvas)
        drawXLines(canvas)
        drawYBaseLines(canvas)
        drawPlot(canvas)
        drawYLines(canvas)
    }

    private fun drawBackground(canvas: Canvas) {
        val maxX = canvas.width.toFloat()
        val maxY = canvas.height.toFloat()

        canvas.drawRect(xMargin.toFloat(), yMargin.toFloat(), maxX - xMargin, maxY - yMargin, backgroundPaint)
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

    private fun drawPlot(canvas: Canvas) {
        val maxX = canvas.width.toFloat()
        val maxY = canvas.height.toFloat()

        for (line in plotLines) {
            if (line.isEmpty() || !line.Visible) continue

            val dataPoints = line.getDataPoints(dimension, dimensionRestriction, dimensionShift)
            if (dataPoints.isEmpty()) continue

            val minValue = line.minValue(dataPoints)!!
            val maxValue = line.maxValue(dataPoints)!!

            val minDimension = line.minDimension(dataPoints, dimension, dimensionRestriction)
            val maxDimension = line.maxDimension(dataPoints, dimension)

            val smoothing = when {
                dimensionSmoothing != null -> dimensionSmoothing
                dimensionSmoothingPercentage != null -> (line.distanceDimension(dataPoints, dimension, dimensionRestriction) * dimensionSmoothingPercentage!!).toLong()
                else -> null
            }

            val zeroCord = y(line.Range.backgroundZero, minValue, maxValue, maxY)

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
                                y(point.y.Value, minValue, maxValue, maxY)!!
                            )
                        }
                        else -> {
                            val x = when (plotPoints.size) {
                                0 -> group.value.minBy { it.x }.x
                                else -> group.value.maxBy { it.x }.x
                            }

                            PlotPoint(
                                x(x, 0f, 1f, maxX)!!,
                                y(line.averageValue(group.value.map { it.y }, dimension), minValue, maxValue, maxY)!!
                            )
                        }
                    }

                    plotPoints.add(plotPoint)
                }

                plotPointCollection.add(plotPoints)
            }

            canvas.save()

            // TOP
            canvas.clipOutRect(0f, 0f, maxX, yMargin.toFloat())
            // BOTTOM
            canvas.clipOutRect(0f, maxY - yMargin, maxX, maxY)
            // LEFT
            canvas.clipOutRect(0f, 0f, xMargin.toFloat(), maxY)
            // RIGHT
            canvas.clipOutRect(maxX - xMargin, 0f, maxX, maxY)

            val joinedPlotPoints = ArrayList<PlotPoint>()

            for (plotPointIndex in plotPointCollection.indices) {
                val plotPoints = plotPointCollection[plotPointIndex]

                when (sessionGapRendering) {
                    PlotSessionGapRendering.JOIN -> joinedPlotPoints.addAll(plotPoints)
                    else -> drawPlot(canvas, line, plotPoints, zeroCord, plotPointIndex == plotLineItemPointCollection.size - 1)
                }
            }

            if (!joinedPlotPoints.isEmpty()) {
                drawPlot(canvas, line, joinedPlotPoints, zeroCord)
            }

            canvas.restore()

            if (plotMarkers?.markers?.isNotEmpty() ?: false) {
                val markers = plotMarkers!!.markers.filter { visibleMarkerTypes.contains(it.MarkerType) }

                for (markerGroup in markers.groupBy { line.x(dataPoints, it.StartTime, PlotDimension.TIME, dimension, minDimension, maxDimension) }) {
                    if (markerGroup.key == null) continue

                    val markerSorted = markerGroup.value.sortedBy { it.MarkerType }
                    val markerTypeGroup = markerSorted.groupBy { it.MarkerType }
                    val markerType = markerSorted.first().MarkerType

                    val markers = markerTypeGroup[markerType]!!

                    val x1 = x(markerGroup.key, 0f, 1f, maxX)
                    val x2 = x(line.x(dataPoints, markers.last().EndTime, PlotDimension.TIME, dimension, minDimension, maxDimension), 0f, 1f, maxX)

                    drawMarker(canvas, markerType, x1, -1)
                    drawMarker(canvas, markerType, x2, 1)

                    if (x1 != null && markers.last().EndTime != null) {
                        val diff = markers.sumOf { it.EndTime!! - it.StartTime }
                        val label = String.format("%02d:%02d", TimeUnit.MINUTES.convert(diff, TimeUnit.NANOSECONDS), TimeUnit.SECONDS.convert(diff, TimeUnit.NANOSECONDS) % 60)
                        val labelPaint = markerPaint[markerType]!!.Label

                        canvas.drawBitmap(
                            markerIcon[markerType]!!,
                            x1 - (textSize / 2f),
                            (yMargin / 3f),
                            labelPaint
                        )

                        canvas.drawText(
                            label,
                            x1 + labelPaint.textSize,
                            yMargin - labelPaint.textSize + 2f,
                            labelPaint
                        )
                    }
                }
            }
        }
    }

    private fun drawMarker(canvas: Canvas, markerType: PlotMarkerType, x: Float?, multiplier: Int) {
        if (x == null) return

        val top = yMargin.toFloat() + 1
        val bottom = canvas.height - yMargin.toFloat() - 1

        val linePath = Path()
        linePath.moveTo(x, top)
        linePath.lineTo(x, bottom)
        canvas.drawPath(linePath, markerPaint[markerType]!!.Line)

        val trianglePath = Path()
        trianglePath.moveTo(x, top)
        trianglePath.lineTo(x, yMargin.toFloat() + 15f)
        trianglePath.lineTo(x + (multiplier * 12f), top)
        trianglePath.lineTo(x, top)
        canvas.drawPath(trianglePath, markerPaint[markerType]!!.Mark)
    }

    private fun drawPlot(canvas: Canvas, line: PlotLine, plotPoints : ArrayList<PlotPoint>, zeroCord: Float?, lastPlot: Boolean = true) {
         val backgroundPaint = when (lastPlot) {
            true -> line.plotPaint!!.PlotBackground
            else -> line.plotPaint!!.PlotBackgroundSecondary
        }

        if (zeroCord != null) {
            drawPlot(canvas, backgroundPaint, plotPoints, zeroCord, true)
        }

        val plotPaint = when (lastPlot) {
            true -> line.plotPaint!!.Plot
            else -> line.plotPaint!!.PlotSecondary
        }

        drawPlot(canvas, plotPaint, plotPoints, zeroCord)
    }

    private fun drawPlot(canvas : Canvas, paint : Paint, plotPoints : ArrayList<PlotPoint>, zeroCord: Float?, background: Boolean = false) {
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

    private fun drawXLines(canvas: Canvas) {
        val maxX = canvas.width.toFloat()
        val maxY = canvas.height.toFloat()

        val distanceDimension = when {
            dimensionRestriction != null -> dimensionRestriction!!.toFloat()
            else -> plotLines.mapNotNull { it.distanceDimension(dimension, dimensionRestriction) }.max()?:0f
        }

        for (i in 0 until xLineCount) {
            val cordX = x(i.toFloat(), 0f, xLineCount.toFloat() - 1, maxX)!!
            val cordY = maxY - yMargin

            val leftZero = (distanceDimension / (xLineCount - 1) * i) + (dimensionShift ?: 0L)
            val rightZero = distanceDimension - leftZero + (2 * (dimensionShift ?: 0L))

            drawXLine(canvas, cordX, maxY, labelLinePaint)

            val label = when (dimension) {
                PlotDimension.INDEX -> String.format("%d", leftZero.toInt())
                PlotDimension.DISTANCE -> when {
                    rightZero < 1000 -> String.format("%d m", (rightZero - (rightZero % 100)).toInt())
                    else -> String.format("%d km", (rightZero / 1000).toInt())
                }
                PlotDimension.TIME -> String.format("%02d:%02d", TimeUnit.MINUTES.convert(leftZero.toLong(), TimeUnit.NANOSECONDS), TimeUnit.SECONDS.convert(leftZero.toLong(), TimeUnit.NANOSECONDS) % 60)
            }

            val bounds = Rect()
            labelPaint.getTextBounds(label, 0, label.length, bounds)

            canvas.drawText(
                label,
                cordX - bounds.width() / 2,
                cordY + yMargin / 2 + bounds.height() / 2,
                labelPaint
            )
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
        val maxX = canvas.width.toFloat()
        val maxY = canvas.height.toFloat()

        for (i in 0 until yLineCount) {
            val cordY = y(i.toFloat(), 0f, yLineCount.toFloat() - 1, maxY)!!
            drawYLine(canvas, cordY, maxX, labelLinePaint)
        }
    }

    private fun drawYLines(canvas: Canvas) {
        val maxX = canvas.width.toFloat()
        val maxY = canvas.height.toFloat()

        val bounds = Rect()
        labelPaint.getTextBounds("Dummy", 0, "Dummy".length, bounds)

        for (drawHighlightLabelOnly in listOf(false, true)) {
            for (line in plotLines.filter { it.Visible }) {
                val dataPoints = line.getDataPoints(dimension, dimensionRestriction, dimensionShift)

                val minValue = line.minValue(dataPoints)!!
                val maxValue = line.maxValue(dataPoints)!!
                val highlight = line.byHighlightMethod(dataPoints)

                val labelShiftY = (bounds.height() / 2).toFloat()
                val valueShiftY = (maxValue - minValue) / (yLineCount - 1)

                val labelUnitXOffset = when (line.LabelPosition) {
                    PlotLabelPosition.LEFT -> 0f
                    PlotLabelPosition.RIGHT -> -labelPaint.measureText(line.Unit) / 2
                    else -> 0f
                }

                val labelCordX = when (line.LabelPosition) {
                    PlotLabelPosition.LEFT -> textSize
                    PlotLabelPosition.RIGHT -> maxX - xMargin + textSize / 2
                    else -> null
                }

                val highlightCordY = y(highlight, minValue, maxValue, maxY)

                if (!drawHighlightLabelOnly) {
                    if (line.LabelPosition !== PlotLabelPosition.NONE && labelCordX != null) {
                        if (line.Unit.isNotEmpty()) {
                            canvas.drawText(line.Unit, labelCordX + labelUnitXOffset,yMargin - (yMargin / 3f), labelPaint)
                        }

                        for (i in 0 until yLineCount) {
                            val valueY = maxValue - i * valueShiftY
                            val cordY = y(valueY, minValue, maxValue, maxY)!!
                            val label = String.format(line.LabelFormat, valueY / line.Divider)

                            canvas.drawText(label, labelCordX, cordY + labelShiftY, labelPaint)
                        }
                    }

                    when (line.HighlightMethod) {
                        PlotHighlightMethod.AVG_BY_INDEX -> drawYLine(canvas, highlightCordY, maxX, line.plotPaint!!.HighlightLabelLine)
                        PlotHighlightMethod.AVG_BY_DISTANCE -> drawYLine(canvas, highlightCordY, maxX, line.plotPaint!!.HighlightLabelLine)
                        PlotHighlightMethod.AVG_BY_TIME -> drawYLine(canvas, highlightCordY, maxX, line.plotPaint!!.HighlightLabelLine)
                        else -> {
                            // Don't draw
                        }
                    }

                    for (baseLineAt in line.baseLineAt) {
                        drawYLine(canvas, y(baseLineAt, minValue, maxValue, maxY), maxX, baseLinePaint)
                    }
                } else {
                    if (labelCordX != null && highlightCordY != null) {
                        val label = String.format(line.HighlightFormat, highlight!! / line.Divider)
                        line.plotPaint!!.HighlightLabel.textSize = 35f
                        val labelWidth = line.plotPaint!!.HighlightLabel.measureText(label)
                        val labelHeight = line.plotPaint!!.HighlightLabel.textSize
                        val textBoxMargin = line.plotPaint!!.HighlightLabel.textSize / 3.5f

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
                            line.plotPaint!!.Plot
                        )

                        canvas.drawText(label, labelCordX + labelUnitXOffset, highlightCordY + labelShiftY, line.plotPaint!!.HighlightLabel)
                    }
                }
            }
        }
    }

    private fun drawYLine(canvas: Canvas, cord: Float?, maxX: Float, paint: Paint?) {
        if (cord == null) return
        val path = Path()
        path.moveTo(xMargin.toFloat(), cord)
        path.lineTo(maxX - xMargin, cord)
        canvas.drawPath(path, paint ?: labelLinePaint)
    }
}