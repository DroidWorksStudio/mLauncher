package com.github.droidworksstudio.mlauncher.listener

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import com.github.droidworksstudio.common.AppLogger
import com.github.droidworksstudio.mlauncher.data.Constants
import kotlin.math.abs

class GestureManager(
    private val context: Context,
    private val listener: GestureListener
) : GestureDetector.SimpleOnGestureListener() {

    private val gestureDetector = GestureDetector(context, this)
    private var downTime: Long = 0
    private var isScrolling = false
    private var initialEvent: MotionEvent? = null

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
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downTime = System.currentTimeMillis()
                isScrolling = false
                initialEvent?.recycle()
                initialEvent = MotionEvent.obtain(event)
            }

            MotionEvent.ACTION_UP -> {
                if (isScrolling && initialEvent != null) {
                    handleScrollFinished(initialEvent!!, event)
                    isScrolling = false
                    initialEvent?.recycle()
                    initialEvent = null
                }
            }
        }
        return gestureDetector.onTouchEvent(event)
    }

    override fun onDown(event: MotionEvent): Boolean {
        downTime = System.currentTimeMillis()
        AppLogger.d(TAG, "onDown - downTime set to $downTime")
        return true
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        AppLogger.d(TAG, "onSingleTapConfirmed")
        listener.onSingleTap()
        return true
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        AppLogger.d(TAG, "onDoubleTap")
        listener.onDoubleTap()
        return true
    }

    override fun onLongPress(e: MotionEvent) {
        AppLogger.d(TAG, "onLongPress")
        listener.onLongPress()
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        isScrolling = true
        return true
    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        if (e1 == null) {
            AppLogger.d(TAG, "onFling - e1 is null, returning false")
            return false
        }

        val duration = System.currentTimeMillis() - downTime
        val isHorizontal = abs(e2.x - e1.x) > abs(e2.y - e1.y)
        val velocity = if (isHorizontal) abs(velocityX) else abs(velocityY)

        return detectSwipeGesture(e1, e2, duration, velocity)
    }

    private fun handleScrollFinished(e1: MotionEvent, e2: MotionEvent) {
        val duration = System.currentTimeMillis() - downTime
        detectSwipeGesture(e1, e2, duration)
    }

    private fun detectSwipeGesture(
        startEvent: MotionEvent,
        endEvent: MotionEvent,
        duration: Long,
        velocity: Float? = null
    ): Boolean {
        val diffX = endEvent.x - startEvent.x
        val diffY = endEvent.y - startEvent.y
        val isHorizontalSwipe = abs(diffX) > abs(diffY)

        val direction = if (isHorizontalSwipe) {
            if (diffX > 0) "RIGHT" else "LEFT"
        } else {
            if (diffY > 0) "DOWN" else "UP"
        }

        Constants.updateSwipeDistanceThreshold(context, direction)

        val shortThreshold = Constants.SHORT_SWIPE_THRESHOLD
        val longThreshold = Constants.LONG_SWIPE_THRESHOLD
        val velocityThreshold = Constants.SWIPE_VELOCITY_THRESHOLD

        val distance = if (isHorizontalSwipe) (abs(diffX)/Constants.USR_DPIX) else (abs(diffY)/Constants.USR_DPIY)
        val isLongSwipe = distance > longThreshold
        val isShortSwipe = distance in (shortThreshold + 1)..longThreshold


        AppLogger.d(
            TAG, "detectSwipeGesture - direction: $direction, distance: $distance, duration: $duration, " +
                    "velocity: $velocity, shortThreshold: $shortThreshold, longThreshold: $longThreshold"
        )

        when {
            isLongSwipe -> AppLogger.d(TAG, "Swipe type: Long swipe")
            isShortSwipe -> AppLogger.d(TAG, "Swipe type: Short swipe")
            else -> AppLogger.d(TAG, "Swipe type: Too short to classify")
        }

        if (velocity != null && velocity < velocityThreshold) {
            AppLogger.d(TAG, "Velocity too low for fling, ignoring gesture.")
            return false
        }

        if (isShortSwipe || isLongSwipe) {
            when {
                isHorizontalSwipe && diffX > 0 ->
                    if (isLongSwipe) listener.onLongSwipeRight() else listener.onShortSwipeRight()

                isHorizontalSwipe && diffX < 0 ->
                    if (isLongSwipe) listener.onLongSwipeLeft() else listener.onShortSwipeLeft()

                !isHorizontalSwipe && diffY > 0 ->
                    if (isLongSwipe) listener.onLongSwipeDown() else listener.onShortSwipeDown()

                !isHorizontalSwipe && diffY < 0 ->
                    if (isLongSwipe) listener.onLongSwipeUp() else listener.onShortSwipeUp()
            }
            return true
        }

        AppLogger.d(TAG, "detectSwipeGesture - No swipe detected.")
        return false
    }
}
