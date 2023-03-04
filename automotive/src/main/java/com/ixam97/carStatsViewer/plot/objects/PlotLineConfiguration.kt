package com.ixam97.carStatsViewer.plot.objects

import com.ixam97.carStatsViewer.plot.enums.*

class PlotLineConfiguration(
    internal val Range: PlotRange,
    var LabelFormat: PlotLineLabelFormat,
    var LabelPosition: PlotLabelPosition,
    var HighlightMethod: PlotHighlightMethod,
    var Unit: String,
    var Divider: Float = 1f,
    var UnitFactor: Float = 1f,
    var DimensionSmoothing: Float? = null,
    var DimensionSmoothingType: PlotDimensionSmoothingType? = null,
    var DimensionSmoothingHighlightMethod: PlotHighlightMethod? = null,
    var SessionGapRendering : PlotSessionGapRendering? = null
)