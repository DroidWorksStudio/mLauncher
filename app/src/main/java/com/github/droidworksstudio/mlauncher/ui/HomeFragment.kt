package com.github.droidworksstudio.mlauncher.ui

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
import android.os.Build
import android.os.Bundle
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
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.github.droidworksstudio.common.ColorIconsExtensions
import com.github.droidworksstudio.common.CrashHandler
import com.github.droidworksstudio.common.launchCalendar
import com.github.droidworksstudio.common.openAccessibilitySettings
import com.github.droidworksstudio.common.openAlarmApp
import com.github.droidworksstudio.common.openBatteryManager
import com.github.droidworksstudio.common.openCameraApp
import com.github.droidworksstudio.common.openDialerApp
import com.github.droidworksstudio.common.openDigitalWellbeing
import com.github.droidworksstudio.common.showLongToast
import com.github.droidworksstudio.common.showShortToast
import com.github.droidworksstudio.mlauncher.MainViewModel
import com.github.droidworksstudio.mlauncher.R
import com.github.droidworksstudio.mlauncher.data.AppListItem
import com.github.droidworksstudio.mlauncher.data.Constants
import com.github.droidworksstudio.mlauncher.data.Constants.Action
import com.github.droidworksstudio.mlauncher.data.Constants.AppDrawerFlag
import com.github.droidworksstudio.mlauncher.data.Prefs
import com.github.droidworksstudio.mlauncher.databinding.FragmentHomeBinding
import com.github.droidworksstudio.mlauncher.helper.ActionService
import com.github.droidworksstudio.mlauncher.helper.AppDetailsHelper.formatMillisToHMS
import com.github.droidworksstudio.mlauncher.helper.AppDetailsHelper.getTotalScreenTime
import com.github.droidworksstudio.mlauncher.helper.AppDetailsHelper.getUsageStats
import com.github.droidworksstudio.mlauncher.helper.AppReloader
import com.github.droidworksstudio.mlauncher.helper.BatteryReceiver
import com.github.droidworksstudio.mlauncher.helper.PrivateSpaceReceiver
import com.github.droidworksstudio.mlauncher.helper.getHexForOpacity
import com.github.droidworksstudio.mlauncher.helper.getNextAlarm
import com.github.droidworksstudio.mlauncher.helper.hideStatusBar
import com.github.droidworksstudio.mlauncher.helper.initActionService
import com.github.droidworksstudio.mlauncher.helper.ismlauncherDefault
import com.github.droidworksstudio.mlauncher.helper.showStatusBar
import com.github.droidworksstudio.mlauncher.helper.togglePrivateSpaceLock
import com.github.droidworksstudio.mlauncher.helper.wordOfTheDay
import com.github.droidworksstudio.mlauncher.listener.OnSwipeTouchListener
import com.github.droidworksstudio.mlauncher.listener.ViewSwipeTouchListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

class HomeFragment : Fragment(), View.OnClickListener, View.OnLongClickListener {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var deviceManager: DevicePolicyManager
    private lateinit var batteryReceiver: BatteryReceiver
    private lateinit var privateSpaceReceiver: PrivateSpaceReceiver
    private lateinit var vibrator: Vibrator

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        val view = binding.root
        prefs = Prefs(requireContext())
        batteryReceiver = BatteryReceiver()
        privateSpaceReceiver = PrivateSpaceReceiver()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        deviceManager =
            context?.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        @Suppress("DEPRECATION")
        vibrator = context?.getSystemService(VIBRATOR_SERVICE) as Vibrator

