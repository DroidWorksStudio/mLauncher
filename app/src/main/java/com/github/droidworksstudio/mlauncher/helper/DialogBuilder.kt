package com.github.droidworksstudio.mlauncher.helper

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.github.droidworksstudio.mlauncher.R
import com.github.droidworksstudio.mlauncher.data.Constants
import com.github.droidworksstudio.mlauncher.data.Prefs
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DialogBuilder(val context: Context, val activity: Activity) {

    private lateinit var prefs: Prefs

    var backupRestoreDialog: AlertDialog? = null

    fun showBackupRestoreDialog() {
        // Dismiss any existing dialog to prevent multiple dialogs open simultaneously
        backupRestoreDialog?.dismiss()

        // Define the items for the dialog (Backup, Restore, Clear Data)
        val items = arrayOf(
            context.getString(R.string.advanced_settings_backup_restore_backup),
            context.getString(R.string.advanced_settings_backup_restore_restore),
            context.getString(R.string.advanced_settings_backup_restore_clear)
        )

        val dialogBuilder = MaterialAlertDialogBuilder(context)
        dialogBuilder.setTitle(context.getString(R.string.advanced_settings_backup_restore_title))
        dialogBuilder.setItems(items) { _, which ->
            when (which) {
                0 -> storeFile(activity)
                1 -> loadFile(activity)
                else -> confirmClearData()
            }
        }

        // Assign the created dialog to backupRestoreDialog
        backupRestoreDialog = dialogBuilder.create()
        backupRestoreDialog?.show()
    }

    // Function to handle the Clear Data action, with a confirmation dialog
    private fun confirmClearData() {
        MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.advanced_settings_backup_restore_clear_title))
            .setMessage(context.getString(R.string.advanced_settings_backup_restore_clear_description))
            .setPositiveButton(context.getString(R.string.advanced_settings_backup_restore_clear_yes)) { _, _ ->
                clearData()
            }
            .setNegativeButton(context.getString(R.string.advanced_settings_backup_restore_clear_no), null)
            .show()
    }

    private fun clearData() {
        prefs = Prefs(context)
        prefs.clear()

        AppReloader.restartApp(context)
    }

    var sliderDialog: AlertDialog? = null

    fun showSliderDialog(
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

    var singleChoiceDialog: AlertDialog? = null

    fun <T> showSingleChoiceDialog(
        context: Context,
        options: Array<T>,
        titleResId: Int,
        fonts: List<Typeface>? = null, // Optional fonts
        fontSize: Float = 18f, // Default font size
        onItemSelected: (T) -> Unit
    ) {
        // Dismiss any existing dialog to prevent multiple dialogs from being open simultaneously
        singleChoiceDialog?.dismiss()

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
        val dialogBuilder = MaterialAlertDialogBuilder(context)
            .setView(dialogView)

        // Assign the created dialog to sliderDialog and show it
        singleChoiceDialog = dialogBuilder.create()
        singleChoiceDialog?.show()

        // Handle item selection (auto-close dialog)
        listView.setOnItemClickListener { _, _, position, _ ->
            onItemSelected(options[position]) // Callback with selected item
            singleChoiceDialog!!.dismiss() // Close dialog immediately
        }

        // Ensure the dialog width remains WRAP_CONTENT
        singleChoiceDialog!!.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        // Enforce max height (7 items max)
        listView.post {
            val itemHeight = listView.getChildAt(0)?.height ?: return@post
            val maxHeight = itemHeight * 7 // Max height for 7 items
            listView.layoutParams.height = maxHeight.coerceAtMost(itemHeight * options.size)
            listView.requestLayout()
        }
    }

    var colorPickerDialog: AlertDialog? = null

    fun showColorPickerDialog(
        context: Context,
        titleResId: Int,
        color: Int,
        onItemSelected: (Int) -> Unit // Callback to handle the selected color
    ) {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)

        var isUpdatingText = false

        val dialogBuilder = MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(titleResId))

        // Create SeekBars for Red, Green, and Blue
        val redSeekBar = createColorSeekBar(context, red)
        val greenSeekBar = createColorSeekBar(context, green)
        val blueSeekBar = createColorSeekBar(context, blue)

        // Create color preview box and RGB Hex input field
        val colorPreviewBox = createColorPreviewBox(context, color)
        val rgbText = createRgbTextField(context, red, green, blue)

        // Layout with SeekBars, Color Preview, and RGB Hex Text Input
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL

            // Create a horizontal layout for the text box and color preview
            val horizontalLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL // Vertically center the views

                // RGB Text field
                val rgbParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = 32 // Optional: Add margin between the text and the color box
                }
                rgbText.layoutParams = rgbParams
                addView(rgbText)

                // Color preview box
                val colorParams = LinearLayout.LayoutParams(150, 50).apply {
                    marginEnd = 32 // Optional: Add margin between the text and the color box
                }
                colorPreviewBox.layoutParams = colorParams
                addView(colorPreviewBox)
            }

            addView(redSeekBar)
            addView(greenSeekBar)
            addView(blueSeekBar)
            addView(horizontalLayout)
        }

        // Update color preview and text input when SeekBars are adjusted
        val updateColorPreview = {
            val updatedColor = Color.rgb(
                redSeekBar.progress, greenSeekBar.progress, blueSeekBar.progress
            )
            colorPreviewBox.setBackgroundColor(updatedColor)

            if (!isUpdatingText) {
                isUpdatingText = true
                rgbText.setText(
                    String.format(
                        "#%02X%02X%02X",
                        redSeekBar.progress, greenSeekBar.progress, blueSeekBar.progress
                    )
                )
                isUpdatingText = false
            }
        }

        // Listeners to update color preview when sliders are adjusted
        redSeekBar.setOnSeekBarChangeListener(createSeekBarChangeListener(updateColorPreview))
        greenSeekBar.setOnSeekBarChangeListener(createSeekBarChangeListener(updateColorPreview))
        blueSeekBar.setOnSeekBarChangeListener(createSeekBarChangeListener(updateColorPreview))

        // Listen for text input and update sliders and preview
        rgbText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                s?.toString()?.trim()?.let { colorString ->
                    if (colorString.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
                        val hexColor = Color.parseColor(colorString)
                        redSeekBar.progress = Color.red(hexColor)
                        greenSeekBar.progress = Color.green(hexColor)
                        blueSeekBar.progress = Color.blue(hexColor)
                        updateColorPreview()
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Set up the dialog view and buttons
        dialogBuilder.setView(layout)
        dialogBuilder.setPositiveButton("OK") { _, _ ->
            val pickedColor = Color.rgb(
                redSeekBar.progress, greenSeekBar.progress, blueSeekBar.progress
            )
            onItemSelected(pickedColor)
        }
        dialogBuilder.setNegativeButton("Cancel", null)

        // Show the dialog
        dialogBuilder.create().show()
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