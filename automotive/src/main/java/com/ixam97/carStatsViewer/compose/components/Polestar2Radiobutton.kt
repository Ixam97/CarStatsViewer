package com.ixam97.carStatsViewer.compose.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.compose.theme.disabledOverlayColor

@Composable
fun Polestar2Radiobutton(
    title: String,
    text: String? = null,
    @DrawableRes iconResId: Int? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    selected: Boolean
) {

    val animatedIndicatorSize = animateDpAsState(targetValue = if (selected) 37.dp else 0.dp)

    Row (
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick ?: {})
            .padding(end = 24.dp)
            .wrapContentHeight()
            .fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Polestar2Row(
            modifier = Modifier.weight(1f),
            title = title,
            text = text,
            enabled = enabled,
            iconResId = iconResId
        )
        Spacer(modifier = Modifier.width(20.dp))
        Column(
            modifier = Modifier
                .padding(vertical = 6.dp)
                .size(101.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box (
                modifier = Modifier
                    .size(60.dp)
            ) {
                Column(
                    modifier = Modifier
                        .size(60.dp)
                        .border(
                            width = 2.dp,
                            color = colorResource(id = if (selected) R.color.polestar_orange else R.color.secondary_text_color)
                        ),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(animatedIndicatorSize.value)
                            .background(color = colorResource(id = R.color.polestar_orange))
                    )
                }
                if (!enabled) {
                    Box (
                        modifier = Modifier
                            .fillMaxSize()
                            .background(disabledOverlayColor)
                    )
                }
            }
        }
    }
}