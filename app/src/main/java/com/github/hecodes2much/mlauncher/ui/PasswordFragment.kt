package com.github.hecodes2much.mlauncher.ui

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.github.hecodes2much.mlauncher.R
import com.github.hecodes2much.mlauncher.data.Prefs
import com.github.hecodes2much.mlauncher.databinding.FragmentPasswordBinding
import com.github.hecodes2much.mlauncher.helper.*
import java.lang.System.currentTimeMillis
import java.util.concurrent.TimeUnit

class PasswordFragment : Fragment() {

    private lateinit var prefs: Prefs

    private var _binding: FragmentPasswordBinding? = null
    private val binding get() = _binding!!

    private lateinit var password1: EditText
    private lateinit var password2: EditText
    private lateinit var password3: EditText
    private lateinit var password4: EditText

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        prefs = Prefs(requireContext())
        _binding = FragmentPasswordBinding.inflate(inflater, container, false)
        val view = binding.root
        val currentTime = currentTimeMillis()
        if (prefs.lastOpenSettings !== null && isNumeric(prefs.lastOpenSettings!!)) {
            val dateTime = prefs.lastOpenSettings!!.toLong()
            val minutesBetween = convertToMinutes(currentTime, dateTime)
            val timer = prefs.lockSettingsTime
            val hasPassedMinutes = isLongerThanMinutes(minutesBetween, timer.toLong())

            if (timer == 0) {
                findNavController().navigate(R.id.action_passwordFragment_to_settingsFragment)
                return view
            }
            if (hasPassedMinutes && prefs.settingPinNumber != 123456) {

                password1 = view.findViewById(R.id.password_1)
                password2 = view.findViewById(R.id.password_2)
                password3 = view.findViewById(R.id.password_3)
                password4 = view.findViewById(R.id.password_4)

                initPasswordClickListeners()

                password4.setOnKeyListener { _, keyCode, _ ->
                    if (
                        keyCode in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 &&
                        password4.text.toString().isNotEmpty()
                    ) {
                        val context = requireContext()
                        val inputMethodManager =
                            activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)

                        if (getPassword() == prefs.settingPinNumber.toString()) {
                            showToastLong(
                                context,
                                resources.getString(R.string.pin_number_match)
                            )
                            prefs.lastOpenSettings = currentTime.toString()
                            findNavController().navigate(
                                R.id.action_passwordFragment_to_settingsFragment
                            )
                        } else {
                            showToastLong(
                                context,
                                resources.getString(R.string.pin_number_do_not_match)
                            )
                        }
                    }
                    false
                }

                showKeyboard(view)
                return view
            }
        } else {
            prefs.lastOpenSettings = currentTime.toString()
        }
        findNavController().navigate(R.id.action_passwordFragment_to_settingsFragment)
        return view
    }

    private fun isNumeric(toCheck: String): Boolean {
        val regex = "-?\\d+(\\.\\d+)?".toRegex()
        return toCheck.matches(regex)
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

    private fun convertToMinutes(currentTime: Long, dateTime: Long): Long {
        val convertTime: Long = (currentTime) - (dateTime)
        return TimeUnit.MILLISECONDS.toMinutes(convertTime)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val hex = getHexForOpacity(requireContext(), prefs)
        binding.scrollView.setBackgroundColor(hex)
    }

    override fun onStart() {
        super.onStart()
        val typeface = ResourcesCompat.getFont(requireActivity(), R.font.roboto)

        binding.lock.textSize = prefs.textSizeLauncher.toFloat() * 1.5f
        binding.message.textSize = prefs.textSizeLauncher.toFloat() * 1.2f
        binding.lock.typeface = typeface

        if (prefs.followAccentColors) {
            val fontColor = getHexFontColor(requireContext())
            binding.lock.setTextColor(fontColor)
            binding.message.setTextColor(fontColor)
        }
        binding.lock.text = "\uf023"
    }

    override fun onResume() {
        super.onResume()
        val hex = getHexForOpacity(requireContext(), prefs)
        binding.scrollView.setBackgroundColor(hex)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


