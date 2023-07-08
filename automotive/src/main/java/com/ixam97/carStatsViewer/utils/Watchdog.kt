package com.ixam97.carStatsViewer.utils

import com.ixam97.carStatsViewer.CarStatsViewer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class Watchdog() {
    private val _watchdogStateFlow = MutableStateFlow<WatchdogState>(WatchdogState())
    val watchdogStateFlow = _watchdogStateFlow.asStateFlow()
    private val _watchdogTriggerFlow = MutableSharedFlow<Unit>(replay = 0)
    val watchdogTriggerFlow = _watchdogTriggerFlow.asSharedFlow()

    fun getCurrentWatchdogState() = watchdogStateFlow.value

    fun updateWatchdogState(watchdogState: WatchdogState) {
        _watchdogStateFlow.value = watchdogState
    }

    fun triggerWatchdog() {
        CoroutineScope(Dispatchers.Default).launch {
            _watchdogTriggerFlow.emit(Unit)
        }
    }
}