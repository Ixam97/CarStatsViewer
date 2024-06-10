package com.ixam97.carStatsViewer.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.compose.theme.CarTheme

@Composable
fun SideTabLayout(
    modifier: Modifier = Modifier,
    tabs: List<SideTab>,
    tabsColumnWidth: Dp? = null,
    tabsColumnBackground: Color = MaterialTheme.colors.surface
) {
    var selectedIndex by remember { mutableStateOf(0) }
    Box(modifier = modifier) {
        Row {
            Column(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .fillMaxHeight()
                    .background(tabsColumnBackground)
                    .padding(top = 10.dp),
                // verticalArrangement = Arrangement.spacedBy(30.dp)
            ) {
                tabs.forEachIndexed { index, tab ->
                    Text(
                        modifier = Modifier
                            .clickable { selectedIndex = index }
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                            .clip(RoundedCornerShape(CarTheme.buttonCornerRadius))
                            .background(if (index == selectedIndex) MaterialTheme.colors.secondary else Color.Transparent)
                            .padding(CarTheme.buttonPaddingValues)
                            .padding(end = 10.dp),
                        text = tab.tabTitle,
                        style = MaterialTheme.typography.h2,
                        // color = if (index == selectedIndex) MaterialTheme.colors.secondary else Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Visible
                    )
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
                tabs[selectedIndex].tabContent()
            }
        }
    }
}

data class SideTab(
    val tabTitle: String,
    val tabContent: @Composable () -> Unit
)