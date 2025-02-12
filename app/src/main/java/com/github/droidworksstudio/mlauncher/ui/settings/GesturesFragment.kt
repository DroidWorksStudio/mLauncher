package com.github.droidworksstudio.mlauncher.ui.settings

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
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
import com.github.droidworksstudio.mlauncher.helper.isSystemInDarkMode
import com.github.droidworksstudio.mlauncher.helper.setThemeMode
import com.github.droidworksstudio.mlauncher.listener.DeviceAdmin
import com.github.droidworksstudio.mlauncher.style.SettingsTheme
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.PageHeader
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.SettingsSelect
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.SettingsTitle
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class GesturesFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
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
        val backgroundColor = getHexForOpacity(prefs)
        binding.scrollView.setBackgroundColor(backgroundColor)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        resetThemeColors()
    }

    @Composable
    private fun Settings(fontSize: TextUnit = TextUnit.Unspecified) {
        var selectedDoubleTapAction by remember { mutableStateOf(prefs.doubleTapAction) }
        var selectedClickClockAction by remember { mutableStateOf(prefs.clickClockAction) }
        var selectedClickDateAction by remember { mutableStateOf(prefs.clickDateAction) }
        var selectedClickAppUsageAction by remember { mutableStateOf(prefs.clickAppUsageAction) }

        var selectedShortSwipeUpAction by remember { mutableStateOf(prefs.shortSwipeUpAction) }
        var selectedShortSwipeDownAction by remember { mutableStateOf(prefs.shortSwipeDownAction) }
        var selectedShortSwipeLeftAction by remember { mutableStateOf(prefs.shortSwipeLeftAction) }
        var selectedShortSwipeRightAction by remember { mutableStateOf(prefs.shortSwipeRightAction) }

        var selectedLongSwipeUpAction by remember { mutableStateOf(prefs.longSwipeUpAction) }
        var selectedLongSwipeDownAction by remember { mutableStateOf(prefs.longSwipeDownAction) }
        var selectedLongSwipeLeftAction by remember { mutableStateOf(prefs.longSwipeLeftAction) }
        var selectedLongSwipeRightAction by remember { mutableStateOf(prefs.longSwipeRightAction) }

        val fs = remember { mutableStateOf(fontSize) }
        Constants.updateMaxHomePages(requireContext())

        val titleFontSize = if (fs.value.isSpecified) {
            (fs.value.value * 1.5).sp
        } else fs.value

        Column {
            PageHeader(
                iconRes = R.drawable.ic_back,
                title = stringResource(R.string.gestures_settings_title),
                onClick = { goBackToLastFragment() }
            )

            Spacer(
                modifier = Modifier
                    .height(16.dp)
            )

            SettingsTitle(
                text = stringResource(R.string.tap_click_actions),
                fontSize = titleFontSize,
            )

            val appLabelDoubleTapAction = prefs.appDoubleTap.activityLabel
            SettingsSelect(
                title = stringResource(R.string.double_tap),
                option = if (selectedDoubleTapAction == Action.OpenApp) {
                    "${stringResource(R.string.open)} $appLabelDoubleTapAction"
                } else {
                    selectedDoubleTapAction.string()
                },
                fontSize = titleFontSize,
                onClick = {
                    showSingleChoiceDialog(
                        context = requireContext(),
                        options = Constants.Action.entries.toTypedArray(),
                        titleResId = R.string.double_tap,
                        onItemSelected = { newDoubleTapAction ->
                            selectedDoubleTapAction = newDoubleTapAction // Update state
                            setGesture(
                                AppDrawerFlag.SetDoubleTap,
                                newDoubleTapAction
                            ) // Persist selection in preferences
                        }
                    )
                }
            )

            val appLabelClickClockAction = prefs.appClickClock.activityLabel.ifEmpty { "Clock" }
            SettingsSelect(
                title = stringResource(R.string.clock_click_app),
                option = if (selectedClickClockAction == Action.OpenApp) {
                    "${stringResource(R.string.open)} $appLabelClickClockAction"
                } else {
                    selectedClickClockAction.string()
                },
                fontSize = titleFontSize,
                onClick = {
                    showSingleChoiceDialog(
                        context = requireContext(),
                        options = Constants.Action.entries.toTypedArray(),
                        titleResId = R.string.clock_click_app,
                        onItemSelected = { newClickClock ->
                            selectedClickClockAction = newClickClock // Update state
                            setGesture(
                                AppDrawerFlag.SetClickClock,
                                newClickClock
                            ) // Persist selection in preferences
                        }
                    )
                }
            )

            val appLabelClickDateAction = prefs.appClickDate.activityLabel.ifEmpty { "Calendar" }
            SettingsSelect(
                title = stringResource(R.string.date_click_app),
                option = if (selectedClickDateAction == Action.OpenApp) {
                    "${stringResource(R.string.open)} $appLabelClickDateAction"
                } else {
                    selectedClickDateAction.string()
                },
                fontSize = titleFontSize,
                onClick = {
                    showSingleChoiceDialog(
                        context = requireContext(),
                        options = Constants.Action.entries.toTypedArray(),
                        titleResId = R.string.date_click_app,
                        onItemSelected = { newClickDate ->
                            selectedClickDateAction = newClickDate // Update state
                            setGesture(
                                AppDrawerFlag.SetClickDate,
                                newClickDate
                            ) // Persist selection in preferences
                        }
                    )
                }
            )

            val appLabelClickAppUsageAction = prefs.appClickUsage.activityLabel.ifEmpty { "Digital Wellbeing" }
            SettingsSelect(
                title = stringResource(R.string.usage_click_app),
                option = if (selectedClickAppUsageAction == Action.OpenApp) {
                    "${stringResource(R.string.open)} $appLabelClickAppUsageAction"
                } else {
                    selectedClickAppUsageAction.string()
                },
                fontSize = titleFontSize,
                onClick = {
                    showSingleChoiceDialog(
                        context = requireContext(),
                        options = Constants.Action.entries.toTypedArray(),
                        titleResId = R.string.usage_click_app,
                        onItemSelected = { newClickAppUsage ->
                            selectedClickAppUsageAction = newClickAppUsage // Update state
                            setGesture(
                                AppDrawerFlag.SetAppUsage,
                                newClickAppUsage
                            ) // Persist selection in preferences
                        }
                    )
                }
            )

            SettingsTitle(
                text = stringResource(R.string.swipe_movement),
                fontSize = titleFontSize,
            )

            val appLabelShortSwipeUpAction = prefs.appShortSwipeUp.activityLabel.ifEmpty { "Camera" }
            SettingsSelect(
                title = stringResource(R.string.short_swipe_up_app),
                option = if (selectedShortSwipeUpAction == Action.OpenApp) {
                    "${stringResource(R.string.open)} $appLabelShortSwipeUpAction"
                } else {
                    selectedShortSwipeUpAction.string()
                },
                fontSize = titleFontSize,
                onClick = {
                    showSingleChoiceDialog(
                        context = requireContext(),
                        options = Constants.Action.entries.toTypedArray(),
                        titleResId = R.string.short_swipe_up_app,
                        onItemSelected = { newShortSwipeUpAction ->
                            selectedShortSwipeUpAction = newShortSwipeUpAction // Update state
                            setGesture(
                                AppDrawerFlag.SetShortSwipeUp,
                                newShortSwipeUpAction
                            ) // Persist selection in preferences
                        }
                    )
                }
            )

            val appLabelShortSwipeDownAction = prefs.appShortSwipeDown.activityLabel.ifEmpty { "Phone" }
            SettingsSelect(
                title = stringResource(R.string.short_swipe_down_app),
                option = if (selectedShortSwipeDownAction == Action.OpenApp) {
                    "${stringResource(R.string.open)} $appLabelShortSwipeDownAction"
                } else {
                    selectedShortSwipeDownAction.string()
                },
                fontSize = titleFontSize,
                onClick = {
                    showSingleChoiceDialog(
                        context = requireContext(),
                        options = Constants.Action.entries.toTypedArray(),
                        titleResId = R.string.short_swipe_down_app,
                        onItemSelected = { newShortSwipeDownAction ->
                            selectedShortSwipeDownAction = newShortSwipeDownAction // Update state
                            setGesture(
                                AppDrawerFlag.SetShortSwipeDown,
                                newShortSwipeDownAction
                            ) // Persist selection in preferences
                        }
                    )
                }
            )

            val appLabelShortSwipeLeftAction = prefs.appShortSwipeLeft.activityLabel.ifEmpty { "Camera" }
            SettingsSelect(
                title = stringResource(R.string.short_swipe_left_app),
                option = if (selectedShortSwipeLeftAction == Action.OpenApp) {
                    "${stringResource(R.string.open)} $appLabelShortSwipeLeftAction"
                } else {
                    selectedShortSwipeLeftAction.string()
                },
                fontSize = titleFontSize,
                onClick = {
                    showSingleChoiceDialog(
                        context = requireContext(),
                        options = Constants.Action.entries.toTypedArray(),
                        titleResId = R.string.short_swipe_left_app,
                        onItemSelected = { newShortSwipeLeftAction ->
                            selectedShortSwipeLeftAction = newShortSwipeLeftAction // Update state
                            setGesture(
                                AppDrawerFlag.SetShortSwipeLeft,
                                newShortSwipeLeftAction
                            ) // Persist selection in preferences
                        }
                    )
                }
            )

            val appLabelShortSwipeRightAction = prefs.appShortSwipeRight.activityLabel.ifEmpty { "Phone" }
            SettingsSelect(
                title = stringResource(R.string.short_swipe_right_app),
                option = if (selectedShortSwipeRightAction == Action.OpenApp) {
                    "${stringResource(R.string.open)} $appLabelShortSwipeRightAction"
                } else {
                    selectedShortSwipeRightAction.string()
                },
                fontSize = titleFontSize,
                onClick = {
                    showSingleChoiceDialog(
                        context = requireContext(),
                        options = Constants.Action.entries.toTypedArray(),
                        titleResId = R.string.short_swipe_right_app,
                        onItemSelected = { newShortSwipeRightAction ->
                            selectedShortSwipeRightAction = newShortSwipeRightAction // Update state
                            setGesture(
                                AppDrawerFlag.SetShortSwipeRight,
                                newShortSwipeRightAction
                            ) // Persist selection in preferences
                        }
                    )
                }
            )

            val appLabelLongSwipeUpAction = prefs.appLongSwipeUp.activityLabel.ifEmpty { "Camera" }
            SettingsSelect(
                title = stringResource(R.string.long_swipe_up_app),
                option = if (selectedLongSwipeUpAction == Action.OpenApp) {
                    "${stringResource(R.string.open)} $appLabelLongSwipeUpAction"
                } else {
                    selectedLongSwipeUpAction.string()
                },
                fontSize = titleFontSize,
                onClick = {
                    showSingleChoiceDialog(
                        context = requireContext(),
                        options = Constants.Action.entries.toTypedArray(),
                        titleResId = R.string.long_swipe_up_app,
                        onItemSelected = { newLongSwipeUpAction ->
                            selectedLongSwipeUpAction = newLongSwipeUpAction // Update state
                            setGesture(
                                AppDrawerFlag.SetLongSwipeUp,
                                newLongSwipeUpAction
                            ) // Persist selection in preferences
                        }
                    )
                }
            )

            val appLabelLongSwipeDownAction = prefs.appLongSwipeDown.activityLabel.ifEmpty { "Phone" }
            SettingsSelect(
                title = stringResource(R.string.long_swipe_down_app),
                option = if (selectedLongSwipeDownAction == Action.OpenApp) {
                    "${stringResource(R.string.open)} $appLabelLongSwipeDownAction"
                } else {
                    selectedLongSwipeDownAction.string()
                },
                fontSize = titleFontSize,
                onClick = {
                    showSingleChoiceDialog(
                        context = requireContext(),
                        options = Constants.Action.entries.toTypedArray(),
                        titleResId = R.string.long_swipe_down_app,
                        onItemSelected = { newLongSwipeDownAction ->
                            selectedLongSwipeDownAction = newLongSwipeDownAction // Update state
                            setGesture(
                                AppDrawerFlag.SetLongSwipeDown,
                                newLongSwipeDownAction
                            ) // Persist selection in preferences
                        }
                    )
                }
            )

            val appLabelLongSwipeLeftAction = prefs.appLongSwipeLeft.activityLabel.ifEmpty { "Camera" }
            SettingsSelect(
                title = stringResource(R.string.long_swipe_left_app),
                option = if (selectedLongSwipeLeftAction == Action.OpenApp) {
                    "${stringResource(R.string.open)} $appLabelLongSwipeLeftAction"
                } else {
                    selectedLongSwipeLeftAction.string()
                },
                fontSize = titleFontSize,
                onClick = {
                    showSingleChoiceDialog(
                        context = requireContext(),
                        options = Constants.Action.entries.toTypedArray(),
                        titleResId = R.string.long_swipe_left_app,
                        onItemSelected = { newLongSwipeLeftAction ->
                            selectedLongSwipeLeftAction = newLongSwipeLeftAction // Update state
                            setGesture(
                                AppDrawerFlag.SetLongSwipeLeft,
                                newLongSwipeLeftAction
                            ) // Persist selection in preferences
                        }
                    )
                }
            )

            val appLabelLongSwipeRightAction = prefs.appLongSwipeRight.activityLabel.ifEmpty { "Phone" }
            SettingsSelect(
                title = stringResource(R.string.long_swipe_right_app),
                option = if (selectedLongSwipeRightAction == Action.OpenApp) {
                    "${stringResource(R.string.open)} $appLabelLongSwipeRightAction"
                } else {
                    selectedLongSwipeRightAction.string()
                },
                fontSize = titleFontSize,
                onClick = {
                    showSingleChoiceDialog(
                        context = requireContext(),
                        options = Constants.Action.entries.toTypedArray(),
                        titleResId = R.string.long_swipe_right_app,
                        onItemSelected = { newLongSwipeRightAction ->
                            selectedLongSwipeRightAction = newLongSwipeRightAction // Update state
                            setGesture(
                                AppDrawerFlag.SetLongSwipeRight,
                                newLongSwipeRightAction
                            ) // Persist selection in preferences
                        }
                    )
                }
            )
        }
    }

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

    private var singleChoiceDialog: AlertDialog? = null

    private fun <T> showSingleChoiceDialog(
        context: Context,
        options: Array<T>,
        titleResId: Int,
        fonts: List<Typeface>? = null, // Optional fonts
        fontSize: Float = 18f, // Default font size
        onItemSelected: (T) -> Unit
    ) {
        val itemStrings = options.map { option ->
            when (option) {
                is Constants.Language -> option.getString(context) // Use getString() if it's a Language enum
                is Enum<*> -> option.name // Fallback for other Enums
                else -> option.toString() // Generic fallback
            }
        }

        // Inflate the custom dialog layout
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_single_choice, null)
        val listView = dialogView.findViewById<ListView>(R.id.dialogListView)
        val titleView = dialogView.findViewById<TextView>(R.id.dialogTitle)

        // Set title text
        titleView.text = context.getString(titleResId)

        // Setup adapter for the ListView
        val adapter = object : ArrayAdapter<String>(context, R.layout.item_single_choice, itemStrings) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view =
                    convertView ?: LayoutInflater.from(context).inflate(R.layout.item_single_choice, parent, false)
                val textView = view.findViewById<TextView>(R.id.text_item)

                // Set text, font, and size
                textView.text = itemStrings[position]
                textView.typeface = fonts?.getOrNull(position) ?: Typeface.DEFAULT
                textView.textSize = fontSize

                return view
            }
        }
        listView.adapter = adapter

        // Create the dialog
        val dialog = MaterialAlertDialogBuilder(context)
            .setView(dialogView)
            .create()

        dialog.show()

        // Handle item selection (auto-close dialog)
        listView.setOnItemClickListener { _, _, position, _ ->
            onItemSelected(options[position]) // Callback with selected item
            dialog.dismiss() // Close dialog immediately
        }

        // Ensure the dialog width remains WRAP_CONTENT
        dialog.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        // Enforce max height (7 items max)
        listView.post {
            val itemHeight = listView.getChildAt(0)?.height ?: return@post
            val maxHeight = itemHeight * 7 // Max height for 7 items
            listView.layoutParams.height = maxHeight.coerceAtMost(itemHeight * options.size)
            listView.requestLayout()
        }
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
                    R.id.action_gesturesFragment_to_appListFragment,
                    bundleOf("flag" to flag.toString())
                )
            }

            else -> {}
        }
    }

    private fun dismissDialogs() {
        singleChoiceDialog?.dismiss()
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