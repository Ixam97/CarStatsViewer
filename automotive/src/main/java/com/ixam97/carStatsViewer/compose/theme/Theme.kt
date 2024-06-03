package com.ixam97.carStatsViewer.compose.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val themedBrands = listOf(
    "Polestar",
    "Volvo",
    "Toy Vehicle"
)

private fun carAppColorPalate(primaryColor: Color? = null) = darkColors(
    primary = primaryColor ?: primaryGray,
    secondary = primaryColor ?: secondaryGray,
    background = darkBackground,
    onBackground = Color.White,
    surface = darkBackground,
    onSurface = Color.White,
    onPrimary = Color.White
)

@Composable
fun ComposeTestTheme(carMake: String? = null, content: @Composable () -> Unit) {
    val colors = carAppColorPalate(
        primaryColor = when (carMake) {
            "Polestar" -> polestarOrange
            "Volvo" -> volvoBlue
            else -> null
        }
    )

    MaterialTheme(
        colors = colors,
        typography = defaultTypography,
        shapes = Shapes,
        content = content
    )
}

@Composable
fun PolestarTheme(content: @Composable () -> Unit) {
    val colors = carAppColorPalate(
        primaryColor = polestarOrange
    )
    MaterialTheme(
        colors = colors,
        typography = polestarTypography,
        shapes = Shapes,
        content = content
    )
}