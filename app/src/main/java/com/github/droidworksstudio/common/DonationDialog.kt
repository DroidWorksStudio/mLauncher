package com.github.droidworksstudio.common

import android.content.Context
import android.content.Intent
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.net.toUri
import com.github.droidworksstudio.mlauncher.R
import com.github.droidworksstudio.mlauncher.helper.FontManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.xmlpull.v1.XmlPullParser

class DonationDialog(private val context: Context) {

    data class DonationLink(
        val name: String,
        val url: String,
        val iconName: String
    )

    private val iconMap = mapOf(
        "github" to R.drawable.ic_donation_github,
        "buymeacoffee" to R.drawable.ic_donation_buymeacoffee,
        "liberapay" to R.drawable.ic_donation_liberapay
    )

    private fun parseDonations(): List<DonationLink> {
        val parser = context.resources.getXml(R.xml.donations)
        val list = mutableListOf<DonationLink>()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "donation") {
                val name = parser.getAttributeValue(null, "name") ?: ""
                val url = parser.getAttributeValue(null, "url") ?: ""
                val icon = parser.getAttributeValue(null, "icon") ?: ""
                list.add(DonationLink(name, url, icon))
            }
            eventType = parser.next()
        }

        return list
    }

    fun show(title: String) {
        val donations = parseDonations()
        val typeface = FontManager.getTypeface(context)

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (16 * context.resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        donations.forEach { donation ->
            val iconResId = iconMap[donation.iconName] ?: 0

            val button = Button(context).apply {
                text = donation.name
                setTypeface(typeface) // ✅ apply custom font
                setCompoundDrawablesWithIntrinsicBounds(iconResId, 0, 0, 0)
                compoundDrawablePadding = (8 * context.resources.displayMetrics.density).toInt()
                setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, donation.url.toUri())
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    }
                }
            }

            layout.addView(button)
        }

        val titleView = TextView(context).apply {
            text = title
            setTypeface(FontManager.getTypeface(context))
            textSize = 20f
            setPadding(32, 32, 32, 16)
        }

        // Build and show dialog with standard MaterialAlertDialogBuilder
        val dialog = MaterialAlertDialogBuilder(context)
            .setCustomTitle(titleView)
            .setTitle(title)
            .setView(layout)
            .setNegativeButton(R.string.close, null)
            .create()

        // ✅ Apply font manually to the dialog title and buttons after it's created
        dialog.setOnShowListener {
            // Buttons
            dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.typeface = typeface
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.typeface = typeface
            dialog.getButton(android.app.AlertDialog.BUTTON_NEUTRAL)?.typeface = typeface
        }

        dialog.show()
    }
}