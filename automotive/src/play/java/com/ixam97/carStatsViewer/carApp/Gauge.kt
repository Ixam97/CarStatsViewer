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
import kotlin.math.roundToInt

class Gauge(
    val carContext: CarContext,
) {

    val appPreferences = CarStatsViewer.appPreferences

    private val valuePaint = Paint().apply {
        color = Color.WHITE
        textSize = 100f
        isAntiAlias = true
    }

    private val unitPaint = Paint().apply {
        color = carContext.getColor(R.color.polestar_orange)
        textSize = 50f
        isAntiAlias = true
    }

    private val posPaint = Paint().apply {
        color = carContext.getColor(R.color.polestar_orange)
    }

    private val blackPaint = Paint().apply {
        color = Color.BLACK
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

    fun draw(
        size: Int,
        value: Float,
        min: Float = 0f,
        max: Float = 100f,
        valueString: String? = null,
        unitString: String? = null,
        selected: Boolean = false
    ): Bitmap {

        valuePaint.textSize = size / 4f
        unitPaint.textSize = size / 8f

        val bottomLine: Float = if (valueString != null) {
            size - valuePaint.textSize
        } else {
            size.toFloat()
        }

        val bottomOffset = size / 50f

        if (min >= max) throw Exception("Min can't be larger than Max!")

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val floatSize = size.toFloat()

        val zeroLineY: Float = bottomLine * (max / (max - min))
        var valueLineY: Float = (zeroLineY - value * (bottomLine / (max - min)))

        if (valueLineY > bottomLine) valueLineY = bottomLine
        else if (valueLineY <= 0) valueLineY = 0f

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
            lineTo(0f + borderPaint.strokeWidth/2, bottomLine  - borderPaint.strokeWidth/2)
            lineTo(floatSize - borderPaint.strokeWidth/2, bottomLine - borderPaint.strokeWidth/2)
            lineTo(floatSize - borderPaint.strokeWidth/2, 0f + borderPaint.strokeWidth/2)
            close()
        }
        canvas.drawPath(gageBorder, borderPaint)

        valueString?.let { it ->
            canvas.drawText(it, 0f, floatSize - bottomOffset, valuePaint)
            if (unitString != null) {
                val xOffset = valuePaint.measureText(valueString) + 10f
                canvas.drawText(unitString, xOffset, floatSize - bottomOffset, unitPaint)
            }
        }

        if (selected) {
            canvas.drawCircle((canvas.width - 21).toFloat(), (canvas.height - 21).toFloat(), 18f, blackPaint)
            val iconDrawable = carContext.getDrawable(R.drawable.ic_car_app_check)
            iconDrawable?.setBounds(canvas.width - 37, canvas.height - 37, canvas.width - 5, canvas.height - 5)
            iconDrawable?.draw(canvas)
        }

        return bitmap
    }

    fun drawPowerGauge(size: Int, value: Float): Bitmap {
        val valueString = "${((value/1_000_000) * 10).toInt() / 10f }"
        return draw(size, value/1_000_000, min = -150f, max = 300f, valueString, "kW")
    }

    fun drawConsumptionGauge(size: Int, valueInstCons: Float?, valueSpeed: Float?): Bitmap {
        val instConsVal: Number? = if (valueInstCons != null && (valueSpeed?:0f) * 3.6 > 3) {
            if (appPreferences.consumptionUnit) {
                appPreferences.distanceUnit.asUnit(valueInstCons).roundToInt()
            } else {
                appPreferences.distanceUnit.asUnit(valueInstCons).roundToInt() / 10
            }
        } else {
            null
        }

        val consUnit = if (appPreferences.consumptionUnit) {
            "Wh/${appPreferences.distanceUnit.unit()}"
        } else {
            "kWh/100${appPreferences.distanceUnit.unit()}"
        }

        val valueString = "${instConsVal?: "∞"}"

        val instCons = if ((valueSpeed?:0f) * 3.6 > 3) valueInstCons else null

        return draw(size, instCons?:0f, -300f, 600f, valueString, consUnit)
    }
}