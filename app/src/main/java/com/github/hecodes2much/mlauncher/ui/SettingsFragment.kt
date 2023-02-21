package com.github.hecodes2much.mlauncher.ui

import com.github.hecodes2much.mlauncher.style.SettingsTheme
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RequiresApi
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
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.github.hecodes2much.mlauncher.BuildConfig
import com.github.hecodes2much.mlauncher.MainViewModel
import com.github.hecodes2much.mlauncher.R
import com.github.hecodes2much.mlauncher.data.Constants
import com.github.hecodes2much.mlauncher.data.Constants.Action
import com.github.hecodes2much.mlauncher.data.Constants.AppDrawerFlag
import com.github.hecodes2much.mlauncher.data.Constants.Theme.*
import com.github.hecodes2much.mlauncher.data.Prefs
import com.github.hecodes2much.mlauncher.databinding.FragmentSettingsBinding
import com.github.hecodes2much.mlauncher.helper.*
import com.github.hecodes2much.mlauncher.listener.DeviceAdmin
import com.github.hecodes2much.mlauncher.ui.compose.SettingsComposable.SettingsArea
import com.github.hecodes2much.mlauncher.ui.compose.SettingsComposable.SettingsGestureItem
import com.github.hecodes2much.mlauncher.ui.compose.SettingsComposable.SettingsItem
import com.github.hecodes2much.mlauncher.ui.compose.SettingsComposable.SettingsSliderItem
import com.github.hecodes2much.mlauncher.ui.compose.SettingsComposable.SettingsToggle
import com.github.hecodes2much.mlauncher.ui.compose.SettingsComposable.SettingsTopView
import com.github.hecodes2much.mlauncher.ui.compose.SettingsComposable.SettingsTextButton
import com.github.hecodes2much.mlauncher.ui.compose.SettingsComposable.SettingsTwoButtonRow
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class SettingsFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var deviceManager: DevicePolicyManager
    private lateinit var componentName: ComponentName

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val offset = 5

    private lateinit var password1: EditText
    private lateinit var password2: EditText
    private lateinit var password3: EditText
    private lateinit var password4: EditText

    private lateinit var submitButton: Button

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        prefs = Prefs(requireContext())
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)

        if (prefs.lastOpenSettings !== null) {
            val minutesBetween = ChronoUnit.MINUTES.between(
                LocalDateTime.parse(prefs.lastOpenSettings),
                LocalDateTime.now()
            )
            val timer = prefs.lockSettingsTime
            val hasPassedMinutes = isLongerThanMinutes(minutesBetween, timer.toLong())

            if (timer == 0) return binding.root
            if (hasPassedMinutes && prefs.settingPinNumber != 123456) {
                binding.settingsView.visibility = View.GONE

                val view = inflater.inflate(R.layout.fragment_password, container, false)
                showKeyboard(view)

                password1 = view.findViewById(R.id.password_1)
                password2 = view.findViewById(R.id.password_2)
                password3 = view.findViewById(R.id.password_3)
                password4 = view.findViewById(R.id.password_4)

                initPasswordClickListeners()

                submitButton = view.findViewById(R.id.submit_button)

                submitButton.setOnClickListener {
                    val context = requireContext()
                    val inputMethodManager = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.hideSoftInputFromWindow(view?.windowToken, 0)

                    if (getPassword() == prefs.settingPinNumber.toString()) {
                        showToastLong(context, resources.getString(R.string.pin_number_match))
                        prefs.lastOpenSettings = LocalDateTime.now().toString()
                        requireActivity().recreate()
                    } else {
                        showToastLong(context, resources.getString(R.string.pin_number_do_not_match))
                    }
                }

                return view
            }
        } else {
            prefs.lastOpenSettings = LocalDateTime.now().toString()
        }
        return binding.root
    }

    private fun showKeyboard(view: View) {
        val context = requireContext()
        if (!Prefs(requireContext()).autoShowKeyboard) return

        val password1 = view.findViewById<TextView>(R.id.password_1)
        password1.requestFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        password1.postDelayed({
            password1.requestFocus()
            @Suppress("DEPRECATION")
            imm.showSoftInput(password1, InputMethodManager.SHOW_FORCED)
        }, 100)
    }

    private fun initPasswordClickListeners() {
        val passwordBoxes = listOf(password1, password2, password3, password4)

        passwordBoxes.forEachIndexed { index, passwordBox ->
            passwordBox.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if (s != null && s.length == 1) {
                        handlePasswordInput(index, passwordBoxes)
                    }
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            passwordBox.setOnKeyListener { _, keyCode, _ ->
                if (keyCode == KeyEvent.KEYCODE_DEL && passwordBox.text.toString().isEmpty() && index > 0) {
                    passwordBoxes[index - 1].apply {
                        requestFocus()
                        setText("")
                    }
                }
                false
            }
        }
    }

    private fun getPassword(): String {
        return listOf(
            password1,
            password2,
            password3,
            password4
        ).joinToString("") {
            it.text.toString()
        }
    }

    private fun isLongerThanMinutes(duration: Long?, minutes: Long): Boolean {
        return duration != null && duration >= minutes
    }

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

    override fun onResume() {
        super.onResume()
        val hex = getHexForOpacity(requireContext(), prefs)
        binding.scrollView.setBackgroundColor(hex)
    }

    @Composable
    private fun Settings(fontSize: TextUnit = TextUnit.Unspecified) {
        val selected = remember { mutableStateOf("") }
        val fs = remember { mutableStateOf(fontSize) }

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
                onClick = { openAppInfo(requireContext(), android.os.Process.myUserHandle(), BuildConfig.APPLICATION_ID) },
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
                if (prefs.lockSettingsTime != 0) {
                    SettingsTextButton(
                        stringResource(R.string.reset_pin),
                        fontSize = iconFs
                    ) {
                        val popup = PopupFragment()
                        val transaction = childFragmentManager.beginTransaction()
                        transaction.add(popup, "popup")
                        transaction.commit()
                    }
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
                            values = arrayOf(System, Light, Dark),
                            onSelect = { j -> setTheme(j) }
                        )
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
                            currentSelection = remember { mutableStateOf(prefs.textSizeLauncher) },
                            min = Constants.TEXT_SIZE_MIN,
                            max = Constants.TEXT_SIZE_MAX,
                            onSelect = { f -> setTextSizeLauncher(f) }
                        )
                    },
                    { open, onChange ->
                        SettingsSliderItem(
                            title = stringResource(R.string.app_padding_size),
                            fontSize = iconFs,
                            open = open,
                            onChange = onChange,
                            currentSelection = remember { mutableStateOf(prefs.textMarginSize) },
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
                            currentSelection = remember { mutableStateOf(prefs.opacityNum) },
                            min = 0,
                            max = 255,
                            onSelect = { j -> updateOpacityNum(j) }
                        )
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
                            title = stringResource(R.string.auto_open_apps),
                            fontSize = iconFs,
                            onChange = onChange,
                            state = remember { mutableStateOf(prefs.autoOpenApp) },
                        ) { toggleAutoOpenApp() }
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
                            currentSelection = remember { mutableStateOf(prefs.homeAppsNum) },
                            min = 0,
                            max = Constants.MAX_HOME_APPS,
                            onSelect = { j -> updateHomeAppsNum(j) }
                        )
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
                        SettingsToggle(
                            title = stringResource(R.string.lock_home_apps),
                            fontSize = iconFs,
                            onChange = onChange,
                            state = remember { mutableStateOf(prefs.homeLocked) }
                        ) { prefs.homeLocked = !prefs.homeLocked }
                    },
                    { _, onChange ->
                        SettingsToggle(
                            title = stringResource(R.string.extend_home_apps_area),
                            fontSize = iconFs,
                            onChange = onChange,
                            state = remember { mutableStateOf(prefs.extendHomeAppsArea) }
                        ) { prefs.extendHomeAppsArea = !prefs.extendHomeAppsArea }
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
                    { open, onChange ->
                        SettingsItem(
                            title = stringResource(R.string.home_alignment),
                            fontSize = iconFs,
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
                            fontSize = iconFs,
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
                            fontSize = iconFs,
                            open = open,
                            onChange = onChange,
                            currentSelection = remember { mutableStateOf(prefs.drawerAlignment) },
                            values = arrayOf(Constants.Gravity.Left, Constants.Gravity.Center, Constants.Gravity.Right),
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
                            title = stringResource(R.string.swipe_up_app),
                            fontSize = iconFs,
                            open = open,
                            onChange = onChange,
                            currentAction = prefs.swipeUpAction,
                            onSelect = { j -> updateGesture(AppDrawerFlag.SetSwipeUp, j) },
                            appLabel = prefs.appSwipeUp.appLabel,
                        )
                    },
                    { open, onChange ->
                        SettingsGestureItem(
                            title = stringResource(R.string.swipe_down_app),
                            fontSize = iconFs,
                            open = open,
                            onChange = onChange,
                            currentAction = prefs.swipeDownAction,
                            onSelect = { j -> updateGesture(AppDrawerFlag.SetSwipeDown, j) },
                            appLabel = prefs.appSwipeDown.appLabel,
                        )
                    },
                    { open, onChange ->
                        SettingsGestureItem(
                            title = stringResource(R.string.swipe_left_app),
                            fontSize = iconFs,
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
                            fontSize = iconFs,
                            open = open,
                            onChange = onChange,
                            currentAction = prefs.swipeRightAction,
                            onSelect = { j -> updateGesture(AppDrawerFlag.SetSwipeRight, j) },
                            appLabel = prefs.appSwipeRight.appLabel.ifEmpty { "Phone" },
                        )
                    },
                    { open, onChange ->
                        SettingsGestureItem(
                            title = stringResource(R.string.clock_click_app),
                            fontSize = iconFs,
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
                            fontSize = iconFs,
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
                            fontSize = iconFs,
                            open = open,
                            onChange = onChange,
                            currentAction = prefs.doubleTapAction,
                            onSelect = { j -> updateGesture(AppDrawerFlag.SetDoubleTap, j) },
                            appLabel = prefs.appDoubleTap.appLabel
                        )
                    }
                )
            )
            SettingsArea(title = getString(R.string.settings),
                selected = selected,
                fontSize = titleFs,
                items = arrayOf(
                    { open, onChange ->
                        SettingsSliderItem(
                            title = stringResource(R.string.settings_text_size),
                            fontSize = iconFs,
                            open = open,
                            onChange = onChange,
                            currentSelection = remember { mutableStateOf(prefs.textSizeSettings) },
                            min = Constants.TEXT_SIZE_MIN,
                            max = Constants.TEXT_SIZE_MAX,
                            onSelect = { f -> setTextSizeSettings(f) }
                        )
                    },
                    { open, onChange ->
                        SettingsSliderItem(
                            title = stringResource(R.string.settings_lock_time),
                            fontSize = iconFs,
                            open = open,
                            onChange = onChange,
                            currentSelection = remember { mutableStateOf(prefs.lockSettingsTime) },
                            min = Constants.LOCK_TIME_MIN,
                            max = Constants.LOCK_TIME_MAX,
                            onSelect = { f -> setLockSettingsTime(f) }
                        )
                    }
                )
            )
            SettingsArea(title = getString(R.string.backup),
                selected = selected,
                fontSize = titleFs,
                items = arrayOf(
                    { _, _ ->
                        SettingsTwoButtonRow(
                            fontSize = iconFs,
                            firstButtonText = getString(R.string.load_backup),
                            secondButtonText = getString(R.string.store_backup),
                            firstButtonAction = { loadFile(requireActivity()) },
                            secondButtonAction = { storeFile(requireActivity()) },
                        )
                    }
                )
            )
            @Suppress("DEPRECATION")
            Text(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(10.dp, 5.dp),
                text = "Version: ${requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName}",
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

    private fun toggleShowBattery() {
        prefs.showBattery = !prefs.showBattery
    }

    private fun toggleCustomIconFont() {
        prefs.useCustomIconFont = !prefs.useCustomIconFont
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

    private fun updateHomeAppsNum(homeAppsNum: Int) {
        prefs.homeAppsNum = homeAppsNum
        viewModel.homeAppsCount.value = homeAppsNum
    }

    private fun updateOpacityNum(opacityNum: Int) {
        prefs.opacityNum = opacityNum
        viewModel.opacityNum.value = opacityNum
    }

    private fun toggleKeyboardText() {
        prefs.autoShowKeyboard = !prefs.autoShowKeyboard
    }

    private fun toggleAutoOpenApp() {
        prefs.autoOpenApp = !prefs.autoOpenApp
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

    private fun setLang(lang_int: Constants.Language) {

        prefs.language = lang_int
        requireActivity().recreate()
    }

    private fun setTextSizeLauncher(size: Int) {
        prefs.textSizeLauncher = size
    }

    private fun setTextSizeSettings(size: Int) {
        prefs.textSizeSettings = size
    }

    private fun setLockSettingsTime(size: Int) {
        prefs.lockSettingsTime = size
    }

    private fun setTextMarginSize(size: Int) {
        prefs.textMarginSize = size
    }

    private fun updateGesture(flag: AppDrawerFlag, action: Action) {
        when (flag) {
            AppDrawerFlag.SetSwipeLeft -> prefs.swipeLeftAction = action
            AppDrawerFlag.SetSwipeRight -> prefs.swipeRightAction = action
            AppDrawerFlag.SetSwipeUp -> prefs.swipeUpAction = action
            AppDrawerFlag.SetSwipeDown -> prefs.swipeDownAction = action
            AppDrawerFlag.SetClickClock -> prefs.clickClockAction = action
            AppDrawerFlag.SetClickDate -> prefs.clickDateAction = action
            AppDrawerFlag.SetDoubleTap -> prefs.doubleTapAction = action
            AppDrawerFlag.SetHomeApp,
            AppDrawerFlag.HiddenApps,
            AppDrawerFlag.LaunchApp -> {}
        }

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


