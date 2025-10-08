package com.github.droidworksstudio.mlauncher.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.github.droidworksstudio.mlauncher.data.Constants
import com.github.droidworksstudio.mlauncher.data.Prefs
import com.github.droidworksstudio.mlauncher.helper.updateAllWidgets
import com.github.droidworksstudio.mlauncher.helper.utils.SystemBarObserver

open class BaseFragment : Fragment() {

    private val prefs: Prefs by lazy { Prefs(requireContext()) }

    private val systemBarObserver: SystemBarObserver by lazy { SystemBarObserver(prefs) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Attach the observer; lazy properties will initialize here
        lifecycle.addObserver(systemBarObserver)

        updateAllWidgets(requireContext())

        val themeMode = when (prefs.appTheme) {
            Constants.Theme.Light -> AppCompatDelegate.MODE_NIGHT_NO
            Constants.Theme.Dark -> AppCompatDelegate.MODE_NIGHT_YES
            Constants.Theme.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(themeMode)
    }
}

