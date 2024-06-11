package com.ixam97.carStatsViewer.compose.screens.settingsScreens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ixam97.carStatsViewer.compose.components.CarGradientButton

@Composable
fun ChangelogScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .padding(vertical = 15.dp, horizontal = 24.dp)
    ) {
        CarGradientButton(onClick = { navController.popBackStack() }) {
            Text(text = "Back")
        }
    }
}