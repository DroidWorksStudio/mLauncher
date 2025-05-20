package com.github.droidworksstudio.mlauncher.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Context.VIBRATOR_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.format.DateFormat
import android.text.style.ImageSpan
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.github.droidworksstudio.common.ColorManager
import com.github.droidworksstudio.common.CrashHandler
import com.github.droidworksstudio.common.getLocalizedString
import com.github.droidworksstudio.common.isGestureNavigationEnabled
import com.github.droidworksstudio.common.launchCalendar
import com.github.droidworksstudio.common.openAccessibilitySettings
import com.github.droidworksstudio.common.openAlarmApp
import com.github.droidworksstudio.common.openBatteryManager
import com.github.droidworksstudio.common.openCameraApp
import com.github.droidworksstudio.common.openDeviceSettings
import com.github.droidworksstudio.common.openDialerApp
import com.github.droidworksstudio.common.openDigitalWellbeing
import com.github.droidworksstudio.common.openPhotosApp
import com.github.droidworksstudio.common.openTextMessagesApp
import com.github.droidworksstudio.common.openWebBrowser
import com.github.droidworksstudio.common.requestLocationPermission
import com.github.droidworksstudio.common.showShortToast
import com.github.droidworksstudio.mlauncher.MainViewModel
import com.github.droidworksstudio.mlauncher.R
import com.github.droidworksstudio.mlauncher.data.Constants
import com.github.droidworksstudio.mlauncher.data.Constants.Action
import com.github.droidworksstudio.mlauncher.data.Constants.AppDrawerFlag
import com.github.droidworksstudio.mlauncher.data.Prefs
import com.github.droidworksstudio.mlauncher.databinding.FragmentHomeBinding
import com.github.droidworksstudio.mlauncher.helper.IconPackHelper
import com.github.droidworksstudio.mlauncher.helper.analytics.AppUsageMonitor
import com.github.droidworksstudio.mlauncher.helper.formatMillisToHMS
import com.github.droidworksstudio.mlauncher.helper.getHexForOpacity
import com.github.droidworksstudio.mlauncher.helper.getNextAlarm
import com.github.droidworksstudio.mlauncher.helper.getSystemIcons
import com.github.droidworksstudio.mlauncher.helper.hasUsageAccessPermission
import com.github.droidworksstudio.mlauncher.helper.hideStatusBar
import com.github.droidworksstudio.mlauncher.helper.initActionService
import com.github.droidworksstudio.mlauncher.helper.ismlauncherDefault
import com.github.droidworksstudio.mlauncher.helper.receivers.BatteryReceiver
import com.github.droidworksstudio.mlauncher.helper.receivers.PrivateSpaceReceiver
import com.github.droidworksstudio.mlauncher.helper.receivers.WeatherReceiver
import com.github.droidworksstudio.mlauncher.helper.showPermissionDialog
import com.github.droidworksstudio.mlauncher.helper.showStatusBar
import com.github.droidworksstudio.mlauncher.helper.utils.AppReloader
import com.github.droidworksstudio.mlauncher.helper.utils.BiometricHelper
import com.github.droidworksstudio.mlauncher.helper.utils.PrivateSpaceManager
import com.github.droidworksstudio.mlauncher.helper.wordOfTheDay
import com.github.droidworksstudio.mlauncher.listener.OnSwipeTouchListener
import com.github.droidworksstudio.mlauncher.listener.ViewSwipeTouchListener
import com.github.droidworksstudio.mlauncher.services.ActionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

class HomeFragment : Fragment(), View.OnClickListener, View.OnLongClickListener {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var deviceManager: DevicePolicyManager
    private lateinit var batteryReceiver: BatteryReceiver
    private lateinit var biometricHelper: BiometricHelper
    private lateinit var privateSpaceReceiver: PrivateSpaceReceiver
    private lateinit var vibrator: Vibrator

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        val view = binding.root
        prefs = Prefs(requireContext())
        batteryReceiver = BatteryReceiver()
        if (PrivateSpaceManager(requireContext()).isPrivateSpaceSupported()) {
            privateSpaceReceiver = PrivateSpaceReceiver()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        biometricHelper = BiometricHelper(this)

        viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        viewModel.ismlauncherDefault()

        deviceManager =
            context?.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        @Suppress("DEPRECATION")
        vibrator = context?.getSystemService(VIBRATOR_SERVICE) as Vibrator

        initAppObservers()
        initClickListeners()
        initPermissionCheck()
        initSwipeTouchListener()
        initObservers()

        // Update view appearance/settings based on prefs
        updateUIFromPreferences()
    }

