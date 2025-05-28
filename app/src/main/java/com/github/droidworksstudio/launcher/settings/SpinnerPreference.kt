package com.github.droidworksstudio.launcher.settings

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.github.droidworksstudio.launcher.R

class SpinnerPreference(context: Context, attrs: AttributeSet? = null) : Preference(context, attrs) {

    private var entries: Array<CharSequence>? = null
    private var entryValues: Array<CharSequence>? = null
    private var currentValue: String? = null
    private var defaultNo: String? = null
    private var spinner: Spinner? = null

    init {
        widgetLayoutResource = R.layout.preference_spinner
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.SpinnerPreference,
            0, 0
        ).apply {

            try {
                entries = getTextArray(R.styleable.SpinnerPreference_android_entries)
                entryValues = getTextArray(R.styleable.SpinnerPreference_android_entryValues)
                defaultNo = getString(R.styleable.SpinnerPreference_android_defaultValue)
            } finally {
                recycle()
            }
        }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        spinner = holder.findViewById(R.id.preferenceOptions) as Spinner

        if (entries != null) {
            val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, entries!!)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner?.adapter = adapter
        }

        val selectedIndex = entryValues?.indexOf(currentValue as? CharSequence) ?: entryValues?.indexOf(defaultNo as CharSequence) ?: 0
        spinner?.setSelection(selectedIndex)

        // Somehow prevents an error :D
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            if (selectedIndex >= 0) {
                summary = entries?.get(selectedIndex)
            }
        }, 0)

        spinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val newValue = entryValues?.get(position).toString()
                if (callChangeListener(newValue)) {
                    currentValue = newValue
                    persistString(newValue)
                    summary = entries?.get(position)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    override fun onClick() {
        // Open the spinner dropdown when the preference is clicked
        spinner?.performClick()
    }

    override fun onAttached() {
        super.onAttached()
        currentValue = getPersistedString(defaultNo)
        persistString(getPersistedString(defaultNo))
    }
}