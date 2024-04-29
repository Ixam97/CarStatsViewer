package com.ixam97.carStatsViewer.carApp.renderer

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.util.Log
import android.view.Surface
import androidx.car.app.CarContext
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.dataProcessor.RealTimeData
import com.ixam97.carStatsViewer.utils.DataConverters
import com.ixam97.carStatsViewer.utils.InAppLogger
import kotlinx.android.synthetic.main.activity_main.main_consumption_plot

class CarDataSurfaceCallback(val carContext: CarContext): SurfaceCallback {

    private val TAG = "CarDataSurfaceCallback"

    private var visibleArea: Rect? = null
    private var stableArea: Rect? = null
    private var surface: Surface? = null

    private var rendererEnabled = false

    private val defaultRenderer = DefaultRenderer(carContext)


    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        Log.d(TAG, "Surface available")
        super.onSurfaceAvailable(surfaceContainer)
        surface = surfaceContainer.surface
        renderFrame()
    }

    override fun onVisibleAreaChanged(visibleArea: Rect) {
        Log.i(TAG, "Visible area changed " + surface + ". stableArea: "
                + stableArea + " visibleArea:" + visibleArea)
        this.visibleArea = visibleArea
        renderFrame()
    }

    override fun onStableAreaChanged(stableArea: Rect) {
        Log.i(TAG, "Stable area changed " + surface + ". stableArea: "
                + stableArea + " visibleArea:" + visibleArea)
        super.onStableAreaChanged(stableArea)
        this.stableArea = stableArea
        renderFrame()
    }

    fun pause() {
        rendererEnabled = false
    }

    fun resume() {
        rendererEnabled = true
    }

    fun renderFrame() {
        if (!rendererEnabled) return

        Log.d(TAG, "Rendering Frame")
        defaultRenderer.setData(CarStatsViewer.dataProcessor.realTimeData)

        surface?.apply {
            if(!isValid) return
            val canvas: Canvas = lockCanvas(null)
            canvas.drawColor(carContext.getColor(R.color.slideup_activity_background))
            // canvas.drawColor(if (carContext.isDarkMode) Color.BLACK else Color.WHITE)

            defaultRenderer.renderFrame(canvas, visibleArea, stableArea)
            unlockCanvasAndPost(canvas)
        }

    }

    fun updateSession() {

        if (defaultRenderer !is DefaultRenderer) return

        defaultRenderer.updateSession()
        renderFrame()
    }
}