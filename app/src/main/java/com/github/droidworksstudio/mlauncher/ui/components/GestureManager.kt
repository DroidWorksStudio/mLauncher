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

                if (longPressTriggered) {
                    isDragging = false
                    handled = true
                }

                val diffX = event.x - initialX
                val diffY = event.y - initialY
                val absDiffX = abs(diffX)
                val absDiffY = abs(diffY)

                // Double-tap detection
                val currentTime = System.currentTimeMillis()
                val timeSinceLastTap = currentTime - lastTapTime
                val dxTap = abs(event.x - lastTapX)
                val dyTap = abs(event.y - lastTapY)

                if (timeSinceLastTap <= doubleTapTimeout && dxTap <= doubleTapSlop && dyTap <= doubleTapSlop) {
                    AppLogger.d(TAG, "Detected Double Tap")
                    listener.onDoubleTap()
                    lastTapTime = 0
                    handled = true
                    isDragging = false
                } else {
                    lastTapTime = currentTime
                    lastTapX = event.x
                    lastTapY = event.y
                }

                // 🛑 Ignore tiny taps (after double-tap check)
                if (absDiffX < 1f && absDiffY < 1f) {
                    AppLogger.d(TAG, "Ignored tap with no movement")
                    isDragging = false
                }

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

                    val swipeDistanceThresholdPx = Constants.SWIPE_DISTANCE_THRESHOLD
                    AppLogger.d(TAG, "ACTION_UP - diffX: $diffX, diffY: $diffY")
                    AppLogger.d(TAG, "Threshold - Distance: $swipeDistanceThresholdPx px")

                    if (absDiffX > absDiffY) {
                        val isLong = absDiffX >= swipeDistanceThresholdPx
                        if (diffX > 0) {
                            if (isLong) {
                                AppLogger.d(TAG, "Detected Long Swipe RIGHT")
                                listener.onLongSwipeRight()
                            } else {
                                AppLogger.d(TAG, "Detected Short Swipe RIGHT")
                                listener.onShortSwipeRight()
                            }
                        } else {
                            if (isLong) {
                                AppLogger.d(TAG, "Detected Long Swipe LEFT")
                                listener.onLongSwipeLeft()
                            } else {
                                AppLogger.d(TAG, "Detected Short Swipe LEFT")
                                listener.onShortSwipeLeft()
                            }
                        }
                    } else {
                        val isLong = absDiffY >= swipeDistanceThresholdPx
                        if (diffY > 0) {
                            if (isLong) {
                                AppLogger.d(TAG, "Detected Long Swipe DOWN")
                                listener.onLongSwipeDown()
                            } else {
                                AppLogger.d(TAG, "Detected Short Swipe DOWN")
                                listener.onShortSwipeDown()
                            }
                        } else {
                            if (isLong) {
                                AppLogger.d(TAG, "Detected Long Swipe UP")
                                listener.onLongSwipeUp()
                            } else {
                                AppLogger.d(TAG, "Detected Short Swipe UP")
                                listener.onShortSwipeUp()
                            }
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