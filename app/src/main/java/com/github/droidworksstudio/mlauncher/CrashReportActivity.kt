package com.github.droidworksstudio.mlauncher

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.droidworksstudio.common.CrashHandler
import com.github.droidworksstudio.mlauncher.helper.utils.SimpleEmailSender

class CrashReportActivity : AppCompatActivity() {
    private var pkgName: String = ""
    private var pkgVersion: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pkgName = getString(R.string.app_name)
        pkgVersion = this.packageManager.getPackageInfo(
            this.packageName,
            0
        ).versionName.toString()

        // Show a dialog to ask if the user wants to report the crash
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.acra_crash))
            .setMessage(getString(R.string.acra_dialog_text).format(pkgName))
            .setPositiveButton(getString(R.string.acra_send_report)) { _, _ ->
                sendCrashReport(this)
            }
            .setNegativeButton(getString(R.string.acra_dont_send)) { _, _ ->
                restartApp()
            }
            .setCancelable(false)
            .show()
    }

    private fun sendCrashReport(context: Context) {
        val crashFileUri: Uri? = CrashHandler.customReportSender(applicationContext)
        val crashFileUris: List<Uri> = crashFileUri?.let { listOf(it) } ?: emptyList()

        val emailSender = SimpleEmailSender() // Create an instance
        val crashReportContent = getString(R.string.acra_mail_body)
        val subject = "$pkgName $pkgVersion Crash Report"
        val recipient = getString(R.string.acra_email) // Replace with your email

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

