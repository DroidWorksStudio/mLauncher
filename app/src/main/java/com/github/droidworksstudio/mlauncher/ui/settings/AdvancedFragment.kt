package com.github.droidworksstudio.mlauncher.ui.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.github.droidworksstudio.common.getLocalizedString
import com.github.droidworksstudio.common.share.ShareUtils
import com.github.droidworksstudio.common.showShortToast
import com.github.droidworksstudio.mlauncher.BuildConfig
import com.github.droidworksstudio.mlauncher.MainActivity
import com.github.droidworksstudio.mlauncher.MainViewModel
import com.github.droidworksstudio.mlauncher.R
import com.github.droidworksstudio.mlauncher.data.Constants.Theme.Dark
import com.github.droidworksstudio.mlauncher.data.Constants.Theme.Light
import com.github.droidworksstudio.mlauncher.data.Constants.Theme.System
import com.github.droidworksstudio.mlauncher.data.Prefs
import com.github.droidworksstudio.mlauncher.databinding.FragmentSettingsBinding
import com.github.droidworksstudio.mlauncher.helper.checkWhoInstalled
import com.github.droidworksstudio.mlauncher.helper.communitySupportButton
import com.github.droidworksstudio.mlauncher.helper.getHexForOpacity
import com.github.droidworksstudio.mlauncher.helper.helpFeedbackButton
import com.github.droidworksstudio.mlauncher.helper.isSystemInDarkMode
import com.github.droidworksstudio.mlauncher.helper.ismlauncherDefault
import com.github.droidworksstudio.mlauncher.helper.openAppInfo
import com.github.droidworksstudio.mlauncher.helper.setThemeMode
import com.github.droidworksstudio.mlauncher.helper.setTopPadding
import com.github.droidworksstudio.mlauncher.helper.utils.AppReloader
import com.github.droidworksstudio.mlauncher.style.SettingsTheme
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.PageHeader
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.SettingsHomeItem
import com.github.droidworksstudio.mlauncher.ui.dialogs.DialogManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AdvancedFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var dialogBuilder: DialogManager
    private lateinit var shareUtils: ShareUtils

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
        shareUtils = ShareUtils(requireContext(), requireActivity())
        val backgroundColor = getHexForOpacity(prefs)
        binding.settingsView.setBackgroundColor(backgroundColor)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        viewModel.ismlauncherDefault()

        resetThemeColors()

        setTopPadding(requireActivity(), binding.settingsView)
    }

    @Composable
    private fun Settings(fontSize: TextUnit = TextUnit.Unspecified) {
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

        val changeLauncherText = if (ismlauncherDefault(requireContext())) {
            R.string.advanced_settings_change_default_launcher
        } else {
            R.string.advanced_settings_set_as_default_launcher
        }

        Column {
            PageHeader(
                iconRes = R.drawable.ic_back,
                title = getLocalizedString(R.string.advanced_settings_title),
                onClick = { goBackToLastFragment() }
            )

            Spacer(
                modifier = Modifier
                    .height(16.dp)
            )

            val versionName =
                requireContext().packageManager.getPackageInfo(
                    requireContext().packageName,
                    0
                ).versionName

            SettingsHomeItem(
                title = getLocalizedString(R.string.advanced_settings_app_info_title),
                description = getLocalizedString(R.string.advanced_settings_app_info_description).format(
                    versionName
                ),
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
                title = getLocalizedString(changeLauncherText),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_change_default_launcher),
                titleFontSize = titleFontSize,
                descriptionFontSize = descriptionFontSize,
                iconSize = iconSize,
                onClick = {
                    viewModel.resetDefaultLauncherApp(requireContext())
                }
            )

            SettingsHomeItem(
                title = getLocalizedString(R.string.advanced_settings_restart_title),
                description = getLocalizedString(R.string.advanced_settings_restart_description).format(
                    versionName
                ),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_restart),
                titleFontSize = titleFontSize,
                descriptionFontSize = descriptionFontSize,
                iconSize = iconSize,
                onClick = {
                    AppReloader.restartApp(requireContext())
                }
            )

            SettingsHomeItem(
                title = getLocalizedString(R.string.settings_exit_mlauncher_title),
                description = getLocalizedString(R.string.settings_exit_mlauncher_description),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_exit),
                titleFontSize = titleFontSize,
                descriptionFontSize = descriptionFontSize,
                iconSize = iconSize,
                onClick = {
                    exitLauncher(requireContext())
                },
            )

            SettingsHomeItem(
                title = getLocalizedString(R.string.advanced_settings_backup_restore_title),
                description = getLocalizedString(R.string.advanced_settings_backup_restore_description),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_backup_restore),
                titleFontSize = titleFontSize,
                descriptionFontSize = descriptionFontSize,
                iconSize = iconSize,
                onClick = {
                    dialogBuilder.showBackupRestoreDialog()
                }
            )

            SettingsHomeItem(
                title = getLocalizedString(R.string.advanced_settings_theme_title),
                description = getLocalizedString(R.string.advanced_settings_theme_description),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_theme),
                titleFontSize = titleFontSize,
                descriptionFontSize = descriptionFontSize,
                iconSize = iconSize,
                onClick = {
                    dialogBuilder.saveLoadThemeDialogDialog()
                }
            )

            SettingsHomeItem(
                title = getLocalizedString(R.string.advanced_settings_wotd_title),
                description = getLocalizedString(R.string.advanced_settings_wotd_description),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_word_of_the_day),
                titleFontSize = titleFontSize,
                descriptionFontSize = descriptionFontSize,
                iconSize = iconSize,
                onClick = {
                    (activity as MainActivity).restoreWordsBackup()
                }
            )

            SettingsHomeItem(
                title = getLocalizedString(R.string.advanced_settings_help_feedback_title),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_help_feedback),
                titleFontSize = titleFontSize,
                descriptionFontSize = descriptionFontSize,
                iconSize = iconSize,
                onClick = {
                    helpFeedbackButton(requireContext())
                }
            )

            SettingsHomeItem(
                title = getLocalizedString(R.string.advanced_settings_community_support_title),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_community),
                titleFontSize = titleFontSize,
                descriptionFontSize = descriptionFontSize,
                iconSize = iconSize,
                onClick = {
                    communitySupportButton(requireContext())
                }
            )

            SettingsHomeItem(
                title = getLocalizedString(R.string.advanced_settings_share_application_title),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_share_app),
                titleFontSize = titleFontSize,
                descriptionFontSize = descriptionFontSize,
                iconSize = iconSize,
                onClick = {
                    shareUtils.showMaterialShareDialog(
                        requireContext(),
                        getLocalizedString(R.string.share_application),
                        checkWhoInstalled(requireContext())
                    )
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

    private fun exitLauncher(context: Context) {
        val pm = context.packageManager

        // Query for all apps that can handle the home intent
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }

        val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .filter {
                val pkgName = it.activityInfo.packageName
                val isLauncher = !pkgName.contains("settings", ignoreCase = true)
                it.activityInfo.enabled && isLauncher
            }

        if (resolveInfos.isEmpty()) {
            showShortToast("No launchers found")
            return
        }

        // Create a list of app names and icons
        val launcherLabels = resolveInfos.map { it.loadLabel(pm).toString() }
        val launcherIcons = resolveInfos.map { it.loadIcon(pm) }

        val iconSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            24f,
            context.resources.displayMetrics
        ).toInt()

        val adapter = object :
            ArrayAdapter<String>(context, android.R.layout.select_dialog_item, launcherLabels) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)

                val originalDrawable = launcherIcons[position]
                val resizedBitmap = drawableToBitmap(originalDrawable, iconSizePx)
                val resizedDrawable = resizedBitmap.toDrawable(context.resources)

                textView.setCompoundDrawablesWithIntrinsicBounds(resizedDrawable, null, null, null)
                textView.compoundDrawablePadding = 16
                return view
            }
        }


        // Show the dialog
        MaterialAlertDialogBuilder(context)
            .setTitle(getLocalizedString(R.string.settings_exit_mlauncher_dialog))
            .setAdapter(adapter) { _, which ->
                val selectedInfo = resolveInfos[which]
                val launchIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    component = ComponentName(
                        selectedInfo.activityInfo.packageName,
                        selectedInfo.activityInfo.name
                    )
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(launchIntent)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    fun drawableToBitmap(drawable: Drawable, size: Int): Bitmap {
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun goBackToLastFragment() {
        findNavController().popBackStack()
    }

    private fun dismissDialogs() {
        dialogBuilder.backupRestoreDialog?.dismiss()
        dialogBuilder.saveLoadThemeDialog?.dismiss()
        shareUtils.shareDialog?.dismiss()
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