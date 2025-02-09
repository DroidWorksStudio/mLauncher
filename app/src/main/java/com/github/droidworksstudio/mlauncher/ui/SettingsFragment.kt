package com.github.droidworksstudio.mlauncher.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.github.droidworksstudio.common.showShortToast
import com.github.droidworksstudio.mlauncher.MainViewModel
import com.github.droidworksstudio.mlauncher.R
import com.github.droidworksstudio.mlauncher.data.Constants
import com.github.droidworksstudio.mlauncher.data.Constants.Action
import com.github.droidworksstudio.mlauncher.data.Constants.AppDrawerFlag
import com.github.droidworksstudio.mlauncher.data.Constants.Theme.Dark
import com.github.droidworksstudio.mlauncher.data.Constants.Theme.Light
import com.github.droidworksstudio.mlauncher.data.Constants.Theme.System
import com.github.droidworksstudio.mlauncher.data.Prefs
import com.github.droidworksstudio.mlauncher.databinding.FragmentSettingsBinding
import com.github.droidworksstudio.mlauncher.helper.AppReloader
import com.github.droidworksstudio.mlauncher.helper.getHexForOpacity
import com.github.droidworksstudio.mlauncher.helper.hideStatusBar
import com.github.droidworksstudio.mlauncher.helper.showStatusBar
import com.github.droidworksstudio.mlauncher.listener.DeviceAdmin
import com.github.droidworksstudio.mlauncher.style.SettingsTheme
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.HeaderWithIconAndTitle
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.SettingsHomeItem
import net.mm2d.color.chooser.ColorChooserDialog

class SettingsFragment : Fragment() {

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


        if (prefs.firstSettingsOpen) {
            prefs.firstSettingsOpen = false
        }

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

    private fun <T> createTypefaceMap(
        context: Context,
        typeMappings: Map<T, Int>
    ): Map<T, Typeface?> {
        return typeMappings.mapValues { (_, fontResId) ->
            ResourcesCompat.getFont(context, fontResId)
        }
    }

    @Composable
    private fun Settings(fontSize: TextUnit = TextUnit.Unspecified) {
        val selected = remember { mutableStateOf("selected") }
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

        val typeMappings: Map<Constants.Fonts, Int> = mapOf(
            Constants.Fonts.System to R.font.roboto,
            Constants.Fonts.Bitter to R.font.bitter,
            Constants.Fonts.Dotness to R.font.dotness,
            Constants.Fonts.DroidSans to R.font.droid_sans,
            Constants.Fonts.GreatVibes to R.font.great_vibes,
            Constants.Fonts.Lato to R.font.lato,
            Constants.Fonts.Lobster to R.font.lobster,
            Constants.Fonts.Merriweather to R.font.merriweather,
            Constants.Fonts.Montserrat to R.font.montserrat,
            Constants.Fonts.OpenSans to R.font.open_sans,
            Constants.Fonts.Pacifico to R.font.pacifico,
            Constants.Fonts.Quicksand to R.font.quicksand,
            Constants.Fonts.Raleway to R.font.raleway,
            Constants.Fonts.Roboto to R.font.roboto,
            Constants.Fonts.SourceCodePro to R.font.source_code_pro
        )

        val typefaceMapFonts: Map<Constants.Fonts, Typeface?> =
            createTypefaceMap(requireActivity(), typeMappings)

        // Shared state to track which section is visible
        val visibleSection = remember { mutableStateOf<String?>(null) }

        Column {
            Spacer(
                modifier = Modifier
                    .height(16.dp)
            )

            HeaderWithIconAndTitle(
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
                onClick = { showShortToast("Features Clicked") },
            )

            SettingsHomeItem(
                title = stringResource(R.string.settings_look_feel_title),
                description = stringResource(R.string.settings_look_feel_description),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_look_feel),
                titleFontSize = titleFontSize,
                descriptionFontSize = descriptionFontSize,
                iconSize = iconSize,
                onClick = { showShortToast("Look & Feel Clicked") },
            )

            SettingsHomeItem(
                title = stringResource(R.string.settings_favorite_apps_title),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_favorite),
                titleFontSize = titleFontSize,
                descriptionFontSize = descriptionFontSize,
                iconSize = iconSize,
                onClick = { showFavoriteApps() },
            )

