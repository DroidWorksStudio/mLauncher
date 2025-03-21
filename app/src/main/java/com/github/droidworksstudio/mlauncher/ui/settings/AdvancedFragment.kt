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
import androidx.navigation.fragment.findNavController
import com.github.droidworksstudio.mlauncher.BuildConfig
import com.github.droidworksstudio.mlauncher.MainViewModel
import com.github.droidworksstudio.mlauncher.R
import com.github.droidworksstudio.mlauncher.data.Constants
import com.github.droidworksstudio.mlauncher.data.Constants.Theme.Dark
import com.github.droidworksstudio.mlauncher.data.Constants.Theme.Light
import com.github.droidworksstudio.mlauncher.data.Constants.Theme.System
import com.github.droidworksstudio.mlauncher.data.Prefs
import com.github.droidworksstudio.mlauncher.databinding.FragmentSettingsBinding
import com.github.droidworksstudio.mlauncher.helper.communitySupportButton
import com.github.droidworksstudio.mlauncher.helper.getHexForOpacity
import com.github.droidworksstudio.mlauncher.helper.helpFeedbackButton
import com.github.droidworksstudio.mlauncher.helper.importWordsOfTheDay
import com.github.droidworksstudio.mlauncher.helper.isSystemInDarkMode
import com.github.droidworksstudio.mlauncher.helper.ismlauncherDefault
import com.github.droidworksstudio.mlauncher.helper.openAppInfo
import com.github.droidworksstudio.mlauncher.helper.setThemeMode
import com.github.droidworksstudio.mlauncher.helper.shareApplicationButton
import com.github.droidworksstudio.mlauncher.helper.utils.AppReloader
import com.github.droidworksstudio.mlauncher.listener.DeviceAdmin
import com.github.droidworksstudio.mlauncher.style.SettingsTheme
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.PageHeader
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.SettingsHomeItem
import com.github.droidworksstudio.mlauncher.ui.dialogs.DialogManager

class AdvancedFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var deviceManager: DevicePolicyManager
    private lateinit var componentName: ComponentName
    private lateinit var dialogBuilder: DialogManager

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        dialogBuilder = DialogManager(requireContext(), requireActivity())
        prefs = Prefs(requireContext())
        val backgroundColor = getHexForOpacity(prefs)
        binding.scrollView.setBackgroundColor(backgroundColor)
        return binding.root
    }


    @RequiresApi(Build.VERSION_CODES.R)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        resetThemeColors()
    }

    @RequiresApi(Build.VERSION_CODES.R)
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
            R.string.advanced_settings_set_as_default_launcher
        } else {
            R.string.advanced_settings_change_default_launcher
        }

        Column {
            PageHeader(
                iconRes = R.drawable.ic_back,
                title = stringResource(R.string.advanced_settings_title),
                onClick = { goBackToLastFragment() }
            )

            Spacer(
                modifier = Modifier
                    .height(16.dp)
            )
            val versionName =
                requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName

            SettingsHomeItem(
                title = stringResource(R.string.advanced_settings_app_info_title),
                description = stringResource(R.string.advanced_settings_app_info_description).format(versionName),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_app_info),
                titleFontSize = titleFontSize,
                descriptionFontSize = descriptionFontSize,
                iconSize = iconSize,
                onClick = {
                    openAppInfo(
                        requireContext(),
                        android.os.Process.myUserHandle(),
                        BuildConfig.APPLICATION_ID
                    )
                },
            )

            SettingsHomeItem(
                title = stringResource(changeLauncherText),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_change_default_launcher),
                titleFontSize = titleFontSize,
                descriptionFontSize = descriptionFontSize,
                iconSize = iconSize,
                onClick = {
                    viewModel.resetDefaultLauncherApp(requireContext())
                }
            )

            SettingsHomeItem(
                title = stringResource(R.string.advanced_settings_restart_title),
                description = stringResource(R.string.advanced_settings_restart_description).format(versionName),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_restart),
                titleFontSize = titleFontSize,
                descriptionFontSize = descriptionFontSize,
                iconSize = iconSize,
                onClick = {
                    AppReloader.restartApp(requireContext())
                }
            )

            SettingsHomeItem(
                title = stringResource(R.string.advanced_settings_backup_restore_title),
                description = stringResource(R.string.advanced_settings_backup_restore_description),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_backup_restore),
                titleFontSize = titleFontSize,
                descriptionFontSize = descriptionFontSize,
                iconSize = iconSize,
                onClick = {
                    dialogBuilder.showBackupRestoreDialog()
                }
            )

            SettingsHomeItem(
                title = stringResource(R.string.advanced_settings_theme_title),
                description = stringResource(R.string.advanced_settings_theme_description),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_theme),
                titleFontSize = titleFontSize,
                descriptionFontSize = descriptionFontSize,
                iconSize = iconSize,
                onClick = {
                    dialogBuilder.saveLoadThemeDialogDialog()
                }
            )

            SettingsHomeItem(
                title = stringResource(R.string.advanced_settings_wotd_title),
                description = stringResource(R.string.advanced_settings_wotd_description),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_word_of_the_day),
                titleFontSize = titleFontSize,
                descriptionFontSize = descriptionFontSize,
                iconSize = iconSize,
                onClick = {
                    importWordsOfTheDay(requireActivity())
                }
            )

            SettingsHomeItem(
                title = stringResource(R.string.advanced_settings_help_feedback_title),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_help_feedback),
                titleFontSize = titleFontSize,
                descriptionFontSize = descriptionFontSize,
                iconSize = iconSize,
                onClick = {
                    helpFeedbackButton(requireContext())
                }
            )

            SettingsHomeItem(
                title = stringResource(R.string.advanced_settings_community_support_title),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_community),
                titleFontSize = titleFontSize,
                descriptionFontSize = descriptionFontSize,
                iconSize = iconSize,
                onClick = {
                    communitySupportButton(requireContext())
                }
            )

            SettingsHomeItem(
                title = stringResource(R.string.advanced_settings_share_application_title),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_share_app),
                titleFontSize = titleFontSize,
                descriptionFontSize = descriptionFontSize,
                iconSize = iconSize,
                onClick = {
                    shareApplicationButton(requireContext())
                }
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

    @RequiresApi(Build.VERSION_CODES.R)
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

    private fun goBackToLastFragment() {
        findNavController().popBackStack()
    }

    private fun dismissDialogs() {
        dialogBuilder.backupRestoreDialog?.dismiss()
        dialogBuilder.saveLoadThemeDialog?.dismiss()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        @Suppress("DEPRECATION")
        super.onActivityCreated(savedInstanceState)
        dialogBuilder = DialogManager(requireContext(), requireActivity())
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

    override fun onStop() {
        super.onStop()
        dismissDialogs()
    }
}