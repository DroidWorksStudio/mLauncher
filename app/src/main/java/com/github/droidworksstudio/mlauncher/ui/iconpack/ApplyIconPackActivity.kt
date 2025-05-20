package com.github.droidworksstudio.mlauncher.ui.iconpack

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.github.droidworksstudio.common.getLocalizedString
import com.github.droidworksstudio.mlauncher.MainViewModel
import com.github.droidworksstudio.mlauncher.R
import com.github.droidworksstudio.mlauncher.data.Constants
import com.github.droidworksstudio.mlauncher.data.Prefs
import com.github.droidworksstudio.mlauncher.helper.IconPackHelper
import com.github.droidworksstudio.mlauncher.helper.utils.AppReloader
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ApplyIconPackActivity : androidx.appcompat.app.AppCompatActivity() {
    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        val packageName = intent.getStringExtra("packageName").toString()
        val packageClass = intent.getStringExtra("packageClass").toString()
        if (packageClass.isNotEmpty()) {
            MaterialAlertDialogBuilder(this)
                .setTitle(getLocalizedString(R.string.apply_icon_pack))
                .setMessage(getLocalizedString(R.string.apply_icon_pack_are_you_sure, packageName))
                .setPositiveButton(getLocalizedString(R.string.apply)) { _, _ ->

                    val iconPackType = Constants.IconPacks.Custom
                    val customIconPackType = packageClass

                    IconPackHelper.preloadIcons(this, customIconPackType)

                    prefs.iconPack = iconPackType
                    viewModel.iconPack.value = iconPackType
                    prefs.customIconPack = customIconPackType
                    viewModel.customIconPack.value = customIconPackType

                    AppReloader.restartApp(this)
                }
                .setNegativeButton(getLocalizedString(R.string.cancel)) { _, _ ->
                    finish()
                }
                .setCancelable(false)
                .show()
        } else {
            finish()
        }
    }
}

