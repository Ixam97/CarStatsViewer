package com.ixam97.carStatsViewer.compose.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.MaterialTheme.typography
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.compose.ComposeViewModel
import com.ixam97.carStatsViewer.compose.components.CarIconButton
import com.ixam97.carStatsViewer.compose.components.CarSwitchRow
import com.ixam97.carStatsViewer.compose.theme.darkBackground
import com.ixam97.carStatsViewer.compose.theme.headerBackground
import com.ixam97.carStatsViewer.compose.theme.themedBrands

@Composable
fun HelloWorldScreen(viewModel: ComposeViewModel) {

    val composeActivityState by viewModel.composeActivityState.collectAsState()

    Box(modifier = Modifier
        .fillMaxSize()
        .padding(15.dp)
        .clip(RoundedCornerShape(10.dp))
        .background(color = darkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = headerBackground),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CarIconButton(
                    onCLick = { viewModel.finishActivity() },
                    iconResId = R.drawable.ic_arrow_back,
                    tint = MaterialTheme.colors.secondary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = "Hello World!", style = typography.h1)
            }
            Box(
                modifier = Modifier
                    .height(3.dp)
                    .fillMaxWidth()
                    .background(
                        brush = if (themedBrands.contains(viewModel.vehicleBrand))
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colors.secondary,
                                    MaterialTheme.colors.secondary
                                )
                            ) else
                            Brush.horizontalGradient(
                                colors = listOf(
                                    colorResource(id = R.color.club_violet_dark),
                                    colorResource(id = R.color.club_violet),
                                    colorResource(id = R.color.club_blue),
                                    colorResource(id = R.color.club_blue_dark)
                                )
                            )
                    ),
                // color = MaterialTheme.colors.secondary
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                CarSwitchRow(
                    switchState = composeActivityState.switchStates[0],
                    onClick = { viewModel.setSwitch(0, !composeActivityState.switchStates[0]) }
                ) {
                    Text(text = "Lorem Ipsum istilani")
                }
                CarSwitchRow(
                    switchState = composeActivityState.switchStates[1],
                    onClick = { viewModel.setSwitch(1, !composeActivityState.switchStates[1]) }
                ) {
                    Text(text = "Row 2")
                }
                CarSwitchRow(
                    switchState = composeActivityState.switchStates[2],
                    onClick = { viewModel.setSwitch(2, !composeActivityState.switchStates[2]) }
                ) {
                    Text(text = "Row 3")
                }
                if (viewModel.vehicleBrand == "Polestar") {
                    Row(
                        modifier = Modifier
                            .height(100.dp)
                            .clickable { viewModel.increaseScreenIndex() }
                            .padding(horizontal = 30.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Open Polestar UI")
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(painterResource(id = R.drawable.ic_chevron_right), contentDescription = null, tint = MaterialTheme.colors.onSurface)
                    }
                }
            }
        }
    }
}