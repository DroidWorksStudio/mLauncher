package com.github.droidworksstudio.mlauncher

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.Fragment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.droidworksstudio.common.CrashHandler
import com.github.droidworksstudio.common.getLocalizedString
import com.github.droidworksstudio.common.hideKeyboard
import com.github.droidworksstudio.common.showShortToast
import com.github.droidworksstudio.mlauncher.data.AppCategory
import com.github.droidworksstudio.mlauncher.data.AppListItem
import com.github.droidworksstudio.mlauncher.data.Constants
import com.github.droidworksstudio.mlauncher.data.Constants.AppDrawerFlag
import com.github.droidworksstudio.mlauncher.data.Prefs
import com.github.droidworksstudio.mlauncher.helper.analytics.AppUsageMonitor
import com.github.droidworksstudio.mlauncher.helper.ismlauncherDefault
import com.github.droidworksstudio.mlauncher.helper.logActivitiesFromPackage
import com.github.droidworksstudio.mlauncher.helper.utils.BiometricHelper
import com.github.droidworksstudio.mlauncher.helper.utils.PrivateSpaceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _appScrollMap = MutableLiveData<Map<String, Int>>()
    val appScrollMap: LiveData<Map<String, Int>> = _appScrollMap

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
    val customIconPackHome = MutableLiveData(prefs.customIconPackHome)
    val iconPackHome = MutableLiveData(prefs.iconPackHome)
    val customIconPackAppList = MutableLiveData(prefs.customIconPackAppList)
    val iconPackAppList = MutableLiveData(prefs.iconPackAppList)

    private val prefsNormal = prefs.prefsNormal
    private val pinnedAppsKey = prefs.pinnedAppsKey

    private val pinnedAppsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == pinnedAppsKey) {
            Log.d("MainViewModel", "Pinned apps changed")
            getAppList()
        }
    }

    init {
        prefsNormal.registerOnSharedPreferenceChangeListener(pinnedAppsListener)
        getAppList()
    }

    fun selectedApp(fragment: Fragment, app: AppListItem, flag: AppDrawerFlag, n: Int = 0) {
        when (flag) {
            AppDrawerFlag.SetHomeApp -> prefs.setHomeAppModel(n, app)
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
            AppDrawerFlag.LaunchApp, AppDrawerFlag.HiddenApps, AppDrawerFlag.PrivateApps -> launchApp(
                app,
                fragment
            )

            AppDrawerFlag.None -> {}
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

    fun setDefaultLauncher(visibility: Boolean) {
        val reverseValue = !visibility
        launcherDefault.value = reverseValue
    }

    fun launchApp(appListItem: AppListItem, fragment: Fragment) {
        biometricHelper = BiometricHelper(fragment)

        val packageName = appListItem.activityPackage
        val currentLockedApps = prefs.lockedApps

        logActivitiesFromPackage(appContext, packageName)

        if (currentLockedApps.contains(packageName)) {
            fragment.hideKeyboard()
            biometricHelper.startBiometricAuth(appListItem, object : BiometricHelper.CallbackApp {
                override fun onAuthenticationSucceeded(appListItem: AppListItem) {
                    launchUnlockedApp(appListItem)
                }

                override fun onAuthenticationFailed() {
                    Log.e(
                        "Authentication", getLocalizedString(R.string.text_authentication_failed)
                    )
                }

                override fun onAuthenticationError(errorCode: Int, errorMessage: CharSequence?) {
                    when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED -> Log.e(
                            "Authentication",
                            getLocalizedString(R.string.text_authentication_cancel)
                        )

                        else -> Log.e(
                            "Authentication",
                            getLocalizedString(R.string.text_authentication_error).format(
                                errorMessage, errorCode
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


    private fun launchAppWithPermissionCheck(
        component: ComponentName,
        packageName: String,
        userHandle: UserHandle,
        launcher: LauncherApps
    ) {
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

    fun getAppList(includeHiddenApps: Boolean = true, includeRecentApps: Boolean = true) {
        viewModelScope.launch {
            appList.value =
                getAppsList(appContext, includeRegularApps = true, includeHiddenApps, includeRecentApps)
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
        (context as MainActivity).setDefaultHomeScreen(context)
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
        val savedOrder =
            (0 until prefs.homeAppsNum).mapNotNull { prefs.getHomeAppModel(it) } // Ensure it doesn’t return null
        homeAppsOrder.postValue(savedOrder) // ✅ Now posts a valid list
    }

    // Clean up listener to prevent memory leaks
    override fun onCleared() {
        super.onCleared()
        prefsNormal.unregisterOnSharedPreferenceChangeListener(pinnedAppsListener)
    }

    suspend fun getAppsList(
        context: Context,
        includeRegularApps: Boolean = true,
        includeHiddenApps: Boolean = false,
        includeRecentApps: Boolean = true
    ): MutableList<AppListItem> = withContext(Dispatchers.Main) {

        val fullList: MutableList<AppListItem> = mutableListOf()
        val scrollIndexMap = mutableMapOf<Char, Int>()

        Log.d(
            "AppListDebug",
            "🔄 getAppsList called with: includeRegular=$includeRegularApps, includeHidden=$includeHiddenApps, includeRecent=$includeRecentApps"
        )
        CrashHandler.logUserAction("Display App List")

        try {
            val prefs = Prefs(context)
            val hiddenApps = prefs.hiddenApps
            val pinnedPackages = prefs.pinnedApps.toSet()
            val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val seenPackages = mutableSetOf<String>()

            for (profile in userManager.userProfiles) {
                Log.d("AppListDebug", "👤 Processing user profile: $profile")

                val isPrivate = PrivateSpaceManager(context).isPrivateSpaceProfile(profile)
                if (isPrivate && PrivateSpaceManager(context).isPrivateSpaceLocked()) {
                    Log.d("AppListDebug", "🔒 Skipping locked private space for profile: $profile")
                    continue
                }

                // Recent Apps
                if (prefs.recentAppsDisplayed && includeRecentApps && fullList.none { it.category == AppCategory.RECENT }) {
                    val tracker = AppUsageMonitor.createInstance(context)
                    val recentApps = tracker.getLastTenAppsUsed(context)

                    Log.d("AppListDebug", "🕓 Adding ${recentApps.size} recent apps")

                    for ((packageName, appName, activityName) in recentApps) {
                        if (seenPackages.contains(packageName)) continue
                        val alias = prefs.getAppAlias(packageName).ifEmpty { appName }
                        val tag = prefs.getAppTag(packageName)

                        fullList.add(
                            AppListItem(appName, packageName, activityName, profile, alias, tag, AppCategory.RECENT)
                        )
                        seenPackages.add(packageName)
                    }
                }

                // Launcher Apps
                val launcherAppList = launcherApps.getActivityList(null, profile)
                Log.d("AppListDebug", "📦 Found ${launcherAppList.size} launcher apps for profile: $profile")

                for (activity in launcherAppList) {
                    val packageName = activity.applicationInfo.packageName
                    val className = activity.componentName.className
                    val label = activity.label.toString()

                    if (packageName == BuildConfig.APPLICATION_ID) continue
                    if (seenPackages.contains(packageName)) continue

                    val isHidden = hiddenApps.contains("$packageName|$profile")
                    if ((isHidden && !includeHiddenApps) || (!isHidden && !includeRegularApps)) continue

                    val alias = prefs.getAppAlias(packageName)
                    val tag = prefs.getAppTag(packageName)

                    val category = when {
                        pinnedPackages.contains(packageName) -> AppCategory.PINNED
                        else -> AppCategory.REGULAR
                    }

                    fullList.add(
                        AppListItem(label, packageName, className, profile, alias, tag, category)
                    )

                    seenPackages.add(packageName)
                }
            }

            // Sort the list: Pinned → Regular → Recent; then alphabetical within category
            fullList.sortWith(
                compareBy<AppListItem> { it.category.ordinal }
                    .thenBy { it.label.lowercase() }
            )

            // Build scroll index (excluding pinned apps)
            for ((index, item) in fullList.withIndex()) {
                if (item.category == AppCategory.PINNED) continue
                val firstChar = item.label.firstOrNull()?.uppercaseChar() ?: continue
                scrollIndexMap.putIfAbsent(firstChar, index)
            }

            Log.d("AppListDebug", "✅ App list built with ${fullList.size} items")

            val scrollIndexMap = mutableMapOf<String, Int>()

            fullList.forEachIndexed { index, item ->
                val key = when (item.category) {
                    AppCategory.PINNED -> "★"
                    else -> item.label.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
                }

                if (!scrollIndexMap.containsKey(key)) {
                    scrollIndexMap[key] = index
                }
            }

            _appScrollMap.postValue(scrollIndexMap)  // ✅ Post the map to LiveData

        } catch (e: Exception) {
            Log.e("AppListDebug", "❌ Error building app list: ${e.message}", e)
        }

        fullList
    }
}
