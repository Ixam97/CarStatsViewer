package com.ixam97.carStatsViewer.carCompose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.ixam97.carcompose.components.controls.CarIconButton
import de.ixam97.carcompose.theme.CarTheme

/**
 * Value with a decrement and an increment button: [−] value [+]. The buttons are CarIconButtons,
 * whose touch target (1.6 × icon size) is kept free from the value text. Only depends on Compose
 * and CarTheme, so it can be moved into the CarCompose library.
 */
@Composable
fun CarStepper(
    valueText: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
    decrementEnabled: Boolean = true,
    incrementEnabled: Boolean = true,
) {
    val touchTargetSize = CarTheme.carDimensions.iconButtonSize * 1.6f
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(touchTargetSize), contentAlignment = Alignment.Center) {
            CarIconButton(
                imageVector = Icons.Default.Remove,
                enabled = decrementEnabled,
                onClick = onDecrement
            )
        }
        Text(
            modifier = Modifier.widthIn(min = 160.dp),
            text = valueText,
            textAlign = TextAlign.Center,
            style = CarTheme.carTypography.rowTitle
        )
        Box(Modifier.size(touchTargetSize), contentAlignment = Alignment.Center) {
            CarIconButton(
                imageVector = Icons.Default.Add,
                enabled = incrementEnabled,
                onClick = onIncrement
            )
        }
    }
}
