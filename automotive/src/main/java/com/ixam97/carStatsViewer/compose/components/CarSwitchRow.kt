package com.ixam97.carStatsViewer.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun CarSwitchRow(
    switchState: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .height(100.dp)
            .clickable { onClick() }
            .padding(horizontal = 30.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
        Spacer(modifier = Modifier.weight(1f))
        Switch(
            modifier = Modifier
                // .scale(2f)
                .graphicsLayer(
                    transformOrigin = TransformOrigin(1f,.5f),
                    scaleX = 2f,
                    scaleY = 2f),
            checked = switchState,
            onCheckedChange = null
        )
    }
}