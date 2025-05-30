package com.github.droidworksstudio.mlauncher.ui.settings

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
import com.github.droidworksstudio.mlauncher.data.Constants.Theme.Dark
import com.github.droidworksstudio.mlauncher.data.Constants.Theme.Light
import com.github.droidworksstudio.mlauncher.data.Constants.Theme.System
import com.github.droidworksstudio.mlauncher.data.Prefs
import com.github.droidworksstudio.mlauncher.databinding.FragmentSettingsBinding
import com.github.droidworksstudio.mlauncher.helper.getHexForOpacity
import com.github.droidworksstudio.mlauncher.helper.isSystemInDarkMode
import com.github.droidworksstudio.mlauncher.helper.setThemeMode
import com.github.droidworksstudio.mlauncher.helper.setTopPadding
import com.github.droidworksstudio.mlauncher.style.SettingsTheme
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.PageHeader
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.SettingsSelect
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.SettingsSwitch
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.SettingsTitle
import com.github.droidworksstudio.mlauncher.ui.dialogs.DialogManager

class NotesFragment : Fragment() {

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

        setTopPadding(binding.settingsView)
    }

    @Composable
    private fun Settings(fontSize: TextUnit = TextUnit.Unspecified) {
        var toggledAutoExpandNotes by remember { mutableStateOf(prefs.autoExpandNotes) }
        var toggledClickToEditDelete by remember { mutableStateOf(prefs.clickToEditDelete) }

        var selectedNotesBackgroundColor by remember { mutableIntStateOf(prefs.notesBackgroundColor) }
        var selectedBubbleBackgroundColor by remember { mutableIntStateOf(prefs.bubbleBackgroundColor) }
        var selectedBubbleMessageTextColor by remember { mutableIntStateOf(prefs.bubbleMessageTextColor) }
        var selectedBubbleTimeDateColor by remember { mutableIntStateOf(prefs.bubbleTimeDateColor) }
        var selectedBubbleCategoryColor by remember { mutableIntStateOf(prefs.bubbleCategoryColor) }

        var selectedInputMessageColor by remember { mutableIntStateOf(prefs.inputMessageColor) }
        var selectedInputMessageHintColor by remember { mutableIntStateOf(prefs.inputMessageHintColor) }


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
                title = getLocalizedString(R.string.notes_settings_title),
                onClick = {
                    goBackToLastFragment()
                }
            )

            Spacer(
                modifier = Modifier
                    .height(16.dp)
            )

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
                    dialogBuilder.showColorPickerDialog(
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
                    dialogBuilder.showColorPickerDialog(
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
                    dialogBuilder.showColorPickerDialog(
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
                    dialogBuilder.showColorPickerDialog(
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
                    dialogBuilder.showColorPickerDialog(
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
                    dialogBuilder.showColorPickerDialog(
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
                    dialogBuilder.showColorPickerDialog(
                        context = requireContext(),
                        color = selectedInputMessageHintColor,
                        titleResId = R.string.message_input_hint_color,
                        onItemSelected = { selectedColor ->
                            selectedInputMessageHintColor = selectedColor
                            prefs.inputMessageHintColor = selectedColor
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