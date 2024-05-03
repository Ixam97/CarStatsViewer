package com.ixam97.carStatsViewer.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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