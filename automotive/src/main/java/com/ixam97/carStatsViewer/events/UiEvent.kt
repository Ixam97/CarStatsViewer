package com.ixam97.carStatsViewer.events

import android.content.Intent

sealed class UiEvent() {
    object popBackstack: UiEvent()
    data class startActivity(val intent: Intent): UiEvent()
}
