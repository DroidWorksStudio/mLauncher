package com.github.droidworksstudio.launcher.utils

import android.app.Activity
import android.widget.EdgeEffect
import androidx.recyclerview.widget.RecyclerView

class AppMenuEdgeFactory(private val activity: Activity) : RecyclerView.EdgeEffectFactory() {

    override fun createEdgeEffect(view: RecyclerView, direction: Int): EdgeEffect {
        return AppMenuEdgeEffect(activity)
    }

    inner class AppMenuEdgeEffect(activity: Activity) : EdgeEffect(activity) {

        //This just speeds up the animation when the scrolling hits the edge so that the app menu can be exited sooner
        private val animationSpeedFactor = 0.75f

        override fun onAbsorb(velocity: Int) {
            super.onAbsorb((velocity * animationSpeedFactor).toInt())
        }

        override fun onPull(deltaDistance: Float, displacement: Float) {
            super.onPull(deltaDistance * animationSpeedFactor, displacement)
        }

        override fun onPullDistance(deltaDistance: Float, displacement: Float): Float {
            return super.onPullDistance(deltaDistance * animationSpeedFactor, displacement)
        }

    }
}