package com.ixam97.carStatsViewer.objects

import com.ixam97.carStatsViewer.plot.*

object DataHolder {
    var currentPowermW = 0F
        set(value) {
            lastPowermW = field
            field = value

            currentPowerSmoothArray.add(value)
            if (currentPowerSmoothArray.size > 20) currentPowerSmoothArray.removeAt(0)
        }

    private var currentPowerSmoothArray = arrayListOf<Float>()
    var currentPowerSmooth = 0f
        get() {
            return currentPowerSmoothArray.average().toFloat()
        }
        private set

    var lastPowermW = 0F
        private set

    var currentSpeed = 0F
        set(value) {
            lastSpeed = field
            field = value

            currentSpeedSmoothArray.add(value)
            if (currentSpeedSmoothArray.size > 20) currentSpeedSmoothArray.removeAt(0)
        }
    private var currentSpeedSmoothArray = arrayListOf<Float>()
    var currentSpeedSmooth = 0f
        get() {
            return currentSpeedSmoothArray.average().toFloat()
        }
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

    var consumptionPlotLine = PlotLine(
        -200f,
        600f,
        100f,
        1f,
        "%.0f",
        "%.0f",
        "Wh/km",
        PlotLabelPosition.LEFT,
        PlotHighlightMethod.AVG_BY_TIME
    )

    var speedPlotLine = PlotLine(
        0f,
        40f,
        40f,
        1f,
        "%.0f",
        "Ø %.0f",
        "km/h",
        PlotLabelPosition.RIGHT,
        PlotHighlightMethod.AVG_BY_TIME,
        false
    )

    var chargePlotLine = PlotLine(
        0f,
        20f,
        20f,
        1f,
        "%.1f",
        "%.1f",
        "kW",
        PlotLabelPosition.LEFT,
        PlotHighlightMethod.AVG_BY_TIME
    )

    var stateOfChargePlotLine = PlotLine(
        0f,
        100f,
        20f,
        1f,
        "%.0f",
        "%.0f",
        "% SoC",
        PlotLabelPosition.RIGHT,
        PlotHighlightMethod.LAST
    )
}
