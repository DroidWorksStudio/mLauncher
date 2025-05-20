package com.github.droidworksstudio.mlauncher.ui.onboarding

import android.os.Bundle
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import androidx.appcompat.app.AppCompatActivity
import com.github.droidworksstudio.common.CrashHandler
import com.github.droidworksstudio.mlauncher.R

class OnboardingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize com.github.droidworksstudio.common.CrashHandler to catch uncaught exceptions
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(applicationContext))

        setContentView(R.layout.activity_onboarding)

        // Load the OnboardingFragment dynamically
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, OnboardingFragment())  // FragmentContainer is a FrameLayout or other container in the activity layout
            .commit()

        window.addFlags(FLAG_LAYOUT_NO_LIMITS)
    }
}


