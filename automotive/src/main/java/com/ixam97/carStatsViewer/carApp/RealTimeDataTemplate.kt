package com.ixam97.carStatsViewer.carApp

import androidx.annotation.OptIn
import androidx.car.app.CarContext
import androidx.car.app.ScreenManager
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.core.graphics.drawable.IconCompat
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carApp.renderer.CarDataSurfaceCallback

@OptIn(ExperimentalCarApi::class)
fun RealTimeDateTemplate(
    carContext: CarContext,
    session: CarStatsViewerSession,
    backButton: Boolean = true,
    invalidateCallback: () -> Unit,
) = NavigationTemplate.Builder().apply {

    val appPreferences = CarStatsViewer.appPreferences

    val selectedDimension = appPreferences.secondaryConsumptionDimension
    val secondaryPlotColor = appPreferences.chargePlotSecondaryColor
    val selectedColor = CarColor.createCustom(carContext.getColor(R.color.secondary_plot_color), carContext.getColor(R.color.secondary_plot_color))
    val debugColor = CarColor.createCustom(carContext.getColor(R.color.polestar_orange), carContext.getColor(R.color.polestar_orange))

    fun localInvalidate() {
        session.carDataSurfaceCallback.requestRenderFrame()
        invalidateCallback()
    }

    setActionStrip(
        ActionStrip.Builder().apply {
            /** Debug button for Emulator **/
            if (CarStatsViewer.dataProcessor.staticVehicleData.modelName == "Speedy Model") {
                addAction(Action.Builder().apply {
                    setIcon(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_car_app_debug)).apply {
                        if (session.carDataSurfaceCallback.getDebugFlag()) {
                            setTint(CarColor.YELLOW)
                        }
                    }.build())
                    setOnClickListener {
                        session.carDataSurfaceCallback.toggleDebugFlag()
                        localInvalidate()
                    }
                }.build())
            }
            addAction(Action.Builder().apply {

                val unitString = appPreferences.distanceUnit.unit()
                val buttonLabel = when (appPreferences.mainPrimaryDimensionRestriction) {
                    1 -> "40 $unitString"
                    2 -> "100 $unitString"
                    else -> "20 $unitString"
                }

                setTitle(buttonLabel)
                val unknownVehicle = when (CarStatsViewer.dataProcessor.staticVehicleData.modelName) {
                    "PS2", "Speedy Model" -> false
                    else -> true
                }
                if (session.carDataSurfaceCallback.getDebugFlag() || unknownVehicle) setFlags(Action.FLAG_IS_PERSISTENT)
                setOnClickListener {
                    val currentDistance = appPreferences.mainPrimaryDimensionRestriction
                    appPreferences.mainPrimaryDimensionRestriction = if (currentDistance >= 2) 0 else currentDistance + 1
                    localInvalidate()
                    session.carDataSurfaceCallback.invalidatePlot()
                }
            }.build())
            if (backButton) addAction(Action.Builder()
                .setFlags(Action.FLAG_IS_PERSISTENT)
                .setIcon(CarIcon.BACK)
                .setOnClickListener {
                    session.carDataSurfaceCallback.pause()
                    carContext.getCarService(ScreenManager::class.java).pop()
                }
                .build())

        }.build()
    )
    setMapActionStrip(ActionStrip.Builder().apply {
        addAction(Action.Builder().apply {
            setIcon(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_car_app_speed)).apply {
                if (selectedDimension == 1) {
                    setTint(selectedColor)
                }
            }.build())
            setFlags(Action.FLAG_IS_PERSISTENT)
            setOnClickListener {
                appPreferences.secondaryConsumptionDimension = if (selectedDimension == 1) 0 else 1

                localInvalidate()
            }
        }.build())
        addAction(Action.Builder().apply {
            setIcon(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_car_app_altitude)).apply {
                if (selectedDimension == 3) {
                    setTint(selectedColor)
                }
            }.build())
            setFlags(Action.FLAG_IS_PERSISTENT)
            setOnClickListener {
                appPreferences.secondaryConsumptionDimension = if (selectedDimension == 3) 0 else 3
                localInvalidate()
            }
        }.build())
        addAction(Action.Builder().apply {
            setIcon(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_car_app_battery)).apply {
                if (selectedDimension == 2) {
                    setTint(selectedColor)
                }
            }.build())
            setFlags(Action.FLAG_IS_PERSISTENT)
            setOnClickListener {
                appPreferences.secondaryConsumptionDimension = if (selectedDimension == 2) 0 else 2
                localInvalidate()
            }
        }.build())
    }.build())
}.build()