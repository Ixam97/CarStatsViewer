package com.ixam97.carStatsViewer.carApp.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.car.app.CarContext
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import kotlin.math.max
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
        selected: Boolean = false,
        textOnly: Boolean = false,
    ): Bitmap = draw(size, size, value, min, max, valueString, unitString, selected, textOnly)

    fun draw(
        width: Int,
        height: Int,
        value: Float,
        min: Float = 0f,
        max: Float = 100f,
        valueString: String? = null,
        unitString: String? = null,
        selected: Boolean = false,
        textOnly: Boolean = false,
    ): Bitmap {

        valuePaint.textSize = max(width, height) / 4f
        unitPaint.textSize = max(width, height) / 8f

        val bottomLine: Float = if (valueString != null) {
            height - valuePaint.textSize
        } else {
            height.toFloat()
        }

        val topLine: Float = if (valueString != null) {
            valuePaint.textSize
        } else {
            0f
        }

        val textOffsetY = valuePaint.textSize / 10f

        if (min >= max) throw Exception("Min can't be larger than Max!")

        val bitmap = Bitmap.createBitmap(max(width, height), max(width, height), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val floatHeight = height.toFloat()
        val floatWidth = width.toFloat()

        if (!textOnly) {
            val zeroLineY: Float = bottomLine * (max / (max - min))
            val zeroLineX: Float = floatWidth - (floatWidth * (max / (max - min)))
            var valueLineY: Float = (zeroLineY - value * (bottomLine / (max - min)))
            var valueLineX: Float = (zeroLineX + value * (floatWidth / (max - min)))

            if (valueLineY > bottomLine) valueLineY = bottomLine
            else if (valueLineY <= 0) valueLineY = 0f

            if (valueLineX > floatWidth) valueLineX = floatWidth
            else if (valueLineX <= 0) valueLineX = 0f

            if (value > 0 || value < 0) {
                gageBarRect.apply {
                    if (width < height) {
                        left = 0f
                        right = floatWidth
                        bottom = zeroLineY
                        top = valueLineY
                    } else {
                        left = zeroLineX
                        right = valueLineX
                        top = topLine
                        bottom = floatHeight
                    }
                }
                canvas.drawRect(gageBarRect, if (value > 0) posPaint else negPaint)
            }

            if (min < 0) {
                gageZeroLine.apply {
                    if (width < height) {
                        reset()
                        moveTo(0f, zeroLineY)
                        lineTo(floatWidth, zeroLineY)
                        close()
                    } else {
                        reset()
                        moveTo(zeroLineX, topLine)
                        lineTo(zeroLineX, floatHeight)
                        close()
                    }
                }
                canvas.drawPath(gageZeroLine, zeroLinePaint)
            }

            gageBorder.apply {
                reset()
                moveTo(0f + borderPaint.strokeWidth/2, topLine + borderPaint.strokeWidth/2)
                lineTo(0f + borderPaint.strokeWidth/2, floatHeight  - borderPaint.strokeWidth/2)
                lineTo(floatWidth - borderPaint.strokeWidth/2, floatHeight - borderPaint.strokeWidth/2)
                lineTo(floatWidth - borderPaint.strokeWidth/2, topLine + borderPaint.strokeWidth/2)
                close()
            }
            canvas.drawPath(gageBorder, borderPaint)
        }

        valueString?.let { it ->
            canvas.drawText(it, 0f, topLine - textOffsetY, valuePaint)
            if (unitString != null) {
                val xOffset = valuePaint.measureText(valueString) + 10f
                canvas.drawText(unitString, xOffset, topLine - textOffsetY, unitPaint)
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

    fun drawPowerGauge(width: Int, height: Int, value: Float): Bitmap {
        val valueString = "${((value/1_000_000) * 10).toInt() / 10f }"
        return draw(width, height, value/1_000_000, min = -150f, max = 300f, valueString, "kW")
    }

    fun drawConsumptionGauge(width: Int, height: Int, valueInstCons: Float?, valueSpeed: Float?): Bitmap {
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

        return draw(width, height, instCons?:0f, -300f, 600f, valueString, consUnit)
    }
}