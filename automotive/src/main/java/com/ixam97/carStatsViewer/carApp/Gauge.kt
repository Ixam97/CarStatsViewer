package com.ixam97.carStatsViewer.carApp

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.car.app.CarContext
import com.ixam97.carStatsViewer.CarStatsViewer
import kotlin.io.path.Path

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


    fun draw(size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.GREEN)

        canvas.drawText("Test", 0f, size.toFloat(), valuePaint)

        return bitmap
    }
}