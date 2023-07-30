package com.ixam97.carStatsViewer.events

import android.content.Intent

sealed class UiEvent() {
    object PopBackstack: UiEvent()
    data class StartActivity(val intent: Intent): UiEvent()
    object TakeScreenshot: UiEvent()
}
