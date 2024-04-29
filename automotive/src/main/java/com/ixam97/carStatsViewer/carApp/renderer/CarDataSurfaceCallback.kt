package com.ixam97.carStatsViewer.carApp.renderer

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.view.Surface
import androidx.car.app.CarContext
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.utils.InAppLogger

class CarDataSurfaceCallback(val carContext: CarContext): SurfaceCallback {

    private val TAG = "CarDataSurfaceCallback"

    private var visibleArea: Rect? = null
    private var stableArea: Rect? = null
    private var surface: Surface? = null

    private var rendererEnabled = false

    private val defaultRenderer = DefaultRenderer(carContext)


    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        InAppLogger.d("[$TAG] Surface available")
        super.onSurfaceAvailable(surfaceContainer)
        surface = surfaceContainer.surface
        renderFrame()
    }

    override fun onVisibleAreaChanged(visibleArea: Rect) {
        InAppLogger.i("[$TAG] Visible area changed " + surface + ". stableArea: "
                + stableArea + " visibleArea:" + visibleArea)
        this.visibleArea = visibleArea
        renderFrame()
    }

    override fun onStableAreaChanged(stableArea: Rect) {
        InAppLogger.i("[$TAG] Stable area changed " + surface + ". stableArea: "
                + stableArea + " visibleArea:" + visibleArea)
        super.onStableAreaChanged(stableArea)
        this.stableArea = stableArea
        renderFrame()
    }

    fun pause() {
        renderFrame(clearFrame = true)
        rendererEnabled = false
    }

    fun resume() {
        rendererEnabled = true
        invalidatePlot()
    }

    fun renderFrame(clearFrame: Boolean = false) {
        if (!rendererEnabled) return

        InAppLogger.d("[$TAG] Rendering Frame")
        defaultRenderer.setData(CarStatsViewer.dataProcessor.realTimeData)

        surface?.apply {
            if(!isValid) return
            val canvas: Canvas = try {
                lockCanvas(null)
            } catch (e: Exception) {
                InAppLogger.w("[$TAG] Failed to get canvas:\n${e.printStackTrace()}")
                return
            }

            if (clearFrame) {
                canvas.drawColor(Color.BLACK)
                unlockCanvasAndPost(canvas)
                return
            }

            canvas.drawColor(carContext.getColor(R.color.slideup_activity_background))
            // canvas.drawColor(if (carContext.isDarkMode) Color.BLACK else Color.WHITE)

            defaultRenderer.renderFrame(canvas, visibleArea, stableArea)
            unlockCanvasAndPost(canvas)
        }

    }

    fun updateSession() {
        if (!rendererEnabled) return
        if (defaultRenderer !is DefaultRenderer) return

        defaultRenderer.updateSession()
        renderFrame()
    }

    fun invalidatePlot() {
        if (!rendererEnabled) return
        defaultRenderer.refreshConsumptionPlot()
        renderFrame()
    }
}