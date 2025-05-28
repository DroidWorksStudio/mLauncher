package com.github.droidworksstudio.launcher.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import com.github.droidworksstudio.launcher.settings.SharedPreferenceManager

class Animations(context: Context) {

    private val sharedPreferenceManager = SharedPreferenceManager(context)

    // fadeViewIn and fadeViewOut are for smaller item transitions, such as the action menu

    fun fadeViewIn(view: View) {
        view.fadeIn()
    }

    fun fadeViewOut(view: View) {
        view.fadeOut()
    }

    fun showHome(homeView: View, appView: View) {
        appView.slideOutToBottom()
        homeView.fadeIn()
    }

    fun showApps(homeView: View, appView: View) {
        appView.slideInFromBottom()
        homeView.fadeOut()
    }

    fun backgroundIn(activity: Activity) {
        val originalColor = sharedPreferenceManager.getBgColor()

        // Only animate darkness onto the transparent background
        if (originalColor == Color.parseColor("#00000000")) {
            val newColor = Color.parseColor("#3F000000")
            val colorDrawable = ColorDrawable(originalColor)
            activity.window.setBackgroundDrawable(colorDrawable)

            val backgroundColorAnimator = ValueAnimator.ofObject(ArgbEvaluator(), originalColor, newColor)
            backgroundColorAnimator.addUpdateListener { animator ->
                colorDrawable.color = animator.animatedValue as Int
            }

            val duration = sharedPreferenceManager.getAnimationSpeed()
            backgroundColorAnimator.duration = duration

            backgroundColorAnimator.start()
        } else {
            return
        }
    }

    fun backgroundOut(activity: Activity) {
        val newColor = sharedPreferenceManager.getBgColor()

        // Only animate darkness onto the transparent background
        if (newColor == Color.parseColor("#00000000")) {
            val originalColor = Color.parseColor("#3F000000")
            val colorDrawable = ColorDrawable(originalColor)
            activity.window.setBackgroundDrawable(colorDrawable)

            val backgroundColorAnimator = ValueAnimator.ofObject(ArgbEvaluator(), originalColor, newColor)
            backgroundColorAnimator.addUpdateListener { animator ->
                colorDrawable.color = animator.animatedValue as Int
            }

            val duration = sharedPreferenceManager.getAnimationSpeed()
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

    private fun View.slideOutToBottom() {
        if (visibility == View.VISIBLE) {
            val duration = sharedPreferenceManager.getAnimationSpeed()

            animate()
                .translationY(height.toFloat() / 5)
                .scaleY(1.2f)
                .alpha(0f)
                .setDuration(duration / 2)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        visibility = View.INVISIBLE
                    }
                })
        }
    }

    private fun View.fadeIn() {
        if (visibility != View.VISIBLE) {
            alpha = 0f
            translationY = -height.toFloat() / 100
            visibility = View.VISIBLE
            val duration = sharedPreferenceManager.getAnimationSpeed()

            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(duration)
                .setListener(null)

        }
    }

    private fun View.fadeOut() {
        if (visibility == View.VISIBLE) {
            val duration = sharedPreferenceManager.getAnimationSpeed()

            animate()
                .alpha(0f)
                .translationY(-height.toFloat() / 100)
                .setDuration(duration / 2)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        visibility = View.GONE
                    }
                })

        }
    }
}