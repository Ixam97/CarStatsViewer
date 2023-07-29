package com.ixam97.carStatsViewer.events

sealed class MainEvent() {
    object OnOpenSettings: MainEvent()
    object OnOpenHistory: MainEvent()
    object OnTakeScreenshot: MainEvent()
    data class OnOpenSummary(val sessionId: Int): MainEvent()
    data class OnSetCurrentTrip(val tripType: Int): MainEvent()
    object OnResetTrip: MainEvent()
}
