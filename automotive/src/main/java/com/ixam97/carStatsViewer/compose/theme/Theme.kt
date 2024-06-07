package com.ixam97.carStatsViewer.compose.theme

import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val themedBrands = listOf(
    "Polestar",
    "VolvoCars",
    // "Toy Vehicle"
)

val clubColorsDark = darkColors(
    primary = clubBlue,
    secondary = clubBlue,
    background = Color.Black,
    onBackground = Color.White,
    surface = clubNightVariant,
    onSurface = Color.White,
    onPrimary = Color.White
)

val clubColorsLight = lightColors(
    primary = clubBlueDeep,
    secondary = clubBlueDeep,
    secondaryVariant = clubBlueDeep,
    background = clubLight,
    onBackground = Color.Black,
    surface = clubMedium,
    onSurface = Color.Black,
    onPrimary = Color.Black
)

val polestarColors = darkColors(
    primary = polestarOrange,
    secondary = polestarOrange,
    background = Color.Black,
    onBackground = Color.White,
    surface = polestarSurface,
    onSurface = Color.White,
    onPrimary = Color.White
)

val volvoColors = darkColors(
    primary = volvoBlue,
    secondary = volvoBlue,
    background = Color.Black,
    onBackground = Color.White,
    surface = polestarSurface,
    onSurface = Color.White,
    onPrimary = Color.White
)

private var pActiveElementBrush: Brush? = null
private var pHeaderLineBrush: Brush? = null

object CarTheme {

    val solidBrush: Brush
        @Composable
        get() = pActiveElementBrush?: Brush.linearGradient(listOf(
            MaterialTheme.colors.primary,
            MaterialTheme.colors.primary
        ))

    val activeElementBrush: Brush
        @Composable
        get() = pActiveElementBrush?: solidBrush

    val headerLineBrush: Brush
        @Composable
        get() = pHeaderLineBrush?: solidBrush


}

@Composable
fun CarTheme(carMake: String? = null, content: @Composable () -> Unit) {

    val typography: Typography
    val colors: Colors

    when (carMake) {
        "Polestar" -> {
            typography = defaultPolestarTypography
            colors = polestarColors
        }
        "VolvoCars" -> {
            colors = volvoColors
            typography = defaultTypography
        }
        else -> {
            colors = clubColorsDark
            typography = defaultTypography
            pActiveElementBrush = Brush.horizontalGradient(listOf(clubBlue, clubViolet))
            pHeaderLineBrush = Brush.horizontalGradient(listOf(clubVioletDark, clubViolet, clubBlue, clubBlueDark))

        }
    }

    // val typography = defaultTypography(fontFamily?: FontFamily.Default)

    MaterialTheme(
        colors = colors,
        typography = typography,
        shapes = Shapes,
        content = content
    )
}

@Composable
fun PolestarTheme(content: @Composable () -> Unit) {
    val colors = polestarColors
    MaterialTheme(
        colors = colors,
        typography = polestarTypography,
        shapes = Shapes,
        content = content
    )
}