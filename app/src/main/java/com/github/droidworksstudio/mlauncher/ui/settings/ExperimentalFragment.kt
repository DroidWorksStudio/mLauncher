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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
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
import com.github.droidworksstudio.mlauncher.helper.isSystemInDarkMode
import com.github.droidworksstudio.mlauncher.helper.setThemeMode
import com.github.droidworksstudio.mlauncher.helper.setTopPadding
import com.github.droidworksstudio.mlauncher.style.SettingsTheme
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.PageHeader
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.SettingsSelect
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.SettingsSwitch
import com.github.droidworksstudio.mlauncher.ui.compose.SettingsComposable.SettingsTitle
import com.github.droidworksstudio.mlauncher.ui.dialogs.DialogManager


class ExperimentalFragment : Fragment() {

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

        setTopPadding(binding.settingsView)
    }

    @Composable
    private fun Settings(fontSize: TextUnit = TextUnit.Unspecified) {
        var toggledExperimentalOptions by remember { mutableStateOf(prefs.enableExperimentalOptions) }
        var selectedSettingsSize by remember { mutableIntStateOf(prefs.settingsSize) }

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
                title = getLocalizedString(R.string.experimental_settings_title),
                onClick = {
                    goBackToLastFragment()
                }
            )

            Spacer(
                modifier = Modifier
                    .height(16.dp)
            )

            SettingsSwitch(
                text = getLocalizedString(R.string.show_floating_button),
                fontSize = titleFontSize,
                defaultState = toggledExperimentalOptions,
                onCheckedChange = {
                    toggledExperimentalOptions = !prefs.enableExperimentalOptions
                    prefs.enableExperimentalOptions = toggledExperimentalOptions
                    goBackToLastFragment()
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