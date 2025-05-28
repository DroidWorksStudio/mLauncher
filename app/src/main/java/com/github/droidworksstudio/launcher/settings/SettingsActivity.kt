package com.github.droidworksstudio.launcher.settings

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.github.droidworksstudio.launcher.MainActivity
import com.github.droidworksstudio.launcher.R
import com.github.droidworksstudio.launcher.databinding.ActivitySettingsBinding
import com.github.droidworksstudio.launcher.utils.PermissionUtils
import org.json.JSONObject

class SettingsActivity : AppCompatActivity() {

    private val permissionUtils = PermissionUtils()

    private lateinit var sharedPreferenceManager: SharedPreferenceManager
    private lateinit var preferences: SharedPreferences
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var performBackup: ActivityResultLauncher<Intent>
    private lateinit var performRestore: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferenceManager = SharedPreferenceManager(this@SettingsActivity)

        preferences = PreferenceManager.getDefaultSharedPreferences(this@SettingsActivity)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings_title)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        if (supportFragmentManager.backStackEntryCount == 0) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settingsLayout, SettingsFragment())
                .commit()
        }

        supportFragmentManager.addOnBackStackChangedListener {
            updateActionBarTitle()
        }

        performBackup = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    saveSharedPreferencesToFile(uri)
                }
            }
        }

        performRestore = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    restoreSharedPreferencesFromFile(uri)
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            onBackPressedDispatcher.onBackPressed()
        }
        return true
    }

    private fun updateActionBarTitle() {
        val fragment = supportFragmentManager.findFragmentById(R.id.settingsLayout)
        if (fragment is TitleProvider) {
            supportActionBar?.title = fragment.getTitle()
        }
    }

    fun createBackup() {
        val createFileIntent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "launcher_backup.json")
        }
        performBackup.launch(createFileIntent)
    }

    private fun saveSharedPreferencesToFile(uri: Uri) {
        val allEntries = preferences.all

        val backupData = JSONObject().apply {
            put("app_id", application.packageName)
            val data = JSONObject()
            for ((key, value) in allEntries) {
                val entry = JSONObject().apply {
                    when (value) {
                        is String -> put("value", value).put("type", "String")
                        is Int -> put("value", value).put("type", "Int")
                        is Boolean -> put("value", value).put("type", "Boolean")
                        is Long -> put("value", value).put("type", "Long")
                        is Float -> put("value", value).put("type", "Float")
                    }
                }
                data.put(key, entry)
            }
            put("data", data)
        }

        val sharedPreferencesText = backupData.toString(4)

        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(sharedPreferencesText.toByteArray())
            }
            Toast.makeText(this, getString(R.string.backup_success), Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(this, getString(R.string.backup_fail), Toast.LENGTH_SHORT).show()
        }
    }

    fun restoreBackup() {
        val openFileIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        performRestore.launch(openFileIntent)
    }

    private fun restoreSharedPreferencesFromFile(uri: Uri) {
        val jsonData = readJsonFile(uri)
        if (jsonData != null) {
            try {
                val backupData = JSONObject(jsonData)
                if (backupData.getString("app_id") != application.packageName) {
                    throw IllegalArgumentException(getString(R.string.restore_wrong_app))
                }
                val data = backupData.getJSONObject("data")

                preferences.edit {

                    val keys = data.keys()

                    while (keys.hasNext()) {
                        val key = keys.next()
                        val entry = data.getJSONObject(key)
                        val type = entry.getString("type")

                        when (type) {
                            "String" -> putString(key, entry.getString("value"))
                            "Int" -> putInt(key, entry.getInt("value"))
                            "Boolean" -> putBoolean(key, entry.getBoolean("value"))
                            "Long" -> putLong(key, entry.getLong("value"))
                            "Float" -> putFloat(key, entry.getDouble("value").toFloat())
                        }
                    }
                    putBoolean("isRestored", true)

                }

                Toast.makeText(this, getString(R.string.restore_success), Toast.LENGTH_SHORT).show()
            } catch (e: IllegalArgumentException) {
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(this, getString(R.string.restore_fail), Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, getString(R.string.restore_error), Toast.LENGTH_SHORT).show()
        }
    }

    private fun readJsonFile(uri: Uri): String? {
        return try {
            contentResolver.openInputStream(uri)?.bufferedReader().use { reader ->
                reader?.readText()
            }
        } catch (_: Exception) {
            null
        }
    }

    fun requestLocationPermission() {
        try {
            ActivityCompat.requestPermissions(
                this@SettingsActivity,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                0
            )
        } catch (_: Exception) {
        }
    }

    fun requestContactsPermission() {
        try {
            ActivityCompat.requestPermissions(
                this@SettingsActivity,
                arrayOf(Manifest.permission.READ_CONTACTS),
                1
            )
        } catch (_: Exception) {
        }
    }

    fun restartApp() {
        val restartIntent = Intent(applicationContext, MainActivity::class.java)
        restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, restartIntent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        pendingIntent.send()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)


        if (requestCode == 0) {
            val fragment = supportFragmentManager.findFragmentById(R.id.settingsLayout) as HomeSettingsFragment
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fragment.setLocationPreference(true)
            } else {
                Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show()
                fragment.setLocationPreference(false)
            }
        }

        if (requestCode == 1) {
            val fragment = supportFragmentManager.findFragmentById(R.id.settingsLayout) as AppMenuSettingsFragment
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fragment.setContactPreference(true)
            } else {
                Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show()
                fragment.setContactPreference(false)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!permissionUtils.hasPermission(this@SettingsActivity, Manifest.permission.READ_CONTACTS)) {
            sharedPreferenceManager.setContactsEnabled(false)
        }
        if (!permissionUtils.hasPermission(this@SettingsActivity, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            sharedPreferenceManager.setWeatherGPS(false)
        }
    }

}