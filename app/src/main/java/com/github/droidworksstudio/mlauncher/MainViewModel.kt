package com.github.droidworksstudio.mlauncher

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.creativecodecat.components.views.FontAppCompatTextView
import com.github.droidworksstudio.common.AppLogger
import com.github.droidworksstudio.common.CrashHandler
import com.github.droidworksstudio.common.getLocalizedString
import com.github.droidworksstudio.common.hideKeyboard
import com.github.droidworksstudio.common.showLongToast
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
import com.github.droidworksstudio.mlauncher.ui.components.DialogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _appScrollMap = MutableLiveData<Map<String, Int>>()
    val appScrollMap: LiveData<Map<String, Int>> = _appScrollMap

    private lateinit var biometricHelper: BiometricHelper
    private lateinit var dialogBuilder: DialogManager

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
        biometricHelper = BiometricHelper(fragment)

        val packageName = appListItem.activityPackage
        val isTimerEnabled = prefs.enableAppTimer
        val currentLockedApps = prefs.lockedApps

        logActivitiesFromPackage(appContext, packageName)

        dialogBuilder = DialogManager(appContext, fragment.requireActivity())

        val bypassTimerApps = loadBypassTimerApps(appContext)

        val proceedToLaunch: () -> Unit = {
            if (packageName in bypassTimerApps) {
                // Bypass timer for these apps, launch immediately
                launchUnlockedApp(appListItem)
            } else if (isTimerEnabled) {
                val savedTargetTime = prefs.getSavedTimer(packageName)
                if (savedTargetTime > System.currentTimeMillis()) {
                    // Timer still valid, launch and start timer with remaining time
                    launchUnlockedApp(appListItem)
                    startAppCloseTimer(appListItem, savedTargetTime)
                } else {
                    prefs.clearTimer(packageName)
                    // No valid timer or expired, ask user with date/time picker
                    dialogBuilder.showTimerBottomSheet(fragment.requireContext()) { targetTimeMillis ->
                        prefs.saveTimer(packageName, targetTimeMillis)
                        launchUnlockedApp(appListItem)
                        startAppCloseTimer(appListItem, targetTimeMillis)
                    }
                }
            } else {
                launchUnlockedApp(appListItem)
            }
        }

        if (currentLockedApps.contains(packageName)) {
            fragment.hideKeyboard()
            biometricHelper.startBiometricAuth(appListItem, object : BiometricHelper.CallbackApp {
                override fun onAuthenticationSucceeded(appListItem: AppListItem) {
                    proceedToLaunch()
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
            proceedToLaunch()
        }
    }

    fun loadBypassTimerApps(context: Context): Set<String> {
        val bypassApps = mutableSetOf<String>()
        // Obtain an XmlPullParser for the bypass_timer_apps.xml file
        context.resources.getXml(R.xml.bypass_timer_apps).use { parser ->
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG && parser.name == "app") {
                    val packageName = parser.getAttributeValue(null, "packageName")
                    bypassApps.add(packageName)
                }
                parser.next()
            }
        }

        return bypassApps
    }


    private fun startAppCloseTimer(appListItem: AppListItem, targetTimeMillis: Long) {
        val packageName = appListItem.activityPackage
        val delay = targetTimeMillis - System.currentTimeMillis()

        if (delay <= 0) {
            // Time already passed, close immediately
            closeAppSession(packageName) { minutes ->
                val newTargetTime = System.currentTimeMillis() + minutes * 60_000
                prefs.saveTimer(packageName, newTargetTime)
                appContext.showShortToast("Extended by $minutes minutes")
                launchUnlockedApp(appListItem)
            }
            return
        }

        Handler(Looper.getMainLooper()).postDelayed({
            closeAppSession(packageName) { minutes ->
                val newTargetTime = System.currentTimeMillis() + minutes * 60_000
                prefs.saveTimer(packageName, newTargetTime)
                appContext.showShortToast("Extended by $minutes minutes")
                launchUnlockedApp(appListItem)
            }
        }, delay)
    }

    private fun closeAppSession(
        packageName: String,
        onExtend: (minutes: Int) -> Unit
    ) {
        val packageManager = appContext.packageManager
        val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
        val appName = packageManager.getApplicationLabel(applicationInfo).toString()
        val isTimerEnabled = prefs.enableAppTimer

        if (!isTimerEnabled) return
        if (Settings.canDrawOverlays(appContext)) {
            val themedContext = ContextThemeWrapper(appContext, R.style.Theme_mLauncher)
            val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            // Helper to convert dp to px
            fun dpToPx(dp: Int): Int =
                (dp * appContext.resources.displayMetrics.density).toInt()

            val overlayView = LinearLayout(themedContext).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimaryBackground))
                setPadding(60, 60, 60, 60)
                elevation = 10f
            }

            val title = FontAppCompatTextView(themedContext).apply {
                text = getLocalizedString(R.string.app_timer_extend_or_close)
                textSize = 18f
                setTextColor(ContextCompat.getColor(context, R.color.colorPrimary))
                setPadding(0, 0, 0, 30)
            }

            overlayView.addView(title)

            val extendOptions = listOf(
                5 to getLocalizedString(R.string.app_timer_extend_5),
                10 to getLocalizedString(R.string.app_timer_extend_10)
            )

            extendOptions.forEach { (minutes, label) ->
                val button = AppCompatButton(themedContext).apply {
                    text = label
                    setTextColor(ContextCompat.getColor(context, R.color.buttonTextPrimary))
                    backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.buttonBackgroundPrimary)
                    )
                    setOnClickListener {
                        windowManager.removeView(overlayView)
                        onExtend(minutes)
                    }
                }
                overlayView.addView(button)
            }

            // Custom timer input
            val customInputLabel = AppCompatTextView(themedContext).apply {
                text = getLocalizedString(R.string.app_timer_custom_minutes_label)
                setTextColor(ContextCompat.getColor(context, R.color.colorPrimary))
                setPadding(0, dpToPx(24), 0, dpToPx(8))
            }

            val customInput = AppCompatEditText(themedContext).apply {
                hint = getLocalizedString(R.string.app_timer_custom_minutes_hint)
                inputType = InputType.TYPE_CLASS_NUMBER
                backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.buttonBackgroundPrimary)
                )
                setTextColor(ContextCompat.getColor(context, R.color.colorPrimary))
                setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            }

            val customStartBtn = AppCompatButton(themedContext).apply {
                text = getLocalizedString(R.string.app_timer_start_custom)
                setTextColor(ContextCompat.getColor(context, R.color.buttonTextPrimary))
                backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.buttonBackgroundPrimary)
                )
                setOnClickListener {
                    val inputText = customInput.text.toString()
                    val customMinutes = inputText.toIntOrNull()

                    if (customMinutes == null || customMinutes <= 0) {
                        appContext.showLongToast(getLocalizedString(R.string.app_timer_invalid_minutes))
                        return@setOnClickListener
                    }

                    windowManager.removeView(overlayView)
                    onExtend(customMinutes)
                }
            }

            overlayView.apply {
                addView(customInputLabel)
                addView(customInput)
                addView(customStartBtn)
            }

            val closeButton = AppCompatButton(themedContext).apply {
                text = getLocalizedString(R.string.app_timer_close_app)
                setTextColor(ContextCompat.getColor(context, R.color.buttonTextPrimary))
                backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.buttonBackgroundError)
                )
                setOnClickListener {
                    windowManager.removeView(overlayView)

                    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    appContext.startActivity(homeIntent)

                    prefs.clearTimer(packageName)
                    appContext.showShortToast("$appName session ended")
                }
            }

            overlayView.addView(closeButton)

            // ✅ This is the ONLY layoutParams that matters for WindowManager
            val layoutParams = WindowManager.LayoutParams(
                dpToPx(400), // set desired fixed width in dp
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            windowManager.addView(overlayView, layoutParams)
        } else {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            appContext.startActivity(homeIntent)

            prefs.clearTimer(packageName)
            appContext.showShortToast("$appName session ended")
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

        AppLogger.d(
            "AppListDebug",
            "🔄 getAppsList called with: includeRegular=$includeRegularApps, includeHidden=$includeHiddenApps, includeRecent=$includeRecentApps"
        )

        try {
            val prefs = Prefs(context)
            val hiddenApps = prefs.hiddenApps
            val pinnedPackages = prefs.pinnedApps.toSet()
            val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val seenAppKeys = mutableSetOf<String>()  // packageName|userId
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
                        val appKey = "$packageName|${profile.hashCode()}"
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
                    val appKey = "$packageName/$className|${profile.hashCode()}"
                    if (seenAppKeys.contains(appKey)) {
                        AppLogger.d("AppListDebug", "⚠️ Skipping duplicate launcher activity: $appKey")
                        continue
                    }

                    val isHidden = hiddenApps.contains(appKey)
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
