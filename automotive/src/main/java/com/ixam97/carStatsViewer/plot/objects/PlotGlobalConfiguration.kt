package com.ixam97.carStatsViewer.plot.objects

import com.ixam97.carStatsViewer.enums.DistanceUnitEnum
import com.ixam97.carStatsViewer.plot.enums.*

object PlotGlobalConfiguration {
    val DataVersion : Int? = 20230206

    val SecondaryDimensionConfiguration: HashMap<PlotSecondaryDimension, PlotLineConfiguration> =
        hashMapOf(
            PlotSecondaryDimension.SPEED to PlotLineConfiguration(
                PlotRange(0f, 40f, 0f, 240f, 40f),
                PlotLineLabelFormat.NUMBER,
                PlotLabelPosition.RIGHT,
                PlotHighlightMethod.AVG_BY_TIME,
                "km/h"
            ),
            PlotSecondaryDimension.DISTANCE to PlotLineConfiguration(
                PlotRange(),
                PlotLineLabelFormat.DISTANCE,
                PlotLabelPosition.RIGHT,
                PlotHighlightMethod.NONE,
                "km"
            ),
            PlotSecondaryDimension.TIME to PlotLineConfiguration(
                PlotRange(),
                PlotLineLabelFormat.TIME,
                PlotLabelPosition.RIGHT,
                PlotHighlightMethod.MAX,
                "Time"
            ),
            PlotSecondaryDimension.STATE_OF_CHARGE to PlotLineConfiguration(
                PlotRange(0f, 100f, backgroundZero = 0f),
                PlotLineLabelFormat.PERCENTAGE,
                PlotLabelPosition.RIGHT,
                PlotHighlightMethod.LAST,
                "% SoC",
                DimensionSmoothing = 1f,
                DimensionSmoothingType = PlotDimensionSmoothingType.PIXEL,
                DimensionSmoothingHighlightMethod = PlotHighlightMethod.MAX,
                SessionGapRendering = PlotSessionGapRendering.GAP
            )
        )

    fun updateDistanceUnit(distanceUnit: DistanceUnitEnum) {

        SecondaryDimensionConfiguration[PlotSecondaryDimension.SPEED]?.UnitFactor = distanceUnit.asFactor()
        SecondaryDimensionConfiguration[PlotSecondaryDimension.SPEED]?.Divider = distanceUnit.asFactor()
        SecondaryDimensionConfiguration[PlotSecondaryDimension.SPEED]?.Unit = "%s/h".format(distanceUnit.unit())


        SecondaryDimensionConfiguration[PlotSecondaryDimension.DISTANCE]?.UnitFactor = distanceUnit.toFactor()
        SecondaryDimensionConfiguration[PlotSecondaryDimension.DISTANCE]?.Divider = distanceUnit.asFactor()
        SecondaryDimensionConfiguration[PlotSecondaryDimension.DISTANCE]?.Unit = "%s".format(distanceUnit.unit())
    }
}