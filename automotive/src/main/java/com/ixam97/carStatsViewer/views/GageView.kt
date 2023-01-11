package com.ixam97.carStatsViewer.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.ixam97.carStatsViewer.objects.AppPreferences
import java.text.DecimalFormat
import java.util.*
import kotlin.math.min
import kotlin.math.roundToInt


class GageView(context: Context, attrs: AttributeSet) : View(context, attrs) {

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

    private var gageValueInt : Int? = null
    private var gageValueFloat: Float? = null

    private var gageValueIntArray = arrayListOf<Int>()
    private var gageValueFloatArray = arrayListOf<Float>()

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
        textSize = dpToPx(30f)
        isAntiAlias = true
    }

    private val unitPaint = Paint().apply {
        color = getPrimaryColor()
        textSize = dpToPx(30f)
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
    private val zeroLinePaint = Paint().apply {
        color = Color.GRAY
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val valuePaint = Paint().apply {
        color = Color.WHITE
        textSize = dpToPx(100f)
        isAntiAlias = true
    }

    private val xTextMargin = dpToPx(15f)
    private val yTextMargin = dpToPx(10f)
    private val gageWidth = if(AppPreferences.gagesOn){
        dpToPx(60f)} else { dpToPx(0f)}

    private val nameYPos = namePaint.textSize * 0.76f
    private val valueYPos = nameYPos + valuePaint.textSize * 0.9f
    private val unitYPos = valueYPos - (valuePaint.textSize * 0.8f - unitPaint.textSize)

    private val viewHeight = valueYPos + dpToPx(3f)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        var gageValue = 0f
        var gagePaint = posPaint

        if (gageValueInt != null) gageValue = gageValueInt!!.toFloat()
        if (gageValueFloat != null) gageValue = gageValueFloat!!

        if (gageValue < 0) gagePaint = negPaint

        val gageBorder = Path()
        val gageZeroLine = Path()

        var gageZeroLineYPos = ((viewHeight - borderPaint.strokeWidth)/(maxValue - minValue)) * maxValue

        if (gageValue < minValue) gageValue = minValue
        if (gageValue > maxValue) gageValue = maxValue

        val gageValueWidth = valuePaint.measureText(gageValue())
        val gageNameWidth = namePaint.measureText(gageName)

        val gageRectYPos = Math.max(
            borderPaint.strokeWidth,
            gageZeroLineYPos - (gageZeroLineYPos/maxValue) * gageValue)

        canvas.drawRect(
            borderPaint.strokeWidth,
            gageRectYPos,
            gageWidth - borderPaint.strokeWidth/2,
            gageZeroLineYPos,
            gagePaint)

        gageBorder.moveTo(borderPaint.strokeWidth/2, borderPaint.strokeWidth/2)
        gageBorder.lineTo(borderPaint.strokeWidth/2, viewHeight - borderPaint.strokeWidth/2)
        gageBorder.lineTo(gageWidth, viewHeight - borderPaint.strokeWidth/2)
        gageBorder.lineTo(gageWidth, borderPaint.strokeWidth/2)
        gageBorder.lineTo(borderPaint.strokeWidth/2, borderPaint.strokeWidth/2)


        // canvas.drawRect(0f, 0f, gageWidth, valueYPos, posPaint)
        canvas.drawPath(gageBorder, borderPaint)
        canvas.drawText(gageName, gageWidth + xTextMargin, nameYPos, namePaint)
        canvas.drawText(gageUnit, gageWidth + xTextMargin * 1.5f + gageNameWidth, nameYPos, unitPaint)
        canvas.drawText(gageValue(), gageWidth + xTextMargin, valueYPos, valuePaint)

        if (minValue >= 0) gageZeroLineYPos += borderPaint.strokeWidth/2
        gageZeroLine.moveTo(0f, gageZeroLineYPos)
        gageZeroLine.lineTo(gageWidth + borderPaint.strokeWidth/2, gageZeroLineYPos)
        canvas.drawPath(gageZeroLine, zeroLinePaint)
        //}
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = viewHeight.toInt()
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
        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorControlActivated, typedValue, true)
        return typedValue.data
    }
}