package com.github.droidworksstudio.mlauncher.helper

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import com.github.droidworksstudio.common.showLongToast
import com.github.droidworksstudio.mlauncher.MainViewModel
import com.github.droidworksstudio.mlauncher.R

private lateinit var viewModel: MainViewModel

/*
 * Checks whether the device supports private space.
 */
fun isPrivateSpaceSupported(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM
}

fun getPrivateSpaceUser(context: Context): UserHandle? {
    if (!isPrivateSpaceSupported()) {
        return null
    }
    val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
    val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    return userManager.userProfiles.firstOrNull { u ->
        launcherApps.getLauncherUserInfo(u)?.userType == UserManager.USER_TYPE_PROFILE_PRIVATE
    }
}

/**
 * Check if the given user profile is the private space.
 */
fun isPrivateSpaceProfile(context: Context, userHandle: UserHandle): Boolean {
    if (!isPrivateSpaceSupported()) {
        return false
    }

    val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    try {
        val userType = launcherApps.getLauncherUserInfo(userHandle)?.userType
        return userType == UserManager.USER_TYPE_PROFILE_PRIVATE
    } catch (e: Exception) {
        Log.e("PrivateSpace", "Failed to retrieve launcher user info", e)
        return false
    }
}


/**
 * Check whether the user has created a private space and whether mLauncher can access it.
 */
fun isPrivateSpaceSetUp(
    context: Context,
    showToast: Boolean = false,
    launchSettings: Boolean = false
): Boolean {
    if (!isPrivateSpaceSupported()) {
        if (showToast) {
            context.showLongToast(context.getString(R.string.alert_requires_android_v))
        }
        return false
    }
    val privateSpaceUser = getPrivateSpaceUser(context)
    if (privateSpaceUser != null) {
        return true
    }
    if (!ismlauncherDefault(context)) {
        if (showToast) {
            context.showLongToast(context.getString(R.string.toast_private_space_default_home_screen))
        }
        if (launchSettings) {
            viewModel.resetDefaultLauncherApp(context)
        }
    } else {
        if (showToast) {
            context.showLongToast(context.getString(R.string.toast_private_space_not_available))
        }
        if (launchSettings) {
            try {
                viewModel.resetDefaultLauncherApp(context)
            } catch (_: ActivityNotFoundException) {
            }
        }
    }
    return false
}

fun isPrivateSpaceLocked(context: Context): Boolean {
    if (!isPrivateSpaceSupported()) {
        return false
    }
    val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
    val privateSpaceUser = getPrivateSpaceUser(context) ?: return false
    return userManager.isQuietModeEnabled(privateSpaceUser)
}

fun lockPrivateSpace(context: Context, lock: Boolean) {
    if (!isPrivateSpaceSupported()) {
        return
    }
    val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
    val privateSpaceUser = getPrivateSpaceUser(context) ?: return
    userManager.requestQuietModeEnabled(lock, privateSpaceUser)
}

fun togglePrivateSpaceLock(context: Context) {
    if (!isPrivateSpaceSetUp(context, showToast = true, launchSettings = true)) {
        return
    }
    if (!ismlauncherDefault(context)) {
        context.showLongToast(context.getString(R.string.toast_private_space_default_home_screen))
        return
    }

    val lock = isPrivateSpaceLocked(context)
    lockPrivateSpace(context, !lock)

    Handler(Looper.getMainLooper()).post {
        if (isPrivateSpaceLocked(context) == !lock) {
            context.showLongToast(context.getString(R.string.toast_private_space_locked))
        }
    }
}