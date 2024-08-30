package com.ixam97.carStatsViewer.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
    onClick: (newState: Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .heightIn(min = 100.dp)
            .clickable { onClick(!switchState) }
            .padding(horizontal = 30.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
        Spacer(modifier = Modifier.weight(1f).widthIn(min = 20.dp))
        Box(
            modifier = Modifier.size(80.dp),
            contentAlignment = Alignment.Center
        ) {
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
}