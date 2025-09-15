package com.github.droidworksstudio.mlauncher.helper.utils

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.github.droidworksstudio.mlauncher.data.Prefs
import com.github.droidworksstudio.mlauncher.helper.hideNavigationBar
import com.github.droidworksstudio.mlauncher.helper.hideStatusBar
import com.github.droidworksstudio.mlauncher.helper.showNavigationBar
import com.github.droidworksstudio.mlauncher.helper.showStatusBar

class SystemBarObserver(private val prefs: Prefs) : DefaultLifecycleObserver {

    override fun onResume(owner: LifecycleOwner) {
        val activity = owner as? AppCompatActivity ?: return
        val window = activity.window
        if (prefs.showStatusBar) showStatusBar(window) else hideStatusBar(window)
        if (prefs.showNavigationBar) showNavigationBar(window) else hideNavigationBar(window)
    }
}
