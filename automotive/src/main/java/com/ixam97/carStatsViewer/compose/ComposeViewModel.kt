package com.ixam97.carStatsViewer.compose

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ixam97.carStatsViewer.CarStatsViewer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ComposeViewModel: ViewModel() {

    data class ComposeActivityState(
        val switchStates: List<Boolean> = listOf(false, false, false),
        val screenIndex: Int = 0
    )

    val vehicleBrand = CarStatsViewer.dataProcessor.staticVehicleData.vehicleMake

    private val _composeActivityState = MutableStateFlow(ComposeActivityState())
    val composeActivityState = _composeActivityState.asStateFlow()

    private val _finishActivityLiveData = MutableLiveData<Event<Boolean>>()
    val finishActivityLiveData: LiveData<Event<Boolean>> = _finishActivityLiveData

    fun setSwitch(switchIndex: Int, value: Boolean) {
        if (switchIndex >= _composeActivityState.value.switchStates.size || switchIndex < 0) return

        _composeActivityState.update {
            val newSwitchStates = it.switchStates.toMutableList()
            newSwitchStates[switchIndex] = value
            it.copy(switchStates = newSwitchStates)
        }
    }

    fun increaseScreenIndex() {
        _composeActivityState.update {
            it.copy(screenIndex = it.screenIndex + 1)
        }
    }

    fun finishActivity() {
        if (composeActivityState.value.screenIndex > 0) {
            _composeActivityState.update {
                it.copy(screenIndex = it.screenIndex - 1)
            }
        } else {
            _finishActivityLiveData.postValue(Event(true))
        }
    }

}

