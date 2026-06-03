package com.ixam97.carStatsViewer.carCompose.screens.tripHistory

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.ixam97.carStatsViewer.carCompose.CarComposeGlobalViewModel
import com.ixam97.carStatsViewer.carCompose.LocalSharedViewModelStoreOwner
import com.ixam97.carStatsViewer.carCompose.SharedViewModelStoreNavEntryDecorator
import com.ixam97.carStatsViewer.carCompose.screens.settings.SettingsScreenNavKey
import com.ixam97.carStatsViewer.carCompose.toContentKey

fun EntryProviderScope<NavKey>.tripHistoryNavEntryBuilder(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit,
    globalViewModel: CarComposeGlobalViewModel
) {
    entry<TripHistoryScreenNavKey>(
        clazzContentKey = { key -> key.toContentKey()}
    ) { TripHistoryScreen(backStack, onBack, globalViewModel) }
    entry<TripHistoryFiltersScreenNavKey>(
        metadata = SharedViewModelStoreNavEntryDecorator.parents(
            TripHistoryScreenNavKey.toContentKey(),
            SettingsScreenNavKey.toContentKey()
        )
    ) {
        val parentViewModel = viewModel(
            modelClass = TripHistoryViewModel::class,
            viewModelStoreOwner = LocalSharedViewModelStoreOwner.current
        )
        TripHistoryFiltersScreen(backStack, onBack, parentViewModel)
    }
}