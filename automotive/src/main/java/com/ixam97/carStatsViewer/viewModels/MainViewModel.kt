package com.ixam97.carStatsViewer.viewModels

import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.dataProcessor.RealTimeData
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.events.MainEvent
import com.ixam97.carStatsViewer.events.MainEvent.*
import com.ixam97.carStatsViewer.events.UiEvent
import com.ixam97.carStatsViewer.ui.activities.HistoryActivity
import com.ixam97.carStatsViewer.ui.activities.SettingsActivity
import com.ixam97.carStatsViewer.utils.InAppLogger
import com.ixam97.carStatsViewer.utils.StringFormatters
import com.ixam97.carStatsViewer.utils.Ticker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MainTripDataState(
    val distanceString: String = "",
    val usedEnergyString: String = "",
    val avgConsumptionString: String = "",
    val tripTimeString: String = "",
    val avgSpeedString: String = ""
)

data class MainRealTimeDataState(
    val currentPower: Float = 0f,
    val currentConsumption: Float = 0f,
    val currentSoC: Float = 0f
)

class MainViewModel: ViewModel() {

    private val applicationContext = CarStatsViewer.appContext

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    val mainTripDataStateLiveData: MutableLiveData<MainTripDataState> by lazy {
        MutableLiveData<MainTripDataState>()
    }

    val mainRealTimeDataStateLiveData: MutableLiveData<MainRealTimeDataState> by lazy {
        MutableLiveData<MainRealTimeDataState>()
    }

    private var mainTripDataState = MainTripDataState()
    private var mainRealTimeDataState = MainRealTimeDataState()

    init {
        viewModelScope.launch {
            CarStatsViewer.dataProcessor.selectedSessionDataFlow.collectLatest { drivingSession ->
                applySessionData(drivingSession)
            }
        }

        viewModelScope.launch {
            CarStatsViewer.dataProcessor.realTimeDataFlow.collectLatest { realTimeData ->
                applyRealTimeData(realTimeData)
            }
        }

        viewModelScope.launch {
            CarStatsViewer.appPreferences.preferencesChangedFlow().collectLatest {
                applySessionData(CarStatsViewer.dataProcessor.selectedSessionDataFlow.value)
                applyRealTimeData(CarStatsViewer.dataProcessor.realTimeDataFlow.value)
            }
        }

        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                Ticker.tickerFlow(1000).collectLatest {
                    CarStatsViewer.dataProcessor.updateTripDataValuesByTick()
                }
            }
        }
    }

    private fun applySessionData(drivingSession: DrivingSession?) {
        mainTripDataState = mainTripDataState.copy(
            distanceString = StringFormatters.getTraveledDistanceString((drivingSession?.driven_distance?:0.0).toFloat()),
            usedEnergyString = StringFormatters.getEnergyString((drivingSession?.used_energy?:0.0).toFloat()),
            avgConsumptionString = StringFormatters.getAvgConsumptionString((drivingSession?.used_energy?:0.0).toFloat(), (drivingSession?.driven_distance?:0.0).toFloat()),
            tripTimeString = StringFormatters.getElapsedTimeString(drivingSession?.drive_time?:0),
            avgSpeedString = StringFormatters.getAvgSpeedString((drivingSession?.driven_distance?:0.0).toFloat(), drivingSession?.drive_time?:0)
        )
        mainTripDataStateLiveData.postValue(mainTripDataState)
    }

    private fun applyRealTimeData(realTimeData: RealTimeData?) {
        mainRealTimeDataState = mainRealTimeDataState.copy(
            currentPower = (realTimeData?.power?:0f) / 1_000_000f,
            currentConsumption = (realTimeData?.instConsumption?:0f),
            currentSoC = (realTimeData?.stateOfCharge?:0f) * 100
        )
        mainRealTimeDataStateLiveData.postValue(mainRealTimeDataState)
    }

    fun onEvent(event: MainEvent) {
        when (event) {
            is OnOpenSettings -> {
                sendUiEvent(UiEvent.startActivity(intent = Intent(applicationContext, SettingsActivity::class.java).apply { putExtra("animation", "right") }))
            }
            is OnOpenHistory -> {
                sendUiEvent(UiEvent.startActivity(intent = Intent(applicationContext, HistoryActivity::class.java).apply { putExtra("animation", "right") }))
            }
            is OnTakeScreenshot -> {

            }
            is OnOpenSummary -> {

            }
            is OnSetCurrentTrip -> {

            }
            is OnResetTrip -> {

            }
        }
    }

    private fun sendUiEvent(event: UiEvent) {
        viewModelScope.launch {
            _uiEvent.send(event)
        }
    }
}