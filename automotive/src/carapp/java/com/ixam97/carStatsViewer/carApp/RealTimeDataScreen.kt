package com.ixam97.carStatsViewer.carApp

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.utils.throttle
import kotlinx.coroutines.launch

class RealTimeDataScreen(
    carContext: CarContext,
    val session: CarStatsViewerSession
): Screen(carContext), DefaultLifecycleObserver {

    private val realTimeDataTemplate = RealTimeDataTemplate(
        carContext,
        session,
        backButton = true,
        navigationOnly = false
    ) {
        invalidate()
    }

    init {
        lifecycle.addObserver(this)
        lifecycleScope.launch {
            CarStatsViewer.dataProcessor.realTimeDataFlow.throttle(500).collect {
                if (session.carDataSurfaceCallback.isEnabled()) session.carDataSurfaceCallback.requestRenderFrame()
            }
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        session.carDataSurfaceCallback.resume()
    }

    override fun onGetTemplate(): Template {
        return realTimeDataTemplate.getTemplate()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        session.carDataSurfaceCallback.pause()
    }
}