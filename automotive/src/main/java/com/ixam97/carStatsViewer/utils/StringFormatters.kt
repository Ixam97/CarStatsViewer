package com.ixam97.carStatsViewer.utils

import com.ixam97.carStatsViewer.appPreferences.AppPreferences
import java.util.*
import java.util.concurrent.TimeUnit

object StringFormatters {

    lateinit var appPreferences: AppPreferences
    lateinit var dateFormat: java.text.DateFormat
    lateinit var timeFormat: java.text.DateFormat

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
            return "%.1f %s".format(
                Locale.ENGLISH,
                (avgConsumption) / 10,
                unitString)
        }
        return "${(avgConsumption).toInt()} $unitString"
    }

    fun getElapsedTimeString(elapsedTime: Long): String {
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
}