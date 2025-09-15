package com.github.droidworksstudio.mlauncher.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.github.droidworksstudio.mlauncher.data.Prefs
import com.github.droidworksstudio.mlauncher.helper.utils.SystemBarObserver

open class BaseFragment : Fragment() {

    private val prefs: Prefs by lazy { Prefs(requireContext()) }

    private val systemBarObserver: SystemBarObserver by lazy { SystemBarObserver(prefs) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Attach the observer; lazy properties will initialize here
        lifecycle.addObserver(systemBarObserver)
    }
}

