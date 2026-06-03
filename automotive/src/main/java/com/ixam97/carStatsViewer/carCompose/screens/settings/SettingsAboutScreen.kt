package com.ixam97.carStatsViewer.carCompose.screens.settings

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.ixam97.carStatsViewer.BuildConfig
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carCompose.CarComposeGlobalViewModel
import com.ixam97.carStatsViewer.carCompose.deviceIsWideScreen
import com.ixam97.carStatsViewer.carCompose.theme.polestar4ContentPadding
import de.ixam97.carcompose.components.controls.CarButton
import de.ixam97.carcompose.components.controls.CarRow
import de.ixam97.carcompose.components.controls.CarRowBrowsableType
import de.ixam97.carcompose.components.layout.CarColumn
import de.ixam97.carcompose.components.layout.CarListItem
import de.ixam97.carcompose.components.layout.CarListSection
import de.ixam97.carcompose.components.layout.CarPaneLayout
import de.ixam97.carcompose.components.layout.CarSnackBarConfig
import de.ixam97.carcompose.components.layout.LocalCarSnackBarState
import de.ixam97.carcompose.theme.CarTheme
import kotlinx.serialization.Serializable

@Serializable
object SettingsAboutScreenNavKey: MainSettingsNavKey

@Composable
fun SettingsAboutScreen(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit,
    globalViewModel: CarComposeGlobalViewModel,
    viewModel: SettingsViewModel = viewModel()
) {
    CarPaneLayout(
        headerTitle = stringResource(R.string.about_title),
        onBackAction = onBack
    ) {
        SettingsAboutContent(
            modifier = Modifier.padding(start = if (deviceIsWideScreen()) polestar4ContentPadding else 0.dp),
            backStack = backStack,
            globalViewModel = globalViewModel,
            viewModel = viewModel,
        )
    }
}

@Composable
fun SettingsAboutContent(
    modifier: Modifier = Modifier,
    backStack: NavBackStack<NavKey>,
    globalViewModel: CarComposeGlobalViewModel,
    viewModel: SettingsViewModel,
) {
    val globalState by globalViewModel.globalState.collectAsState()
    var versionTapCounter by remember { mutableStateOf(0) }

    val context = LocalContext.current
    val snackBarState = LocalCarSnackBarState.current

    CarColumn(
        modifier = modifier
    ) {
        CarListSection(
            sectionTitle = stringResource(R.string.about_section_about),
            dividerAtBottom = true,
            listItems = listOf(
                CarListItem {
                    CarRow(
                        title = "Version",
                        description = "${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})",
                        trailingContent = {
                            CarButton(
                                modifier = Modifier
                                    .widthIn(min = CarTheme.carDimensions.buttonMinWidth * 2)
                                    .height(CarTheme.carDimensions.buttonMinHeight),
                                onClick = { backStack.add(SettingsChangelogScreenNavKey) }
                            ) { Text(stringResource(R.string.settings_changelog)) }
                        },
                        browsable = true,
                        browsableType = CarRowBrowsableType.Hidden,
                        onBrowse = {
                            if (!globalState.devModeEnabled) {
                                if (versionTapCounter < 7) versionTapCounter++
                                else {
                                    globalViewModel.setDevModeEnabled(true)
                                    snackBarState.showSnackBar(CarSnackBarConfig(
                                        identifier = "DevModeEnabledSnackBar",
                                        content = { Text("Developer Options enabled.") },
                                        drawableResId = R.drawable.ic_debug
                                    ))
                                }
                            }
                        }
                    )
                },
                CarListItem {
                    CarRow(
                        title = "Copyright",
                        description = "©2022-2026 Maximilian Goldschmidt"
                    )
                },
                CarListItem {
                    CarRow(
                        title = stringResource(R.string.about_support),
                        description = "${stringResource(id = R.string.about_supporters_message)} ${
                            stringResource(
                                R.string.about_support_description
                            )
                        }",
                        browsable = true,
                        onBrowse = { viewModel.openGitHubLink(context) },
                        browsableType = CarRowBrowsableType.External
                    )
                },
                CarListItem {
                    CarRow(
                        title = stringResource(R.string.about_contributors),
                        description = contributorsString()
                    )
                },
            )
        )

        CarListSection(
            sectionTitle = stringResource(R.string.about_section_feedback),
            dividerAtBottom = true,
            listItems = listOf(
                CarListItem {
                    CarRow(
                        title = "GitHub Issues",
                        description = stringResource(R.string.about_github_issues_description),
                        browsable = true,
                        onBrowse = { viewModel.openGitHubIssuesLink(context) },
                        browsableType = CarRowBrowsableType.External
                    )
                },
                CarListItem {
                    CarRow(
                        title = "Polestar Club",
                        description = stringResource(R.string.about_polestar_fans_description),
                        browsable = true,
                        onBrowse = { viewModel.openClubLink(context) },
                        browsableType = CarRowBrowsableType.External
                    )
                },
                CarListItem {
                    CarRow(
                        title = "Polestar Forum",
                        description = stringResource(R.string.about_polestar_forum_description),
                        browsable = true,
                        onBrowse = { viewModel.openForumsLink(context) },
                        browsableType = CarRowBrowsableType.External
                    )
                },
            )
        )

        CarListSection(
            sectionTitle = stringResource(R.string.about_section_misc),
            listItems = listOf(
                CarListItem {
                    CarRow(
                        title = stringResource(R.string.about_third_party_licenses),
                        browsable = true,
                        onBrowse = { backStack.add(SettingsLicensesScreenNavKey) }
                    )
                },
                CarListItem {
                    CarRow(
                        title = stringResource(R.string.settings_privacy),
                        browsable = true,
                        onBrowse = { viewModel.openPrivacyLink(context) },
                        browsableType = CarRowBrowsableType.External
                    )
                }
            )
        )
    }
}

@Composable
internal fun contributorsString(): String {
    var contributorsString = ""
    stringArrayResource(id = R.array.contributors).forEachIndexed { index, contributor ->
        if (index > 0) contributorsString += ", "
        contributorsString += contributor
    }
    return contributorsString
}