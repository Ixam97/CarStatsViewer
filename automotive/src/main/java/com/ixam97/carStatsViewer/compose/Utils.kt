package com.ixam97.carStatsViewer.compose

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.compose.theme.ColorTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun brandSelector(themeSetting: Int): String? = when (CarStatsViewer.appPreferences.colorTheme) {
    ColorTheme.OEM -> {
        if (Build.MODEL == "Polestar 2") Build.MODEL
        else if (CarStatsViewer.dataProcessor.staticVehicleData.modelName == "PS2") "Polestar 2"
        else Build.BRAND
    }
    ColorTheme.ORANGE -> "Orange"
    ColorTheme.BLUE -> "Blue"
    else -> null
}

@Composable
fun Modifier.verticalScrollWithScrollbar(
    state: ScrollState
): Modifier = this
    .verticalScrollBar(state)
    .verticalScroll(state)

@Composable
fun Modifier.verticalScrollBar(
    state: ScrollState,
    color: Color = Color.Gray,
    // ratio: Float = 3f,
    width: Dp = 6.dp
): Modifier {
    var initial by remember { mutableStateOf(true) }// = state.lastScrolledBackward == state.lastScrolledForward
    val targetAlpha = if (state.isScrollInProgress || initial) 1f else 0f
    val duration = if (state.isScrollInProgress) 50 else 500
    val delay = if (state.isScrollInProgress) 0 else 750

    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = duration, delayMillis = delay)
    )

    LaunchedEffect(null) {
        CoroutineScope(Dispatchers.Default).launch {
            delay(1000)
            initial = false
        }
    }

    return drawWithContent {
        drawContent()

        val needDrawScrollbar = state.isScrollInProgress || alpha > 0.0f
        val viewportHeight = this.size.height
        val contentHeight = state.maxValue + viewportHeight
        val barHeight = viewportHeight * (viewportHeight / contentHeight)
        val barRange = (viewportHeight - barHeight) / state.maxValue
        if (needDrawScrollbar && contentHeight > viewportHeight) {
            val position = state.value * barRange
            drawRect(
                color = color.copy(alpha = alpha),
                topLeft = Offset(this.size.width - width.toPx() - 4f, position),
                size = Size(width.toPx(), barHeight)
            )
        }
    }
}