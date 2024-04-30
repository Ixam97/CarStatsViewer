package com.ixam97.carStatsViewer.carApp

import androidx.annotation.OptIn
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.core.graphics.drawable.IconCompat
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R

@OptIn(ExperimentalCarApi::class)
internal fun CarStatsViewerScreen.NavigationTest() = NavigationTemplate.Builder().apply {

    val selectedDimension = appPreferences.secondaryConsumptionDimension
    val secondaryPlotColor = appPreferences.chargePlotSecondaryColor
    val selectedColor = CarColor.createCustom(carContext.getColor(R.color.secondary_plot_color), carContext.getColor(R.color.secondary_plot_color))


    setActionStrip(
        ActionStrip.Builder().apply {
            addAction(Action.Builder().apply {

                val unitString = appPreferences.distanceUnit.unit()
                val buttonLabel = when (appPreferences.mainPrimaryDimensionRestriction) {
                    1 -> "40 $unitString"
                    2 -> "100 $unitString"
                    else -> "20 $unitString"
                }

                setTitle(buttonLabel)
                // setIcon(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_arrow_diagonal)).build())
                when (CarStatsViewer.dataProcessor.staticVehicleData.modelName) {
                    "PS2", "Speedy Model" -> {}
                    else -> setFlags(Action.FLAG_IS_PERSISTENT)
                }
                setOnClickListener {
                    val currentDistance = appPreferences.mainPrimaryDimensionRestriction
                    appPreferences.mainPrimaryDimensionRestriction = if (currentDistance >= 2) 0 else currentDistance + 1
                    //invalidate()
                    invalidateTabView()
                    carDataSurfaceCallback.invalidatePlot()
                    // carDataSurfaceCallback.renderFrame()
                }
            }.build())
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
                carDataSurfaceCallback.renderFrame()
                //invalidate()
                invalidateTabView()
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
                carDataSurfaceCallback.renderFrame()
                //invalidate()
                invalidateTabView()
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
                carDataSurfaceCallback.renderFrame()
                //invalidate()
                invalidateTabView()
            }
        }.build())
    }.build())
}.build()