package com.ixam97.carStatsViewer.ui.activities

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.ixam97.carStatsViewer.BuildConfig
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.utils.applyTypeface
import kotlinx.android.synthetic.main.activity_about.*

class AboutActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        about_button_back.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        about_version_widget.setOnRowClickListener {
            CarStatsViewer.getChangelogDialog(this).show()
        }
        about_support_widget.setOnRowClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.readme_link))))
        }

        about_support_widget.setOnRowClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.readme_link))))
        }

        about_forums_widget.setOnRowClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.polestar_forum_link))))
        }

        about_club_widget.setOnRowClickListener() {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.polestar_fans_link))))
        }

        about_github_issues_widget.setOnRowClickListener() {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_issues_link))))
        }

        about_libs_widget.setOnRowClickListener {
            startActivity(Intent(this, LibsActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        about_version_widget.bottomText = "%s (%s)".format(BuildConfig.VERSION_NAME, BuildConfig.APPLICATION_ID)

        var contributors = ""
        val contributorsArray = resources.getStringArray(R.array.contributors)
        for ((index, contributor) in contributorsArray.withIndex()) {
            contributors += contributor
            if (index < contributorsArray.size -1) contributors += ", "
        }
        about_contributors_widget.bottomText = contributors


        about_translators_widget.setOnRowClickListener {
            val translatorsDialog = AlertDialog.Builder(this).apply {
                setPositiveButton(getString(R.string.dialog_close)) { dialog, _ ->
                    dialog.cancel()
                }
                setTitle(getString(R.string.about_translators))
                val translatorsArray = resources.getStringArray(R.array.translators)
                var translators = ""
                for ((index, translator) in translatorsArray.withIndex()) {
                    translators += translator
                    if (index < translatorsArray.size - 1) translators += ", "
                }
                setMessage(translators)
                setCancelable(true)
                create()
            }
            translatorsDialog.show()
        }

        about_supporters_widget.setOnRowClickListener {
            val supportersDialog = AlertDialog.Builder(this).apply {
                setPositiveButton(getString(R.string.dialog_close)) { dialog, _ ->
                    dialog.cancel()
                }
                setTitle(getString(R.string.about_thank_you))
                val supportersArray = resources.getStringArray(R.array.supporters)
                var supporters = getString(R.string.about_supporters_message) + "\n\n"
                for ((index, supporter) in supportersArray.withIndex()) {
                    supporters += supporter
                    if (index < supportersArray.size - 1) supporters += ", "
                }
                setMessage(supporters)
                setCancelable(true)
                create()
            }
            supportersDialog.show()
        }

        CarStatsViewer.typefaceMedium?.let {
            applyTypeface(about_activity)
        }

    }
}