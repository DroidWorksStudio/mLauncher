package app.mlauncher.ui

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Context.VIBRATOR_SERVICE
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.text.format.DateFormat.getBestDateTimePattern
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import app.mlauncher.MainViewModel
import app.mlauncher.R
import app.mlauncher.data.AppModel
import app.mlauncher.data.Constants.Action
import app.mlauncher.data.Constants.AppDrawerFlag
import app.mlauncher.data.Prefs
import app.mlauncher.databinding.FragmentHomeBinding
import app.mlauncher.helper.*
import app.mlauncher.listener.OnSwipeTouchListener
import app.mlauncher.listener.ViewSwipeTouchListener
import kotlinx.coroutines.launch


class HomeFragment : Fragment(), View.OnClickListener, View.OnLongClickListener {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var deviceManager: DevicePolicyManager
    private lateinit var vibrator: Vibrator

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        val view = binding.root
        prefs = Prefs(requireContext())

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        deviceManager = context?.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        @Suppress("DEPRECATION")
        vibrator = context?.getSystemService(VIBRATOR_SERVICE) as Vibrator

        initObservers()

        initSwipeTouchListener()
        initClickListeners()
    }

    override fun onStart() {
        super.onStart()
        if (prefs.showStatusBar) showStatusBar(requireActivity()) else hideStatusBar(requireActivity())

        binding.clock.textSize = prefs.textSize * 2.5f
        binding.date.textSize = prefs.textSize.toFloat()

    }

    override fun onResume() {
        super.onResume()

        val locale = prefs.language.locale()
        val best12 = getBestDateTimePattern(locale, "hhmma")
        val best24 = getBestDateTimePattern(locale, "HHmm")
        Log.d("locale", "$locale, $best12, $best24")
        binding.clock.format12Hour = best12
        binding.clock.format24Hour = best24

        val best12Date = getBestDateTimePattern(locale, "eeeddMMM")
        val best24Date = getBestDateTimePattern(locale,"eeeddMMM")
        Log.d("locale", "$locale, $best12Date, $best24Date")
        binding.date.format12Hour = best12Date
        binding.date.format24Hour = best24Date

        // only show "set as default"-button if tips are GONE
        if (binding.firstRunTips.visibility == View.GONE) {
            binding.setDefaultLauncher.visibility =
                if (ismlauncherDefault(requireContext())) View.GONE else View.VISIBLE
        }
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
            R.id.setDefaultLauncher -> viewModel.resetDefaultLauncherApp(requireContext())
            else -> {
                try { // Launch app
                    val appLocation = view.id.toString().toInt()
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
        val name = prefs.getHomeAppModel(n).appLabel
        showAppList(AppDrawerFlag.SetHomeApp, name.isNotEmpty(), n)
        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initSwipeTouchListener() {
        val context = requireContext()
        binding.touchArea.setOnTouchListener(getHomeScreenGestureListener(context))
    }

    private fun initClickListeners() {
        binding.clock.setOnClickListener(this)
        binding.date.setOnClickListener(this)
        binding.setDefaultLauncher.setOnClickListener(this)
    }

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

    private fun showAppList(flag: AppDrawerFlag, showHiddenApps: Boolean = false, n: Int = 0) {
        viewModel.getAppList(showHiddenApps)
        lifecycleScope.launch {
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
        // Source: https://stackoverflow.com/a/51132142
        try {
            val statusBarService = context.getSystemService("statusbar")
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            val method = statusBarManager.getMethod("expandNotificationsPanel")
            method.invoke(statusBarService)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("WrongConstant", "PrivateApi")
    private fun expandQuickSettings(context: Context) {
        try {
            val statusBarService = context.getSystemService("statusbar")
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            val method = statusBarManager.getMethod("expandSettingsPanel")
            method.invoke(statusBarService)
        } catch (e: Exception) {
            e.printStackTrace()
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
        when(action) {
            Action.ShowNotification -> expandNotificationDrawer(requireContext())
            Action.LockScreen -> lockPhone()
            Action.ShowAppList -> showAppList(AppDrawerFlag.LaunchApp)
            Action.OpenApp -> {} // this should be handled in the respective onSwipe[Down,Right,Left] functions
            Action.OpenQuickSettings -> expandQuickSettings(requireContext())
            Action.ShowRecents -> initActionService(requireContext())?.showRecents()
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
                    showToastLong(requireContext(), "App does not have the permission to lock the device")
                } catch (e: Exception) {
                    showToastLong(requireContext(), "mLauncher failed to lock device.\nPlease check your app settings.")
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
                when(val action = prefs.swipeLeftAction) {
                    Action.OpenApp -> openSwipeLeftApp()
                    else -> handleOtherAction(action)
                }
            }

            override fun onSwipeRight() {
                super.onSwipeRight()
                when(val action = prefs.swipeRightAction) {
                    Action.OpenApp -> openSwipeRightApp()
                    else -> handleOtherAction(action)
                }
            }

            override fun onSwipeUp() {
                super.onSwipeUp()
                when(val action = prefs.swipeUpAction) {
                    Action.OpenApp -> openSwipeUpApp()
                    else -> handleOtherAction(action)
                }
            }

            override fun onSwipeDown() {
                super.onSwipeDown()
                when(val action = prefs.swipeDownAction) {
                    Action.OpenApp -> openSwipeDownApp()
                    else -> handleOtherAction(action)
                }
            }

            override fun onLongClick() {
                super.onLongClick()
                try {
                    findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
                    viewModel.firstOpen(false)
                } catch (e: java.lang.Exception) {
                    Log.d("onLongClick", e.toString())
                }
            }

            override fun onDoubleClick() {
                super.onDoubleClick()
                when(val action = prefs.doubleTapAction) {
                    Action.OpenApp -> openDoubleTapApp()
                    else -> handleOtherAction(action)
                }
            }
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
                when(val action = prefs.swipeLeftAction) {
                    Action.OpenApp -> openSwipeLeftApp()
                    else -> handleOtherAction(action)
                }
            }

            override fun onSwipeRight() {
                super.onSwipeRight()
                when(val action = prefs.swipeRightAction) {
                    Action.OpenApp -> openSwipeRightApp()
                    else -> handleOtherAction(action)
                }
            }

            override fun onSwipeUp() {
                super.onSwipeUp()
                when(val action = prefs.swipeUpAction) {
                    Action.OpenApp -> openSwipeUpApp()
                    else -> handleOtherAction(action)
                }
            }

            override fun onSwipeDown() {
                super.onSwipeDown()
                when(val action = prefs.swipeDownAction) {
                    Action.OpenApp -> openSwipeDownApp()
                    else -> handleOtherAction(action)
                }
            }
        }
    }

    // updates number of apps visible on home screen
    // does nothing if number has not changed
    @SuppressLint("InflateParams")
    private fun updateAppCount(newAppsNum: Int) {
        val oldAppsNum = binding.homeAppsLayout.size // current number
        val diff = oldAppsNum - newAppsNum

        if (diff in 1 until oldAppsNum) { // 1 <= diff <= oldNumApps
            binding.homeAppsLayout.children.drop(diff)
        } else if (diff < 0) {
            val alignment = prefs.homeAlignment.value() // make only one call to prefs and store here

            // add all missing apps to list
            for (i in oldAppsNum until newAppsNum) {
                val view = layoutInflater.inflate(R.layout.home_app_button, null) as TextView
                view.apply {
                    textSize = prefs.textSize.toFloat()
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
                binding.homeAppsLayout.addView(view)
            }
        }
    }
}
