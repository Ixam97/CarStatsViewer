package com.ixam97.carStatsViewer.dataProcessor

import android.car.VehicleIgnitionState

object IgnitionState {
    const val UNDEFINED = VehicleIgnitionState.UNDEFINED
    const val LOCK = VehicleIgnitionState.LOCK
    const val OFF = VehicleIgnitionState.OFF
    const val ACC = VehicleIgnitionState.ACC
    const val ON = VehicleIgnitionState.ON
    const val START = VehicleIgnitionState.START

    val nameMap = mapOf<Int, String>(
        UNDEFINED to "Undefined",
        LOCK to "Locked",
        OFF to "Off",
        ACC to "Accessory",
        ON to "On",
        START to "Started"
    )

}