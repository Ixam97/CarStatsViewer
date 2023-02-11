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
                PlotHighlightMethod.AVG_BY_DIMENSION,
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
                "% SoC"
            )
        )

    fun updateDistanceUnit(distanceUnit: DistanceUnitEnum) {
        SecondaryDimensionConfiguration[PlotSecondaryDimension.SPEED]?.Divider = 1f / distanceUnit.toFactor()
        SecondaryDimensionConfiguration[PlotSecondaryDimension.SPEED]?.Unit = "%s/h".format(distanceUnit.unit())

        SecondaryDimensionConfiguration[PlotSecondaryDimension.DISTANCE]?.Divider = 1f / distanceUnit.toFactor()
        SecondaryDimensionConfiguration[PlotSecondaryDimension.DISTANCE]?.Unit = "%s".format(distanceUnit.unit())
    }
}