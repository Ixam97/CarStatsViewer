package com.ixam97.carStatsViewer.utils

import kotlinx.coroutines.flow.*

object Ticker {
    fun tickerFlow(interval: Long) = flow {
        var startTime = System.currentTimeMillis()
        // var loops = 1L

        while (true) {
            while (true) {
                val currentTime = System.currentTimeMillis()
                if (currentTime >= (startTime + interval)) {
                    startTime = currentTime
                    break
                }
            }
            // loops ++
            emit(Unit)
        }
    }
}