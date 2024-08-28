package com.ixam97.carStatsViewer.compose.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun CarIconButton(modifier: Modifier = Modifier, onClick: () -> Unit, @DrawableRes iconResId: Int, tint: Color = Color.White) {
    IconButton(
        modifier = modifier
            .padding(10.dp)
            .size(70.dp),
        onClick = onClick
    ) {
        Icon(
            painterResource(id = iconResId),
            tint = tint,
            contentDescription = null
        )
    }
}