package com.ixam97.carStatsViewer.compose.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ixam97.carStatsViewer.R

@Composable
fun CarIconButton(onCLick: () -> Unit, @DrawableRes iconResId: Int, tint: Color = Color.White) {
    IconButton(
        modifier = Modifier
            .padding(10.dp)
            .size(70.dp),
        onClick = onCLick
    ) {
        Icon(
            painterResource(id = iconResId),
            tint = tint,
            contentDescription = null
        )
    }
}