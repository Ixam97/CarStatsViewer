package com.ixam97.carStatsViewer.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toolbar.LayoutParams
import com.airbnb.paris.extensions.style
import com.ixam97.carStatsViewer.R
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.util.toStringArray
import kotlinx.android.synthetic.main.activity_libs.*

class LibsActivity: Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_libs)

        libs_button_back.setOnClickListener {
            finish()
        }

        val libraries = Libs(this, R.string::class.java.fields.toStringArray()).libraries
        for ((index, lib) in libraries.withIndex()) {
            val container = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                orientation = LinearLayout.VERTICAL
            }
            val libName = TextView(this).apply {
                style(R.style.menu_row_top_text)
                text = lib.libraryName
                textSize = 30f
            }
            container.addView(libName)

            val licenseWebsite = if (lib.licenses != null) lib.licenses!!.first().licenseWebsite else ""
            val libWebsite =
                if (lib.authorWebsite.isNotEmpty()) lib.authorWebsite
                else if (lib.libraryWebsite.isNotEmpty()) lib.libraryWebsite
                else if (licenseWebsite.isNotEmpty()) licenseWebsite
                else ""

            val authorInfo = lib.author + if (libWebsite.isNotEmpty()) " | $libWebsite" else ""

            val authorName = TextView(this).apply {
                style(R.style.menu_row_content_text)
                textSize = 20f
                text = authorInfo
            }
            if (lib.author.isNotEmpty()) container.addView(authorName)

            if (lib.licenses != null) {
                for (license in lib.licenses!!) {
                    val libLicense = TextView(this).apply {
                        style(R.style.menu_row_content_text)
                        text = license.licenseName
                        textSize = 20f
                    }
                    if (license.licenseName.isNotEmpty()) container.addView(libLicense)
                }
            }


            val dividerLine = View(this).apply {
                style(R.style.menu_divider_style)
                setBackgroundColor(Color.DKGRAY)
            }
            if (libWebsite.isNotEmpty()) {
                container.setOnClickListener {
                    Log.i("LibLink", libWebsite)
                    startActivity(Intent( Intent.ACTION_VIEW, Uri.parse(libWebsite)))
                }
            }

            libs_container.addView(container)
            if (index < libraries.size - 1) libs_container.addView(dividerLine)
            else {
                dividerLine.setBackgroundColor(Color.TRANSPARENT)
                libs_container.addView(dividerLine)
            }
        }
    }
}