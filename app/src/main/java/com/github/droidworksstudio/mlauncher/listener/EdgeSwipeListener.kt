package com.github.droidworksstudio.mlauncher.listener

import android.annotation.SuppressLint
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.github.droidworksstudio.mlauncher.data.Prefs
import com.github.droidworksstudio.mlauncher.services.EdgeService

class EdgeSwipeListener(
    private val isLeftEdge: Boolean = false,
    private val isRightEdge: Boolean = false,
) : View.OnTouchListener {
    private var startX = 0f
    private lateinit var prefs: Prefs

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        event ?: return false

        val context = v?.context as? EdgeService
        context ?: return false  // Ensure context is of EdgeService
        prefs = Prefs(context)

        if (prefs.showEdgePanel) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX
                    return true
                }

                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - startX

                    // Handle the swipe direction to open panels
                    if (isLeftEdge && deltaX >= 100) {  // Swipe right from left edge
                        Log.d("EdgeSwipe", "Swipe LEFT detected from LEFT edge!")
                        context.closePanel()
                        context.openPanel("LEFT")
                        return true
                    }

                    if (isRightEdge && deltaX <= -100) {  // Swipe left from right edge
                        Log.d("EdgeSwipe", "Swipe RIGHT detected from RIGHT edge!")
                        context.closePanel()
                        context.openPanel("RIGHT")
                        return true
                    }
                }
            }
            return false
        }
        return false
    }

}
