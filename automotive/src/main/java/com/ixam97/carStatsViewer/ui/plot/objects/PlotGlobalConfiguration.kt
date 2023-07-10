package com.ixam97.carStatsViewer.ui.plot.objects

import com.ixam97.carStatsViewer.utils.DistanceUnitEnum
import com.ixam97.carStatsViewer.ui.plot.enums.*

object PlotGlobalConfiguration {
    val DataVersion : Int? = 20230206

    val DimensionYConfiguration: HashMap<PlotDimensionY, PlotLineConfiguration> =
        hashMapOf(
            PlotDimensionY.SPEED to PlotLineConfiguration(
                PlotRange(0f, 40f, 0f, 240f, 40f),
                PlotLineLabelFormat.NUMBER,
                PlotHighlightMethod.AVG_BY_VALUE,
                "km/h",
                DimensionSmoothing = 0.005f,
                DimensionSmoothingType = PlotDimensionSmoothingType.PERCENTAGE,
                SessionGapRendering = PlotSessionGapRendering.JOIN,
                DimensionSmoothingHighlightMethod = PlotHighlightMethod.AVG_BY_TIME
            ),
            PlotDimensionY.DISTANCE to PlotLineConfiguration(
                PlotRange(),
                PlotLineLabelFormat.DISTANCE,
                PlotHighlightMethod.NONE,
                "km"
            ),
            PlotDimensionY.TIME to PlotLineConfiguration(
                PlotRange(),
                PlotLineLabelFormat.TIME,
                PlotHighlightMethod.MAX,
                "Time"
            ),
            PlotDimensionY.STATE_OF_CHARGE to PlotLineConfiguration(
                PlotRange(0f, 100f, backgroundZero = 0f),
                PlotLineLabelFormat.PERCENTAGE,
                PlotHighlightMethod.LAST,
                "% SoC",
                DimensionSmoothing = 0.005f,
                DimensionSmoothingType = PlotDimensionSmoothingType.PERCENTAGE,
                DimensionSmoothingHighlightMethod = PlotHighlightMethod.LAST,
                SessionGapRendering = PlotSessionGapRendering.GAP
            ),
            PlotDimensionY.ALTITUDE to PlotLineConfiguration(
                PlotRange(smoothAxis = 20f),
                PlotLineLabelFormat.ALTITUDE,
                PlotHighlightMethod.AVG_BY_TIME,
                "m",
                SessionGapRendering = PlotSessionGapRendering.GAP
            ),
            PlotDimensionY.CONSUMPTION to PlotLineConfiguration(
                PlotRange(-300f, 900f, -300f, 900f, 100f, 0f),
                PlotLineLabelFormat.NUMBER,
                PlotHighlightMethod.AVG_BY_VALUE,
                "Wh/km"
            ),
        )

    fun updateDistanceUnit(distanceUnit: DistanceUnitEnum, consumptionUnitFactor: Boolean = false) {

        DimensionYConfiguration[PlotDimensionY.SPEED]?.UnitFactor = distanceUnit.asFactor()
        DimensionYConfiguration[PlotDimensionY.SPEED]?.Divider = distanceUnit.asFactor()
        DimensionYConfiguration[PlotDimensionY.SPEED]?.Unit = "%s/h".format(distanceUnit.unit())

        DimensionYConfiguration[PlotDimensionY.DISTANCE]?.UnitFactor = distanceUnit.toFactor()
        DimensionYConfiguration[PlotDimensionY.DISTANCE]?.Divider = distanceUnit.asFactor()
        DimensionYConfiguration[PlotDimensionY.DISTANCE]?.Unit = "%s".format(distanceUnit.unit())

        DimensionYConfiguration[PlotDimensionY.ALTITUDE]?.UnitFactor = distanceUnit.asSubFactor()
        DimensionYConfiguration[PlotDimensionY.ALTITUDE]?.Unit = "%s".format(distanceUnit.subUnit())

        if (consumptionUnitFactor) {
            DimensionYConfiguration[PlotDimensionY.CONSUMPTION]?.Unit = "Wh/%s".format(distanceUnit.unit())
            DimensionYConfiguration[PlotDimensionY.CONSUMPTION]?.LabelFormat = PlotLineLabelFormat.NUMBER
            DimensionYConfiguration[PlotDimensionY.CONSUMPTION]?.Divider = distanceUnit.toFactor() * 1f
        }
        else {
            DimensionYConfiguration[PlotDimensionY.CONSUMPTION]?.Unit = "kWh/100%s".format(distanceUnit.unit())
            DimensionYConfiguration[PlotDimensionY.CONSUMPTION]?.LabelFormat = PlotLineLabelFormat.FLOAT
            DimensionYConfiguration[PlotDimensionY.CONSUMPTION]?.Divider = distanceUnit.toFactor() * 10f
        }
    }
}