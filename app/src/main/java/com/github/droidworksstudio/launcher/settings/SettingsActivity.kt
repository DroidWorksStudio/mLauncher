package com.github.droidworksstudio.launcher.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.droidworksstudio.launcher.R
import com.github.droidworksstudio.launcher.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settingsLayout, SettingsFragment())
            .commit()
    }

}