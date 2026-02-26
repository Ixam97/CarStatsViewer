package com.ixam97.carStatsViewer.carCompose

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.ixam97.carStatsViewer.carCompose.screens.main.CarComposeMainScreen
import com.ixam97.carStatsViewer.carCompose.screens.settings.SettingsScreen
import com.ixam97.carStatsViewer.carCompose.screens.main.MainScreenNavKey
import com.ixam97.carStatsViewer.carCompose.screens.settings.AboutScreenNavKey
import com.ixam97.carStatsViewer.carCompose.screens.settings.SettingsAppearanceScreenNavKey
import com.ixam97.carStatsViewer.carCompose.screens.settings.SettingsAboutScreen
import com.ixam97.carStatsViewer.carCompose.screens.settings.SettingsAppearanceScreen
import com.ixam97.carStatsViewer.carCompose.screens.settings.SettingsChangelogScreen
import com.ixam97.carStatsViewer.carCompose.screens.settings.CarComposeLicensesScreen
import com.ixam97.carStatsViewer.carCompose.screens.settings.SettingsApisScreen
import com.ixam97.carStatsViewer.carCompose.screens.settings.CarComposeSettingsGeneralScreen
import com.ixam97.carStatsViewer.carCompose.screens.settings.CarComposeSettingsLocationScreen
import com.ixam97.carStatsViewer.carCompose.screens.settings.SettingsChangelogScreenNavKey
import com.ixam97.carStatsViewer.carCompose.screens.settings.LicensesScreenNavKey
import com.ixam97.carStatsViewer.carCompose.screens.settings.SettingsApisNavKey
import com.ixam97.carStatsViewer.carCompose.screens.settings.SettingsGeneralScreenNavKey
import com.ixam97.carStatsViewer.carCompose.screens.settings.SettingsLocationScreenNavKey
import com.ixam97.carStatsViewer.carCompose.screens.settings.SettingsScreenNavKey
import com.ixam97.carStatsViewer.carCompose.screens.tripDetails.TripDetailsPortraitScreen
import com.ixam97.carStatsViewer.carCompose.screens.tripDetails.TripDetailsScreenNavKey
import com.ixam97.carStatsViewer.utils.InAppLogger
import de.ixam97.carcompose.theme.CarTheme
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

@Composable
fun CarComposeNavigationRoot(
    globalViewModel: CarComposeGlobalViewModel,
    backStack: NavBackStack<NavKey>,
    debugOnClose: (() -> Unit)? = null
) {

    InAppLogger.w("Recomposition of navigation root!")

    NavDisplay(
        backStack = backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator { true },
        ),
        entryProvider = entryProvider {
            entry<ProxyScreenNavKey>(
                metadata = NavDisplay.transitionSpec { EnterTransition.None togetherWith ExitTransition.None }
                + NavDisplay.popTransitionSpec { EnterTransition.None togetherWith ExitTransition.None }
                + NavDisplay.predictivePopTransitionSpec { EnterTransition.None togetherWith ExitTransition.None }
            ) { key ->
                ProxyScreen(key.proxyNavKeys, backStack)
            }

            entry<MainScreenNavKey>(
                metadata = NavDisplay.transitionSpec { EnterTransition.None togetherWith ExitTransition.None }
            ) { key ->
                CarComposeMainScreen(
                    initialTabKey = key.selectedTabKey,
                    globalViewModel = globalViewModel,
                    backStack = backStack,
                    debugOnClose = debugOnClose
                )
            }

            // Settings
            entry<SettingsScreenNavKey> {
                SettingsScreen(backStack, globalViewModel)
            }
            entry<SettingsGeneralScreenNavKey> {
                CarComposeSettingsGeneralScreen(
                    backStack,
                    globalViewModel
                )
            }
            entry<SettingsAppearanceScreenNavKey> {
                SettingsAppearanceScreen(
                    backStack,
                    globalViewModel
                )
            }
            entry<SettingsLocationScreenNavKey> {
                CarComposeSettingsLocationScreen(
                    globalViewModel,
                    backStack
                )
            }
            entry<SettingsApisNavKey> {
                SettingsApisScreen(
                    backStack
                )
            }
            entry<AboutScreenNavKey> {
                SettingsAboutScreen(
                    backStack,
                    globalViewModel
                )
            }
            entry<SettingsChangelogScreenNavKey> {
                SettingsChangelogScreen(
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

            // Trip History
            entry<TripDetailsScreenNavKey> { key ->
                TripDetailsPortraitScreen(
                    globalViewModel = globalViewModel,
                    onBackClick = { backStack.removeLastOrNull() },
                    sessionId = key.sessionId
                )
            }
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

@Serializable
data class ProxyScreenNavKey(
    val proxyNavKeys: List<NavKey>
): NavKey

@Composable
fun ProxyScreen(
    proxyNavKeys: List<NavKey>,
    backStack: NavBackStack<NavKey>
) {
    Box(Modifier
        .fillMaxSize()
        .background(CarTheme.carColors.background))
    LaunchedEffect(null) {
        delay(5)
        backStack.clear()
        backStack.addAll(proxyNavKeys)
    }
}