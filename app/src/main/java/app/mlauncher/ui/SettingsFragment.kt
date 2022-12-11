package app.mlauncher.ui

import SettingsTheme
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import app.mlauncher.ui.compose.SettingsComposable.SettingsAppSelector
import app.mlauncher.ui.compose.SettingsComposable.SettingsArea
import app.mlauncher.ui.compose.SettingsComposable.SettingsItem
import app.mlauncher.ui.compose.SettingsComposable.SettingsNumberItem
import app.mlauncher.ui.compose.SettingsComposable.SettingsToggle
import app.mlauncher.ui.compose.SettingsComposable.SettingsTopView
import app.mlauncher.ui.compose.SettingsComposable.SimpleTextButton

class SettingsFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var deviceManager: DevicePolicyManager
    private lateinit var componentName: ComponentName

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val offset = 5

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

        binding.testView.setContent {

            val isDark = when (prefs.appTheme) {
                Light -> false
                Dark -> true
                System -> isSystemInDarkTheme()
            }

            SettingsTheme(isDark) {
                Settings((prefs.textSize - offset).sp)
            }
        }
    }

    @Composable
    private fun Settings(fontSize: TextUnit = TextUnit.Unspecified) {
        val selected = remember { mutableStateOf("") }
        /*val fs = remember { mutableStateOf(fontSize) }

        val titleFs = if (fs.value.isSpecified) {
            (fs.value.value * 2).sp
        } else fs.value

        val iconFs = if (fs.value.isSpecified) {
            (fs.value.value * 1.5).sp
        } else fs.value */

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
                SimpleTextButton(stringResource(R.string.hidden_apps) ) {
                    showHiddenApps()
                }
                SimpleTextButton(stringResource(changeLauncherText) ) {
                    resetDefaultLauncher(requireContext())
                }
            }
            SettingsArea(
                title = stringResource(R.string.appearance),
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
                            onValueChange = { }, // newSize -> fs.value = (newSize - offset).sp },
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
                    },
                    { _, onChange ->
                        SettingsToggle(
                            title = stringResource(R.string.extend_home_apps_area),
                            onChange = onChange,

                            state = remember { mutableStateOf(prefs.extendHomeAppsArea) }
                        ) { prefs.extendHomeAppsArea = !prefs.extendHomeAppsArea }
                    },
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
                    { _, onChange ->
                        SettingsToggle(
                            title = stringResource(R.string.home_alignment_bottom),
                            onChange = onChange,

                            state = remember { mutableStateOf(prefs.homeAlignmentBottom) }
                        ) { toggleHomeAppsBottom() }
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
                            onSelect = { j -> viewModel.updateDrawerAlignment(j) }
                        )
                    },
                )
            )
            SettingsArea(title = stringResource(R.string.gestures),
                selected = selected,
                items = arrayOf(
                    { open, onChange ->
                        SettingsItem(
                            open = open,
                            onChange = onChange,
                            title = stringResource(R.string.swipe_left_app),
                            currentSelection = remember { mutableStateOf(prefs.swipeLeftAction) },
                            currentSelectionName = if (prefs.swipeLeftAction == Action.OpenApp) "Open ${prefs.appSwipeLeft.appLabel}" else prefs.swipeLeftAction.string(),
                            values = Action.values(),
                            active = prefs.swipeLeftAction != Action.Disabled,
                            onSelect = { j -> updateGesture(AppDrawerFlag.SetSwipeLeft, j) }
                        )
                    },
                    { open, onChange ->
                        SettingsItem(
                            open = open,
                            onChange = onChange,
                            title = stringResource(R.string.swipe_right_app),
                            currentSelection = remember { mutableStateOf(prefs.swipeRightAction) },
                            currentSelectionName = if (prefs.swipeRightAction == Action.OpenApp) "Open ${prefs.appSwipeRight.appLabel}" else prefs.swipeRightAction.string(),
                            values = Action.values(),
                            active = prefs.swipeRightAction != Action.Disabled,
                            onSelect = { j -> updateGesture(AppDrawerFlag.SetSwipeRight, j) }
                        )
                    },
                    { open, onChange ->
                        SettingsItem(
                            open = open,
                            onChange = onChange,
                            title = stringResource(R.string.swipe_down_app),
                            currentSelection = remember { mutableStateOf(prefs.swipeDownAction) },
                            currentSelectionName = if (prefs.swipeDownAction == Action.OpenApp) "Open ${prefs.appSwipeDown.appLabel}" else prefs.swipeDownAction.string(),
                            values = Action.values(),
                            active = prefs.swipeDownAction != Action.Disabled,
                            onSelect = { j -> updateGesture(AppDrawerFlag.SetSwipeDown, j) }
                        )
                    },
                    { open, onChange ->
                        SettingsItem(
                            open = open,
                            onChange = onChange,
                            title = stringResource(R.string.clock_click_app),
                            currentSelection = remember { mutableStateOf(prefs.clickClockAction) },
                            currentSelectionName = if (prefs.clickClockAction == Action.OpenApp) "Open ${prefs.appClickClock.appLabel}" else prefs.clickClockAction.string(),
                            values = Action.values(),
                            active = prefs.clickClockAction != Action.Disabled,
                            onSelect = { j -> updateGesture(AppDrawerFlag.SetClickClock, j) },
                        )
                    },
                    { open, onChange ->
                        SettingsItem(
                            open = open,
                            onChange = onChange,
                            title = stringResource(R.string.date_click_app),
                            currentSelection = remember { mutableStateOf(prefs.clickDateAction) },
                            currentSelectionName = if (prefs.clickDateAction == Action.OpenApp) "Open ${prefs.appClickDate.appLabel}" else prefs.clickDateAction.string(),
                            values = Action.values(),
                            active = prefs.clickDateAction != Action.Disabled,
                            onSelect = { j -> updateGesture(AppDrawerFlag.SetClickDate, j) },
                        )
                    },
                    { _, onChange ->
                        SettingsToggle(
                            title = stringResource(R.string.double_tap_to_lock_screen),
                            onChange = onChange,

                            state = remember { mutableStateOf(prefs.lockModeOn) }
                        ) { toggleLockMode() }
                    }
                )
            )
            Text(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(10.dp, 5.dp),
                text = "Version: ${requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName}",

                color = Color.DarkGray
            )
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        prefs = Prefs(requireContext())
        viewModel = activity?.run {
            ViewModelProvider(this).get(MainViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        viewModel.ismlauncherDefault()

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
        viewModel.updateHomeAppsAlignment(gravity, prefs.homeAlignmentBottom)
    }

    private fun toggleHomeAppsBottom() {
        val onBottom  = !prefs.homeAlignmentBottom

        prefs.homeAlignmentBottom = onBottom
        viewModel.updateHomeAppsAlignment(prefs.homeAlignment, onBottom)
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

    private fun toggleShowDate() {
        prefs.showDate = !prefs.showDate
        viewModel.setShowDate(prefs.showDate)
    }

    private fun toggleShowTime() {
        prefs.showTime = !prefs.showTime
        viewModel.setShowTime(prefs.showTime)
    }

    private fun showHiddenApps() {
        viewModel.getHiddenApps()
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

    private fun toggleLockMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            when {
                prefs.lockModeOn -> {
                    prefs.lockModeOn = false
                    deviceManager.removeActiveAdmin(componentName) // for backward compatibility
                }
                isAccessServiceEnabled(requireContext()) -> prefs.lockModeOn = true
                else -> {
                    showToastLong(requireContext(), "Please turn on accessibility service for mlauncher")
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            }
        } else {
            val isAdmin: Boolean = deviceManager.isAdminActive(componentName)
            if (isAdmin) {
                deviceManager.removeActiveAdmin(componentName)
                prefs.lockModeOn = false
                showToastShort(requireContext(), "Admin permission removed.")
            } else {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                intent.putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    getString(R.string.admin_permission_message)
                )
                activity?.startActivityForResult(intent, Constants.REQUEST_CODE_ENABLE_ADMIN)
            }
        }
    }

    private fun updateHomeAppsNum(homeAppsNum: Int) {
        prefs.homeAppsNum = homeAppsNum
        viewModel.homeAppsCount.value = homeAppsNum
    }

    private fun toggleKeyboardText() {
        prefs.autoShowKeyboard = !prefs.autoShowKeyboard
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
        prefs.swipeDownAction = action

        when(action) {
            Action.OpenApp -> {
                viewModel.getAppList()
                findNavController().navigate(
                    R.id.action_settingsFragment_to_appListFragment,
                    bundleOf("flag" to flag.toString())
                )
            }
            else -> {}
        }
    }
}
