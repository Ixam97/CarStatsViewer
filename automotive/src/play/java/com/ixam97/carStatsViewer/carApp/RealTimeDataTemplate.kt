package com.ixam97.carStatsViewer.carApp

import androidx.annotation.OptIn
import androidx.car.app.CarContext
import androidx.car.app.ScreenManager
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.car.app.annotations.RequiresCarApi
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Header
import androidx.car.app.model.Item
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.PlaceListMapTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.model.Toggle
import androidx.car.app.navigation.model.MapController
import androidx.car.app.navigation.model.MapTemplate
import androidx.car.app.navigation.model.MapWithContentTemplate
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.navigation.model.PlaceListNavigationTemplate
import androidx.car.app.versioning.CarAppApiLevel
import androidx.core.graphics.drawable.IconCompat
import com.ixam97.carStatsViewer.BuildConfig
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carApp.renderer.DefaultRenderer

class RealTimeDataTemplate(
    val carContext: CarContext,
    val session: CarStatsViewerSession,
    val backButton: Boolean = true,
    val navigationOnly: Boolean = true,
    val invalidateCallback: () -> Unit,
) {

    private data class DebugState(
        var showDebugMenu: Boolean = false,
        var showBounds: Boolean = false,
        var fixedSizes: Boolean = false,
        var fixedSizesLayout: String? = null,
        var showLayoutSelection: Boolean = false,
        var hideAllButtons: Boolean = false
    )

    private val debugState = DebugState()

    private val appPreferences = CarStatsViewer.appPreferences

    private var selectedDimension = appPreferences.secondaryConsumptionDimension
    private val secondaryPlotColor = appPreferences.chargePlotSecondaryColor
    private val selectedColor = CarColor.createCustom(
        carContext.getColor(R.color.secondary_plot_color),
        carContext.getColor(R.color.secondary_plot_color)
    )
    private val debugColor = CarColor.createCustom(
        carContext.getColor(R.color.polestar_orange),
        carContext.getColor(R.color.polestar_orange)
    )



    @OptIn(ExperimentalCarApi::class)
    fun getTemplate(): Template {
        // Set states when template is requested
        selectedDimension = appPreferences.secondaryConsumptionDimension
        debugState.fixedSizes = session.carDataSurfaceCallback.defaultRenderer.useFixedSizes
        debugState.showBounds = session.carDataSurfaceCallback.defaultRenderer.drawBoundingBoxes
        debugState.fixedSizesLayout = session.carDataSurfaceCallback.defaultRenderer.overrideLayout

        if (navigationOnly) return navigationTemplate()
        else if (debugState.showDebugMenu) return if (carContext.carAppApiLevel >= 7) mapWithContentTemplate() else mapTemplate()
        return navigationTemplate()
    }



    private fun navigationTemplate() = NavigationTemplate.Builder().apply {
        setActionStrip(actionStrip())
        setMapActionStrip(mapActionStrip())
    }.build()

    @RequiresCarApi(5)
    private fun mapTemplate() = MapTemplate.Builder().apply {
        if (!debugState.showLayoutSelection) {
            setItemList(debugMenu())
            setHeader(debugHeader())
        } else {
            setItemList(debugMenuLayoutSelection())
            setHeader(debugHeaderLayoutSelection())
        }
        setMapController(MapController.Builder().apply {
            setMapActionStrip(mapActionStrip())
        }.build())
        setActionStrip(actionStrip())
    }.build()

    @RequiresCarApi(7)
    private fun mapWithContentTemplate() = MapWithContentTemplate.Builder().apply {
        setActionStrip(actionStrip())
        setMapController(MapController.Builder().apply {
            setMapActionStrip(mapActionStrip())
        }.build())
        setContentTemplate(ListTemplate.Builder().apply {
            if (debugState.showLayoutSelection) {
                setSingleList(debugMenuLayoutSelection())
                setHeader(debugHeaderLayoutSelection())
            } else {
                setSingleList(debugMenu())
                setHeader(debugHeader())
            }
        }.build())
    }.build()

    private fun localInvalidate() {
        session.carDataSurfaceCallback.requestRenderFrame()
        invalidateCallback()
    }

    private fun actionStrip() = ActionStrip.Builder().apply {
        /** Debug button for Emulator **/
        if (BuildConfig.FLAVOR_version == "dev" && !navigationOnly) {
            addAction(Action.Builder().apply {
                setIcon(
                    CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_car_app_debug)).apply {
                        if (debugState.showDebugMenu) setTint(CarColor.YELLOW)
                    }.build()
                )
                if (!debugState.hideAllButtons) setFlags(Action.FLAG_IS_PERSISTENT)
                setOnClickListener {
                    debugState.showDebugMenu = !debugState.showDebugMenu
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
            val unknownVehicle =
                when (CarStatsViewer.dataProcessor.staticVehicleData.modelName) {
                    "PS2", "Speedy Model" -> false
                    else -> true
                }
            if (session.carDataSurfaceCallback.getDebugFlag() || unknownVehicle && !debugState.hideAllButtons) setFlags(
                Action.FLAG_IS_PERSISTENT
            )
            setOnClickListener {
                val currentDistance = appPreferences.mainPrimaryDimensionRestriction
                appPreferences.mainPrimaryDimensionRestriction =
                    if (currentDistance >= 2) 0 else currentDistance + 1
                localInvalidate()
                session.carDataSurfaceCallback.invalidatePlot()
            }
        }.build())
        if (backButton) addAction(Action.Builder().apply {
            if (!debugState.hideAllButtons) setFlags(Action.FLAG_IS_PERSISTENT)
            setIcon(CarIcon.BACK)
            setOnClickListener {
                session.carDataSurfaceCallback.pause()
                carContext.getCarService(ScreenManager::class.java).pop()
            }
        }.build())

    }.build()

    private fun mapActionStrip() = ActionStrip.Builder().apply {
        addAction(Action.Builder().apply {
            setIcon(
                CarIcon.Builder(
                    IconCompat.createWithResource(
                        carContext,
                        R.drawable.ic_car_app_speed
                    )
                ).apply {
                    if (selectedDimension == 1) {
                        setTint(selectedColor)
                    }
                }.build()
            )
            if (!debugState.hideAllButtons) setFlags(Action.FLAG_IS_PERSISTENT)
            setOnClickListener {
                appPreferences.secondaryConsumptionDimension =
                    if (selectedDimension == 1) 0 else 1

                localInvalidate()
            }
        }.build())
        addAction(Action.Builder().apply {
            setIcon(
                CarIcon.Builder(
                    IconCompat.createWithResource(
                        carContext,
                        R.drawable.ic_car_app_altitude
                    )
                ).apply {
                    if (selectedDimension == 3) {
                        setTint(selectedColor)
                    }
                }.build()
            )
            if (!debugState.hideAllButtons) setFlags(Action.FLAG_IS_PERSISTENT)
            setOnClickListener {
                appPreferences.secondaryConsumptionDimension =
                    if (selectedDimension == 3) 0 else 3
                localInvalidate()
            }
        }.build())
        addAction(Action.Builder().apply {
            setIcon(
                CarIcon.Builder(
                    IconCompat.createWithResource(
                        carContext,
                        R.drawable.ic_car_app_battery
                    )
                ).apply {
                    if (selectedDimension == 2) {
                        setTint(selectedColor)
                    }
                }.build()
            )
            if (!debugState.hideAllButtons) setFlags(Action.FLAG_IS_PERSISTENT)
            setOnClickListener {
                appPreferences.secondaryConsumptionDimension =
                    if (selectedDimension == 2) 0 else 2
                localInvalidate()
            }
        }.build())
    }.build()

    private fun debugMenuLayoutSelection() = ItemList.Builder().apply {
        DefaultRenderer.layoutList.forEach {
            addItem(Row.Builder().apply {
                setTitle(it.key)
                setOnClickListener {
                    debugState.fixedSizesLayout = it.key
                    session.carDataSurfaceCallback.defaultRenderer.overrideLayout = debugState.fixedSizesLayout
                    debugState.showLayoutSelection = false
                    localInvalidate()
                }
            }.build())
        }
    }.build()

    private fun debugMenu() = ItemList.Builder().apply {
        addItem(Row.Builder().apply {
            setTitle("Bounding box")
            setToggle(Toggle.Builder {
                debugState.showBounds = it
                session.carDataSurfaceCallback.defaultRenderer.drawBoundingBoxes = debugState.showBounds
                session.carDataSurfaceCallback.requestRenderFrame()
            }.setChecked(debugState.showBounds).build())
        }.build())
        addItem(Row.Builder().apply {
            setTitle("Fixed sizes")
            setToggle(Toggle.Builder {
                debugState.fixedSizes = it
                session.carDataSurfaceCallback.defaultRenderer.useFixedSizes = debugState.fixedSizes
                session.carDataSurfaceCallback.defaultRenderer.overrideLayout =
                    if(debugState.fixedSizesLayout != null && debugState.fixedSizes)
                        debugState.fixedSizesLayout
                    else null
                session.carDataSurfaceCallback.requestRenderFrame()
                localInvalidate()
            }.setChecked(debugState.fixedSizes).build())
        }.build())
        if (debugState.fixedSizes) {
            addItem(Row.Builder().apply {
                setTitle("Selected layout: ${debugState.fixedSizesLayout?:"Auto"}")
                setBrowsable(true)
                setOnClickListener {
                    debugState.showLayoutSelection = true
                    localInvalidate()
                }
            }.build())
        }
        addItem(Row.Builder().apply {
            setTitle("Hide Buttons")
            setToggle(Toggle.Builder {
                debugState.hideAllButtons = it
                session.carDataSurfaceCallback.requestRenderFrame()
            }.setChecked(debugState.hideAllButtons).build())
        }.build())
    }.build()

    private fun debugHeader() = Header.Builder().apply {
        addEndHeaderAction(Action.Builder().apply {
            setIcon(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_close)).build())
            setOnClickListener {
                debugState.showDebugMenu = !debugState.showDebugMenu
                localInvalidate()
            }
        }.build())
        setTitle("Debug Menu")
    }.build()

    private fun debugHeaderLayoutSelection() = Header.Builder().apply {
        addEndHeaderAction(Action.Builder().apply {
            setIcon(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_arrow_back)).build())
            setOnClickListener {
                debugState.showLayoutSelection = false
                localInvalidate()
            }
        }.build())
        setTitle("Debug Menu")
    }.build()
}