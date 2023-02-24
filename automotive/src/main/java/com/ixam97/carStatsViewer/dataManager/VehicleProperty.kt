package com.ixam97.carStatsViewer.dataManager

class VehicleProperty(val printableName: String, val propertyId: Int) {
    internal val startupTimestamp = System.nanoTime()

    /** Value of the Vehicle */
    var value: Any? = null
        internal set(value) {
            lastValue = field
            field = value
        }

    var lastValue: Any? = null
        internal set

    /** Timestamp of the last time the value was changed in nanoseconds */
    var timestamp: Long = startupTimestamp
        internal set(value) {
            lastTimestamp = field
            field = value
        }

    internal var lastTimestamp: Long = startupTimestamp

    /** Time difference between value changes in nanoseconds */
    val timeDelta: Long get() {
        if (lastTimestamp == 0L || lastTimestamp < startupTimestamp) return 0L
        return timestamp - lastTimestamp
    }

    /** Returns true if the value difference is null, therefore the value is the initial value */
    val isInitialValue: Boolean get() = (valueDelta == null)

    /** Value difference since last value change */
    val valueDelta: Any?
        get() {
            value.let {
                if (it == null || lastValue == null) return null
                if (it is Float) return (it - lastValue as Float)
                if (it is Int) return (it - lastValue as Int)
                if (it is Boolean) return !(lastValue as Boolean)
            }
            return null
        }
}