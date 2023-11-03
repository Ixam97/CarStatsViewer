package com.coderax.carStatsViewer.ui.plot.graphics

import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface

class PlotPaint(
    val Plot: Paint,
    val PlotGap: Paint,
    val PlotBackground: Paint,

    val PlotSecondary: Paint,
    val PlotGapSecondary: Paint,
    val PlotBackgroundSecondary: Paint,

    val Color: Int,
    val TransparentColor: Int,

    val HighlightLabel: Paint,
    val HighlightLabelLine: Paint
) {
    companion object {
        private val paintCache : HashMap<Int, HashMap<Float, PlotPaint>> = HashMap()

        var typeface: Typeface? = null
        var letterSpacing: Float? = null

        fun byColor(color : Int, textSize: Float): PlotPaint {

            val cached = paintCache[color]?.get(textSize)
            if (cached != null) return cached

            val basePaint = basePaint(textSize)

            val plotPaint = Paint(basePaint)
            plotPaint.color = color
            plotPaint.strokeWidth = 3f

            val plotGapPaint = Paint(plotPaint)
            plotGapPaint.color = Color.argb(128, Color.red(color), Color.green(color), Color.blue(color))
            plotGapPaint.pathEffect = DashPathEffect(floatArrayOf(2f, 10f), 0f)

            val plotBackgroundPaint = Paint(plotPaint)
            plotBackgroundPaint.color = Color.argb(160, Color.red(color), Color.green(color), Color.blue(color))
            plotBackgroundPaint.style = Paint.Style.FILL

//            val plotSecondaryPaint = Paint(plotPaint)
//            plotSecondaryPaint.color = Color.argb(160, Color.red(color), Color.green(color), Color.blue(color))
//            plotSecondaryPaint.strokeWidth = 2f
//
//            val plotGapSecondaryPaint = Paint(plotSecondaryPaint)
//            plotGapSecondaryPaint.pathEffect = DashPathEffect(floatArrayOf(5f, 10f), 0f)
//
//            val plotBackgroundSecondaryPaint = Paint(plotBackgroundPaint)
//            plotBackgroundSecondaryPaint.color = Color.argb(32, Color.red(color), Color.green(color), Color.blue(color))
//            plotBackgroundSecondaryPaint.strokeWidth = 2f

            val highlightLabelPaint = Paint(basePaint)
            highlightLabelPaint.color = color
            highlightLabelPaint.style = Paint.Style.FILL

            val highlightLabelLinePaint = Paint(plotPaint)
            highlightLabelLinePaint.strokeWidth = 3f
            highlightLabelLinePaint.pathEffect = DashPathEffect(floatArrayOf(5f, 10f), 0f)

            val paint = PlotPaint(
                plotPaint,
                plotGapPaint,
                plotBackgroundPaint,
                plotPaint, // use same as primary for now
                plotGapPaint,
                plotBackgroundPaint,
                color, // use same as primary for now
                Color.argb(0, Color.red(color), Color.green(color), Color.blue(color)),
                highlightLabelPaint,
                highlightLabelLinePaint
            )

            if (paintCache[color] == null) paintCache[color] = HashMap()

            paintCache[color]?.set(textSize, paint)

            return paint
        }

        fun basePaint(textSize: Float): Paint {
            // defines paint and canvas
            val basePaint = Paint()
            basePaint.isAntiAlias = true
            basePaint.strokeWidth = 1f
            basePaint.style = Paint.Style.STROKE
            basePaint.strokeJoin = Paint.Join.ROUND
            basePaint.strokeCap = Paint.Cap.ROUND
            basePaint.textSize = textSize
            typeface?.let {
                basePaint.typeface = it
            }
            letterSpacing?.let {
                basePaint.letterSpacing = it
            }

            return basePaint
        }
    }
}