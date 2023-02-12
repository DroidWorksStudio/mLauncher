package com.github.hecodes2much.mlauncher.ui

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
import androidx.fragment.app.DialogFragment
import com.github.hecodes2much.mlauncher.R
import com.github.hecodes2much.mlauncher.data.Prefs
import com.github.hecodes2much.mlauncher.helper.*

class PopupFragment : DialogFragment() {

    private lateinit var prefs: Prefs

    private lateinit var password1: EditText
    private lateinit var password2: EditText
    private lateinit var password3: EditText
    private lateinit var password4: EditText

    private lateinit var confirmPassword1: EditText
    private lateinit var confirmPassword2: EditText
    private lateinit var confirmPassword3: EditText
    private lateinit var confirmPassword4: EditText

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        prefs = Prefs(requireContext())

        val view = inflater.inflate(R.layout.fragment_set_password, container, false)

        password1 = view.findViewById(R.id.password_1)
        password2 = view.findViewById(R.id.password_2)
        password3 = view.findViewById(R.id.password_3)
        password4 = view.findViewById(R.id.password_4)

        initPasswordClickListeners()

        confirmPassword1 = view.findViewById(R.id.confirm_password_1)
        confirmPassword2 = view.findViewById(R.id.confirm_password_2)
        confirmPassword3 = view.findViewById(R.id.confirm_password_3)
        confirmPassword4 = view.findViewById(R.id.confirm_password_4)

        initConfirmPasswordClickListeners()

        val passwordListLabel = view.findViewById<TextView>(R.id.password_list_label)
        passwordListLabel.text = resources.getString(R.string.pin_number)

        val confirmPasswordListLabel = view.findViewById<TextView>(R.id.confirm_password_list_label)
        confirmPasswordListLabel.text = resources.getString(R.string.confirm_pin_number)

        val saveButton = view.findViewById<Button>(R.id.save_button)
        saveButton.setOnClickListener {
            dismiss()
            val context = requireContext()
            val inputMethodManager = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(view?.windowToken, 0)

            if (getPassword() == getConfirmPassword()) {
                showToastLong(context, resources.getString(R.string.pin_number_match))
                prefs.settingPinNumber = getPassword().toInt()
                requireActivity().recreate()
            } else {
                showToastLong(context, resources.getString(R.string.pin_number_do_not_match))
            }

        }

        return view
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

    private fun initConfirmPasswordClickListeners() {
        val passwordBoxes = listOf(confirmPassword1, confirmPassword2, confirmPassword3, confirmPassword4)

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

    private fun getConfirmPassword(): String {
        return listOf(
            confirmPassword1,
            confirmPassword2,
            confirmPassword3,
            confirmPassword4
        ).joinToString("") {
            it.text.toString()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}