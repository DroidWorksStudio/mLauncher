package com.github.droidworksstudio.mlauncher

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
import android.util.Log
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.Fragment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.NavHostFragment.Companion.findNavController
import com.github.droidworksstudio.common.CrashHandler
import com.github.droidworksstudio.common.hideKeyboard
import com.github.droidworksstudio.common.showShortToast
import com.github.droidworksstudio.mlauncher.data.AppListItem
import com.github.droidworksstudio.mlauncher.data.Constants
import com.github.droidworksstudio.mlauncher.data.Constants.AppDrawerFlag
import com.github.droidworksstudio.mlauncher.data.Prefs
import com.github.droidworksstudio.mlauncher.helper.analytics.AppUsageMonitor
import com.github.droidworksstudio.mlauncher.helper.getAppsList
import com.github.droidworksstudio.mlauncher.helper.ismlauncherDefault
import com.github.droidworksstudio.mlauncher.helper.setDefaultHomeScreen
import com.github.droidworksstudio.mlauncher.helper.utils.BiometricHelper
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private lateinit var biometricHelper: BiometricHelper

    private val appContext by lazy { application.applicationContext }
    private val prefs = Prefs(appContext)

    // setup variables with initial values
    val firstOpen = MutableLiveData<Boolean>()

    val appList = MutableLiveData<List<AppListItem>?>()
    val hiddenApps = MutableLiveData<List<AppListItem>?>()
    val homeAppsOrder = MutableLiveData<List<AppListItem>>()  // Store actual app items
    val launcherDefault = MutableLiveData<Boolean>()

    val showDate = MutableLiveData(prefs.showDate)
    val showClock = MutableLiveData(prefs.showClock)
    val showAlarm = MutableLiveData(prefs.showAlarm)
    val showDailyWord = MutableLiveData(prefs.showDailyWord)
    val showFloating = MutableLiveData(prefs.showFloating)
    val clockAlignment = MutableLiveData(prefs.clockAlignment)
    val dateAlignment = MutableLiveData(prefs.dateAlignment)
    val alarmAlignment = MutableLiveData(prefs.alarmAlignment)
    val dailyWordAlignment = MutableLiveData(prefs.dailyWordAlignment)
    val homeAppsAlignment = MutableLiveData(Pair(prefs.homeAlignment, prefs.homeAlignmentBottom))
    val homeAppsNum = MutableLiveData(prefs.homeAppsNum)
    val homePagesNum = MutableLiveData(prefs.homePagesNum)
    val opacityNum = MutableLiveData(prefs.opacityNum)
    val filterStrength = MutableLiveData(prefs.filterStrength)
    val recentCounter = MutableLiveData(prefs.recentCounter)
    val iconPack = MutableLiveData(prefs.iconPack)

    fun selectedApp(fragment: Fragment, app: AppListItem, flag: AppDrawerFlag, n: Int = 0) {
        when (flag) {
            AppDrawerFlag.LaunchApp,
            AppDrawerFlag.HiddenApps,
            AppDrawerFlag.PrivateApps -> {
                launchApp(app, fragment)
            }

            AppDrawerFlag.SetHomeApp -> {
                prefs.setHomeAppModel(n, app)
                findNavController(fragment).popBackStack()
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
            AppDrawerFlag.SetFloating -> prefs.appFloating = app
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

    fun setShowClock(visibility: Boolean) {
        showClock.value = visibility
    }

    fun setShowAlarm(visibility: Boolean) {
        showAlarm.value = visibility
    }

    fun setShowDailyWord(visibility: Boolean) {
        showDailyWord.value = visibility
    }

    fun setShowFloating(visibility: Boolean) {
        showFloating.value = visibility
    }

    fun setDefaultLauncher(visibility: Boolean) {
        launcherDefault.value = visibility
    }

    fun launchApp(appListItem: AppListItem, fragment: Fragment) {
        biometricHelper = BiometricHelper(fragment)

        val packageName = appListItem.activityPackage
        val currentLockedApps = prefs.lockedApps

        if (currentLockedApps.contains(packageName)) {
            fragment.hideKeyboard()
            biometricHelper.startBiometricAuth(appListItem, object : BiometricHelper.CallbackApp {
                override fun onAuthenticationSucceeded(appListItem: AppListItem) {
                    launchUnlockedApp(appListItem)
                }

                override fun onAuthenticationFailed() {
                    Log.e(
                        "Authentication",
                        appContext.getString(R.string.text_authentication_failed)
                    )
                }

                override fun onAuthenticationError(errorCode: Int, errorMessage: CharSequence?) {
                    when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED -> Log.e(
                            "Authentication",
                            appContext.getString(R.string.text_authentication_cancel)
                        )

                        else -> Log.e(
                            "Authentication",
                            appContext.getString(R.string.text_authentication_error).format(
                                errorMessage,
                                errorCode
                            )
                        )
                    }
                }
            })
        } else {
            launchUnlockedApp(appListItem)
        }
    }

    private fun launchUnlockedApp(appListItem: AppListItem) {
        val packageName = appListItem.activityPackage
        val userHandle = appListItem.user
        val launcher = appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val activityInfo = launcher.getActivityList(packageName, userHandle)

        if (activityInfo.isNotEmpty()) {
            val component = ComponentName(packageName, activityInfo.first().name)
            launchAppWithPermissionCheck(component, packageName, userHandle, launcher)
        } else {
            appContext.showShortToast("App not found")
        }
    }


    private fun launchAppWithPermissionCheck(component: ComponentName, packageName: String, userHandle: UserHandle, launcher: LauncherApps) {
        try {
            val appUsageTracker = AppUsageMonitor.createInstance(appContext)
            appUsageTracker.updateLastUsedTimestamp(packageName)
            launcher.startMainActivity(component, userHandle, null, null)
            CrashHandler.logUserAction("${component.packageName} App Launched")
        } catch (_: SecurityException) {
            try {
                val appUsageTracker = AppUsageMonitor.createInstance(appContext)
                appUsageTracker.updateLastUsedTimestamp(packageName)
                launcher.startMainActivity(component, Process.myUserHandle(), null, null)
                CrashHandler.logUserAction("${component.packageName} App Launched")
            } catch (_: Exception) {
                appContext.showShortToast("Unable to launch app")
            }
        } catch (_: Exception) {
            appContext.showShortToast("Unable to launch app")
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
        val isDefault = ismlauncherDefault(appContext)
        launcherDefault.value = !isDefault
    }

    fun resetDefaultLauncherApp(context: Context) {
        setDefaultHomeScreen(context)
    }

    fun updateDrawerAlignment(gravity: Constants.Gravity) {
        prefs.drawerAlignment = gravity
    }

    fun updateDateAlignment(gravity: Constants.Gravity) {
        dateAlignment.value = gravity
    }

    fun updateClockAlignment(gravity: Constants.Gravity) {
        clockAlignment.value = gravity
    }

    fun updateAlarmAlignment(gravity: Constants.Gravity) {
        alarmAlignment.value = gravity
    }

    fun updateDailyWordAlignment(gravity: Constants.Gravity) {
        dailyWordAlignment.value = gravity
    }

    fun updateHomeAppsAlignment(gravity: Constants.Gravity, onBottom: Boolean) {
        homeAppsAlignment.value = Pair(gravity, onBottom)
    }

    fun updateAppOrder(fromPosition: Int, toPosition: Int) {
        val currentOrder = homeAppsOrder.value?.toMutableList() ?: return

        // Move the actual app object in the list
        val app = currentOrder.removeAt(fromPosition)
        currentOrder.add(toPosition, app)

        homeAppsOrder.postValue(currentOrder)
        saveAppOrder(currentOrder)  // Save new order in preferences
    }

    private fun saveAppOrder(order: List<AppListItem>) {
        order.forEachIndexed { index, app ->
            prefs.setHomeAppModel(index, app)  // Save app in its new order
        }
    }

    fun loadAppOrder() {
        val savedOrder = (0 until prefs.homeAppsNum)
            .mapNotNull { prefs.getHomeAppModel(it) } // Ensure it doesn’t return null
        homeAppsOrder.postValue(savedOrder) // ✅ Now posts a valid list
    }
}
