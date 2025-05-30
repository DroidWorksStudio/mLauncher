package com.github.droidworksstudio.mlauncher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.droidworksstudio.common.CrashHandler
import com.github.droidworksstudio.common.getLocalizedString
import com.github.droidworksstudio.mlauncher.helper.emptyString
import com.github.droidworksstudio.mlauncher.helper.getDeviceInfo
import com.github.droidworksstudio.mlauncher.helper.utils.SimpleEmailSender
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class CrashReportActivity : AppCompatActivity() {
    private var pkgName: String = emptyString()
    private var pkgVersion: String = emptyString()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pkgName = getLocalizedString(R.string.app_name)
        pkgVersion = this.packageManager.getPackageInfo(
            this.packageName,
            0
        ).versionName.toString()

        // Show a dialog to ask if the user wants to report the crash
        MaterialAlertDialogBuilder(this)
            .setTitle(getLocalizedString(R.string.acra_crash))
            .setMessage(getLocalizedString(R.string.acra_dialog_text).format(pkgName))
            .setPositiveButton(getLocalizedString(R.string.acra_send_report)) { _, _ ->
                sendCrashReport(this)
            }
            .setNegativeButton(getLocalizedString(R.string.acra_dont_send)) { _, _ ->
                restartApp()
            }
            .setCancelable(false)
            .show()
    }

    private fun sendCrashReport(context: Context) {
        val crashFileUri: Uri? = CrashHandler.customReportSender(applicationContext)
        val crashFileUris: List<Uri> = crashFileUri?.let { listOf(it) } ?: emptyList()

        val emailSender = SimpleEmailSender() // Create an instance
        val deviceInfo = getDeviceInfo(context)
        val crashReportContent = getLocalizedString(R.string.acra_mail_body, deviceInfo)
        val subject = String.format("Crash Report %s - %s", pkgName, pkgVersion)
        val recipient = getLocalizedString(R.string.acra_email) // Replace with your email

        emailSender.sendCrashReport(context, crashReportContent, crashFileUris, subject, recipient)
    }


    private fun restartApp() {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        finish()
    }
}

