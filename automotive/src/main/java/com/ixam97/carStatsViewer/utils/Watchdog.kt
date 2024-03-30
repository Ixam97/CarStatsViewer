package com.ixam97.carStatsViewer.utils

import com.ixam97.carStatsViewer.CarStatsViewer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

class Watchdog() {
    private val _watchdogStateFlow = MutableStateFlow<WatchdogState>(WatchdogState())
    val watchdogStateFlow = _watchdogStateFlow.asStateFlow()
    private val _watchdogTriggerFlow = MutableSharedFlow<Unit>(replay = 0)
    val watchdogTriggerFlow = _watchdogTriggerFlow.asSharedFlow()

    private var executor :Executor? = null
    private var runnable :Runnable? = null

    fun getCurrentWatchdogState() = watchdogStateFlow.value

    fun setAaosCallback(executor: Executor, runnable: Runnable) {
        this.executor = executor
        this.runnable = runnable
    }

    fun updateWatchdogState(watchdogState: WatchdogState) {
        _watchdogStateFlow.value = watchdogState
    }

    fun triggerWatchdog() {
        CoroutineScope(Dispatchers.Default).launch {
            _watchdogTriggerFlow.emit(Unit)
            executor?.execute(runnable)
        }
    }
}