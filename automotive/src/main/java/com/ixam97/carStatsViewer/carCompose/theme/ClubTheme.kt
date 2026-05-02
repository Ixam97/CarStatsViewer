//package com.ixam97.carStatsViewer.carCompose.theme
//
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.CornerSize
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.res.colorResource
//import androidx.compose.ui.unit.dp
//import com.ixam97.carStatsViewer.R
//import de.ixam97.carcompose.theme.CarColors
//import de.ixam97.carcompose.theme.CarShapes
//import de.ixam97.carcompose.theme.CarThemeConfig
//import de.ixam97.carcompose.theme.GenericCarColors
//import de.ixam97.carcompose.theme.GenericCarDimensions
//import de.ixam97.carcompose.theme.GenericCarShapes
//import de.ixam97.carcompose.theme.GenericCarTypography
//import de.ixam97.carcompose.theme.GenericCarUiProperties
//
//val clubColors: CarColors
//@Composable get() = GenericCarColors.copy(
//    accent = colorResource(R.color.club_blue),// Color(0xFF447ea6),
//    accentContainer = listOf(
//        colorResource(R.color.club_blue),
//        colorResource(R.color.club_violet)
//    ),
//    background = Color.Black,
//    secondarySurface = listOf(colorResource(R.color.club_night_variant)),
//    onBackground = colorResource(R.color.club_light),
//    onSurface = colorResource(R.color.club_light),
//    onAccentContainer = colorResource(R.color.club_light),
//    primaryDivider = listOf(
//        colorResource(R.color.club_violet_dark),
//        colorResource(R.color.club_violet),
//        colorResource(R.color.club_blue),
//        colorResource(R.color.club_blue_dark),
//    )
//)
//
//val clubDimensions = GenericCarDimensions.copy()
//
//val clubShapes = CarShapes(
//    buttonCornerSize = CornerSize(25.dp),
//    segmentedButtonInnerCornerSize = CornerSize(5.dp),
//    switchThumbShape = RoundedCornerShape(CornerSize(5.dp)),
//    radioButtonOuterShape = CircleShape
//)
//
//val ClubThemeConfig: CarThemeConfig
//    @Composable get()= CarThemeConfig(
//        carTypography = GenericCarTypography,
//        carDimensions = clubDimensions,
//        carUiProperties = GenericCarUiProperties,
//        carDarkColors = clubColors,
//        carShapes = clubShapes,
//    )