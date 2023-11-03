package com.coderax.carStatsViewer.utils

import kotlin.math.roundToLong

enum class DistanceUnitEnum {
    KM, MILES;

    fun toFactor(): Float {
        return when (this) {
            KM -> 1.0f
            else -> 1f / asFactor()
        }
    }

    fun toSubFactor(): Float {
        return when (this) {
            KM -> 1.0f
            else -> 1f / asSubFactor()
        }
    }

    fun asFactor(): Float {
        return when (this) {
            KM -> 1.0f
            else -> 1.60934f
        }
    }

    fun asSubFactor(): Float {
        return when (this) {
            KM -> 1.0f
            else -> 0.3048f
        }
    }

    fun toUnit(value: Float): Float {
        return when (this) {
            KM -> value
            else -> value * this.toFactor()
        }
    }

    fun toSubUnit(value: Float): Float {
        return when (this) {
            KM -> value
            else -> value * this.toSubFactor()
        }
    }

    fun asUnit(value: Float): Float {
        return when (this) {
            KM -> value
            else -> value / this.toFactor()
        }
    }

    fun asSubUnit(value: Float): Float {
        return when (this) {
            KM -> value
            else -> value / this.toSubFactor()
        }
    }

    fun toUnit(value: Double): Double {
        return when (this) {
            KM -> value
            else -> value * this.toFactor()
        }
    }

    fun toSubUnit(value: Double): Double {
        return when (this) {
            KM -> value
            else -> value * this.toSubFactor()
        }
    }

    fun asUnit(value: Double): Double {
        return when (this) {
            KM -> value
            else -> value / this.toFactor()
        }
    }

    fun asSubUnit(value: Double): Double {
        return when (this) {
            KM -> value
            else -> value / this.toSubFactor()
        }
    }

    fun toUnit(value: Long): Long {
        return when (this) {
            KM -> value
            else -> (value * this.toFactor()).roundToLong()
        }
    }

    fun toSubUnit(value: Long): Long {
        return when (this) {
            KM -> value
            else -> (value * this.toSubFactor()).roundToLong()
        }
    }

    fun asUnit(value: Long): Long {
        return when (this) {
            KM -> value
            else -> (value / this.toFactor()).roundToLong()
        }
    }

    fun asSubUnit(value: Long): Long {
        return when (this) {
            KM -> value
            else -> (value / this.toSubFactor()).roundToLong()
        }
    }

    fun unit(): String {
        return when (this) {
            KM -> "km"
            else -> "mi"
        }
    }

    fun subUnit(): String {
        return when (this) {
            KM -> "m"
            else -> "ft"
        }
    }
}