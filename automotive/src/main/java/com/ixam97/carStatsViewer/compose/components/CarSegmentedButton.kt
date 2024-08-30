package com.ixam97.carStatsViewer.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ixam97.carStatsViewer.compose.theme.CarTheme

@Composable
fun CarSegmentedButton(
    modifier: Modifier = Modifier,
    options: List<String>,
    selectedIndex: Int,
    onSelectedIndexChanged: (index: Int) -> Unit,
    enabled: Boolean = true,
    gradient: Brush = CarTheme.brushes.activeElementBrush,
    contentPadding: PaddingValues = CarTheme.buttonPaddingValues,
    shape: Shape = RoundedCornerShape(CarTheme.buttonCornerRadius),
) {
    Box(modifier = Modifier
        .height(IntrinsicSize.Max)
        .clip(shape)
        .then(modifier)
    ) {
        Box(modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            // .border(1.dp, MaterialTheme.colors.onBackground, shape)
        )
        Row(
            modifier = Modifier
                .clip(shape),
                // .border(3.dp, MaterialTheme.colors.primary, shape),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { index, option ->
                 if (index > 0) Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(Color.Transparent)
                )

                val textModifier = if (index == selectedIndex) {
                    Modifier.background(gradient)
                } else {
                    Modifier.background(MaterialTheme.colors.surface)
                }

                Text(
                    modifier = textModifier
                        .clickable {
                            onSelectedIndexChanged(index)
                        }
                        .weight(1f)
                        .padding(paddingValues = contentPadding),
                    text = option,
                    softWrap = false,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}