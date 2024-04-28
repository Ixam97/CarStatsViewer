package com.ixam97.carStatsViewer.liveDataApi

import android.content.Context
import android.os.Handler
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.dataProcessor.RealTimeData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

abstract class LiveDataApi(
    val apiIdentifier: String,
    val apiNameStringId: Int,
    var detailedLog: Boolean
    ){

    /**
     * Indicates the current connection status of the API
     *      0: Unused
     *      1: Connected
     *      2: Error
     *      3: Limited connection
     */
    var connectionStatus: ConnectionStatus = ConnectionStatus.UNUSED
    var timeout: Int = 5_000
    var originalInterval: Int = 5_000

    enum class ConnectionStatus(val status: Int) {
        UNUSED(0),
        CONNECTED(1),
        ERROR(2),
        LIMITED(3);

        companion object {
            fun fromInt(status: Int) = values().first { it.status == status }
        }
    }

    /**
     * Dialog to setup API.
     */
    open fun showSettingsDialog(context: Context) {
        context.setTheme(R.style.AppTheme)
    }

    /**
     * creates a runnable to be executed in intervals. Returns null if API does not send data in
     * timed intervals.
     */
    /*
    open fun createLiveDataTask(
        // dataManager: DataManager,
        realTimeData: RealTimeData,
        handler: Handler,
        interval: Int
    ): Runnable? {
        timeout = interval
        return object : Runnable {
            override fun run() {
                coroutineSendNow(realTimeData)
                handler.postDelayed(this, timeout.toLong())
            }
        }
    }
    */

    /**
     * sendNow, but wrapped in a coroutine to not block main thread.
     */
    suspend fun coroutineSendNow(realTimeData: RealTimeData) {
    //    CoroutineScope(Dispatchers.Default).launch {
            sendNow(realTimeData)
            updateWatchdog()
    //    }
    }

    fun requestFlow(serviceScope: CoroutineScope, realTimeData: () -> RealTimeData, interval: Int): Flow<Unit> {
        timeout = interval
        originalInterval = interval
        return flow {
            while (true) {
                coroutineSendNow(realTimeData())
                delay(timeout.toLong())
            }
        }
    }

    /**
     * Code to be executed in coroutineSendNow. This function should not be called outside a
     * coroutine to not block main thread.
     */
    protected abstract suspend fun sendNow(realTimeData: RealTimeData)

    private fun updateWatchdog() {
        val currentApiStateMap = CarStatsViewer.watchdog.getCurrentWatchdogState().apiState.toMutableMap()
        currentApiStateMap[apiIdentifier] = connectionStatus.status
        CarStatsViewer.watchdog.updateWatchdogState(CarStatsViewer.watchdog.getCurrentWatchdogState().copy(
            apiState = currentApiStateMap.toMap()
        ))
    }
}