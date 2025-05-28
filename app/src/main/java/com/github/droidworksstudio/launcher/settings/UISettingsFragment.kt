package com.github.droidworksstudio.launcher.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.github.droidworksstudio.launcher.R

class UISettingsFragment : PreferenceFragmentCompat(), TitleProvider {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.ui_preferences, rootKey)
    }

    override fun getTitle(): String {
        return getString(R.string.ui_settings_title)
    }
}