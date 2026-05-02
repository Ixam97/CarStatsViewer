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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.ixam97.carStatsViewer.carCompose.screens.main.CarComposeMainScreen
import com.ixam97.carStatsViewer.carCompose.screens.main.MainScreenNavKey
import com.ixam97.carStatsViewer.carCompose.screens.settings.settingsEntryBuilder
import com.ixam97.carStatsViewer.carCompose.screens.tripDetails.tripDetailsNavEntryBuilder
import com.ixam97.carStatsViewer.carCompose.screens.tripHistory.tripHistoryNavEntryBuilder
import com.ixam97.carStatsViewer.utils.InAppLogger
import de.ixam97.carcompose.theme.CarTheme
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

@Composable
fun CarComposeNavigationRoot(
    globalViewModel: CarComposeGlobalViewModel,
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit,
    debugOnClose: (() -> Unit)? = null
) {

    val globalState by globalViewModel.globalState.collectAsState()

    InAppLogger.w("Recomposition of navigation root!")

    NavDisplay(
        backStack = backStack,
        onBack = onBack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator { true },
            rememberSharedViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<ProxyScreenNavKey>(
                metadata = NavDisplay.transitionSpec { EnterTransition.None togetherWith ExitTransition.None }
                + NavDisplay.popTransitionSpec { EnterTransition.None togetherWith ExitTransition.None }
                + NavDisplay.predictivePopTransitionSpec { EnterTransition.None togetherWith ExitTransition.None }
            ) { key ->
                ProxyScreen(key.proxyNavKeys, backStack)
            }

            entry<MainScreenNavKey> { key ->
                CarComposeMainScreen(
                    initialTabKey = key.selectedTabKey,
                    globalViewModel = globalViewModel,
                    backStack = backStack,
                    debugOnClose = debugOnClose
                )
            }

            settingsEntryBuilder(backStack, onBack, globalViewModel)
            tripDetailsNavEntryBuilder(backStack, onBack, globalViewModel)
            tripHistoryNavEntryBuilder(backStack, onBack, globalViewModel)
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