package com.github.droidworksstudio.launcher.utils

import android.text.Html
import android.text.method.LinkMovementMethod
import android.widget.TextView

class StringUtils {

    fun addEndTextIfNotEmpty(value: String, addition: String): String {
        return if (value.isNotEmpty()) "$value$addition" else value
    }

    fun addStartTextIfNotEmpty(value: String, addition: String): String {
        return if (value.isNotEmpty()) "$addition$value" else value
    }

    fun cleanString(string: String?): String? {
        return string?.replace("[^\\p{L}0-9]".toRegex(), "")
    }

    fun setLink(view: TextView, link: String) {
        view.text = Html.fromHtml(link, Html.FROM_HTML_MODE_LEGACY)
        view.movementMethod = LinkMovementMethod.getInstance()
    }

}