package com.ixam97.carStatsViewer.carCompose.screens.settings

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.ixam97.carStatsViewer.carCompose.CarComposeGlobalViewModel

interface MainSettingsNavKey: NavKey

fun EntryProviderScope<NavKey>.settingsEntryBuilder(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit,
    globalViewModel: CarComposeGlobalViewModel
) {
    entry<SettingsScreenNavKey> { SettingsScreen(backStack, onBack, globalViewModel) }
    entry<SettingsGeneralScreenNavKey> { SettingsGeneralScreen(backStack, onBack, globalViewModel) }
    entry<SettingsAppearanceScreenNavKey> { SettingsAppearanceScreen(backStack, onBack, globalViewModel) }
    entry<SettingsLocationScreenNavKey> { SettingsPrivacyScreen(backStack,onBack, globalViewModel) }
    entry<SettingsApisNavKey> { SettingsApisScreen(backStack, onBack) }
    entry<SettingsAboutScreenNavKey> { SettingsAboutScreen(backStack, onBack, globalViewModel) }
    entry<SettingsChangelogScreenNavKey> { SettingsChangelogScreen(backStack, onBack) }
    entry<SettingsLicensesScreenNavKey> { SettingsLicensesScreen(backStack, onBack) }
    entry<SettingsDevScreenNavKey> { SettingsDevScreen(backStack, onBack, globalViewModel) }
}