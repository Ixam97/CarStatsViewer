package com.ixam97.carStatsViewer.carCompose.screens.tripDetails

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TripDetailsViewModelFactory(private val sessionId: Long) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TripDetailsViewModel(sessionId) as T
    }
}

class TripDetailsViewModel(sessionId: Long): ViewModel() {

    data class TripDetailsState(
        val isLoading: Boolean = false
    )

    data class TripDataState(
        val distance: Float? = null
    )

    var tripDetailsState by mutableStateOf(TripDetailsState())
        private set

    init {

        tripDetailsState = tripDetailsState.copy(
            isLoading = true
        )

        viewModelScope.launch {
            delay(5000)
            tripDetailsState = tripDetailsState.copy(
                isLoading = false
            )
        }
    }
}