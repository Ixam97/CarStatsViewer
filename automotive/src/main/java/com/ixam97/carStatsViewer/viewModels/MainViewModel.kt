package com.ixam97.carStatsViewer.viewModels

import android.content.Intent
import androidx.core.content.ContextCompat.startForegroundService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.dataCollector.DataCollector
import com.ixam97.carStatsViewer.dataProcessor.RealTimeData
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.viewModels.MainEvent.*
import com.ixam97.carStatsViewer.ui.activities.HistoryActivity
import com.ixam97.carStatsViewer.ui.activities.SettingsActivity
import com.ixam97.carStatsViewer.ui.fragments.SummaryFragment
import com.ixam97.carStatsViewer.ui.plot.objects.PlotGlobalConfiguration
import com.ixam97.carStatsViewer.utils.StringFormatters
import com.ixam97.carStatsViewer.utils.Ticker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class MainViewModel: ViewModel() {

    companion object {
        const val CONSUMPTION_LAYOUT = 0
        const val CHARGE_LAYOUT = 1
    }

    private val applicationContext = CarStatsViewer.appContext

    var currentLayoutMode: Int = CONSUMPTION_LAYOUT
    var drivingOptimization: Boolean = false

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    val mainTripDataStateLiveData: MutableLiveData<MainTripDataState> by lazy { MutableLiveData<MainTripDataState>() }
    val mainRealTimeDataStateLiveData: MutableLiveData<MainRealTimeDataState> by lazy { MutableLiveData<MainRealTimeDataState>() }
    val mainPreferencesStateLiveData: MutableLiveData<MainPreferencesState> by lazy { MutableLiveData<MainPreferencesState>() }
    val mainButtonEnabledStateLiveData: MutableLiveData<MainButtonEnabledState> by lazy { MutableLiveData<MainButtonEnabledState>() }

    private var mainTripDataState = MainTripDataState()
    private var mainRealTimeDataState = MainRealTimeDataState()
    private var mainPreferencesState = MainPreferencesState()
    private var mainButtonEnabledState = MainButtonEnabledState()

    init {

        startForegroundService(applicationContext, Intent(applicationContext, DataCollector::class.java))

        /** Get Updates from the data processor representing the currently selected trip type */
        viewModelScope.launch {
            CarStatsViewer.dataProcessor.selectedSessionDataFlow.collectLatest { drivingSession ->
                applySessionData(drivingSession)
            }
        }

        /** Get updates from the data processor representing current real time data */
        viewModelScope.launch {
            CarStatsViewer.dataProcessor.realTimeDataFlow.collectLatest { realTimeData ->
                applyRealTimeData(realTimeData)
            }
        }

        /** Subscribe to app preferences changes to apply new configuration to screen */
        viewModelScope.launch {
            CarStatsViewer.appPreferences.preferencesChangedFlow().collectLatest {
                applyChangedPreferences()
            }
        }

        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                Ticker.tickerFlow(1000).collectLatest {
                    CarStatsViewer.dataProcessor.updateTripDataValuesByTick()
                }
            }
        }

        /** Apply preferences on first start */
        applyChangedPreferences()
    }

    /** Handle ui events */
    fun onEvent(event: MainEvent) {
        when (event) {
            is OnOpenSettings -> {
                sendUiEvent(UiEvent.StartActivity(intent = Intent(applicationContext, SettingsActivity::class.java).apply { putExtra("animation", "right") }))
            }
            is OnOpenHistory -> {
                sendUiEvent(UiEvent.StartActivity(intent = Intent(applicationContext, HistoryActivity::class.java).apply { putExtra("animation", "right") }))
            }
            is OnTakeScreenshot -> {
                sendUiEvent(UiEvent.TakeScreenshot)
            }
            is OnOpenSummary -> {
                CarStatsViewer.dataProcessor.selectedSessionDataFlow.value?.let { session ->
                    sendUiEvent(UiEvent.OpenFragment(SummaryFragment(session), "SummaryFragment"))
                }
            }
            is OnCloseChargeLayout -> {
                currentLayoutMode = CONSUMPTION_LAYOUT
                applyRealTimeData(CarStatsViewer.dataProcessor.realTimeDataFlow.value)
            }
            is OnSetCurrentTrip -> {

            }
            is OnResetTrip -> {

            }
        }
    }

    private fun applySessionData(drivingSession: DrivingSession?) {
        drivingSession?: return
        mainTripDataState = mainTripDataState.copy(
            distanceString = StringFormatters.getTraveledDistanceString(drivingSession.driven_distance.toFloat()),
            usedEnergyString = StringFormatters.getEnergyString(drivingSession.used_energy.toFloat()),
            avgConsumptionString = StringFormatters.getAvgConsumptionString(drivingSession.used_energy.toFloat(), drivingSession.driven_distance.toFloat()),
            tripTimeString = StringFormatters.getElapsedTimeString(drivingSession.drive_time),
            avgSpeedString = StringFormatters.getAvgSpeedString(drivingSession.driven_distance.toFloat(), drivingSession.drive_time),
            tripType = drivingSession.session_type
        )
        mainTripDataStateLiveData.value = mainTripDataState
    }

    /** The real time values are formatted to numbers according to the unit settings */
    private fun applyRealTimeData(realTimeData: RealTimeData?) {
        realTimeData?: return

        drivingOptimization = (realTimeData.speed?:0f) > 0
        if (realTimeData.chargePortConnected == true) currentLayoutMode = CHARGE_LAYOUT
        else if (drivingOptimization) currentLayoutMode = CONSUMPTION_LAYOUT

        val formattedPower = (realTimeData.power?:0f) / 1_000_000f
        val formattedConsumption: Any? = when {
            realTimeData.instConsumption == null -> null
            CarStatsViewer.appPreferences.consumptionUnit -> CarStatsViewer.appPreferences.distanceUnit.asUnit(realTimeData.instConsumption?:0f).roundToInt()
            !CarStatsViewer.appPreferences.consumptionUnit -> CarStatsViewer.appPreferences.distanceUnit.asUnit(realTimeData.instConsumption?:0f) / 10
            else -> null
        }

        mainRealTimeDataState = mainRealTimeDataState.copy(
            currentPowerFormatted = if (formattedPower.absoluteValue >= 100f) formattedPower.toInt() else formattedPower,
            currentConsumptionFormatted = formattedConsumption,
            currentStateOfChargeFormatted = ((realTimeData.stateOfCharge?:0f) * 100f).roundToInt(),
            layoutMode = currentLayoutMode
        )
        mainRealTimeDataStateLiveData.value = mainRealTimeDataState

        mainButtonEnabledState = mainButtonEnabledState.copy(
            closeChargeLayoutButtonEnabled = realTimeData.chargePortConnected == false,
            historyButtonEnabled = !drivingOptimization,
            summaryButtonEnabled = !drivingOptimization
        )
        mainButtonEnabledStateLiveData.value = mainButtonEnabledState

        sendUiEvent(UiEvent.ApplyDrivingOptimization(drivingOptimization))
    }

    private fun applyChangedPreferences() {
        val newDistanceUnit = CarStatsViewer.appPreferences.distanceUnit.unit()
        mainPreferencesState = mainPreferencesState.copy(
            distanceUnit = newDistanceUnit,
            consumptionUnit = if (CarStatsViewer.appPreferences.consumptionUnit) {
                // Wh/distance
                "Wh/$newDistanceUnit"
            } else {
                // kWh/100distance
                "kWh/100$newDistanceUnit"
            },
            powerUnit = "kW", // might ad Horsepower later...
            visibleChargingGages = CarStatsViewer.appPreferences.chargePlotVisibleGages,
            visibleConsumptionGages = CarStatsViewer.appPreferences.consumptionPlotVisibleGages
        )
        PlotGlobalConfiguration.updateDistanceUnit(CarStatsViewer.appPreferences.distanceUnit)
        mainPreferencesStateLiveData.postValue(mainPreferencesState)
        applySessionData(CarStatsViewer.dataProcessor.selectedSessionDataFlow.value)
        applyRealTimeData(CarStatsViewer.dataProcessor.realTimeDataFlow.value)
    }

    private fun sendUiEvent(event: UiEvent) {
        viewModelScope.launch {
            _uiEvent.send(event)
        }
    }
}