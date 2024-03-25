package com.github.hecodes2much.mlauncher

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.hecodes2much.mlauncher.data.AppModel
import com.github.hecodes2much.mlauncher.data.Constants
import com.github.hecodes2much.mlauncher.data.Constants.AppDrawerFlag
import com.github.hecodes2much.mlauncher.data.Prefs
import com.github.hecodes2much.mlauncher.helper.AppUsageTracker
import com.github.hecodes2much.mlauncher.helper.getAppsList
import com.github.hecodes2much.mlauncher.helper.getDefaultLauncherPackage
import com.github.hecodes2much.mlauncher.helper.ismlauncherDefault
import com.github.hecodes2much.mlauncher.helper.resetDefaultLauncher
import com.github.hecodes2much.mlauncher.helper.showToastShort
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext by lazy { application.applicationContext }
    private val prefs = Prefs(appContext)

    // setup variables with initial values
    val firstOpen = MutableLiveData<Boolean>()
    val showMessageDialog = MutableLiveData<String>()

    val appList = MutableLiveData<List<AppModel>?>()
    val hiddenApps = MutableLiveData<List<AppModel>?>()
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

    fun selectedApp(appModel: AppModel, flag: AppDrawerFlag, n: Int = 0) {
        when (flag) {
            AppDrawerFlag.LaunchApp, AppDrawerFlag.HiddenApps -> {
                launchApp(appModel)
            }

            AppDrawerFlag.SetHomeApp, AppDrawerFlag.ReorderApps -> {
                prefs.setHomeAppModel(n, appModel)
            }

            AppDrawerFlag.SetShortSwipeUp -> prefs.appShortSwipeUp = appModel
            AppDrawerFlag.SetShortSwipeDown -> prefs.appShortSwipeDown = appModel
            AppDrawerFlag.SetShortSwipeLeft -> prefs.appShortSwipeLeft = appModel
            AppDrawerFlag.SetShortSwipeRight -> prefs.appShortSwipeRight = appModel
            AppDrawerFlag.SetLongSwipeUp -> prefs.appLongSwipeUp = appModel
            AppDrawerFlag.SetLongSwipeDown -> prefs.appLongSwipeDown = appModel
            AppDrawerFlag.SetLongSwipeLeft -> prefs.appLongSwipeLeft = appModel
            AppDrawerFlag.SetLongSwipeRight -> prefs.appLongSwipeRight = appModel
            AppDrawerFlag.SetClickClock -> prefs.appClickClock = appModel
            AppDrawerFlag.SetClickDate -> prefs.appClickDate = appModel
            AppDrawerFlag.SetDoubleTap -> prefs.appDoubleTap = appModel
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

    private fun launchApp(appModel: AppModel) {
        val packageName = appModel.appPackage
        val appActivityName = appModel.appActivityName
        val userHandle = appModel.user
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
