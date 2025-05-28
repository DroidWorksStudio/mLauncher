package com.github.droidworksstudio.launcher.settings

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.github.droidworksstudio.launcher.R

class SettingsFragment : PreferenceFragmentCompat() {

    private var manualLocationPref: Preference? = null
    private var leftSwipePref: Preference? = null
    private var rightSwipePref: Preference? = null
    private lateinit var sharedPreferenceManager: SharedPreferenceManager

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        sharedPreferenceManager = SharedPreferenceManager(requireContext())

        val gpsLocationPref = findPreference<SwitchPreference?>("gpsLocation")
        manualLocationPref = findPreference("manualLocation")
        leftSwipePref = findPreference("leftSwipeApp")
        rightSwipePref = findPreference("rightSwipeApp")
        val aboutPref = findPreference<Preference?>("aboutPage")
        val hiddenPref = findPreference<Preference?>("hiddenApps")
        val homePref = findPreference<Preference?>("defaultHome")

        // Only enable manual location when gps location is disabled
        if (gpsLocationPref != null && manualLocationPref != null) {
            manualLocationPref?.isEnabled = !gpsLocationPref.isChecked

            gpsLocationPref.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, newValue ->
                    val isGpsEnabled = newValue as Boolean
                    manualLocationPref?.isEnabled = !isGpsEnabled
                    true
                }

            manualLocationPref?.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    requireActivity().supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.settingsLayout, LocationFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }
        }

        hiddenPref?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                requireActivity().supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.settingsLayout, HiddenAppsFragment())
                    .addToBackStack(null)
                    .commit()
                true
            }

        leftSwipePref?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                requireActivity().supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.settingsLayout, GestureAppsFragment("left"))
                    .addToBackStack(null)
                    .commit()
                true
            }

        rightSwipePref?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                requireActivity().supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.settingsLayout, GestureAppsFragment("right"))
                    .addToBackStack(null)
                    .commit()
                true
            }

        aboutPref?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                requireActivity().supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.settingsLayout, AboutFragment())
                    .addToBackStack(null)
                    .commit()
                true
            }

        homePref?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                if (intent.resolveActivity(requireContext().packageManager) != null) {
                    startActivity(intent)
                } else {
                    Toast.makeText(requireContext(), "Unable to launch settings", Toast.LENGTH_SHORT).show()
                }
                true
            }
    }

    override fun onResume() {
        super.onResume()
        manualLocationPref?.summary = sharedPreferenceManager.getWeatherRegion()

        leftSwipePref?.summary = sharedPreferenceManager.getGestureName("left")

        rightSwipePref?.summary = sharedPreferenceManager.getGestureName("right")
    }
}