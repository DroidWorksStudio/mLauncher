package com.github.droidworksstudio.mlauncher.ui.settings

import android.content.Intent
import android.os.Bundle
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.github.droidworksstudio.common.getLocalizedString
import com.github.droidworksstudio.common.isGestureNavigationEnabled
import com.github.droidworksstudio.mlauncher.MainViewModel
import com.github.droidworksstudio.mlauncher.R
import com.github.droidworksstudio.mlauncher.data.Constants
import com.github.droidworksstudio.mlauncher.data.Constants.Theme.Dark
import com.github.droidworksstudio.mlauncher.data.Constants.Theme.Light
import com.github.droidworksstudio.mlauncher.data.Constants.Theme.System
import com.github.droidworksstudio.mlauncher.data.Prefs
import com.github.droidworksstudio.mlauncher.databinding.FragmentSettingsBinding
import com.github.droidworksstudio.mlauncher.helper.emptyString
import com.github.droidworksstudio.mlauncher.helper.getHexForOpacity
import com.github.droidworksstudio.mlauncher.helper.hideStatusBar
import com.github.droidworksstudio.mlauncher.helper.isSystemInDarkMode
import com.github.droidworksstudio.mlauncher.helper.setThemeMode
import com.github.droidworksstudio.mlauncher.helper.setTopPadding
import com.github.droidworksstudio.mlauncher.helper.showStatusBar
import com.github.droidworksstudio.mlauncher.style.SettingsTheme
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.PageHeader
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.SettingsSelect
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.SettingsSwitch
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.SettingsTitle
import com.github.droidworksstudio.mlauncher.ui.dialogs.DialogManager
import com.github.droidworksstudio.mlauncher.ui.iconpack.CustomIconSelectionActivity

class LookFeelFragment : Fragment() {

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
        var selectedAppIcons by remember { mutableStateOf(prefs.iconPack) }
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
                title = getLocalizedString(R.string.look_feel_settings_title),
                onClick = {
                    goBackToLastFragment()
                }
            )

            Spacer(
                modifier = Modifier
                    .height(16.dp)
            )

            SettingsTitle(
                text = getLocalizedString(R.string.text_size_adjustments),
                fontSize = titleFontSize,
            )

            SettingsSelect(
                title = getLocalizedString(R.string.app_text_size),
                option = selectedAppSize.toString(),
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showSliderDialog(
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
                    dialogBuilder.showSliderDialog(
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
                    dialogBuilder.showSliderDialog(
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
                    dialogBuilder.showSliderDialog(
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
                    dialogBuilder.showSliderDialog(
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
                    dialogBuilder.showSliderDialog(
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
                    dialogBuilder.showSliderDialog(
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
                        dialogBuilder.showSliderDialog(
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
                title = getLocalizedString(R.string.select_app_icons),
                option = selectedAppIcons.string(),
                fontSize = titleFontSize,
                onClick = {
                    // Generate options and icons
                    val iconPacksEntries = Constants.IconPacks.entries

                    val iconPacksOptions = iconPacksEntries.map { it.getString() }

                    dialogBuilder.showSingleChoiceDialog(
                        context = requireContext(),
                        options = iconPacksOptions.map { it.toString() }.toTypedArray(),
                        titleResId = R.string.select_app_icons,
                        onItemSelected = { newAppIconsName ->
                            val newIconPacksIndex =
                                iconPacksOptions.indexOfFirst { it.toString() == newAppIconsName }
                            if (newIconPacksIndex != -1) {
                                val newAppIcons =
                                    iconPacksEntries[newIconPacksIndex] // Get the selected FontFamily enum
                                if (newAppIcons == Constants.IconPacks.Custom) {
                                    openCustomIconSelectionDialog()
                                } else {
                                    prefs.customIconPack = emptyString()
                                    selectedAppIcons = newAppIcons // Update state
                                    prefs.iconPack = newAppIcons // Persist selection in preferences
                                    viewModel.iconPack.value = newAppIcons
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
                        dialogBuilder.showSliderDialog(
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
                    dialogBuilder.showSingleChoiceDialog(
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
                    dialogBuilder.showSingleChoiceDialog(
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
                    dialogBuilder.showSingleChoiceDialog(
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
                    dialogBuilder.showSingleChoiceDialog(
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
                    dialogBuilder.showSingleChoiceDialog(
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
                    dialogBuilder.showSingleChoiceDialog(
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
                    dialogBuilder.showColorPickerDialog(
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
                    dialogBuilder.showColorPickerDialog(
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
                    dialogBuilder.showColorPickerDialog(
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
                    dialogBuilder.showColorPickerDialog(
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
                    dialogBuilder.showColorPickerDialog(
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
                    dialogBuilder.showColorPickerDialog(
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
                    dialogBuilder.showColorPickerDialog(
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
                    dialogBuilder.showColorPickerDialog(
                        context = requireContext(),
                        color = selectedShortcutIconsColor,
                        titleResId = R.string.shortcuts_color,
                        onItemSelected = { selectedColor ->
                            selectedShortcutIconsColor = selectedColor
                            prefs.shortcutIconsColor = selectedColor
                        })
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

    private fun openCustomIconSelectionDialog() {
        val intent = Intent(requireActivity(), CustomIconSelectionActivity::class.java)
        startActivity(intent)
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
        dialogBuilder.colorPickerDialog?.dismiss()
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