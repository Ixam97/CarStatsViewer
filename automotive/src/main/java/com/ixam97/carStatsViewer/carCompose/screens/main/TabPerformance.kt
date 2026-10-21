package com.ixam97.carStatsViewer.carCompose.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.ixam97.carcompose.theme.CarTheme
import kotlin.math.absoluteValue

@Composable
fun TabPerformance() {
    // Placeholder("Placeholder for real time performance data.")

    val performanceViewModel: PerformanceViewModel = viewModel()

    Row {
        RealTimeDataBar(
            text = "${(performanceViewModel.performanceState.power / 1000000).toInt()} kW",
            fraction = (performanceViewModel.performanceState.power / 400000000f)
        )
        RealTimeDataBar(
            text= "Wh/km",
            fraction = 0f
        )
        RealTimeDataBar(
            text= "${(performanceViewModel.performanceState.speed * 3.6).toInt()} km/h",
            fraction = ((performanceViewModel.performanceState.speed * 3.6f) / 200f)
        )
    }
}

@Composable
private fun RowScope.RealTimeDataBar(
    text: String,
    fraction: Float
) {
    BoxWithConstraints(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .padding(CarTheme.carDimensions.defaultHorizontalPadding)
            .background(CarTheme.carColors.secondaryDivider)
            .padding(2.dp)
            .background(CarTheme.carColors.background),
        contentAlignment = Alignment.BottomEnd
    ) {
        val maxHeight = this.maxHeight
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        if (fraction < 0) maxHeight * fraction.absoluteValue
                        else 0.dp
                    )
                    .background(Color.LightGray)
            )
            Box(
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        if (fraction > 0) maxHeight * fraction
                        else 0.dp
                    )
                    .background(CarTheme.carColors.accentContainer)
            )
        }

        Text(
            modifier = Modifier.padding(end = 20.dp),
            text = text,
            style = CarTheme.carTypography.title,
            color = CarTheme.carColors.onBackground,
            fontSize = 80.sp
        )
    }
}