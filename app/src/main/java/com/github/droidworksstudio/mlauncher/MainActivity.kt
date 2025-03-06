package com.github.droidworksstudio.mlauncher

// import android.content.pm.PackageManager
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.github.droidworksstudio.common.CrashHandler
import com.github.droidworksstudio.mlauncher.data.Constants
import com.github.droidworksstudio.mlauncher.data.Migration
import com.github.droidworksstudio.mlauncher.data.Prefs
import com.github.droidworksstudio.mlauncher.databinding.ActivityMainBinding
import com.github.droidworksstudio.mlauncher.helper.AppReloader
import com.github.droidworksstudio.mlauncher.helper.isTablet
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var migration: Migration
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
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        migration = Migration(this)

        // Initialize com.github.droidworksstudio.common.CrashHandler to catch uncaught exceptions
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(applicationContext))

        val themeMode = when (prefs.appTheme) {
            Constants.Theme.Light -> AppCompatDelegate.MODE_NIGHT_NO
            Constants.Theme.Dark -> AppCompatDelegate.MODE_NIGHT_YES
            Constants.Theme.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(themeMode)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        setLanguage()
        migration.migratePreferencesOnVersionUpdate(prefs)

        navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        if (prefs.firstOpen) {
            viewModel.firstOpen(true)
            prefs.firstOpen = false
        }

        initClickListeners()
        initObservers(viewModel)
        viewModel.getAppList(includeHiddenApps = true)
        setupOrientation()

        window.addFlags(FLAG_LAYOUT_NO_LIMITS)

        // Get the version and info of any app by passing app name. (maybe later used for Pro features if I want top release them for the play store)
        // pm = packageManager
        // val getAppVersionAndHash = AppDetailsHelper.getAppVersionAndHash(this, "app.mlauncher.debug", pm)
        // Log.d("isPremiumInstalled", getAppVersionAndHash.toString())
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
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
                            val prefs = Prefs(applicationContext).saveToString()
                            stream.channel.truncate(0)
                            stream.write(prefs.toByteArray())
                        }
                    }
                }
            }

            Constants.THEME_BACKUP_READ -> {
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
                        prefs.loadFromTheme(string)
                    }
                }
                startActivity(Intent.makeRestartActivityTask(this.intent?.component))
            }


            Constants.THEME_BACKUP_WRITE -> {
                // Step 1: Read the color names from theme.xml
                val colorNames = mutableListOf<String>()

                // Obtain an XmlPullParser for the theme.xml file
                applicationContext.resources.getXml(R.xml.theme).use { parser ->
                    while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                        if (parser.eventType == XmlPullParser.START_TAG && parser.name == "color") {
                            val colorName = parser.getAttributeValue(null, "colorName")
                            colorNames.add(colorName)
                        }
                        parser.next()
                    }
                }

                // Step 2: Back up the relevant preferences based on the extracted colorNames
                data?.data?.also { uri ->
                    applicationContext.contentResolver.openFileDescriptor(uri, "w")?.use { file ->
                        FileOutputStream(file.fileDescriptor).use { stream ->
                            // Get the filtered preferences (only those in the colorNames list)
                            val prefs = Prefs(applicationContext).saveToTheme(colorNames)
                            stream.channel.truncate(0)
                            stream.write(prefs.toByteArray())
                        }
                    }
                }
            }

            Constants.IMPORT_WORDS_OF_THE_DAY -> {
                data?.data?.let { uri ->
                    // Handle the imported file
                    val inputStream = contentResolver.openInputStream(uri)
                    val importedWords = readWordsFromFile(inputStream)
                    saveCustomWordList(importedWords)
                    AppReloader.restartApp(applicationContext)
                }
            }
        }
    }

    private fun readWordsFromFile(inputStream: InputStream?): List<String> {
        val words = mutableListOf<String>()

        // Make sure the input stream is not null
        inputStream?.let {
            // Read the input stream into a string
            val reader = InputStreamReader(it)

            // Use Gson to parse the JSON
            val gson = Gson()
            try {
                // Use TypeToken to specify the expected type (a list of strings)
                val type = object : TypeToken<Map<String, List<String>>>() {}.type
                val jsonMap: Map<String, List<String>> = gson.fromJson(reader, type)

                // Get the list of words from the "word_of_the_day" key
                words.addAll(jsonMap["word_of_the_day"] ?: emptyList())
            } catch (e: Exception) {
                e.printStackTrace() // Handle the error (e.g., logging)
            }
        }

        return words
    }


    private fun saveCustomWordList(words: List<String>) {
        val wordList = words.joinToString(";")
        prefs.wordList = wordList
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
        val locale = prefs.appLanguage.locale()
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

    private fun showMessage(message: String) {
        if (message.isEmpty()) return
        binding.messageTextView.text = message
        binding.messageLayout.visibility = View.VISIBLE
    }
}
