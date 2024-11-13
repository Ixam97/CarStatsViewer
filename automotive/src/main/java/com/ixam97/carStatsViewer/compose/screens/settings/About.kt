package com.ixam97.carStatsViewer.compose.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ixam97.carStatsViewer.BuildConfig
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.compose.SettingsViewModel
import com.ixam97.carStatsViewer.compose.components.CarGradientButton
import com.ixam97.carStatsViewer.compose.components.CarRow
import com.ixam97.carStatsViewer.compose.screens.SettingsScreens

@Composable
fun About(
    navController: NavController,
    viewModel: SettingsViewModel
) = Column (
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {

    val context = LocalContext.current

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
        onClick = { viewModel.versionClick(context) },
        // text = "${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})",
        customContent = {
            Text("${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})")
            CarGradientButton(
                modifier = Modifier.padding(top = 15.dp).wrapContentSize(),
                onClick = { navController.navigate(SettingsScreens.ABOUT_CHANGELOG) }
            ) {
                Text(text = "Changelog")
            }
        }
    )
    Divider(modifier = Modifier.padding(horizontal = 24.dp))
    CarRow(
        title = "Copyright",
        text = "©2022-2024 Maximilian Goldschmidt"
    )
    Divider(modifier = Modifier.padding(horizontal = 24.dp))
    CarRow(
        title = stringResource(id = R.string.about_support),
        text = "${stringResource(id = R.string.about_supporters_message)} ${stringResource(R.string.about_support_description)}",
        browsable = true,
        external = true,
        onClick = { viewModel.openGitHubLink(context) }
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
        title = "GitHub Issues",
        text = stringResource(id = R.string.about_github_issues_description),
        browsable = true,
        external = true,
        onClick = { viewModel.openGitHubIssuesLink(context) }
    )
    Divider(modifier = Modifier.padding(horizontal = 24.dp))
    CarRow(
        title = "Polestar Club",
        text = stringResource(id = R.string.about_polestar_fans_description),
        browsable = true,
        external = true,
        onClick = { viewModel.openClubLink(context) }
    )
    Divider(modifier = Modifier.padding(horizontal = 24.dp))
    CarRow(
        title = "Polestar Forum",
        text = stringResource(id = R.string.about_polestar_forum_description),
        browsable = true,
        external = true,
        onClick = { viewModel.openForumsLink(context) }
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
        onClick = { navController.navigate(SettingsScreens.ABOUT_LICENSES) }
    )
    Divider(modifier = Modifier.padding(horizontal = 24.dp))
    CarRow(
        title = "Privacy Policy",
        browsable = true,
        external = true,
        onClick = { viewModel.openPrivacyLink(context) }
    )
}