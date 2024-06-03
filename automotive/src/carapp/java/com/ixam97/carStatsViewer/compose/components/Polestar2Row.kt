package com.ixam97.carStatsViewer.compose.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.compose.theme.disabledTextColor

@Composable
fun Polestar2Row(
    modifier: Modifier = Modifier,
    title: String,
    text: String? = null,
    @DrawableRes iconResId: Int? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    minHeight: Dp = 113.dp
) {
    val rowModifier = if (onClick != null) {
        modifier
            .clickable(onClick = onClick, enabled = enabled)
            .defaultMinSize(minHeight = minHeight)
    } else {
        modifier
            .defaultMinSize(minHeight = minHeight)
    }
    Row (
        modifier = rowModifier
            .padding(horizontal = 24.dp),
        // verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconResId != null) {
            Icon(
                modifier = Modifier
                    .height(minHeight.coerceAtLeast(80.dp))
                    .width(80.dp),
                painter = painterResource(id = iconResId),
                contentDescription = null,
                tint = if (enabled) Color.White else disabledTextColor
            )
            Spacer(modifier = Modifier.size(24.dp))
        }
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = minHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(top = 21.dp, bottom = 15.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = title, color = if (enabled) Color.White else disabledTextColor)
                if (text != null) {
                    Spacer(modifier = Modifier.size(15.dp))
                    Text(
                        text = text,
                        color = if (enabled) colorResource(id = R.color.secondary_text_color) else disabledTextColor
                    )
                }
            }
            if (onClick != null) {
                Spacer(modifier = Modifier.size(20.dp))
                Icon(
                    modifier = Modifier
                        .size(55.dp),
                    painter = painterResource(id = R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = if (enabled) Color.White else disabledTextColor
                )
            }
        }
    }
}