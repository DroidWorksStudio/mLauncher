package com.github.droidworksstudio.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat.getDrawable
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import com.github.droidworksstudio.launcher.databinding.ActivityMainBinding
import com.github.droidworksstudio.launcher.settings.SharedPreferenceManager
import com.github.droidworksstudio.launcher.utils.Animations
import kotlinx.coroutines.launch

class AppActionMenu(private val activity: MainActivity, private val binding: ActivityMainBinding, private val launcherApps: LauncherApps, private val searchView: EditText) {

    private val animations = Animations(activity)
    private val sharedPreferenceManager = SharedPreferenceManager(activity)

    fun setActionListeners(
        textView: TextView,
        editLayout: LinearLayout,
        actionMenu: View,
        appActivity: LauncherActivityInfo,
        userHandle: UserHandle,
        workProfile: Int
    ) {
        val pinButton = actionMenu.findViewById<TextView>(R.id.pin)
        val infoButton = actionMenu.findViewById<TextView>(R.id.info)
        val uninstallButton = actionMenu.findViewById<TextView>(R.id.uninstall)
        val renameButton = actionMenu.findViewById<TextView>(R.id.rename)
        val hideButton = actionMenu.findViewById<TextView>(R.id.hide)
        val closeButton = actionMenu.findViewById<TextView>(R.id.close)

        val enablePin = sharedPreferenceManager.isPinEnabled()
        val enableInfo = sharedPreferenceManager.isInfoEnabled()
        val enableUninstall = sharedPreferenceManager.isUninstallEnabled()
        val enableRename = sharedPreferenceManager.isRenameEnabled()
        val enableHide = sharedPreferenceManager.isHideEnabled()
        val enableClose = sharedPreferenceManager.isCloseEnabled()

        if (enablePin) {
            pinButton.visibility = View.VISIBLE
            setPinState(pinButton, appActivity, workProfile)

            ViewCompat.addAccessibilityAction(
                textView,
                activity.getString(R.string.accessibility_pin)
            ) { _, _ ->
                pinApp(appActivity, workProfile)
                true
            }

            pinButton.setOnClickListener {
                pinApp(appActivity, workProfile)
                animations.fadeViewOut(actionMenu)
                textView.visibility = View.VISIBLE
            }
        } else {
            pinButton.visibility = View.GONE
        }

        if (enableInfo) {
            infoButton.visibility = View.VISIBLE

            ViewCompat.addAccessibilityAction(
                textView,
                activity.getString(R.string.accessibility_info)
            ) { _, _ ->
                appInfo(appActivity, userHandle)
                true
            }

            infoButton.setOnClickListener {
                appInfo(appActivity, userHandle)
                animations.fadeViewOut(actionMenu)
                textView.visibility = View.VISIBLE
            }
        } else {
            infoButton.visibility = View.GONE
        }

        if (enableUninstall) {
            if (appActivity.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
                uninstallButton.visibility = View.VISIBLE
            }

            ViewCompat.addAccessibilityAction(
                textView,
                activity.getString(R.string.accessibility_uninstall)
            ) { _, _ ->
                uninstallApp(appActivity.applicationInfo, userHandle)
                true
            }

            uninstallButton.setOnClickListener {
                uninstallApp(appActivity.applicationInfo, userHandle)
                animations.fadeViewOut(actionMenu)
                textView.visibility = View.VISIBLE
            }
        } else {
            uninstallButton.visibility = View.GONE
        }

        if (enableRename) {
            renameButton.visibility = View.VISIBLE

            ViewCompat.addAccessibilityAction(
                textView,
                activity.getString(R.string.accessibility_rename)
            ) { _, _ ->
                renameApp(textView, editLayout, actionMenu, appActivity, userHandle, workProfile)
                true
            }

            renameButton.setOnClickListener {
                renameApp(textView, editLayout, actionMenu, appActivity, userHandle, workProfile)
            }
        } else {
            renameButton.visibility = View.GONE
        }

        if (enableHide) {
            hideButton.visibility = View.VISIBLE

            ViewCompat.addAccessibilityAction(
                textView,
                activity.getString(R.string.accessibility_hide)
            ) { _, _ ->
                hideApp(editLayout, textView, actionMenu, appActivity, workProfile)
                true
            }

            hideButton.setOnClickListener {
                hideApp(editLayout, textView, actionMenu, appActivity, workProfile)
            }
        } else {
            hideButton.visibility = View.GONE
        }

        if (enableClose) {
            closeButton.visibility = View.VISIBLE

            closeButton.setOnClickListener {
                animations.fadeViewOut(actionMenu)
                textView.visibility = View.VISIBLE
            }
        } else {
            closeButton.visibility = View.GONE
        }
    }

