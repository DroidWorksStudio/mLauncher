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

    /** Create a Regex pattern for simple Fuzzy search
     *
     * Example:
     * 'cl' will create 'c.*l' which matches 'Clock', 'Calendar'
     * 'msg' will create 'm.*s.*g' which matches 'Messages'
     * 'cmr' will create 'c.*m.*r' which matches 'Camera'
     */
    fun getFuzzyPattern(query: String): Regex {
        val pattern = query
            .flatMap { char -> listOf(char.toString(), ".*") }
            // remove the last unnecessary .* since the char itself is sufficient
            .dropLast(1)
            .joinToString(separator = "")
        return Regex(pattern, RegexOption.IGNORE_CASE)
    }
}