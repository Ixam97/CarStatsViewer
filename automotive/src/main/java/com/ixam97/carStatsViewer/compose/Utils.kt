package com.ixam97.carStatsViewer.compose

import android.os.Build
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.compose.theme.ColorTheme
import com.ixam97.carStatsViewer.compose.theme.polestarOrange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import my.nanihadesuka.compose.ColumnScrollbar
import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSettings

fun brandSelector(themeSetting: Int): String? = when (CarStatsViewer.appPreferences.colorTheme) {
    ColorTheme.OEM -> {
        if (Build.MODEL == "Polestar 2") Build.MODEL
        else if (CarStatsViewer.dataProcessor.staticVehicleData.modelName == "PS2") "Polestar 2"
        else Build.BRAND
    }
    ColorTheme.ORANGE -> "Orange"
    ColorTheme.BLUE -> "Blue"
    else -> null
}

val DefaultScrollbarSettings = ScrollbarSettings(
    scrollbarPadding = 4.dp,
    thumbUnselectedColor = Color.White.copy(alpha = 0.5f),
    thumbSelectedColor = polestarOrange,
    thumbThickness = 6.dp,
    hideDisplacement = 0.dp,
    thumbShape = RectangleShape,
    alwaysShowScrollbar = true
)

@Composable
fun DefaultLazyListScrollbar(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    reverseLayout: Boolean = false,
    content: (LazyListScope.() -> Unit)
) {
    var isInit by remember { mutableStateOf(true) }
    val primaryColor = MaterialTheme.colors.primary

    val isScrollable = rememberUpdatedState(state.canScrollForward || state.canScrollBackward)
    val scrollbarSettings by rememberUpdatedState {
        DefaultScrollbarSettings.copy(
            enabled = isScrollable.value,
            alwaysShowScrollbar = isInit,
            thumbSelectedColor = primaryColor
        )
    }

    LaunchedEffect(null) {
        CoroutineScope(Dispatchers.Default).launch {
            delay(1000)
            isInit = false
        }
    }

    LazyColumnScrollbar(
        state = state,
        settings = scrollbarSettings()
    ) {
        LazyColumn(
            modifier = modifier,
            state = state,
            reverseLayout = reverseLayout,
            content = content
        )
    }
}

@Composable
fun DefaultColumnScrollbar(
    modifier: Modifier = Modifier,
    state: ScrollState = rememberScrollState(),
    content: @Composable (ColumnScope.() -> Unit)
) {

    var isInit by remember { mutableStateOf(true) }
    val primaryColor = MaterialTheme.colors.primary

    val isScrollable = rememberUpdatedState(state.canScrollForward || state.canScrollBackward)
    val scrollbarSettings by rememberUpdatedState {
        DefaultScrollbarSettings.copy(
            enabled = isScrollable.value,
            alwaysShowScrollbar = isInit,
            thumbSelectedColor = primaryColor
        )
    }

    LaunchedEffect(null) {
        CoroutineScope(Dispatchers.Default).launch {
            delay(1000)
            isInit = false
        }
    }

    ColumnScrollbar(
        state = state,
        settings = scrollbarSettings()
    ) {
        Column(
            modifier = modifier
                .verticalScroll(state),
            content = content
        )
    }
}