package com.ixam97.carStatsViewer.utils

import kotlinx.coroutines.flow.*

object Ticker {
    fun tickerFlow(interval: Long) = flow {
        val startTime = System.currentTimeMillis()
        var loops = 0L

        while (true) {
            while (true) {
                if (System.currentTimeMillis() > (startTime + (loops * interval))) break
            }
            loops ++
            emit(Unit)
        }
    }
}