package com.ixam97.carStatsViewer.plot

import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint

class PlotPaint(
    val Plot: Paint,
    val PlotBackground: Paint,
    val HighlightLabel: Paint,
    val HighlightLabelLine: Paint
) {
    companion object {
        fun byColor(color : Int, textSize: Float): PlotPaint {
            val basePaint = basePaint(textSize);

            val plotPaint = Paint(basePaint)
            plotPaint.color = color
            plotPaint.strokeWidth = 3f

            val plotBackgroundPaint = Paint(plotPaint)
            plotBackgroundPaint.color = Color.argb(32, Color.red(color), Color.green(color), Color.blue(color))
            plotBackgroundPaint.style = Paint.Style.FILL

            val highlightLabelPaint = Paint(basePaint)
            highlightLabelPaint.color = color
            highlightLabelPaint.style = Paint.Style.FILL

            val highlightLabelLinePaint = Paint(plotPaint)
            highlightLabelLinePaint.strokeWidth = 3f
            highlightLabelLinePaint.pathEffect = DashPathEffect(floatArrayOf(5f, 10f), 0f)

            return PlotPaint(plotPaint, plotBackgroundPaint, highlightLabelPaint, highlightLabelLinePaint)
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

            return basePaint
        }
    }
}