            SettingsHomeItem(
                title = stringResource(R.string.settings_hidden_apps_title),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_hidden),
                titleFontSize = titleFontSize,
                descriptionFontSize = descriptionFontSize,
                iconSize = iconSize,
                onClick = { showHiddenApps() },
            )

            SettingsHomeItem(
                title = stringResource(R.string.settings_advanced_title),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_advanced),
                titleFontSize = titleFontSize,
                descriptionFontSize = descriptionFontSize,
                iconSize = iconSize,
                onClick = { showAdvancedSettings() },
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

    private fun showColorPickerDialog(requestCode: String, color: Int) {
        ColorChooserDialog.show(
            fragment = this,
            requestKey = requestCode,
            initialColor = color,
            withAlpha = true,
            initialTab = 1,
            tabs = intArrayOf(
                ColorChooserDialog.TAB_RGB,
                ColorChooserDialog.TAB_HSV,
                ColorChooserDialog.TAB_PALETTE
            )
        )

        ColorChooserDialog.registerListener(fragment = this, requestKey = requestCode, { pickedColor ->
            when (requestCode) {
                "appColor" -> prefs.appColor = pickedColor
                "dateColor" -> prefs.dateColor = pickedColor
                "timeColor" -> prefs.timeColor = pickedColor
            }
        })
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

    private fun setHomeAlignment(gravity: Constants.Gravity) {
        prefs.homeAlignment = gravity
        viewModel.updateHomeAppsAlignment(gravity, prefs.homeAlignmentBottom)
    }

    private fun toggleHomeAppsBottom() {
        val onBottom = !prefs.homeAlignmentBottom

        prefs.homeAlignmentBottom = onBottom
        viewModel.updateHomeAppsAlignment(prefs.homeAlignment, onBottom)
    }

    private fun toggleHiddenAppsDisplayed() {
        prefs.hiddenAppsDisplayed = !prefs.hiddenAppsDisplayed
    }

    private fun toggleRecentAppsDisplayed() {
        prefs.recentAppsDisplayed = !prefs.recentAppsDisplayed
    }

    private fun setClockAlignment(gravity: Constants.Gravity) {
        prefs.clockAlignment = gravity
        viewModel.updateClockAlignment(gravity)
    }

    private fun toggleStatusBar() {
        val showStatusbar = !prefs.showStatusBar
        prefs.showStatusBar = showStatusbar
        if (showStatusbar) showStatusBar(requireActivity()) else hideStatusBar(requireActivity())
    }

    private fun toggleShowBattery() {
        prefs.showBattery = !prefs.showBattery
    }

    private fun toggleShowBatteryIcon() {
        prefs.showBatteryIcon = !prefs.showBatteryIcon
    }

    private fun toggleHomeLocked() {
        prefs.homeLocked = !prefs.homeLocked
    }

    private fun toggleSettingsLocked() {
        prefs.settingsLocked = !prefs.settingsLocked
    }

    private fun toggleExtendHomeAppsArea() {
        prefs.extendHomeAppsArea = !prefs.extendHomeAppsArea
    }

    private fun toggleAppUsageStats() {
        prefs.appUsageStats = !prefs.appUsageStats
    }

    private fun toggleAllAppsText() {
        prefs.useAllAppsText = !prefs.useAllAppsText
    }

    private fun toggleShowDate() {
        prefs.showDate = !prefs.showDate
        viewModel.setShowDate(prefs.showDate)
    }

    private fun toggleShowTime() {
        prefs.showTime = !prefs.showTime
        viewModel.setShowTime(prefs.showTime)
    }

    private fun toggleShowTimeFormat() {
        prefs.showTimeFormat = !prefs.showTimeFormat
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
            bundleOf("flag" to AppDrawerFlag.ReorderApps.toString())
        )
    }

    private fun showAdvancedSettings() {
        findNavController().navigate(
            R.id.action_settingsFragment_to_settingsAdvancedFragment,
        )
    }

    private fun setHomeAppsNum(homeAppsNum: Int) {
        prefs.homeAppsNum = homeAppsNum
        viewModel.homeAppsCount.value = homeAppsNum
    }

    private fun setHomePagesNum(homePagesNum: Int) {
        prefs.homePagesNum = homePagesNum
        viewModel.homePagesCount.value = homePagesNum
    }

    private fun setOpacityNum(opacityNum: Int) {
        prefs.opacityNum = opacityNum
        viewModel.opacityNum.value = opacityNum
    }

    private fun setFilterStrength(filterStrength: Int) {
        prefs.filterStrength = filterStrength
        viewModel.filterStrength.value = filterStrength
    }

    private fun setRecentCounter(recentCount: Int) {
        prefs.recentCounter = recentCount
        viewModel.recentCounter.value = recentCount
    }

    private fun toggleKeyboardText() {
        prefs.autoShowKeyboard = !prefs.autoShowKeyboard
    }

    private fun toggleAutoOpenApp() {
        prefs.autoOpenApp = !prefs.autoOpenApp
    }

    private fun toggleHomePagerOn() {
        prefs.homePagerOn = !prefs.homePagerOn
    }

    private fun toggleSearchFromStart() {
        prefs.searchFromStart = !prefs.searchFromStart
    }

    private fun setTheme(appTheme: Constants.Theme) {
        prefs.appTheme = appTheme
        requireActivity().recreate()
    }

    private fun setLang(langInt: Constants.Language) {
        prefs.language = langInt
        requireActivity().recreate()
    }

    private fun setLauncherFont(fontInt: Constants.Fonts) {
        prefs.launcherFont = fontInt
        Handler(Looper.getMainLooper()).postDelayed({
            AppReloader.restartApp(requireContext())
        }, 500) // Delay in milliseconds (e.g., 500ms)
    }

    private fun setEngine(engineInt: Constants.SearchEngines) {
        prefs.searchEngines = engineInt
        requireActivity().recreate()
    }

    private fun setAppTextSize(size: Int) {
        prefs.appSize = size
        requireActivity().recreate()
    }

    private fun setTextSize(size: Int) {
        prefs.settingsSize = size
        requireActivity().recreate()
    }

    private fun setClockSize(size: Int) {
        prefs.clockSize = size
        requireActivity().recreate()
    }

    private fun setDateSize(size: Int) {
        prefs.dateSize = size
        requireActivity().recreate()
    }

    private fun setBatterySize(size: Int) {
        prefs.batterySize = size
        requireActivity().recreate()
    }

    private fun setTextPaddingSize(size: Int) {
        prefs.textPaddingSize = size
        requireActivity().recreate()
    }

    private fun setGesture(flag: AppDrawerFlag, action: Action) {
        when (flag) {
            AppDrawerFlag.SetShortSwipeUp -> prefs.shortSwipeUpAction = action
            AppDrawerFlag.SetShortSwipeDown -> prefs.shortSwipeDownAction = action
            AppDrawerFlag.SetShortSwipeLeft -> prefs.shortSwipeLeftAction = action
            AppDrawerFlag.SetShortSwipeRight -> prefs.shortSwipeRightAction = action
            AppDrawerFlag.SetClickClock -> prefs.clickClockAction = action
            AppDrawerFlag.SetAppUsage -> prefs.clickAppUsageAction = action
            AppDrawerFlag.SetClickDate -> prefs.clickDateAction = action
            AppDrawerFlag.SetDoubleTap -> prefs.doubleTapAction = action
            AppDrawerFlag.SetLongSwipeUp -> prefs.longSwipeUpAction = action
            AppDrawerFlag.SetLongSwipeDown -> prefs.longSwipeDownAction = action
            AppDrawerFlag.SetLongSwipeLeft -> prefs.longSwipeLeftAction = action
            AppDrawerFlag.SetLongSwipeRight -> prefs.longSwipeRightAction = action
            AppDrawerFlag.SetHomeApp,
            AppDrawerFlag.HiddenApps,
            AppDrawerFlag.ReorderApps,
            AppDrawerFlag.LaunchApp -> {
            }
        }

        when (action) {
            Action.OpenApp -> {
                viewModel.getAppList(true)
                findNavController().navigate(
                    R.id.action_settingsFragment_to_appListFragment,
                    bundleOf("flag" to flag.toString())
                )
            }

            else -> {}
        }
    }
}