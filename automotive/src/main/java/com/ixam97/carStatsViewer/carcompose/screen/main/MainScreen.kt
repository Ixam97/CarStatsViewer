package com.ixam97.carStatsViewer.carcompose.screen.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carcompose.CarComposeViewModel
import com.ixam97.carStatsViewer.carcompose.TabIndexes
import com.ixam97.carStatsViewer.carcompose.TabOrientation
import de.ixam97.carcompose.components.controls.CarIconButton
import de.ixam97.carcompose.components.controls.CarRow
import de.ixam97.carcompose.components.controls.CarRowSwitch
import de.ixam97.carcompose.components.controls.CarSegmentedButton
import de.ixam97.carcompose.components.layout.CarColumn
import de.ixam97.carcompose.components.layout.CarListDivider
import de.ixam97.carcompose.components.layout.CarListItem
import de.ixam97.carcompose.components.layout.CarListSection
import de.ixam97.carcompose.components.layout.CarTabLayout
import de.ixam97.carcompose.resources.CarIcons
import de.ixam97.carcompose.theme.CarTheme

@Composable
fun MainScreenLandscape(
    viewModel: CarComposeViewModel,
    onBackClick: () -> Unit
) {
    CarTabLayout(
        isLoading = viewModel.carComposeState.isLoading,
        tabSelectedIndex = viewModel.carComposeState.selectedTab,
        tabOrientation = viewModel.carComposeState.tabLayoutOrientation,
        tabs = listOf(
            CarTabLayout.Tab(
                title = "Trip Data",
                icon = painterResource(R.drawable.ic_distance)
            ),
            CarTabLayout.Tab(
                title = "Performance",
                icon = painterResource(R.drawable.ic_speed)
            ),
            CarTabLayout.Tab(
                title = "History",
                icon = painterResource(R.drawable.ic_history),
                enabled = !viewModel.movingState
            ),
            CarTabLayout.Tab(
                title = "UI Demo Settings",
                icon = CarIcons.settings,
                iconActive = CarIcons.Filled.settings,
                enabled = !viewModel.movingState
            )
        ),
        tabOnIndexChanged = {
            viewModel.setLoading(false)
            viewModel.setSelectedTab(it)
        },
        headerTitle = "Car Compose Demo",
        headerStartContent = {
            Image(
                modifier = Modifier
                    .width(CarTheme.carDimensions.iconButtonSize)
                    .fillMaxHeight(),
                bitmap = LocalContext.current.resources.getDrawable(R.mipmap.ic_launcher, null).toBitmap().asImageBitmap(),
                contentDescription = null
            )
        },
        headerIconButtons = listOf(
            {
                CarIconButton(
                    painter = CarIcons.close,
                    tint = Color.Red,
                    onClick = onBackClick
                )
            },
            {
                CarIconButton(
                    painter = CarIcons.settings,
                    onClick = { /* Navigate to settings Screen */ },
                    enabled = false
                )
            },
        )
    ) {
        when (viewModel.carComposeState.selectedTab) {
            TabIndexes.TRIP -> TabTripData()
            TabIndexes.PERFORMANCE -> TabPerformance()
            TabIndexes.HISTORY -> TabHistory(viewModel)
            else -> DemoLayout(viewModel)
        }
    }
}

fun placeholderRowList(numRows: Int): List<CarListItem> {
    val listItems = mutableListOf<CarListItem>()
    for (i in 0..numRows - 1) {
        listItems.add(
            CarListItem {
                CarRow(
                    title = "Car Row Number $i",
                    description = "Content text of car row number $i."
                )
            }
        )
    }

    return listItems.toList()
}

@Composable
fun DemoLayout(
    viewModel: CarComposeViewModel
) {
    CarColumn {
        CarListSection(
            sectionTitle = "Theme Selection:",
            listItems = listOf(
                /*
                CarListItem {
                    CarRowSwitch(
                        title = "Polestar Modern",
                        description = "Inspired by Polestar 3 and 4 UI theme.",
                        state = viewModel.carComposeState.uiType == UiType.PolestarModern,
                        onStateChange = {
                            if (it) {
                                viewModel.setUiType(UiType.PolestarModern)
                            }
                        }
                    )
                },
                CarListItem {
                    CarRowSwitch(
                        title = "Polestar Classic",
                        description = "Inspired by Polestar 2 UI theme.",
                        state = viewModel.carComposeState.uiType == UiType.PolestarClassic,
                        onStateChange = {
                            if (it) {
                                viewModel.setUiType(UiType.PolestarClassic)
                            }
                        }
                    )
                },
                CarListItem {
                    CarRowSwitch(
                        title = "Generic",
                        description = "Basic Android theme.",
                        state = viewModel.carComposeState.uiType == UiType.Generic,
                        onStateChange = {
                            if (it) {
                                viewModel.setUiType(UiType.Generic)
                            }
                        }
                    )
                }
                 */
                CarListItem {
                    CarRow(
                        title = "Car Compose Theme",
                        descriptionContent = {
                            CarSegmentedButton(
                                buttonContents = listOf(
                                    { Text("Club") },
                                    { Text("Polestar Modern") },
                                    { Text("Polestar Classic") },
                                    { Text("Generic") },
                                ),
                                selectedIndex = viewModel.carComposeState.uiTypeIndex,
                                onIndexChanged = {
                                    viewModel.setUiTypeIndex(it)
                                },
                                enabledIndexes = listOf(true, true, true, true)
                            )
                        }
                    )
                },
                CarListItem {
                    CarRow(
                        title = "Tabs Orientation",
                        descriptionContent = {
                            CarSegmentedButton(
                                buttonContents = listOf(
                                    { Text("Vertical") },
                                    { Text("Horizontal") },
                                ),
                                selectedIndex = when (viewModel.carComposeState.tabLayoutOrientation) {
                                    CarTabLayout.Orientation.Horizontal -> TabOrientation.HORIZONTAL
                                    CarTabLayout.Orientation.Vertical -> TabOrientation.VERTICAL
                                },
                                onIndexChanged = {
                                    viewModel.setTabOrientationIndex(it)
                                }
                            )
                        }
                    )
                }
            )
        )
        CarListDivider()
        CarListSection(
            sectionTitle = "Other",
            listItems = listOf(
                CarListItem {
                    CarRowSwitch(
                        title = "Set loading state",
                        state = viewModel.carComposeState.isLoading,
                        onStateChange = { viewModel.setLoading(it) }
                    )
                },
            )
        )
        // CarListDivider()
        // CarListSection(
        //     sectionTitle = "Demo List:",
        //     listItems = placeholderRowList(10)
        // )
    }
}

@Composable
internal fun Placeholder(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 40.sp
        )
    }
}