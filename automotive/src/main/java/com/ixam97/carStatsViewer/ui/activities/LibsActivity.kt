package com.ixam97.carStatsViewer.ui.activities

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toolbar.LayoutParams
import androidx.fragment.app.FragmentActivity
import com.airbnb.paris.extensions.style
import com.ixam97.carStatsViewer.BuildConfig
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.databinding.ActivityLibsBinding
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.util.withContext

class LibsActivity: FragmentActivity() {

    private lateinit var binding: ActivityLibsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (CarStatsViewer.appPreferences.colorTheme > 0) setTheme(R.style.ColorTestTheme)
        binding = ActivityLibsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.libsButtonBack.setOnClickListener {
            finish()
            if (BuildConfig.FLAVOR_aaos != "carapp")
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        val libs = Libs.Builder().withContext(applicationContext).build()
        val libraries = libs.libraries
        for ((index, lib) in libraries.withIndex()) {
            val container = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                orientation = LinearLayout.VERTICAL
            }
            val libName = TextView(this).apply {
                style(R.style.menu_row_top_text)
                text = lib.name
                textSize = 30f
            }
            container.addView(libName)

            val licenseWebsite = if (lib.licenses.isNotEmpty()) lib.licenses.first().url else ""
            val libWebsite =
                if (lib.website?.isNotEmpty() == true) lib.website
                else if (licenseWebsite?.isNotEmpty() == true) licenseWebsite
                else ""

            val authorInfo = if (lib.developers.isNotEmpty()) lib.developers[0].name?:"" else ""

            val authorName = TextView(this).apply {
                style(R.style.menu_row_content_text)
                textSize = 20f
                text = authorInfo
            }
            if (authorInfo.isNotEmpty()) container.addView(authorName)

            for (license in lib.licenses) {
                val libLicense = TextView(this).apply {
                    style(R.style.menu_row_content_text)
                    text = license.name
                    textSize = 20f
                }
                if (license.name.isNotEmpty()) container.addView(libLicense)
            }

            if (libWebsite?.isNotEmpty() == true) {
                container.setOnClickListener {
                    startActivity(Intent( Intent.ACTION_VIEW, Uri.parse(libWebsite)))
                }
            }

            val dividerLine = View(this).apply {
                style(R.style.menu_divider_style)
                setBackgroundColor(Color.DKGRAY)
            }

            binding.libsContainer.addView(container)
            if (index < libraries.size - 1) binding.libsContainer.addView(dividerLine)
            else {
                dividerLine.setBackgroundColor(Color.TRANSPARENT)
                binding.libsContainer.addView(dividerLine)
            }
        }
    }
}