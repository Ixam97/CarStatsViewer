package com.ixam97.carStatsViewer.enums

import kotlin.math.roundToLong

enum class DistanceUnit {
    KM, MILES;

    fun toFactor(): Float {
        return when (this) {
            KM -> 1.0f
            else -> 1f / asFactor()
        }
    }

    fun asFactor(): Float {
        return when (this) {
            KM -> 1.0f
            else -> 1.60934f
        }
    }

    fun toUnit(value: Float): Float {
        return when (this) {
            KM -> value
            else -> value * this.toFactor()
        }
    }

    fun asUnit(value: Float): Float {
        return when (this) {
            KM -> value
            else -> value / this.toFactor()
        }
    }

    fun toUnit(value: Double): Double {
        return when (this) {
            KM -> value
            else -> value * this.toFactor()
        }
    }

    fun asUnit(value: Double): Double {
        return when (this) {
            KM -> value
            else -> value / this.toFactor()
        }
    }

    fun toUnit(value: Long): Long {
        return when (this) {
            KM -> value
            else -> (value * this.toFactor()).roundToLong()
        }
    }

    fun asUnit(value: Long): Long {
        return when (this) {
            KM -> value
            else -> (value / this.toFactor()).roundToLong()
        }
    }

    fun unit(): String {
        return when (this) {
            KM -> "km"
            else -> "mi"
        }
    }
}