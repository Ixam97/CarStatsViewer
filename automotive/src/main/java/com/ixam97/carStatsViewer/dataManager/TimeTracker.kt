package com.ixam97.carStatsViewer.dataManager

import java.util.*

/**
 * The TimeTracker is able to track a time span based on the Date-object.
 */
class TimeTracker(val printableName: String = "", val doLog: Boolean = false) {
    private var startDate: Date? = null
    private var timeSpan: Long = 0

    /** Start or resume time tracking */
    fun start() {
        if (startDate == null) startDate = Date()
    }

    /** Stop time tracking */
    fun stop() {
        startDate.let {
            if (it != null) timeSpan += (Date().time - it.time)
        }
        startDate = null
    }

    /** Reset tracked time to zero */
    fun reset() {
        startDate = null
        timeSpan = 0L
    }

    /** Restore tracked time to defined base value */
    fun restore(pTimeSpan: Long) {
        timeSpan = pTimeSpan
    }

    /** returns the current tracked time in milliseconds */
    fun getTime(): Long {
        return timeSpan + (Date().time - (startDate?.time?: Date().time))
    }
}