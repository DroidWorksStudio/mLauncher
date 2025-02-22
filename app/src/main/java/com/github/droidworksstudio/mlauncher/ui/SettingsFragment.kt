package com.github.droidworksstudio.mlauncher.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
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
import com.github.droidworksstudio.mlauncher.MainViewModel
import com.github.droidworksstudio.mlauncher.R
import com.github.droidworksstudio.mlauncher.data.Constants
import com.github.droidworksstudio.mlauncher.data.Constants.AppDrawerFlag
import com.github.droidworksstudio.mlauncher.data.Constants.Theme.Dark
import com.github.droidworksstudio.mlauncher.data.Constants.Theme.Light
import com.github.droidworksstudio.mlauncher.data.Constants.Theme.System
import com.github.droidworksstudio.mlauncher.data.Prefs
import com.github.droidworksstudio.mlauncher.databinding.FragmentSettingsBinding
import com.github.droidworksstudio.mlauncher.helper.isSystemInDarkMode
import com.github.droidworksstudio.mlauncher.helper.setThemeMode
import com.github.droidworksstudio.mlauncher.helper.togglePrivateSpaceLock
import com.github.droidworksstudio.mlauncher.listener.DeviceAdmin
import com.github.droidworksstudio.mlauncher.style.SettingsTheme
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.SettingsHomeItem
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.TopMainHeader

class SettingsFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var deviceManager: DevicePolicyManager
    private lateinit var componentName: ComponentName

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

        resetThemeColors()
    }

    @Composable
    private fun Settings(fontSize: TextUnit = TextUnit.Unspecified) {
        val fs = remember { mutableStateOf(fontSize) }
        Constants.updateMaxHomePages(requireContext())

        val titleFontSize = if (fs.value.isSpecified) {
            (fs.value.value * 1.5).sp
        } else fs.value

        val descriptionFontSize = if (fs.value.isSpecified) {
            (fs.value.value * 1.2).sp
        } else fs.value

        val iconSize = if (fs.value.isSpecified) {
            tuToDp((fs.value * 0.8))
        } else tuToDp(fs.value)

        Column {
            Spacer(
                modifier = Modifier
                    .height(16.dp)
            )

            TopMainHeader(
                iconRes = R.drawable.app_launcher,
                title = stringResource(R.string.settings_name)
            )

            SettingsHomeItem(
                title = stringResource(R.string.settings_features_title),
                description = stringResource(R.string.settings_features_description),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_feature),
                titleFontSize = titleFontSize,
                descriptionFontSize = descriptionFontSize,
                iconSize = iconSize,
                onClick = {
                    showFeaturesSettings()
                },
            )

            SettingsHomeItem(
                title = stringResource(R.string.settings_look_feel_title),
                description = stringResource(R.string.settings_look_feel_description),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_look_feel),
                titleFontSize = titleFontSize,
                descriptionFontSize = descriptionFontSize,
                iconSize = iconSize,
                onClick = {
                    showLookFeelSettings()
                },
            )

            SettingsHomeItem(
                title = stringResource(R.string.settings_gestures_title),
                description = stringResource(R.string.settings_gestures_description),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_gestures),
                titleFontSize = titleFontSize,
                descriptionFontSize = descriptionFontSize,
                iconSize = iconSize,
                onClick = {
                    showGesturesSettings()
                },
            )

            SettingsHomeItem(
                title = stringResource(R.string.private_space),
                imageVector = ImageVector.vectorResource(id = R.drawable.private_space),
                titleFontSize = titleFontSize,
                descriptionFontSize = descriptionFontSize,
                iconSize = iconSize,
                onClick = {
                    togglePrivateSpaceLock(requireContext())
                }
            )

            SettingsHomeItem(
                title = stringResource(R.string.settings_favorite_apps_title),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_favorite),
                titleFontSize = titleFontSize,
                descriptionFontSize = descriptionFontSize,
                iconSize = iconSize,
                onClick = {
                    showFavoriteApps()
                },
            )

            SettingsHomeItem(
                title = stringResource(R.string.settings_hidden_apps_title),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_hidden),
                titleFontSize = titleFontSize,
                descriptionFontSize = descriptionFontSize,
                iconSize = iconSize,
                onClick = {
                    showHiddenApps()
                },
            )

            SettingsHomeItem(
                title = stringResource(R.string.settings_advanced_title),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_advanced),
                titleFontSize = titleFontSize,
                descriptionFontSize = descriptionFontSize,
                iconSize = iconSize,
                onClick = {
                    showAdvancedSettings()
                },
            )
        }
    }

    @Composable
    fun tuToDp(textUnit: TextUnit): Dp {
        val density = LocalDensity.current.density
        val scaledDensity = LocalDensity.current.fontScale
        val dpValue = textUnit.value * (density / scaledDensity)
        return dpValue.dp  // Convert to Dp using the 'dp' extension
    }

    private fun resetThemeColors() {
        binding.settingsView.setContent {

            val isDark = when (prefs.appTheme) {
                Light -> false
                Dark -> true
                System -> isSystemInDarkMode(requireContext())
            }

            setThemeMode(requireContext(), isDark, binding.scrollView)
            val settingsSize = (prefs.settingsSize - 3)

            SettingsTheme(isDark) {
                Settings(settingsSize.sp)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        @Suppress("DEPRECATION")
        super.onActivityCreated(savedInstanceState)
        prefs = Prefs(requireContext())
        viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        viewModel.ismlauncherDefault()

        deviceManager =
            context?.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        componentName = ComponentName(requireContext(), DeviceAdmin::class.java)
        checkAdminPermission()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun checkAdminPermission() {
        val isAdmin: Boolean = deviceManager.isAdminActive(componentName)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
            prefs.lockModeOn = isAdmin
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