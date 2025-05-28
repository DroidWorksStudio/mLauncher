package com.github.droidworksstudio.launcher.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.github.droidworksstudio.launcher.R

class ContextMenuSettingsFragment : PreferenceFragmentCompat(), TitleProvider {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.context_menu_preferences, rootKey)
    }

    override fun getTitle(): String {
        return getString(R.string.context_menu_settings_title)
    }
}