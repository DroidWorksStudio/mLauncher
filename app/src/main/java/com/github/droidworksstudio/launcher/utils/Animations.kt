package com.github.droidworksstudio.launcher.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.view.View
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import com.github.droidworksstudio.launcher.settings.SharedPreferenceManager

class Animations(context: Context) {

    private val sharedPreferenceManager = SharedPreferenceManager(context)
    var isInAnim = false

    // fadeViewIn and fadeViewOut are for smaller item transitions, such as the action menu

    fun fadeViewIn(view: View) {
        view.fadeIn()
    }

    fun fadeViewOut(view: View) {
        view.fadeOut()
    }

    fun showHome(homeView: View, appView: View, duration: Long) {
        appView.slideOutToBottom(duration)
        homeView.fadeIn(duration)
    }

    fun showApps(homeView: View, appView: View) {
        appView.slideInFromBottom()
        homeView.fadeOut()
    }

    fun backgroundIn(activity: Activity) {
        val originalColor = sharedPreferenceManager.getBgColor()

        // Only animate darkness onto the transparent background
        if (originalColor == "#00000000".toColorInt()) {
            val newColor = "#3F000000".toColorInt()

            val backgroundColorAnimator = ValueAnimator.ofObject(ArgbEvaluator(), originalColor, newColor)
            backgroundColorAnimator.addUpdateListener { animator ->
                activity.window.decorView.setBackgroundColor(animator.animatedValue as Int)
            }

            val duration = sharedPreferenceManager.getAnimationSpeed()
            backgroundColorAnimator.duration = duration

            backgroundColorAnimator.start()
        } else {
            return
        }
    }

    fun backgroundOut(activity: Activity, duration: Long) {
        val newColor = sharedPreferenceManager.getBgColor()

        // Only animate darkness onto the transparent background
        if (newColor == "#00000000".toColorInt()) {
            val originalColor = "#3F000000".toColorInt()

            val backgroundColorAnimator = ValueAnimator.ofObject(ArgbEvaluator(), originalColor, newColor)
            backgroundColorAnimator.addUpdateListener { animator ->
                activity.window.decorView.setBackgroundColor(animator.animatedValue as Int)
            }

            backgroundColorAnimator.duration = duration

            backgroundColorAnimator.start()
        } else {
            return
        }
    }

    private fun View.slideInFromBottom() {
        if (visibility != View.VISIBLE) {
            translationY = height.toFloat() / 5
            scaleY = 1.2f
            alpha = 0f
            visibility = View.VISIBLE
            val duration = sharedPreferenceManager.getAnimationSpeed()

            animate()
                .translationY(0f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(duration)
                .setListener(null)
        }
    }

    private fun View.slideOutToBottom(duration: Long) {
        if (isVisible) {
            isInAnim = true
            animate()
                .translationY(height.toFloat() / 5)
                .scaleY(1.2f)
                .alpha(0f)
                .setDuration(duration / 2)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        visibility = View.INVISIBLE
                        isInAnim = false
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        super.onAnimationCancel(animation)
                        visibility = View.INVISIBLE
                        isInAnim = false
                    }
                })
        }
    }

    private fun View.fadeIn(duration: Long = sharedPreferenceManager.getAnimationSpeed()) {
        if (visibility != View.VISIBLE) {
            alpha = 0f
            translationY = -height.toFloat() / 100
            visibility = View.VISIBLE

            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(duration)
                .setListener(null)
        }
    }

    private fun View.fadeOut() {
        if (isVisible) {
            isInAnim = true
            val duration = sharedPreferenceManager.getAnimationSpeed()

            animate()
                .alpha(0f)
                .translationY(-height.toFloat() / 100)
                .setDuration(duration / 2)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        visibility = View.INVISIBLE
                        isInAnim = false
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        super.onAnimationCancel(animation)
                        visibility = View.INVISIBLE
                        isInAnim = false
                    }
                })

        }
    }
}