package com.github.droidworksstudio.mlauncher

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.droidworksstudio.mlauncher.data.AppListItem
import com.github.droidworksstudio.mlauncher.data.Constants
import com.github.droidworksstudio.mlauncher.data.Constants.AppDrawerFlag
import com.github.droidworksstudio.mlauncher.data.Prefs
import com.github.droidworksstudio.mlauncher.helper.AppUsageTracker
import com.github.droidworksstudio.mlauncher.helper.getAppsList
import com.github.droidworksstudio.mlauncher.helper.getDefaultLauncherPackage
import com.github.droidworksstudio.mlauncher.helper.ismlauncherDefault
import com.github.droidworksstudio.mlauncher.helper.resetDefaultLauncher
import com.github.droidworksstudio.mlauncher.helper.showToastShort
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext by lazy { application.applicationContext }
    private val prefs = Prefs(appContext)

    // setup variables with initial values
    val firstOpen = MutableLiveData<Boolean>()
    val showMessageDialog = MutableLiveData<String>()

    val appList = MutableLiveData<List<AppListItem>?>() // TODO why maybe?
    val hiddenApps = MutableLiveData<List<AppListItem>?>()
    private val launcherDefault = MutableLiveData<Boolean>()
    val launcherResetFailed = MutableLiveData<Boolean>()

    val showTime = MutableLiveData(prefs.showTime)
    val showDate = MutableLiveData(prefs.showDate)
    val clockAlignment = MutableLiveData(prefs.clockAlignment)
    val homeAppsAlignment = MutableLiveData(Pair(prefs.homeAlignment, prefs.homeAlignmentBottom))
    val homeAppsCount = MutableLiveData(prefs.homeAppsNum)
    val homePagesCount = MutableLiveData(prefs.homePagesNum)
    val opacityNum = MutableLiveData(prefs.opacityNum)
    val filterStrength = MutableLiveData(prefs.filterStrength)
    val recentCounter = MutableLiveData(prefs.recentCounter)

    fun selectedApp(app: AppListItem, flag: AppDrawerFlag, n: Int = 0) {
        when (flag) {
            AppDrawerFlag.LaunchApp, AppDrawerFlag.HiddenApps -> {
                launchApp(app)
            }

            AppDrawerFlag.SetHomeApp, AppDrawerFlag.ReorderApps -> {
                prefs.setHomeAppModel(n, app)
            }

            AppDrawerFlag.SetShortSwipeUp -> prefs.appShortSwipeUp = app
            AppDrawerFlag.SetShortSwipeDown -> prefs.appShortSwipeDown = app
            AppDrawerFlag.SetShortSwipeLeft -> prefs.appShortSwipeLeft = app
            AppDrawerFlag.SetShortSwipeRight -> prefs.appShortSwipeRight = app
            AppDrawerFlag.SetLongSwipeUp -> prefs.appLongSwipeUp = app
            AppDrawerFlag.SetLongSwipeDown -> prefs.appLongSwipeDown = app
            AppDrawerFlag.SetLongSwipeLeft -> prefs.appLongSwipeLeft = app
            AppDrawerFlag.SetLongSwipeRight -> prefs.appLongSwipeRight = app
            AppDrawerFlag.SetClickClock -> prefs.appClickClock = app
            AppDrawerFlag.SetAppUsage -> prefs.appClickUsage = app
            AppDrawerFlag.SetClickDate -> prefs.appClickDate = app
            AppDrawerFlag.SetDoubleTap -> prefs.appDoubleTap = app
        }
    }

    fun firstOpen(value: Boolean) {
        firstOpen.postValue(value)
    }

    fun setShowDate(visibility: Boolean) {
        showDate.value = visibility
    }

    fun setShowTime(visibility: Boolean) {
        showTime.value = visibility
    }

    private fun launchApp(appListItem: AppListItem) {
        val packageName = appListItem.activityPackage
        val appActivityName = appListItem.activityClass
        val userHandle = appListItem.user
        val launcher = appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val activityInfo = launcher.getActivityList(packageName, userHandle)

        val component = when (activityInfo.size) {
            0 -> {
                showToastShort(appContext, "App not found")
                return
            }

            1 -> ComponentName(packageName, activityInfo[0].name)
            else -> if (appActivityName.isNotEmpty()) {
                ComponentName(packageName, appActivityName)
            } else {
                ComponentName(packageName, activityInfo[activityInfo.size - 1].name)
            }
        }

        try {
            val appUsageTracker = AppUsageTracker.createInstance(appContext)
            appUsageTracker.updateLastUsedTimestamp(packageName)
            launcher.startMainActivity(component, userHandle, null, null)
        } catch (e: SecurityException) {
            try {
                val appUsageTracker = AppUsageTracker.createInstance(appContext)
                appUsageTracker.updateLastUsedTimestamp(packageName)
                launcher.startMainActivity(component, android.os.Process.myUserHandle(), null, null)
            } catch (e: Exception) {
                showToastShort(appContext, "Unable to launch app")
            }
        } catch (e: Exception) {
            showToastShort(appContext, "Unable to launch app")
        }
    }

    fun getAppList(includeHiddenApps: Boolean = true) {
        viewModelScope.launch {
            appList.value = getAppsList(appContext, includeRegularApps = true, includeHiddenApps)
        }
    }

    fun getHiddenApps() {
        viewModelScope.launch {
            hiddenApps.value =
                getAppsList(appContext, includeRegularApps = false, includeHiddenApps = true)
        }
    }

    fun ismlauncherDefault() {
        launcherDefault.value = ismlauncherDefault(appContext)
    }

    fun resetDefaultLauncherApp(context: Context) {
        resetDefaultLauncher(context)
        launcherResetFailed.value = getDefaultLauncherPackage(
            appContext
        ).contains(".")
    }

    fun updateDrawerAlignment(gravity: Constants.Gravity) {
        prefs.drawerAlignment = gravity
    }

    fun updateClockAlignment(gravity: Constants.Gravity) {
        clockAlignment.value = gravity
    }

    fun updateHomeAppsAlignment(gravity: Constants.Gravity, onBottom: Boolean) {
        homeAppsAlignment.value = Pair(gravity, onBottom)
    }

    fun showMessageDialog(message: String) {
        showMessageDialog.postValue(message)
    }
}
