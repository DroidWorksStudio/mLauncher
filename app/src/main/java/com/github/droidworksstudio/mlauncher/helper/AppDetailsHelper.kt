package com.github.droidworksstudio.mlauncher.helper

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object AppDetailsHelper {

    data class AppDetails(
        val apkName: String,
        val apkVersionCode: Long,
        val apkVersionName: String,
        val apkHash: String
    )

    fun Context.isSystemApp(packageName: String): Boolean {
        if (packageName.isBlank()) return true
        return try {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            ((applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0)
                    || (applicationInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0))
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getAppVersionAndHash(context: Context, packageName: String, pm: PackageManager): AppDetails? {
        val pkgInstalled = context.isPackageInstalled(packageName)
        if (pkgInstalled) {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            }

            // Get version code and name
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            val versionName = packageInfo.versionName

            // Get APK hash
            val apkPath = packageInfo.applicationInfo.sourceDir
            val apkHash = computeHash(apkPath)
            return apkHash?.let { AppDetails(packageName, versionCode, versionName, it) }
        } else {
            return null
        }
    }

    private fun computeHash(apkPath: String): String? {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            val inputStream = FileInputStream(File(apkPath))
            val buffer = ByteArray(8192)
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
            val hashBytes = digest.digest()
            val sb = StringBuilder()
            for (b in hashBytes) {
                sb.append(String.format("%02x", b))
            }
            sb.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
