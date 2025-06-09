package com.github.creativecodecat.components.views

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.widget.TextClock
import com.github.droidworksstudio.mlauncher.helper.CustomFontView
import com.github.droidworksstudio.mlauncher.helper.FontManager

class FontTextClock @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : TextClock(context, attrs), CustomFontView {

    init {
        FontManager.register(this)
    }

    override fun applyFont(typeface: Typeface?) {
        this.typeface = typeface
    }
}


