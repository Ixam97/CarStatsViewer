package com.ixam97.carStatsViewer.compose.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.compose.SettingsViewModel
import com.ixam97.carStatsViewer.compose.components.CarRow
import com.ixam97.carStatsViewer.compose.components.CarSwitchRow
import com.ixam97.carStatsViewer.compose.screens.SettingsScreens

@Composable
fun ApiSettings(
    viewModel: SettingsViewModel,
    navController: NavController
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 10.dp),
            color = MaterialTheme.colors.primary,
            text = "External APIs"
        )
        Divider(modifier = Modifier.padding(horizontal = 24.dp))
        CarRow(
            title = stringResource(R.string.settings_apis_abrp),
            // iconResId = R.mipmap.ic_abrp,
            browsable = true,
            onClick = {
                navController.navigate(SettingsScreens.APIS_ABRP)
            },
            leadingContent = {
                Image(
                    modifier = Modifier
                        .width(80.dp)
                        .fillMaxHeight(),
                    painter = painterResource(R.mipmap.ic_abrp),
                    contentDescription = null
                )
            }
        )
        Divider(Modifier.padding(horizontal = 20.dp))
        CarRow(
            title = stringResource(R.string.settings_apis_http),
            iconResId = R.drawable.ic_webhook,
            browsable = true,
            onClick = {
                navController.navigate(SettingsScreens.APIS_HTTP)
            }
        )
        Divider(Modifier.padding(horizontal = 24.dp))
        Text(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 10.dp),
            color = MaterialTheme.colors.primary,
            text = "Trip export"
        )
        Divider(Modifier.padding(horizontal = 24.dp))
        CarRow(
            title = "Trip exports send a request to the developer's server containing your trip data. This data will then be sent to your specified address as a CSV file. The trip data is then immediately deleted from the developer's server."
        )
        Divider(Modifier.padding(horizontal = 24.dp))
        CarSwitchRow(
            switchState = false,
            onClick = { },
        ) {
            Text("Enable trip export")
        }
        Divider(Modifier.padding(horizontal = 20.dp))
        CarRow(
            title = "Trip export Email address",
            customContent = {
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = "",
                    onValueChange = { newValue -> },
                )
            }
        )
    }
}