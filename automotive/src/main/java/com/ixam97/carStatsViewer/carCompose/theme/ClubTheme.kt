package com.ixam97.carStatsViewer.carCompose.theme

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.ixam97.carcompose.theme.CarThemeConfig
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

val clubNightDarker = Color(0xff05080C)
val clubNight = Color(0xff0a0f14)
val clubNightVariant = Color(0xff111922)

val clubAccentContainerBrush= buildGradientBrush(listOf(
    clubBlueMedium,
    clubVioletMedium
))

val clubPrimaryDividerBrush = buildGradientBrush(listOf(
    clubVioletDark,
    clubViolet,
    clubBlue,
    clubBlueDark
))

val ClubThemeConfig = CarThemeConfig(
    carTypography = GenericCarTypography,
    carDimensions = GenericCarDimensions,
    carUiProperties = GenericCarUiProperties.copy(
        backButtonIconStyle = CarNavIconStyle.ArrowBackwards,
        listSectionBackground = true
    ),
    carDarkColors = CarColors(
        accent = clubBlue,
        accentContainer = clubBlue,
        accentContainerBrush = clubAccentContainerBrush,
        background = Color.Black,
        primarySurface = clubNightVariant,// Color(0xFF2C2C2C),
        secondarySurface = clubNightVariant, //Color(0xFF3F3F3F),
        onBackground = Color.White,
        onSurface = Color.White,
        onAccentContainer = Color.White,
        primaryDivider = clubPrimaryDividerBrush,
        secondaryDivider = buildSolidBrush(Color(0xFF2C2C2C)),
        listSectionBackground = buildSolidBrush(clubNight.copy(alpha = 0.85f)),
        switchColors = CarSwitchColors(
            border = Color.Transparent,
            track = buildSolidBrush(clubNightVariant),
            onTrack = Color.White,
            trackChecked = buildSolidBrush(clubNightVariant),
            onTrackChecked = Color.White,
            thumb = clubAccentContainerBrush,
            onThumb = Color.White
        ),
        radioButtonColors = CarRadioButtonColors(
            background = clubNightVariant,
            borderColor = clubNightVariant,
            selectedBorderColor = Color.Transparent,
            selectedBackground = clubBlue,
            selectorColor = clubNightDarker
        )
    ),
    carShapes = CarShapes(
        defaultOuterCornerSize = CornerSize(25.dp),
        defaultInnerCornerSize = CornerSize(5.dp)
    )
)