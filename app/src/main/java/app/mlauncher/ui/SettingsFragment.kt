package app.mlauncher.ui

import app.mlauncher.style.SettingsTheme
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import app.mlauncher.BuildConfig
import app.mlauncher.MainViewModel
import app.mlauncher.R
import app.mlauncher.data.Constants
import app.mlauncher.data.Constants.Action
import app.mlauncher.data.Constants.AppDrawerFlag
import app.mlauncher.data.Constants.Theme.*
import app.mlauncher.data.Prefs
import app.mlauncher.databinding.FragmentSettingsBinding
import app.mlauncher.helper.*
import app.mlauncher.listener.DeviceAdmin
import app.mlauncher.ui.compose.SettingsComposable.SettingsArea
import app.mlauncher.ui.compose.SettingsComposable.SettingsItem
import app.mlauncher.ui.compose.SettingsComposable.SettingsGestureItem
import app.mlauncher.ui.compose.SettingsComposable.SettingsNumberItem
import app.mlauncher.ui.compose.SettingsComposable.SettingsToggle
import app.mlauncher.ui.compose.SettingsComposable.SettingsTopView
import app.mlauncher.ui.compose.SettingsComposable.SettingsTextButton

class SettingsFragment : Fragment() {

    private lateinit var prefs: Prefs
    private var viewModel: MainViewModel? = null
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

