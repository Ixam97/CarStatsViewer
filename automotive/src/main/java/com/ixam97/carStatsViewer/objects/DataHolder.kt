package com.ixam97.carStatsViewer.objects

import android.car.VehicleGear
import com.ixam97.carStatsViewer.plot.*

object DataHolder {

    private const val maxSmoothSize = 20
    private const val FTC = 2f

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
    var averageConsumption = 0F
    var chargePortConnected = false

    var resetTimestamp = 0L
    var parkTimestamp = 0L

    var consumptionPlotLine = PlotLine(
        PlotRange(-200f, 600f, -400f, 800f, 100f),
        1f,
        "%.0f",
        "%.0f",
        "Wh/km",
        PlotLabelPosition.LEFT,
        PlotHighlightMethod.AVG_BY_DISTANCE
    )

    var speedPlotLine = PlotLine(
        PlotRange(0f, 40f, 0f, 160f, 40f),
        1f,
        "%.0f",
        "Ø %.0f",
        "km/h",
        PlotLabelPosition.RIGHT,
        PlotHighlightMethod.AVG_BY_TIME,
        false
    )

    var chargePlotLine = PlotLine(
        PlotRange(0f,20f, 0f, 20f,20f),
        1f,
        "%.1f",
        "%.1f",
        "kW",
        PlotLabelPosition.LEFT,
        PlotHighlightMethod.AVG_BY_TIME
    )

    var stateOfChargePlotLine = PlotLine(
        PlotRange(0f,100f, 0f, 100f, 20f),
        1f,
        "%.0f",
        "%.0f",
        "% SoC",
        PlotLabelPosition.RIGHT,
        PlotHighlightMethod.LAST
    )
}
