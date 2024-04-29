package com.ixam97.carStatsViewer.carApp.renderer

import android.graphics.Canvas
import android.graphics.Rect


interface Renderer {
    /**
     * Informs the renderer that it will receive [.renderFrame] calls.
     *
     * @param onChangeListener a runnable that will initiate a render pass in the controller
     */
    fun enable(onChangeListener: Runnable)

    /** Informs the renderer that it will no longer receive [.renderFrame] calls.  */
    fun disable()

    /** Request that a frame should be drawn to the supplied canvas.  */
    fun renderFrame(canvas: Canvas, visibleArea: Rect?, stableArea: Rect?)
}
