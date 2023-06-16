package com.ixam97.carStatsViewer.plot.graphics

import android.graphics.Color
import android.graphics.Paint

class PlotMarkerPaint(
    val Mark: Paint,
    val Label: Paint
){
    companion object {
        fun byColor(color : Int, textSize: Float): PlotMarkerPaint {
            val basePaint = basePaint(textSize)

            val markerPaint = Paint(basePaint)
            markerPaint.strokeWidth = 2f
            markerPaint.color = Color.argb(64, Color.red(color), Color.green(color), Color.blue(color))
            markerPaint.style = Paint.Style.FILL_AND_STROKE

            val labelPaint = Paint(basePaint)
            labelPaint.color = color
            labelPaint.textSize = textSize - 6f

            return PlotMarkerPaint(markerPaint, labelPaint)
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