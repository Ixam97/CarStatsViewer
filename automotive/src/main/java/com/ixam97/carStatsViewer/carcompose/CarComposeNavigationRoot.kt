package com.ixam97.carStatsViewer.carcompose

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.ixam97.carStatsViewer.carcompose.screen.CarComposeMainScreen
import com.ixam97.carStatsViewer.carcompose.screen.settings.CarComposeSettingsScreen
import com.ixam97.carStatsViewer.carcompose.screen.MainScreenNavKey
import com.ixam97.carStatsViewer.carcompose.screen.settings.AboutScreenNavKey
import com.ixam97.carStatsViewer.carcompose.screen.settings.AppearanceScreenNavKey
import com.ixam97.carStatsViewer.carcompose.screen.settings.CarComposeAboutScreen
import com.ixam97.carStatsViewer.carcompose.screen.settings.CarComposeAppearanceScreen
import com.ixam97.carStatsViewer.carcompose.screen.settings.CarComposeChangelogScreen
import com.ixam97.carStatsViewer.carcompose.screen.settings.CarComposeLicensesScreen
import com.ixam97.carStatsViewer.carcompose.screen.settings.ChangelogScreenNavKey
import com.ixam97.carStatsViewer.carcompose.screen.settings.LicensesScreenNavKey
import com.ixam97.carStatsViewer.carcompose.screen.settings.SettingsScreenNavKey

@Composable
fun CarComposeNavigationRoot(
    globalViewModel: CarComposeViewModel,
    backStack: NavBackStack<NavKey>,
    debugOnClose: (() -> Unit)? = null
) {


    NavDisplay(
        backStack = backStack,
        entryProvider = entryProvider {
            entry<MainScreenNavKey> {
                CarComposeMainScreen(
                    globalViewModel,
                    backStack,
                    debugOnClose
                )
            }

            // Settings
            entry<SettingsScreenNavKey> {
                CarComposeSettingsScreen(
                    globalViewModel,
                    backStack
                )
            }
            entry<AppearanceScreenNavKey> {
                CarComposeAppearanceScreen(
                    globalViewModel,
                    backStack
                )
            }
            entry<AboutScreenNavKey> {
                CarComposeAboutScreen(
                    globalViewModel,
                    backStack
                )
            }
            entry<ChangelogScreenNavKey> {
                CarComposeChangelogScreen(
                    globalViewModel,
                    backStack
                )
            }
            entry<LicensesScreenNavKey> {
                CarComposeLicensesScreen(
                    globalViewModel,
                    backStack
                )
            }

            //
        },
        transitionSpec = {
            // Slide in from right when navigating forward
            slideInHorizontally(initialOffsetX = { it }) togetherWith
                    slideOutHorizontally(targetOffsetX = { -it })
        },
        popTransitionSpec = {
            // Slide in from left when navigating back
            slideInHorizontally(initialOffsetX = { -it }) togetherWith
                    slideOutHorizontally(targetOffsetX = { it })
        },
        predictivePopTransitionSpec = {
            // Slide in from left when navigating back
            slideInHorizontally(initialOffsetX = { -it }) togetherWith
                    slideOutHorizontally(targetOffsetX = { it })
        },
    )
}