    private fun setPinState(button: TextView, appActivity: LauncherActivityInfo, workProfile: Int) {
        val isPinned = sharedPreferenceManager.isAppPinned(appActivity.componentName.flattenToString(), workProfile)
        val topDrawable = when (isPinned) {
            true -> getDrawable(activity, R.drawable.pin_off)
            false -> getDrawable(activity, R.drawable.pin)
        }

        button.setCompoundDrawablesWithIntrinsicBounds(null, topDrawable, null, null)

        val pinLabel = when (isPinned) {
            true -> "Unpin"
            false -> "Pin"
        }

        button.text = pinLabel
    }

    private fun pinApp(appActivity: LauncherActivityInfo, workProfile: Int) {
        sharedPreferenceManager.setPinnedApp(appActivity.componentName.flattenToString(), workProfile)
    }

    private fun appInfo(
        appActivity: LauncherActivityInfo?,
        userHandle: UserHandle
    ) {
        // Launch app info in phone settings
        if (appActivity != null) {
            launcherApps.startAppDetailsActivity(
                appActivity.componentName,
                userHandle,
                null,
                null
            )
        }
    }

    private fun uninstallApp(appInfo: ApplicationInfo, userHandle: UserHandle) {
        val intent = Intent(Intent.ACTION_DELETE)
        intent.data = "package:${appInfo.packageName}".toUri()
        intent.putExtra(Intent.EXTRA_USER, userHandle)
        activity.startActivity(intent)
        activity.returnAllowed = false
    }

    private fun renameApp(textView: TextView, editLayout: LinearLayout, actionMenu: View, appActivity: LauncherActivityInfo, userHandle: UserHandle, workProfile: Int) {
        activity.disableAppMenuScroll()
        textView.visibility = View.INVISIBLE
        animations.fadeViewIn(editLayout)
        animations.fadeViewOut(actionMenu)
        val editText = editLayout.findViewById<EditText>(R.id.appNameEdit)
        val resetButton = editLayout.findViewById<AppCompatButton>(R.id.reset)

        val app = Triple(appActivity, userHandle, workProfile)

        val searchEnabled = sharedPreferenceManager.isSearchEnabled()

        if (searchEnabled) {
            searchView.visibility = View.INVISIBLE
        } else {
            searchView.visibility = View.GONE
        }

        editText.requestFocus()

        // Open keyboard
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            val imm =
                activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        }, 100)

        binding.root.addOnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->

            // If the keyboard is closed, exit editing mode
            if (bottom - top > oldBottom - oldTop) {
                editLayout.clearFocus()

                animations.fadeViewOut(editLayout)
                animations.fadeViewIn(textView)
                if (searchEnabled) {
                    searchView.visibility = View.VISIBLE
                } else {
                    searchView.visibility = View.GONE
                }
                activity.enableAppMenuScroll()
            }
        }

        editText.setOnEditorActionListener { _, actionId, _ ->

            // Once the new name is confirmed, close the keyboard, save the new app name and update the apps on screen
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (editText.text.isNullOrBlank()) {
                    Toast.makeText(activity, activity.getString(R.string.empty_rename), Toast.LENGTH_SHORT).show()
                    return@setOnEditorActionListener true
                }
                val imm =
                    activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(editText.windowToken, 0)
                sharedPreferenceManager.setAppName(
                    app.first.componentName.flattenToString(),
                    workProfile,
                    editText.text.toString()
                )
                activity.lifecycleScope.launch {
                    activity.applySearch()
                }
                activity.enableAppMenuScroll()

                return@setOnEditorActionListener true
            }
            false
        }

        resetButton.setOnClickListener {

            // If reset is pressed, close keyboard, remove saved edited name and update the apps on screen
            val imm =
                activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(editLayout.windowToken, 0)
            sharedPreferenceManager.resetAppName(
                app.first.componentName.flattenToString(),
                app.third
            )

            activity.lifecycleScope.launch {
                activity.applySearch()
            }
        }
    }

    private fun hideApp(editLayout: LinearLayout, textView: TextView, actionMenu: View, appActivity: LauncherActivityInfo, workProfile: Int) {
        editLayout.visibility = View.GONE
        textView.visibility = View.GONE
        actionMenu.visibility = View.GONE
        activity.lifecycleScope.launch {
            sharedPreferenceManager.setAppHidden(appActivity.componentName.flattenToString(), workProfile, true)
            activity.refreshAppMenu()
        }
    }
}