package com.ixam97.carStatsViewer.plot.graphics

import android.graphics.Color
import android.graphics.Paint

class PlotMarkerPaint(
    val Line: Paint,
    val Mark: Paint,
    val Label: Paint
){
    companion object {
        fun byColor(color : Int, textSize: Float): PlotMarkerPaint {
            val basePaint = basePaint(textSize)

            val linePaint = Paint(basePaint)
            linePaint.strokeWidth = 2f
            linePaint.color = Color.argb(64, Color.red(color), Color.green(color), Color.blue(color))
            linePaint.style = Paint.Style.STROKE

            val markerPaint = Paint(linePaint)
            markerPaint.color = color
            markerPaint.style = Paint.Style.FILL_AND_STROKE

            val labelPaint = Paint(basePaint)
            labelPaint.color = color
            labelPaint.textSize = textSize - 6f

            return PlotMarkerPaint(linePaint, markerPaint, labelPaint)
        }

        fun basePaint(textSize: Float): Paint {
            // defines paint and canvas
            val basePaint = Paint()
            basePaint.color = Color.GRAY
            basePaint.isAntiAlias = true
            basePaint.strokeWidth = 1f
            basePaint.style = Paint.Style.FILL
            basePaint.strokeJoin = Paint.Join.ROUND
            basePaint.strokeCap = Paint.Cap.ROUND
            basePaint.textSize = textSize

            return basePaint
        }
    }
}