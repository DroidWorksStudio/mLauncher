package com.github.droidworksstudio.mlauncher.ui.settings

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.github.droidworksstudio.mlauncher.MainViewModel
import com.github.droidworksstudio.mlauncher.R
import com.github.droidworksstudio.mlauncher.data.Constants
import com.github.droidworksstudio.mlauncher.data.Constants.Theme.Dark
import com.github.droidworksstudio.mlauncher.data.Constants.Theme.Light
import com.github.droidworksstudio.mlauncher.data.Constants.Theme.System
import com.github.droidworksstudio.mlauncher.data.Prefs
import com.github.droidworksstudio.mlauncher.databinding.FragmentSettingsBinding
import com.github.droidworksstudio.mlauncher.helper.isSystemInDarkMode
import com.github.droidworksstudio.mlauncher.helper.setThemeMode
import com.github.droidworksstudio.mlauncher.listener.DeviceAdmin
import com.github.droidworksstudio.mlauncher.style.SettingsTheme
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.PageHeader
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.SettingsSelect
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.SettingsSwitch
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.SettingsTitle
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class FeaturesFragment : Fragment() {

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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        resetThemeColors()
    }

    @Composable
    private fun Settings(fontSize: TextUnit = TextUnit.Unspecified) {
        var selectedTheme by remember { mutableStateOf(prefs.appTheme) }
        var selectedLanguage by remember { mutableStateOf(prefs.appLanguage) }
        var selectedFilterStrength by remember { mutableIntStateOf(prefs.filterStrength) }
        val fs = remember { mutableStateOf(fontSize) }
        Constants.updateMaxHomePages(requireContext())

        val titleFontSize = if (fs.value.isSpecified) {
            (fs.value.value * 1.5).sp
        } else fs.value

        Column {
            PageHeader(
                iconRes = R.drawable.ic_back,
                title = stringResource(R.string.features_settings_title),
                onClick = { goBackToLastFragment() }
            )

            Spacer(
                modifier = Modifier
                    .height(16.dp)
            )

            SettingsTitle(
                text = stringResource(R.string.behavior),
                fontSize = titleFontSize,
            )

            SettingsSelect(
                title = stringResource(R.string.theme_mode),
                option = selectedTheme.string(),
                fontSize = titleFontSize,
                onClick = {
                    showSingleChoiceDialog(
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
                            setThemeMode(requireContext(), isDark, binding.scrollView)
                            requireActivity().recreate()
                        }
                    )
                }
            )

            SettingsSelect(
                title = stringResource(R.string.app_language),
                option = selectedLanguage.string(),
                fontSize = titleFontSize,
                onClick = {
                    showSingleChoiceDialog(
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

            SettingsSwitch(
                text = stringResource(R.string.auto_show_keyboard),
                fontSize = titleFontSize,
                defaultState = prefs.autoShowKeyboard,
                onCheckedChange = { _ ->
                    prefs.autoShowKeyboard = !prefs.autoShowKeyboard
                }
            )

            SettingsSwitch(
                text = stringResource(R.string.auto_open_apps),
                fontSize = titleFontSize,
                defaultState = prefs.autoOpenApp,
                onCheckedChange = { _ ->
                    prefs.autoOpenApp = !prefs.autoOpenApp
                }
            )

            SettingsSwitch(
                text = stringResource(R.string.search_from_start),
                fontSize = titleFontSize,
                defaultState = prefs.searchFromStart,
                onCheckedChange = { _ ->
                    prefs.searchFromStart = !prefs.searchFromStart
                }
            )

            SettingsSwitch(
                text = stringResource(R.string.lock_settings),
                fontSize = titleFontSize,
                defaultState = prefs.settingsLocked,
                onCheckedChange = { _ ->
                    prefs.settingsLocked = !prefs.settingsLocked
                }
            )

            SettingsSelect(
                title = stringResource(R.string.filter_strength),
                option = selectedFilterStrength.toString(),
                fontSize = titleFontSize,
                onClick = {
                    showSliderDialog(
                        context = requireContext(),
                        title = getString(R.string.filter_strength),
                        minValue = Constants.MIN_FILTER_STRENGTH,
                        maxValue = Constants.MAX_FILTER_STRENGTH,
                        currentValue = prefs.filterStrength,
                        onValueSelected = { newFilterStrength ->
                            selectedFilterStrength = newFilterStrength // Update state
                            prefs.filterStrength = newFilterStrength // Persist selection in preferences
                            viewModel.filterStrength.value = newFilterStrength
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
        val itemStrings = options.map { (it as Enum<*>).name } // Convert enum options to string

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

    private var sliderDialog: AlertDialog? = null

    private fun showSliderDialog(
        context: Context,
        title: String,
        minValue: Int,
        maxValue: Int,
        currentValue: Int,
        onValueSelected: (Int) -> Unit // Callback for when the user selects a value
    ) {
        // Dismiss any existing dialog to prevent multiple dialogs from being open simultaneously
        sliderDialog?.dismiss()

        var seekBar: SeekBar

        // Create a layout to hold the SeekBar and the value display
        val seekBarLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(16, 16, 16, 16)

            // TextView to display the current value
            val valueText = TextView(context).apply {
                text = "$currentValue"
                textSize = 16f
                gravity = Gravity.CENTER
            }

            // Declare the seekBar outside the layout block so we can access it later
            seekBar = SeekBar(context).apply {
                min = minValue // Minimum value
                max = maxValue // Maximum value
                progress = currentValue // Default value
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                        valueText.text = "$progress"
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar) {
                        // Not used
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar) {
                        // Not used
                    }
                })
            }

            // Add TextView and SeekBar to the layout
            addView(valueText)
            addView(seekBar)
        }

        // Create the dialog
        val dialogBuilder = MaterialAlertDialogBuilder(context).apply {
            setTitle(title)
            setView(seekBarLayout) // Add the slider directly to the dialog
            setPositiveButton(context.getString(R.string.okay)) { _, _ ->
                // Get the progress from the seekBar now that it's accessible
                val finalValue = seekBar.progress
                onValueSelected(finalValue) // Trigger the callback with the selected value
            }
            setNegativeButton(context.getString(R.string.cancel), null)
        }

        // Assign the created dialog to sliderDialog and show it
        sliderDialog = dialogBuilder.create()
        sliderDialog?.show()
    }


    private fun dismissDialogs() {
        singleChoiceDialog?.dismiss()
        sliderDialog?.dismiss()
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