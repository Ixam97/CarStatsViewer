package com.ixam97.carStatsViewer.carApp.renderer

import android.graphics.Bitmap
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

    private var canvasSize = Rect(0,0,0,0)


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
        val thread = Thread.currentThread().name
        if (thread != "main") {
            InAppLogger.w("[$TAG] Rendering not in main thread. Aborting")
            return
        }
        // InAppLogger.d("[$TAG] Thread: ${Thread.currentThread().name}")
        if (!rendererEnabled) return

        // InAppLogger.v("[$TAG] Rendering Frame")
        defaultRenderer.setData(CarStatsViewer.dataProcessor.realTimeData)

        surface?.let { surface ->

            if (!surface.isValid) return
            //var offScreenBitmap: Bitmap? = null
            if (canvasSize.width() > 0 && canvasSize.height() > 0) {
                visibleArea?.let { visibleArea ->
                    val offScreenBitmap = Bitmap.createBitmap(
                        canvasSize.width(),
                        canvasSize.height(),
                        Bitmap.Config.ARGB_8888
                    )
                    val offScreenCanvas = Canvas(offScreenBitmap)

                    if (clearFrame) {
                        offScreenCanvas.drawColor(Color.BLACK)
                    } else {
                        offScreenCanvas.drawColor(carContext.getColor(R.color.slideup_activity_background))
                        defaultRenderer.renderFrame(offScreenCanvas, visibleArea, visibleArea)
                    }

                    try {
                        surface.lockCanvas(null)?.apply {
                            InAppLogger.d("[$TAG] Applying Bitmap to canvas.")
                            canvasSize = Rect(0, 0, width, height)
                            drawBitmap(offScreenBitmap, 0f, 0f, null)
                            surface.unlockCanvasAndPost(this)
                        } ?: InAppLogger.e("[$TAG] Could not lock canvas!")
                    } catch (e: Exception) {
                        InAppLogger.e("[$TAG] Could not draw canvas!")
                    }

                }
            } else {
                try {
                    surface.lockCanvas(null)?.apply {
                        canvasSize = Rect(0, 0, width, height)
                        surface.unlockCanvasAndPost(this)
                    }?:InAppLogger.e("[$TAG] Could not lock canvas!")
                } catch (e: Exception) {
                    InAppLogger.e("[$TAG] Could not draw canvas!")
                }
            }
        }
/*
        surface?.let {

            var offScreenBitmap: Bitmap? = null
            if (canvasSize.width() > 0 && canvasSize.height() > 0) {
                if (!it.isValid) return
                visibleArea?.let {
                    offScreenBitmap = Bitmap.createBitmap(
                        canvasSize.width(),
                        canvasSize.height(),
                        Bitmap.Config.ARGB_8888
                    )
                }
                if (offScreenBitmap == null) return
                var offScreenCanvas = Canvas(offScreenBitmap!!)

                if (clearFrame) {
                    offScreenCanvas.drawColor(Color.BLACK)
                } else {
                    offScreenCanvas.drawColor(carContext.getColor(R.color.slideup_activity_background))
                    defaultRenderer.renderFrame(offScreenCanvas, visibleArea, visibleArea)
                }
            }
            var canvas: Canvas? = null
            try {
                canvas = it.lockCanvas(null)
                canvasSize = Rect(0, 0, canvas?.width?:0, canvas?.height?:0)
                offScreenBitmap?.let { bitmap -> canvas?.drawBitmap(bitmap, 0f, 0f, null)}
            } catch (e: Exception) {
                InAppLogger.w("[$TAG] Failed to draw canvas:\n${e.printStackTrace()}")
            } finally {
                it.unlockCanvasAndPost(canvas)
            }
        }
 */
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

    fun toggleDebugFlag() {
        defaultRenderer.debugFlag = !defaultRenderer.debugFlag
    }

    fun getDebugFlag() = defaultRenderer.debugFlag
}