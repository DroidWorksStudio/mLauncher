package com.github.droidworksstudio.mlauncher.ui.components

import android.content.Context
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import com.github.droidworksstudio.mlauncher.data.Constants
import kotlin.math.abs

class GestureManager(
    private val context: Context,
    private val listener: GestureListener
) : GestureDetector.SimpleOnGestureListener() {

    private val gestureDetector = GestureDetector(context, this)
    private var downTime: Long = 0

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
        fun onSingleTap()
        fun onDoubleTap()
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    override fun onDown(event: MotionEvent): Boolean {
        downTime = System.currentTimeMillis()
        Log.d(TAG, "onDown - downTime set to $downTime")
        return true
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        Log.d(TAG, "onSingleTapConfirmed")
        listener.onSingleTap()
        return true
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        Log.d(TAG, "onDoubleTap")
        listener.onDoubleTap()
        return true
    }

    override fun onLongPress(e: MotionEvent) {
        Log.d(TAG, "onLongPress")
        listener.onLongPress()
    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        if (e1 == null) {
            Log.d(TAG, "onFling - e1 is null, returning false")
            return false
        }

        val diffX = e2.x - e1.x
        val diffY = e2.y - e1.y
        val duration = System.currentTimeMillis() - downTime

        val isHorizontalSwipe = abs(diffX) > abs(diffY)

        val direction = if (isHorizontalSwipe) {
            if (diffX > 0) "RIGHT" else "LEFT"
        } else {
            if (diffY > 0) "DOWN" else "UP"
        }

        // Update swipe thresholds based on current context/screen
        Constants.updateSwipeDistanceThreshold(context, direction)

        val velocityThreshold = Constants.SWIPE_VELOCITY_THRESHOLD
        val holdDurationThreshold = Constants.HOLD_DURATION_THRESHOLD

        Log.d(
            TAG, "onFling - direction: $direction, duration: $duration, " +
                    "SHORT threshold: ${Constants.SHORT_SWIPE_THRESHOLD}, LONG threshold: ${Constants.LONG_SWIPE_THRESHOLD}, velocityThreshold: $velocityThreshold"
        )

        val distance = if (isHorizontalSwipe) abs(diffX) else abs(diffY)
        val velocity = if (isHorizontalSwipe) abs(velocityX) else abs(velocityY)

        Log.d(TAG, "Fling - distance: $distance, velocity: $velocity")

        // Check if velocity is sufficient
        if (velocity < velocityThreshold) {
            Log.d(TAG, "Fling velocity below threshold, ignoring fling")
            return false
        }

        val isLongSwipe = distance > Constants.LONG_SWIPE_THRESHOLD
        val isShortSwipe = distance > Constants.SHORT_SWIPE_THRESHOLD && distance <= Constants.LONG_SWIPE_THRESHOLD

        if (isLongSwipe || isShortSwipe) {
            val isLongDuration = duration > holdDurationThreshold
            Log.d(TAG, "Swipe detected - isLongSwipe: $isLongSwipe, isShortSwipe: $isShortSwipe, durationLongEnough: $isLongDuration")

            if (isHorizontalSwipe) {
                if (diffX > 0) {
                    if (isLongSwipe) listener.onLongSwipeRight() else listener.onShortSwipeRight()
                } else {
                    if (isLongSwipe) listener.onLongSwipeLeft() else listener.onShortSwipeLeft()
                }
            } else {
                if (diffY > 0) {
                    if (isLongSwipe) listener.onLongSwipeDown() else listener.onShortSwipeDown()
                } else {
                    if (isLongSwipe) listener.onLongSwipeUp() else listener.onShortSwipeUp()
                }
            }
            return true
        }

        Log.d(TAG, "onFling - No swipe detected")
        return false
    }
}