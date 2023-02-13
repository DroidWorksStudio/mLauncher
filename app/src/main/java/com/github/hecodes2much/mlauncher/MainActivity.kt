package com.github.hecodes2much.mlauncher

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.github.hecodes2much.mlauncher.data.Constants
import com.github.hecodes2much.mlauncher.data.Prefs
import com.github.hecodes2much.mlauncher.databinding.ActivityMainBinding
import com.github.hecodes2much.mlauncher.helper.BatteryReceiver
import com.github.hecodes2much.mlauncher.helper.isTablet
import com.github.hecodes2much.mlauncher.helper.showToastLong
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var navController: NavController
    private lateinit var viewModel: MainViewModel
    private lateinit var batteryReceiver: BatteryReceiver
    private lateinit var binding: ActivityMainBinding

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        @Suppress("DEPRECATION")
        if (navController.currentDestination?.id != R.id.mainFragment)
            super.onBackPressed()
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

        batteryReceiver = BatteryReceiver()
        /* register battery changes */
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, filter)

        setLanguage()

        navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        if (prefs.firstOpen) {
            viewModel.firstOpen(true)
            prefs.firstOpen = false
        }

        initClickListeners()
        initObservers(viewModel)
        viewModel.getAppList()
        setupOrientation()

        window.addFlags(FLAG_LAYOUT_NO_LIMITS)
    }

    override fun onDestroy() {
        super.onDestroy()
        batteryReceiver = BatteryReceiver()
        /* unregister battery changes */
        unregisterReceiver(batteryReceiver)
    }

    override fun onPause() {
        super.onPause()
        batteryReceiver = BatteryReceiver()
        /* unregister battery changes */
        unregisterReceiver(batteryReceiver)
    }

    @Suppress("DEPRECATION")
    fun setLanguage() {
        val locale = prefs.language.locale()
        val config = resources.configuration
        config.locale = locale
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    override fun onStop() {
        backToHomeScreen()
        super.onStop()
    }

    override fun onUserLeaveHint() {
        backToHomeScreen()
        super.onUserLeaveHint()
    }

    override fun onNewIntent(intent: Intent?) {
        backToHomeScreen()
        super.onNewIntent(intent)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        recreate()
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
                showToastLong(
                    this,
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

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) {
            // showToastLong(applicationContext, "Intent Error")
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
}
