package com.ixam97.carStatsViewer.compose.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
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
fun CarRow(
    modifier: Modifier = Modifier,
    title: String,
    text: String? = null,
    customContent: ( @Composable () -> Unit)? = null,
    leadingContent: ( @Composable () -> Unit)? = null,
    trailingContent: ( @Composable () -> Unit)? = null,
    @DrawableRes iconResId:  Int? = null,
    browsable: Boolean = false,
    external: Boolean = false,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    minHeight: Dp = 100.dp
) {

    val rowModifier = if (onClick != null) {
        modifier
            .clickable(onClick = onClick, enabled = enabled)
            .defaultMinSize(minHeight = minHeight)
            .height(IntrinsicSize.Max)
    } else {
        modifier
            .defaultMinSize(minHeight = minHeight)
            .height(IntrinsicSize.Max)
    }
    Row (
        modifier = rowModifier
            .padding(horizontal = 24.dp)
            // .then(modifier),
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
                .defaultMinSize(minHeight = minHeight)
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(top = 21.dp, bottom = 15.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = title, color = MaterialTheme.colors.onBackground.copy(alpha = if (enabled) 1f else 0.46f))
                if (text != null && customContent == null) {
                    Spacer(modifier = Modifier.size(15.dp))
                    Text(
                        text = text,
                        color = if (enabled) colorResource(id = R.color.secondary_text_color) else disabledTextColor
                    )
                } else if (customContent != null) {
                    Spacer(modifier = Modifier.size(15.dp))
                    customContent()
                }
            }
            if (onClick != null && browsable) {
                Spacer(modifier = Modifier.size(20.dp))
                Icon(
                    modifier = Modifier
                        .size(55.dp),
                    painter = painterResource(
                        id = if (!external) R.drawable.ic_chevron_right else R.drawable.ic_arrow_diagonal
                    ),
                    contentDescription = null,
                    tint = if (enabled) Color.White else disabledTextColor
                )
            }

            if (trailingContent != null) {
                Spacer(modifier = Modifier.size(20.dp))
                trailingContent()
            }
        }
    }
}