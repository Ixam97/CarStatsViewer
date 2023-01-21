package com.ixam97.carStatsViewer.objects

import android.car.VehicleGear
import com.ixam97.carStatsViewer.BuildConfig
import com.ixam97.carStatsViewer.InAppLogger
import com.ixam97.carStatsViewer.plot.*
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

    var currentBatteryCapacity = 0
        set(value) {
            lastBatteryCapacity = field
            field = value
        }

    var lastBatteryCapacity = 0
        private set

    var maxBatteryCapacity = 0

    var traveledDistance = 0F
    var usedEnergy = 0F
    var chargedEnergy = 0F
    var averageConsumption = 0F
    var chargePortConnected = false
    var travelTimeMillis = 0L

    var lastPlotDistance = 0F
    var lastPlotEnergy = 0F
    var lastPlotTime = 0L
    var lastPlotGear = VehicleGear.GEAR_PARK
    var lastPlotMarker : PlotLineMarkerType? = null
    var lastChargePower = 0f

    var plotMarkers = PlotMarkers()

    var consumptionPlotLine = PlotLine(
        PlotRange(-300f, 900f, -300f, 900f, 100f, 0f),
        1f,
        "%.0f",
        "%.0f",
        "Wh/km",
        PlotLabelPosition.LEFT,
        PlotHighlightMethod.AVG_BY_DISTANCE
    )

    var speedPlotLine = PlotLine(
        PlotRange(0f, 40f, 0f, 240f, 40f),
        1f,
        "%.0f",
        "Ø %.0f",
        "km/h",
        PlotLabelPosition.RIGHT,
        PlotHighlightMethod.AVG_BY_TIME,
        false
    )

    var chargePlotLine = PlotLine(
        PlotRange(0f, 20f, 0f, 160f, 20f),
        1f,
        "%.0f",
        "Ø %.0f",
        "kW",
        PlotLabelPosition.LEFT,
        PlotHighlightMethod.AVG_BY_TIME
    )

    var stateOfChargePlotLine = PlotLine(
        PlotRange(0f, 100f, 0f, 100f, 20f, 0f),
        1f,
        "%.0f",
        "%.0f %%",
        "% SoC",
        PlotLabelPosition.RIGHT,
        PlotHighlightMethod.LAST
    )

    var chargeCurves: ArrayList<ChargeCurve> = ArrayList()

    fun applyTripData(tripData: TripData) {
        if (tripData.appVersion != BuildConfig.VERSION_NAME) InAppLogger.log("File saved with older app version, trying to convert ...")
        traveledDistance = if (tripData.traveledDistance != null) tripData.traveledDistance else 0f
        usedEnergy = if (tripData.usedEnergy != null) tripData.usedEnergy else 0f
        averageConsumption = if(tripData.averageConsumption != null) tripData.averageConsumption else 0f
        travelTimeMillis = if(tripData.travelTimeMillis != null) tripData.travelTimeMillis else 0L
        lastPlotDistance = if(tripData.lastPlotDistance != null) tripData.lastPlotDistance else 0F
        lastPlotEnergy = if(tripData.lastPlotEnergy != null) tripData.lastPlotEnergy else 0F
        lastPlotTime = if(tripData.lastPlotTime != null) tripData.lastPlotTime else 0L
        lastPlotGear = if(tripData.lastPlotGear != null) tripData.lastPlotGear else VehicleGear.GEAR_PARK
        lastPlotMarker = tripData.lastPlotMarker
        lastChargePower = if(tripData.lastChargePower != null) tripData.lastChargePower else 0F
        consumptionPlotLine.reset()
        speedPlotLine.reset()
        if (tripData.consumptionPlotLine != null) consumptionPlotLine.addDataPoints(tripData.consumptionPlotLine)
        if (tripData.speedPlotLine != null) speedPlotLine.addDataPoints(tripData.speedPlotLine)
        // chargeCurves = if (tripData.chargeCurves != null) ArrayList(tripData.chargeCurves) else ArrayList()
        if (tripData.markers != null) plotMarkers.addMarkers(tripData.markers)
    }

    fun getTripData(): TripData {
        return TripData(
            BuildConfig.VERSION_NAME,
            Date(),
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
            speedPlotLine.getDataPoints(PlotDimension.DISTANCE),
            // chargeCurves.toList()
            plotMarkers.markers.toList()
        )
    }
}
