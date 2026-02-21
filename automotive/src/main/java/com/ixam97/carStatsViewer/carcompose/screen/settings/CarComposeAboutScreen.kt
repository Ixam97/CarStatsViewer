package com.ixam97.carStatsViewer.carcompose.screen.settings

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.ixam97.carStatsViewer.BuildConfig
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carcompose.CarComposeViewModel
import com.ixam97.carStatsViewer.carcompose.VehicleModel
import com.ixam97.carStatsViewer.carcompose.theme.polestar4ContentPadding
import de.ixam97.carcompose.components.controls.CarButton
import de.ixam97.carcompose.components.controls.CarIconButton
import de.ixam97.carcompose.components.controls.CarRow
import de.ixam97.carcompose.components.controls.CarRowBrowsableType
import de.ixam97.carcompose.components.layout.CarColumn
import de.ixam97.carcompose.components.layout.CarListDivider
import de.ixam97.carcompose.components.layout.CarListSectionTitle
import de.ixam97.carcompose.components.layout.CarPaneLayout
import de.ixam97.carcompose.theme.CarTheme
import kotlinx.serialization.Serializable

@Serializable
object AboutScreenNavKey: NavKey

@Composable
fun CarComposeAboutScreen(
    globalViewModel: CarComposeViewModel,
    backStack: NavBackStack<NavKey>
) {
    CarPaneLayout(
        headerTitle = stringResource(R.string.about_title),
        headerStartContent = {
            CarIconButton(
                painter = painterResource(R.drawable.ic_arrow_backwards_48),
                tint = CarTheme.carColors.accent,
                onClick = { backStack.removeAt(backStack.lastIndex) }
            )
        }
    ) {
        CarComposeAboutContent(
            modifier = Modifier.padding(start = if (globalViewModel.carComposeState.vehicleModel == VehicleModel.Polestar4) polestar4ContentPadding else 0.dp) ,
            globalViewModel = globalViewModel,
            backStack = backStack
        )
    }
}

@Composable
fun CarComposeAboutContent(
    modifier: Modifier = Modifier,
    globalViewModel: CarComposeViewModel,
    backStack: NavBackStack<NavKey>
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

    CarColumn(
        modifier = modifier
    ) {
        CarListSectionTitle(stringResource(R.string.about_section_about))
        CarRow(
            title = "Version",
            description = "${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})",
            trailingContent = {
                CarButton(
                    modifier = Modifier
                        .widthIn(min = CarTheme.carDimensions.buttonMinWidth * 2)
                        .height(CarTheme.carDimensions.buttonMinHeight),
                    onClick = {  backStack.add(ChangelogScreenNavKey) }
                ) { Text(stringResource(R.string.settings_changelog)) }
            },
            browsable = true,
            browsableType = CarRowBrowsableType.Hidden
        )
        CarListDivider()
        CarRow(
            title = "Copyright",
            description = "©2022-2024 Maximilian Goldschmidt"
        )
        CarListDivider()
        CarRow(
            title = stringResource(R.string.about_support),
            description = "${stringResource(id = R.string.about_supporters_message)} ${stringResource(R.string.about_support_description)}",
            browsable = true,
            browsableType = CarRowBrowsableType.External
        )
        CarListDivider()
        CarRow(
            title = stringResource(R.string.about_contributors),
            description = contributorsString()
        )
        CarListDivider()

        CarListSectionTitle(stringResource(R.string.about_section_feedback))
        CarRow(
            title = "GitHub Issues",
            description = stringResource(R.string.about_github_issues_description),
            browsable = true,
            browsableType = CarRowBrowsableType.External
        )
        CarListDivider()
        CarRow(
            title = "Polestar Club",
            description = stringResource(R.string.about_polestar_fans_description),
            browsable = true,
            browsableType = CarRowBrowsableType.External
        )
        CarListDivider()
        CarRow(
            title = "Polestar Forum",
            description = stringResource(R.string.about_polestar_forum_description),
            browsable = true,
            browsableType = CarRowBrowsableType.External
        )
        CarListDivider()

        CarListSectionTitle(stringResource(R.string.about_section_misc))
        CarRow(
            title = stringResource(R.string.about_third_party_licenses),
            browsable = true,
            onBrowse = { backStack.add(LicensesScreenNavKey) }
        )
        CarListDivider()
        CarRow(
            title = stringResource(R.string.settings_privacy),
            browsable = true,
            browsableType = CarRowBrowsableType.External
        )
    }
}