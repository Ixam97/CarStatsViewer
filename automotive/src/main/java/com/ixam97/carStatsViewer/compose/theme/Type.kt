package com.ixam97.carStatsViewer.compose.theme

import androidx.compose.material.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import java.io.File


val polestarFont = if (File("/product/fonts/PolestarUnica77-Regular.otf").exists())  {
    FontFamily(
        Font(
            File("/product/fonts/PolestarUnica77-Regular.otf"),
            // volvosansgloballight1
            weight = FontWeight.Normal
        ),
        Font(
            File("/product/fonts/PolestarUnica77-Medium.otf"),
            // volvosansglobalmedium1
            weight = FontWeight.Medium
        )
    )
} else if (File("/product/fonts/Unica77PolestarTT-Regular.ttf").exists()) {
    FontFamily(
        Font(
            File("/product/fonts/Unica77PolestarTT-Regular.ttf")
        )
    )
} else {
    FontFamily.Default
}

val defaultTypography = Typography(
    defaultFontFamily = FontFamily.Default,
    body1 = TextStyle(
        // fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 30.sp,
        color = Color.White
    ),
    body2 = TextStyle(
        // fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        color = Color.White
    ),
    button = TextStyle(
        // fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 30.sp
    ),
    h1 = TextStyle(
        // fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 38.sp,
        color = Color.White
    ),
    h2 = TextStyle(
        // fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 32.sp,
        color = Color.White
    ),
    h6 = TextStyle(
        // fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 32.sp,
        color = Color.White
    )
)


val defaultPolestarTypography = Typography(
    defaultFontFamily = polestarFont,
    body1 = TextStyle(
        // fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        color = Color.White
    ),
    button = TextStyle(
        // fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp
    ),
    h1 = TextStyle(
        // fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 38.sp,
        color = Color.White
    ),
    h2 = TextStyle(
        // fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 35.sp,
        color = Color.White
    )
)

val polestarTypography = Typography(
    defaultFontFamily = polestarFont,
    body1 = TextStyle(
        // fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 31.sp,
        color = Color.White
    ),
    button = TextStyle(
        // fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 31.sp
    ),
    h1 = TextStyle(
        // fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 48.sp,
        color = Color.White
    ),
    h2 = TextStyle(
        // fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 38.sp,
        color = Color.White
    )
)
