package com.ixam97.carStatsViewer.carcompose.screen.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carcompose.CarComposeViewModel
import com.ixam97.carStatsViewer.carcompose.VehicleModel
import com.ixam97.carStatsViewer.carcompose.theme.polestar4ContentPadding
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.util.author
import com.mikepenz.aboutlibraries.util.withContext
import de.ixam97.carcompose.components.controls.CarIconButton
import de.ixam97.carcompose.components.controls.CarRow
import de.ixam97.carcompose.components.controls.CarRowBrowsableType
import de.ixam97.carcompose.components.layout.CarColumn
import de.ixam97.carcompose.components.layout.CarLazyColumn
import de.ixam97.carcompose.components.layout.CarListDivider
import de.ixam97.carcompose.components.layout.CarPaneLayout
import de.ixam97.carcompose.theme.CarTheme
import kotlinx.serialization.Serializable

@Serializable
object LicensesScreenNavKey: NavKey

internal data class DialogLibrary(
    val name: String,
    val content: List<String>,
    val urls: Boolean
)

@Composable
fun CarComposeLicensesScreen(
    globalViewModel: CarComposeViewModel,
    backStack: NavBackStack<NavKey>,
) {
    CarPaneLayout(
        headerTitle = stringResource(R.string.about_third_party_licenses),
        headerStartContent = {
            CarIconButton(
                painter = painterResource(R.drawable.ic_arrow_backwards_48),
                tint = CarTheme.carColors.accent,
                onClick = { backStack.removeAt(backStack.lastIndex) }
            )
        }
    ) {
        CarComposeLicensesContent(
            modifier = Modifier.padding(start = if (globalViewModel.carComposeState.vehicleModel == VehicleModel.Polestar4) polestar4ContentPadding else 0.dp) ,
            globalViewModel = globalViewModel,
            backStack = backStack
        )
    }
}

@Composable
fun CarComposeLicensesContent(
    modifier: Modifier = Modifier,
    globalViewModel: CarComposeViewModel,
    backStack: NavBackStack<NavKey>
) {
    val libraries = Libs.Builder().withContext(LocalContext.current).build().libraries
    var dialogLibrary by remember { mutableStateOf<DialogLibrary?>(null)}

    BoxWithConstraints(
        modifier = modifier
    ) {
        val constraintsScope = this
        Row(
            modifier = Modifier
                .fillMaxSize()
        ) {
            if (constraintsScope.maxWidth > 1700.dp) {
                CarLazyColumn(
                    modifier = Modifier.width(constraintsScope.maxWidth / 2f)
                ) {
                    itemsIndexed(items = libraries) { index, lib ->
                        LibRow(
                            lib = lib,
                            setDialogLibrary = { dialogLibrary = it }
                        )
                        if (index < libraries.size - 1)
                            CarListDivider()
                    }
                }
                dialogLibrary?.let {
                    LicenseContent(
                        modifier = Modifier.weight(1f),
                        dialogLibrary = it
                    )
                }
            } else {
                Box() {
                    CarLazyColumn() {
                        itemsIndexed(items = libraries) { index, lib ->
                            LibRow(
                                lib = lib,
                                setDialogLibrary = { dialogLibrary = it }
                            )
                            if (index < libraries.size - 1)
                                CarListDivider()
                        }
                    }
                    dialogLibrary?.let {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.75f))
                                .padding(50.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(CarTheme.carColors.secondarySurface.first())
                            ) {
                                LicenseContent(
                                    dialogLibrary = it,
                                    onCloseClick = { dialogLibrary = null }
                                )
                            }
                        }
                    }
                }
            }
        }
    }


}

@Composable
private fun LicenseContent(
    modifier: Modifier = Modifier,
    dialogLibrary: DialogLibrary,
    onCloseClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
    ) {
        Spacer(Modifier.size(CarTheme.carDimensions.defaultVerticalPadding))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        horizontal = CarTheme.carDimensions.defaultHorizontalPadding,
                        vertical = CarTheme.carDimensions.defaultVerticalPadding
                    ),
                text = dialogLibrary.name,
                style = CarTheme.carTypography.title,)
            onCloseClick?.let {
                Spacer(Modifier.size(CarTheme.carDimensions.defaultHorizontalPadding))
                CarIconButton(
                    imageVector = Icons.Default.Close,
                    onClick = it
                )
                Spacer(Modifier.size(CarTheme.carDimensions.defaultHorizontalPadding))
            }
        }

        CarListDivider()
        CarColumn() {
            Spacer(Modifier.size(CarTheme.carDimensions.defaultVerticalPadding))
            dialogLibrary.content.forEach { licenseContent ->
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            enabled = dialogLibrary.urls
                        ) {
                            if (dialogLibrary.urls) {
                                context.startActivity(Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(licenseContent)
                                ))
                            }
                        }
                        .padding(
                            horizontal = CarTheme.carDimensions.defaultHorizontalPadding,
                            vertical = CarTheme.carDimensions.defaultVerticalPadding
                        ),
                    text = licenseContent,
                    style = CarTheme.carTypography.rowContent
                )
            }
        }
    }
}

@Composable
private fun LibRow(
    lib: Library,
    setDialogLibrary: (DialogLibrary) -> Unit
) {
    CarRow(
        title = lib.name,
        descriptionContent = {
            Column() {
                if (lib.author.isNotBlank()) {
                    Text(
                        text = lib.author,
                        style = CarTheme.carTypography.rowContent,
                        color = LocalContentColor.current.copy(alpha =  0.7f)
                    )
                }
                if (lib.licenses.isNotEmpty()) {
                    var licensesString = ""
                    lib.licenses.forEachIndexed { index, license ->
                        if (index > 0) licensesString += ", "
                        licensesString += license.name
                    }
                    Text(
                        text = licensesString,
                        style = CarTheme.carTypography.rowContent,
                        color = LocalContentColor.current.copy(alpha =  0.7f)
                    )
                }
            }
        },
        browsable = true,
        browsableType = CarRowBrowsableType.Hidden,
        onBrowse = {
            val dialogContent = mutableListOf<String>()
            val dialogUrls = mutableListOf<String>()
            lib.licenses.forEach { license ->
                license.licenseContent?.let {
                    if (it.isNotBlank()) dialogContent.add(it)
                }
                license.url?.let {
                    if (it.isNotBlank()) dialogUrls.add(it)
                }
            }
            if (dialogContent.isNotEmpty()) {

                setDialogLibrary(DialogLibrary(
                    name = lib.name,
                    content = dialogContent,
                    urls = false
                ))
            } else {
                setDialogLibrary(DialogLibrary(
                    name = lib.name,
                    content = dialogUrls,
                    urls = true
                ))
            }
        }
    )
}