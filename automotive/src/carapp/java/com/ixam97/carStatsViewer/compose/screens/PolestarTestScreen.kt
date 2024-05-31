package com.ixam97.carStatsViewer.compose.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.compose.ComposeViewModel
import com.ixam97.carStatsViewer.compose.components.Polestar2Header
import com.ixam97.carStatsViewer.compose.components.Polestar2Radiobutton
import com.ixam97.carStatsViewer.compose.components.Polestar2Row
import com.ixam97.carStatsViewer.compose.components.Polestar2Switch
import com.ixam97.carStatsViewer.compose.theme.disabledTextColor

@Composable
fun PolestarTestScreen(viewModel: ComposeViewModel) {

    val composeActivityState by viewModel.composeActivityState.collectAsState()

    Box(modifier = Modifier
        .fillMaxSize()
        // .padding(15.dp)
        // .clip(RoundedCornerShape(10.dp))
        // .background(color = darkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Polestar2Header {
                viewModel.finishActivity()
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Polestar2Switch(
                    switchState = composeActivityState.switchStates[2],
                    title = "Lorem ipsum",
                    text = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam",
                    onClick = {viewModel.setSwitch(2, !composeActivityState.switchStates[2])}
                )
                Divider(
                    modifier = Modifier
                        .height(2.dp)
                        .padding(horizontal = 23.dp)
                )
                Polestar2Switch(
                    switchState = composeActivityState.switchStates[0],
                    enabled = false,
                    title = "Lorem ipsum",
                    iconResId = R.drawable.ic_debug,
                    onClick = {viewModel.setSwitch(0, !composeActivityState.switchStates[0])}
                )
                Divider(
                    modifier = Modifier
                        .height(2.dp)
                        .padding(horizontal = 23.dp)
                )
                Polestar2Radiobutton(
                    title = "Radio Button",
                    selected = composeActivityState.switchStates[2],
                    onClick = {
                        viewModel.setSwitch(2, !composeActivityState.switchStates[2])
                    }
                )
                Divider(
                    modifier = Modifier
                        .height(2.dp)
                        .padding(horizontal = 23.dp)
                )
                Text(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .padding(top = 25.dp, bottom = 15.dp),
                    text = "Divider Text",
                    fontWeight = FontWeight.Medium,
                    color = disabledTextColor
                )
                Divider(
                    modifier = Modifier
                        .height(2.dp)
                        .padding(horizontal = 23.dp)
                )
                Polestar2Radiobutton(
                    title = "Radio Button",
                    text = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam",
                    selected = composeActivityState.switchStates[1],
                    iconResId = R.drawable.ic_debug,
                    onClick = {
                        viewModel.setSwitch(1, !composeActivityState.switchStates[1])
                    }
                )
                Divider(
                    modifier = Modifier
                        .height(2.dp)
                        .padding(horizontal = 23.dp)
                )
                Polestar2Radiobutton(
                    title = "Radio Button",
                    enabled = false,
                    selected = composeActivityState.switchStates[2],
                    onClick = {
                        viewModel.setSwitch(2, !composeActivityState.switchStates[2])
                    }
                )
                Divider(
                    modifier = Modifier
                        .height(2.dp)
                        .padding(horizontal = 23.dp)
                )
                Polestar2Row(
                    title = "Lorem ipsum",
                    onClick = { }
                )
                Divider(
                    modifier = Modifier
                        .height(2.dp)
                        .padding(horizontal = 23.dp)
                )
                Polestar2Row(
                    title = "Lorem ipsum",
                    text = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam",
                    enabled = false,
                    iconResId = R.drawable.ic_debug,
                    onClick = { }
                )
            }
        }
    }
}