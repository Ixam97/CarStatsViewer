package com.ixam97.carStatsViewer.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.compose.components.CarIconButton
import com.ixam97.carStatsViewer.compose.components.CarSwitchRow
import com.ixam97.carStatsViewer.compose.theme.ComposeTestTheme
import com.ixam97.carStatsViewer.compose.theme.Typography
import com.ixam97.carStatsViewer.compose.theme.darkBackground
import com.ixam97.carStatsViewer.compose.theme.headerBackground
import com.ixam97.carStatsViewer.utils.InAppLogger

class ComposeActivity: ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)

        val brand = CarStatsViewer.dataProcessor.staticVehicleData.vehicleMake

        InAppLogger.d("brand: $brand")

        setContent {

            ComposeTestTheme(brand) {

                val composeViewModel: ComposeViewModel = viewModel()

                val composeActivityState by composeViewModel.composeActivityState.collectAsState()

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
                            CarIconButton(onCLick = { finish() }, iconResId = R.drawable.ic_arrow_back)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "Hello World!", style = Typography.h1)
                        }
                        Divider(
                            modifier = Modifier.height(3.dp),
                            color = MaterialTheme.colors.secondary
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                        ) {
                            CarSwitchRow(
                                switchState = composeActivityState.switchStates[0],
                                onClick = { composeViewModel.setSwitch(0, !composeActivityState.switchStates[0]) }
                            ) {
                                Text(text = "Row 1")
                            }
                            CarSwitchRow(
                                switchState = composeActivityState.switchStates[1],
                                onClick = { composeViewModel.setSwitch(1, !composeActivityState.switchStates[1]) }
                            ) {
                                Text(text = "Row 2")
                            }
                            CarSwitchRow(
                                switchState = composeActivityState.switchStates[2],
                                onClick = { composeViewModel.setSwitch(2, !composeActivityState.switchStates[2]) }
                            ) {
                                Text(text = "Row 3")
                            }
                        }
                    }
                }
            }
        }
    }
}