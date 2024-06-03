package com.ixam97.carStatsViewer.compose.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.compose.theme.disabledOverlayColor
import com.ixam97.carStatsViewer.compose.theme.polestarOrange

@Composable
fun Polestar2MultiSelect(
    modifier: Modifier = Modifier,
    title: String,
    text: String? = null,
    options: List<String>,
    onIndexChanged: (index: Int) -> Unit,
    selectedIndex: Int,
    enabled: Boolean = true
) {
    Box(modifier = modifier) {
        Column {

            Polestar2Row(
                title = title,
                text = text,
                enabled = enabled,
                minHeight = 0.dp
            )

            var size by remember {
                mutableStateOf(IntSize.Zero)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .height(101.dp)
                    .padding(horizontal = 24.dp)
                    .background(colorResource(id = R.color.default_button_color))
                    .onSizeChanged { size = it }
            ) {

                Row (
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    options.forEachIndexed { index, s ->
                        Spacer(modifier = Modifier.weight(1f))
                        if (index < options.size - 1) {
                            Divider(
                                modifier = Modifier
                                    .width(2.dp)
                                    .fillMaxHeight()
                                    .padding(vertical = 20.dp)
                            )
                        }
                    }
                }

                val animatedLeftPadding = animateDpAsState(targetValue = with(LocalDensity.current) { ((size.width.toFloat() / options.size) * selectedIndex).toDp() - 2.dp})

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(start = animatedLeftPadding.value.coerceAtLeast(0.dp))
                        .width(with(LocalDensity.current) { (size.width.toFloat() / options.size).toDp() + if (selectedIndex == 0) 2.dp else 4.dp })
                        .background(polestarOrange)
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    options.forEachIndexed { index, s ->
                        Column(
                            modifier = Modifier
                                .clickable(enabled = enabled) {
                                    onIndexChanged(index)
                                }
                                .fillMaxHeight()
                                .weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                text = s,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                if (!enabled) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(disabledOverlayColor)
                    )
                }
            }
        }
    }
}