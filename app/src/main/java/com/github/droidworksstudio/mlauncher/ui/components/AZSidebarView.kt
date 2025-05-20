package com.github.droidworksstudio.mlauncher.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.github.droidworksstudio.mlauncher.helper.sp2px

class AZSidebarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val letters = ('A'..'Z').toList()
    private val baseTextSizeSp = 20f
    private val selectedTextSizeSp = baseTextSizeSp + 2f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        textSize = sp2px(resources, baseTextSizeSp)
        textAlign = Paint.Align.CENTER
        style = Paint.Style.FILL
    }

    var onLetterSelected: ((Char) -> Unit)? = null

    private var spacingFactor = 1f
    private var selectedIndex: Int = -1
    private val shiftPx = 10 * resources.displayMetrics.density // 10dp

    init {
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels.toFloat()
        val density = displayMetrics.density

        val topBottomPadding = 180 * density
        val interLetterSpacing = (letters.size - 1) * density // 1dp between each letter
        val availableHeight = screenHeight - topBottomPadding - interLetterSpacing

        val baseTextHeight = sp2px(resources, baseTextSizeSp)
        spacingFactor = availableHeight / (letters.size * baseTextHeight)
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val itemHeight = sp2px(resources, baseTextSizeSp) * spacingFactor
        val totalHeight = itemHeight * letters.size
        val startY = (height - totalHeight) / 2f

        letters.forEachIndexed { i, letter ->
            val isSelected = i == selectedIndex

            paint.isFakeBoldText = isSelected
            paint.textSize = sp2px(resources, if (isSelected) selectedTextSizeSp else baseTextSizeSp)

            val x = width / 2f + if (isSelected) shiftPx else 0f
            val y = startY + itemHeight * i - (paint.descent() + paint.ascent()) / 2

            canvas.drawText(letter.toString(), x, y, paint)
        }

        // Reset paint to defaults
        paint.isFakeBoldText = false
        paint.textSize = sp2px(resources, baseTextSizeSp)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val itemHeight = sp2px(resources, baseTextSizeSp) * spacingFactor
        val totalHeight = itemHeight * letters.size
        val startY = (height - totalHeight) / 2f

        val relativeY = event.y - startY
        val index = (relativeY / itemHeight).toInt().coerceIn(0, letters.size - 1)

        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            if (index != selectedIndex) {
                selectedIndex = index
                onLetterSelected?.invoke(letters[index])
                invalidate()
            }
        }

        return true
    }

    private var selectedLetter: Char? = null

    fun setSelectedLetter(letter: Char) {
        val index = letters.indexOf(letter.uppercaseChar())
        if (index != -1 && selectedIndex != index) {
            selectedIndex = index
            selectedLetter = letter
            onLetterSelected?.invoke(letter) // optional: trigger listener
            invalidate()
        }
    }

}