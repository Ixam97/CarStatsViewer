package com.ixam97.carStatsViewer.compose.components

import android.util.Log
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ixam97.carStatsViewer.compose.DefaultColumnScrollbar
import com.ixam97.carStatsViewer.compose.theme.CarTheme
import com.ixam97.carStatsViewer.compose.theme.LocalBrushes

@Composable
fun SideTabLayout(
    modifier: Modifier = Modifier,
    tabs: List<SideTab>,
    topLevelBackAction: () -> Unit,
    topLevelTitle: String,
    tabsColumnBackground: Color = MaterialTheme.colors.surface,
    navController: NavHostController = rememberNavController()
) {
    // val navController = rememberNavController()

    var size by remember {
        mutableStateOf(IntSize.Zero)
    }


    var selectedIndex by remember { mutableStateOf(0) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
    ) {
        val width = with(LocalDensity.current){ size.width.toDp() }
        Log.d("WINDOW SIZE", "$width dp")

        if (width < 1300.dp) {
            // Nested navigation for slim displays
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                NavHost(
                    navController = navController,
                    startDestination = "Parent",
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(250)) },
                    exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(250)) },
                    popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(250)) },
                    popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(250)) }
                ) {
                    composable("Parent") {
                        Column {
                            CarHeader(title = topLevelTitle, onBackClick = topLevelBackAction)
                            Column {
                                tabs.filter { it.type == SideTab.Type.Tab }.forEachIndexed { index, tab ->
                                    if (tab.enabled) {
                                        if (index > 0) Divider(
                                            modifier = Modifier.padding(
                                                horizontal = 24.dp
                                            )
                                        )
                                        CarRow(
                                            title = tab.tabTitle,
                                            onClick = { navController.navigate(tab.route) },
                                            browsable = true,
                                            iconImageVector = tab.tabIcon
                                        )
                                    }
                                }
                            }
                        }
                    }
                    tabs.forEach { tab ->
                        composable(tab.route) {
                            Column {
                                CarHeader(title = tab.tabTitle, onBackClick = { navController.popBackStack() })
                                tab.content(navController)
                            }
                        }
                    }
                }
            }
        } else {
            // Wide screens allow for side Tabs
            Row {
                Column(
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .defaultMinSize(minWidth = 500.dp)
                        .fillMaxHeight()
                        .background(tabsColumnBackground)
                        // .padding(top = 10.dp),
                    // verticalArrangement = Arrangement.spacedBy(30.dp)
                ) {
                    CarHeader(
                        title = topLevelTitle,
                        onBackClick = topLevelBackAction
                    )
                    DefaultColumnScrollbar {

                        Spacer(modifier = Modifier.size(30.dp))
                        tabs.filter{it.type == SideTab.Type.Tab}.forEachIndexed { index, tab ->
                            if (tab.enabled) {
                                Row(
                                    modifier = Modifier
                                        .clickable {
                                            selectedIndex = index
                                            navController.navigate(tab.route) {
                                                navController.popBackStack()
                                            }
                                        }
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 10.dp)
                                        .clip(RoundedCornerShape(CarTheme.buttonCornerRadius))
                                        .background(if (index == selectedIndex) MaterialTheme.colors.secondary else Color.Transparent)
                                        .padding(CarTheme.buttonPaddingValues)
                                        .padding(end = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (tab.tabIcon != null) {
                                        Icon(
                                            modifier = Modifier
                                                .heightIn(min = 50.dp)
                                                .width(50.dp),
                                            imageVector = tab.tabIcon,
                                            contentDescription = null,
                                            tint = MaterialTheme.colors.onBackground
                                        )
                                        Spacer(Modifier.size(24.dp))
                                    }
                                    Text(

                                        text = tab.tabTitle,
                                        style = MaterialTheme.typography.h2,
                                        // color = if (index == selectedIndex) MaterialTheme.colors.secondary else Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Visible
                                    )
                                }
                            }
                        }
                    }
                }
                // Divider(
                //     modifier = Modifier
                //         .padding(20.dp)
                //         .fillMaxHeight()
                //         .width(2.dp)
                // )
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = tabs.filter { it.type == SideTab.Type.Tab }[0].route,
                        enterTransition = { fadeIn() },
                        exitTransition = { fadeOut() },
                        popEnterTransition = { fadeIn() },
                        popExitTransition = { fadeOut() }
                    ) {
                        tabs.forEach { tab ->
                            composable(tab.route) {
                                Column {
                                    if (tab.type == SideTab.Type.Tab) {
                                        CarHeader(
                                            title = tab.tabTitle,
                                            headerLineBrush = LocalBrushes.current.headerLineBrush
                                        )
                                    }
                                    else {
                                        CarHeader(
                                            title = tab.tabTitle,
                                            onBackClick = { navController.popBackStack() },
                                            headerLineBrush = LocalBrushes.current.headerLineBrush
                                        )
                                    }
                                    tab.content(navController)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class SideTab(
    val tabTitle: String,
    val tabIcon: ImageVector? = null,
    val route: String,
    val type: Type,
    val content: @Composable (navController: NavController) -> Unit,
    val enabled: Boolean = true
) {
    sealed class Type() {
        object Tab: Type()
        object Detail: Type()
    }
}