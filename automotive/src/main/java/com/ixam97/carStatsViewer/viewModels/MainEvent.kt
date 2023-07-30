package com.ixam97.carStatsViewer.viewModels

sealed class MainEvent() {
    object OnOpenSettings: MainEvent()
    object OnOpenHistory: MainEvent()
    object OnTakeScreenshot: MainEvent()
    object OnOpenSummary: MainEvent()
    object OnCloseChargeLayout: MainEvent()
    data class OnSetCurrentTrip(val tripType: Int): MainEvent()
    object OnResetTrip: MainEvent()
}
