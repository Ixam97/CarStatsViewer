package com.ixam97.carStatsViewer.carCompose

import android.app.Activity
import androidx.compose.runtime.compositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlin.collections.removeLastOrNull

class CarComposeNavController(
    val backStack: NavBackStack<NavKey>,
    private val activityContext: Activity
) {

    fun navTo(navKey: NavKey) {
        backStack.add(navKey)
    }

    fun navBack() {
        if (backStack.size > 1) backStack.removeLastOrNull()
        else activityContext.finish()
    }
}

val LocalNavController = compositionLocalOf<CarComposeNavController?> { null }