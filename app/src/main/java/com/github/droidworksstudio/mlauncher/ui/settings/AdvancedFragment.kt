package com.github.droidworksstudio.mlauncher.ui.settings

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.github.droidworksstudio.mlauncher.MainViewModel
import com.github.droidworksstudio.mlauncher.R
import com.github.droidworksstudio.mlauncher.data.Constants
import com.github.droidworksstudio.mlauncher.data.Constants.Theme.Dark
import com.github.droidworksstudio.mlauncher.data.Constants.Theme.Light
import com.github.droidworksstudio.mlauncher.data.Constants.Theme.System
import com.github.droidworksstudio.mlauncher.data.Prefs
import com.github.droidworksstudio.mlauncher.databinding.FragmentSettingsBinding
import com.github.droidworksstudio.mlauncher.helper.getHexForOpacity
import com.github.droidworksstudio.mlauncher.helper.ismlauncherDefault
import com.github.droidworksstudio.mlauncher.helper.resetDefaultLauncher
import com.github.droidworksstudio.mlauncher.listener.DeviceAdmin
import com.github.droidworksstudio.mlauncher.style.SettingsTheme
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.SettingsHomeItem

class AdvancedFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var deviceManager: DevicePolicyManager
    private lateinit var componentName: ComponentName

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        prefs = Prefs(requireContext())
        val backgroundColor = getHexForOpacity(prefs)
        binding.scrollView.setBackgroundColor(backgroundColor)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val backgroundColor = getHexForOpacity(prefs)
        binding.scrollView.setBackgroundColor(backgroundColor)

        binding.settingsView.setContent {

            val isDark = when (prefs.appTheme) {
                Light -> false
                Dark -> true
                System -> isSystemInDarkTheme()
            }

            val settingsSize = (prefs.settingsSize - 3)

            SettingsTheme(isDark) {
                Settings(settingsSize.sp)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onResume() {
        super.onResume()
        val backgroundColor = getHexForOpacity(prefs)
        binding.scrollView.setBackgroundColor(backgroundColor)
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

        val changeLauncherText = if (ismlauncherDefault(requireContext())) {
            R.string.change_default_launcher
        } else {
            R.string.set_as_default_launcher
        }

        Column {
            Spacer(
                modifier = Modifier
                    .height(16.dp)
            )
            val versionName =
                requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName

            SettingsHomeItem(
                title = stringResource(R.string.settings_app_info_title),
                description = stringResource(R.string.settings_app_info_description).format(versionName),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_app_info),
                titleFontSize = titleFontSize,
                descriptionFontSize = descriptionFontSize,
                iconSize = iconSize,
            )

            SettingsHomeItem(
                title = stringResource(changeLauncherText),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_change_default_launcher),
                titleFontSize = titleFontSize,
                descriptionFontSize = descriptionFontSize,
                iconSize = iconSize,
                onClick = { resetDefaultLauncher(requireContext()) }
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}