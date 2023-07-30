package com.ixam97.carStatsViewer.viewModels

import android.content.Intent
import androidx.fragment.app.Fragment

sealed class UiEvent() {
    object PopBackstack: UiEvent()
    data class StartActivity(val intent: Intent): UiEvent()
    data class OpenFragment(val fragment: Fragment, val tag: String): UiEvent()
    object TakeScreenshot: UiEvent()
    data class ApplyDrivingOptimization(val drivingOptimization: Boolean): UiEvent()
}
