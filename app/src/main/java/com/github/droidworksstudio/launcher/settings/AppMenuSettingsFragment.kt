package com.github.droidworksstudio.launcher.settings

import android.Manifest
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.github.droidworksstudio.launcher.R
import com.github.droidworksstudio.launcher.utils.PermissionUtils
import com.github.droidworksstudio.launcher.utils.UIUtils

class AppMenuSettingsFragment : PreferenceFragmentCompat(), TitleProvider {
    private val permissionUtils = PermissionUtils()
    private var contactPref: SwitchPreference? = null
    private var webSearchPref: SwitchPreference? = null
    private var autoLaunchPref: SwitchPreference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.app_menu_preferences, rootKey)

        val uiUtils = UIUtils(requireContext())
        val contextMenuSettings = findPreference<Preference>("contextMenuSettings")

        contactPref = findPreference("contactsEnabled")
        webSearchPref = findPreference("webSearchEnabled")
        autoLaunchPref = findPreference("autoLaunch")

        contactPref?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->

            if (newValue as Boolean && !permissionUtils.hasPermission(requireContext(), Manifest.permission.READ_CONTACTS)) {
                (requireActivity() as SettingsActivity).requestContactsPermission()
                return@OnPreferenceChangeListener false
            } else {
                return@OnPreferenceChangeListener true
            }
        }

        if (webSearchPref != null && autoLaunchPref != null) {
            webSearchPref?.isEnabled = (autoLaunchPref?.isChecked == false)
            autoLaunchPref?.isEnabled = (webSearchPref?.isChecked == false)
            webSearchPref?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                autoLaunchPref?.isEnabled = !(newValue as Boolean)
                return@OnPreferenceChangeListener true
            }
            autoLaunchPref?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                webSearchPref?.isEnabled = !(newValue as Boolean)
                return@OnPreferenceChangeListener true
            }
        }

        contextMenuSettings?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                uiUtils.switchFragment(requireActivity(), ContextMenuSettingsFragment())
                true
            }
    }

    override fun getTitle(): String {
        return getString(R.string.app_settings_title)
    }

    fun setContactPreference(isEnabled: Boolean) {
        contactPref?.isChecked = isEnabled
    }
}