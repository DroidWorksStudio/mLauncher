package com.github.droidworksstudio.mlauncher.ui.components

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import com.github.droidworksstudio.common.AppLogger
import com.github.droidworksstudio.mlauncher.data.Constants
import kotlin.math.abs

class GestureManager(
    private val context: Context,
    private val listener: GestureListener
) {

    private var downTime: Long = 0
    private var initialX: Float = 0f
    private var initialY: Float = 0f
    private var isDragging = false

    // Double tap detection
    private var lastTapTime = 0L
    private var lastTapX = 0f
    private var lastTapY = 0f
    private val doubleTapTimeout = 300L
    private val doubleTapSlop = 100f

    // Long press detection
    private val longPressTimeout = 500L
    private val longPressHandler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null
    private var longPressTriggered = false

    companion object {
        private const val TAG = "GestureManager"
    }

    interface GestureListener {
        fun onShortSwipeLeft()
        fun onLongSwipeLeft()
        fun onShortSwipeRight()
        fun onLongSwipeRight()
        fun onShortSwipeUp()
        fun onLongSwipeUp()
        fun onShortSwipeDown()
        fun onLongSwipeDown()
        fun onLongPress()
        fun onDoubleTap()
        fun onSingleTap()
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        var handled = false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialX = event.x
                initialY = event.y
                downTime = System.currentTimeMillis()
                isDragging = true
                longPressTriggered = false

                longPressRunnable = Runnable {
                    longPressTriggered = true
                    AppLogger.d(TAG, "Detected Long Press")
                    listener.onLongPress()
                }
                longPressHandler.postDelayed(longPressRunnable!!, longPressTimeout)

                AppLogger.d(TAG, "ACTION_DOWN - InitialX: $initialX, InitialY: $initialY, downTime: $downTime")
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = abs(event.x - initialX)
                val dy = abs(event.y - initialY)
                if (dx > doubleTapSlop || dy > doubleTapSlop) {
                    longPressHandler.removeCallbacks(longPressRunnable!!)
                }
            }

            MotionEvent.ACTION_UP -> {
                longPressHandler.removeCallbacks(longPressRunnable!!)

                val diffX = event.x - initialX
                val diffY = event.y - initialY
                val absDiffX = abs(diffX)
                val absDiffY = abs(diffY)

                val currentTime = System.currentTimeMillis()
                val timeSinceLastTap = currentTime - lastTapTime
                val dxTap = abs(event.x - lastTapX)
                val dyTap = abs(event.y - lastTapY)

                if (longPressTriggered) {
                    isDragging = false
                    handled = true
                    return true
                }

                // ✅ Double Tap
                if (timeSinceLastTap <= doubleTapTimeout && dxTap <= doubleTapSlop && dyTap <= doubleTapSlop) {
                    AppLogger.d(TAG, "Detected Double Tap")
                    listener.onDoubleTap()
                    lastTapTime = 0
                    isDragging = false
                    handled = true
                    return true
                } else {
                    lastTapTime = currentTime
                    lastTapX = event.x
                    lastTapY = event.y
                }

                // ✅ Add Single Tap logic
                if (absDiffX < 10f && absDiffY < 10f) {
                    AppLogger.d(TAG, "Detected Single Tap")
                    listener.onSingleTap()
                    handled = true
                    isDragging = false
                    return true
                }

                // 🛑 Continue with swipe logic...
                if (isDragging) {
                    val direction = when {
                        absDiffX > absDiffY && diffX > 0 -> "RIGHT"
                        absDiffX > absDiffY && diffX < 0 -> "LEFT"
                        absDiffY > absDiffX && diffY > 0 -> "DOWN"
                        absDiffY > absDiffX && diffY < 0 -> "UP"
                        else -> "UNKNOWN"
                    }

                    if (direction != "UNKNOWN") {
                        Constants.updateSwipeDistanceThreshold(context, direction)
                        AppLogger.d(TAG, "Swipe detected - Direction: $direction")
                    }

                    val longThreshold = Constants.LONG_SWIPE_THRESHOLD
                    val shortThreshold = Constants.SHORT_SWIPE_THRESHOLD

                    AppLogger.d(TAG, "ACTION_UP - diffX: $diffX, diffY: $diffY")
                    AppLogger.d(TAG, "Short Threshold: $shortThreshold px, Long Threshold: $longThreshold px")

                    if (absDiffX > absDiffY) {
                        when {
                            absDiffX >= longThreshold -> {
                                if (diffX > 0) listener.onLongSwipeRight()
                                else listener.onLongSwipeLeft()
                            }

                            absDiffX >= shortThreshold -> {
                                if (diffX > 0) listener.onShortSwipeRight()
                                else listener.onShortSwipeLeft()
                            }

                            else -> AppLogger.d(TAG, "Swipe too short — ignored")
                        }
                    } else {
                        when {
                            absDiffY >= longThreshold -> {
                                if (diffY > 0) listener.onLongSwipeDown()
                                else listener.onLongSwipeUp()
                            }

                            absDiffY >= shortThreshold -> {
                                if (diffY > 0) listener.onShortSwipeDown()
                                else listener.onShortSwipeUp()
                            }

                            else -> AppLogger.d(TAG, "Swipe too short — ignored")
                        }
                    }

                    handled = true
                    isDragging = false
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                longPressHandler.removeCallbacks(longPressRunnable!!)
                isDragging = false
            }
        }

        return handled
    }
}