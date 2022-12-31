package com.github.hecodes2much.mlauncher.helper

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.hecodes2much.mlauncher.R

class FakeHomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fake_home)
    }
}