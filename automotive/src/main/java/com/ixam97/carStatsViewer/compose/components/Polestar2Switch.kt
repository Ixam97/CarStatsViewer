package com.ixam97.carStatsViewer.compose.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.compose.theme.disabledOverlayColor

@Composable
fun Polestar2Switch(
    switchState: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
    title: String,
    text: String? = null,
    @DrawableRes iconResId: Int? = null
) {

    val animatedSwitchThumbPadding = animateDpAsState(
        targetValue = if (switchState) 144.dp else 0.dp
    )

    Row (
        modifier = Modifier
            .clickable(enabled = enabled) { onClick() }
            .padding(end = 24.dp)
            .wrapContentHeight()
            .fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Polestar2Row(
            modifier = Modifier.weight(1f),
            title = title,
            text = text,
            iconResId = iconResId,
            enabled = enabled)
        Spacer(modifier = Modifier.width(20.dp))
        Box(
            modifier = Modifier
                .padding(vertical = 6.dp)
                .height(101.dp)
                .width(288.dp)
                .background(colorResource(id = R.color.default_button_color))
        ) {
            Box(
                modifier = Modifier
                    .padding(start = animatedSwitchThumbPadding.value)
                    .width(144.dp)
                    .fillMaxHeight()
                    .background(colorResource(id = R.color.polestar_orange))
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Text(
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f),
                    text = "Off"
                )
                Text(
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f),
                    text = "On"
                )
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