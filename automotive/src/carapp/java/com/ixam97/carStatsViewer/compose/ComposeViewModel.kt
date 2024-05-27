package com.ixam97.carStatsViewer.compose

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ComposeViewModel: ViewModel() {

    data class ComposeActivityState(
        val switchStates: List<Boolean> = listOf(false, false, false)
    )

    private val _composeActivityState = MutableStateFlow(ComposeActivityState())
    val composeActivityState = _composeActivityState.asStateFlow()

    fun setSwitch(switchIndex: Int, value: Boolean) {
        if (switchIndex >= _composeActivityState.value.switchStates.size || switchIndex < 0) return

        _composeActivityState.update {
            val newSwitchStates = it.switchStates.toMutableList()
            newSwitchStates[switchIndex] = value
            it.copy(switchStates = newSwitchStates)
        }
    }

}