    override fun onStart() {
        super.onStart()

        // Register battery receiver
        batteryReceiver = BatteryReceiver()
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            requireContext().registerReceiver(batteryReceiver, filter)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Register private space receiver if supported
        if (PrivateSpaceManager(requireContext()).isPrivateSpaceSupported()) {
            privateSpaceReceiver = PrivateSpaceReceiver()
            try {
                val filter = IntentFilter(Intent.ACTION_PROFILE_AVAILABLE)
                requireContext().registerReceiver(privateSpaceReceiver, filter)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Weather: may change frequently
        if (prefs.showWeather) getWeather() else binding.weather.visibility = View.GONE

        // Handle status bar once per view creation
        if (prefs.showStatusBar) showStatusBar(requireActivity()) else hideStatusBar(requireActivity())

        // Update only dynamic elements (not all UI prefs)
        updateTimeAndInfo()
    }

    override fun onStop() {
        super.onStop()
        try {
            requireContext().unregisterReceiver(batteryReceiver)
            if (PrivateSpaceManager(requireContext()).isPrivateSpaceSupported()) {
                requireContext().unregisterReceiver(privateSpaceReceiver)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateUIFromPreferences() {
        val timezone = prefs.appLanguage.timezone()
        val is24HourFormat = DateFormat.is24HourFormat(requireContext())

        binding.apply {
            val best12 = DateFormat.getBestDateTimePattern(
                timezone,
                if (prefs.showClockFormat) "hhmma" else "hhmm"
            ).let {
                if (!prefs.showClockFormat) it.removeSuffix(" a") else it
            }
            val best24 = DateFormat.getBestDateTimePattern(timezone, "HHmm")
            val timePattern = if (is24HourFormat) best24 else best12
            clock.format12Hour = timePattern
            clock.format24Hour = timePattern

            val datePattern = DateFormat.getBestDateTimePattern(timezone, "eeeddMMM")
            date.format12Hour = datePattern
            date.format24Hour = datePattern

            // Static UI setup
            date.textSize = prefs.dateSize.toFloat()
            clock.textSize = prefs.clockSize.toFloat()
            alarm.textSize = prefs.alarmSize.toFloat()
            dailyWord.textSize = prefs.dailyWordSize.toFloat()
            battery.textSize = prefs.batterySize.toFloat()
            homeScreenPager.textSize = prefs.appSize.toFloat()

            battery.visibility = if (prefs.showBattery) View.VISIBLE else View.GONE
            mainLayout.setBackgroundColor(getHexForOpacity(prefs))

            date.setTextColor(prefs.dateColor)
            clock.setTextColor(prefs.clockColor)
            alarm.setTextColor(prefs.alarmClockColor)
            dailyWord.setTextColor(prefs.dailyWordColor)
            battery.setTextColor(prefs.batteryColor)
            totalScreenTime.setTextColor(prefs.appColor)
            setDefaultLauncher.setTextColor(prefs.appColor)

            val fabList =
                listOf(fabPhone, fabMessages, fabCamera, fabPhotos, fabBrowser, fabSettings)
            val colors = ColorManager.getRandomHueColors(prefs.shortcutIconsColor, fabList.size)
            fabList.zip(colors).forEach { (fab, color) ->
                fab.setColorFilter(if (prefs.iconRainbowColors) color else prefs.shortcutIconsColor)
            }
        }
    }

    private fun updateTimeAndInfo() {
        binding.apply {
            alarm.text = getNextAlarm(requireContext(), prefs)
            dailyWord.text = wordOfTheDay(requireContext(), prefs)
        }
    }


    override fun onClick(view: View) {
        when (view.id) {
            R.id.clock -> {
                when (val action = prefs.clickClockAction) {
                    Action.OpenApp -> openClickClockApp()
                    else -> handleOtherAction(action)
                }
                CrashHandler.logUserAction("Clock Clicked")
            }

            R.id.date -> {
                when (val action = prefs.clickDateAction) {
                    Action.OpenApp -> openClickDateApp()
                    else -> handleOtherAction(action)
                }
                CrashHandler.logUserAction("Date Clicked")
            }

            R.id.totalScreenTime -> {
                when (val action = prefs.clickAppUsageAction) {
                    Action.OpenApp -> openClickUsageApp()
                    else -> handleOtherAction(action)
                }
                CrashHandler.logUserAction("TotalScreenTime Clicked")
            }

            R.id.setDefaultLauncher -> {
                viewModel.resetDefaultLauncherApp(requireContext())
                CrashHandler.logUserAction("SetDefaultLauncher Clicked")
            }

            R.id.battery -> {
                requireContext().openBatteryManager()
                CrashHandler.logUserAction("Battery Clicked")
            }

            R.id.fabPhone -> {
                context?.openDialerApp()
                CrashHandler.logUserAction("fabPhone Clicked")
            }

            R.id.fabMessages -> {
                context?.openTextMessagesApp()
                CrashHandler.logUserAction("fabMessages Clicked")
            }

            R.id.fabCamera -> {
                context?.openCameraApp()
                CrashHandler.logUserAction("fabCamera Clicked")
            }

            R.id.fabPhotos -> {
                context?.openPhotosApp()
                CrashHandler.logUserAction("fabPhotos Clicked")
            }

            R.id.fabBrowser -> {
                context?.openWebBrowser()
                CrashHandler.logUserAction("fabBrowser Clicked")
            }

            R.id.fabSettings -> {
                trySettings()
                CrashHandler.logUserAction("fabSettings Clicked")
            }

            R.id.fabAction -> {
                when (val action = prefs.clickFloatingAction) {
                    Action.OpenApp -> openFabActionApp()
                    else -> handleOtherAction(action)
                }
                CrashHandler.logUserAction("fabAction Clicked")
            }

            else -> {
                try { // Launch app
                    val appLocation = view.id
                    homeAppClicked(appLocation)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onLongClick(view: View): Boolean {
        if (prefs.homeLocked) return true

        val n = view.id
        showAppList(AppDrawerFlag.SetHomeApp, includeHiddenApps = true, n)
        CrashHandler.logUserAction("Show App List")
        return true
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun initSwipeTouchListener() {
        binding.touchArea.setOnTouchListener(getHomeScreenGestureListener(requireContext()))
    }

    private fun initPermissionCheck() {
        val context = requireContext()
        if (prefs.recentAppsDisplayed || prefs.appUsageStats) {
            // Check if the usage permission is not granted
            if (!hasUsageAccessPermission(context)) {
                // Postpone showing the dialog until the activity is running
                Handler(Looper.getMainLooper()).post {
                    // Instantiate MainActivity and pass it to showPermissionDialog
                    showPermissionDialog(context)
                }
            }
        }
    }

    private fun initClickListeners() {
        binding.apply {
            clock.setOnClickListener(this@HomeFragment)
            date.setOnClickListener(this@HomeFragment)
            totalScreenTime.setOnClickListener(this@HomeFragment)
            setDefaultLauncher.setOnClickListener(this@HomeFragment)
            battery.setOnClickListener(this@HomeFragment)

            fabPhone.setOnClickListener(this@HomeFragment)
            fabMessages.setOnClickListener(this@HomeFragment)
            fabCamera.setOnClickListener(this@HomeFragment)
            fabPhotos.setOnClickListener(this@HomeFragment)
            fabBrowser.setOnClickListener(this@HomeFragment)
            fabAction.setOnClickListener(this@HomeFragment)
            fabSettings.setOnClickListener(this@HomeFragment)
        }
    }


    private fun initAppObservers() {
        binding.apply {
            if (prefs.firstSettingsOpen) firstRunTips.visibility = View.VISIBLE
            else firstRunTips.visibility = View.GONE

            if (!ismlauncherDefault(requireContext())) setDefaultLauncher.visibility = View.VISIBLE
            else setDefaultLauncher.visibility = View.GONE
        }

        with(viewModel) {
            homeAppsNum.observe(viewLifecycleOwner) {
                if (prefs.appUsageStats) {
                    updateAppCountWithUsageStats(it)
                } else {
                    updateAppCount(it)
                }
            }
            launcherDefault.observe(viewLifecycleOwner) {
                binding.setDefaultLauncher.visibility = if (it) View.VISIBLE else View.GONE
            }
        }
    }

    private fun initObservers() {
        with(viewModel) {
            showDate.observe(viewLifecycleOwner) {
                binding.date.visibility = if (it) View.VISIBLE else View.GONE
            }
            showClock.observe(viewLifecycleOwner) {
                binding.clock.visibility = if (it) View.VISIBLE else View.GONE
            }
            showAlarm.observe(viewLifecycleOwner) {
                binding.alarm.visibility = if (it) View.VISIBLE else View.GONE
            }
            showDailyWord.observe(viewLifecycleOwner) {
                binding.dailyWord.visibility = if (it) View.VISIBLE else View.GONE
            }

            clockAlignment.observe(viewLifecycleOwner) { clockGravity ->
                binding.clock.gravity = clockGravity.value()

                // Set layout_gravity to align the TextClock (clock) within the parent (LinearLayout)
                binding.clock.layoutParams =
                    (binding.clock.layoutParams as LinearLayout.LayoutParams).apply {
                        gravity = clockGravity.value()
                    }
            }

            dateAlignment.observe(viewLifecycleOwner) { dateGravity ->
                binding.date.gravity = dateGravity.value()

                // Set layout_gravity to align the TextClock (date) within the parent (LinearLayout)
                binding.date.layoutParams =
                    (binding.date.layoutParams as LinearLayout.LayoutParams).apply {
                        gravity = dateGravity.value()
                    }
            }

            alarmAlignment.observe(viewLifecycleOwner) { alarmGravity ->
                binding.alarm.gravity = alarmGravity.value()

                // Set layout_gravity to align the TextView (alarm) within the parent (LinearLayout)
                binding.alarm.layoutParams =
                    (binding.alarm.layoutParams as LinearLayout.LayoutParams).apply {
                        gravity = alarmGravity.value()
                    }
            }

            dailyWordAlignment.observe(viewLifecycleOwner) { dailyWordGravity ->
                binding.dailyWord.gravity = dailyWordGravity.value()

                // Set layout_gravity to align the TextView (alarm) within the parent (LinearLayout)
                binding.dailyWord.layoutParams =
                    (binding.dailyWord.layoutParams as LinearLayout.LayoutParams).apply {
                        gravity = dailyWordGravity.value()
                    }
            }

            homeAppsAlignment.observe(viewLifecycleOwner) { (homeAppsGravity, onBottom) ->
                val horizontalAlignment = if (onBottom) Gravity.BOTTOM else Gravity.CENTER_VERTICAL
                binding.homeAppsLayout.gravity = homeAppsGravity.value() or horizontalAlignment

                binding.homeAppsLayout.children.forEach { view ->
                    if (prefs.appUsageStats) {
                        (view as LinearLayout).gravity = homeAppsGravity.value()
                    } else {
                        (view as TextView).gravity = homeAppsGravity.value()
                    }
                }
            }
        }
    }

    private fun homeAppClicked(location: Int) {
        CrashHandler.logUserAction("Clicked Home App: $location")
        if (prefs.getAppName(location).isEmpty()) showLongPressToast()
        else viewModel.launchApp(prefs.getHomeAppModel(location), this)
    }

    private fun showAppList(flag: AppDrawerFlag, includeHiddenApps: Boolean = false, n: Int = 0) {
        viewModel.getAppList(includeHiddenApps, flag)
        try {
            if (findNavController().currentDestination?.id == R.id.mainFragment) {
                findNavController().navigate(
                    R.id.action_mainFragment_to_appListFragment,
                    bundleOf("flag" to flag.toString(), "n" to n)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showNotesManager() {
        CrashHandler.logUserAction("Display Notes Manager")
        try {
            if (findNavController().currentDestination?.id == R.id.mainFragment) {
                findNavController().navigate(R.id.action_mainFragment_to_notesManagerFragment)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("PrivateApi")
    private fun expandNotificationDrawer(context: Context) {
        try {
            Class.forName("android.app.StatusBarManager")
                .getMethod("expandNotificationsPanel")
                .invoke(context.getSystemService("statusbar"))
        } catch (exception: Exception) {
            initActionService(requireContext())?.openNotifications()
            exception.printStackTrace()
        }
        CrashHandler.logUserAction("Expand Notification Drawer")
    }

    @SuppressLint("PrivateApi")
    private fun expandQuickSettings(context: Context) {
        try {
            Class.forName("android.app.StatusBarManager")
                .getMethod("expandSettingsPanel")
                .invoke(context.getSystemService("statusbar"))
        } catch (exception: Exception) {
            initActionService(requireContext())?.openQuickSettings()
            exception.printStackTrace()
        }
        CrashHandler.logUserAction("Expand Quick Settings")
    }

    private fun openSwipeUpApp() {
        CrashHandler.logUserAction("Open Swipe Up App")
        if (prefs.appShortSwipeUp.activityPackage.isNotEmpty())
            viewModel.launchApp(prefs.appShortSwipeUp, this)
        else
            requireContext().openDeviceSettings()
    }

    private fun openSwipeDownApp() {
        CrashHandler.logUserAction("Open Swipe Down App")
        if (prefs.appShortSwipeDown.activityPackage.isNotEmpty())
            viewModel.launchApp(prefs.appShortSwipeDown, this)
        else
            requireContext().openDialerApp()
    }

    private fun openSwipeLeftApp() {
        CrashHandler.logUserAction("Open Swipe Left App")
        if (prefs.appShortSwipeLeft.activityPackage.isNotEmpty())
            viewModel.launchApp(prefs.appShortSwipeLeft, this)
        else
            requireContext().openDeviceSettings()
    }

    private fun openSwipeRightApp() {
        CrashHandler.logUserAction("Open Swipe Right App")
        if (prefs.appShortSwipeRight.activityPackage.isNotEmpty())
            viewModel.launchApp(prefs.appShortSwipeRight, this)
        else
            requireContext().openDialerApp()
    }

    private fun openLongSwipeUpApp() {
        CrashHandler.logUserAction("Open Swipe Long Up App")
        if (prefs.appLongSwipeUp.activityPackage.isNotEmpty())
            viewModel.launchApp(prefs.appLongSwipeUp, this)
        else
            requireContext().openDeviceSettings()
    }

    private fun openLongSwipeDownApp() {
        CrashHandler.logUserAction("Open Swipe Long Down App")
        if (prefs.appLongSwipeDown.activityPackage.isNotEmpty())
            viewModel.launchApp(prefs.appLongSwipeDown, this)
        else
            requireContext().openDialerApp()
    }

    private fun openLongSwipeLeftApp() {
        CrashHandler.logUserAction("Open Swipe Long Left App")
        if (prefs.appLongSwipeLeft.activityPackage.isNotEmpty())
            viewModel.launchApp(prefs.appLongSwipeLeft, this)
        else
            requireContext().openDeviceSettings()
    }

    private fun openLongSwipeRightApp() {
        CrashHandler.logUserAction("Open Swipe Long Right App")
        if (prefs.appLongSwipeRight.activityPackage.isNotEmpty())
            viewModel.launchApp(prefs.appLongSwipeRight, this)
        else
            requireContext().openDialerApp()
    }

    private fun openClickClockApp() {
        CrashHandler.logUserAction("Open Clock App")
        if (prefs.appClickClock.activityPackage.isNotEmpty())
            viewModel.launchApp(prefs.appClickClock, this)
        else
            requireContext().openAlarmApp()
    }

    private fun openClickUsageApp() {
        CrashHandler.logUserAction("Open Usage App")
        if (prefs.appClickUsage.activityPackage.isNotEmpty())
            viewModel.launchApp(prefs.appClickUsage, this)
        else
            requireContext().openDigitalWellbeing()
    }

    private fun openClickDateApp() {
        CrashHandler.logUserAction("Open Date App")
        if (prefs.appClickDate.activityPackage.isNotEmpty())
            viewModel.launchApp(prefs.appClickDate, this)
        else
            requireContext().launchCalendar()
    }

    private fun openDoubleTapApp() {
        CrashHandler.logUserAction("Open Double Tap App")
        if (prefs.appDoubleTap.activityPackage.isNotEmpty())
            viewModel.launchApp(prefs.appDoubleTap, this)
        else
            AppReloader.restartApp(requireContext())
    }

    private fun openFabActionApp() {
        CrashHandler.logUserAction("Open Fab App")
        if (prefs.appFloating.activityPackage.isNotEmpty())
            viewModel.launchApp(prefs.appFloating, this)
        else
            findNavController().navigate(R.id.action_mainFragment_to_notesManagerFragment)
    }

    // This function handles all swipe actions that a independent of the actual swipe direction
    @SuppressLint("NewApi")
    private fun handleOtherAction(action: Action) {
        when (action) {
            Action.ShowNotification -> expandNotificationDrawer(requireContext())
            Action.LockScreen -> lockPhone()
            Action.TogglePrivateSpace -> PrivateSpaceManager(requireContext()).togglePrivateSpaceLock(
                showToast = true,
                launchSettings = true
            )

            Action.ShowAppList -> showAppList(AppDrawerFlag.LaunchApp, includeHiddenApps = false)
            Action.ShowNotesManager -> showNotesManager()
            Action.ShowDigitalWellbeing -> requireContext().openDigitalWellbeing()
            Action.OpenApp -> {} // this should be handled in the respective onSwipe[Up,Down,Right,Left] functions
            Action.OpenQuickSettings -> expandQuickSettings(requireContext())
            Action.ShowRecents -> initActionService(requireContext())?.showRecents()
            Action.OpenPowerDialog -> initActionService(requireContext())?.openPowerDialog()
            Action.TakeScreenShot -> initActionService(requireContext())?.takeScreenShot()
            Action.LeftPage -> handleSwipeLeft(prefs.homePagesNum)
            Action.RightPage -> handleSwipeRight(prefs.homePagesNum)
            Action.RestartApp -> AppReloader.restartApp(requireContext())
            Action.Disabled -> {}

        }
    }

    private fun lockPhone() {
        val actionService = ActionService.instance()
        if (actionService != null) {
            actionService.lockScreen()
        } else {
            requireContext().openAccessibilitySettings()
        }
    }

    private fun showLongPressToast() =
        showShortToast(getLocalizedString(R.string.long_press_to_select_app))

    private fun textOnClick(view: View) = onClick(view)

    private fun textOnLongClick(view: View) = onLongClick(view)

    private fun getHomeScreenGestureListener(context: Context): View.OnTouchListener {
        return object : OnSwipeTouchListener(context) {
            private var startX = 0f
            private var startY = 0f
            private var startTime: Long = 0
            private var longSwipeTriggered = false // Flag to indicate if onLongSwipe was triggered

            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = motionEvent.x
                        startY = motionEvent.y
                        startTime = System.currentTimeMillis()
                        longSwipeTriggered = false
                    }

                    MotionEvent.ACTION_UP -> {
                        val endX = motionEvent.x
                        val endY = motionEvent.y
                        val endTime = System.currentTimeMillis()
                        val deltaX = endX - startX
                        val deltaY = endY - startY
                        val distance = calculateDistance(deltaX, deltaY)
                        val duration = endTime - startTime
                        val direction = determineSwipeDirection(deltaX, deltaY)

                        val isLongSwipe = isLongSwipe(direction, duration, distance)

                        if (isLongSwipe) {
                            onLongSwipe(direction)
                            longSwipeTriggered = true
                        }
                    }
                }
                return !longSwipeTriggered && super.onTouch(view, motionEvent)
            }


            override fun onSwipeLeft() {
                super.onSwipeLeft()
                when (val action = prefs.shortSwipeLeftAction) {
                    Action.OpenApp -> openSwipeLeftApp()
                    else -> handleOtherAction(action)
                }
                CrashHandler.logUserAction("SwipeLeft Gesture")
            }

            override fun onSwipeRight() {
                super.onSwipeRight()
                when (val action = prefs.shortSwipeRightAction) {
                    Action.OpenApp -> openSwipeRightApp()
                    else -> handleOtherAction(action)
                }
                CrashHandler.logUserAction("SwipeRight Gesture")
            }

            override fun onSwipeUp() {
                super.onSwipeUp()
                when (val action = prefs.shortSwipeUpAction) {
                    Action.OpenApp -> openSwipeUpApp()
                    else -> handleOtherAction(action)
                }
                CrashHandler.logUserAction("SwipeUp Gesture")
            }

            override fun onSwipeDown() {
                super.onSwipeDown()
                when (val action = prefs.shortSwipeDownAction) {
                    Action.OpenApp -> openSwipeDownApp()
                    else -> handleOtherAction(action)
                }
                CrashHandler.logUserAction("SwipeDown Gesture")
            }

            override fun onLongClick() {
                super.onLongClick()
                CrashHandler.logUserAction("Launcher Settings Opened")
                trySettings()
            }

            override fun onDoubleClick() {
                super.onDoubleClick()
                when (val action = prefs.doubleTapAction) {
                    Action.OpenApp -> openDoubleTapApp()
                    else -> handleOtherAction(action)
                }
                CrashHandler.logUserAction("DoubleClick Gesture")
            }
        }
    }

    private fun getHomeAppsGestureListener(context: Context, view: View): View.OnTouchListener {
        return object : ViewSwipeTouchListener(context, view) {
            private var startX = 0f
            private var startY = 0f
            private var startTime: Long = 0
            private var longSwipeTriggered = false

            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = motionEvent.x
                        startY = motionEvent.y
                        startTime = System.currentTimeMillis()
                        longSwipeTriggered = false
                    }

                    MotionEvent.ACTION_UP -> {
                        val endX = motionEvent.x
                        val endY = motionEvent.y
                        val endTime = System.currentTimeMillis()
                        val deltaX = endX - startX
                        val deltaY = endY - startY
                        val distance = calculateDistance(deltaX, deltaY)
                        val duration = endTime - startTime
                        val direction = determineSwipeDirection(deltaX, deltaY)

                        val isLongSwipe = isLongSwipe(direction, duration, distance)

                        if (isLongSwipe) {
                            onLongSwipe(direction)
                            longSwipeTriggered = true
                        }
                    }
                }
                return !longSwipeTriggered && super.onTouch(view, motionEvent)
            }

            override fun onLongClick(view: View) {
                super.onLongClick(view)
                textOnLongClick(view)
            }

            override fun onClick(view: View) {
                super.onClick(view)
                textOnClick(view)
            }

            // Short swipe handling for various directions
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                when (val action = prefs.shortSwipeLeftAction) {
                    Action.OpenApp -> openSwipeLeftApp()
                    else -> handleOtherAction(action)
                }
                CrashHandler.logUserAction("SwipeLeft Gesture")
            }

            override fun onSwipeRight() {
                super.onSwipeRight()
                when (val action = prefs.shortSwipeRightAction) {
                    Action.OpenApp -> openSwipeRightApp()
                    else -> handleOtherAction(action)
                }
                CrashHandler.logUserAction("SwipeRight Gesture")
            }

            override fun onSwipeUp() {
                super.onSwipeUp()
                when (val action = prefs.shortSwipeUpAction) {
                    Action.OpenApp -> openSwipeUpApp()
                    else -> handleOtherAction(action)
                }
                CrashHandler.logUserAction("SwipeUp Gesture")
            }

            override fun onSwipeDown() {
                super.onSwipeDown()
                when (val action = prefs.shortSwipeDownAction) {
                    Action.OpenApp -> openSwipeDownApp()
                    else -> handleOtherAction(action)
                }
                CrashHandler.logUserAction("SwipeDown Gesture")
            }
        }
    }

    // Helper method to calculate distance of swipe
    private fun calculateDistance(deltaX: Float, deltaY: Float): Float {
        return sqrt((deltaX * deltaX + deltaY * deltaY).toDouble()).toFloat()
    }

    // Helper method to determine the swipe direction
    private fun determineSwipeDirection(deltaX: Float, deltaY: Float): String {
        return if (abs(deltaX) < abs(deltaY)) {
            if (deltaY < 0) "up" else "down"
        } else {
            if (deltaX < 0) "left" else "right"
        }
    }

    // Helper method to check if the swipe is long based on duration and distance
    private fun isLongSwipe(direction: String, duration: Long, distance: Float): Boolean {
        val holdDurationThreshold = Constants.HOLD_DURATION_THRESHOLD
        Constants.updateSwipeDistanceThreshold(requireContext(), direction)
        val swipeDistanceThreshold = Constants.SWIPE_DISTANCE_THRESHOLD

        return duration <= holdDurationThreshold && distance >= swipeDistanceThreshold
    }

    private fun onLongSwipe(direction: String) {
        when (direction) {
            "up" -> when (val action = prefs.longSwipeUpAction) {
                Action.OpenApp -> openLongSwipeUpApp()
                else -> handleOtherAction(action)
            }

            "down" -> when (val action = prefs.longSwipeDownAction) {
                Action.OpenApp -> openLongSwipeDownApp()
                else -> handleOtherAction(action)
            }

            "left" -> when (val action = prefs.longSwipeLeftAction) {
                Action.OpenApp -> openLongSwipeLeftApp()
                else -> handleOtherAction(action)
            }

            "right" -> when (val action = prefs.longSwipeRightAction) {
                Action.OpenApp -> openLongSwipeRightApp()
                else -> handleOtherAction(action)
            }
        }
        CrashHandler.logUserAction("LongSwipe_${direction} Gesture")
    }


    @SuppressLint("InflateParams")
    private fun updateAppCountWithUsageStats(newAppsNum: Int) {
        val appUsageMonitor = AppUsageMonitor.getInstance(requireContext())
        val oldAppsNum = binding.homeAppsLayout.childCount // current number of apps
        val diff = newAppsNum - oldAppsNum

        if (diff > 0) {
            // Add new apps
            for (i in oldAppsNum until newAppsNum) {
                // Create a horizontal LinearLayout to hold both existingAppView and newAppView
                val parentLinearLayout = LinearLayout(context)
                parentLinearLayout.apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, // Use MATCH_PARENT for full width
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }

                // Create existingAppView
                val existingAppView =
                    layoutInflater.inflate(R.layout.home_app_button, null) as TextView
                existingAppView.apply {
                    // Set properties of existingAppView
                    textSize = prefs.appSize.toFloat()
                    id = i
                    text = prefs.getHomeAppModel(i).activityLabel
                    setOnTouchListener(getHomeAppsGestureListener(context, this))
                    setOnClickListener(this@HomeFragment)
                    if (!prefs.extendHomeAppsArea) {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }
                    val padding: Int = prefs.textPaddingSize
                    setPadding(0, padding, 0, padding)
                    setTextColor(prefs.appColor)
                }

                // Create newAppView
                val newAppView = TextView(context)
                newAppView.apply {
                    // Set properties of newAppView
                    textSize = prefs.appSize.toFloat() / 1.5f
                    id = i
                    text = formatMillisToHMS(
                        appUsageMonitor.getUsageStats(
                            context,
                            prefs.getHomeAppModel(i).activityPackage
                        ), false
                    )
                    setOnTouchListener(getHomeAppsGestureListener(context, this))
                    setOnClickListener(this@HomeFragment)
                    if (!prefs.extendHomeAppsArea) {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }
                    val padding: Int = prefs.textPaddingSize
                    setPadding(0, padding, 0, padding)
                    setTextColor(prefs.appColor)
                }

                // Add a space between existingAppView and newAppView
                val space = Space(context)
                space.layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f // Weight to fill available space
                )

                // Add existingAppView to parentLinearLayout
                parentLinearLayout.addView(existingAppView)
                // Add space and newAppView to parentLinearLayout
                parentLinearLayout.addView(space)
                parentLinearLayout.addView(newAppView)

                // Add parentLinearLayout to homeAppsLayout
                binding.homeAppsLayout.addView(parentLinearLayout)
            }
        } else if (diff < 0) {
            // Remove extra apps
            binding.homeAppsLayout.removeViews(oldAppsNum + diff, -diff)
        }

        // Create a new TextView instance
        val totalText = getLocalizedString(R.string.show_total_screen_time)
        val totalTime = appUsageMonitor.getTotalScreenTime(requireContext())
        val totalScreenTime = formatMillisToHMS(totalTime, true)
        Log.d("totalScreenTime", totalScreenTime)
        val totalScreenTimeJoin = "$totalText: $totalScreenTime"
        // Set properties for the TextView (optional)
        binding.totalScreenTime.apply {
            text = totalScreenTimeJoin
            if (totalTime > 300L) { // Checking if totalTime is greater than 5 minutes (300,000 milliseconds)
                visibility = View.VISIBLE
            }
        }

        // Update the total number of pages and calculate maximum apps per page
        updatePagesAndAppsPerPage(prefs.homeAppsNum, prefs.homePagesNum)
        adjustTextViewMargins()
    }

    private fun adjustTextViewMargins() {
        binding.apply {
            val homeAppsLayout = homeAppsLayout
            val homeScreenPager = homeScreenPager
            val totalScreenTime = totalScreenTime
            val setDefaultLauncher = setDefaultLauncher
            val fabLayout = fabLayout

            val views = listOf(
                setDefaultLauncher,
                totalScreenTime,
                homeScreenPager,
                fabLayout,
                homeAppsLayout
            )

            // Check if device is using gesture navigation or 3-button navigation
            val isGestureNav = isGestureNavigationEnabled(requireContext())

            val numOfElements = 5
            val incrementBy = 35
            // Set margins based on navigation mode
            val margins = if (isGestureNav) {
                val startAt = 50
                List(numOfElements) { index -> startAt + (index * incrementBy) } // Adjusted margins for gesture navigation
            } else {
                val startAt = 100
                List(numOfElements) { index -> startAt + (index * incrementBy) } // Adjusted margins for 3-button navigation
            }

            val visibleViews = views.filter { it.isVisible }
            val visibleMargins =
                margins.take(visibleViews.size) // Trim margins list to match visible views

            // Reset margins for all views
            views.forEach { view ->
                val params = view.layoutParams as ViewGroup.MarginLayoutParams
                params.bottomMargin = 0
                view.layoutParams = params
            }

            // Apply correct spacing for visible views
            visibleViews.forEachIndexed { index, view ->
                val params = view.layoutParams as ViewGroup.MarginLayoutParams
                var bottomMargin = visibleMargins.getOrElse(index) { 0 }

                // Add extra space above fabLayout if it's visible
                if (prefs.homeAlignmentBottom) {
                    if (visibleViews.contains(fabLayout)) {
                        if (view == homeAppsLayout) {
                            bottomMargin += 65
                        }
                    }
                }

                if (view == homeScreenPager) {
                    bottomMargin += 10
                }

                params.bottomMargin = bottomMargin
                view.layoutParams = params
            }
        }
    }


    @SuppressLint("InflateParams", "DiscouragedApi", "UseCompatLoadingForDrawables")
    private fun updateAppCount(newAppsNum: Int) {
        val oldAppsNum = binding.homeAppsLayout.childCount // current number of apps
        val diff = newAppsNum - oldAppsNum

        if (diff > 0) {
            // Add new apps
            for (i in oldAppsNum until newAppsNum) {
                val view = layoutInflater.inflate(R.layout.home_app_button, null) as TextView
                view.apply {
                    textSize = prefs.appSize.toFloat()
                    id = i
                    text = prefs.getHomeAppModel(i).activityLabel
                    setOnTouchListener(getHomeAppsGestureListener(context, this))
                    setOnClickListener(this@HomeFragment)

                    if (!prefs.extendHomeAppsArea) {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }

                    gravity = prefs.homeAlignment.value()
                    isFocusable = true
                    isFocusableInTouchMode = true

                    val padding: Int = prefs.textPaddingSize
                    setPadding(0, padding, 0, padding)
                    setTextColor(prefs.appColor)

                    val packageName = prefs.getHomeAppModel(i).activityPackage
                    val packageManager = context.packageManager

                    if (packageName.isNotBlank() && prefs.iconPack != Constants.IconPacks.Disabled) {
                        val iconPackPackage = prefs.customIconPack
                        // Get app icon or fallback drawable
                        val icon: Drawable? = try {
                            if (iconPackPackage.isNotEmpty() && prefs.iconPack == Constants.IconPacks.Custom) {
                                if (IconPackHelper.isReady()) {
                                    IconPackHelper.getCachedIcon(context, packageName)
                                    // Use the icon if not null
                                } else {
                                    packageManager.getApplicationIcon(packageName)
                                }
                            } else {
                                packageManager.getApplicationIcon(packageName)
                            }
                        } catch (e: PackageManager.NameNotFoundException) {
                            e.printStackTrace()
                            // Handle exception gracefully, fall back to the system icon
                            packageManager.getApplicationIcon(packageName)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            // Handle any other exceptions gracefully, fallback to the system icon
                            packageManager.getApplicationIcon(packageName)
                        }

                        val defaultIcon = packageManager.getApplicationIcon(packageName)
                        val nonNullDrawable: Drawable = icon ?: defaultIcon

                        // Recolor the icon with the dominant color
                        val appNewIcon: Drawable? = getSystemIcons(context, prefs, nonNullDrawable)

                        // Set the icon size to match text size and add padding
                        val iconSize = (prefs.appSize * 1.4).toInt()  // Base size from preferences
                        val iconPadding = (iconSize / 1.2).toInt() //

                        appNewIcon?.setBounds(0, 0, iconSize, iconSize)
                        nonNullDrawable.setBounds(
                            0,
                            0,
                            ((iconSize * 1.8).toInt()),
                            ((iconSize * 1.8).toInt())
                        )

                        // Set drawable position based on alignment
                        when (prefs.homeAlignment) {
                            Constants.Gravity.Left -> {
                                setCompoundDrawables(
                                    appNewIcon ?: nonNullDrawable,
                                    null,
                                    null,
                                    null
                                )
                                // Add padding between text and icon if an icon is set
                                compoundDrawablePadding = iconPadding
                            }

                            Constants.Gravity.Right -> {
                                setCompoundDrawables(
                                    null,
                                    null,
                                    appNewIcon ?: nonNullDrawable,
                                    null
                                )
                                // Add padding between text and icon if an icon is set
                                compoundDrawablePadding = iconPadding
                            }

                            else -> setCompoundDrawables(null, null, null, null)
                        }
                    }
                }
                // Add the view to the layout
                binding.homeAppsLayout.addView(view)
            }
        } else if (diff < 0) {
            // Remove extra apps
            binding.homeAppsLayout.removeViews(oldAppsNum + diff, -diff)
        }

        // Update the total number of pages and calculate maximum apps per page
        updatePagesAndAppsPerPage(prefs.homeAppsNum, prefs.homePagesNum)
        adjustTextViewMargins()
    }


    // updates number of apps visible on home screen
    // does nothing if number has not changed
    private var currentPage = 0
    private var appsPerPage = 0

    private fun updatePagesAndAppsPerPage(totalApps: Int, totalPages: Int) {
        // Calculate the maximum number of apps per page
        appsPerPage = if (totalPages > 0) {
            (totalApps + totalPages - 1) / totalPages // Round up to ensure all apps are displayed
        } else {
            0 // Return 0 if totalPages is 0 to avoid division by zero
        }

        // Update app visibility based on the current page and calculated apps per page
        updateAppsVisibility(totalPages)
    }

    private fun updateAppsVisibility(totalPages: Int) {
        val startIdx = currentPage * appsPerPage
        val endIdx = minOf((currentPage + 1) * appsPerPage, getTotalAppsCount())

        for (i in 0 until getTotalAppsCount()) {
            val view = binding.homeAppsLayout.getChildAt(i)
            view.visibility = if (i in startIdx until endIdx) View.VISIBLE else View.GONE
        }

        val pageSelectorIcons = MutableList(totalPages) { _ -> R.drawable.ic_new_page }
        pageSelectorIcons[currentPage] = R.drawable.ic_current_page

        val spannable = SpannableStringBuilder()

        pageSelectorIcons.forEach { drawableRes ->
            val drawable = ContextCompat.getDrawable(requireContext(), drawableRes)?.apply {
                setBounds(0, 0, intrinsicWidth, intrinsicHeight)
                val colorFilterColor: ColorFilter =
                    PorterDuffColorFilter(prefs.appColor, PorterDuff.Mode.SRC_IN)
                colorFilter = colorFilterColor
            }
            val imageSpan = drawable?.let { ImageSpan(it, ImageSpan.ALIGN_BASELINE) }

            val placeholder = SpannableString(" ") // Placeholder for the image
            imageSpan?.let { placeholder.setSpan(it, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) }

            spannable.append(placeholder)
            spannable.append(" ") // Add space between icons
        }

        // Set the text for the page selector corresponding to each page
        binding.homeScreenPager.text = spannable
        if (prefs.homePagesNum > 1 && prefs.homePager) binding.homeScreenPager.visibility =
            View.VISIBLE
        if (prefs.showFloating) binding.fabLayout.visibility = View.VISIBLE
    }

    private fun handleSwipeLeft(totalPages: Int) {
        if (totalPages <= 0) return // Prevent issues if totalPages is 0 or negative

        currentPage = if (currentPage == 0) {
            totalPages - 1 // Wrap to last page if on the first page
        } else {
            currentPage - 1 // Move to the previous page
        }

        updateAppsVisibility(totalPages)
    }

    private fun handleSwipeRight(totalPages: Int) {
        if (totalPages <= 0) return // Prevent issues if totalPages is 0 or negative

        currentPage = if (currentPage == totalPages - 1) {
            0 // Wrap to first page if on the last page
        } else {
            currentPage + 1 // Move to the next page
        }

        updateAppsVisibility(totalPages)
    }


    private fun getTotalAppsCount(): Int {
        return binding.homeAppsLayout.childCount
    }

    private fun trySettings() {
        lifecycleScope.launch(Dispatchers.Main) {
            if (prefs.settingsLocked) {
                biometricHelper.startBiometricSettingsAuth(object :
                    BiometricHelper.CallbackSettings {
                    override fun onAuthenticationSucceeded() {
                        sendToSettingFragment()
                    }

                    override fun onAuthenticationFailed() {
                        Log.e(
                            "Authentication",
                            getLocalizedString(R.string.text_authentication_failed)
                        )
                    }

                    override fun onAuthenticationError(
                        errorCode: Int,
                        errorMessage: CharSequence?
                    ) {
                        when (errorCode) {
                            BiometricPrompt.ERROR_USER_CANCELED -> Log.e(
                                "Authentication",
                                getLocalizedString(R.string.text_authentication_cancel)
                            )

                            else ->
                                Log.e(
                                    "Authentication",
                                    getLocalizedString(R.string.text_authentication_error).format(
                                        errorMessage,
                                        errorCode
                                    )
                                )
                        }
                    }
                })
            } else {
                sendToSettingFragment()
            }
        }
    }

    private fun sendToSettingFragment() {
        try {
            findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
            viewModel.firstOpen(false)
        } catch (e: java.lang.Exception) {
            Log.d("onLongClick", e.toString())
        }
    }

    @SuppressLint("MissingPermission")
    private fun getWeather() {
        val locationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requireContext().requestLocationPermission(Constants.ACCESS_FINE_LOCATION)
            return
        }

        val provider = when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> {
                Log.e("WeatherReceiver", "No location provider enabled.")
                return
            }
        }

        // ✅ Try last known location first
        val lastKnown = locationManager.getLastKnownLocation(provider)
        if (lastKnown != null) {
            handleLocation(lastKnown)
            return
        }

        // 🔁 Fallback: wait for real-time update
        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                locationManager.removeUpdates(this)
                handleLocation(location)
            }

            override fun onProviderDisabled(provider: String) {}
            override fun onProviderEnabled(provider: String) {}

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            }
        }

        locationManager.requestLocationUpdates(
            provider,
            0L,
            0f,
            locationListener,
            Looper.getMainLooper()
        )

        // ⏱ Optional: timeout fallback in case no update happens
        Handler(Looper.getMainLooper()).postDelayed({
            locationManager.removeUpdates(locationListener)
            Log.w("WeatherReceiver", "Location update timed out.")
        }, 10000) // 10 seconds
    }

    private fun handleLocation(location: Location) {
        val lat = location.latitude
        val lon = location.longitude
        Log.d("WeatherReceiver", "Location: $lat, $lon")

        val receiver = WeatherReceiver()
        viewLifecycleOwner.lifecycleScope.launch {
            val weatherReceiver = receiver.getCurrentWeather(lat, lon)
            val weatherType =
                receiver.getWeatherEmoji(weatherReceiver?.currentWeather?.weatherCode ?: -1)

            binding.apply {
                if (weatherReceiver != null) {
                    weather.textSize = prefs.batterySize.toFloat()
                    weather.setTextColor(prefs.batteryColor)
                    weather.text = String.format(
                        "%s %s%s",
                        weatherType,
                        weatherReceiver.currentWeather.temperature,
                        weatherReceiver.currentUnits.temperatureUnit
                    )
                    weather.visibility = View.VISIBLE
                }
            }
        }
    }

}
