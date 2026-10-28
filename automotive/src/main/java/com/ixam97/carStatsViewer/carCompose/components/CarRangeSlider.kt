package com.ixam97.carStatsViewer.carCompose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import de.ixam97.carcompose.theme.CarTheme

/**
 * Range slider with two thumbs, sized for car touch screens (thumbs 48 dp, touch target at least
 * 76 dp high) and colored from [CarTheme]. Only depends on Compose and CarTheme, so it can be moved
 * into the CarCompose library.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarRangeSlider(
    value: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val colors = SliderDefaults.colors(
        thumbColor = CarTheme.carColors.accentContainer,
        activeTrackColor = CarTheme.carColors.accentContainer,
        inactiveTrackColor = CarTheme.carColors.secondarySurface,
        disabledThumbColor = CarTheme.carColors.accentContainer.copy(alpha = CarTheme.carColors.disabledAlpha),
        disabledActiveTrackColor = CarTheme.carColors.accentContainer.copy(alpha = CarTheme.carColors.disabledAlpha),
    )
    val thumb = @Composable {
        Box(
            modifier = Modifier
                .size(CarTheme.carDimensions.iconButtonSize)
                .clip(CircleShape)
                .background(if (enabled) colors.thumbColor else colors.disabledThumbColor)
        )
    }
    RangeSlider(
        modifier = modifier.heightIn(min = 76.dp),
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        enabled = enabled,
        onValueChangeFinished = onValueChangeFinished,
        colors = colors,
        startThumb = { thumb() },
        endThumb = { thumb() },
        track = { state ->
            SliderDefaults.Track(
                rangeSliderState = state,
                modifier = Modifier.heightIn(min = 16.dp),
                enabled = enabled,
                colors = colors,
                drawStopIndicator = null,
                thumbTrackGapSize = 0.dp,
            )
        },
    )
}
