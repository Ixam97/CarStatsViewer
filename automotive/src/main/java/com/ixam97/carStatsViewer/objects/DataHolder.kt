package com.ixam97.carStatsViewer.objects

import android.car.VehicleGear
import com.ixam97.carStatsViewer.BuildConfig
import com.ixam97.carStatsViewer.InAppLogger
import com.ixam97.carStatsViewer.plot.enums.*
import com.ixam97.carStatsViewer.plot.objects.*
import java.util.*
import kotlin.collections.ArrayList

object DataHolder {

    private const val maxSmoothSize = 20
    private const val FTC = 10f

    var currentGear: Int = VehicleGear.GEAR_PARK

    var currentPowermW = 0F
        set(value) {
            lastPowermW = field
            field = value

            currentPowerSmooth += ((1f / FTC) * (value - currentPowerSmooth))
            // currentPowerSmoothArray.add(value)
            // if (currentPowerSmoothArray.size > maxSmoothSize) currentPowerSmoothArray.removeAt(0)
        }

    private var currentPowerSmoothArray = arrayListOf<Float>()
    var currentPowerSmooth = 0f
        //get() {
        //    return currentPowerSmooth // Array.average().toFloat()
        //}
        private set

    var lastPowermW = 0F
        private set

    var currentSpeed = 0F
        set(value) {
            lastSpeed = field
            field = value

            currentSpeedSmooth += ((1f / FTC) * (value - currentSpeedSmooth))
            // currentSpeedSmoothArray.add(value)
            // if (currentSpeedSmoothArray.size > maxSmoothSize) currentSpeedSmoothArray.removeAt(0)
        }
    private var currentSpeedSmoothArray = arrayListOf<Float>()
    var currentSpeedSmooth = 0f
        //get() {
        //    return currentSpeedSmooth // Array.average().toFloat()
        //}
        private set

    var avgSpeed = 0F

    var lastSpeed = 0F
        private set

    var currentBatteryCapacity = 0f
        set(value) {
            lastBatteryCapacity = field
            field = value
        }

    var lastBatteryCapacity = 0f
        private set

    var maxBatteryCapacity = 0f

    var tripStartDate = Date()
    var traveledDistance = 0F
    var usedEnergy = 0F
    var chargedEnergy = 0F
    var averageConsumption = 0F
    var chargePortConnected = false
    var travelTimeMillis = 0L
    var chargeTimeMillis = 0L

    var lastPlotDistance = 0F
    var lastPlotEnergy = 0F
    var lastPlotTime = 0L
    var lastPlotGear = VehicleGear.GEAR_PARK
    var lastPlotMarker : PlotLineMarkerType? = null
    var lastChargePower = 0f

    var plotMarkers = PlotMarkers()

    var consumptionPlotLine = PlotLine(
        PlotLineConfiguration(
            PlotRange(-300f, 900f, -300f, 900f, 100f, 0f),
            PlotLineLabelFormat.NUMBER,
            PlotLabelPosition.LEFT,
            PlotHighlightMethod.AVG_BY_DISTANCE,
            "Wh/km"
        ),
    )

    var chargePlotLine = PlotLine(
        PlotLineConfiguration(
            PlotRange(0f, 20f, 0f, 160f, 20f),
            PlotLineLabelFormat.NUMBER,
            PlotLabelPosition.LEFT,
            PlotHighlightMethod.AVG_BY_TIME,
            "kW"
        ),
    )

    var chargeCurves: ArrayList<ChargeCurve> = ArrayList()

    fun stateOfCharge(): Float {
        return 100f / maxBatteryCapacity * currentBatteryCapacity
    }

    fun applyTripData(tripData: TripData) {
        if (tripData.appVersion != BuildConfig.VERSION_NAME) {
            InAppLogger.log("File saved with older app version, trying to convert ...")
        }

        traveledDistance = tripData.traveledDistance ?: 0f
        tripStartDate = tripData.tripStartDate ?: Date()
        usedEnergy = tripData.usedEnergy ?: 0f
        averageConsumption = tripData.averageConsumption ?: 0f
        travelTimeMillis = tripData.travelTimeMillis ?: 0L
        lastPlotDistance = tripData.lastPlotDistance ?: 0F
        lastPlotEnergy = tripData.lastPlotEnergy ?: 0F
        lastPlotTime = tripData.lastPlotTime ?: 0L
        lastPlotGear = tripData.lastPlotGear ?: VehicleGear.GEAR_PARK
        lastPlotMarker = tripData.lastPlotMarker
        lastChargePower = tripData.lastChargePower ?: 0F
        consumptionPlotLine.reset()
        chargePlotLine.reset()

        if (tripData.consumptionPlotLine?.isNotEmpty() == true) {
            consumptionPlotLine.addDataPoints(tripData.consumptionPlotLine)
        }

        chargeCurves = ArrayList()
        if (tripData.chargeCurves?.isNotEmpty() == true) {
            // move StateOfCharge PlotLine to charge PlotLine STateOfCharge Value
            for (curve in tripData.chargeCurves) {
                if (curve.stateOfChargePlotLine?.isNotEmpty() == true) {
                    var lastStateOfCharge = curve.stateOfChargePlotLine!!.first().Value
                    for (index in curve.stateOfChargePlotLine!!.indices) {
                        val stateOfCharge = curve.stateOfChargePlotLine!![index].Value
                        curve.chargePlotLine[index].StateOfCharge = stateOfCharge
                        curve.chargePlotLine[index].StateOfChargeDelta = lastStateOfCharge - stateOfCharge
                        lastStateOfCharge = stateOfCharge
                    }
                    curve.stateOfChargePlotLine = null
                }
                chargeCurves.add(curve)
            }

            chargePlotLine.addDataPoints(tripData.chargeCurves.last().chargePlotLine)
        }

        if (tripData.markers?.isNotEmpty() == true){
            plotMarkers.addMarkers(tripData.markers)
        }
    }

    fun getTripData(fileName: String): TripData {
        return getTripData()
    }

    fun getTripData(): TripData {
        return TripData(
            BuildConfig.VERSION_NAME,
            tripStartDate,
            traveledDistance,
            usedEnergy,
            averageConsumption,
            travelTimeMillis,
            lastPlotDistance,
            lastPlotEnergy,
            lastPlotTime,
            lastPlotGear,
            lastPlotMarker,
            lastChargePower,
            consumptionPlotLine.getDataPoints(PlotDimension.DISTANCE),
            chargeCurves.toList(),
            plotMarkers.markers.toList()
        )
    }

    fun resetDataHolder() {
        traveledDistance = 0f
        tripStartDate = Date()
        usedEnergy = 0f
        chargedEnergy = 0f
        chargeTimeMillis = 0L
        averageConsumption = 0f
        travelTimeMillis = 0L
        lastPlotDistance = 0F
        lastPlotEnergy = 0F
        lastPlotTime = 0L
        lastPlotGear = VehicleGear.GEAR_PARK
        lastPlotMarker = null
        lastChargePower = 0F
        consumptionPlotLine.reset()
        chargePlotLine.reset()
        chargeCurves = ArrayList()
        plotMarkers = PlotMarkers()
    }
}
