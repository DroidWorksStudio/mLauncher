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

        Constants.updateSwipeDistanceThreshold(context, direction)

        val density = context.resources.displayMetrics.density
        val swipeDistanceThresholdPx = Constants.SWIPE_DISTANCE_THRESHOLD * density
        val swipeVelocityThreshold = Constants.SWIPE_VELOCITY_THRESHOLD
        val holdDurationThreshold = Constants.HOLD_DURATION_THRESHOLD

        Log.d(
            TAG, "onFling - direction: $direction, duration: $duration, " +
                    "distanceThreshold(px): $swipeDistanceThresholdPx, velocityThreshold: $swipeVelocityThreshold"
        )

        if (isHorizontalSwipe) {
            val distance = abs(diffX)
            val velocity = abs(velocityX)

            Log.d(TAG, "Horizontal fling - distance: $distance, velocity: $velocity")

            if (distance > swipeDistanceThresholdPx && velocity > swipeVelocityThreshold) {
                val isLong = duration > holdDurationThreshold
                Log.d(TAG, "Horizontal fling isLong: $isLong")

                if (diffX > 0) {
                    if (isLong) listener.onLongSwipeRight() else listener.onShortSwipeRight()
                } else {
                    if (isLong) listener.onLongSwipeLeft() else listener.onShortSwipeLeft()
                }
                return true
            }
        } else {
            val distance = abs(diffY)
            val velocity = abs(velocityY)

            Log.d(TAG, "Vertical fling - distance: $distance, velocity: $velocity")

            if (distance > swipeDistanceThresholdPx && velocity > swipeVelocityThreshold) {
                val isLong = duration > holdDurationThreshold
                Log.d(TAG, "Vertical fling isLong: $isLong")

                if (diffY > 0) {
                    if (isLong) listener.onLongSwipeDown() else listener.onShortSwipeDown()
                } else {
                    if (isLong) listener.onLongSwipeUp() else listener.onShortSwipeUp()
                }
                return true
            }
        }

        Log.d(TAG, "onFling - No swipe detected")
        return false
    }
}