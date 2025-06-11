package com.github.creativecodecat.components.views

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.droidworksstudio.common.AppLogger
import com.github.droidworksstudio.mlauncher.helper.CustomFontView
import com.github.droidworksstudio.mlauncher.helper.FontManager

class FontRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : RecyclerView(context, attrs), CustomFontView {

    init {
        try {
            FontManager.register(this)
            applyFont(FontManager.getTypeface(context)) // Apply initial font
        } catch (e: Exception) {
            AppLogger.e("FontRecyclerView", "Initialization failed", e)
        }
    }

    override fun applyFont(typeface: Typeface?) {
        try {
            // Propagate font to all visible child TextViews
            for (i in 0 until childCount) {
                val holder = getChildAt(i)
                applyFontRecursively(holder, typeface)
            }
        } catch (e: Exception) {
            AppLogger.e("FontRecyclerView", "Font application failed", e)
        }
    }

    private fun applyFontRecursively(view: View?, typeface: Typeface?) {
        if (view == null || typeface == null) return

        when (view) {
            is android.widget.TextView -> {
                // force override material theming
                view.setTypeface(typeface, Typeface.NORMAL)
            }

            is ViewGroup -> {
                for (i in 0 until view.childCount) {
                    applyFontRecursively(view.getChildAt(i), typeface)
                }
            }
        }
    }

}