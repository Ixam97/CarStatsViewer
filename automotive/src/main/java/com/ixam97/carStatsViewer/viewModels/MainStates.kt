package com.ixam97.carStatsViewer.viewModels

data class MainTripDataState(
    val distanceString: String = "",
    val usedEnergyString: String = "",
    val avgConsumptionString: String = "",
    val tripTimeString: String = "",
    val avgSpeedString: String = "",
    val tripType: Int = 0,
)

/** The real time values are formatted to numbers according to the unit settings */
data class MainRealTimeDataState(
    val currentPowerFormatted: Any = 0f,
    val currentConsumptionFormatted: Any? = null,
    val currentStateOfChargeFormatted: Int = 0,
    val layoutMode: Int = MainViewModel.CONSUMPTION_LAYOUT
)

data class MainPreferencesState(
    val distanceUnit: String = "",
    val powerUnit: String = "",
    val consumptionUnit: String = "",
    val visibleConsumptionGages: Boolean = true,
    val visibleChargingGages: Boolean = true
)

data class MainButtonEnabledState(
    val historyButtonEnabled: Boolean = true,
    val summaryButtonEnabled: Boolean = true,
    val closeChargeLayoutButtonEnabled: Boolean = true
)