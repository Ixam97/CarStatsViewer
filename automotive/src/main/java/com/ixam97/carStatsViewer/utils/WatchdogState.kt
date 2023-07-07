package com.ixam97.carStatsViewer.utils

data class WatchdogState(
    val locationState: Int = 0,
    val apiState: Map<String,Int> = mapOf()
) {
    companion object {
        const val DISABLED: Int = 0
        const val NOMINAL: Int = 1
        const val ERROR: Int = 2
        const val LIMITED: Int = 3
    }
}