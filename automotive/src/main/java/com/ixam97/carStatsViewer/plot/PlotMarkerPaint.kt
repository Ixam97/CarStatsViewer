package com.ixam97.carStatsViewer.plot

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

class PlotMarkerPaint(
    val Line: Paint,
    val Mark: Paint,
    val Label: Paint
){
    companion object {
        fun byColor(color : Int, textSize: Float): PlotMarkerPaint {
            val basePaint = basePaint(textSize);

            val linePaint = Paint(basePaint)
            linePaint.strokeWidth = 2f
            linePaint.color = color
            linePaint.style = Paint.Style.STROKE

            val markerPaint = Paint(linePaint)
            markerPaint.style = Paint.Style.FILL_AND_STROKE

            val labelPaint = Paint(basePaint)
            labelPaint.color = color

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