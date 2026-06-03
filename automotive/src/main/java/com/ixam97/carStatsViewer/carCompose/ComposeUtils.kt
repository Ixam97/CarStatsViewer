package com.ixam97.carStatsViewer.carCompose

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowDpSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun deviceIsWideScreen(threshold: Dp = 1500.dp) : Boolean  {
    val windowDpSize = currentWindowDpSize()

    return windowDpSize.width > threshold
}

@Composable
fun onInitialComposition(
    action: () -> Unit
) {
    val isInitialComposition = remember { true }
    key(isInitialComposition) {
        action()
    }
}