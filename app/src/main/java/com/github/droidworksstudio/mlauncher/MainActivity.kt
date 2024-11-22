package com.github.droidworksstudio.mlauncher

// import android.content.pm.PackageManager
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.github.droidworksstudio.common.showLongToast
import com.github.droidworksstudio.mlauncher.data.Constants
import com.github.droidworksstudio.mlauncher.data.Prefs
import com.github.droidworksstudio.mlauncher.databinding.ActivityMainBinding
import com.github.droidworksstudio.mlauncher.helper.hasUsagePermission
import com.github.droidworksstudio.mlauncher.helper.isTablet
import com.github.droidworksstudio.mlauncher.helper.showPermissionDialog
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var navController: NavController
    private lateinit var viewModel: MainViewModel
    private lateinit var binding: ActivityMainBinding
    // private lateinit var pm: PackageManager

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        @Suppress("DEPRECATION")
        if (navController.currentDestination?.id != R.id.mainFragment)
            super.onBackPressed()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_MENU -> {
                when (navController.currentDestination?.id) {
                    R.id.mainFragment -> {
                        Navigation.findNavController(this, R.id.nav_host_fragment)
                            .navigate(R.id.action_mainFragment_to_appListFragment)
                        true
                    }

                    else -> {
                        false
                    }
                }
            }

            KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_B, KeyEvent.KEYCODE_C, KeyEvent.KEYCODE_D,
            KeyEvent.KEYCODE_E, KeyEvent.KEYCODE_F, KeyEvent.KEYCODE_G, KeyEvent.KEYCODE_H,
            KeyEvent.KEYCODE_I, KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_K, KeyEvent.KEYCODE_L,
            KeyEvent.KEYCODE_M, KeyEvent.KEYCODE_N, KeyEvent.KEYCODE_O, KeyEvent.KEYCODE_P,
            KeyEvent.KEYCODE_Q, KeyEvent.KEYCODE_R, KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_T,
            KeyEvent.KEYCODE_U, KeyEvent.KEYCODE_V, KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_X,
            KeyEvent.KEYCODE_Y, KeyEvent.KEYCODE_Z -> {
                when (navController.currentDestination?.id) {
                    R.id.mainFragment -> {
                        val bundle = Bundle()
                        bundle.putInt("letterKeyCode", keyCode) // Pass the letter key code
                        Navigation.findNavController(this, R.id.nav_host_fragment)
                            .navigate(R.id.action_mainFragment_to_appListFragment, bundle)
                        true
                    }

                    else -> {
                        false
                    }
                }
            }

            KeyEvent.KEYCODE_ESCAPE -> {
                backToHomeScreen()
                true
            }

            else -> {
                super.onKeyDown(keyCode, event)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = Prefs(this)
        val themeMode = when (prefs.appTheme) {
            Constants.Theme.Light -> AppCompatDelegate.MODE_NIGHT_NO
            Constants.Theme.Dark -> AppCompatDelegate.MODE_NIGHT_YES
            Constants.Theme.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(themeMode)

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setLanguage()

        navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        if (prefs.firstOpen) {
            viewModel.firstOpen(true)
            prefs.firstOpen = false
        }

        initClickListeners()
        initObservers(viewModel)
        viewModel.getAppList(includeHiddenApps = prefs.hiddenAppsDisplayed)
        setupOrientation()

        window.addFlags(FLAG_LAYOUT_NO_LIMITS)

        // Get the version and info of any app by passing app name. (maybe later used for Pro features if I want top release them for the play store)
        // pm = packageManager
        // val getAppVersionAndHash = AppDetailsHelper.getAppVersionAndHash(this, "app.olauncher.debug", pm)
        // Log.d("isPremiumInstalled", getAppVersionAndHash.toString())

        if (prefs.recentAppsDisplayed || prefs.appUsageStats) {
            // Check if the usage permission is not granted
            if (!hasUsagePermission(this)) {
                // Postpone showing the dialog until the activity is running
                Handler(Looper.getMainLooper()).post {
                    // Check if the activity is still running before showing the dialog
                    if (!isFinishing) {
                        // Instantiate MainActivity and pass it to showPermissionDialog
                        showPermissionDialog(this)
                    }
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != RESULT_OK) {
            showLongToast("Intent Error")
            return
        }

        when (requestCode) {
            Constants.REQUEST_CODE_ENABLE_ADMIN -> {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P)
                    showMessage(getString(R.string.double_tap_lock_is_enabled_message))
                else
                    showMessage(getString(R.string.double_tap_lock_uninstall_message))
            }

            Constants.BACKUP_READ -> {
                data?.data?.also { uri ->
                    applicationContext.contentResolver.openInputStream(uri).use { inputStream ->
                        val stringBuilder = StringBuilder()
                        BufferedReader(InputStreamReader(inputStream)).use { reader ->
                            var line: String? = reader.readLine()
                            while (line != null) {
                                stringBuilder.append(line)
                                line = reader.readLine()
                            }
                        }

                        val string = stringBuilder.toString()
                        val prefs = Prefs(applicationContext)
                        prefs.clear()
                        prefs.loadFromString(string)
                    }
                }
                startActivity(Intent.makeRestartActivityTask(this.intent?.component))
            }

            Constants.BACKUP_WRITE -> {
                data?.data?.also { uri ->
                    applicationContext.contentResolver.openFileDescriptor(uri, "w")?.use { file ->
                        FileOutputStream(file.fileDescriptor).use { stream ->
                            val text = Prefs(applicationContext).saveToString()
                            stream.channel.truncate(0)
                            stream.write(text.toByteArray())
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        backToHomeScreen()
        super.onStop()
    }

    override fun onUserLeaveHint() {
        backToHomeScreen()
        super.onUserLeaveHint()
    }

    override fun onResume() {
        backToHomeScreen()
        super.onResume()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        recreate()
    }

    @Suppress("DEPRECATION")
    private fun setLanguage() {
        val locale = prefs.language.locale()
        val config = resources.configuration
        config.locale = locale
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun initClickListeners() {
        binding.okay.setOnClickListener {
            binding.messageLayout.visibility = View.GONE
            viewModel.showMessageDialog("")
        }
    }

    private fun initObservers(viewModel: MainViewModel) {
        viewModel.launcherResetFailed.observe(this) {
            openLauncherChooser(it)
        }
        viewModel.showMessageDialog.observe(this) {
            showMessage(it)
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun setupOrientation() {
        if (isTablet(this)) return
        // In Android 8.0, windowIsTranslucent cannot be used with screenOrientation=portrait
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.O)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    private fun backToHomeScreen() {
        // Whenever home button is pressed or user leaves the launcher,
        // pop all the fragments except main
        if (navController.currentDestination?.id != R.id.mainFragment)
            navController.popBackStack(R.id.mainFragment, false)
    }

    private fun openLauncherChooser(resetFailed: Boolean) {
        if (resetFailed) {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS) else {
                showLongToast(
                    "Search for launcher or home app"
                )
                Intent(Settings.ACTION_SETTINGS)
            }
            startActivity(intent)
        }
    }

    private fun showMessage(message: String) {
        if (message.isEmpty()) return
        binding.messageTextView.text = message
        binding.messageLayout.visibility = View.VISIBLE
    }
}
