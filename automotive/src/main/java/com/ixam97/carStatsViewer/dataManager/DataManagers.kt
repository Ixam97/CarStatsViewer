package com.ixam97.carStatsViewer.dataManager

enum class DataManagers(var doTrack: Boolean = true, val dataManager: DataManager) {
    CURRENT_TRIP(dataManager = DataManager("CurrentTripData")),
    SINCE_CHARGE(dataManager = DataManager("SinceChargeData")),
    AUTO_DRIVE(dataManager = DataManager("AutoTripData")),
    CURRENT_MONTH(dataManager = DataManager("CurrentMonthData"))
}
