package com.github.droidworksstudio.mlauncher.ui.components

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.graphics.toColorInt
import com.github.droidworksstudio.common.getLocalizedString
import com.github.droidworksstudio.mlauncher.MainActivity
import com.github.droidworksstudio.mlauncher.R
import com.github.droidworksstudio.mlauncher.data.Constants
import com.github.droidworksstudio.mlauncher.data.Prefs
import com.github.droidworksstudio.mlauncher.helper.themeDownloadButton
import com.github.droidworksstudio.mlauncher.helper.utils.AppReloader
import com.github.droidworksstudio.mlauncher.helper.wordofthedayDownloadButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DialogManager(val context: Context, val activity: Activity) {

    private lateinit var prefs: Prefs

    var backupRestoreBottomSheet: LockedBottomSheetDialog? = null

    fun showBackupRestoreBottomSheet() {
        // Dismiss existing bottom sheet if it's showing
        backupRestoreBottomSheet?.dismiss()

        // Create layout programmatically
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // Utility function to create a clickable item
        fun createItem(text: String, onClick: () -> Unit): TextView {
            return TextView(context).apply {
                this.text = text
                textSize = 16f
                setPadding(0, 32, 0, 32)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    onClick()
                    backupRestoreBottomSheet?.dismiss()
                }
            }
        }

        // Add the three items
        layout.addView(createItem(getLocalizedString(R.string.advanced_settings_backup_restore_backup)) {
            (activity as MainActivity).createFullBackup()
        })

        layout.addView(createItem(getLocalizedString(R.string.advanced_settings_backup_restore_restore)) {
            (activity as MainActivity).restoreFullBackup()
        })

        layout.addView(createItem(getLocalizedString(R.string.advanced_settings_backup_restore_clear)) {
            confirmClearData()
        })

        // Create and show the bottom sheet
        backupRestoreBottomSheet = LockedBottomSheetDialog(context).apply {
            setContentView(layout)
        }
        backupRestoreBottomSheet?.show() // ✅ Correct method call
    }

    var saveLoadThemeBottomSheet: LockedBottomSheetDialog? = null

    fun showSaveLoadThemeBottomSheet() {
        // Dismiss any existing bottom sheet
        saveLoadThemeBottomSheet?.dismiss()

        // Create vertical layout
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // Utility function to create clickable items
        fun createItem(text: String, onClick: () -> Unit): TextView {
            return TextView(context).apply {
                this.text = text
                textSize = 16f
                setPadding(0, 32, 0, 32)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    onClick()
                    saveLoadThemeBottomSheet?.dismiss()
                }
            }
        }

        // Add Download Option
        layout.addView(createItem(getLocalizedString(R.string.advanced_settings_theme_download)) {
            themeDownloadButton(context)
        })

        // Add Export and Import options
        layout.addView(createItem(getLocalizedString(R.string.advanced_settings_theme_export)) {
            (activity as MainActivity).createThemeBackup()
        })

        layout.addView(createItem(getLocalizedString(R.string.advanced_settings_theme_import)) {
            (activity as MainActivity).restoreThemeBackup()
        })

        // Create and show the LockedBottomSheetDialog
        saveLoadThemeBottomSheet = LockedBottomSheetDialog(context).apply {
            setContentView(layout)
        }
        saveLoadThemeBottomSheet?.show()
    }

    var saveDownloadWOTDBottomSheet: LockedBottomSheetDialog? = null

    fun showSaveDownloadWOTDBottomSheet() {
        // Dismiss any existing bottom sheet
        saveDownloadWOTDBottomSheet?.dismiss()

        // Create vertical layout
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // Utility function to create clickable items
        fun createItem(text: String, onClick: () -> Unit): TextView {
            return TextView(context).apply {
                this.text = text
                textSize = 16f
                setPadding(0, 32, 0, 32)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    onClick()
                    saveDownloadWOTDBottomSheet?.dismiss()
                }
            }
        }

        // Add Download Option
        layout.addView(createItem(getLocalizedString(R.string.advanced_settings_wotd_download)) {
            wordofthedayDownloadButton(context)
        })

        // Add Import options
        layout.addView(createItem(getLocalizedString(R.string.advanced_settings_wotd_import)) {
            (activity as MainActivity).restoreWordsBackup()
        })

        // Create and show the LockedBottomSheetDialog
        saveDownloadWOTDBottomSheet = LockedBottomSheetDialog(context).apply {
            setContentView(layout)
        }
        saveDownloadWOTDBottomSheet?.show()
    }


    // Function to handle the Clear Data action, with a confirmation dialog
    private fun confirmClearData() {
        MaterialAlertDialogBuilder(context)
            .setTitle(getLocalizedString(R.string.advanced_settings_backup_restore_clear_title))
            .setMessage(getLocalizedString(R.string.advanced_settings_backup_restore_clear_description))
            .setPositiveButton(getLocalizedString(R.string.advanced_settings_backup_restore_clear_yes)) { _, _ ->
                clearData()
            }
            .setNegativeButton(getLocalizedString(R.string.advanced_settings_backup_restore_clear_no), null)
            .show()
    }

    private fun clearData() {
        prefs = Prefs(context)
        prefs.clear()

        AppReloader.restartApp(context)
    }

    var sliderBottomSheet: LockedBottomSheetDialog? = null

    fun showSliderBottomSheet(
        context: Context,
        title: String,
        minValue: Int,
        maxValue: Int,
        currentValue: Int,
        onValueSelected: (Int) -> Unit
    ) {
        // Dismiss any existing sheet
        sliderBottomSheet?.dismiss()

        // Outer layout
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Title
        val titleText = TextView(context).apply {
            text = title
            textSize = 18f
            setPadding(0, 0, 0, 32)
            gravity = Gravity.CENTER
        }

        // Value display
        val valueText = TextView(context).apply {
            text = currentValue.toString()
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }

        // SeekBar with callback on stop
        val seekBar = SeekBar(context).apply {
            min = minValue
            max = maxValue
            progress = currentValue
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    valueText.text = progress.toString()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    onValueSelected(seekBar?.progress ?: currentValue)
                }
            })
        }

        // Build and show layout
        container.addView(titleText)
        container.addView(valueText)
        container.addView(seekBar)

        sliderBottomSheet = LockedBottomSheetDialog(context).apply {
            setContentView(container)
            show()
        }
    }

    var singleChoiceBottomSheet: LockedBottomSheetDialog? = null

    fun <T> showSingleChoiceBottomSheet(
        context: Context,
        options: Array<T>,
        titleResId: Int,
        fonts: List<Typeface>? = null,
        fontSize: Float = 18f,
        onItemSelected: (T) -> Unit
    ) {
        // Dismiss any existing sheet
        singleChoiceBottomSheet?.dismiss()

        // Convert options to display strings
        val itemStrings = options.map {
            when (it) {
                is Constants.Language -> it.getString()
                is Enum<*> -> it.name
                else -> it.toString()
            }
        }

        // Create root container
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Title
        val titleView = TextView(context).apply {
            text = context.getString(titleResId)
            textSize = 20f
            setPadding(0, 0, 0, 32)
            gravity = Gravity.CENTER
        }

        // ListView
        val listView = ListView(context).apply {
            divider = null
            isVerticalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Custom adapter with font support
        val adapter = object : ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, itemStrings) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: TextView(context).apply {
                    setPadding(32, 24, 32, 24)
                    layoutParams = AbsListView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }

                (view as TextView).apply {
                    text = itemStrings[position]
                    typeface = fonts?.getOrNull(position) ?: Typeface.DEFAULT
                    textSize = fontSize
                }

                return view
            }
        }

        listView.adapter = adapter

        // Add title and list to layout
        container.addView(titleView)
        container.addView(listView)

        // Create and show LockedBottomSheetDialog
        singleChoiceBottomSheet = LockedBottomSheetDialog(context).apply {
            setContentView(container)
            show()
        }

        // Item click handling
        listView.setOnItemClickListener { _, _, position, _ ->
            onItemSelected(options[position])
            singleChoiceBottomSheet?.dismiss()
        }

        // Limit visible height to 7 items
        listView.post {
            val itemHeight = listView.getChildAt(0)?.height ?: 0
            if (itemHeight > 0) {
                val maxHeight = itemHeight * 7
                listView.layoutParams.height = minOf(maxHeight, itemHeight * options.size)
                listView.requestLayout()
            }
        }
    }

    var colorPickerBottomSheet: LockedBottomSheetDialog? = null

    fun showColorPickerBottomSheet(
        context: Context,
        titleResId: Int,
        color: Int,
        onItemSelected: (Int) -> Unit
    ) {
        colorPickerBottomSheet?.dismiss()

        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)

        var isUpdatingText = false

        // Title
        val titleTextView = TextView(context).apply {
            text = context.getString(titleResId)
            textSize = 20f
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 16)
        }

        // SeekBars
        val redSeekBar = createColorSeekBar(context, red)
        val greenSeekBar = createColorSeekBar(context, green)
        val blueSeekBar = createColorSeekBar(context, blue)

        // Color preview
        val colorPreviewBox = createColorPreviewBox(context, color)

        // Hex input
        val rgbText = createRgbTextField(context, red, green, blue)

        // Layout
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Horizontal layout for text and preview
        val horizontalLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 16, 0, 32)

            val rgbParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 16
            }
            rgbText.layoutParams = rgbParams
            addView(rgbText)

            val previewParams = LinearLayout.LayoutParams(150, 50)
            colorPreviewBox.layoutParams = previewParams
            addView(colorPreviewBox)
        }

        // Add all views to layout
        layout.addView(titleTextView)
        layout.addView(redSeekBar)
        layout.addView(greenSeekBar)
        layout.addView(blueSeekBar)
        layout.addView(horizontalLayout)

        // Shared update function
        val updateColor = {
            val updatedColor = Color.rgb(redSeekBar.progress, greenSeekBar.progress, blueSeekBar.progress)
            colorPreviewBox.setBackgroundColor(updatedColor)

            if (!isUpdatingText) {
                isUpdatingText = true
                rgbText.setText(String.format("#%02X%02X%02X", redSeekBar.progress, greenSeekBar.progress, blueSeekBar.progress))
                isUpdatingText = false
            }

            // Auto-save
            onItemSelected(updatedColor)
        }

        // Listeners
        redSeekBar.setOnSeekBarChangeListener(createSeekBarChangeListener(updateColor))
        greenSeekBar.setOnSeekBarChangeListener(createSeekBarChangeListener(updateColor))
        blueSeekBar.setOnSeekBarChangeListener(createSeekBarChangeListener(updateColor))

        rgbText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (isUpdatingText) return
                s?.toString()?.trim()?.let { colorString ->
                    if (colorString.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
                        val hexColor = try {
                            colorString.toColorInt()
                        } catch (_: Exception) {
                            return
                        }
                        redSeekBar.progress = Color.red(hexColor)
                        greenSeekBar.progress = Color.green(hexColor)
                        blueSeekBar.progress = Color.blue(hexColor)
                        updateColor()
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Show bottom sheet
        colorPickerBottomSheet = LockedBottomSheetDialog(context).apply {
            setContentView(layout)
            show()
        }
    }

    private fun createColorSeekBar(context: Context, initialValue: Int): SeekBar {
        return SeekBar(context).apply {
            max = 255
            progress = initialValue
        }
    }

    private fun createColorPreviewBox(context: Context, color: Int): View {
        return View(context).apply {
            setBackgroundColor(color)
        }
    }

    private fun createRgbTextField(context: Context, red: Int, green: Int, blue: Int): EditText {
        return EditText(context).apply {
            setText(String.format("#%02X%02X%02X", red, green, blue))
            inputType = InputType.TYPE_CLASS_TEXT

            // Remove the bottom line (underline) from the EditText
            background = null
        }
    }

    private fun createSeekBarChangeListener(updateColorPreview: () -> Unit): SeekBar.OnSeekBarChangeListener {
        return object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateColorPreview()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }
    }
}