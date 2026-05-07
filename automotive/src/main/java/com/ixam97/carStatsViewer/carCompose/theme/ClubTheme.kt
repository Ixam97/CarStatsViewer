package com.ixam97.carStatsViewer.carCompose.theme

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import de.ixam97.carcompose.theme.CarThemeConfig
import com.ixam97.carStatsViewer.R
import de.ixam97.carcompose.components.controls.CarRadioButtonColors
import de.ixam97.carcompose.components.controls.CarSwitchColors
import de.ixam97.carcompose.theme.CarColors
import de.ixam97.carcompose.theme.CarNavIconStyle
import de.ixam97.carcompose.theme.CarShapes
import de.ixam97.carcompose.theme.GenericCarDimensions
import de.ixam97.carcompose.theme.GenericCarTypography
import de.ixam97.carcompose.theme.GenericCarUiProperties
import de.ixam97.carcompose.utils.buildGradientBrush
import de.ixam97.carcompose.utils.buildSolidBrush

val clubBlueLight = Color(0xFF60BCD5)
val clubBlue = Color(0xFF447EA6)
val clubBlueMedium = Color(0xFF255577)
val clubBlueDark = Color(0xFF161D39)
val clubVioletLight = Color(0xFF8480BD)
val clubViolet = Color(0xFF77347B)
val clubVioletMedium = Color(0xFF4F1852)
val clubVioletDark = Color(0xFF2A1037)

val clubAccentContainerBrush
    @Composable get() = buildGradientBrush(listOf(
        clubBlueMedium,
        clubVioletMedium
    ))

val clubPassiveAccentContainerBrush
    @Composable get() = buildGradientBrush(listOf(
        colorResource(R.color.club_blue_dark),
        colorResource(R.color.club_violet_dark)
    ))

val clubPrimaryDividerBrush
    @Composable get() = buildGradientBrush(listOf(
        colorResource(R.color.club_violet_dark),
        colorResource(R.color.club_violet),
        colorResource(R.color.club_blue),
        colorResource(R.color.club_blue_dark),
    ))

val ClubThemeConfig: CarThemeConfig
    @Composable get() = CarThemeConfig(
        carTypography = GenericCarTypography,
        carDimensions = GenericCarDimensions,
        carUiProperties = GenericCarUiProperties.copy(
            backButtonIconStyle = CarNavIconStyle.ArrowBackwards,
            listSectionBackground = true
        ),
        carDarkColors = CarColors(
            accent = colorResource(R.color.club_blue),
            accentContainer = colorResource(R.color.club_blue),
            accentContainerBrush = clubAccentContainerBrush,
            background = Color.Black,
            primarySurface = colorResource(R.color.club_night_variant),// Color(0xFF2C2C2C),
            secondarySurface = colorResource(R.color.club_night_variant), //Color(0xFF3F3F3F),
            onBackground = Color.White,
            onSurface = Color.White,
            onAccentContainer = Color.White,
            primaryDivider = clubPrimaryDividerBrush,
            secondaryDivider = buildSolidBrush(Color(0xFF2C2C2C)),
            listSectionBackground = buildSolidBrush(colorResource(R.color.club_night).copy(alpha = 0.85f)),
            switchColors = CarSwitchColors(
                border = Color.Transparent,
                track = buildSolidBrush(colorResource(R.color.club_night_variant)),
                onTrack = Color.White,
                trackChecked = buildSolidBrush(colorResource(R.color.club_night_variant)),
                onTrackChecked = Color.White,
                thumb = clubAccentContainerBrush,
                onThumb = Color.White
            ),
            radioButtonColors = CarRadioButtonColors(
                background = colorResource(R.color.club_night_variant),
                borderColor = colorResource(R.color.club_night_variant),
                selectedBorderColor = Color.Transparent,
                selectedBackground = colorResource(R.color.club_blue),
                selectorColor = colorResource(R.color.club_night_darker)
            )
        ),
        carShapes = CarShapes(
            defaultOuterCornerSize = CornerSize(25.dp),
            defaultInnerCornerSize = CornerSize(5.dp)
        )
    )