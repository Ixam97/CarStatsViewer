package com.ixam97.carStatsViewer.carCompose.screens.settings

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.ixam97.carStatsViewer.carCompose.CarComposeGlobalViewModel

interface MainSettingsNavKey: NavKey

fun EntryProviderScope<NavKey>.settingsEntryBuilder(
    backStack: NavBackStack<NavKey>,
    globalViewModel: CarComposeGlobalViewModel
) {
    entry<SettingsScreenNavKey> { SettingsScreen(backStack, globalViewModel) }
    entry<SettingsGeneralScreenNavKey> { SettingsGeneralScreen(backStack, globalViewModel) }
    entry<SettingsAppearanceScreenNavKey> { SettingsAppearanceScreen(backStack, globalViewModel) }
    entry<SettingsLocationScreenNavKey> { SettingsPrivacyScreen(backStack, globalViewModel) }
    entry<SettingsApisNavKey> { SettingsApisScreen(backStack) }
    entry<SettingsAboutScreenNavKey> { SettingsAboutScreen(backStack, globalViewModel) }
    entry<SettingsChangelogScreenNavKey> { SettingsChangelogScreen(backStack) }
    entry<SettingsLicensesScreenNavKey> { SettingsLicensesScreen(backStack) }
    entry<SettingsDevScreenNavKey> { SettingsDevScreen(backStack, globalViewModel) }
}