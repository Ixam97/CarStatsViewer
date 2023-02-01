package com.ixam97.carStatsViewer.dataManager

enum class DataManagers(var doTrack: Boolean = true, val dataManager: DataManager) {
    CURRENT_TRIP(dataManager = DataManager()),
    SINCE_CHARGE(dataManager = DataManager()),
    AUTO_DRIVE(dataManager = DataManager()),
    CURRENT_MONTH(dataManager = DataManager())
}
