package com.ixam97.carStatsViewer.carApp.renderer

import android.app.Presentation
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.util.Log
import android.view.Surface
import androidx.car.app.CarContext
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.ui.views.PlotView
import com.ixam97.carStatsViewer.utils.InAppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext

class CarDataSurfaceCallback(val carContext: CarContext): SurfaceCallback {

    private val TAG = "CarDataSurfaceCallback"

    private var visibleArea: Rect? = null
    private var stableArea: Rect? = null
    private var surface: Surface? = null

    private lateinit var virtualDisplay: VirtualDisplay
    private lateinit var presentation: Presentation

    @OptIn(DelicateCoroutinesApi::class)
    private val rendererContext = newSingleThreadContext("rendererContext")

    private var rendererEnabled = false

    val defaultRenderer = DefaultRenderer(carContext)

    private var canvasSize = Rect(0,0,0,0)


    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        InAppLogger.d("[$TAG] Surface available")
        synchronized(this){
            surface = surfaceContainer.surface
            renderFrame()
        }
        /*
        virtualDisplay = carContext
            .getSystemService(DisplayManager::class.java)
            .createVirtualDisplay(
                "Dashboard_Display",
                surfaceContainer.width,
                surfaceContainer.height,
                surfaceContainer.dpi,
                surfaceContainer.surface,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY
            )

        presentation = Presentation(carContext, virtualDisplay.display)

        presentation.setContentView(PlotView(carContext))
        presentation.show()
        */
    }

    override fun onVisibleAreaChanged(visibleArea: Rect) {
        synchronized(this) {
            InAppLogger.i(
                "[$TAG] Visible area changed " + surface + ". stableArea: "
                        + stableArea + " visibleArea:" + visibleArea
            )
            this.visibleArea = visibleArea
            renderFrame()
        }
    }

    override fun onStableAreaChanged(stableArea: Rect) {
        synchronized(this) {
            InAppLogger.i(
                "[$TAG] Stable area changed " + surface + ". stableArea: "
                        + stableArea + " visibleArea:" + visibleArea
            )
            super.onStableAreaChanged(stableArea)
            this.stableArea = stableArea
            renderFrame()
        }
    }

    fun pause() {
        if (!rendererEnabled) return
        synchronized(this) {
            renderFrame(clearFrame = true)
        }
        rendererEnabled = false
        InAppLogger.i("[$TAG] Renderer paused")
    }

    fun resume() {
        if (rendererEnabled) return
        rendererEnabled = true
        invalidatePlot()
        InAppLogger.i("[$TAG] Renderer enabled")
    }

    fun isEnabled() = rendererEnabled

    fun requestRenderFrame() {
        synchronized(this) {
            renderFrame()
        }
    }

    private fun renderFrame(clearFrame: Boolean = false) {

        if (!rendererEnabled) return
        surface?.let { surface ->
            if (!surface.isValid) return

            CoroutineScope(rendererContext).launch {

                defaultRenderer.setData(CarStatsViewer.dataProcessor.realTimeData)
                val thread = Thread.currentThread().name
                InAppLogger.d("[$TAG] Thread: $thread")
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
                        withContext(Dispatchers.Main) {
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
                    }
                } else {
                    withContext(Dispatchers.Main) {
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
    }

    fun updateSession() {
        synchronized(this) {
            if (!rendererEnabled) return
            if (defaultRenderer !is DefaultRenderer) return

            defaultRenderer.updateSession()
            renderFrame()
        }
    }

    fun invalidatePlot() {
        synchronized(this) {
            if (!rendererEnabled) return
            defaultRenderer.refreshConsumptionPlot()
            renderFrame()
        }
    }

    fun toggleDebugFlag() {
        defaultRenderer.debugFlag = !defaultRenderer.debugFlag
    }

    fun getDebugFlag() = defaultRenderer.debugFlag
}