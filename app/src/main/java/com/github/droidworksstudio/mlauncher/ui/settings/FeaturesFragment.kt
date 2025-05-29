package com.github.droidworksstudio.mlauncher.ui.settings

import android.content.Context
import android.os.Bundle
import android.os.UserManager
import android.util.Log
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.github.droidworksstudio.common.getLocalizedString
import com.github.droidworksstudio.common.isBiometricEnabled
import com.github.droidworksstudio.common.isGestureNavigationEnabled
import com.github.droidworksstudio.mlauncher.MainActivity
import com.github.droidworksstudio.mlauncher.MainViewModel
import com.github.droidworksstudio.mlauncher.R
import com.github.droidworksstudio.mlauncher.data.AppListItem
import com.github.droidworksstudio.mlauncher.data.Constants
import com.github.droidworksstudio.mlauncher.data.Constants.Theme.Dark
import com.github.droidworksstudio.mlauncher.data.Constants.Theme.Light
import com.github.droidworksstudio.mlauncher.data.Constants.Theme.System
import com.github.droidworksstudio.mlauncher.data.Prefs
import com.github.droidworksstudio.mlauncher.databinding.FragmentSettingsBinding
import com.github.droidworksstudio.mlauncher.helper.emptyString
import com.github.droidworksstudio.mlauncher.helper.getTrueSystemFont
import com.github.droidworksstudio.mlauncher.helper.isSystemInDarkMode
import com.github.droidworksstudio.mlauncher.helper.setThemeMode
import com.github.droidworksstudio.mlauncher.helper.setTopPadding
import com.github.droidworksstudio.mlauncher.helper.utils.AppReloader
import com.github.droidworksstudio.mlauncher.style.SettingsTheme
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.PageHeader
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.SettingsSelect
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.SettingsSwitch
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.SettingsTitle
import com.github.droidworksstudio.mlauncher.ui.dialogs.DialogManager


class FeaturesFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
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
        var selectedTheme by remember { mutableStateOf(prefs.appTheme) }
        var selectedLanguage by remember { mutableStateOf(prefs.appLanguage) }
        var selectedFontFamily by remember { mutableStateOf(prefs.fontFamily) }
        var selectedSettingsSize by remember { mutableIntStateOf(prefs.settingsSize) }
        var toggledHideSearchView by remember { mutableStateOf(prefs.hideSearchView) }
        var toggledFloating by remember { mutableStateOf(prefs.showFloating) }
        var toggledLockOrientation by remember { mutableStateOf(prefs.lockOrientation) }

        var selectedSearchEngine by remember { mutableStateOf(prefs.searchEngines) }
        var toggledShowAZSidebar by remember { mutableStateOf(prefs.showAZSidebar) }
        var toggledAutoShowKeyboard by remember { mutableStateOf(prefs.autoShowKeyboard) }
        var toggledSearchFromStart by remember { mutableStateOf(prefs.searchFromStart) }
        var toggledEnableFilterStrength by remember { mutableStateOf(prefs.enableFilterStrength) }
        var selectedFilterStrength by remember { mutableIntStateOf(prefs.filterStrength) }

        var toggledAutoOpenApp by remember { mutableStateOf(prefs.autoOpenApp) }
        var toggledAppsLocked by remember { mutableStateOf(prefs.homeLocked) }
        var toggledSettingsLocked by remember { mutableStateOf(prefs.settingsLocked) }
        var selectedHomeAppsNum by remember { mutableIntStateOf(prefs.homeAppsNum) }
        var selectedHomePagesNum by remember { mutableIntStateOf(prefs.homePagesNum) }
        var toggledHomePager by remember { mutableStateOf(prefs.homePager) }

        var toggledShowDate by remember { mutableStateOf(prefs.showDate) }
        var toggledShowClock by remember { mutableStateOf(prefs.showClock) }
        var toggledShowClockFormat by remember { mutableStateOf(prefs.showClockFormat) }
        var toggledShowAlarm by remember { mutableStateOf(prefs.showAlarm) }
        var toggledShowDailyWord by remember { mutableStateOf(prefs.showDailyWord) }
        var toggledShowBattery by remember { mutableStateOf(prefs.showBattery) }
        var toggledShowBatteryIcon by remember { mutableStateOf(prefs.showBatteryIcon) }
        var toggledShowWeather by remember { mutableStateOf(prefs.showWeather) }

        val fs = remember { mutableStateOf(fontSize) }

        val titleFontSize = if (fs.value.isSpecified) {
            (fs.value.value * 1.5).sp
        } else fs.value

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            PageHeader(
                iconRes = R.drawable.ic_back,
                title = getLocalizedString(R.string.features_settings_title),
                onClick = {
                    goBackToLastFragment()
                }
            )

            Spacer(
                modifier = Modifier
                    .height(16.dp)
            )

            SettingsTitle(
                text = getLocalizedString(R.string.user_preferences),
                fontSize = titleFontSize,
            )

            SettingsSelect(
                title = getLocalizedString(R.string.theme_mode),
                option = selectedTheme.string(),
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showSingleChoiceDialog(
                        context = requireContext(),
                        options = Constants.Theme.entries.toTypedArray(),
                        titleResId = R.string.theme_mode,
                        onItemSelected = { newTheme ->
                            selectedTheme = newTheme // Update state
                            prefs.appTheme = newTheme // Persist selection in preferences

                            val isDark = when (prefs.appTheme) {
                                Light -> false
                                Dark -> true
                                System -> isSystemInDarkMode(requireContext())
                            }
                            setThemeMode(requireContext(), isDark, binding.settingsView)
                            requireActivity().recreate()
                        }
                    )
                }
            )

            SettingsSelect(
                title = getLocalizedString(R.string.app_language),
                option = selectedLanguage.string(),
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showSingleChoiceDialog(
                        context = requireContext(),
                        options = Constants.Language.entries.toTypedArray(),
                        titleResId = R.string.app_language,
                        onItemSelected = { newLanguage ->
                            selectedLanguage = newLanguage // Update state
                            prefs.appLanguage = newLanguage // Persist selection in preferences
                            requireActivity().recreate()
                        }
                    )
                }
            )

            SettingsSelect(
                title = getLocalizedString(R.string.font_family),
                option = selectedFontFamily.string(),
                fontSize = titleFontSize,
                onClick = {
                    // Generate options and fonts
                    val fontFamilyEntries = Constants.FontFamily.entries

                    val fontFamilyOptions = fontFamilyEntries.map { it.getString() }
                    val fontFamilyFonts = fontFamilyEntries.map {
                        it.getFont(requireContext()) ?: getTrueSystemFont()
                    }

                    dialogBuilder.showSingleChoiceDialog(
                        context = requireContext(),
                        options = fontFamilyOptions.toTypedArray(),
                        fonts = fontFamilyFonts,
                        titleResId = R.string.font_family,
                        onItemSelected = { newFontFamilyName ->
                            val newFontFamilyIndex =
                                fontFamilyOptions.indexOfFirst { it == newFontFamilyName }
                            if (newFontFamilyIndex != -1) {
                                val newFontFamily = fontFamilyEntries[newFontFamilyIndex]
                                if (newFontFamily == Constants.FontFamily.Custom) {
                                    // Show file picker and handle upload
                                    launchFontPicker()
                                } else {
                                    selectedFontFamily = newFontFamily
                                    prefs.fontFamily = newFontFamily
                                    AppReloader.restartApp(requireContext())
                                }
                            }
                        }
                    )
                }

            )


            SettingsSelect(
                title = getLocalizedString(R.string.settings_text_size),
                option = selectedSettingsSize.toString(),
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showSliderDialog(
                        context = requireContext(),
                        title = getLocalizedString(R.string.settings_text_size),
                        minValue = Constants.MIN_TEXT_SIZE,
                        maxValue = Constants.MAX_TEXT_SIZE,
                        currentValue = prefs.settingsSize,
                        onValueSelected = { newSettingsSize ->
                            selectedSettingsSize = newSettingsSize // Update state
                            prefs.settingsSize = newSettingsSize // Persist selection in preferences
                        }
                    )
                }
            )

            SettingsSwitch(
                text = getLocalizedString(R.string.hide_search_view),
                fontSize = titleFontSize,
                defaultState = toggledHideSearchView,
                onCheckedChange = {
                    toggledHideSearchView = !prefs.hideSearchView
                    prefs.hideSearchView = toggledHideSearchView

                    if (prefs.hideSearchView) {
                        toggledAutoShowKeyboard = false
                        prefs.autoShowKeyboard = toggledAutoShowKeyboard
                    }
                }
            )

            SettingsSwitch(
                text = getLocalizedString(R.string.show_floating_button),
                fontSize = titleFontSize,
                defaultState = toggledFloating,
                onCheckedChange = {
                    toggledFloating = !prefs.showFloating
                    prefs.showFloating = toggledFloating
                }
            )

            SettingsSwitch(
                text = getLocalizedString(R.string.lock_orientation),
                fontSize = titleFontSize,
                defaultState = toggledLockOrientation,
                onCheckedChange = {
                    toggledLockOrientation = !prefs.lockOrientation
                    prefs.lockOrientation = toggledLockOrientation
                    AppReloader.restartApp(requireContext())
                }
            )

            SettingsTitle(
                text = getLocalizedString(R.string.search),
                fontSize = titleFontSize,
            )

            SettingsSelect(
                title = getLocalizedString(R.string.search_engine),
                option = selectedSearchEngine.string(),
                fontSize = titleFontSize,
                onClick = {
                    val searchEnginesEntries = Constants.SearchEngines.entries

                    val searchEnginesOptions = searchEnginesEntries.map {
                        it.getString()
                    }

                    dialogBuilder.showSingleChoiceDialog(
                        context = requireContext(),
                        options = searchEnginesOptions.map { it }.toTypedArray(),
                        titleResId = R.string.search_engine,
                        onItemSelected = { newSearchEngineName ->
                            val newFontFamilyIndex =
                                searchEnginesOptions.indexOfFirst { it == newSearchEngineName }
                            if (newFontFamilyIndex != -1) {
                                val newSearchEngine =
                                    searchEnginesEntries[newFontFamilyIndex] // Get the selected FontFamily enum
                                selectedSearchEngine = newSearchEngine // Update state
                                prefs.searchEngines =
                                    newSearchEngine // Persist selection in preferences
                            }
                        }
                    )
                }
            )

            if (!toggledHideSearchView) {
                SettingsSwitch(
                    text = getLocalizedString(R.string.auto_show_keyboard),
                    fontSize = titleFontSize,
                    defaultState = toggledAutoShowKeyboard,
                    onCheckedChange = {
                        toggledAutoShowKeyboard = !prefs.autoShowKeyboard
                        prefs.autoShowKeyboard = toggledAutoShowKeyboard
                    }
                )
            }

            SettingsSwitch(
                text = getLocalizedString(R.string.show_az_sidebar),
                fontSize = titleFontSize,
                defaultState = toggledShowAZSidebar,
                onCheckedChange = {
                    toggledShowAZSidebar = !prefs.showAZSidebar
                    prefs.showAZSidebar = toggledShowAZSidebar
                }
            )

            SettingsSwitch(
                text = getLocalizedString(R.string.search_from_start),
                fontSize = titleFontSize,
                defaultState = toggledSearchFromStart,
                onCheckedChange = {
                    toggledSearchFromStart = !prefs.searchFromStart
                    prefs.searchFromStart = toggledSearchFromStart
                }
            )

            SettingsSwitch(
                text = getLocalizedString(R.string.enable_filter_strength),
                fontSize = titleFontSize,
                defaultState = toggledEnableFilterStrength,
                onCheckedChange = {
                    toggledEnableFilterStrength = !prefs.enableFilterStrength
                    prefs.enableFilterStrength = toggledEnableFilterStrength
                }
            )

            if (toggledEnableFilterStrength) {
                SettingsSelect(
                    title = getLocalizedString(R.string.filter_strength),
                    option = selectedFilterStrength.toString(),
                    fontSize = titleFontSize,
                    onClick = {
                        dialogBuilder.showSliderDialog(
                            context = requireContext(),
                            title = getLocalizedString(R.string.filter_strength),
                            minValue = Constants.MIN_FILTER_STRENGTH,
                            maxValue = Constants.MAX_FILTER_STRENGTH,
                            currentValue = prefs.filterStrength,
                            onValueSelected = { newFilterStrength ->
                                selectedFilterStrength = newFilterStrength // Update state
                                prefs.filterStrength =
                                    newFilterStrength // Persist selection in preferences
                                viewModel.filterStrength.value = newFilterStrength
                            }
                        )
                    }
                )
            }

            SettingsTitle(
                text = getLocalizedString(R.string.home_management),
                fontSize = titleFontSize,
            )


            SettingsSwitch(
                text = getLocalizedString(R.string.auto_open_apps),
                fontSize = titleFontSize,
                defaultState = toggledAutoOpenApp,
                onCheckedChange = {
                    toggledAutoOpenApp = !prefs.autoOpenApp
                    prefs.autoOpenApp = toggledAutoOpenApp
                }
            )

            SettingsSwitch(
                text = getLocalizedString(R.string.lock_home_apps),
                fontSize = titleFontSize,
                defaultState = toggledAppsLocked,
                onCheckedChange = {
                    toggledAppsLocked = !prefs.homeLocked
                    prefs.homeLocked = toggledAppsLocked
                }
            )

            if (requireContext().isBiometricEnabled()) {
                SettingsSwitch(
                    text = getLocalizedString(R.string.lock_settings),
                    fontSize = titleFontSize,
                    defaultState = toggledSettingsLocked,

                    onCheckedChange = {
                        toggledSettingsLocked = !prefs.settingsLocked
                        prefs.settingsLocked = toggledSettingsLocked
                    }
                )
            }

            SettingsSelect(
                title = getLocalizedString(R.string.apps_on_home_screen),
                option = selectedHomeAppsNum.toString(),
                fontSize = titleFontSize,
                onClick = {
                    Constants.updateMaxAppsBasedOnPages(requireContext())
                    val oldHomeAppsNum = selectedHomeAppsNum + 1
                    dialogBuilder.showSliderDialog(
                        context = requireContext(),
                        title = getLocalizedString(R.string.apps_on_home_screen),
                        minValue = Constants.MIN_HOME_APPS,
                        maxValue = Constants.MAX_HOME_APPS,
                        currentValue = prefs.homeAppsNum,
                        onValueSelected = { newHomeAppsNum ->
                            selectedHomeAppsNum = newHomeAppsNum // Update state
                            prefs.homeAppsNum = newHomeAppsNum // Persist selection in preferences
                            viewModel.homeAppsNum.value = newHomeAppsNum

                            // Check if homeAppsNum is less than homePagesNum and update homePagesNum accordingly
                            if (newHomeAppsNum in 1..<selectedHomePagesNum) {
                                selectedHomePagesNum = newHomeAppsNum
                                prefs.homePagesNum = newHomeAppsNum // Persist the new homePagesNum
                                viewModel.homePagesNum.value = newHomeAppsNum
                            }

                            val userManager = requireContext().getSystemService(Context.USER_SERVICE) as UserManager

                            val clearApp = AppListItem(
                                "NoApp",
                                emptyString(),
                                emptyString(),
                                user = userManager.userProfiles[0], // No user associated with the "NoApp" option
                                customLabel = "NoApp",
                            )

                            for (n in newHomeAppsNum..oldHomeAppsNum) {
                                // i is outside the range between oldHomeAppsNum and newHomeAppsNum
                                // Do something with i
                                prefs.setHomeAppModel(n, clearApp)
                            }
                        }
                    )
                }
            )

            SettingsSelect(
                title = getLocalizedString(R.string.pages_on_home_screen),
                option = selectedHomePagesNum.toString(),
                fontSize = titleFontSize,
                onClick = {
                    Constants.updateMaxHomePages(requireContext())
                    dialogBuilder.showSliderDialog(
                        context = requireContext(),
                        title = getLocalizedString(R.string.pages_on_home_screen),
                        minValue = Constants.MIN_HOME_PAGES,
                        maxValue = Constants.MAX_HOME_PAGES,
                        currentValue = prefs.homePagesNum,
                        onValueSelected = { newHomePagesNum ->
                            selectedHomePagesNum = newHomePagesNum // Update state
                            prefs.homePagesNum = newHomePagesNum // Persist selection in preferences
                            viewModel.homePagesNum.value = newHomePagesNum
                        }
                    )
                }
            )

            if (prefs.homePagesNum > 1) {
                SettingsSwitch(
                    text = getLocalizedString(R.string.enable_home_pager),
                    fontSize = titleFontSize,
                    defaultState = toggledHomePager,
                    onCheckedChange = {
                        toggledHomePager = !prefs.homePager
                        prefs.homePager = toggledHomePager
                    }
                )
            }

            SettingsTitle(
                text = getLocalizedString(R.string.battery_date_time),
                fontSize = titleFontSize,
            )

            SettingsSwitch(
                text = getLocalizedString(R.string.show_date),
                fontSize = titleFontSize,
                defaultState = toggledShowDate,
                onCheckedChange = {
                    toggledShowDate = !prefs.showDate
                    prefs.showDate = toggledShowDate
                    viewModel.setShowDate(prefs.showDate)
                }
            )

            SettingsSwitch(
                text = getLocalizedString(R.string.show_clock),
                fontSize = titleFontSize,
                defaultState = toggledShowClock,
                onCheckedChange = {
                    toggledShowClock = !prefs.showClock
                    prefs.showClock = toggledShowClock
                    viewModel.setShowClock(prefs.showClock)
                }
            )

            SettingsSwitch(
                text = getLocalizedString(R.string.show_clock_format),
                fontSize = titleFontSize,
                defaultState = toggledShowClockFormat,
                onCheckedChange = {
                    toggledShowClockFormat = !prefs.showClockFormat
                    prefs.showClockFormat = toggledShowClockFormat
                }
            )

            SettingsSwitch(
                text = getLocalizedString(R.string.show_alarm),
                fontSize = titleFontSize,
                defaultState = toggledShowAlarm,
                onCheckedChange = {
                    toggledShowAlarm = !prefs.showAlarm
                    prefs.showAlarm = toggledShowAlarm
                    viewModel.setShowAlarm(prefs.showAlarm)
                }
            )

            SettingsSwitch(
                text = getLocalizedString(R.string.show_daily_word),
                fontSize = titleFontSize,
                defaultState = toggledShowDailyWord,
                onCheckedChange = {
                    toggledShowDailyWord = !prefs.showDailyWord
                    prefs.showDailyWord = toggledShowDailyWord
                    viewModel.setShowDailyWord(prefs.showDailyWord)
                }
            )

            SettingsSwitch(
                text = getLocalizedString(R.string.show_battery),
                fontSize = titleFontSize,
                defaultState = toggledShowBattery,
                onCheckedChange = {
                    toggledShowBattery = !prefs.showBattery
                    prefs.showBattery = toggledShowBattery
                }
            )

            SettingsSwitch(
                text = getLocalizedString(R.string.show_battery_icon),
                fontSize = titleFontSize,
                defaultState = toggledShowBatteryIcon,
                onCheckedChange = {
                    toggledShowBatteryIcon = !prefs.showBatteryIcon
                    prefs.showBatteryIcon = toggledShowBatteryIcon
                }
            )

            SettingsTitle(
                text = getLocalizedString(R.string.weather),
                fontSize = titleFontSize,
            )

            SettingsSwitch(
                text = getLocalizedString(R.string.show_weather),
                fontSize = titleFontSize,
                defaultState = toggledShowWeather,
                onCheckedChange = {
                    toggledShowWeather = !prefs.showWeather
                    prefs.showWeather = toggledShowWeather
                }
            )

            if (!isGestureNavigationEnabled(requireContext())) {
                Spacer(
                    modifier = Modifier
                        .height(52.dp)
                )
            }
        }
    }

    fun launchFontPicker() {
        Log.d("FontPicker", "Launching picker...")
        (activity as MainActivity).pickCustomFont()
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

    private fun goBackToLastFragment() {
        findNavController().popBackStack()
    }


    private fun dismissDialogs() {
        dialogBuilder.singleChoiceDialog?.dismiss()
        dialogBuilder.sliderDialog?.dismiss()
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