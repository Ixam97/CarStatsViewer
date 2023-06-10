package com.ixam97.carStatsViewer.utils

class TimestampSynchronizer {
    /**
     * System time in millis and property timestamp in nanos associated with the same time.
     */
    private var systemTime: Long = 0
    private var propertyTimestamp: Long = 0

    fun sync(referenceSystemTime: Long, referenceTimestamp: Long) {
        systemTime = referenceSystemTime
        propertyTimestamp = referenceTimestamp
    }

    fun getSystemTimeFromNanosTimestamp(timestamp: Long): Long {
        if (!isSynced()) throw Exception("TimestampSynchronizer has not been synced.")
        val nanosDelta = (timestamp - propertyTimestamp) / 1_000_000
        return systemTime + nanosDelta
    }

    fun isSynced() = systemTime > 0 && propertyTimestamp > 0
}