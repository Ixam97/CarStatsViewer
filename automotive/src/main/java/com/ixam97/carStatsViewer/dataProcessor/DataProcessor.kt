package com.ixam97.carStatsViewer.dataProcessor

import com.ixam97.carStatsViewer.utils.InAppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class DataProcessor {
    private val _realTimeDataFlow = MutableStateFlow<Float>(0f)
    val realTimeDataFlow = _realTimeDataFlow.asStateFlow()

    fun updateRealTimeData(value: Float) {
        _realTimeDataFlow.value = value
    }
}