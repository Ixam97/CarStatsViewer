package com.ixam97.carStatsViewer.ui.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.ixam97.carStatsViewer.CarStatsViewer
import java.util.*
import kotlin.math.roundToInt


class GageView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    companion object {
        var descriptionTextSize = 30f
        var valueTextSize = 100f
    }

    var gageName : String = "gageName"
        set(value) {
            field = "$value |"
            this.invalidate()
        }
    var gageUnit : String = "gageUnit"
        set(value) {
            field = value
            this.invalidate()
        }

    var minValue = 0f
    var maxValue = 1f

    var barVisibility : Boolean = true
        set(value) {
            field = value
            this.invalidate()
        }

    var primaryColor: Int? = null
        set(value) {
            field = value
            unitPaint.color = getPrimaryColor()
            posPaint.color = getPrimaryColor()
        }

    private var gageValueInt : Int? = null
    private var gageValueFloat: Float? = null

    fun setValue(value: Int?) {
        gageValueInt = value
        gageValueFloat = null
        this.invalidate()
    }


    fun setValue(value: Float?) {
        gageValueFloat = value
        gageValueInt = null
        this.invalidate()
    }

    private val posPaint = Paint().apply {
        color = getPrimaryColor()
    }

    private val negPaint = Paint().apply {
        color = Color.LTGRAY
    }

    private val namePaint = Paint().apply {
        color = Color.GRAY
        textSize = descriptionTextSize
        CarStatsViewer.typefaceRegular?.let {
            typeface = it
            letterSpacing = -0.025f
        }
        isAntiAlias = true
    }

    private val unitPaint = Paint().apply {
        color = getPrimaryColor()
        textSize = descriptionTextSize
        CarStatsViewer.typefaceRegular?.let {
            typeface = it
            letterSpacing = -0.025f
        }
        isAntiAlias = true
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

    private val backgroundPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }
    private val zeroLinePaint = Paint().apply {
        color = Color.GRAY
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val valuePaint = Paint().apply {
        color = Color.WHITE
        textSize = valueTextSize
        CarStatsViewer.typefaceRegular?.let {
            typeface = it
            letterSpacing = -0.025f
        }
        isAntiAlias = true
    }

    private val xTextMargin = dpToPx(15f)
    private val yTextMargin = dpToPx(10f)
    private val gageWidth = 2 * descriptionTextSize

    private val nameYPos = namePaint.textSize * 0.76f
    private val valueYPos = nameYPos + valuePaint.textSize * 0.9f
    private val unitYPos = valueYPos - (valuePaint.textSize * 0.8f - unitPaint.textSize)

    private val viewHeight = valueYPos + dpToPx(3f)
    private var viewWidth = 0f

    private var gageBarRect = RectF()
    private val gageBorder = Path()
    private val gageZeroLine = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        var gageValue = 0f
        var gagePaint = posPaint

        if (gageValueInt != null) gageValue = gageValueInt!!.toFloat()
        if (gageValueFloat != null) gageValue = gageValueFloat!!

        if (gageValue < 0) gagePaint = negPaint

        var gageZeroLineYPos = ((viewHeight - borderPaint.strokeWidth)/(maxValue - minValue)) * maxValue

        if (gageValue < minValue) gageValue = minValue
        if (gageValue > maxValue) gageValue = maxValue

        val gageValueWidth = valuePaint.measureText(gageValue())
        val gageNameWidth = namePaint.measureText(gageName)
        val gageUnitWidth = unitPaint.measureText(gageUnit)

        val gageRectYPos =
            borderPaint.strokeWidth.coerceAtLeast(gageZeroLineYPos - (gageZeroLineYPos / maxValue) * gageValue)

        val textXStart = when (barVisibility) {
            true -> gageWidth + xTextMargin
            else -> 0f
        }

        if (barVisibility) {
            gageBarRect.left = borderPaint.strokeWidth
            gageBarRect.top = gageRectYPos
            gageBarRect.right = gageWidth - borderPaint.strokeWidth/2
            gageBarRect.bottom = gageZeroLineYPos

            gageBorder.moveTo(borderPaint.strokeWidth/2, borderPaint.strokeWidth/2)
            gageBorder.lineTo(borderPaint.strokeWidth/2, viewHeight - borderPaint.strokeWidth/2)
            gageBorder.lineTo(gageWidth, viewHeight - borderPaint.strokeWidth/2)
            gageBorder.lineTo(gageWidth, borderPaint.strokeWidth/2)
            gageBorder.close()
            // gageBorder.lineTo(borderPaint.strokeWidth/2, borderPaint.strokeWidth/2)

            if (minValue >= 0) gageZeroLineYPos += borderPaint.strokeWidth/2
            gageZeroLine.reset() // Reset the path to not draw zero line multiple times
            gageZeroLine.moveTo(0f, gageZeroLineYPos)
            gageZeroLine.lineTo(gageWidth + borderPaint.strokeWidth/2, gageZeroLineYPos)


            canvas.drawRect(
                borderPaint.strokeWidth,
                borderPaint.strokeWidth,
                gageWidth - borderPaint.strokeWidth/2,
                viewHeight-borderPaint.strokeWidth,
                backgroundPaint)
            canvas.drawRect(gageBarRect, gagePaint) // actual gage
            canvas.drawPath(gageBorder, borderPaint) // gage border
            canvas.drawPath(gageZeroLine, zeroLinePaint) // zero Line

            gageBorder.reset()
            gageZeroLine.reset()
        }

        canvas.drawText(gageName, textXStart, nameYPos, namePaint)
        canvas.drawText(gageUnit, textXStart + xTextMargin * 0.5f + gageNameWidth, nameYPos, unitPaint)
        canvas.drawText(gageValue(), textXStart, valueYPos, valuePaint)

        //}
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = viewHeight.toInt()
        // val width = viewWidth.toInt()
        setMeasuredDimension(widthMeasureSpec, height)
    }

    private fun gageValue() : String {
        if (gageValueInt != null){
            //if (gageValueInt!! > 2 * maxValue)
            //    return String.format(">%d", (2 * maxValue).toInt())
            //if (gageValueInt!! < 2 * minValue && minValue < 0)
            //    return  String.format("<%d", (2 * minValue).toInt())
            return String.format("%d", gageValueInt)
        }
        if (gageValueFloat != null){
            //if (gageValueFloat!! > 2 * maxValue)
            //    return ">%.1f".format(Locale.ENGLISH, (2 * maxValue))
            //if (gageValueFloat!! < 2 * minValue && minValue < 0)
            //    return  "<%.1f".format(Locale.ENGLISH, (2 * minValue))
            return "%.1f".format(Locale.ENGLISH, gageValueFloat)
        } // String.format("%.1f", gageValueFloat)
        return "-/-"
    }

    private fun dpToPx(dpSize: Float) : Float {
        return (dpSize * resources.displayMetrics.density)
    }

    private fun getPrimaryColor(): Int {
        if (primaryColor == null) {
            val typedValue = TypedValue()
            context.theme.resolveAttribute(android.R.attr.colorControlActivated, typedValue, true)
            return typedValue.data
        }
        return primaryColor!!
    }
}