package com.github.droidworksstudio.mlauncher.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.UserManager
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.github.droidworksstudio.common.DonationDialog
import com.github.droidworksstudio.common.getLocalizedString
import com.github.droidworksstudio.common.isBiometricEnabled
import com.github.droidworksstudio.common.isGestureNavigationEnabled
import com.github.droidworksstudio.common.share.ShareUtils
import com.github.droidworksstudio.common.showInstantToast
import com.github.droidworksstudio.common.showShortToast
import com.github.droidworksstudio.mlauncher.BuildConfig
import com.github.droidworksstudio.mlauncher.MainActivity
import com.github.droidworksstudio.mlauncher.MainViewModel
import com.github.droidworksstudio.mlauncher.R
import com.github.droidworksstudio.mlauncher.data.AppListItem
import com.github.droidworksstudio.mlauncher.data.Constants
import com.github.droidworksstudio.mlauncher.data.Constants.Action
import com.github.droidworksstudio.mlauncher.data.Constants.AppDrawerFlag
import com.github.droidworksstudio.mlauncher.data.Constants.Theme.Dark
import com.github.droidworksstudio.mlauncher.data.Constants.Theme.Light
import com.github.droidworksstudio.mlauncher.data.Constants.Theme.System
import com.github.droidworksstudio.mlauncher.data.Prefs
import com.github.droidworksstudio.mlauncher.databinding.FragmentSettingsBinding
import com.github.droidworksstudio.mlauncher.helper.IconCacheTarget
import com.github.droidworksstudio.mlauncher.helper.checkWhoInstalled
import com.github.droidworksstudio.mlauncher.helper.communitySupportButton
import com.github.droidworksstudio.mlauncher.helper.emptyString
import com.github.droidworksstudio.mlauncher.helper.getTrueSystemFont
import com.github.droidworksstudio.mlauncher.helper.helpFeedbackButton
import com.github.droidworksstudio.mlauncher.helper.hideStatusBar
import com.github.droidworksstudio.mlauncher.helper.isSystemInDarkMode
import com.github.droidworksstudio.mlauncher.helper.ismlauncherDefault
import com.github.droidworksstudio.mlauncher.helper.openAppInfo
import com.github.droidworksstudio.mlauncher.helper.setThemeMode
import com.github.droidworksstudio.mlauncher.helper.setTopPadding
import com.github.droidworksstudio.mlauncher.helper.showStatusBar
import com.github.droidworksstudio.mlauncher.helper.utils.AppReloader
import com.github.droidworksstudio.mlauncher.helper.utils.PrivateSpaceManager
import com.github.droidworksstudio.mlauncher.style.SettingsTheme
import com.github.droidworksstudio.mlauncher.ui.components.DialogManager
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.PageHeader
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.SettingsHomeItem
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.SettingsSelect
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.SettingsSwitch
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.SettingsTitle
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.TopMainHeader
import com.github.droidworksstudio.mlauncher.ui.iconpack.CustomIconSelectionActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsFragment : Fragment() {

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
        shareUtils = ShareUtils(requireContext(), requireActivity())
        prefs = Prefs(requireContext())
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (prefs.firstSettingsOpen) {
            prefs.firstSettingsOpen = false
        }

        viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        viewModel.ismlauncherDefault()

        resetThemeColors()

        setTopPadding(binding.settingsView)
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

    @Composable
    private fun Settings(fontSize: TextUnit = TextUnit.Unspecified) {
        var toggledPrivateSpaces by remember { mutableStateOf(PrivateSpaceManager(requireContext()).isPrivateSpaceLocked()) }
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

        val context = LocalContext.current
        val scrollState = rememberScrollState()

        var currentScreen by remember { mutableStateOf("main") }

        // Features Settings
        var selectedTheme by remember { mutableStateOf(prefs.appTheme) }
        var selectedLanguage by remember { mutableStateOf(prefs.appLanguage) }
        var selectedFontFamily by remember { mutableStateOf(prefs.fontFamily) }
        var toggledHideSearchView by remember { mutableStateOf(prefs.hideSearchView) }
        var toggledFloating by remember { mutableStateOf(prefs.showFloating) }

        var selectedSearchEngine by remember { mutableStateOf(prefs.searchEngines) }
        var toggledShowAZSidebar by remember { mutableStateOf(prefs.showAZSidebar) }
        var toggledAutoShowKeyboard by remember { mutableStateOf(prefs.autoShowKeyboard) }
        var toggledSearchFromStart by remember { mutableStateOf(prefs.searchFromStart) }
        var toggledEnableFilterStrength by remember { mutableStateOf(prefs.enableFilterStrength) }
        var selectedFilterStrength by remember { mutableIntStateOf(prefs.filterStrength) }

        var toggledAutoOpenApp by remember { mutableStateOf(prefs.autoOpenApp) }
        var toggledAppsLocked by remember { mutableStateOf(prefs.homeLocked) }
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

        // Look & Feel Settings
        var selectedAppSize by remember { mutableIntStateOf(prefs.appSize) }
        var selectedDateSize by remember { mutableIntStateOf(prefs.dateSize) }
        var selectedClockSize by remember { mutableIntStateOf(prefs.clockSize) }
        var selectedAlarmSize by remember { mutableIntStateOf(prefs.alarmSize) }
        var selectedDailyWordSize by remember { mutableIntStateOf(prefs.dailyWordSize) }
        var selectedBatterySize by remember { mutableIntStateOf(prefs.batterySize) }

        var selectedPaddingSize by remember { mutableIntStateOf(prefs.textPaddingSize) }
        var toggledExtendHomeAppsArea by remember { mutableStateOf(prefs.extendHomeAppsArea) }
        var toggledHomeAlignmentBottom by remember { mutableStateOf(prefs.homeAlignmentBottom) }

        var toggledShowStatusBar by remember { mutableStateOf(prefs.showStatusBar) }
        var toggledRecentAppsDisplayed by remember { mutableStateOf(prefs.recentAppsDisplayed) }
        var selectedRecentCounter by remember { mutableIntStateOf(prefs.recentCounter) }
        var toggledRecentAppUsageStats by remember { mutableStateOf(prefs.appUsageStats) }
        var selectedIconPackHome by remember { mutableStateOf(prefs.iconPackHome) }
        var selectedIconPackAppList by remember { mutableStateOf(prefs.iconPackAppList) }
        var toggledShowBackground by remember { mutableStateOf(prefs.showBackground) }
        var selectedBackgroundOpacity by remember { mutableIntStateOf(prefs.opacityNum) }

        var selectedHomeAlignment by remember { mutableStateOf(prefs.homeAlignment) }
        var selectedClockAlignment by remember { mutableStateOf(prefs.clockAlignment) }
        var selectedDateAlignment by remember { mutableStateOf(prefs.dateAlignment) }
        var selectedAlarmAlignment by remember { mutableStateOf(prefs.alarmAlignment) }
        var selectedDailyWordAlignment by remember { mutableStateOf(prefs.dailyWordAlignment) }
        var selectedDrawAlignment by remember { mutableStateOf(prefs.drawerAlignment) }

        var selectedBackgroundColor by remember { mutableIntStateOf(prefs.backgroundColor) }
        var selectedAppColor by remember { mutableIntStateOf(prefs.appColor) }
        var selectedDateColor by remember { mutableIntStateOf(prefs.dateColor) }
        var selectedClockColor by remember { mutableIntStateOf(prefs.clockColor) }
        var selectedAlarmColor by remember { mutableIntStateOf(prefs.alarmClockColor) }
        var selectedDailyWordColor by remember { mutableIntStateOf(prefs.dailyWordColor) }
        var selectedBatteryColor by remember { mutableIntStateOf(prefs.batteryColor) }
        var toggledIconRainbowColors by remember { mutableStateOf(prefs.iconRainbowColors) }
        var selectedShortcutIconsColor by remember { mutableIntStateOf(prefs.shortcutIconsColor) }

        // Gestures Settings
        var selectedDoubleTapAction by remember { mutableStateOf(prefs.doubleTapAction) }
        var selectedClickClockAction by remember { mutableStateOf(prefs.clickClockAction) }
        var selectedClickDateAction by remember { mutableStateOf(prefs.clickDateAction) }
        var selectedClickAppUsageAction by remember { mutableStateOf(prefs.clickAppUsageAction) }
        var selectedClickFloatingAction by remember { mutableStateOf(prefs.clickFloatingAction) }

        var selectedShortSwipeUpAction by remember { mutableStateOf(prefs.shortSwipeUpAction) }
        var selectedShortSwipeDownAction by remember { mutableStateOf(prefs.shortSwipeDownAction) }
        var selectedShortSwipeLeftAction by remember { mutableStateOf(prefs.shortSwipeLeftAction) }
        var selectedShortSwipeRightAction by remember { mutableStateOf(prefs.shortSwipeRightAction) }

        var selectedLongSwipeUpAction by remember { mutableStateOf(prefs.longSwipeUpAction) }
        var selectedLongSwipeDownAction by remember { mutableStateOf(prefs.longSwipeDownAction) }
        var selectedLongSwipeLeftAction by remember { mutableStateOf(prefs.longSwipeLeftAction) }
        var selectedLongSwipeRightAction by remember { mutableStateOf(prefs.longSwipeRightAction) }

        val actions = Action.entries

        // Filter out 'TogglePrivateSpace' if private space is not supported
        val filteredActions =
            if (!PrivateSpaceManager(requireContext()).isPrivateSpaceSupported()) {
                actions.filter { it != Action.TogglePrivateSpace }
            } else {
                actions
            }

        // Convert enums to their string representations
        val actionStrings = filteredActions.map { it.getString() }.toTypedArray()

        // Notes Settings
        var toggledAutoExpandNotes by remember { mutableStateOf(prefs.autoExpandNotes) }
        var toggledClickToEditDelete by remember { mutableStateOf(prefs.clickToEditDelete) }

        var selectedNotesBackgroundColor by remember { mutableIntStateOf(prefs.notesBackgroundColor) }
        var selectedBubbleBackgroundColor by remember { mutableIntStateOf(prefs.bubbleBackgroundColor) }
        var selectedBubbleMessageTextColor by remember { mutableIntStateOf(prefs.bubbleMessageTextColor) }
        var selectedBubbleTimeDateColor by remember { mutableIntStateOf(prefs.bubbleTimeDateColor) }
        var selectedBubbleCategoryColor by remember { mutableIntStateOf(prefs.bubbleCategoryColor) }

        var selectedInputMessageColor by remember { mutableIntStateOf(prefs.inputMessageColor) }
        var selectedInputMessageHintColor by remember { mutableIntStateOf(prefs.inputMessageHintColor) }

        // Private Spaces Settings
        val (setPrivateSpacesIcon, setPrivateSpacesStatus) = if (toggledPrivateSpaces) {
            R.drawable.ic_lock to R.string.locked
        } else {
            R.drawable.ic_unlock to R.string.unlocked
        }

        // Advanced Settings
        val changeLauncherText = if (ismlauncherDefault(requireContext())) {
            R.string.advanced_settings_change_default_launcher
        } else {
            R.string.advanced_settings_set_as_default_launcher
        }

        // Experimental Settings
        var toggledExpertOptions by remember { mutableStateOf(prefs.enableExpertOptions) }
        var selectedSettingsSize by remember { mutableIntStateOf(prefs.settingsSize) }
        var toggledSettingsLocked by remember { mutableStateOf(prefs.settingsLocked) }
        var toggledLockOrientation by remember { mutableStateOf(prefs.lockOrientation) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            when (currentScreen) {
                "main" -> {
                    Spacer(modifier = Modifier.height(16.dp))

                    TopMainHeader(
                        iconRes = R.drawable.app_launcher,
                        title = getLocalizedString(R.string.settings_name)
                    )

                    SettingsHomeItem(
                        title = getLocalizedString(R.string.settings_features_title),
                        description = getLocalizedString(R.string.settings_features_description),
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_feature),
                        titleFontSize = titleFontSize,
                        descriptionFontSize = descriptionFontSize,
                        iconSize = iconSize,
                        onClick = { currentScreen = "features" },
                    )

                    SettingsHomeItem(
                        title = getLocalizedString(R.string.settings_look_feel_title),
                        description = getLocalizedString(R.string.settings_look_feel_description),
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_look_feel),
                        titleFontSize = titleFontSize,
                        descriptionFontSize = descriptionFontSize,
                        iconSize = iconSize,
                        onClick = { currentScreen = "look_feel" },
                    )

                    SettingsHomeItem(
                        title = getLocalizedString(R.string.settings_gestures_title),
                        description = getLocalizedString(R.string.settings_gestures_description),
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_gestures),
                        titleFontSize = titleFontSize,
                        descriptionFontSize = descriptionFontSize,
                        iconSize = iconSize,
                        onClick = { currentScreen = "gestures" },
                    )

                    SettingsHomeItem(
                        title = getLocalizedString(R.string.settings_notes_title),
                        description = getLocalizedString(R.string.settings_notes_description),
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_notes),
                        titleFontSize = titleFontSize,
                        descriptionFontSize = descriptionFontSize,
                        iconSize = iconSize,
                        onClick = { currentScreen = "notes" },
                    )

                    if (
                        PrivateSpaceManager(context).isPrivateSpaceSupported() &&
                        PrivateSpaceManager(context).isPrivateSpaceSetUp(showToast = false, launchSettings = false)
                    ) {
                        SettingsHomeItem(
                            title = getLocalizedString(R.string.private_space, getLocalizedString(setPrivateSpacesStatus)),
                            imageVector = ImageVector.vectorResource(id = setPrivateSpacesIcon),
                            titleFontSize = titleFontSize,
                            descriptionFontSize = descriptionFontSize,
                            iconSize = iconSize,
                            onClick = {
                                PrivateSpaceManager(context).togglePrivateSpaceLock(
                                    showToast = true,
                                    launchSettings = true
                                )
                                toggledPrivateSpaces = !toggledPrivateSpaces
                            }
                        )
                    }

                    SettingsHomeItem(
                        title = getLocalizedString(R.string.settings_favorite_apps_title),
                        description = getLocalizedString(R.string.settings_favorite_apps_description),
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_favorite),
                        titleFontSize = titleFontSize,
                        descriptionFontSize = descriptionFontSize,
                        iconSize = iconSize,
                        onClick = { showFavoriteApps() },
                    )

                    SettingsHomeItem(
                        title = getLocalizedString(R.string.settings_hidden_apps_title),
                        description = getLocalizedString(R.string.settings_hidden_apps_description),
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_hidden),
                        titleFontSize = titleFontSize,
                        descriptionFontSize = descriptionFontSize,
                        iconSize = iconSize,
                        onClick = { showHiddenApps() },
                    )

                    SettingsHomeItem(
                        title = getLocalizedString(R.string.settings_advanced_title),
                        description = getLocalizedString(R.string.settings_advanced_description),
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_advanced),
                        titleFontSize = titleFontSize,
                        descriptionFontSize = descriptionFontSize,
                        iconSize = iconSize,
                        onClick = { currentScreen = "advanced" },
                    )

                    if (prefs.enableExpertOptions) {
                        SettingsHomeItem(
                            title = getLocalizedString(R.string.settings_expert_title),
                            description = getLocalizedString(R.string.settings_expert_description),
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_experimental),
                            titleFontSize = titleFontSize,
                            descriptionFontSize = descriptionFontSize,
                            iconSize = iconSize,
                            onClick = { currentScreen = "expert" },
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    SettingsHomeItem(
                        title = getLocalizedString(R.string.settings_donation_title),
                        description = getLocalizedString(R.string.settings_donation_description),
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_donation),
                        titleFontSize = titleFontSize,
                        descriptionFontSize = descriptionFontSize,
                        iconSize = iconSize,
                        onClick = {
                            DonationDialog(context).show(getLocalizedString(R.string.settings_donation_dialog))
                        },
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isGestureNavigationEnabled(context)) {
                        Spacer(modifier = Modifier.height(52.dp))
                    }
                }

                "features" -> {
                    BackHandler {
                        currentScreen = "main"
                    }

                    PageHeader(
                        iconRes = R.drawable.ic_back,
                        title = getLocalizedString(R.string.features_settings_title),
                        onClick = {
                            currentScreen = "main"
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SettingsTitle(
                        text = getLocalizedString(R.string.user_preferences),
                        fontSize = titleFontSize,
                    )

                    SettingsSelect(
                        title = getLocalizedString(R.string.theme_mode),
                        option = selectedTheme.string(),
                        fontSize = titleFontSize,
                        onClick = {
                            dialogBuilder.showSingleChoiceBottomSheet(
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
                            dialogBuilder.showSingleChoiceBottomSheet(
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

                            dialogBuilder.showSingleChoiceBottomSheet(
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

                            dialogBuilder.showSingleChoiceBottomSheet(
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

                    SettingsSwitch(
                        text = getLocalizedString(R.string.show_az_sidebar),
                        fontSize = titleFontSize,
                        defaultState = toggledShowAZSidebar,
                        onCheckedChange = {
                            toggledShowAZSidebar = !prefs.showAZSidebar
                            prefs.showAZSidebar = toggledShowAZSidebar
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
                                dialogBuilder.showSliderBottomSheet(
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

                    SettingsSelect(
                        title = getLocalizedString(R.string.apps_on_home_screen),
                        option = selectedHomeAppsNum.toString(),
                        fontSize = titleFontSize,
                        onClick = {
                            Constants.updateMaxAppsBasedOnPages(requireContext())
                            val oldHomeAppsNum = selectedHomeAppsNum + 1
                            dialogBuilder.showSliderBottomSheet(
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
                            dialogBuilder.showSliderBottomSheet(
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

                    if (!isGestureNavigationEnabled(context)) {
                        Spacer(modifier = Modifier.height(52.dp))
                    }
                }

                "look_feel" -> {
                    BackHandler {
                        currentScreen = "main"
                    }

                    PageHeader(
                        iconRes = R.drawable.ic_back,
                        title = getLocalizedString(R.string.look_feel_settings_title),
                        onClick = {
                            currentScreen = "main"
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SettingsTitle(
                        text = getLocalizedString(R.string.text_size_adjustments),
                        fontSize = titleFontSize,
                    )

                    SettingsSelect(
                        title = getLocalizedString(R.string.app_text_size),
                        option = selectedAppSize.toString(),
                        fontSize = titleFontSize,
                        onClick = {
                            dialogBuilder.showSliderBottomSheet(
                                context = requireContext(),
                                title = getLocalizedString(R.string.app_text_size),
                                minValue = Constants.MIN_TEXT_SIZE,
                                maxValue = Constants.MAX_TEXT_SIZE,
                                currentValue = prefs.appSize,
                                onValueSelected = { newAppSize ->
                                    selectedAppSize = newAppSize // Update state
                                    prefs.appSize = newAppSize // Persist selection in preferences
                                }
                            )
                        }
                    )

                    SettingsSelect(
                        title = getLocalizedString(R.string.date_text_size),
                        option = selectedDateSize.toString(),
                        fontSize = titleFontSize,
                        onClick = {
                            dialogBuilder.showSliderBottomSheet(
                                context = requireContext(),
                                title = getLocalizedString(R.string.date_text_size),
                                minValue = Constants.MIN_CLOCK_DATE_SIZE,
                                maxValue = Constants.MAX_CLOCK_DATE_SIZE,
                                currentValue = prefs.dateSize,
                                onValueSelected = { newDateSize ->
                                    selectedDateSize = newDateSize // Update state
                                    prefs.dateSize = newDateSize // Persist selection in preferences
                                }
                            )
                        }
                    )

                    SettingsSelect(
                        title = getLocalizedString(R.string.clock_text_size),
                        option = selectedClockSize.toString(),
                        fontSize = titleFontSize,
                        onClick = {
                            dialogBuilder.showSliderBottomSheet(
                                context = requireContext(),
                                title = getLocalizedString(R.string.clock_text_size),
                                minValue = Constants.MIN_CLOCK_DATE_SIZE,
                                maxValue = Constants.MAX_CLOCK_DATE_SIZE,
                                currentValue = prefs.clockSize,
                                onValueSelected = { newClockSize ->
                                    selectedClockSize = newClockSize // Update state
                                    prefs.clockSize = newClockSize // Persist selection in preferences
                                }
                            )
                        }
                    )

                    SettingsSelect(
                        title = getLocalizedString(R.string.alarm_text_size),
                        option = selectedAlarmSize.toString(),
                        fontSize = titleFontSize,
                        onClick = {
                            dialogBuilder.showSliderBottomSheet(
                                context = requireContext(),
                                title = getLocalizedString(R.string.alarm_text_size),
                                minValue = Constants.MIN_ALARM_SIZE,
                                maxValue = Constants.MAX_ALARM_SIZE,
                                currentValue = prefs.alarmSize,
                                onValueSelected = { newDateSize ->
                                    selectedAlarmSize = newDateSize // Update state
                                    prefs.alarmSize = newDateSize // Persist selection in preferences
                                }
                            )
                        }
                    )

                    SettingsSelect(
                        title = getLocalizedString(R.string.daily_word_text_size),
                        option = selectedDailyWordSize.toString(),
                        fontSize = titleFontSize,
                        onClick = {
                            dialogBuilder.showSliderBottomSheet(
                                context = requireContext(),
                                title = getLocalizedString(R.string.daily_word_text_size),
                                minValue = Constants.MIN_DAILY_WORD_SIZE,
                                maxValue = Constants.MAX_DAILY_WORD_SIZE,
                                currentValue = prefs.dailyWordSize,
                                onValueSelected = { newDateSize ->
                                    selectedDailyWordSize = newDateSize // Update state
                                    prefs.dailyWordSize = newDateSize // Persist selection in preferences
                                }
                            )
                        }
                    )

                    SettingsSelect(
                        title = getLocalizedString(R.string.battery_text_size),
                        option = selectedBatterySize.toString(),
                        fontSize = titleFontSize,
                        onClick = {
                            dialogBuilder.showSliderBottomSheet(
                                context = requireContext(),
                                title = getLocalizedString(R.string.battery_text_size),
                                minValue = Constants.MIN_BATTERY_SIZE,
                                maxValue = Constants.MAX_BATTERY_SIZE,
                                currentValue = prefs.batterySize,
                                onValueSelected = { newBatterySize ->
                                    selectedBatterySize = newBatterySize // Update state
                                    prefs.batterySize = newBatterySize // Persist selection in preferences
                                }
                            )
                        }
                    )

                    SettingsTitle(
                        text = getLocalizedString(R.string.layout_positioning),
                        fontSize = titleFontSize,
                    )

                    SettingsSelect(
                        title = getLocalizedString(R.string.app_padding_size),
                        option = selectedPaddingSize.toString(),
                        fontSize = titleFontSize,
                        onClick = {
                            dialogBuilder.showSliderBottomSheet(
                                context = requireContext(),
                                title = getLocalizedString(R.string.app_padding_size),
                                minValue = Constants.MIN_TEXT_PADDING,
                                maxValue = Constants.MAX_TEXT_PADDING,
                                currentValue = prefs.textPaddingSize,
                                onValueSelected = { newPaddingSize ->
                                    selectedPaddingSize = newPaddingSize // Update state
                                    prefs.textPaddingSize =
                                        newPaddingSize // Persist selection in preferences
                                }
                            )
                        }
                    )

                    SettingsSwitch(
                        text = getLocalizedString(R.string.extend_home_apps_area),
                        fontSize = titleFontSize,
                        defaultState = toggledExtendHomeAppsArea,
                        onCheckedChange = {
                            toggledExtendHomeAppsArea = !prefs.extendHomeAppsArea
                            prefs.extendHomeAppsArea = toggledExtendHomeAppsArea
                        }
                    )

                    SettingsSwitch(
                        text = getLocalizedString(R.string.alignment_to_bottom),
                        fontSize = titleFontSize,
                        defaultState = toggledHomeAlignmentBottom,
                        onCheckedChange = {
                            toggledHomeAlignmentBottom = !prefs.homeAlignmentBottom
                            prefs.homeAlignmentBottom = toggledHomeAlignmentBottom
                            viewModel.updateHomeAppsAlignment(
                                prefs.homeAlignment,
                                prefs.homeAlignmentBottom
                            )
                        }
                    )

                    SettingsTitle(
                        text = getLocalizedString(R.string.visibility_display),
                        fontSize = titleFontSize,
                    )

                    SettingsSwitch(
                        text = getLocalizedString(R.string.show_status_bar),
                        fontSize = titleFontSize,
                        defaultState = toggledShowStatusBar,
                        onCheckedChange = {
                            toggledShowStatusBar = !prefs.showStatusBar
                            prefs.showStatusBar = toggledShowStatusBar
                            if (toggledShowStatusBar) showStatusBar(requireActivity()) else hideStatusBar(
                                requireActivity()
                            )
                        }
                    )

                    SettingsSwitch(
                        text = getLocalizedString(R.string.show_recent_apps),
                        fontSize = titleFontSize,
                        defaultState = toggledRecentAppsDisplayed,
                        onCheckedChange = {
                            toggledRecentAppsDisplayed = !prefs.recentAppsDisplayed
                            prefs.recentAppsDisplayed = toggledRecentAppsDisplayed
                        }
                    )

                    if (toggledRecentAppsDisplayed) {
                        SettingsSelect(
                            title = getLocalizedString(R.string.number_of_recents),
                            option = selectedRecentCounter.toString(),
                            fontSize = titleFontSize,
                            onClick = {
                                dialogBuilder.showSliderBottomSheet(
                                    context = requireContext(),
                                    title = getLocalizedString(R.string.number_of_recents),
                                    minValue = Constants.MIN_RECENT_COUNTER,
                                    maxValue = Constants.MAX_RECENT_COUNTER,
                                    currentValue = prefs.recentCounter,
                                    onValueSelected = { newRecentCounter ->
                                        selectedRecentCounter = newRecentCounter // Update state
                                        prefs.recentCounter =
                                            newRecentCounter // Persist selection in preferences
                                        viewModel.recentCounter.value = newRecentCounter
                                    }
                                )
                            }
                        )
                    }

                    SettingsSwitch(
                        text = getLocalizedString(R.string.show_app_usage_stats),
                        fontSize = titleFontSize,
                        defaultState = toggledRecentAppUsageStats,
                        onCheckedChange = {
                            toggledRecentAppUsageStats = !prefs.appUsageStats
                            prefs.appUsageStats = toggledRecentAppUsageStats
                        }
                    )

                    SettingsSelect(
                        title = getLocalizedString(R.string.select_home_icons),
                        option = selectedIconPackHome.getString(IconCacheTarget.HOME.name),
                        fontSize = titleFontSize,
                        onClick = {
                            // Generate options and icons
                            val iconPacksEntries = Constants.IconPacks.entries

                            val iconPacksOptions =
                                iconPacksEntries.map { it.getString(emptyString()) }

                            dialogBuilder.showSingleChoiceBottomSheet(
                                context = requireContext(),
                                options = iconPacksOptions.map { it.toString() }.toTypedArray(),
                                titleResId = R.string.select_home_icons,
                                onItemSelected = { newAppIconsName ->
                                    val newIconPacksIndex =
                                        iconPacksOptions.indexOfFirst { it.toString() == newAppIconsName }
                                    if (newIconPacksIndex != -1) {
                                        val newAppIcons =
                                            iconPacksEntries[newIconPacksIndex] // Get the selected FontFamily enum
                                        if (newAppIcons == Constants.IconPacks.Custom) {
                                            openCustomIconSelectionDialog(IconCacheTarget.HOME)
                                        } else {
                                            prefs.customIconPackHome = emptyString()
                                            selectedIconPackHome = newAppIcons // Update state
                                            prefs.iconPackHome =
                                                newAppIcons // Persist selection in preferences
                                            viewModel.iconPackHome.value = newAppIcons
                                        }
                                    }
                                }
                            )
                        }
                    )

                    SettingsSelect(
                        title = getLocalizedString(R.string.select_app_list_icons),
                        option = selectedIconPackAppList.getString(IconCacheTarget.APP_LIST.name),
                        fontSize = titleFontSize,
                        onClick = {
                            // Generate options and icons
                            val iconPacksEntries = Constants.IconPacks.entries

                            val iconPacksOptions =
                                iconPacksEntries.map { it.getString(emptyString()) }

                            dialogBuilder.showSingleChoiceBottomSheet(
                                context = requireContext(),
                                options = iconPacksOptions.map { it.toString() }.toTypedArray(),
                                titleResId = R.string.select_app_list_icons,
                                onItemSelected = { newAppIconsName ->
                                    val newIconPacksIndex =
                                        iconPacksOptions.indexOfFirst { it.toString() == newAppIconsName }
                                    if (newIconPacksIndex != -1) {
                                        val newAppIcons =
                                            iconPacksEntries[newIconPacksIndex] // Get the selected FontFamily enum
                                        if (newAppIcons == Constants.IconPacks.Custom) {
                                            openCustomIconSelectionDialog(IconCacheTarget.APP_LIST)
                                        } else {
                                            prefs.customIconPackAppList = emptyString()
                                            selectedIconPackAppList = newAppIcons // Update state
                                            prefs.iconPackAppList =
                                                newAppIcons // Persist selection in preferences
                                            viewModel.iconPackAppList.value = newAppIcons
                                        }
                                    }
                                }
                            )
                        }
                    )

                    SettingsSwitch(
                        text = getLocalizedString(R.string.show_background),
                        fontSize = titleFontSize,
                        defaultState = toggledShowBackground,
                        onCheckedChange = {
                            toggledShowBackground = !prefs.showBackground
                            prefs.showBackground = toggledShowBackground
                        }
                    )

                    if (!toggledShowBackground) {
                        SettingsSelect(
                            title = getLocalizedString(R.string.background_opacity),
                            option = selectedBackgroundOpacity.toString(),
                            fontSize = titleFontSize,
                            onClick = {
                                dialogBuilder.showSliderBottomSheet(
                                    context = requireContext(),
                                    title = getLocalizedString(R.string.background_opacity),
                                    minValue = Constants.MIN_OPACITY,
                                    maxValue = Constants.MAX_OPACITY,
                                    currentValue = prefs.opacityNum,
                                    onValueSelected = { newBackgroundOpacity ->
                                        selectedBackgroundOpacity = newBackgroundOpacity // Update state
                                        prefs.opacityNum =
                                            newBackgroundOpacity // Persist selection in preferences
                                        viewModel.opacityNum.value = newBackgroundOpacity
                                    }
                                )
                            }
                        )
                    }

                    SettingsTitle(
                        text = getLocalizedString(R.string.element_alignment),
                        fontSize = titleFontSize,
                    )

                    SettingsSelect(
                        title = getLocalizedString(R.string.clock_alignment),
                        option = selectedClockAlignment.string(),
                        fontSize = titleFontSize,
                        onClick = {
                            dialogBuilder.showSingleChoiceBottomSheet(
                                context = requireContext(),
                                options = Constants.Gravity.entries.toTypedArray(),
                                titleResId = R.string.clock_alignment,
                                onItemSelected = { newGravity ->
                                    selectedClockAlignment = newGravity // Update state
                                    prefs.clockAlignment = newGravity // Persist selection in preferences
                                    viewModel.updateClockAlignment(newGravity)
                                }
                            )
                        }
                    )

                    SettingsSelect(
                        title = getLocalizedString(R.string.date_alignment),
                        option = selectedDateAlignment.string(),
                        fontSize = titleFontSize,
                        onClick = {
                            dialogBuilder.showSingleChoiceBottomSheet(
                                context = requireContext(),
                                options = Constants.Gravity.entries.toTypedArray(),
                                titleResId = R.string.date_alignment,
                                onItemSelected = { newGravity ->
                                    selectedDateAlignment = newGravity // Update state
                                    prefs.dateAlignment = newGravity // Persist selection in preferences
                                    viewModel.updateDateAlignment(newGravity)
                                }
                            )
                        }
                    )

                    SettingsSelect(
                        title = getLocalizedString(R.string.alarm_alignment),
                        option = selectedAlarmAlignment.string(),
                        fontSize = titleFontSize,
                        onClick = {
                            dialogBuilder.showSingleChoiceBottomSheet(
                                context = requireContext(),
                                options = Constants.Gravity.entries.toTypedArray(),
                                titleResId = R.string.alarm_alignment,
                                onItemSelected = { newGravity ->
                                    selectedAlarmAlignment = newGravity // Update state
                                    prefs.alarmAlignment = newGravity // Persist selection in preferences
                                    viewModel.updateAlarmAlignment(newGravity)
                                }
                            )
                        }
                    )

                    SettingsSelect(
                        title = getLocalizedString(R.string.daily_word_alignment),
                        option = selectedDailyWordAlignment.string(),
                        fontSize = titleFontSize,
                        onClick = {
                            dialogBuilder.showSingleChoiceBottomSheet(
                                context = requireContext(),
                                options = Constants.Gravity.entries.toTypedArray(),
                                titleResId = R.string.daily_word_alignment,
                                onItemSelected = { newGravity ->
                                    selectedDailyWordAlignment = newGravity // Update state
                                    prefs.dailyWordAlignment =
                                        newGravity // Persist selection in preferences
                                    viewModel.updateDailyWordAlignment(newGravity)
                                }
                            )
                        }
                    )

                    SettingsSelect(
                        title = getLocalizedString(R.string.home_alignment),
                        option = selectedHomeAlignment.string(),
                        fontSize = titleFontSize,
                        onClick = {
                            dialogBuilder.showSingleChoiceBottomSheet(
                                context = requireContext(),
                                options = Constants.Gravity.entries.toTypedArray(),
                                titleResId = R.string.home_alignment,
                                onItemSelected = { newGravity ->
                                    selectedHomeAlignment = newGravity // Update state
                                    prefs.homeAlignment = newGravity // Persist selection in preferences
                                    viewModel.updateHomeAppsAlignment(
                                        prefs.homeAlignment,
                                        prefs.homeAlignmentBottom
                                    )
                                }
                            )
                        }
                    )

                    SettingsSelect(
                        title = getLocalizedString(R.string.drawer_alignment),
                        option = selectedDrawAlignment.string(),
                        fontSize = titleFontSize,
                        onClick = {
                            dialogBuilder.showSingleChoiceBottomSheet(
                                context = requireContext(),
                                options = Constants.Gravity.entries.toTypedArray(),
                                titleResId = R.string.drawer_alignment,
                                onItemSelected = { newGravity ->
                                    selectedDrawAlignment = newGravity // Update state
                                    prefs.drawerAlignment = newGravity // Persist selection in preferences
                                    viewModel.updateDrawerAlignment(newGravity)
                                }
                            )
                        }
                    )

                    SettingsTitle(
                        text = getLocalizedString(R.string.element_colors),
                        fontSize = titleFontSize,
                    )

                    val hexBackgroundColor = String.format("#%06X", (0xFFFFFF and selectedBackgroundColor))
                    SettingsSelect(
                        title = getLocalizedString(R.string.background_color),
                        option = hexBackgroundColor,
                        fontSize = titleFontSize,
                        fontColor = Color(hexBackgroundColor.toColorInt()),
                        onClick = {
                            dialogBuilder.showColorPickerBottomSheet(
                                context = requireContext(),
                                color = selectedBackgroundColor,
                                titleResId = R.string.background_color,
                                onItemSelected = { selectedColor ->
                                    selectedBackgroundColor = selectedColor
                                    prefs.backgroundColor = selectedColor
                                })
                        }
                    )

                    val hexAppColor = String.format("#%06X", (0xFFFFFF and selectedAppColor))
                    SettingsSelect(
                        title = getLocalizedString(R.string.app_color),
                        option = hexAppColor,
                        fontSize = titleFontSize,
                        fontColor = Color(hexAppColor.toColorInt()),
                        onClick = {
                            dialogBuilder.showColorPickerBottomSheet(
                                context = requireContext(),
                                color = selectedAppColor,
                                titleResId = R.string.app_color,
                                onItemSelected = { selectedColor ->
                                    selectedAppColor = selectedColor
                                    prefs.appColor = selectedColor
                                })
                        }
                    )

                    val hexDateColor = String.format("#%06X", (0xFFFFFF and selectedDateColor))
                    SettingsSelect(
                        title = getLocalizedString(R.string.date_color),
                        option = hexDateColor,
                        fontSize = titleFontSize,
                        fontColor = Color(hexDateColor.toColorInt()),
                        onClick = {
                            dialogBuilder.showColorPickerBottomSheet(
                                context = requireContext(),
                                color = selectedDateColor,
                                titleResId = R.string.date_color,
                                onItemSelected = { selectedColor ->
                                    selectedDateColor = selectedColor
                                    prefs.dateColor = selectedColor
                                })
                        }
                    )

                    val hexClockColor = String.format("#%06X", (0xFFFFFF and selectedClockColor))
                    SettingsSelect(
                        title = getLocalizedString(R.string.clock_color),
                        option = hexClockColor,
                        fontSize = titleFontSize,
                        fontColor = Color(hexClockColor.toColorInt()),
                        onClick = {
                            dialogBuilder.showColorPickerBottomSheet(
                                context = requireContext(),
                                color = selectedClockColor,
                                titleResId = R.string.clock_color,
                                onItemSelected = { selectedColor ->
                                    selectedClockColor = selectedColor
                                    prefs.clockColor = selectedColor
                                })
                        }
                    )

                    val hexAlarmColor = String.format("#%06X", (0xFFFFFF and selectedAlarmColor))
                    SettingsSelect(
                        title = getLocalizedString(R.string.alarm_color),
                        option = hexAlarmColor,
                        fontSize = titleFontSize,
                        fontColor = Color(hexAlarmColor.toColorInt()),
                        onClick = {
                            dialogBuilder.showColorPickerBottomSheet(
                                context = requireContext(),
                                color = selectedAlarmColor,
                                titleResId = R.string.alarm_color,
                                onItemSelected = { selectedColor ->
                                    selectedAlarmColor = selectedColor
                                    prefs.alarmClockColor = selectedColor
                                })
                        }
                    )

                    val hexDailyWordColor = String.format("#%06X", (0xFFFFFF and selectedDailyWordColor))
                    SettingsSelect(
                        title = getLocalizedString(R.string.daily_word_color),
                        option = hexDailyWordColor,
                        fontSize = titleFontSize,
                        fontColor = Color(hexDailyWordColor.toColorInt()),
                        onClick = {
                            dialogBuilder.showColorPickerBottomSheet(
                                context = requireContext(),
                                color = selectedDailyWordColor,
                                titleResId = R.string.daily_word_color,
                                onItemSelected = { selectedColor ->
                                    selectedDailyWordColor = selectedColor
                                    prefs.dailyWordColor = selectedColor
                                })
                        }
                    )

                    val hexBatteryColor = String.format("#%06X", (0xFFFFFF and selectedBatteryColor))
                    SettingsSelect(
                        title = getLocalizedString(R.string.battery_color),
                        option = hexBatteryColor,
                        fontSize = titleFontSize,
                        fontColor = Color(hexBatteryColor.toColorInt()),
                        onClick = {
                            dialogBuilder.showColorPickerBottomSheet(
                                context = requireContext(),
                                color = selectedBatteryColor,
                                titleResId = R.string.battery_color,
                                onItemSelected = { selectedColor ->
                                    selectedBatteryColor = selectedColor
                                    prefs.batteryColor = selectedColor
                                })
                        }
                    )

                    SettingsSwitch(
                        text = getLocalizedString(R.string.rainbow_shortcuts),
                        fontSize = titleFontSize,
                        defaultState = toggledIconRainbowColors,
                        onCheckedChange = {
                            toggledIconRainbowColors = !prefs.iconRainbowColors
                            prefs.iconRainbowColors = toggledIconRainbowColors
                        }
                    )

                    val hexShortcutIconsColor =
                        String.format("#%06X", (0xFFFFFF and selectedShortcutIconsColor))
                    SettingsSelect(
                        title = getLocalizedString(R.string.shortcuts_color),
                        option = hexShortcutIconsColor,
                        fontSize = titleFontSize,
                        fontColor = Color(hexShortcutIconsColor.toColorInt()),
                        onClick = {
                            dialogBuilder.showColorPickerBottomSheet(
                                context = requireContext(),
                                color = selectedShortcutIconsColor,
                                titleResId = R.string.shortcuts_color,
                                onItemSelected = { selectedColor ->
                                    selectedShortcutIconsColor = selectedColor
                                    prefs.shortcutIconsColor = selectedColor
                                })
                        }
                    )

                    if (!isGestureNavigationEnabled(context)) {
                        Spacer(modifier = Modifier.height(52.dp))
                    }
                }

                "gestures" -> {
                    BackHandler {
                        currentScreen = "main"
                    }

                    PageHeader(
                        iconRes = R.drawable.ic_back,
                        title = getLocalizedString(R.string.gestures_settings_title),
                        onClick = {
                            currentScreen = "main"
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SettingsTitle(
                        text = getLocalizedString(R.string.tap_click_actions),
                        fontSize = titleFontSize,
                    )

                    val appLabelDoubleTapAction = prefs.appDoubleTap.activityLabel
                    SettingsSelect(
                        title = getLocalizedString(R.string.double_tap),
                        option = if (selectedDoubleTapAction == Action.OpenApp) {
                            "${getLocalizedString(R.string.open)} $appLabelDoubleTapAction"
                        } else {
                            selectedDoubleTapAction.string()
                        },
                        fontSize = titleFontSize,
                        onClick = {
                            dialogBuilder.showSingleChoiceBottomSheet(
                                context = requireContext(),
                                options = actionStrings,
                                titleResId = R.string.double_tap,
                                onItemSelected = { newDoubleTapAction ->
                                    val selectedAction =
                                        actions.firstOrNull { it.getString() == newDoubleTapAction }
                                    if (selectedAction != null) {
                                        selectedDoubleTapAction = selectedAction // Store the enum itself
                                        setGesture(
                                            AppDrawerFlag.SetDoubleTap,
                                            selectedAction
                                        ) // Persist selection in preferences
                                    }
                                }
                            )
                        }
                    )

                    val appLabelClickClockAction = prefs.appClickClock.activityLabel.ifEmpty { "Clock" }
                    SettingsSelect(
                        title = getLocalizedString(R.string.clock_click_app),
                        option = if (selectedClickClockAction == Action.OpenApp) {
                            "${getLocalizedString(R.string.open)} $appLabelClickClockAction"
                        } else {
                            selectedClickClockAction.string()
                        },
                        fontSize = titleFontSize,
                        onClick = {
                            dialogBuilder.showSingleChoiceBottomSheet(
                                context = requireContext(),
                                options = actionStrings,
                                titleResId = R.string.clock_click_app,
                                onItemSelected = { newClickClock ->
                                    val selectedAction =
                                        actions.firstOrNull { it.getString() == newClickClock }
                                    if (selectedAction != null) {
                                        selectedClickClockAction = selectedAction // Store the enum itself
                                        setGesture(
                                            AppDrawerFlag.SetClickClock,
                                            selectedAction
                                        ) // Persist selection in preferences
                                    }
                                }
                            )
                        }
                    )

                    val appLabelClickDateAction = prefs.appClickDate.activityLabel.ifEmpty { "Calendar" }
                    SettingsSelect(
                        title = getLocalizedString(R.string.date_click_app),
                        option = if (selectedClickDateAction == Action.OpenApp) {
                            "${getLocalizedString(R.string.open)} $appLabelClickDateAction"
                        } else {
                            selectedClickDateAction.string()
                        },
                        fontSize = titleFontSize,
                        onClick = {
                            dialogBuilder.showSingleChoiceBottomSheet(
                                context = requireContext(),
                                options = actionStrings,
                                titleResId = R.string.date_click_app,
                                onItemSelected = { newClickDate ->
                                    val selectedAction =
                                        actions.firstOrNull { it.getString() == newClickDate }
                                    if (selectedAction != null) {
                                        selectedClickDateAction = selectedAction // Store the enum itself
                                        setGesture(
                                            AppDrawerFlag.SetClickDate,
                                            selectedAction
                                        ) // Persist selection in preferences
                                    }
                                }
                            )
                        }
                    )

                    val appLabelClickAppUsageAction =
                        prefs.appClickUsage.activityLabel.ifEmpty { "Digital Wellbeing" }
                    SettingsSelect(
                        title = getLocalizedString(R.string.usage_click_app),
                        option = if (selectedClickAppUsageAction == Action.OpenApp) {
                            "${getLocalizedString(R.string.open)} $appLabelClickAppUsageAction"
                        } else {
                            selectedClickAppUsageAction.string()
                        },
                        fontSize = titleFontSize,
                        onClick = {
                            dialogBuilder.showSingleChoiceBottomSheet(
                                context = requireContext(),
                                options = actionStrings,
                                titleResId = R.string.usage_click_app,
                                onItemSelected = { newClickAppUsage ->
                                    val selectedAction =
                                        actions.firstOrNull { it.getString() == newClickAppUsage }
                                    if (selectedAction != null) {
                                        selectedClickAppUsageAction =
                                            selectedAction // Store the enum itself
                                        setGesture(
                                            AppDrawerFlag.SetAppUsage,
                                            selectedAction
                                        ) // Persist selection in preferences
                                    }
                                }
                            )
                        }
                    )

                    val appLabelClickFloatingAction = prefs.appFloating.activityLabel.ifEmpty { "Notes" }
                    SettingsSelect(
                        title = getLocalizedString(R.string.floating_click_app),
                        option = if (selectedClickFloatingAction == Action.OpenApp) {
                            "${getLocalizedString(R.string.open)} $appLabelClickFloatingAction"
                        } else {
                            selectedClickFloatingAction.string()
                        },
                        fontSize = titleFontSize,
                        onClick = {
                            dialogBuilder.showSingleChoiceBottomSheet(
                                context = requireContext(),
                                options = actionStrings,
                                titleResId = R.string.floating_click_app,
                                onItemSelected = { newClickFloating ->
                                    val selectedAction =
                                        actions.firstOrNull { it.getString() == newClickFloating }
                                    if (selectedAction != null) {
                                        selectedClickFloatingAction =
                                            selectedAction // Store the enum itself
                                        setGesture(
                                            AppDrawerFlag.SetFloating,
                                            selectedAction
                                        ) // Persist selection in preferences
                                    }
                                }
                            )
                        }
                    )

                    SettingsTitle(
                        text = getLocalizedString(R.string.swipe_movement),
                        fontSize = titleFontSize,
                    )

                    val appLabelShortSwipeUpAction =
                        prefs.appShortSwipeUp.activityLabel.ifEmpty { "Settings" }
                    SettingsSelect(
                        title = getLocalizedString(R.string.short_swipe_up_app),
                        option = if (selectedShortSwipeUpAction == Action.OpenApp) {
                            "${getLocalizedString(R.string.open)} $appLabelShortSwipeUpAction"
                        } else {
                            selectedShortSwipeUpAction.string()
                        },
                        fontSize = titleFontSize,
                        onClick = {
                            dialogBuilder.showSingleChoiceBottomSheet(
                                context = requireContext(),
                                options = actionStrings,
                                titleResId = R.string.short_swipe_up_app,
                                onItemSelected = { newShortSwipeUpAction ->

                                    val selectedAction =
                                        actions.firstOrNull { it.getString() == newShortSwipeUpAction }
                                    if (selectedAction != null) {
                                        selectedShortSwipeUpAction = selectedAction // Store the enum itself
                                        setGesture(
                                            AppDrawerFlag.SetShortSwipeUp,
                                            selectedAction
                                        ) // Persist selection in preferences
                                    }
                                }
                            )
                        }
                    )

                    val appLabelShortSwipeDownAction =
                        prefs.appShortSwipeDown.activityLabel.ifEmpty { "Phone" }
                    SettingsSelect(
                        title = getLocalizedString(R.string.short_swipe_down_app),
                        option = if (selectedShortSwipeDownAction == Action.OpenApp) {
                            "${getLocalizedString(R.string.open)} $appLabelShortSwipeDownAction"
                        } else {
                            selectedShortSwipeDownAction.string()
                        },
                        fontSize = titleFontSize,
                        onClick = {
                            dialogBuilder.showSingleChoiceBottomSheet(
                                context = requireContext(),
                                options = actionStrings,
                                titleResId = R.string.short_swipe_down_app,
                                onItemSelected = { newShortSwipeDownAction ->
                                    val selectedAction =
                                        actions.firstOrNull { it.getString() == newShortSwipeDownAction }
                                    if (selectedAction != null) {
                                        selectedShortSwipeDownAction =
                                            selectedAction // Store the enum itself
                                        setGesture(
                                            AppDrawerFlag.SetShortSwipeDown,
                                            selectedAction
                                        ) // Persist selection in preferences
                                    }
                                }
                            )
                        }
                    )

                    val appLabelShortSwipeLeftAction =
                        prefs.appShortSwipeLeft.activityLabel.ifEmpty { "Settings" }
                    SettingsSelect(
                        title = getLocalizedString(R.string.short_swipe_left_app),
                        option = if (selectedShortSwipeLeftAction == Action.OpenApp) {
                            "${getLocalizedString(R.string.open)} $appLabelShortSwipeLeftAction"
                        } else {
                            selectedShortSwipeLeftAction.string()
                        },
                        fontSize = titleFontSize,
                        onClick = {
                            dialogBuilder.showSingleChoiceBottomSheet(
                                context = requireContext(),
                                options = actionStrings,
                                titleResId = R.string.short_swipe_left_app,
                                onItemSelected = { newShortSwipeLeftAction ->
                                    val selectedAction =
                                        actions.firstOrNull { it.getString() == newShortSwipeLeftAction }
                                    if (selectedAction != null) {
                                        selectedShortSwipeLeftAction =
                                            selectedAction // Store the enum itself
                                        setGesture(
                                            AppDrawerFlag.SetShortSwipeLeft,
                                            selectedAction
                                        ) // Persist selection in preferences
                                    }
                                }
                            )
                        }
                    )

                    val appLabelShortSwipeRightAction =
                        prefs.appShortSwipeRight.activityLabel.ifEmpty { "Phone" }
                    SettingsSelect(
                        title = getLocalizedString(R.string.short_swipe_right_app),
                        option = if (selectedShortSwipeRightAction == Action.OpenApp) {
                            "${getLocalizedString(R.string.open)} $appLabelShortSwipeRightAction"
                        } else {
                            selectedShortSwipeRightAction.string()
                        },
                        fontSize = titleFontSize,
                        onClick = {
                            dialogBuilder.showSingleChoiceBottomSheet(
                                context = requireContext(),
                                options = actionStrings,
                                titleResId = R.string.short_swipe_right_app,
                                onItemSelected = { newShortSwipeRightAction ->
                                    val selectedAction =
                                        actions.firstOrNull { it.getString() == newShortSwipeRightAction }
                                    if (selectedAction != null) {
                                        selectedShortSwipeRightAction =
                                            selectedAction // Store the enum itself
                                        setGesture(
                                            AppDrawerFlag.SetShortSwipeRight,
                                            selectedAction
                                        ) // Persist selection in preferences
                                    }
                                }
                            )
                        }
                    )

                    val appLabelLongSwipeUpAction =
                        prefs.appLongSwipeUp.activityLabel.ifEmpty { "Settings" }
                    SettingsSelect(
                        title = getLocalizedString(R.string.long_swipe_up_app),
                        option = if (selectedLongSwipeUpAction == Action.OpenApp) {
                            "${getLocalizedString(R.string.open)} $appLabelLongSwipeUpAction"
                        } else {
                            selectedLongSwipeUpAction.string()
                        },
                        fontSize = titleFontSize,
                        onClick = {
                            dialogBuilder.showSingleChoiceBottomSheet(
                                context = requireContext(),
                                options = actionStrings,
                                titleResId = R.string.long_swipe_up_app,
                                onItemSelected = { newLongSwipeUpAction ->
                                    val selectedAction =
                                        actions.firstOrNull { it.getString() == newLongSwipeUpAction }
                                    if (selectedAction != null) {
                                        selectedLongSwipeUpAction = selectedAction // Store the enum itself
                                        setGesture(
                                            AppDrawerFlag.SetLongSwipeUp,
                                            selectedAction
                                        ) // Persist selection in preferences
                                    }
                                }
                            )
                        }
                    )

                    val appLabelLongSwipeDownAction =
                        prefs.appLongSwipeDown.activityLabel.ifEmpty { "Phone" }
                    SettingsSelect(
                        title = getLocalizedString(R.string.long_swipe_down_app),
                        option = if (selectedLongSwipeDownAction == Action.OpenApp) {
                            "${getLocalizedString(R.string.open)} $appLabelLongSwipeDownAction"
                        } else {
                            selectedLongSwipeDownAction.string()
                        },
                        fontSize = titleFontSize,
                        onClick = {
                            dialogBuilder.showSingleChoiceBottomSheet(
                                context = requireContext(),
                                options = actionStrings,
                                titleResId = R.string.long_swipe_down_app,
                                onItemSelected = { newLongSwipeDownAction ->
                                    val selectedAction =
                                        actions.firstOrNull { it.getString() == newLongSwipeDownAction }
                                    if (selectedAction != null) {
                                        selectedLongSwipeDownAction =
                                            selectedAction // Store the enum itself
                                        setGesture(
                                            AppDrawerFlag.SetLongSwipeDown,
                                            selectedAction
                                        ) // Persist selection in preferences
                                    }
                                }
                            )
                        }
                    )

                    val appLabelLongSwipeLeftAction =
                        prefs.appLongSwipeLeft.activityLabel.ifEmpty { "Settings" }
                    SettingsSelect(
                        title = getLocalizedString(R.string.long_swipe_left_app),
                        option = if (selectedLongSwipeLeftAction == Action.OpenApp) {
                            "${getLocalizedString(R.string.open)} $appLabelLongSwipeLeftAction"
                        } else {
                            selectedLongSwipeLeftAction.string()
                        },
                        fontSize = titleFontSize,
                        onClick = {
                            dialogBuilder.showSingleChoiceBottomSheet(
                                context = requireContext(),
                                options = actionStrings, // Pass the list of localized strings
                                titleResId = R.string.long_swipe_left_app,
                                onItemSelected = { newLongSwipeLeftAction ->
                                    val selectedAction =
                                        actions.firstOrNull { it.getString() == newLongSwipeLeftAction }
                                    if (selectedAction != null) {
                                        selectedLongSwipeLeftAction =
                                            selectedAction // Store the enum itself
                                        setGesture(
                                            AppDrawerFlag.SetLongSwipeLeft,
                                            selectedAction
                                        ) // Persist selection in preferences
                                    }
                                }
                            )
                        }
                    )

                    val appLabelLongSwipeRightAction =
                        prefs.appLongSwipeRight.activityLabel.ifEmpty { "Phone" }
                    SettingsSelect(
                        title = getLocalizedString(R.string.long_swipe_right_app),
                        option = if (selectedLongSwipeRightAction == Action.OpenApp) {
                            "${getLocalizedString(R.string.open)} $appLabelLongSwipeRightAction"
                        } else {
                            selectedLongSwipeRightAction.string()
                        },
                        fontSize = titleFontSize,
                        onClick = {
                            dialogBuilder.showSingleChoiceBottomSheet(
                                context = requireContext(),
                                options = actionStrings,
                                titleResId = R.string.long_swipe_right_app,
                                onItemSelected = { newLongSwipeRightAction ->
                                    val selectedAction =
                                        actions.firstOrNull { it.getString() == newLongSwipeRightAction }
                                    if (selectedAction != null) {
                                        selectedLongSwipeRightAction =
                                            selectedAction // Store the enum itself
                                        setGesture(
                                            AppDrawerFlag.SetLongSwipeRight,
                                            selectedAction
                                        ) // Persist selection in preferences
                                    }
                                }
                            )
                        }
                    )

                    if (!isGestureNavigationEnabled(context)) {
                        Spacer(modifier = Modifier.height(52.dp))
                    }
                }

                "notes" -> {
                    BackHandler {
                        currentScreen = "main"
                    }

                    PageHeader(
                        iconRes = R.drawable.ic_back,
                        title = getLocalizedString(R.string.notes_settings_title),
                        onClick = {
                            currentScreen = "main"
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SettingsTitle(
                        text = getLocalizedString(R.string.display_options),
                        fontSize = titleFontSize,
                    )

                    SettingsSwitch(
                        text = getLocalizedString(R.string.auto_expand_notes),
                        fontSize = titleFontSize,
                        defaultState = toggledAutoExpandNotes,
                        onCheckedChange = {
                            toggledAutoExpandNotes = !prefs.autoExpandNotes
                            prefs.autoExpandNotes = toggledAutoExpandNotes
                        }
                    )

                    SettingsSwitch(
                        text = getLocalizedString(R.string.click_to_edit_delete),
                        fontSize = titleFontSize,
                        defaultState = toggledClickToEditDelete,
                        onCheckedChange = {
                            toggledClickToEditDelete = !prefs.clickToEditDelete
                            prefs.clickToEditDelete = toggledClickToEditDelete
                        }
                    )

                    SettingsTitle(
                        text = getLocalizedString(R.string.notes_colors),
                        fontSize = titleFontSize,
                    )

                    val hexBackgroundColor =
                        String.format("#%06X", (0xFFFFFF and selectedNotesBackgroundColor))
                    SettingsSelect(
                        title = getLocalizedString(R.string.notes_background_color),
                        option = hexBackgroundColor,
                        fontSize = titleFontSize,
                        fontColor = Color(hexBackgroundColor.toColorInt()),
                        onClick = {
                            dialogBuilder.showColorPickerBottomSheet(
                                context = requireContext(),
                                color = selectedNotesBackgroundColor,
                                titleResId = R.string.notes_background_color,
                                onItemSelected = { selectedColor ->
                                    selectedNotesBackgroundColor = selectedColor
                                    prefs.notesBackgroundColor = selectedColor
                                })
                        }
                    )

                    val hexBubbleBackgroundColor =
                        String.format("#%06X", (0xFFFFFF and selectedBubbleBackgroundColor))
                    SettingsSelect(
                        title = getLocalizedString(R.string.bubble_background_color),
                        option = hexBubbleBackgroundColor,
                        fontSize = titleFontSize,
                        fontColor = Color(hexBubbleBackgroundColor.toColorInt()),
                        onClick = {
                            dialogBuilder.showColorPickerBottomSheet(
                                context = requireContext(),
                                color = selectedBubbleBackgroundColor,
                                titleResId = R.string.bubble_background_color,
                                onItemSelected = { selectedColor ->
                                    selectedBubbleBackgroundColor = selectedColor
                                    prefs.bubbleBackgroundColor = selectedColor
                                })
                        }
                    )

                    val hexMessageTextColor =
                        String.format("#%06X", (0xFFFFFF and selectedBubbleMessageTextColor))
                    SettingsSelect(
                        title = getLocalizedString(R.string.bubble_message_color),
                        option = hexMessageTextColor,
                        fontSize = titleFontSize,
                        fontColor = Color(hexMessageTextColor.toColorInt()),
                        onClick = {
                            dialogBuilder.showColorPickerBottomSheet(
                                context = requireContext(),
                                color = selectedBubbleMessageTextColor,
                                titleResId = R.string.bubble_message_color,
                                onItemSelected = { selectedColor ->
                                    selectedBubbleMessageTextColor = selectedColor
                                    prefs.bubbleMessageTextColor = selectedColor
                                })
                        }
                    )

                    val hexBubbleTimeDateColor =
                        String.format("#%06X", (0xFFFFFF and selectedBubbleTimeDateColor))
                    SettingsSelect(
                        title = getLocalizedString(R.string.bubble_date_time_color),
                        option = hexBubbleTimeDateColor,
                        fontSize = titleFontSize,
                        fontColor = Color(hexBubbleTimeDateColor.toColorInt()),
                        onClick = {
                            dialogBuilder.showColorPickerBottomSheet(
                                context = requireContext(),
                                color = selectedBubbleTimeDateColor,
                                titleResId = R.string.bubble_date_time_color,
                                onItemSelected = { selectedColor ->
                                    selectedBubbleTimeDateColor = selectedColor
                                    prefs.bubbleTimeDateColor = selectedColor
                                })
                        }
                    )

                    val hexBubbleCategoryColor =
                        String.format("#%06X", (0xFFFFFF and selectedBubbleCategoryColor))
                    SettingsSelect(
                        title = getLocalizedString(R.string.bubble_category_color),
                        option = hexBubbleCategoryColor,
                        fontSize = titleFontSize,
                        fontColor = Color(hexBubbleCategoryColor.toColorInt()),
                        onClick = {
                            dialogBuilder.showColorPickerBottomSheet(
                                context = requireContext(),
                                color = selectedBubbleCategoryColor,
                                titleResId = R.string.bubble_category_color,
                                onItemSelected = { selectedColor ->
                                    selectedBubbleCategoryColor = selectedColor
                                    prefs.bubbleCategoryColor = selectedColor
                                })
                        }
                    )

                    SettingsTitle(
                        text = getLocalizedString(R.string.input_colors),
                        fontSize = titleFontSize,
                    )

                    val hexBubbleInputMessageColor =
                        String.format("#%06X", (0xFFFFFF and selectedInputMessageColor))
                    SettingsSelect(
                        title = getLocalizedString(R.string.message_input_color),
                        option = hexBubbleInputMessageColor,
                        fontSize = titleFontSize,
                        fontColor = Color(hexBubbleInputMessageColor.toColorInt()),
                        onClick = {
                            dialogBuilder.showColorPickerBottomSheet(
                                context = requireContext(),
                                color = selectedInputMessageColor,
                                titleResId = R.string.message_input_color,
                                onItemSelected = { selectedColor ->
                                    selectedInputMessageColor = selectedColor
                                    prefs.inputMessageColor = selectedColor
                                })
                        }
                    )

                    val hexBubbleInputMessageHintColor =
                        String.format("#%06X", (0xFFFFFF and selectedInputMessageHintColor))
                    SettingsSelect(
                        title = getLocalizedString(R.string.message_input_hint_color),
                        option = hexBubbleInputMessageHintColor,
                        fontSize = titleFontSize,
                        fontColor = Color(hexBubbleInputMessageHintColor.toColorInt()),
                        onClick = {
                            dialogBuilder.showColorPickerBottomSheet(
                                context = requireContext(),
                                color = selectedInputMessageHintColor,
                                titleResId = R.string.message_input_hint_color,
                                onItemSelected = { selectedColor ->
                                    selectedInputMessageHintColor = selectedColor
                                    prefs.inputMessageHintColor = selectedColor
                                })
                        }
                    )

                    if (!isGestureNavigationEnabled(context)) {
                        Spacer(modifier = Modifier.height(52.dp))
                    }
                }

                "advanced" -> {
                    BackHandler {
                        currentScreen = "main"
                    }

                    PageHeader(
                        iconRes = R.drawable.ic_back,
                        title = getLocalizedString(R.string.advanced_settings_title),
                        onClick = {
                            currentScreen = "main"
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

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
                        enableMultiClick = true,
                        onMultiClick = { count ->
                            if (!prefs.enableExpertOptions) {
                                if (count in 2..4) {
                                    showInstantToast(getLocalizedString(R.string.expert_options_tap_hint, count))
                                } else if (count == 5) {
                                    showInstantToast(getLocalizedString(R.string.expert_options_unlocked))
                                    toggledExpertOptions = !prefs.enableExpertOptions
                                    prefs.enableExpertOptions = toggledExpertOptions
                                }
                            }
                        }
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
                            dialogBuilder.showBackupRestoreBottomSheet()
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
                            dialogBuilder.showSaveLoadThemeBottomSheet()
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
                            dialogBuilder.showSaveDownloadWOTDBottomSheet()
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

                    if (!isGestureNavigationEnabled(context)) {
                        Spacer(modifier = Modifier.height(52.dp))
                    }
                }

                "expert" -> {
                    BackHandler {
                        currentScreen = "main"
                    }

                    PageHeader(
                        iconRes = R.drawable.ic_back,
                        title = getLocalizedString(R.string.expert_settings_title),
                        onClick = {
                            currentScreen = "main"
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SettingsSwitch(
                        text = getLocalizedString(R.string.expert_options_display),
                        fontSize = titleFontSize,
                        defaultState = toggledExpertOptions,
                        onCheckedChange = {
                            toggledExpertOptions = !prefs.enableExpertOptions
                            prefs.enableExpertOptions = toggledExpertOptions
                            currentScreen = "main"
                        }
                    )

                    SettingsTitle(
                        text = getLocalizedString(R.string.user_preferences),
                        fontSize = titleFontSize,
                    )

                    SettingsSelect(
                        title = getLocalizedString(R.string.settings_text_size),
                        option = selectedSettingsSize.toString(),
                        fontSize = titleFontSize,
                        onClick = {
                            dialogBuilder.showSliderBottomSheet(
                                context = context,
                                title = getLocalizedString(R.string.settings_text_size),
                                minValue = Constants.MIN_TEXT_SIZE,
                                maxValue = Constants.MAX_TEXT_SIZE,
                                currentValue = prefs.settingsSize,
                                onValueSelected = { newSettingsSize ->
                                    selectedSettingsSize = newSettingsSize
                                    prefs.settingsSize = newSettingsSize
                                }
                            )
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

                    if (!isGestureNavigationEnabled(context)) {
                        Spacer(modifier = Modifier.height(52.dp))
                    }
                }
            }
        }
    }

    @Composable
    fun tuToDp(textUnit: TextUnit): Dp {
        val density = LocalDensity.current.density
        val scaledDensity = LocalDensity.current.fontScale
        val dpValue = textUnit.value * (density / scaledDensity)
        return dpValue.dp  // Convert to Dp using the 'dp' extension
    }

    private fun dismissDialogs() {
        dialogBuilder.backupRestoreBottomSheet?.dismiss()
        dialogBuilder.saveLoadThemeBottomSheet?.dismiss()
        dialogBuilder.saveDownloadWOTDBottomSheet?.dismiss()
        dialogBuilder.singleChoiceBottomSheet?.dismiss()
        dialogBuilder.colorPickerBottomSheet?.dismiss()
        dialogBuilder.sliderBottomSheet?.dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStop() {
        super.onStop()
        dismissDialogs()
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

    private fun launchFontPicker() {
        Log.d("FontPicker", "Launching picker...")
        (activity as MainActivity).pickCustomFont()
    }

    private fun openCustomIconSelectionDialog(target: IconCacheTarget) {
        val intent = Intent(requireActivity(), CustomIconSelectionActivity::class.java).apply {
            putExtra("IconCacheTarget", "$target")
        }
        startActivity(intent)
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
            AppDrawerFlag.None,
            AppDrawerFlag.SetHomeApp,
            AppDrawerFlag.HiddenApps,
            AppDrawerFlag.PrivateApps,
            AppDrawerFlag.SetFloating,
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

            else -> {
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
}