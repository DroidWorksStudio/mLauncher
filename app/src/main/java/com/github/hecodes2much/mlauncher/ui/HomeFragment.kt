package com.github.hecodes2much.mlauncher.ui

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Context.VIBRATOR_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.text.format.DateFormat
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.github.hecodes2much.mlauncher.MainViewModel
import com.github.hecodes2much.mlauncher.R
import com.github.hecodes2much.mlauncher.data.AppModel
import com.github.hecodes2much.mlauncher.data.Constants.Action
import com.github.hecodes2much.mlauncher.data.Constants.AppDrawerFlag
import com.github.hecodes2much.mlauncher.data.Prefs
import com.github.hecodes2much.mlauncher.databinding.FragmentHomeBinding
import com.github.hecodes2much.mlauncher.helper.ActionService
import com.github.hecodes2much.mlauncher.helper.BatteryReceiver
import com.github.hecodes2much.mlauncher.helper.getHexFontColor
import com.github.hecodes2much.mlauncher.helper.getHexForOpacity
import com.github.hecodes2much.mlauncher.helper.hideStatusBar
import com.github.hecodes2much.mlauncher.helper.initActionService
import com.github.hecodes2much.mlauncher.helper.ismlauncherDefault
import com.github.hecodes2much.mlauncher.helper.openAccessibilitySettings
import com.github.hecodes2much.mlauncher.helper.openAlarmApp
import com.github.hecodes2much.mlauncher.helper.openCalendar
import com.github.hecodes2much.mlauncher.helper.openCameraApp
import com.github.hecodes2much.mlauncher.helper.openDialerApp
import com.github.hecodes2much.mlauncher.helper.showStatusBar
import com.github.hecodes2much.mlauncher.helper.showToastLong
import com.github.hecodes2much.mlauncher.helper.showToastShort
import com.github.hecodes2much.mlauncher.listener.OnSwipeTouchListener
import com.github.hecodes2much.mlauncher.listener.ViewSwipeTouchListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeFragment : Fragment(), View.OnClickListener, View.OnLongClickListener {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var deviceManager: DevicePolicyManager
    private lateinit var batteryReceiver: BatteryReceiver
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

        return view
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val hex = getHexForOpacity(requireContext(), prefs)
        binding.mainLayout.setBackgroundColor(hex)

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
        val typeface = ResourcesCompat.getFont(requireActivity(), R.font.roboto)

        binding.clock.textSize = prefs.textSizeLauncher * 2.5f
        binding.date.textSize = prefs.textSizeLauncher.toFloat()
        binding.batteryIcon.textSize = prefs.textSizeLauncher.toFloat() / 1.5f
        binding.batteryText.textSize = prefs.textSizeLauncher.toFloat() / 1.5f

        if (prefs.useCustomIconFont) {
            binding.clock.typeface = typeface
            binding.date.typeface = typeface
            binding.batteryText.typeface = typeface
            binding.setDefaultLauncher.typeface = typeface
        }
        binding.batteryIcon.typeface = typeface

        val backgroundColor = getHexForOpacity(requireContext(), prefs)
        binding.mainLayout.setBackgroundColor(backgroundColor)
        if (prefs.followAccentColors) {
            val fontColor = getHexFontColor(requireContext())
            binding.clock.setTextColor(fontColor)
            binding.date.setTextColor(fontColor)
            binding.batteryIcon.setTextColor(fontColor)
            binding.batteryText.setTextColor(fontColor)
            binding.setDefaultLauncher.setTextColor(fontColor)
        }
    }

    override fun onResume() {
        super.onResume()

        batteryReceiver = BatteryReceiver()
        /* register battery changes */
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        requireContext().registerReceiver(batteryReceiver, filter)

        val locale = prefs.language.locale()
        val best12 = DateFormat.getBestDateTimePattern(locale, "hhmma")
        val best24 = DateFormat.getBestDateTimePattern(locale, "HHmm")
        binding.clock.format12Hour = best12
        binding.clock.format24Hour = best24

        val best12Date = DateFormat.getBestDateTimePattern(locale, "eeeddMMM")
        val best24Date = DateFormat.getBestDateTimePattern(locale, "eeeddMMM")
        binding.date.format12Hour = best12Date
        binding.date.format24Hour = best24Date

        // only show "set as default"-button if tips are GONE
        if (binding.firstRunTips.visibility == View.GONE) {
            binding.setDefaultLauncher.visibility =
                if (ismlauncherDefault(requireContext())) View.GONE else View.VISIBLE
        }

    }

    override fun onPause() {
        super.onPause()
        /* unregister battery changes */
        requireContext().unregisterReceiver(batteryReceiver)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.clock -> {
                when (val action = prefs.clickClockAction) {
                    Action.OpenApp -> openClickClockApp()
                    else -> handleOtherAction(action)
                }
            }

            R.id.date -> {
                when (val action = prefs.clickDateAction) {
                    Action.OpenApp -> openClickDateApp()
                    else -> handleOtherAction(action)
                }
            }

            R.id.setDefaultLauncher -> {
                viewModel.resetDefaultLauncherApp(requireContext())
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
        showAppList(AppDrawerFlag.SetHomeApp, includeHiddenApps = prefs.hiddenAppsDisplayed, n)
        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initSwipeTouchListener() {
        binding.touchArea.setOnTouchListener(getHomeScreenGestureListener(requireContext()))
    }

    private fun initClickListeners() {
        binding.clock.setOnClickListener(this)
        binding.date.setOnClickListener(this)
        binding.setDefaultLauncher.setOnClickListener(this)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun initObservers() {
        if (prefs.firstSettingsOpen) {
            binding.firstRunTips.visibility = View.VISIBLE
            binding.setDefaultLauncher.visibility = View.GONE
        } else binding.firstRunTips.visibility = View.GONE

        with(viewModel) {

            clockAlignment.observe(viewLifecycleOwner) { gravity ->
                binding.dateTimeLayout.gravity = gravity.value()
            }
            homeAppsAlignment.observe(viewLifecycleOwner) { (gravity, onBottom) ->
                val horizontalAlignment = if (onBottom) Gravity.BOTTOM else Gravity.CENTER_VERTICAL
                binding.homeAppsLayout.gravity = gravity.value() or horizontalAlignment

                binding.homeAppsLayout.children.forEach { view ->
                    (view as TextView).gravity = gravity.value()
                }
            }
            homeAppsCount.observe(viewLifecycleOwner) {
                updateAppCount(it)
            }
            showTime.observe(viewLifecycleOwner) {
                binding.clock.visibility = if (it) View.VISIBLE else View.GONE
            }
            showDate.observe(viewLifecycleOwner) {
                binding.date.visibility = if (it) View.VISIBLE else View.GONE
            }
        }
    }

    private fun homeAppClicked(location: Int) {
        if (prefs.getAppName(location).isEmpty()) showLongPressToast()
        else launchApp(prefs.getHomeAppModel(location))
    }

    private fun launchApp(appModel: AppModel) {
        viewModel.selectedApp(appModel, AppDrawerFlag.LaunchApp)
    }

    private fun showAppList(flag: AppDrawerFlag, includeHiddenApps: Boolean = false, n: Int = 0) {
        viewModel.getAppList(includeHiddenApps)
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                findNavController().navigate(
                    R.id.action_mainFragment_to_appListFragment,
                    bundleOf("flag" to flag.toString(), "n" to n)
                )
            } catch (e: Exception) {
                findNavController().navigate(
                    R.id.appListFragment,
                    bundleOf("flag" to flag.toString())
                )
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("WrongConstant", "PrivateApi")
    private fun expandNotificationDrawer(context: Context) {
        try {
            Class.forName("android.app.StatusBarManager")
                .getMethod("expandNotificationsPanel")
                .invoke(context.getSystemService("statusbar"))
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }

    @SuppressLint("WrongConstant", "PrivateApi")
    private fun expandQuickSettings(context: Context) {
        try {
            Class.forName("android.app.StatusBarManager")
                .getMethod("expandSettingsPanel")
                .invoke(context.getSystemService("statusbar"))
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }

    private fun openSwipeLeftApp() {
        if (prefs.appSwipeLeft.appPackage.isNotEmpty())
            launchApp(prefs.appSwipeLeft)
        else openCameraApp(requireContext())
    }

    private fun openSwipeRightApp() {
        if (prefs.appSwipeRight.appPackage.isNotEmpty())
            launchApp(prefs.appSwipeRight)
        else openDialerApp(requireContext())
    }

    private fun openSwipeDownApp() {
        if (prefs.appSwipeDown.appPackage.isNotEmpty())
            launchApp(prefs.appSwipeDown)
        else openDialerApp(requireContext())
    }

    private fun openSwipeUpApp() {
        if (prefs.appSwipeUp.appPackage.isNotEmpty())
            launchApp(prefs.appSwipeUp)
        else openDialerApp(requireContext())
    }

    private fun openClickClockApp() {
        if (prefs.appClickClock.appPackage.isNotEmpty())
            launchApp(prefs.appClickClock)
        else openAlarmApp(requireContext())
    }

    private fun openClickDateApp() {
        if (prefs.appClickDate.appPackage.isNotEmpty())
            launchApp(prefs.appClickDate)
        else openCalendar(requireContext())
    }

    private fun openDoubleTapApp() {
        if (prefs.appDoubleTap.appPackage.isNotEmpty())
            launchApp(prefs.appDoubleTap)
        else openCameraApp(requireContext())
    }

    // This function handles all swipe actions that a independent of the actual swipe direction
    @SuppressLint("NewApi")
    private fun handleOtherAction(action: Action) {
        when (action) {
            Action.ShowNotification -> expandNotificationDrawer(requireContext())
            Action.LockScreen -> lockPhone()
            Action.ShowAppList -> showAppList(AppDrawerFlag.LaunchApp, includeHiddenApps = false)
            Action.OpenApp -> {} // this should be handled in the respective onSwipe[Up,Down,Right,Left] functions
            Action.OpenQuickSettings -> expandQuickSettings(requireContext())
            Action.ShowRecents -> initActionService(requireContext())?.showRecents()
            Action.OpenPowerDialog -> initActionService(requireContext())?.openPowerDialog()
            Action.TakeScreenShot -> initActionService(requireContext())?.takeScreenShot()
            Action.Disabled -> {}
        }
    }

    private fun lockPhone() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val actionService = ActionService.instance()
            if (actionService != null) {
                actionService.lockScreen()
            } else {
                openAccessibilitySettings(requireContext())
            }
        } else {
            requireActivity().runOnUiThread {
                try {
                    deviceManager.lockNow()
                } catch (e: SecurityException) {
                    showToastLong(
                        requireContext(),
                        "App does not have the permission to lock the device"
                    )
                } catch (e: Exception) {
                    showToastLong(
                        requireContext(),
                        "mLauncher failed to lock device.\nPlease check your app settings."
                    )
                    prefs.lockModeOn = false
                }
            }
        }
    }

    private fun showLongPressToast() = showToastShort(requireContext(), "Long press to select app")

    private fun textOnClick(view: View) = onClick(view)

    private fun textOnLongClick(view: View) = onLongClick(view)

    private fun getHomeScreenGestureListener(context: Context): View.OnTouchListener {
        return object : OnSwipeTouchListener(context) {
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                when (val action = prefs.swipeLeftAction) {
                    Action.OpenApp -> openSwipeLeftApp()
                    else -> handleOtherAction(action)
                }
            }

            override fun onSwipeRight() {
                super.onSwipeRight()
                when (val action = prefs.swipeRightAction) {
                    Action.OpenApp -> openSwipeRightApp()
                    else -> handleOtherAction(action)
                }
            }

            override fun onSwipeUp() {
                super.onSwipeUp()
                when (val action = prefs.swipeUpAction) {
                    Action.OpenApp -> openSwipeUpApp()
                    else -> handleOtherAction(action)
                }
            }

            override fun onSwipeDown() {
                super.onSwipeDown()
                when (val action = prefs.swipeDownAction) {
                    Action.OpenApp -> openSwipeDownApp()
                    else -> handleOtherAction(action)
                }
            }

            override fun onLongClick() {
                super.onLongClick()
                lifecycleScope.launch(Dispatchers.Main) {
                    biometricPrompt = BiometricPrompt(this@HomeFragment,
                        ContextCompat.getMainExecutor(requireContext()),
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationError(
                                errorCode: Int,
                                errString: CharSequence
                            ) {
                                when (errorCode) {
                                    BiometricPrompt.ERROR_USER_CANCELED -> showToastLong(
                                        requireContext(),
                                        getString(R.string.text_authentication_cancel)
                                    )

                                    else -> showToastLong(
                                        requireContext(),
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
                                showToastLong(
                                    requireContext(),
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

                    authenticate()
                }
            }

            override fun onDoubleClick() {
                super.onDoubleClick()
                when (val action = prefs.doubleTapAction) {
                    Action.OpenApp -> openDoubleTapApp()
                    else -> handleOtherAction(action)
                }
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

            else -> showToastLong(requireContext(), getString(R.string.text_authentication_error))
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

    private fun getHomeAppsGestureListener(context: Context, view: View): View.OnTouchListener {
        return object : ViewSwipeTouchListener(context, view) {
            override fun onLongClick(view: View) {
                super.onLongClick(view)
                textOnLongClick(view)
            }

            override fun onClick(view: View) {
                super.onClick(view)
                textOnClick(view)
            }

            override fun onSwipeLeft() {
                super.onSwipeLeft()
                when (val action = prefs.swipeLeftAction) {
                    Action.OpenApp -> openSwipeLeftApp()
                    else -> handleOtherAction(action)
                }
            }

            override fun onSwipeRight() {
                super.onSwipeRight()
                when (val action = prefs.swipeRightAction) {
                    Action.OpenApp -> openSwipeRightApp()
                    else -> handleOtherAction(action)
                }
            }

            override fun onSwipeUp() {
                super.onSwipeUp()
                when (val action = prefs.swipeUpAction) {
                    Action.OpenApp -> openSwipeUpApp()
                    else -> handleOtherAction(action)
                }
            }

            override fun onSwipeDown() {
                super.onSwipeDown()
                when (val action = prefs.swipeDownAction) {
                    Action.OpenApp -> openSwipeDownApp()
                    else -> handleOtherAction(action)
                }
            }
        }
    }

    // updates number of apps visible on home screen
    // does nothing if number has not changed
    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("InflateParams")
    private fun updateAppCount(newAppsNum: Int) {
        val oldAppsNum = binding.homeAppsLayout.size // current number
        val diff = oldAppsNum - newAppsNum

        if (diff in 1 until oldAppsNum) { // 1 <= diff <= oldNumApps
            binding.homeAppsLayout.children.drop(diff)
        } else if (diff < 0) {
            val alignment =
                prefs.homeAlignment.value() // make only one call to prefs and store here

            // add all missing apps to list
            for (i in oldAppsNum until newAppsNum) {
                val view = layoutInflater.inflate(R.layout.home_app_button, null) as TextView
                view.apply {
                    textSize = prefs.textSizeLauncher.toFloat()
                    id = i
                    text = prefs.getHomeAppModel(i).appLabel
                    setOnTouchListener(getHomeAppsGestureListener(context, this))
                    if (!prefs.extendHomeAppsArea) {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }
                    gravity = alignment
                }
                val padding: Int = prefs.textMarginSize
                view.setPadding(0, padding, 0, padding)
                if (prefs.useCustomIconFont) {
                    val typeface = ResourcesCompat.getFont(requireActivity(), R.font.roboto)
                    typeface.also { view.typeface = it }
                }
                if (prefs.followAccentColors) {
                    val fontColor = getHexFontColor(requireContext())
                    view.setTextColor(fontColor)
                }
                binding.homeAppsLayout.addView(view)
            }
        }
    }
}
