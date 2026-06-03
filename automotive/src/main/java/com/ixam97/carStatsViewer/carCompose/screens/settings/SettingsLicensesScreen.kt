package com.ixam97.carStatsViewer.carCompose.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carCompose.deviceIsWideScreen
import com.ixam97.carStatsViewer.carCompose.theme.polestar4ContentPadding
import com.ixam97.carStatsViewer.compose.RowContentText
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
import de.ixam97.carcompose.components.layout.CarListItem
import de.ixam97.carcompose.components.layout.CarPaneLayout
import de.ixam97.carcompose.components.layout.carListSection
import de.ixam97.carcompose.theme.CarTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.serialization.Serializable

@Serializable
object SettingsLicensesScreenNavKey: NavKey

internal data class DialogLibrary(
    val name: String,
    val content: List<String>,
    val urls: Boolean
)

@Composable
fun SettingsLicensesScreen(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit,
) {
    CarPaneLayout(
        headerTitle = stringResource(R.string.about_third_party_licenses),
        onBackAction = onBack
    ) {
        SettingsLicensesContent(
            modifier = Modifier.padding(start = if (deviceIsWideScreen()) polestar4ContentPadding else 0.dp)
        )
    }
}

@Composable
fun SettingsLicensesContent(
    modifier: Modifier = Modifier
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
                LibrariesColumn(
                    modifier = Modifier.width(constraintsScope.maxWidth / 2f),
                    libraries = libraries,
                    setDialogLibrary = { dialogLibrary = it },
                    constraintsScope = constraintsScope
                )
                dialogLibrary?.let {
                    LicenseContent(
                        modifier = Modifier.weight(1f),
                        dialogLibrary = it
                    )
                }
            } else {
                Box() {
                    LibrariesColumn(
                        libraries = libraries,
                        setDialogLibrary = { dialogLibrary = it },
                        constraintsScope = constraintsScope
                    )
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
                                    .background(CarTheme.carColors.secondarySurface)
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
internal fun LibrariesColumn(
    modifier: Modifier = Modifier,
    libraries: ImmutableList<Library>,
    setDialogLibrary: (DialogLibrary) -> Unit,
    constraintsScope: BoxWithConstraintsScope
) {
    CarLazyColumn(modifier) {
        carListSection(
            listItems = libraries.map { lib ->
                CarListItem {
                    LibRow(
                        lib = lib,
                        setDialogLibrary = setDialogLibrary
                    )
                }
            }
        )
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
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(licenseContent)
                                    )
                                )
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
                    RowContentText(text = lib.author)
                }
                if (lib.licenses.isNotEmpty()) {
                    var licensesString = ""
                    lib.licenses.forEachIndexed { index, license ->
                        if (index > 0) licensesString += ", "
                        licensesString += license.name
                    }
                    RowContentText(text = licensesString)
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