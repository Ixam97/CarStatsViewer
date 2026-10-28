package com.ixam97.carStatsViewer.map

import androidx.compose.ui.graphics.Color
import com.ixam97.carStatsViewer.utils.DistanceUnitEnum

/** [value] is given in base units (m/s or Wh/km). */
data class ColorStop(val value: Float, val color: Color)

/**
 * A color scale with the values labeled in the legend ([labels], base units). With [openEnd], the last
 * label gets a "+" since all higher values keep the last color.
 */
data class RouteColorScale(
    val stops: List<ColorStop>,
    val labels: List<Float>,
    val openEnd: Boolean = false
)

/**
 * Fixed color scales for the colored trip route. Used for the map layers as well as the legend,
 * fixed instead of relative to the trip to keep trips comparable with each other.
 */
object RouteColorScales {
    private const val KMH = 1f / 3.6f
    // Same factor as DistanceUnitEnum, so the legend labels convert back to round mph / Wh/mi values.
    private const val MPH = 1.60934f / 3.6f
    private const val WH_PER_MI = 1f / 1.60934f

    // Same spectrum as consumption above 0 (Turbo 0.42 to 0.95), spread linearly over 0 to 180 km/h.
    // Faster sections keep the last color. Labels follow typical limits (30 zone, 50 urban, 100 rural,
    // 130 motorway), extrapolated to 180.
    val speedKmh = RouteColorScale(
        stops = listOf(
            ColorStop(0f * KMH,   Color(0xFF5AFB76)),
            ColorStop(20f * KMH,  Color(0xFF85FC58)),
            ColorStop(40f * KMH,  Color(0xFFB3F242)),
            ColorStop(60f * KMH,  Color(0xFFDDDD32)),
            ColorStop(80f * KMH,  Color(0xFFFABE28)),
            ColorStop(100f * KMH, Color(0xFFFF9821)),
            ColorStop(120f * KMH, Color(0xFFFE6F1A)),
            ColorStop(140f * KMH, Color(0xFFE34612)),
            ColorStop(160f * KMH, Color(0xFFBD2409)),
            ColorStop(180f * KMH, Color(0xFF9A0E00)),
        ),
        labels = listOf(0f, 30f, 50f, 100f, 130f, 180f).map { it * KMH },
        openEnd = true
    )

    // The same spectrum over 0 to 110 mph (~177 km/h), so the legend shows round values. Labels follow
    // typical limits (30 urban, 50 rural, 70 motorway), extrapolated in steps of 20.
    val speedMph = RouteColorScale(
        stops = listOf(
            ColorStop(0f * MPH,   Color(0xFF5AFB76)),
            ColorStop(10f * MPH,  Color(0xFF7DFD5D)),
            ColorStop(20f * MPH,  Color(0xFFA3F749)),
            ColorStop(30f * MPH,  Color(0xFFC7EA3A)),
            ColorStop(40f * MPH,  Color(0xFFE6D52F)),
            ColorStop(50f * MPH,  Color(0xFFFCBB27)),
            ColorStop(60f * MPH,  Color(0xFFFF9C21)),
            ColorStop(70f * MPH,  Color(0xFFFF7A1C)),
            ColorStop(80f * MPH,  Color(0xFFF15816)),
            ColorStop(90f * MPH,  Color(0xFFD6390F)),
            ColorStop(100f * MPH, Color(0xFFB61F07)),
            ColorStop(110f * MPH, Color(0xFF9A0E00)),
        ),
        labels = listOf(0f, 30f, 50f, 70f, 90f, 110f).map { it * MPH },
        openEnd = true
    )

    // Wh/km. Hard cut at 0: regeneration is blue (brighter = stronger), consumption uses the
    // upper part of the Turbo palette (green to dark red) without its blue low end. More stops
    // between 100 and 250 Wh/km, where typical driving lies.
    val consumptionKm = RouteColorScale(
        stops = listOf(
            ColorStop(-150f,  Color(0xFF00E5FF)),  // strong regeneration
            ColorStop(-0.01f, Color(0xFF2962FF)),  // light regeneration, hard cut
            ColorStop(0f,     Color(0xFF5AFB76)),  // turbo(0.42)
            ColorStop(100f,   Color(0xFF96FA4F)),  // turbo(0.50)
            ColorStop(150f,   Color(0xFFDFDB31)),  // turbo(0.60)
            ColorStop(200f,   Color(0xFFFFA222)),  // turbo(0.70)
            ColorStop(250f,   Color(0xFFF45C17)),  // turbo(0.80)
            ColorStop(400f,   Color(0xFF9A0E00)),  // turbo(0.95), very high
        ),
        labels = listOf(-150f, 0f, 100f, 200f, 400f)
    )

    // The same colors at round Wh/mi values (roughly the km stops converted), so the legend shows
    // round values in Wh/mi and kWh/100 mi.
    val consumptionMi = RouteColorScale(
        stops = listOf(
            ColorStop(-250f * WH_PER_MI, Color(0xFF00E5FF)),  // strong regeneration
            ColorStop(-0.01f,            Color(0xFF2962FF)),  // light regeneration, hard cut
            ColorStop(0f,                Color(0xFF5AFB76)),
            ColorStop(150f * WH_PER_MI,  Color(0xFF96FA4F)),
            ColorStop(250f * WH_PER_MI,  Color(0xFFDFDB31)),
            ColorStop(325f * WH_PER_MI,  Color(0xFFFFA222)),
            ColorStop(400f * WH_PER_MI,  Color(0xFFF45C17)),
            ColorStop(650f * WH_PER_MI,  Color(0xFF9A0E00)),  // very high
        ),
        labels = listOf(-250f, 0f, 150f, 300f, 650f).map { it * WH_PER_MI }
    )

    val unknown = Color(0xFF9E9E9E)

    fun speed(distanceUnit: DistanceUnitEnum): RouteColorScale = when (distanceUnit) {
        DistanceUnitEnum.KM -> speedKmh
        DistanceUnitEnum.MILES -> speedMph
    }

    fun consumption(distanceUnit: DistanceUnitEnum): RouteColorScale = when (distanceUnit) {
        DistanceUnitEnum.KM -> consumptionKm
        DistanceUnitEnum.MILES -> consumptionMi
    }

    fun forMode(mode: MapboxInterface.RouteColorMode, distanceUnit: DistanceUnitEnum): RouteColorScale? = when (mode) {
        MapboxInterface.RouteColorMode.SOLID -> null
        MapboxInterface.RouteColorMode.SPEED -> speed(distanceUnit)
        MapboxInterface.RouteColorMode.CONSUMPTION -> consumption(distanceUnit)
    }
}
