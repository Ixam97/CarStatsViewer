package com.ixam97.carStatsViewer.plot.objects

class PlotPoint(
    val x: Float,
    val y: Float,
    val value: Float?)

class PlotLineItemPoint(
    val x: Float,
    val y: PlotLineItem,
    val group: Any)