package com.github.droidworksstudio.launcher.utils

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.droidworksstudio.launcher.MainActivity

class AppMenuLinearLayoutManager(private val activity: MainActivity) : LinearLayoutManager(activity) {

    private var firstVisibleItemPosition = 0
    private var scrollStarted = false

    fun setScrollInfo() {
        firstVisibleItemPosition = findFirstCompletelyVisibleItemPosition()
        scrollStarted = true
    }

    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
        val scrollRange = super.scrollVerticallyBy(dy, recycler, state)
        val overscroll: Int = dy - scrollRange

        // If the user scrolls up when already on top, go back to home. Only if the keyboard isn't open, though
        if (overscroll < 0 && (firstVisibleItemPosition == 0 || firstVisibleItemPosition < 0) && scrollStarted) {
            activity.backToHome()
        }

        if (scrollStarted) {
            scrollStarted = false
        }

        return scrollRange
    }

}