package com.ixam97.carStatsViewer.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import kotlin.math.roundToInt


class GageView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    var gageName : String = "gageName"
        set(value) {
            field = value
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

    fun setValue(value: Int) {
        gageValueInt = value
        gageValueFloat = null
        this.invalidate()
    }


    fun setValue(value: Float) {
        gageValueFloat = value
        this.invalidate()
    }

    private val posPaint = Paint().apply {
        var typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorControlActivated, typedValue, true)
        color = typedValue.data
    }

    private val negPaint = Paint().apply {
        color = Color.GREEN
    }

    private val namePaint = Paint().apply {
        color = Color.WHITE
        textSize = dpToPx(35f)
        isAntiAlias = true
    }

    private val unitPaint = Paint().apply {
        color = Color.DKGRAY
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
        textSize = dpToPx(110f)
        isAntiAlias = true
    }

    private val xTextMargin = dpToPx(15f)
    private val yTextMargin = dpToPx(10f)
    private val gageWidth = dpToPx(100f)

    private val nameYPos = namePaint.textSize * 0.76f
    private val unitYPos = nameYPos + yTextMargin + unitPaint.textSize
    private val valueYPos = unitYPos + valuePaint.textSize * 0.85f

    private val viewHeight = valueYPos + dpToPx(20f)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        var gageValue = 0f
        var gagePaint = posPaint

        if (gageValueInt != null) gageValue = gageValueInt!!.toFloat()
        if (gageValueFloat != null) gageValue = gageValueFloat!!

        if (gageValue < 0) gagePaint = negPaint

        val gageBorder = Path()
        val gageZeroLine = Path()

        val gageZeroLineYPos = ((valueYPos - borderPaint.strokeWidth/2)/(maxValue - minValue)) * maxValue

        if (gageValue < minValue) gageValue = minValue
        if (gageValue > maxValue) gageValue = maxValue

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
        gageBorder.lineTo(borderPaint.strokeWidth/2, valueYPos)
        gageBorder.lineTo(gageWidth, valueYPos)
        gageBorder.lineTo(gageWidth, borderPaint.strokeWidth/2)
        gageBorder.lineTo(borderPaint.strokeWidth/2, borderPaint.strokeWidth/2)


        // canvas.drawRect(0f, 0f, gageWidth, valueYPos, posPaint)
        canvas.drawPath(gageBorder, borderPaint)
        canvas.drawText(gageName, gageWidth + xTextMargin, nameYPos, namePaint)
        canvas.drawText(gageUnit, gageWidth + xTextMargin, unitYPos, unitPaint)
        canvas.drawText(gageValue(), gageWidth + xTextMargin, valueYPos, valuePaint)

        if (minValue < 0) {
            gageZeroLine.moveTo(0f, gageZeroLineYPos)
            gageZeroLine.lineTo(gageWidth + borderPaint.strokeWidth/2, gageZeroLineYPos)
            canvas.drawPath(gageZeroLine, zeroLinePaint)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = viewHeight.toInt()
        setMeasuredDimension(widthMeasureSpec, height)
    }

    private fun gageValue() : String {
        if (gageValueInt != null) return String.format("%d", gageValueInt)
        if (gageValueFloat != null) return String.format("%.1f", gageValueFloat)
        return "N/A"
    }

    private fun dpToPx(dpSize: Float) : Float {
        return (dpSize * resources.displayMetrics.density)
    }
}