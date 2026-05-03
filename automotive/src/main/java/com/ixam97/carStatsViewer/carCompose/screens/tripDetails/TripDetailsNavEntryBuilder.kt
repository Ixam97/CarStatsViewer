package com.ixam97.carStatsViewer.carCompose.screens.tripDetails

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.ixam97.carStatsViewer.carCompose.CarComposeGlobalViewModel

fun EntryProviderScope<NavKey>.tripDetailsNavEntryBuilder(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit,
    globalViewModel: CarComposeGlobalViewModel
) {
    entry<TripDetailsScreenNavKey> { key ->
        TripDetailsScreen(
            globalViewModel = globalViewModel,
            onBackClick = onBack,
            sessionId = key.sessionId
        )
    }
}