package com.ixam97.carStatsViewer.carApp

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Header
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import com.ixam97.carStatsViewer.BuildConfig
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.utils.ChangeLogCreator

class ChangesScreen(carContext: CarContext): Screen(carContext) {
    override fun onGetTemplate(): Template {
        return PaneTemplate.Builder(Pane.Builder().apply {
            addRow(messageRow())

            createVersionNoticesRows().forEach {row ->
                addRow(row)
            }

            addAction(Action.Builder().apply {
                setTitle("OK")
                setFlags(Action.FLAG_PRIMARY)
                setOnClickListener {
                    screenManager.pop()
                }
            }.build())
            setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_car_app_upgrade)).build())
        }.build()).apply {
            setHeader(Header.Builder().apply {
                setTitle(carContext.getString(R.string.car_app_changes_title, BuildConfig.VERSION_NAME.dropLast(5)))
                setStartHeaderAction(Action.APP_ICON)
            }.build())
        }.build()
    }

    private fun createVersionNoticesRows(): List<Row> {
        val changesRowList = mutableListOf<Row>()
        val changes = carContext.resources.getStringArray(R.array.changes)
        ChangeLogCreator.createChangelogFromList(changes.toList()).forEach {
            changesRowList.add(Row.Builder().apply {
                setTitle(carContext.getString(R.string.main_changelog_dialog_title, it.key))
                addText(it.value)
            }.build())
        }
        return changesRowList
    }

    private fun messageRow(): Row {
        val message = carContext.getString(
            R.string.car_app_changes_message,
            carContext.getString(R.string.app_name),
            BuildConfig.VERSION_NAME.dropLast(5)
        )
        return Row.Builder().apply {
            setTitle(message)
        }.build()
    }
}