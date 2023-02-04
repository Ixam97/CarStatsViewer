package com.ixam97.carStatsViewer.utils

import android.text.format.DateFormat
import android.content.Context
import android.content.res.Resources
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.appPreferences.AppPreferences
import java.util.*
import java.util.concurrent.TimeUnit

object StringFormatters {

    lateinit var appPreferences: AppPreferences
    lateinit var dateFormat: java.text.DateFormat
    lateinit var timeFormat: java.text.DateFormat


    fun getDateString(tripStartDate: Date): String {
        return "${dateFormat.format(tripStartDate)}, ${timeFormat.format(tripStartDate)}"
    }

    fun getUsedEnergyString(usedEnergy: Float): String {
        if (!appPreferences.consumptionUnit) {
            return "%.1f kWh".format(
                Locale.ENGLISH,
                usedEnergy / 1000)
        }
        return "${usedEnergy.toInt()} Wh"
    }

    fun getChargedEnergyString(chargedEnergy: Float): String {
        if (!appPreferences.consumptionUnit) {
            return "%.1f kWh".format(Locale.ENGLISH, chargedEnergy / 1000)
        }
        return "${chargedEnergy.toInt()} Wh"
    }

    fun getTraveledDistanceString(traveledDistance: Float): String {
        return "%.1f km".format(Locale.ENGLISH, traveledDistance / 1000)
    }

    fun getAvgConsumptionString(usedEnergy: Float, traveledDistance: Float): String {
        val avgConsumption = usedEnergy / (traveledDistance / 1000)
        val unitString = if (appPreferences.consumptionUnit) "Wh/km" else "kWh/100km"
        if (traveledDistance <= 0) {
            return "-/- $unitString"
        }
        if (!appPreferences.consumptionUnit) {
            return "%.1f %s".format(
                Locale.ENGLISH,
                (avgConsumption)/10,
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
}