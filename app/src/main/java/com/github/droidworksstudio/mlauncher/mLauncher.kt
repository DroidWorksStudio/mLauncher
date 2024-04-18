package com.github.droidworksstudio.mlauncher

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import com.github.droidworksstudio.mlauncher.data.Prefs
import org.acra.config.dialog
import org.acra.config.mailSender
import org.acra.config.notification
import org.acra.data.StringFormat
import org.acra.ktx.initAcra

class Mlauncher : Application() {
    private lateinit var prefs: Prefs

    override fun onCreate() {
        super.onCreate()

        prefs = Prefs(baseContext)
        createNotificationChannel()

        // Check if notifications are enabled
        if (!areNotificationsEnabled() && prefs.enableNotifications ) {
            // Prompt the user to enable notifications
            showNotificationSettings()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = applicationInfo.uid.toString()
            val channelName = getString(R.string.acra_send_report)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                name = channelName
            }

            val notificationManager: NotificationManager =
                getSystemService(NotificationManager::class.java)
                    ?: return

            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun areNotificationsEnabled(): Boolean {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Check if notifications are enabled
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.areNotificationsEnabled()
        } else {
            true
        }
    }

    private fun showNotificationSettings() {
        val intent = Intent()
        intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)
        } else {
            intent.putExtra("app_package", packageName)
            intent.putExtra("app_uid", applicationInfo.uid)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        val pkgName = getString(R.string.app_name)
        val pkgVersion = base.packageManager.getPackageInfo(
            base.packageName,
            0
        ).versionName

        initAcra {
            //core configuration:
            buildConfigClass = BuildConfig::class.java
            reportFormat = StringFormat.KEY_VALUE_LIST
            //each plugin you chose above can be configured in a block like this:
            if (prefs.enableNotifications) {
                notification {
                    //required
                    title = getString(R.string.acra_dialog_text).format(pkgName)
                    //required
                    text = getString(R.string.acra_crash)
                    //required
                    channelName = getString(R.string.acra_send_report)
                    //defaults to android.R.string.ok
                    sendButtonText = getString(R.string.acra_send_report)
                    //defaults to android.R.string.cancel
                    discardButtonText = getString(R.string.acra_dont_send)
                    //defaults to false
                    sendOnClick = true
                }
            } else {
                dialog {
                    //required
                    text = getString(R.string.acra_dialog_text).format(pkgName)
                    //optional, enables the dialog title
                    title = getString(R.string.acra_crash)
                    //defaults to android.R.string.ok
                    positiveButtonText = getString(R.string.acra_send_report)
                    //defaults to android.R.string.cancel
                    negativeButtonText = getString(R.string.acra_dont_send)
                    //optional, defaults to @android:style/Theme.Dialog
                    resTheme = R.style.MaterialDialogTheme
                }
            }

            mailSender {
                //required
                mailTo = "wayne6324@gmail.com"
                //defaults to true
                reportAsFile = true
                //defaults to ACRA-report.stacktrace
                reportFileName = "$pkgName-crash-report.stacktrace"
                //defaults to "<applicationId> Crash Report"
                subject = "$pkgName $pkgVersion Crash Report"
                //defaults to empty
                body = getString(R.string.acra_mail_body)
            }
        }
    }
}
