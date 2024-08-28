package com.ixam97.carStatsViewer.compose.screens.settingsScreens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ixam97.carStatsViewer.BuildConfig
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.compose.components.CarGradientButton
import com.ixam97.carStatsViewer.compose.components.CarRow
import com.ixam97.carStatsViewer.compose.components.SideTab
import com.ixam97.carStatsViewer.compose.screens.SettingsScreens
import kotlinx.serialization.Serializable

@Composable
fun AboutScreen(
    navController: NavController
) = Column (
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {

    @Composable
    fun contributorsString(): String {
        var contributorsString = ""
        stringArrayResource(id = R.array.contributors).forEachIndexed { index, contributor ->
            if (index > 0) contributorsString += ", "
            contributorsString += contributor
        }
        return contributorsString
    }

    Text(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .padding(top = 24.dp, bottom = 10.dp),
        color = MaterialTheme.colors.primary,
        text = stringResource(id = R.string.about_section_about)
    )
    Divider(modifier = Modifier.padding(horizontal = 24.dp))
    CarRow(
        title = "Version",
        onClick = { /* enable dev mode */ },
        text = "${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})"
    )

    CarGradientButton(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
        onClick = { navController.navigate(SettingsScreens.ABOUT_CHANGELOG) }
    ) {
        Text(text = "Changelog")
    }
    Divider(modifier = Modifier.padding(horizontal = 24.dp))
    CarRow(
        title = "Copyright",
        text = "©2022-2024 Maximilian Goldschmidt"
    )
    Divider(modifier = Modifier.padding(horizontal = 24.dp))
    CarRow(
        title = stringResource(id = R.string.about_support),
        text = stringResource(id = R.string.about_supporters_message),
        browsable = true,
        external = true,
        onClick = { }
    )
    Divider(modifier = Modifier.padding(horizontal = 24.dp))
    CarRow(
        title = stringResource(id = R.string.about_contributors),
        text = contributorsString()
    )
    Divider(modifier = Modifier.padding(horizontal = 24.dp))
    Text(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .padding(top = 24.dp, bottom = 10.dp),
        color = MaterialTheme.colors.primary,
        text = stringResource(id = R.string.about_section_feedback)
    )
    Divider(modifier = Modifier.padding(horizontal = 24.dp))
    CarRow(
        title = "Polestar Club",
        text = stringResource(id = R.string.about_polestar_fans_description),
        browsable = true,
        external = true,
        onClick = { }
    )
    Divider(modifier = Modifier.padding(horizontal = 24.dp))
    CarRow(
        title = "Polestar Forum",
        text = stringResource(id = R.string.about_polestar_forum_description),
        browsable = true,
        external = true,
        onClick = { }
    )
    Divider(modifier = Modifier.padding(horizontal = 24.dp))
    Text(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .padding(top = 24.dp, bottom = 10.dp),
        color = MaterialTheme.colors.primary,
        text = stringResource(id = R.string.about_section_misc)
    )
    Divider(modifier = Modifier.padding(horizontal = 24.dp))
    CarRow(
        title = stringResource(id = R.string.about_third_party_licenses),
        browsable = true,
        onClick = {  }
    )
    Divider(modifier = Modifier.padding(horizontal = 24.dp))
    CarRow(
        title = "Privacy Policy",
        browsable = true,
        external = true,
        onClick = {  }
    )
}