        binding.settingsView.setContent {

            val isDark = when (prefs.appTheme) {
                Light -> false
                Dark -> true
                System -> isSystemInDarkTheme()
            }

            SettingsTheme(isDark) {
                Settings()
            }
        }
    }

    @Composable
    private fun Settings() {
        val selected = remember { mutableStateOf("") }

        val changeLauncherText = if (ismlauncherDefault(requireContext())) {
            R.string.change_default_launcher
        } else {
           R.string.set_as_default_launcher
        }

        Column {
            SettingsTopView(
                stringResource(R.string.app_name),
                onClick = { openAppInfo(requireContext(), android.os.Process.myUserHandle(), BuildConfig.APPLICATION_ID) },
            ) {
                SettingsTextButton(stringResource(R.string.hidden_apps) ) {
                    showHiddenApps()
                }
                SettingsTextButton(stringResource(changeLauncherText) ) {
                    resetDefaultLauncher(requireContext())
                }
            }
            SettingsArea(
                title = stringResource(R.string.appearance),
                selected = selected,
                items = arrayOf(
                    { _, onChange ->
                        SettingsToggle(
                            title = stringResource(R.string.status_bar),
                            onChange = onChange,

                            state = remember { mutableStateOf(prefs.showStatusBar) },
                        ) { toggleStatusBar() }
                    },
                    { open, onChange ->
                        SettingsItem(
                            title = stringResource(R.string.theme_mode),
                            open = open,
                            onChange = onChange,

                            currentSelection = remember { mutableStateOf(prefs.appTheme) },
                            values = arrayOf(System, Light, Dark),
                            onSelect = { j -> setTheme(j) }
                        )
                    },
                    { open, onChange ->
                        SettingsItem(
                            open = open,
                            onChange = onChange,

                            title = stringResource(R.string.app_language),
                            currentSelection = remember { mutableStateOf(prefs.language) },
                            values = Constants.Language.values(),
                            onSelect = { j -> setLang(j) }
                        )
                    },
                    { open, onChange ->
                        SettingsNumberItem(
                            title = stringResource(R.string.app_text_size),
                            open = open,

                            onChange = onChange,
                            currentSelection = remember { mutableStateOf(prefs.textSize) },
                            min = Constants.TEXT_SIZE_MIN,
                            max = Constants.TEXT_SIZE_MAX,
                            onValueChange = { },
                            onSelect = { f -> setTextSize(f) }
                        )
                    }
                )
            )
            SettingsArea(title = stringResource(R.string.homescreen),
                selected = selected,
                items = arrayOf(
                    { open, onChange ->
                        SettingsNumberItem(
                            title = stringResource(R.string.apps_on_home_screen),
                            open = open,

                            onChange = onChange,
                            currentSelection = remember { mutableStateOf(prefs.homeAppsNum) },
                            min = 0,
                            max = Constants.MAX_HOME_APPS,
                            onSelect = { j -> updateHomeAppsNum(j) }
                        )
                    },
                    { _, onChange ->
                        SettingsToggle(
                            title = stringResource(R.string.show_time),
                            onChange = onChange,

                            state = remember { mutableStateOf(prefs.showTime) }
                        ) { toggleShowTime() }
                    },
                    { _, onChange ->
                        SettingsToggle(
                            title = stringResource(R.string.show_date),
                            onChange = onChange,

                            state = remember { mutableStateOf(prefs.showDate) }
                        ) { toggleShowDate() }
                    },
                    { _, onChange ->
                        SettingsToggle(
                            title = stringResource(R.string.lock_home_apps),
                            onChange = onChange,

                            state = remember { mutableStateOf(prefs.homeLocked) }
                        ) { prefs.homeLocked = !prefs.homeLocked }
                    }
                )
            )
            SettingsArea(title = stringResource(R.string.alignment),
                selected = selected,
                items = arrayOf(
                    { open, onChange ->
                        SettingsItem(
                            title = stringResource(R.string.home_alignment),
                            open = open,
                            onChange = onChange,

                            currentSelection = remember { mutableStateOf(prefs.homeAlignment) },
                            values = arrayOf(Constants.Gravity.Left, Constants.Gravity.Center, Constants.Gravity.Right),
                            onSelect = { gravity -> setHomeAlignment(gravity) }
                        )
                    },
                    { open, onChange ->
                        SettingsItem(
                            title = stringResource(R.string.clock_alignment),
                            open = open,
                            onChange = onChange,

                            currentSelection = remember { mutableStateOf(prefs.clockAlignment) },
                            values = arrayOf(Constants.Gravity.Left, Constants.Gravity.Center, Constants.Gravity.Right),
                            onSelect = { gravity -> setClockAlignment(gravity) }
                        )
                    },
                    { open, onChange ->
                        SettingsItem(
                            title = stringResource(R.string.drawer_alignment),
                            open = open,
                            onChange = onChange,

                            currentSelection = remember { mutableStateOf(prefs.drawerAlignment) },
                            values = arrayOf(Constants.Gravity.Left, Constants.Gravity.Center, Constants.Gravity.Right),
                            onSelect = { j -> viewModel?.updateDrawerAlignment(j) }
                        )
                    },
                )
            )
            SettingsArea(title = stringResource(R.string.gestures),
                selected = selected,
                items = arrayOf(
                    { open, onChange ->
                        SettingsGestureItem(
                            title = stringResource(R.string.swipe_left_app),
                            open = open,
                            onChange = onChange,
                            currentAction = prefs.swipeLeftAction,
                            onSelect = { j -> updateGesture(AppDrawerFlag.SetSwipeLeft, j) },
                            appLabel = prefs.appSwipeLeft.appLabel.ifEmpty { "Camera" },
                        )
                    },
                    { open, onChange ->
                        SettingsGestureItem(
                            title = stringResource(R.string.swipe_right_app),
                            open = open,
                            onChange = onChange,
                            currentAction = prefs.swipeRightAction,
                            onSelect = { j -> updateGesture(AppDrawerFlag.SetSwipeRight, j) },
                            appLabel = prefs.appSwipeRight.appLabel.ifEmpty { "Phone" },
                        )
                    },
                    { open, onChange ->
                        SettingsGestureItem(
                            title = stringResource(R.string.swipe_down_app),
                            open = open,
                            onChange = onChange,
                            currentAction = prefs.swipeDownAction,
                            onSelect = { j -> updateGesture(AppDrawerFlag.SetSwipeDown, j) },
                            appLabel = prefs.appSwipeDown.appLabel,
                        )
                    },
                    { open, onChange ->
                        SettingsGestureItem(
                            title = stringResource(R.string.swipe_up_app),
                            open = open,
                            onChange = onChange,
                            currentAction = prefs.swipeUpAction,
                            onSelect = { j -> updateGesture(AppDrawerFlag.SetSwipeUp, j) },
                            appLabel = prefs.appSwipeUp.appLabel,
                        )
                    },
                    { open, onChange ->
                        SettingsGestureItem(
                            title = stringResource(R.string.clock_click_app),
                            open = open,
                            onChange = onChange,
                            currentAction = prefs.clickClockAction,
                            onSelect = { j -> updateGesture(AppDrawerFlag.SetClickClock, j) },
                            appLabel = prefs.appClickClock.appLabel.ifEmpty { "Clock" },
                        )
                    },
                    { open, onChange ->
                        SettingsGestureItem(
                            title = stringResource(R.string.date_click_app),
                            open = open,
                            onChange = onChange,
                            currentAction = prefs.clickDateAction,
                            onSelect = { j -> updateGesture(AppDrawerFlag.SetClickDate, j) },
                            appLabel = prefs.appClickDate.appLabel.ifEmpty { "Calendar" },
                        )
                    },
                    { open, onChange ->
                        SettingsGestureItem(
                            title = stringResource(R.string.double_tap),
                            open = open,
                            onChange = onChange,
                            currentAction = prefs.doubleTapAction,
                            onSelect = { j -> updateGesture(AppDrawerFlag.SetDoubleTap, j) },
                            appLabel = prefs.appClickDate.appLabel
                        )
                    }
                )
            )
            SettingsArea(title = stringResource(R.string.behavior),
                selected = selected,
                items = arrayOf(
                    { _, onChange ->
                        SettingsToggle(
                            title = stringResource(R.string.auto_show_keyboard),
                            onChange = onChange,

                            state = remember { mutableStateOf(prefs.autoShowKeyboard) },
                        ) { toggleKeyboardText() }
                    },
                    { _, onChange ->
                        SettingsToggle(
                            title = stringResource(R.string.auto_open_apps),
                            onChange = onChange,

                            state = remember { mutableStateOf(prefs.autoOpenApp) },
                        ) { toggleAutoOpenApp() }
                    },
                    { _, onChange ->
                        SettingsToggle(
                            title = stringResource(R.string.home_alignment_bottom),
                            onChange = onChange,

                            state = remember { mutableStateOf(prefs.homeAlignmentBottom) }
                        ) { toggleHomeAppsBottom() }
                    },
                    { _, onChange ->
                        SettingsToggle(
                            title = stringResource(R.string.extend_home_apps_area),
                            onChange = onChange,

                            state = remember { mutableStateOf(prefs.extendHomeAppsArea) }
                        ) { prefs.extendHomeAppsArea = !prefs.extendHomeAppsArea }
                    }
                )
            )
            @Suppress("DEPRECATION")
            Text(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(10.dp, 5.dp),
                text = "Version: ${requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName}",

                color = Color.DarkGray
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

        viewModel!!.ismlauncherDefault()

        deviceManager = context?.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        componentName = ComponentName(requireContext(), DeviceAdmin::class.java)
        checkAdminPermission()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setHomeAlignment(gravity: Constants.Gravity) {
        prefs.homeAlignment = gravity
        viewModel?.updateHomeAppsAlignment(gravity, prefs.homeAlignmentBottom)
    }

    private fun toggleHomeAppsBottom() {
        val onBottom  = !prefs.homeAlignmentBottom

        prefs.homeAlignmentBottom = onBottom
        viewModel?.updateHomeAppsAlignment(prefs.homeAlignment, onBottom)
    }

    private fun setClockAlignment(gravity: Constants.Gravity) {
        prefs.clockAlignment = gravity
        viewModel?.updateClockAlignment(gravity)
    }

    private fun toggleStatusBar() {
        val showStatusbar = !prefs.showStatusBar
        prefs.showStatusBar = showStatusbar
        if (showStatusbar) showStatusBar(requireActivity()) else hideStatusBar(requireActivity())
    }

    private fun toggleShowDate() {
        prefs.showDate = !prefs.showDate
        viewModel?.setShowDate(prefs.showDate)
    }

    private fun toggleShowTime() {
        prefs.showTime = !prefs.showTime
        viewModel?.setShowTime(prefs.showTime)
    }

    private fun showHiddenApps() {
        viewModel?.getHiddenApps()
        findNavController().navigate(
            R.id.action_settingsFragment_to_appListFragment,
            bundleOf("flag" to AppDrawerFlag.HiddenApps.toString())
        )
    }

    private fun checkAdminPermission() {
        val isAdmin: Boolean = deviceManager.isAdminActive(componentName)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
            prefs.lockModeOn = isAdmin
    }

    private fun updateHomeAppsNum(homeAppsNum: Int) {
        prefs.homeAppsNum = homeAppsNum
        viewModel?.homeAppsCount?.value = homeAppsNum
    }

    private fun toggleKeyboardText() {
        prefs.autoShowKeyboard = !prefs.autoShowKeyboard
    }

    private fun toggleAutoOpenApp() {
        prefs.autoOpenApp = !prefs.autoOpenApp
    }

    private fun setTheme(appTheme: Constants.Theme) {
        prefs.appTheme = appTheme
        requireActivity().recreate()
    }

    private fun setLang(lang_int: Constants.Language) {
        prefs.language = lang_int
        requireActivity().recreate()
    }
    private fun setTextSize(size: Int) {
        prefs.textSize = size
        requireActivity().recreate()
    }

    private fun updateGesture(flag: AppDrawerFlag, action: Action) {
        when (flag){
            AppDrawerFlag.SetSwipeLeft -> prefs.swipeLeftAction = action
            AppDrawerFlag.SetSwipeRight -> prefs.swipeRightAction = action
            AppDrawerFlag.SetSwipeDown -> prefs.swipeDownAction = action
            AppDrawerFlag.SetSwipeUp -> prefs.swipeUpAction = action
            AppDrawerFlag.SetClickClock -> prefs.clickClockAction = action
            AppDrawerFlag.SetClickDate -> prefs.clickDateAction = action
            AppDrawerFlag.SetDoubleTap -> prefs.doubleTapAction = action
            AppDrawerFlag.SetHomeApp,
            AppDrawerFlag.HiddenApps,
            AppDrawerFlag.LaunchApp -> {}
        }

        when(action) {
            Action.OpenApp -> {
                viewModel?.getAppList()
                findNavController().navigate(
                    R.id.action_settingsFragment_to_appListFragment,
                    bundleOf("flag" to flag.toString())
                )
            }
            else -> {}
        }
    }
}
