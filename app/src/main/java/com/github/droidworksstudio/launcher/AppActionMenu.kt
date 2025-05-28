package com.github.droidworksstudio.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import com.github.droidworksstudio.launcher.databinding.ActivityMainBinding
import com.github.droidworksstudio.launcher.settings.SharedPreferenceManager
import com.github.droidworksstudio.launcher.utils.Animations
import kotlinx.coroutines.launch

class AppActionMenu {

    fun setActionListeners(
        activity: MainActivity,
        binding: ActivityMainBinding,
        textView: TextView,
        editLayout: LinearLayout,
        actionMenu: View,
        searchView: EditText,
        appInfo: ApplicationInfo,
        userHandle: UserHandle,
        workProfile: Int,
        launcherApps: LauncherApps,
        appActivity: LauncherActivityInfo?
    ) {
        val animations = Animations(activity)
        val sharedPreferenceManager = SharedPreferenceManager(activity)

        actionMenu.findViewById<TextView>(R.id.info).setOnClickListener {

            // Launch app info in phone settings
            if (appActivity != null) {
                launcherApps.startAppDetailsActivity(
                    appActivity.componentName,
                    userHandle,
                    null,
                    null
                )
            }

            animations.fadeViewOut(actionMenu)
            textView.visibility = View.VISIBLE
        }

        actionMenu.findViewById<TextView>(R.id.uninstall).setOnClickListener {
            val intent = Intent(Intent.ACTION_DELETE)
            intent.data = Uri.parse("package:${appInfo.packageName}")
            intent.putExtra(Intent.EXTRA_USER, userHandle)
            activity.startActivity(intent)

            animations.fadeViewOut(actionMenu)

            textView.visibility = View.VISIBLE
        }

        actionMenu.findViewById<TextView>(R.id.rename).setOnClickListener {
            textView.visibility = View.INVISIBLE
            animations.fadeViewIn(editLayout)
            animations.fadeViewOut(actionMenu)
            val editText = editLayout.findViewById<EditText>(R.id.appNameEdit)
            val resetButton = editLayout.findViewById<AppCompatButton>(R.id.reset)

            val app = Triple(appActivity!!, userHandle, workProfile)

            searchView.visibility = View.INVISIBLE
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
                    textView.visibility = View.VISIBLE
                    searchView.visibility = View.VISIBLE
                }
            }

            editText.setOnEditorActionListener { _, actionId, _ ->

                // Once the new name is confirmed, close the keyboard, save the new app name and update the apps on screen
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    val imm =
                        activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(editText.windowToken, 0)
                    sharedPreferenceManager.setAppName(
                        appInfo.packageName,
                        workProfile,
                        editText.text.toString()
                    )
                    activity.lifecycleScope.launch {
                        activity.applySearch()
                    }


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
                    app.first.applicationInfo.packageName,
                    app.third
                )

                activity.lifecycleScope.launch {
                    activity.applySearch()
                }
            }
        }

        actionMenu.findViewById<TextView>(R.id.hide).setOnClickListener {
            editLayout.visibility = View.GONE
            textView.visibility = View.GONE
            actionMenu.visibility = View.GONE
            activity.lifecycleScope.launch {
                sharedPreferenceManager.setAppHidden(appInfo.packageName, workProfile, true)
                activity.refreshAppMenu()
            }
        }

        actionMenu.findViewById<TextView>(R.id.close).setOnClickListener {
            animations.fadeViewOut(actionMenu)
            textView.visibility = View.VISIBLE
        }
    }
}