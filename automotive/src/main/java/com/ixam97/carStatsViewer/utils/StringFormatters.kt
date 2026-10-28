package com.ixam97.carStatsViewer.utils

import android.text.format.DateFormat
import com.ixam97.carStatsViewer.CarStatsViewer
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

object StringFormatters {

    private val dateFormat = DateFormat.getDateFormat(CarStatsViewer.appContext)
    private val timeFormat = DateFormat.getTimeFormat(CarStatsViewer.appContext)
    // private val appPreferences = CarStatsViewer.appPreferences

    fun getGearString(gear: Int): String {
        return when(gear) {
            1 -> "N"
            2 -> "R"
            4 -> "P"
            8 -> "D"
            else -> "UNKNOWN"
        }
    }

    /** Divides a Float by 1000 and rounds it up to one decimal point to be on par with board computer */
    private fun kiloRounder(number: Float): Float {
        return (number.toInt() / 100).toFloat() / 10
    }

    fun getDateString(date: Date?): String {
        if (date == null) return "-/-"
        return "${dateFormat.format(date)} | ${timeFormat.format(date)}"
    }

    fun getDateString(tripStartDate: Calendar): String {
        return "${dateFormat.format(tripStartDate.time)} | ${timeFormat.format(tripStartDate.time)}"
    }

    fun getStartToEndDateString(start: Date, end: Date? = null):String {
        val startDate = dateFormat.format(start.time)
        val startTime = timeFormat.format(start.time)

        if (end == null) return "$startDate | $startTime"

        val endDate = dateFormat.format(end.time)
        val endTime = timeFormat.format(end.time)

        return if (startDate == endDate) {
            "$startDate | $startTime – $endTime"
        } else {
            "$startDate | $startTime – $endDate | $endTime"
        }
    }

    fun getEnergyString(usedEnergy: Float): String {
        if (!CarStatsViewer.appPreferences.consumptionUnit) {
            return "%.1f kWh".format(
                Locale.ENGLISH,
                kiloRounder(usedEnergy))
        }
        return "${usedEnergy.toInt()} Wh"
    }

    fun getTraveledDistanceString(traveledDistance: Float): String {
        return "%.1f %s".format(Locale.ENGLISH, kiloRounder(CarStatsViewer.appPreferences.distanceUnit.toUnit(traveledDistance)), CarStatsViewer.appPreferences.distanceUnit.unit())
    }

    fun getAvgSpeedString(traveledDistance: Float, timeDriven: Long): String {
        val speedString = if (timeDriven < 1) "-/-" else "%.0f".format((CarStatsViewer.appPreferences.distanceUnit.toUnit(traveledDistance) / 1000f) / (timeDriven.toFloat() / (1000 * 60 * 60)))
        return "Ø %s %s".format(Locale.ENGLISH, speedString, CarStatsViewer.appPreferences.distanceUnit.unit() + "/h")
    }

    fun getRemainingRangeString(remainingRange: Float): String {
        return "%d %s".format(
            Locale.ENGLISH,
            (((kiloRounder(CarStatsViewer.appPreferences.distanceUnit.toUnit(remainingRange)).toInt()/10)*10)),
            CarStatsViewer.appPreferences.distanceUnit.unit()
        )
    }

    fun getAvgConsumptionString(usedEnergy: Float, traveledDistance: Float): String {
        val avgConsumption = CarStatsViewer.appPreferences.distanceUnit.asUnit(usedEnergy / (traveledDistance / 1000))
        val unitString = when {
            CarStatsViewer.appPreferences.consumptionUnit -> "Wh/%s".format(CarStatsViewer.appPreferences.distanceUnit.unit())
            else -> "kWh/100%s".format(CarStatsViewer.appPreferences.distanceUnit.unit())
        }

        if (traveledDistance <= 0) {
            return "-/- $unitString"
        }
        if (!CarStatsViewer.appPreferences.consumptionUnit) {
            return "Ø %.1f %s".format(
                Locale.ENGLISH,
                (avgConsumption) / 10,
                unitString)
        }
        return "Ø ${(avgConsumption).toInt()} $unitString"
    }

    /**
     * Formats a single consumption value given in Wh/km in the user's units, without the "Ø" prefix.
     * Whole kWh/100 values are shown without decimals ("15" instead of "15.0").
     */
    fun getConsumptionValueString(consumption: Float, withUnit: Boolean = true): String {
        val value = CarStatsViewer.appPreferences.distanceUnit.asUnit(consumption)
        val valueString = if (CarStatsViewer.appPreferences.consumptionUnit) {
            "${value.roundToInt()}"
        } else {
            val tenths = value.roundToInt()
            if (tenths % 10 == 0) "${tenths / 10}" else "%.1f".format(Locale.ENGLISH, value / 10)
        }
        return if (withUnit) "$valueString ${getConsumptionUnitString()}" else valueString
    }

    fun getConsumptionUnitString(): String = when {
        CarStatsViewer.appPreferences.consumptionUnit -> "Wh/%s".format(CarStatsViewer.appPreferences.distanceUnit.unit())
        else -> "kWh/100%s".format(CarStatsViewer.appPreferences.distanceUnit.unit())
    }

    /** Formats a single speed value given in m/s as km/h or mph. */
    fun getSpeedValueString(speed: Float, withUnit: Boolean = true): String {
        val value = CarStatsViewer.appPreferences.distanceUnit.toUnit(speed * 3.6f).roundToInt()
        return if (withUnit) "$value ${getSpeedUnitString()}" else "$value"
    }

    fun getSpeedUnitString(): String = "${CarStatsViewer.appPreferences.distanceUnit.unit()}/h"

    fun getElapsedTimeString(elapsedTime: Long, minutes: Boolean = false): String {
        if (minutes) return String.format("%02d:%02d",
            TimeUnit.MILLISECONDS.toHours(elapsedTime),
            TimeUnit.MILLISECONDS.toMinutes(elapsedTime) % TimeUnit.HOURS.toMinutes(1))

        return String.format("%02d:%02d:%02d",
            TimeUnit.MILLISECONDS.toHours(elapsedTime),
            TimeUnit.MILLISECONDS.toMinutes(elapsedTime) % TimeUnit.HOURS.toMinutes(1),
            TimeUnit.MILLISECONDS.toSeconds(elapsedTime) % TimeUnit.MINUTES.toSeconds(1))
    }

    fun getTemperatureString(temperature: Float?): String {
        if (temperature == null) return "-/-"
        val unitString = "°C"
        return "%d %s".format(temperature.toInt(), unitString)
    }

    fun getAltitudeString(altUp: Float?, altDown: Float?): String {
        if (altUp == null || altDown == null) return ""
        val unitAltUp = CarStatsViewer.appPreferences.distanceUnit.asSubUnit(altUp).toInt()
        val unitAltDown = CarStatsViewer.appPreferences.distanceUnit.asSubUnit(altDown).toInt()
        val unitString = CarStatsViewer.appPreferences.distanceUnit.subUnit()
        val formattedAltUp = "%d %s".format(unitAltUp, unitString)
        val formattedAltDown = "%d %s".format(unitAltDown, unitString)
        val formattedDelta = "%d %s".format(unitAltUp - unitAltDown, unitString)

        return "↑$formattedAltUp, ↓$formattedAltDown, Δ$formattedDelta"
    }
}