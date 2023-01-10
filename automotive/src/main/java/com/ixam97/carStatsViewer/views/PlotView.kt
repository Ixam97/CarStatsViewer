package com.ixam97.carStatsViewer.views

import android.R
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.ixam97.carStatsViewer.plot.*
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.math.ceil

class PlotView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    private val textSize = 26f

    var xMargin: Int = 100
        set(value) {
            if (value > 0) {
                field = value
                invalidate()
            }
        }

    var xLineCount: Int = 6
        set(value) {
            if (value > 1) {
                field = value
                invalidate()
            }
        }

    var yMargin: Int = 60
        set(value) {
            if (value > 0) {
                field = value
                invalidate()
            }
        }

    var yLineCount: Int = 5
        set(value) {
            if (value > 1) {
                field = value
                invalidate()
            }
        }

    var dimension: PlotDimension = PlotDimension.INDEX
    var dimensionRestriction: Float? = null
        set(value) {
            field = value
            invalidate()
        }

    private val plotLines = ArrayList<PlotLine>()
    private val plotPaint = ArrayList<PlotPaint>()

    private lateinit var labelPaint: Paint
    private lateinit var labelLinePaint: Paint
    private lateinit var baseLinePaint: Paint
    private lateinit var backgroundPaint: Paint

    init {
        setupPaint()
    }

    // Setup paint with color and stroke styles
    private fun setupPaint() {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(R.attr.colorControlActivated, typedValue, true)

        val basePaint = PlotPaint.basePaint(textSize)

        labelLinePaint = Paint(basePaint)
        labelLinePaint.color = Color.DKGRAY

        labelPaint = Paint(labelLinePaint)
        labelPaint.style = Paint.Style.FILL

        baseLinePaint = Paint(labelLinePaint)
        baseLinePaint.color = Color.LTGRAY

        backgroundPaint = Paint(basePaint)
        backgroundPaint.color = Color.BLACK
        backgroundPaint.style = Paint.Style.FILL

        val plotColors = listOf(
            null,
            Color.GREEN,
            Color.CYAN,
            Color.BLUE,
            Color.RED
        )

        for (color in plotColors) {
            plotPaint.add(PlotPaint.byColor(color ?: typedValue.data, textSize))
        }
    }

    fun reset() {
        for (item in plotLines) {
            item.reset()
        }
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
                yMargin + abs(px - px * value)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawXLines(canvas)
        drawYLines(canvas)
        // drawPlot(canvas) Moved to drawYLines to move it behind labels
    }

    private fun drawPlot(canvas: Canvas) {
        val maxX = canvas.width.toFloat()
        val maxY = canvas.height.toFloat()
        val areaX = maxX - 2 * xMargin

        for (line in plotLines) {
            if (line.isEmpty() || !line.Visible) continue

            val dataPoints = line.getDataPoints(dimension, dimensionRestriction)
            if (dataPoints.isEmpty()) continue

            val minValue = line.minValue(dataPoints)!!
            val maxValue = line.maxValue(dataPoints)!!

            val prePlotPointCollections = ArrayList<ArrayList<PlotLineItemPoint>>()
            var prePlotPoints = ArrayList<PlotLineItemPoint>()

            for (index in dataPoints.indices) {
                val item = dataPoints[index]

                prePlotPoints.add(
                    PlotLineItemPoint(
                        when (dimension) {
                            PlotDimension.INDEX -> PlotLineItem.cord((dimensionRestriction?:dataPoints.size.toFloat()) - dataPoints.size + index, 0f, (dimensionRestriction?:dataPoints.size.toFloat()) - 1f)
                            PlotDimension.DISTANCE -> line.x(item.Distance, dimension, dimensionRestriction, dataPoints)
                            PlotDimension.TIME -> line.x(item.timestampInSeconds, dimension, dimensionRestriction, dataPoints)
                        },
                        item
                    )
                )

                if (item.Marker == PlotMarker.END_SESSION) {
                    prePlotPointCollections.add(prePlotPoints)
                    prePlotPoints = ArrayList()
                }
            }
            prePlotPointCollections.add(prePlotPoints)

            val postPlotPointCollections = ArrayList<ArrayList<PlotPoint>>()
            for (midPlotPoints in prePlotPointCollections) {
                if (midPlotPoints.isEmpty()) continue

                val postPlotPoints = ArrayList<PlotPoint>()
                val min = midPlotPoints.minBy { it.x }?.x!!
                val groupBy = 5 / areaX

                for (group in midPlotPoints.groupBy { ceil((it.x - min) / groupBy).toInt() }) {
                    val postPoint = when (group.value.size > 1) {
                        false -> {
                            val point = group.value.first()
                            PlotPoint(
                                x(point.x, 0f, 1f, maxX)!!,
                                y(point.y.Value, minValue, maxValue, maxY)!!
                            )
                        }
                        else -> {
                            val x = when (group.key) {
                                0 -> group.value.minBy { it.x }?.x!!
                                else -> group.value.maxBy { it.x }?.x!!
                            }

                            PlotPoint(
                                x(x, 0f, 1f, maxX)!!,
                                y(line.averageValue(group.value.map { it.y }, dimension), minValue, maxValue, maxY)!!
                            )
                        }
                    }

                    postPlotPoints.add(postPoint)
                }
                postPlotPointCollections.add(postPlotPoints)
            }

            for (postPlotPoints in postPlotPointCollections) {
                val path = Path()
                var prevPoint: PlotPoint? = null

                for (i in postPlotPoints.indices) {
                    val point = postPlotPoints[i]

                    if (i == 0) {
                        path.moveTo(point.x, point.y)
                        if (postPlotPoints.size == 1) {
                            path.lineTo(point.x, point.y)
                        }
                    } else if (prevPoint != null) {
                        val midX = (prevPoint.x + point.x) / 2
                        val midY = (prevPoint.y + point.y) / 2

                        if (i == 1) {
                            path.lineTo(midX, midY)
                        } else {
                            path.quadTo(prevPoint.x, prevPoint.y, midX, midY)
                        }
                    }

                    prevPoint = point
                }

                if (prevPoint != null) {
                    path.lineTo(prevPoint.x, prevPoint.y)
                }

                canvas.drawPath(path, line.plotPaint!!.Plot)
            }
        }
    }

    private fun drawXLines(canvas: Canvas) {
        val maxX = canvas.width.toFloat()
        val maxY = canvas.height.toFloat()

        val minDimension = when (dimensionRestriction) {
           null -> plotLines.mapNotNull { it.minDimension(dimension) }.min()?:0f
           else -> 0f
        }

        val maxDimension = when (dimensionRestriction) {
            null -> plotLines.mapNotNull { it.maxDimension(dimension) }.max()?:0f
            else -> dimensionRestriction!! - 1
        }

        val distanceDimension = maxDimension - minDimension

        for (i in 0 until xLineCount) {
            val cordX = x(i.toFloat(), 0f, xLineCount.toFloat() - 1, maxX)!!
            val cordY = maxY - yMargin

            val leftZero = distanceDimension / (xLineCount - 1) * i
            val rightZero = distanceDimension - leftZero

            drawXLine(canvas, cordX, maxY, labelLinePaint)

            val label = when (dimension) {
                PlotDimension.INDEX -> String.format("%d", leftZero.toInt())
                PlotDimension.DISTANCE -> when(rightZero < 1000) {
                    true -> String.format("%d m", (rightZero - (rightZero % 100)).toInt())
                    else -> String.format("%d km", (rightZero / 1000).toInt())
                }
                PlotDimension.TIME -> String.format("%02d:%02d", (rightZero / 60).toInt(), (rightZero % 60).toInt())
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

    private fun drawYLines(canvas: Canvas) {
        val maxX = canvas.width.toFloat()
        val maxY = canvas.height.toFloat()

        for (i in 0 until yLineCount) {
            val cordY = y(i.toFloat(), 0f, yLineCount.toFloat() - 1, maxY)!!
            drawYLine(canvas, cordY, maxX, labelLinePaint)
        }

        val bounds = Rect()
        labelPaint.getTextBounds("Dummy", 0, "Dummy".length, bounds)

        for (line in plotLines) {
            if (line.isEmpty() || !line.Visible) continue

            val dataPoints = line.getDataPoints(dimension, dimensionRestriction)
            if (dataPoints.isEmpty()) continue

            val minValue = line.minValue(dataPoints)!!
            val maxValue = line.maxValue(dataPoints)!!
            val highlight = line.byHighlightMethod(dataPoints)

            val labelShiftY = (bounds.height() / 2).toFloat()
            val valueShiftY = (maxValue - minValue) / (yLineCount - 1)

            val labelUnitXOffset = when (line.LabelPosition) {
                PlotLabelPosition.LEFT -> 0f
                PlotLabelPosition.RIGHT -> -labelPaint.measureText(line.Unit)/2
                else -> 0f
            }

            val labelCordX = when (line.LabelPosition) {
                PlotLabelPosition.LEFT -> textSize
                PlotLabelPosition.RIGHT -> maxX - xMargin + textSize/2
                else -> null
            }

            val highlightCordY = y(highlight, minValue, maxValue, maxY)

            if (line.LabelPosition !== PlotLabelPosition.NONE && labelCordX != null) {
                if (line.Unit.isNotEmpty()) {
                    canvas.drawText(line.Unit, labelCordX + labelUnitXOffset,yMargin - (yMargin / 3f), labelPaint)
                }

                for (i in 0 until yLineCount) {
                    val valueY = maxValue - i * valueShiftY
                    val cordY = y(valueY, minValue, maxValue, maxY)!!
                    val label = String.format(line.LabelFormat, valueY / line.Divider)

                    // not render label when highlight is covering it
                    if (highlightCordY == null || abs(cordY - highlightCordY) > textSize) {
                        canvas.drawText(label, labelCordX, cordY + labelShiftY, labelPaint)
                    }
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

            drawPlot(canvas)

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

    private fun drawYLine(canvas: Canvas, cord: Float?, maxX: Float, paint: Paint?) {
        if (cord == null) return
        val path = Path()
        path.moveTo(xMargin.toFloat(), cord)
        path.lineTo(maxX - xMargin, cord)
        canvas.drawPath(path, paint ?: labelLinePaint)
    }
}