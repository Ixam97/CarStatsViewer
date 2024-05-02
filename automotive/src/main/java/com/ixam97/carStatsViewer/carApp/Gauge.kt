package com.ixam97.carStatsViewer.carApp

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.car.app.CarContext
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.utils.InAppLogger
import kotlin.math.roundToInt

class Gauge(
    carContext: CarContext,
) {

    var valueTextSize = 100f

    private val valuePaint = Paint().apply {
        color = Color.BLACK
        textSize = valueTextSize
        CarStatsViewer.typefaceRegular?.let {
            typeface = it
            letterSpacing = -0.025f
        }
        isAntiAlias = true
    }

    private val posPaint = Paint().apply {
        color = carContext.getColor(R.color.polestar_orange)
    }

    private val negPaint = Paint().apply {
        color = Color.LTGRAY
    }

    private val borderPaint = Paint().apply {
        color = Color.DKGRAY
        color = Color.argb(
            (Color.alpha(color) * .5f).roundToInt(),
            Color.red(color),
            Color.green(color),
            Color.blue(color))
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val zeroLinePaint = Paint().apply {
        color = Color.GRAY
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private var gageBarRect = RectF()
    private val gageBorder = Path()
    private val gageZeroLine = Path()

    fun draw(size: Int, value: Float, min: Float = 0f, max: Float = 100f): Bitmap {

        if (min >= max) throw Exception("Min can't be larger than Max!")

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val floatSize = size.toFloat()

        val zeroLineY: Float = floatSize * (max / (max - min))
        val valueLineY: Float = (zeroLineY - value * (floatSize / (max - min))).apply {
            coerceAtLeast(0f)
            coerceAtMost(floatSize)
        }

        if (value > 0 || value < 0) {
            gageBarRect.apply {
                left = 0f
                right = floatSize
                bottom = zeroLineY
                top = valueLineY
            }
            canvas.drawRect(gageBarRect, if (value > 0) posPaint else negPaint)
        }

        if (min < 0) {
            gageZeroLine.apply {
                reset()
                moveTo(0f, zeroLineY)
                lineTo(floatSize, zeroLineY)
            }
            canvas.drawPath(gageZeroLine, zeroLinePaint)
        }

        gageBorder.apply {
            reset()
            moveTo(0f + borderPaint.strokeWidth/2, 0f + borderPaint.strokeWidth/2)
            lineTo(0f + borderPaint.strokeWidth/2, floatSize  - borderPaint.strokeWidth/2)
            lineTo(floatSize - borderPaint.strokeWidth/2, floatSize - borderPaint.strokeWidth/2)
            lineTo(floatSize - borderPaint.strokeWidth/2, 0f + borderPaint.strokeWidth/2)
            close()
        }
        canvas.drawPath(gageBorder, borderPaint)

        return bitmap
    }
}