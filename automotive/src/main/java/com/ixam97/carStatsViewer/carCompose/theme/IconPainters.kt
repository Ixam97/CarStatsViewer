package com.ixam97.carStatsViewer.carCompose.theme

import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import com.ixam97.carStatsViewer.R
import de.ixam97.carcompose.theme.CarTheme

@Composable
fun CarComposeIcon(
    @DrawableRes resID: Int,
    modifier: Modifier = Modifier,
) {
    Icon(
        modifier = modifier
            .size(CarTheme.carDimensions.iconButtonSize),
        painter = painterResource(resID),
        contentDescription = null,
    )
}

@Composable
fun CarComposeIcon(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
) {
    Icon(
        modifier = modifier
            .size(CarTheme.carDimensions.iconButtonSize),
        imageVector = imageVector,
        contentDescription = null,
    )
}

@Composable
fun adaptiveIconPainterResource(
    @DrawableRes resID: Int
): Painter {
    val context = LocalContext.current
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val adaptiveIcon = remember(resID) {
            ResourcesCompat.getDrawable(context.resources, resID, context.theme)
        }
        if (adaptiveIcon != null) {
            remember(resID) { BitmapPainter(adaptiveIcon.toBitmap().asImageBitmap()) }
        } else {
            painterResource(R.drawable.ic_error)
        }
    } else {
        painterResource(resID)
    }
}