package com.github.droidworksstudio.mlauncher.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.github.droidworksstudio.mlauncher.BuildConfig
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
import com.github.droidworksstudio.mlauncher.helper.getHexForOpacity
import com.github.droidworksstudio.mlauncher.helper.hideStatusBar
import com.github.droidworksstudio.mlauncher.helper.ismlauncherDefault
import com.github.droidworksstudio.mlauncher.helper.loadFile
import com.github.droidworksstudio.mlauncher.helper.openAppInfo
import com.github.droidworksstudio.mlauncher.helper.resetDefaultLauncher
import com.github.droidworksstudio.mlauncher.helper.showStatusBar
import com.github.droidworksstudio.mlauncher.helper.storeFile
import com.github.droidworksstudio.mlauncher.listener.DeviceAdmin
import com.github.droidworksstudio.mlauncher.style.SettingsTheme
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.SettingsArea
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.SettingsGestureItem
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.SettingsItem
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.SettingsSliderItem
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.SettingsTextButton
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.SettingsToggle
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.SettingsTopView
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.SettingsThreeButtonRow

class SettingsFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var deviceManager: DevicePolicyManager
    private lateinit var componentName: ComponentName

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val offset = 5

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        prefs = Prefs(requireContext())
        val hex = getHexForOpacity(requireContext(), prefs)
        binding.scrollView.setBackgroundColor(hex)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val hex = getHexForOpacity(requireContext(), prefs)
        binding.scrollView.setBackgroundColor(hex)


        if (prefs.firstSettingsOpen) {
            prefs.firstSettingsOpen = false
        }

        binding.settingsView.setContent {

            val isDark = when (prefs.appTheme) {
                Light -> false
                Dark -> true
                System -> isSystemInDarkTheme()
            }

            SettingsTheme(isDark) {
                Settings((prefs.textSizeSettings - offset).sp)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onResume() {
        super.onResume()
        val hex = getHexForOpacity(requireContext(), prefs)
        binding.scrollView.setBackgroundColor(hex)
    }

    @Composable
    private fun Settings(fontSize: TextUnit = TextUnit.Unspecified) {
        val selected = remember { mutableStateOf("") }
        val fs = remember { mutableStateOf(fontSize) }
        Constants.updateMaxHomePages(requireContext())

        val titleFs = if (fs.value.isSpecified) {
            (fs.value.value * 2.2).sp
        } else fs.value

        val iconFs = if (fs.value.isSpecified) {
            (fs.value.value * 1.3).sp
        } else fs.value

        val changeLauncherText = if (ismlauncherDefault(requireContext())) {
            R.string.change_default_launcher
        } else {
            R.string.set_as_default_launcher
        }

        Column {
            SettingsTopView(
                stringResource(R.string.app_name),
                fontSize = titleFs,
                onClick = {
                    openAppInfo(
                        requireContext(),
                        android.os.Process.myUserHandle(),
                        BuildConfig.APPLICATION_ID
                    )
                },
            ) {
                SettingsTextButton(
                    stringResource(R.string.hidden_apps),
                    fontSize = iconFs
                ) {
                    showHiddenApps()
                }
                SettingsTextButton(
                    stringResource(changeLauncherText),
                    fontSize = iconFs
                ) {
                    resetDefaultLauncher(requireContext())
                }
                SettingsTextButton(
                    stringResource(R.string.reorder_apps),
                    fontSize = iconFs
                ) {
                    showReorderApps()
                }
            }
            SettingsArea(
                title = stringResource(R.string.appearance),
                fontSize = titleFs,
                selected = selected,
                items = arrayOf(
                    { _, onChange ->
                        SettingsToggle(
                            title = stringResource(R.string.status_bar),
                            fontSize = iconFs,
                            onChange = onChange,
                            state = remember { mutableStateOf(prefs.showStatusBar) },
                        ) { toggleStatusBar() }
                    },
                    { open, onChange ->
                        SettingsItem(
                            title = stringResource(R.string.theme_mode),
                            fontSize = iconFs,
                            open = open,
                            onChange = onChange,
                            currentSelection = remember { mutableStateOf(prefs.appTheme) },
                            values = Constants.Theme.values(),
                            onSelect = { j -> setTheme(j) }
                        )
                    },
                        { open, onChange ->
                            if (prefs.appTheme.name == "Dark") {
                                SettingsItem(
                                    title = stringResource(R.string.color_mode),
                                    fontSize = iconFs,
                                    open = open,
                                    onChange = onChange,
                                    currentSelection = remember { mutableStateOf(prefs.appDarkColors) },
                                    values = Constants.DarkColors.values(),
                                    onSelect = { j -> setDarkColors(j) }
                                )
                            }
                            if (prefs.appTheme.name == "Light") {
                                SettingsItem(
                                    title = stringResource(R.string.color_mode),
                                    fontSize = iconFs,
                                    open = open,
                                    onChange = onChange,
                                    currentSelection = remember { mutableStateOf(prefs.appLightColors) },
                                    values = Constants.LightColors.values(),
                                    onSelect = { j -> setLightColors(j) }
                                )
                            }
                        },
                    { open, onChange ->
                        SettingsItem(
                            title = stringResource(R.string.app_language),
                            fontSize = iconFs,
                            open = open,
                            onChange = onChange,
                            currentSelection = remember { mutableStateOf(prefs.language) },
                            values = Constants.Language.values(),
                            onSelect = { j -> setLang(j) }
                        )
                    },
                    { open, onChange ->
                        SettingsSliderItem(
                            title = stringResource(R.string.app_text_size),
                            fontSize = iconFs,
                            open = open,
                            onChange = onChange,
                            currentSelection = remember { mutableIntStateOf(prefs.textSizeLauncher) },
                            min = Constants.TEXT_SIZE_MIN,
                            max = Constants.TEXT_SIZE_MAX,
                            onSelect = { f -> setTextSizeLauncher(f) }
                        )
                    },
                    { open, onChange ->
                        SettingsSliderItem(
                            title = stringResource(R.string.clock_text_size),
                            fontSize = iconFs,
                            open = open,
                            onChange = onChange,
                            currentSelection = remember { mutableIntStateOf(prefs.clockSize) },
                            min = Constants.CLOCK_SIZE_MIN,
                            max = Constants.CLOCK_SIZE_MAX,
                            onSelect = { f -> setClockSizeSettings(f) }
                        )
                    },
                    { open, onChange ->
                        SettingsSliderItem(
                            title = stringResource(R.string.app_padding_size),
                            fontSize = iconFs,
                            open = open,
                            onChange = onChange,
                            currentSelection = remember { mutableIntStateOf(prefs.textMarginSize) },
                            min = Constants.TEXT_MARGIN_MIN,
                            max = Constants.TEXT_MARGIN_MAX,
                            onSelect = { f -> setTextMarginSize(f) }
                        )
                    },
                    { _, onChange ->
                        SettingsToggle(
                            title = stringResource(R.string.follow_accent_colors),
                            fontSize = iconFs,
                            onChange = onChange,
                            state = remember { mutableStateOf(prefs.followAccentColors) },
                        ) { toggleFollowAccent() }
                    },
                    { open, onChange ->
                        SettingsSliderItem(
                            title = stringResource(R.string.background_opacity),
                            fontSize = iconFs,
                            open = open,
                            onChange = onChange,
                            currentSelection = remember { mutableIntStateOf(prefs.opacityNum) },
                            min = Constants.MIN_OPACITY,
                            max = Constants.MAX_OPACITY,
                            onSelect = { j -> setOpacityNum(j) }
                        )
                    },
                    { _, onChange ->
                        SettingsToggle(
                            title = stringResource(R.string.all_apps_text),
                            fontSize = iconFs,
                            onChange = onChange,
                            state = remember { mutableStateOf(prefs.useAllAppsText) },
                        ) { toggleAllAppsText() }
                    },
                )
            )
            SettingsArea(
                title = stringResource(R.string.behavior),
                fontSize = titleFs,
                selected = selected,
                items = arrayOf(
                    { _, onChange ->
                        SettingsToggle(
                            title = stringResource(R.string.auto_show_keyboard),
                            fontSize = iconFs,
                            onChange = onChange,
                            state = remember { mutableStateOf(prefs.autoShowKeyboard) },
                        ) { toggleKeyboardText() }
                    },
                    { _, onChange ->
                        SettingsToggle(
                            title = stringResource(R.string.display_recent_apps),
                            fontSize = iconFs,
                            onChange = onChange,
                            state = remember { mutableStateOf(prefs.recentAppsDisplayed) }
                        ) { toggleRecentAppsDisplayed() }
                    },
                    { open, onChange ->
                        if (prefs.recentAppsDisplayed) {
                            SettingsSliderItem(
                                title = stringResource(R.string.number_of_recents),
                                fontSize = iconFs,
                                open = open,
                                onChange = onChange,
                                currentSelection = remember { mutableIntStateOf(prefs.recentCounter) },
                                min = Constants.RECENT_COUNTER_MIN,
                                max = Constants.RECENT_COUNTER_MAX,
                                onSelect = { j -> setRecentCounter(j) }
                            )
                        }
                    },
                    { _, onChange ->
                        SettingsToggle(
                            title = stringResource(R.string.auto_open_apps),
                            fontSize = iconFs,
                            onChange = onChange,
                            state = remember { mutableStateOf(prefs.autoOpenApp) },
                        ) { toggleAutoOpenApp() }
                    },
                    { open, onChange ->
                        SettingsSliderItem(
                            title = stringResource(R.string.filter_strength),
                            fontSize = iconFs,
                            open = open,
                            onChange = onChange,
                            currentSelection = remember { mutableIntStateOf(prefs.filterStrength) },
                            min = Constants.FILTER_STRENGTH_MIN,
                            max = Constants.FILTER_STRENGTH_MAX,
                            onSelect = { j -> setFilterStrength(j) }
                        )
                    },
                    { _, onChange ->
                        SettingsToggle(
                            title = stringResource(R.string.search_from_start),
                            fontSize = iconFs,
                            onChange = onChange,
                            state = remember { mutableStateOf(prefs.searchFromStart) },
                        ) { toggleSearchFromStart() }
                    },
                    { _, onChange ->
                        SettingsToggle(
                            title = stringResource(R.string.icon_font),
                            fontSize = iconFs,
                            onChange = onChange,
                            state = remember { mutableStateOf(prefs.useCustomIconFont) },
                        ) { toggleCustomIconFont() }
                    },
                )
            )
            SettingsArea(
                title = stringResource(R.string.homescreen),
                fontSize = titleFs,
                selected = selected,
                items = arrayOf(
                    { open, onChange ->
                        SettingsSliderItem(
                            title = stringResource(R.string.apps_on_home_screen),
                            fontSize = iconFs,
                            open = open,
                            onChange = onChange,
                            currentSelection = remember { mutableIntStateOf(prefs.homeAppsNum) },
                            min = Constants.MIN_HOME_APPS,
                            max = Constants.MAX_HOME_APPS,
                            onSelect = { j -> setHomeAppsNum(j) }
                        )
                    },
                    { open, onChange ->
                        if (prefs.homeAppsNum >= 1) {
                            SettingsSliderItem(
                                title = stringResource(R.string.pages_on_home_screen),
                                fontSize = iconFs,
                                open = open,
                                onChange = onChange,
                                currentSelection = remember { mutableIntStateOf(prefs.homePagesNum) },
                                min = Constants.MIN_HOME_PAGES,
                                max = Constants.MAX_HOME_PAGES,
                                onSelect = { j -> setHomePagesNum(j) }
                            )
                        }
                    },
                    { _, onChange ->
                        if (prefs.homePagesNum > 1) {
                            SettingsToggle(
                                title = stringResource(R.string.enable_home_pager),
                                fontSize = iconFs,
                                onChange = onChange,
                                state = remember { mutableStateOf(prefs.homePagerOn) }
                            ) { toggleHomePagerOn() }
                        }
                    },
                    { _, onChange ->
                        SettingsToggle(
                            title = stringResource(R.string.show_time),
                            fontSize = iconFs,
                            onChange = onChange,
                            state = remember { mutableStateOf(prefs.showTime) }
                        ) { toggleShowTime() }
                    },
                    { _, onChange ->
                        if (prefs.showTime) {
                            SettingsToggle(
                                title = stringResource(R.string.show_time_format),
                                fontSize = iconFs,
                                onChange = onChange,
                                state = remember { mutableStateOf(prefs.showTimeFormat) }
                            ) { toggleShowTimeFormat() }
                        }
                    },
                    { _, onChange ->
                        SettingsToggle(
                            title = stringResource(R.string.show_date),
                            fontSize = iconFs,
                            onChange = onChange,
                            state = remember { mutableStateOf(prefs.showDate) }
                        ) { toggleShowDate() }
                    },
                    { _, onChange ->
                        SettingsToggle(
                            title = stringResource(R.string.show_battery),
                            fontSize = iconFs,
                            onChange = onChange,
                            state = remember { mutableStateOf(prefs.showBattery) }
                        ) { toggleShowBattery() }
                    },
                    { _, onChange ->
                        if(prefs.showBattery) {
                            SettingsToggle(
                                title = stringResource(R.string.show_battery_icon),
                                fontSize = iconFs,
                                onChange = onChange,
                                state = remember { mutableStateOf(prefs.showBatteryIcon) }
                            ) { toggleShowBatteryIcon() }
                        }
                    },
                    { _, onChange ->
                        SettingsToggle(
                            title = stringResource(R.string.lock_home_apps),
                            fontSize = iconFs,
                            onChange = onChange,
                            state = remember { mutableStateOf(prefs.homeLocked) }
                        ) { toggleHomeLocked() }
                    },
                    { _, onChange ->
                        SettingsToggle(
                            title = stringResource(R.string.extend_home_apps_area),
                            fontSize = iconFs,
                            onChange = onChange,
                            state = remember { mutableStateOf(prefs.extendHomeAppsArea) }
                        ) { toggleExtendHomeAppsArea() }
                    },
                    { _, onChange ->
                        SettingsToggle(
                            title = stringResource(R.string.home_alignment_bottom),
                            fontSize = iconFs,
                            onChange = onChange,
                            state = remember { mutableStateOf(prefs.homeAlignmentBottom) }
                        ) { toggleHomeAppsBottom() }
                    }
                )
            )
            SettingsArea(
                title = stringResource(R.string.alignment),
                fontSize = titleFs,
                selected = selected,
                items = arrayOf(
                    { _, onChange ->
                        SettingsToggle(
                            title = stringResource(R.string.display_app_usage_stats),
                            fontSize = iconFs,
                            onChange = onChange,
                            state = remember { mutableStateOf(prefs.appUsageStats) }
                        ) { toggleAppUsageStats() }
                    },
                    { open, onChange ->
                        if (!prefs.appUsageStats) {
                            SettingsItem(
                                title = stringResource(R.string.home_alignment),
                                fontSize = iconFs,
                                open = open,
                                onChange = onChange,
                                currentSelection = remember { mutableStateOf(prefs.homeAlignment) },
                                values = arrayOf(
                                    Constants.Gravity.Left,
                                    Constants.Gravity.Center,
                                    Constants.Gravity.Right
                                ),
                                onSelect = { gravity -> setHomeAlignment(gravity) }
                            )
                        }
                    },
                    { open, onChange ->
                        SettingsItem(
                            title = stringResource(R.string.clock_alignment),
                            fontSize = iconFs,
                            open = open,
                            onChange = onChange,
                            currentSelection = remember { mutableStateOf(prefs.clockAlignment) },
                            values = arrayOf(
                                Constants.Gravity.Left,
                                Constants.Gravity.Center,
                                Constants.Gravity.Right
                            ),
                            onSelect = { gravity -> setClockAlignment(gravity) }
                        )
                    },
                    { open, onChange ->
                        SettingsItem(
                            title = stringResource(R.string.drawer_alignment),
                            fontSize = iconFs,
                            open = open,
                            onChange = onChange,
                            currentSelection = remember { mutableStateOf(prefs.drawerAlignment) },
                            values = arrayOf(
                                Constants.Gravity.Left,
                                Constants.Gravity.Center,
                                Constants.Gravity.Right
                            ),
                            onSelect = { j -> viewModel.updateDrawerAlignment(j) }
                        )
                    },
                )
            )
            SettingsArea(
                title = stringResource(R.string.gestures),
                fontSize = titleFs,
                selected = selected,
                items = arrayOf(
                    { open, onChange ->
                        SettingsGestureItem(
                            title = stringResource(R.string.short_swipe_up_app),
                            fontSize = iconFs,
                            open = open,
                            onChange = onChange,
                            currentAction = prefs.shortSwipeUpAction,
                            onSelect = { j -> setGesture(AppDrawerFlag.SetShortSwipeUp, j) },
                            appLabel = prefs.appShortSwipeUp.appLabel,
                        )
                    },
                    { open, onChange ->
                        SettingsGestureItem(
                            title = stringResource(R.string.short_swipe_down_app),
                            fontSize = iconFs,
                            open = open,
                            onChange = onChange,
                            currentAction = prefs.shortSwipeDownAction,
                            onSelect = { j -> setGesture(AppDrawerFlag.SetShortSwipeDown, j) },
                            appLabel = prefs.appShortSwipeDown.appLabel,
                        )
                    },
                    { open, onChange ->
                        SettingsGestureItem(
                            title = stringResource(R.string.short_swipe_left_app),
                            fontSize = iconFs,
                            open = open,
                            onChange = onChange,
                            currentAction = prefs.shortSwipeLeftAction,
                            onSelect = { j -> setGesture(AppDrawerFlag.SetShortSwipeLeft, j) },
                            appLabel = prefs.appShortSwipeLeft.appLabel.ifEmpty { "Camera" },
                        )
                    },
                    { open, onChange ->
                        SettingsGestureItem(
                            title = stringResource(R.string.short_swipe_right_app),
                            fontSize = iconFs,
                            open = open,
                            onChange = onChange,
                            currentAction = prefs.shortSwipeRightAction,
                            onSelect = { j -> setGesture(AppDrawerFlag.SetShortSwipeRight, j) },
                            appLabel = prefs.appShortSwipeRight.appLabel.ifEmpty { "Phone" },
                        )
                    },
                    { open, onChange ->
                        SettingsGestureItem(
                            title = stringResource(R.string.long_swipe_up_app),
                            fontSize = iconFs,
                            open = open,
                            onChange = onChange,
                            currentAction = prefs.longSwipeUpAction,
                            onSelect = { j -> setGesture(AppDrawerFlag.SetLongSwipeUp, j) },
                            appLabel = prefs.appLongSwipeUp.appLabel,
                        )
                    },
                    { open, onChange ->
                        SettingsGestureItem(
                            title = stringResource(R.string.long_swipe_down_app),
                            fontSize = iconFs,
                            open = open,
                            onChange = onChange,
                            currentAction = prefs.longSwipeDownAction,
                            onSelect = { j -> setGesture(AppDrawerFlag.SetLongSwipeDown, j) },
                            appLabel = prefs.appLongSwipeDown.appLabel,
                        )
                    },
                    { open, onChange ->
                        SettingsGestureItem(
                            title = stringResource(R.string.long_swipe_left_app),
                            fontSize = iconFs,
                            open = open,
                            onChange = onChange,
                            currentAction = prefs.longSwipeLeftAction,
                            onSelect = { j -> setGesture(AppDrawerFlag.SetLongSwipeLeft, j) },
                            appLabel = prefs.appLongSwipeLeft.appLabel,
                        )
                    },
                    { open, onChange ->
                        SettingsGestureItem(
                            title = stringResource(R.string.long_swipe_right_app),
                            fontSize = iconFs,
                            open = open,
                            onChange = onChange,
                            currentAction = prefs.longSwipeRightAction,
                            onSelect = { j -> setGesture(AppDrawerFlag.SetLongSwipeRight, j) },
                            appLabel = prefs.appLongSwipeRight.appLabel,
                        )
                    },
                    { open, onChange ->
                        SettingsGestureItem(
                            title = stringResource(R.string.clock_click_app),
                            fontSize = iconFs,
                            open = open,
                            onChange = onChange,
                            currentAction = prefs.clickClockAction,
                            onSelect = { j -> setGesture(AppDrawerFlag.SetClickClock, j) },
                            appLabel = prefs.appClickClock.appLabel.ifEmpty { "Clock" },
                        )
                    },
                    { open, onChange ->
                        SettingsGestureItem(
                            title = stringResource(R.string.date_click_app),
                            fontSize = iconFs,
                            open = open,
                            onChange = onChange,
                            currentAction = prefs.clickDateAction,
                            onSelect = { j -> setGesture(AppDrawerFlag.SetClickDate, j) },
                            appLabel = prefs.appClickDate.appLabel.ifEmpty { "Calendar" },
                        )
                    },
                    { open, onChange ->
                        SettingsGestureItem(
                            title = stringResource(R.string.usage_click_app),
                            fontSize = iconFs,
                            open = open,
                            onChange = onChange,
                            currentAction = prefs.clickAppUsageAction,
                            onSelect = { j -> setGesture(AppDrawerFlag.SetAppUsage, j) },
                            appLabel = prefs.appClickUsage.appLabel.ifEmpty { "Digital Wellbeing" },
                        )
                    },
                    { open, onChange ->
                        SettingsGestureItem(
                            title = stringResource(R.string.double_tap),
                            fontSize = iconFs,
                            open = open,
                            onChange = onChange,
                            currentAction = prefs.doubleTapAction,
                            onSelect = { j -> setGesture(AppDrawerFlag.SetDoubleTap, j) },
                            appLabel = prefs.appDoubleTap.appLabel
                        )
                    }
                )
            )
            SettingsArea(
                title = getString(R.string.miscellaneous),
                selected = selected,
                fontSize = titleFs,
                items = arrayOf(
                    { open, onChange ->
                        SettingsItem(
                            title = stringResource(R.string.search_engine),
                            fontSize = iconFs,
                            open = open,
                            onChange = onChange,
                            currentSelection = remember { mutableStateOf(prefs.searchEngines) },
                            values = Constants.SearchEngines.values(),
                            onSelect = { j -> setEngine(j) }
                        )
                    },
                    { open, onChange ->
                        SettingsSliderItem(
                            title = stringResource(R.string.settings_text_size),
                            fontSize = iconFs,
                            open = open,
                            onChange = onChange,
                            currentSelection = remember { mutableIntStateOf(prefs.textSizeSettings) },
                            min = Constants.TEXT_SIZE_MIN,
                            max = Constants.TEXT_SIZE_MAX,
                            onSelect = { f -> setTextSizeSettings(f) }
                        )
                    },
                    { _, onChange ->
                        SettingsToggle(
                            title = stringResource(R.string.display_hidden_apps),
                            fontSize = iconFs,
                            onChange = onChange,
                            state = remember { mutableStateOf(prefs.hiddenAppsDisplayed) }
                        ) { toggleHiddenAppsDisplayed() }
                    },
                    { _, onChange ->
                        SettingsToggle(
                            title = stringResource(R.string.lock_settings),
                            fontSize = iconFs,
                            onChange = onChange,
                            state = remember { mutableStateOf(prefs.settingsLocked) }
                        ) { toggleSettingsLocked() }
                    },
                )
            )
            SettingsArea(
                title = getString(R.string.backup),
                selected = selected,
                fontSize = titleFs,
                items = arrayOf(
                    { _, _ ->
                        SettingsThreeButtonRow(
                            fontSize = iconFs,
                            firstButtonText = getString(R.string.load_backup),
                            secondButtonText = getString(R.string.save_backup),
                            thirdButtonText = getString(R.string.clear_backup),
                            firstButtonAction = {
                                loadFile(requireActivity())
                            },
                            secondButtonAction = {
                                storeFile(requireActivity())
                            },
                            thirdButtonAction = {
                                prefs.clear()
                                requireActivity().recreate()
                            },
                        )
                    }
                )
            )
            Text(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(10.dp, 5.dp),
                text = "${getString(R.string.version)}: ${
                    requireContext().packageManager.getPackageInfo(
                        requireContext().packageName,
                        0
                    ).versionName
                }",
                fontSize = iconFs,
                color = Color.LightGray
            )
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

    private fun toggleCustomIconFont() {
        prefs.useCustomIconFont = !prefs.useCustomIconFont
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

    private fun showReorderApps() {
        viewModel.getHiddenApps()
        findNavController().navigate(
            R.id.action_settingsFragment_to_appListReorderFragment,
            bundleOf("flag" to AppDrawerFlag.ReorderApps.toString())
        )
    }

    private fun checkAdminPermission() {
        val isAdmin: Boolean = deviceManager.isAdminActive(componentName)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
            prefs.lockModeOn = isAdmin
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

    private fun toggleFollowAccent() {
        prefs.followAccentColors = !prefs.followAccentColors
    }

    private fun setTheme(appTheme: Constants.Theme) {
        prefs.appTheme = appTheme
        requireActivity().recreate()
    }

    private fun setDarkColors(appTheme: Constants.DarkColors) {
        prefs.appDarkColors = appTheme
        requireActivity().recreate()
    }

    private fun setLightColors(appTheme: Constants.LightColors) {
        prefs.appLightColors = appTheme
        requireActivity().recreate()
    }

    private fun setLang(langInt: Constants.Language) {
        prefs.language = langInt
        requireActivity().recreate()
    }

    private fun setEngine(engineInt: Constants.SearchEngines) {
        prefs.searchEngines = engineInt
        requireActivity().recreate()
    }

    private fun setTextSizeLauncher(size: Int) {
        prefs.textSizeLauncher = size
        requireActivity().recreate()
    }

    private fun setTextSizeSettings(size: Int) {
        prefs.textSizeSettings = size
        requireActivity().recreate()
    }

    private fun setClockSizeSettings(size: Int) {
        prefs.clockSize = size
        requireActivity().recreate()
    }

    private fun setTextMarginSize(size: Int) {
        prefs.textMarginSize = size
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


