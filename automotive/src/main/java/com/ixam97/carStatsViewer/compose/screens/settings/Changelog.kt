package com.ixam97.carStatsViewer.compose.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ixam97.carStatsViewer.compose.components.CarRow
import com.ixam97.carStatsViewer.utils.ChangeLogCreator

@Composable
fun Changelog() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        val changelogMap = ChangeLogCreator.createChangelog(LocalContext.current)

        changelogMap.forEach { (version, changes) ->
            CarRow(
                title = version,
                text = changes
            )
        }
    }
}