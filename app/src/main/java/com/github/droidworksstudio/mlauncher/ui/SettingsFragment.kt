package com.github.droidworksstudio.mlauncher.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.github.droidworksstudio.common.DonationDialog
import com.github.droidworksstudio.common.getLocalizedString
import com.github.droidworksstudio.common.isGestureNavigationEnabled
import com.github.droidworksstudio.mlauncher.MainViewModel
import com.github.droidworksstudio.mlauncher.R
import com.github.droidworksstudio.mlauncher.data.Constants.AppDrawerFlag
import com.github.droidworksstudio.mlauncher.data.Constants.Theme.Dark
import com.github.droidworksstudio.mlauncher.data.Constants.Theme.Light
import com.github.droidworksstudio.mlauncher.data.Constants.Theme.System
import com.github.droidworksstudio.mlauncher.data.Prefs
import com.github.droidworksstudio.mlauncher.databinding.FragmentSettingsBinding
import com.github.droidworksstudio.mlauncher.helper.isSystemInDarkMode
import com.github.droidworksstudio.mlauncher.helper.setThemeMode
import com.github.droidworksstudio.mlauncher.helper.utils.PrivateSpaceManager
import com.github.droidworksstudio.mlauncher.style.SettingsTheme
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.SettingsHomeItem
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.TopMainHeader

class SettingsFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        prefs = Prefs(requireContext())
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (prefs.firstSettingsOpen) {
            prefs.firstSettingsOpen = false
        }

        viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        viewModel.ismlauncherDefault()

        resetThemeColors()
    }

    private fun resetThemeColors() {
        binding.settingsView.setContent {

            val isDark = when (prefs.appTheme) {
                Light -> false
                Dark -> true
                System -> isSystemInDarkMode(requireContext())
            }

            setThemeMode(requireContext(), isDark, binding.settingsView)
            val settingsSize = (prefs.settingsSize - 3)

            SettingsTheme(isDark) {
                Settings(settingsSize.sp)
            }
        }
    }

    @Composable
    private fun Settings(fontSize: TextUnit = TextUnit.Unspecified) {
        var toggledPrivateSpaces by remember { mutableStateOf(PrivateSpaceManager(requireContext()).isPrivateSpaceLocked()) }
        val fs = remember { mutableStateOf(fontSize) }

        val titleFontSize = if (fs.value.isSpecified) {
            (fs.value.value * 1.5).sp
        } else fs.value

        val descriptionFontSize = if (fs.value.isSpecified) {
            (fs.value.value * 1.2).sp
        } else fs.value

        val iconSize = if (fs.value.isSpecified) {
            tuToDp((fs.value * 0.8))
        } else tuToDp(fs.value)

        val setPrivateSpaces = if (toggledPrivateSpaces) {
            R.drawable.ic_lock
        } else {
            R.drawable.ic_unlock
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(
                modifier = Modifier
                    .height(16.dp)
            )

            TopMainHeader(
                iconRes = R.drawable.app_launcher,
                title = getLocalizedString(R.string.settings_name)
            )

            SettingsHomeItem(
                title = getLocalizedString(R.string.settings_features_title),
                description = getLocalizedString(R.string.settings_features_description),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_feature),
                titleFontSize = titleFontSize,
                descriptionFontSize = descriptionFontSize,
                iconSize = iconSize,
                onClick = {
                    showFeaturesSettings()
                },
            )

            SettingsHomeItem(
                title = getLocalizedString(R.string.settings_look_feel_title),
                description = getLocalizedString(R.string.settings_look_feel_description),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_look_feel),
                titleFontSize = titleFontSize,
                descriptionFontSize = descriptionFontSize,
                iconSize = iconSize,
                onClick = {
                    showLookFeelSettings()
                },
            )

            SettingsHomeItem(
                title = getLocalizedString(R.string.settings_gestures_title),
                description = getLocalizedString(R.string.settings_gestures_description),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_gestures),
                titleFontSize = titleFontSize,
                descriptionFontSize = descriptionFontSize,
                iconSize = iconSize,
                onClick = {
                    showGesturesSettings()
                },
            )

            SettingsHomeItem(
                title = getLocalizedString(R.string.settings_notes_title),
                description = getLocalizedString(R.string.settings_notes_description),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_notes),
                titleFontSize = titleFontSize,
                descriptionFontSize = descriptionFontSize,
                iconSize = iconSize,
                onClick = {
                    showNotesSettings()
                },
            )

            if (PrivateSpaceManager(requireContext()).isPrivateSpaceSupported() &&
                PrivateSpaceManager(requireContext()).isPrivateSpaceSetUp(
                    showToast = false,
                    launchSettings = false
                )
            ) {
                SettingsHomeItem(
                    title = getLocalizedString(R.string.private_space),
                    imageVector = ImageVector.vectorResource(id = setPrivateSpaces),
                    titleFontSize = titleFontSize,
                    descriptionFontSize = descriptionFontSize,
                    iconSize = iconSize,
                    onClick = {
                        PrivateSpaceManager(requireContext()).togglePrivateSpaceLock(
                            showToast = true,
                            launchSettings = true
                        )
                        toggledPrivateSpaces = !toggledPrivateSpaces
                    }
                )
            }

            SettingsHomeItem(
                title = getLocalizedString(R.string.settings_favorite_apps_title),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_favorite),
                titleFontSize = titleFontSize,
                descriptionFontSize = descriptionFontSize,
                iconSize = iconSize,
                onClick = {
                    showFavoriteApps()
                },
            )

            SettingsHomeItem(
                title = getLocalizedString(R.string.settings_hidden_apps_title),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_hidden),
                titleFontSize = titleFontSize,
                descriptionFontSize = descriptionFontSize,
                iconSize = iconSize,
                onClick = {
                    showHiddenApps()
                },
            )

            SettingsHomeItem(
                title = getLocalizedString(R.string.settings_advanced_title),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_advanced),
                titleFontSize = titleFontSize,
                descriptionFontSize = descriptionFontSize,
                iconSize = iconSize,
                onClick = {
                    showAdvancedSettings()
                },
            )

            // 👇 This spacer pushes the next item (donation) to the bottom
            Spacer(modifier = Modifier.weight(1f))

            SettingsHomeItem(
                title = getLocalizedString(R.string.settings_donation_title),
                description = getLocalizedString(R.string.settings_donation_description),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_donation),
                titleFontSize = titleFontSize,
                descriptionFontSize = descriptionFontSize,
                iconSize = iconSize,
                onClick = {
                    DonationDialog(requireContext()).show(getLocalizedString(R.string.settings_donation_dialog))
                },
            )

            Spacer(
                modifier = Modifier
                    .height(16.dp)
            )

            if (!isGestureNavigationEnabled(requireContext())) {
                Spacer(
                    modifier = Modifier
                        .height(52.dp)
                )
            }
        }
    }

    @Composable
    fun tuToDp(textUnit: TextUnit): Dp {
        val density = LocalDensity.current.density
        val scaledDensity = LocalDensity.current.fontScale
        val dpValue = textUnit.value * (density / scaledDensity)
        return dpValue.dp  // Convert to Dp using the 'dp' extension
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showFeaturesSettings() {
        findNavController().navigate(
            R.id.action_settingsFragment_to_settingsFeaturesFragment,
        )
    }

    private fun showLookFeelSettings() {
        findNavController().navigate(
            R.id.action_settingsFragment_to_settingsLookFeelFragment,
        )
    }

    private fun showGesturesSettings() {
        findNavController().navigate(
            R.id.action_settingsFragment_to_settingsGesturesFragment,
        )
    }

    private fun showNotesSettings() {
        findNavController().navigate(
            R.id.action_settingsFragment_to_settingsNotesFragment,
        )
    }

    private fun showHiddenApps() {
        viewModel.getHiddenApps()
        findNavController().navigate(
            R.id.action_settingsFragment_to_appListFragment,
            bundleOf("flag" to AppDrawerFlag.HiddenApps.toString())
        )
    }

    private fun showFavoriteApps() {
        findNavController().navigate(
            R.id.action_settingsFragment_to_appFavoriteFragment,
            bundleOf("flag" to AppDrawerFlag.SetHomeApp.toString())
        )
    }

    private fun showAdvancedSettings() {
        findNavController().navigate(
            R.id.action_settingsFragment_to_settingsAdvancedFragment,
        )
    }
}