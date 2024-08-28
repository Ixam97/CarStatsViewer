package com.ixam97.carStatsViewer.compose.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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

    val buttonCornerRadius = 20.dp
    val buttonPaddingValues = PaddingValues(horizontal = 40.dp, vertical = 20.dp)

}

@Composable
fun CarTheme(carMake: String? = null, content: @Composable () -> Unit) {

    val typography: Typography
    val colors: Colors

    when (carMake) {
        "Polestar" -> {
            typography = defaultPolestarTypography
            colors = polestarColors
            pActiveElementBrush = Brush.horizontalGradient(listOf(colors.primary, colors.primary))
            pHeaderLineBrush = Brush.horizontalGradient(listOf(colors.primary, colors.primary))

        }
        "VolvoCars" -> {
            colors = volvoColors
            typography = defaultTypography
            pActiveElementBrush = Brush.horizontalGradient(listOf(colors.primary, colors.primary))
            pHeaderLineBrush = Brush.horizontalGradient(listOf(colors.primary, colors.primary))
        }
        else -> {
            colors = clubColorsDark
            typography = defaultTypography
            pActiveElementBrush = Brush.horizontalGradient(listOf(clubBlue, clubViolet))
            pHeaderLineBrush = Brush.horizontalGradient(listOf(clubVioletDark, clubViolet, clubBlue, clubBlueDark))

        }
    }


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