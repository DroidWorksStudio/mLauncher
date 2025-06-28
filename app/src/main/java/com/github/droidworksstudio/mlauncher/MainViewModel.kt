package com.github.droidworksstudio.mlauncher

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.Fragment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.droidworksstudio.common.AppLogger
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
            AppLogger.d("MainViewModel", "Pinned apps changed")
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
        biometricHelper = BiometricHelper(fragment.requireActivity())

        val packageName = appListItem.activityPackage
        val currentLockedApps = prefs.lockedApps

        logActivitiesFromPackage(appContext, packageName)

        if (currentLockedApps.contains(packageName)) {

            biometricHelper.startBiometricAuth(appListItem, object : BiometricHelper.CallbackApp {
                override fun onAuthenticationSucceeded(appListItem: AppListItem) {
                    if (fragment.isAdded) {
                        fragment.hideKeyboard()
                    }
                    launchUnlockedApp(appListItem)
                }

                override fun onAuthenticationFailed() {
                    AppLogger.e(
                        "Authentication",
                        getLocalizedString(R.string.text_authentication_failed)
                    )
                }

                override fun onAuthenticationError(errorCode: Int, errorMessage: CharSequence?) {
                    when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED -> AppLogger.e(
                            "Authentication",
                            getLocalizedString(R.string.text_authentication_cancel)
                        )

                        else -> AppLogger.e(
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
        val appUsageTracker = AppUsageMonitor.createInstance(appContext)

        fun tryLaunch(user: UserHandle): Boolean {
            return try {
                appUsageTracker.updateLastUsedTimestamp(packageName)
                launcher.startMainActivity(component, user, null, null)
                CrashHandler.logUserAction("${component.packageName} App Launched")
                true
            } catch (_: Exception) {
                false
            }
        }

        if (!tryLaunch(userHandle)) {
            if (!tryLaunch(Process.myUserHandle())) {
                appContext.showShortToast("Unable to launch app")
            }
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

        AppLogger.d(
            "AppListDebug",
            "🔄 getAppsList called with: includeRegular=$includeRegularApps, includeHidden=$includeHiddenApps, includeRecent=$includeRecentApps"
        )

        try {
            val prefs = Prefs(context)
            val hiddenApps = prefs.hiddenApps
            val pinnedPackages = prefs.pinnedApps.toSet()
            val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
            val seenAppKeys = mutableSetOf<String>()  // packageName|activityName|userId
            val scrollIndexMap = mutableMapOf<String, Int>()

            for (profile in userManager.userProfiles) {
                AppLogger.d("AppListDebug", "👤 Processing user profile: $profile")

                val isPrivate = PrivateSpaceManager(context).isPrivateSpaceProfile(profile)
                if (isPrivate && PrivateSpaceManager(context).isPrivateSpaceLocked()) {
                    AppLogger.d("AppListDebug", "🔒 Skipping locked private space for profile: $profile")
                    continue
                }

                // Recent Apps
                if (prefs.recentAppsDisplayed && includeRecentApps && fullList.none { it.category == AppCategory.RECENT }) {
                    val tracker = AppUsageMonitor.createInstance(context)
                    val recentApps = tracker.getLastTenAppsUsed(context)

                    AppLogger.d("AppListDebug", "🕓 Adding ${recentApps.size} recent apps")

                    for ((packageName, appName, activityName) in recentApps) {
                        val appKey = "$packageName|$activityName|${profile.hashCode()}"
                        if (seenAppKeys.contains(appKey)) {
                            AppLogger.d("AppListDebug", "⚠️ Skipping duplicate recent app: $appKey")
                            continue
                        }

                        val alias = prefs.getAppAlias(packageName).ifEmpty { appName }
                        val tag = prefs.getAppTag(packageName)

                        fullList.add(
                            AppListItem(appName, packageName, activityName, profile, alias, tag, AppCategory.RECENT)
                        )
                        seenAppKeys.add(appKey)
                    }
                }

                val pm = context.packageManager
                val intent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }

                val activities = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
                AppLogger.d("AppListDebug", "📦 Found ${activities.size} launcher activities")

                for (resolveInfo in activities) {
                    val activityInfo = resolveInfo.activityInfo
                    val packageName = activityInfo.packageName
                    val className = activityInfo.name
                    val label = resolveInfo.loadLabel(pm).toString()

                    if (packageName == BuildConfig.APPLICATION_ID) continue

                    // ✅ Deduplicate based on activity, not just package
                    val appKey = "$packageName|$className|${profile.hashCode()}"
                    if (seenAppKeys.contains(appKey)) {
                        AppLogger.d("AppListDebug", "⚠️ Skipping duplicate launcher activity: $appKey")
                        continue
                    }

                    val isHidden = listOf(packageName, appKey, "$packageName|${profile.hashCode()}").any { it in hiddenApps }
                    if ((isHidden && !includeHiddenApps) || (!isHidden && !includeRegularApps)) {
                        AppLogger.d("AppListDebug", "🚫 Skipping app due to filter: $appKey (hidden=$isHidden)")
                        continue
                    }

                    val alias = prefs.getAppAlias(packageName)
                    val tag = prefs.getAppTag(packageName)

                    val category = when {
                        pinnedPackages.contains(packageName) -> AppCategory.PINNED
                        else -> AppCategory.REGULAR
                    }

                    fullList.add(
                        AppListItem(label, packageName, className, profile, alias, tag, category)
                    )
                    AppLogger.d("AppListDebug", "✅ Added app: $label ($packageName/$className) from profile: $profile")
                    seenAppKeys.add(appKey)
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
                val key = item.label.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
                scrollIndexMap.putIfAbsent(key, index)
            }

            // Include scroll index for pinned apps under '★'
            fullList.forEachIndexed { index, item ->
                val key = when (item.category) {
                    AppCategory.PINNED -> "★"
                    else -> item.label.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
                }

                if (!scrollIndexMap.containsKey(key)) {
                    scrollIndexMap[key] = index
                }
            }

            AppLogger.d("AppListDebug", "✅ App list built with ${fullList.size} items")
            _appScrollMap.postValue(scrollIndexMap)

        } catch (e: Exception) {
            AppLogger.e("AppListDebug", "❌ Error building app list: ${e.message}", e)
        }

        fullList
    }
}