        initObservers()
        initSwipeTouchListener()
        initClickListeners()
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onStart() {
        super.onStart()
        if (prefs.showStatusBar) showStatusBar(requireActivity()) else hideStatusBar(requireActivity())

        batteryReceiver = BatteryReceiver()
        /* register battery changes */
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            requireContext().registerReceiver(batteryReceiver, filter)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        privateSpaceReceiver = PrivateSpaceReceiver()
        /* register private Space changes */
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                val filter = IntentFilter(Intent.ACTION_PROFILE_AVAILABLE)
                requireContext().registerReceiver(privateSpaceReceiver, filter)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val timezone = prefs.appLanguage.timezone()
        val is24HourFormat = DateFormat.is24HourFormat(requireContext())
        val best12 = DateFormat.getBestDateTimePattern(
            timezone,
            if (prefs.showClockFormat) "hhmma" else "hhmm"
        ).let {
            if (!prefs.showClockFormat) it.removeSuffix(" a") else it
        }
        binding.apply {
            val best24 = DateFormat.getBestDateTimePattern(timezone, "HHmm")
            val timePattern = if (is24HourFormat) best24 else best12
            clock.format12Hour = timePattern
            clock.format24Hour = timePattern

            val datePattern = DateFormat.getBestDateTimePattern(timezone, "eeeddMMM")
            date.format12Hour = datePattern
            date.format24Hour = datePattern

            alarm.text = getNextAlarm(requireContext(), prefs)
            dailyWord.text = wordOfTheDay(requireContext(), prefs)

            date.textSize = prefs.dateSize.toFloat()
            clock.textSize = prefs.clockSize.toFloat()
            alarm.textSize = prefs.alarmSize.toFloat()
            dailyWord.textSize = prefs.dailyWordSize.toFloat()
            battery.textSize = prefs.batterySize.toFloat()
            homeScreenPager.textSize = prefs.appSize.toFloat()

            battery.visibility = if (prefs.showBattery) View.VISIBLE else View.GONE
            val backgroundColor = getHexForOpacity(prefs)
            mainLayout.setBackgroundColor(backgroundColor)

            date.setTextColor(prefs.dateColor)
            clock.setTextColor(prefs.clockColor)
            alarm.setTextColor(prefs.alarmClockColor)
            dailyWord.setTextColor(prefs.dailyWordColor)
            battery.setTextColor(prefs.batteryColor)
            totalScreenTime.setTextColor(prefs.appColor)
            setDefaultLauncher.setTextColor(prefs.appColor)
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            /* unregister battery changes if the receiver is registered */
            requireContext().unregisterReceiver(batteryReceiver)
            requireContext().unregisterReceiver(privateSpaceReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
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

            R.id.floatingActionButton -> {
                when (val action = prefs.clickFloatingAction) {
                    Action.OpenApp -> openFloatingActionApp()
                    else -> handleOtherAction(action)
                }
                CrashHandler.logUserAction("FloatingActionButton Clicked")
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

    private fun initClickListeners() {
        binding.apply {
            clock.setOnClickListener(this@HomeFragment)
            date.setOnClickListener(this@HomeFragment)
            totalScreenTime.setOnClickListener(this@HomeFragment)
            setDefaultLauncher.setOnClickListener(this@HomeFragment)
            battery.setOnClickListener(this@HomeFragment)
            floatingActionButton.setOnClickListener(this@HomeFragment)
        }
    }

    private fun initObservers() {
        binding.apply {
            if (prefs.firstSettingsOpen) {
                firstRunTips.visibility = View.VISIBLE
                setDefaultLauncher.visibility = View.GONE
            } else firstRunTips.visibility = View.GONE

            if (!ismlauncherDefault(requireContext())) {
                setDefaultLauncher.visibility = View.VISIBLE
            }
        }

        with(viewModel) {

            clockAlignment.observe(viewLifecycleOwner) { clockGravity ->
                binding.clock.gravity = clockGravity.value()

                // Set layout_gravity to align the TextClock (clock) within the parent (LinearLayout)
                binding.clock.layoutParams = (binding.clock.layoutParams as LinearLayout.LayoutParams).apply {
                    gravity = clockGravity.value()
                }
            }

            dateAlignment.observe(viewLifecycleOwner) { dateGravity ->
                binding.date.gravity = dateGravity.value()

                // Set layout_gravity to align the TextClock (date) within the parent (LinearLayout)
                binding.date.layoutParams = (binding.date.layoutParams as LinearLayout.LayoutParams).apply {
                    gravity = dateGravity.value()
                }
            }

            alarmAlignment.observe(viewLifecycleOwner) { alarmGravity ->
                binding.alarm.gravity = alarmGravity.value()

                // Set layout_gravity to align the TextView (alarm) within the parent (LinearLayout)
                binding.alarm.layoutParams = (binding.alarm.layoutParams as LinearLayout.LayoutParams).apply {
                    gravity = alarmGravity.value()
                }
            }

            dailyWordAlignment.observe(viewLifecycleOwner) { dailyWordGravity ->
                binding.dailyWord.gravity = dailyWordGravity.value()

                // Set layout_gravity to align the TextView (alarm) within the parent (LinearLayout)
                binding.dailyWord.layoutParams = (binding.dailyWord.layoutParams as LinearLayout.LayoutParams).apply {
                    gravity = dailyWordGravity.value()
                }
            }

            homeAppsAlignment.observe(viewLifecycleOwner) { (homeAppsGravity, onBottom) ->
                val horizontalAlignment = if (onBottom) Gravity.BOTTOM else Gravity.CENTER_VERTICAL
                binding.homeAppsLayout.gravity = homeAppsGravity.value() or horizontalAlignment

                binding.homeAppsLayout.children.forEach { view ->
                    (view as TextView).gravity = homeAppsGravity.value()
                }
            }
            homeAppsNum.observe(viewLifecycleOwner) {
                if (prefs.appUsageStats) {
                    updateAppCountWithUsageStats(it)
                } else {
                    updateAppCount(it)
                }
            }
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
            showFloating.observe(viewLifecycleOwner) {
                binding.floatingActionButton.visibility = if (it) View.VISIBLE else View.GONE
            }
        }
    }

    private fun homeAppClicked(location: Int) {
        if (prefs.getAppName(location).isEmpty()) showLongPressToast()
        else launchApp(prefs.getHomeAppModel(location))
    }

    private fun launchApp(app: AppListItem) {
        viewModel.selectedApp(app, AppDrawerFlag.LaunchApp)
        CrashHandler.logUserAction("${app.activityLabel} App Launched")
    }

    private fun showAppList(flag: AppDrawerFlag, includeHiddenApps: Boolean = false, n: Int = 0) {
        viewModel.getAppList(includeHiddenApps)
        try {
            if (findNavController().currentDestination?.id == R.id.mainFragment) {
                findNavController().navigate(
                    R.id.action_mainFragment_to_appListFragment,
                    bundleOf("flag" to flag.toString(), "n" to n)
                )
            }
        } catch (e: Exception) {
            if (findNavController().currentDestination?.id == R.id.mainFragment) {
                findNavController().navigate(
                    R.id.appListFragment,
                    bundleOf("flag" to flag.toString())
                )
            }
            e.printStackTrace()
        }
    }

    @SuppressLint("WrongConstant", "PrivateApi")
    private fun expandNotificationDrawer(context: Context) {
        try {
            Class.forName("android.app.StatusBarManager")
                .getMethod("expandNotificationsPanel")
                .invoke(context.getSystemService("statusbar"))
        } catch (exception: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                initActionService(requireContext())?.openNotifications()
            }
            exception.printStackTrace()
        }
        CrashHandler.logUserAction("Expand Notification Drawer")
    }

    @SuppressLint("WrongConstant", "PrivateApi")
    private fun expandQuickSettings(context: Context) {
        try {
            Class.forName("android.app.StatusBarManager")
                .getMethod("expandSettingsPanel")
                .invoke(context.getSystemService("statusbar"))
        } catch (exception: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                initActionService(requireContext())?.openQuickSettings()
            }
            exception.printStackTrace()
        }
        CrashHandler.logUserAction("Expand Quick Settings")
    }

    private fun openSwipeUpApp() {
        if (prefs.appShortSwipeUp.activityPackage.isNotEmpty())
            launchApp(prefs.appShortSwipeUp)
        else
            requireContext().openCameraApp()
    }

    private fun openSwipeDownApp() {
        if (prefs.appShortSwipeDown.activityPackage.isNotEmpty())
            launchApp(prefs.appShortSwipeDown)
        else
            requireContext().openDialerApp()
    }

    private fun openSwipeLeftApp() {
        if (prefs.appShortSwipeLeft.activityPackage.isNotEmpty())
            launchApp(prefs.appShortSwipeLeft)
        else
            requireContext().openCameraApp()
    }

    private fun openSwipeRightApp() {
        if (prefs.appShortSwipeRight.activityPackage.isNotEmpty())
            launchApp(prefs.appShortSwipeRight)
        else
            requireContext().openDialerApp()
    }

    private fun openLongSwipeUpApp() {
        if (prefs.appLongSwipeUp.activityPackage.isNotEmpty())
            launchApp(prefs.appLongSwipeUp)
        else
            requireContext().openCameraApp()
    }

    private fun openLongSwipeDownApp() {
        if (prefs.appLongSwipeDown.activityPackage.isNotEmpty())
            launchApp(prefs.appLongSwipeDown)
        else
            requireContext().openDialerApp()
    }

    private fun openLongSwipeLeftApp() {
        if (prefs.appLongSwipeLeft.activityPackage.isNotEmpty())
            launchApp(prefs.appLongSwipeLeft)
        else
            requireContext().openCameraApp()
    }

    private fun openLongSwipeRightApp() {
        if (prefs.appLongSwipeRight.activityPackage.isNotEmpty())
            launchApp(prefs.appLongSwipeRight)
        else
            requireContext().openDialerApp()
    }

    private fun openClickClockApp() {
        if (prefs.appClickClock.activityPackage.isNotEmpty())
            launchApp(prefs.appClickClock)
        else
            requireContext().openAlarmApp()
    }

    private fun openClickUsageApp() {
        if (prefs.appClickUsage.activityPackage.isNotEmpty())
            launchApp(prefs.appClickUsage)
        else
            requireContext().openDigitalWellbeing()
    }

    private fun openFloatingActionApp() {
        if (prefs.appFloating.activityPackage.isNotEmpty())
            launchApp(prefs.appFloating)
        else
            requireContext().openBatteryManager()
    }

    private fun openClickDateApp() {
        if (prefs.appClickDate.activityPackage.isNotEmpty())
            launchApp(prefs.appClickDate)
        else
            requireContext().launchCalendar()
    }

    private fun openDoubleTapApp() {
        if (prefs.appDoubleTap.activityPackage.isNotEmpty())
            launchApp(prefs.appDoubleTap)
        else
            AppReloader.restartApp(requireContext())
    }

    // This function handles all swipe actions that a independent of the actual swipe direction
    @SuppressLint("NewApi")
    private fun handleOtherAction(action: Action) {
        when (action) {
            Action.ShowNotification -> expandNotificationDrawer(requireContext())
            Action.LockScreen -> lockPhone()
            Action.TogglePrivateSpace -> togglePrivateSpaceLock(requireContext())
            Action.ShowAppList -> showAppList(AppDrawerFlag.LaunchApp, includeHiddenApps = false)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val actionService = ActionService.instance()
            if (actionService != null) {
                actionService.lockScreen()
            } else {
                requireContext().openAccessibilitySettings()
            }
        } else {
            requireActivity().runOnUiThread {
                try {
                    deviceManager.lockNow()
                } catch (_: SecurityException) {
                    showLongToast(
                        "App does not have the permission to lock the device"
                    )
                } catch (_: Exception) {
                    showLongToast(
                        "mLauncher failed to lock device.\nPlease check your app settings."
                    )
                    prefs.lockModeOn = false
                }
            }
        }
    }

    private fun showLongPressToast() = showShortToast(getString(R.string.long_press_to_select_app))

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
                        getUsageStats(
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

        // Update the total number of pages and calculate maximum apps per page
        updatePagesAndAppsPerPage(prefs.homeAppsNum, prefs.homePagesNum)

        // Create a new TextView instance
        val totalText = getString(R.string.show_total_screen_time)
        val totalTime = context?.let { getTotalScreenTime(it) }
        val totalScreenTime = totalTime?.let { formatMillisToHMS(it, true) }
        Log.d("totalScreenTime", "$totalScreenTime")
        val totalScreenTimeJoin = "$totalText: $totalScreenTime"
        // Set properties for the TextView (optional)
        binding.totalScreenTime.apply {
            text = totalScreenTimeJoin
            if (totalTime != null && totalTime > 300000L) { // Checking if totalTime is greater than 5 minutes (300,000 milliseconds)
                visibility = View.VISIBLE
            }
        }

    }


    @SuppressLint("InflateParams")
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
                        // Get app icon or fallback drawable
                        val icon: Drawable? = try {
                            packageManager.getApplicationIcon(packageName)
                        } catch (e: PackageManager.NameNotFoundException) {
                            null
                        }

                        val defaultIcon = ContextCompat.getDrawable(context, R.drawable.launcher_dot_icon)
                        val nonNullDrawable: Drawable = icon ?: defaultIcon!!

                        // Recolor the icon with the dominant color
                        val appNewIcon: Drawable? =
                            when (prefs.iconPack) {
                                Constants.IconPacks.EasyDots -> {
                                    val newIcon = ContextCompat.getDrawable(
                                        context,
                                        R.drawable.launcher_dot_icon
                                    )!!
                                    val bitmap = ColorIconsExtensions.drawableToBitmap(nonNullDrawable)
                                    val dominantColor = ColorIconsExtensions.getDominantColor(bitmap)
                                    ColorIconsExtensions.recolorDrawable(newIcon, dominantColor)
                                }

                                Constants.IconPacks.NiagaraDots -> {
                                    val newIcon = ContextCompat.getDrawable(
                                        context,
                                        R.drawable.niagara_dot_icon
                                    )!!
                                    val bitmap = ColorIconsExtensions.drawableToBitmap(nonNullDrawable)
                                    val dominantColor = ColorIconsExtensions.getDominantColor(bitmap)
                                    ColorIconsExtensions.recolorDrawable(newIcon, dominantColor)
                                }

                                else -> {
                                    null
                                }
                            }

                        // Set the icon size to match text size and add padding
                        val iconSize = (prefs.appSize * 1.4).toInt()  // Base size from preferences
                        val iconPadding = (iconSize / 1.2).toInt() //

                        appNewIcon?.setBounds(0, 0, iconSize, iconSize)
                        nonNullDrawable.setBounds(0, 0, ((iconSize * 1.8).toInt()), ((iconSize * 1.8).toInt()))

                        // Set drawable position based on alignment
                        when (prefs.homeAlignment) {
                            Constants.Gravity.Left -> setCompoundDrawables(appNewIcon ?: nonNullDrawable, null, null, null)
                            Constants.Gravity.Right -> setCompoundDrawables(null, null, appNewIcon ?: nonNullDrawable, null)
                            else -> setCompoundDrawables(null, null, null, null)
                        }

                        // Add padding between text and icon if an icon is set
                        if (prefs.homeAlignment == Constants.Gravity.Left || prefs.homeAlignment == Constants.Gravity.Right) {
                            compoundDrawablePadding = iconPadding
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
                val colorFilterColor: ColorFilter = PorterDuffColorFilter(prefs.appColor, PorterDuff.Mode.SRC_IN)
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
            biometricPrompt = BiometricPrompt(this@HomeFragment,
                ContextCompat.getMainExecutor(requireContext()),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence
                    ) {
                        when (errorCode) {
                            BiometricPrompt.ERROR_USER_CANCELED -> showLongToast(
                                getString(R.string.text_authentication_cancel)
                            )

                            else -> showLongToast(
                                getString(R.string.text_authentication_error).format(
                                    errString,
                                    errorCode
                                )
                            )
                        }
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        sendToSettingFragment()
                    }

                    override fun onAuthenticationFailed() {
                        showLongToast(
                            getString(R.string.text_authentication_failed)
                        )
                    }
                })

            promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.text_biometric_login))
                .setSubtitle(getString(R.string.text_biometric_login_sub))
                .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
                .setConfirmationRequired(false)
                .build()

            if (prefs.settingsLocked) {
                authenticate()
            } else {
                sendToSettingFragment()
            }
        }
    }

    private fun authenticate() {
        val code = BiometricManager.from(requireContext())
            .canAuthenticate(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
        when (code) {
            BiometricManager.BIOMETRIC_SUCCESS -> biometricPrompt.authenticate(promptInfo)
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> sendToSettingFragment()

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> sendToSettingFragment()

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> sendToSettingFragment()

            else -> showLongToast(getString(R.string.text_authentication_error))
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
}
