package com.ixam97.carStatsViewer.ui.activities

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.ixam97.carStatsViewer.BuildConfig
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.databinding.ActivityAboutBinding

class AboutActivity : FragmentActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (CarStatsViewer.appPreferences.colorTheme > 0) setTheme(R.style.ColorTestTheme)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)



        binding.aboutButtonBack.setOnClickListener {
            finish()
            if (BuildConfig.FLAVOR_aaos != "carapp")
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        binding.aboutVersionWidget.setOnRowClickListener {
            CarStatsViewer.getChangelogDialog(this, isChangelog = true).show()
        }
        binding.aboutSupportWidget.setOnRowClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.readme_link))))
        }

        binding.aboutForumsWidget.setOnRowClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.polestar_forum_link))))
        }

        binding.aboutClubWidget.setOnRowClickListener() {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.polestar_fans_link))))
        }

        binding.aboutGithubIssuesWidget.setOnRowClickListener() {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_issues_link))))
        }

        binding.aboutLibsWidget.setOnRowClickListener {
            startActivity(Intent(this, LibsActivity::class.java))
            if (BuildConfig.FLAVOR_aaos != "carapp")
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        binding.aboutPrivacyWidget.setOnRowClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.privacy_policy_link))))
        }

        binding.aboutVersionWidget.bottomText = "%s (%s)".format(BuildConfig.VERSION_NAME, BuildConfig.APPLICATION_ID)

        var contributors = ""
        val contributorsArray = resources.getStringArray(R.array.contributors)
        for ((index, contributor) in contributorsArray.withIndex()) {
            contributors += contributor
            if (index < contributorsArray.size -1) contributors += ", "
        }
        binding.aboutContributorsWidget.bottomText = contributors

        binding.aboutTranslatorsWidget.setOnRowClickListener {
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

        binding.aboutSupportersWidget.setOnRowClickListener {
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
    }
}