package com.ixam97.carStatsViewer.carCompose.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.ixam97.carStatsViewer.carCompose.screens.main.MainScreenTabKeys
import com.ixam97.carStatsViewer.utils.InAppLogger

class CarComposeMainScreenViewModel(private val initialTabKey: MainScreenTabKeys?): ViewModel() {
    var selectedTabKey by mutableStateOf(initialTabKey.toValidKey())
        private set

    private fun MainScreenTabKeys?.toValidKey(): MainScreenTabKeys {
        return if (this == MainScreenTabKeys.Dashboard || this == MainScreenTabKeys.History) this
        else MainScreenTabKeys.Dashboard
    }

    fun setTabKey(key: MainScreenTabKeys?) {
        selectedTabKey = key.toValidKey()
    }

    init {
        InAppLogger.w("MainScreenViewModel created!")
    }

    override fun onCleared() {
        InAppLogger.w("MainScreenViewModel cleared!")
        super.onCleared()
    }
}