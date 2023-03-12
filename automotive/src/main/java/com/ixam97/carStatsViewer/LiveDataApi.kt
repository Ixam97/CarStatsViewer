package com.ixam97.carStatsViewer

import android.content.Context
import android.content.Intent
import android.os.Handler
import com.ixam97.carStatsViewer.dataManager.DataManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class LiveDataApi(
    val broadcastAction: String
    ){

    /**
     * Indicates the current connection status of the API
     *      0: Unused
     *      1: Connected
     *      2: Error
     */
    var connectionStatus: Int = 0

    /**
     * Dialog to setup API.
     */
    abstract fun showSettingsDialog(context: Context)

    /**
     * creates a runnable to be executed in intervals. Returns null if API does not send data in
     * timed intervals.
     */
    open fun createLiveDataTask(
        dataManager: DataManager,
        handler: Handler,
        interval: Long
    ): Runnable? {
        return object : Runnable {
            override fun run() {
                coroutineSendNow(dataManager)
                handler.postDelayed(this, interval)
            }
        }
    }

    /**
     * sendNow, but wrapped in a coroutine to not block main thread.
     */
    fun coroutineSendNow(dataManager: DataManager) {
        CoroutineScope(Dispatchers.Default).launch {
            sendNow(dataManager)
            sendStatusBroadcast(CarStatsViewer.appContext)
        }
    }

    /**
     * Code to be executed in coroutineSendNow. This function should not be called outside a
     * coroutine to not block main thread.
     */
    protected abstract fun sendNow(dataManager: DataManager)

    private fun sendStatusBroadcast(context: Context) {
        val broadcastIntent = Intent(broadcastAction)
        broadcastIntent.putExtra("status", connectionStatus)
        context.sendBroadcast(broadcastIntent)
    }
}