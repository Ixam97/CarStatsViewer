package com.ixam97.carStatsViewer.compose.screens.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ixam97.carStatsViewer.compose.DefaultColumnScrollbar
import com.ixam97.carStatsViewer.compose.components.CarRow
import com.ixam97.carStatsViewer.utils.ChangeLogCreator

@Composable
fun Changelog() {
    DefaultColumnScrollbar(
        modifier = Modifier
            .fillMaxSize()
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