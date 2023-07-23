package com.ixam97.carStatsViewer.utils

import android.text.format.DateFormat
import com.ixam97.carStatsViewer.CarStatsViewer
import java.util.*
import java.util.concurrent.TimeUnit

object StringFormatters {

    private val dateFormat = DateFormat.getDateFormat(CarStatsViewer.appContext)
    private val timeFormat = DateFormat.getTimeFormat(CarStatsViewer.appContext)
    private val appPreferences = CarStatsViewer.appPreferences

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
        return "${dateFormat.format(date)}, ${timeFormat.format(date)}"
    }

    fun getDateString(tripStartDate: Calendar): String {
        return "${dateFormat.format(tripStartDate.time)}, ${timeFormat.format(tripStartDate.time)}"
    }

    fun getEnergyString(usedEnergy: Float): String {
        if (!appPreferences.consumptionUnit) {
            return "%.1f kWh".format(
                Locale.ENGLISH,
                kiloRounder(usedEnergy))
        }
        return "${usedEnergy.toInt()} Wh"
    }

    fun getTraveledDistanceString(traveledDistance: Float): String {
        return "%.1f %s".format(Locale.ENGLISH, kiloRounder(appPreferences.distanceUnit.toUnit(traveledDistance)), appPreferences.distanceUnit.unit())
    }

    fun getAvgSpeedString(traveledDistance: Float, timeDriven: Long): String {
        val speedString = if (timeDriven < 1) "-/-" else "%.0f".format((appPreferences.distanceUnit.toUnit(traveledDistance) / 1000f) / (timeDriven.toFloat() / (1000 * 60 * 60)))
        return "Ø %s %s".format(Locale.ENGLISH, speedString, appPreferences.distanceUnit.unit() + "/h")
    }

    fun getRemainingRangeString(remainingRange: Float): String {
        return "%d %s".format(
            Locale.ENGLISH,
            (((kiloRounder(appPreferences.distanceUnit.toUnit(remainingRange)).toInt()/10)*10)),
            appPreferences.distanceUnit.unit()
        )
    }

    fun getAvgConsumptionString(usedEnergy: Float, traveledDistance: Float): String {
        val avgConsumption = appPreferences.distanceUnit.asUnit(usedEnergy / (traveledDistance / 1000))
        val unitString = when {
            appPreferences.consumptionUnit -> "Wh/%s".format(appPreferences.distanceUnit.unit())
            else -> "kWh/100%s".format(appPreferences.distanceUnit.unit())
        }

        if (traveledDistance <= 0) {
            return "-/- $unitString"
        }
        if (!appPreferences.consumptionUnit) {
            return "Ø %.1f %s".format(
                Locale.ENGLISH,
                (avgConsumption) / 10,
                unitString)
        }
        return "Ø ${(avgConsumption).toInt()} $unitString"
    }

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
        val unitAltUp = appPreferences.distanceUnit.asSubUnit(altUp).toInt()
        val unitAltDown = appPreferences.distanceUnit.asSubUnit(altDown).toInt()
        val unitString = appPreferences.distanceUnit.subUnit()
        val formattedAltUp = "%d %s".format(unitAltUp, unitString)
        val formattedAltDown = "%d %s".format(unitAltDown, unitString)
        val formattedDelta = "%d %s".format(unitAltUp - unitAltDown, unitString)

        return "↑$formattedAltUp, ↓$formattedAltDown, Δ$formattedDelta"
    }
}