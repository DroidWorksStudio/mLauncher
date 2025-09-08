package com.github.droidworksstudio.mlauncher.services

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Debug
import com.github.droidworksstudio.mlauncher.BuildConfig
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

object SecurityService {

    /**
     * Check if the app is built in debug mode OR running with debug options enabled.
     */
    fun isDebugBuild(context: Context): Boolean {
        return isBuildConfigDebug() ||
                isManifestDebuggable(context) ||
                isDebugSignature(context) ||
                isDebuggerAttached()
    }

    /** 1. BuildConfig.DEBUG flag */
    private fun isBuildConfigDebug(): Boolean {
        return BuildConfig.DEBUG
    }

    /** 2. Manifest flag android:debuggable */
    private fun isManifestDebuggable(context: Context): Boolean {
        return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    /** 3. Detect if a debugger is currently attached */
    private fun isDebuggerAttached(): Boolean {
        return Debug.isDebuggerConnected()
    }

    /** 4. Check signing certificate: Debug keystores contain "Android Debug" */
    private fun isDebugSignature(context: Context): Boolean {
        return try {
            val pm = context.packageManager
            val packageInfo =
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)

            val signatures =
                packageInfo.signingInfo?.apkContentsSigners

            if (signatures == null) return false

            val cf = CertificateFactory.getInstance("X.509")
            signatures.any { sig ->
                val cert = cf.generateCertificate(ByteArrayInputStream(sig.toByteArray())) as X509Certificate
                cert.subjectDN.name.contains("Android Debug")
            }
        } catch (_: Exception) {
            false
        }
    }
}