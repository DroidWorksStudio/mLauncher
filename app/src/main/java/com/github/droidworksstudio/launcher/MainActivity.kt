package com.github.droidworksstudio.launcher

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.provider.AlarmClock
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.marginLeft
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.github.droidworksstudio.launcher.databinding.ActivityMainBinding
import com.github.droidworksstudio.launcher.settings.SettingsActivity
import com.github.droidworksstudio.launcher.settings.SharedPreferenceManager
import com.github.droidworksstudio.launcher.tasks.BatteryReceiver
import com.github.droidworksstudio.launcher.tasks.ScreenLockService
import com.github.droidworksstudio.launcher.utils.Animations
import com.github.droidworksstudio.launcher.utils.AppMenuEdgeFactory
import com.github.droidworksstudio.launcher.utils.AppMenuLinearLayoutManager
import com.github.droidworksstudio.launcher.utils.AppUtils
import com.github.droidworksstudio.launcher.utils.GestureUtils
import com.github.droidworksstudio.launcher.utils.StringUtils
import com.github.droidworksstudio.launcher.utils.UIUtils
import com.github.droidworksstudio.launcher.utils.WeatherSystem
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.reflect.Method
import kotlin.math.abs


class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener, AppMenuAdapter.OnItemClickListener, AppMenuAdapter.OnShortcutListener,
    AppMenuAdapter.OnItemLongClickListener {

    private lateinit var weatherSystem: WeatherSystem
    private lateinit var appUtils: AppUtils
    private val stringUtils = StringUtils()
    private lateinit var uiUtils: UIUtils
    private lateinit var gestureUtils: GestureUtils

    private var appActionMenu = AppActionMenu()
    private val appMenuLinearLayoutManager = AppMenuLinearLayoutManager(this@MainActivity)
    private val appMenuEdgeFactory = AppMenuEdgeFactory(this@MainActivity)

    private lateinit var sharedPreferenceManager: SharedPreferenceManager

    private lateinit var animations: Animations

    private lateinit var clock: TextClock
    private var clockMargin = 0
    private lateinit var dateText: TextClock
    private var dateElements = mutableListOf<String>()

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: TextInputEditText
    private var adapter: AppMenuAdapter? = null
    private var batteryReceiver: BatteryReceiver? = null

    private lateinit var binding: ActivityMainBinding
    private lateinit var launcherApps: LauncherApps
    private lateinit var installedApps: List<Triple<LauncherActivityInfo, UserHandle, Int>>

    private lateinit var preferences: SharedPreferences

    private var isBatteryReceiverRegistered = false
    private var isJobActive = true

    private val swipeThreshold = 100
    private val swipeVelocityThreshold = 100

    private lateinit var leftSwipeActivity: Pair<LauncherActivityInfo?, Int?>
    private lateinit var rightSwipeActivity: Pair<LauncherActivityInfo?, Int?>

    private lateinit var gestureDetector: GestureDetector
    private lateinit var shortcutGestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(null)

        setMainVariables()

        setShortcuts()

        setPreferences()

        setHomeListeners()

        // Task to update the app menu every 5 seconds
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    refreshAppMenu()
                    delay(5000)
                }
            }
        }

        // Task to update the weather every 10 minutes
        lifecycleScope.launch(Dispatchers.IO) {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    updateWeather()
                    delay(600000)
                }
            }
        }
        setupApps()
    }

    private fun setMainVariables() {
        launcherApps = getSystemService(LAUNCHER_APPS_SERVICE) as LauncherApps

        weatherSystem = WeatherSystem(this@MainActivity)
        appUtils = AppUtils(this@MainActivity, launcherApps)
        uiUtils = UIUtils(this@MainActivity)
        gestureUtils = GestureUtils(this@MainActivity)
        sharedPreferenceManager = SharedPreferenceManager(this@MainActivity)
        animations = Animations(this@MainActivity)

        gestureDetector = GestureDetector(this, GestureListener())
        shortcutGestureDetector = GestureDetector(this, TextGestureListener())

        clock = findViewById(R.id.textClock)

        clockMargin = clock.marginLeft

        dateText = findViewById(R.id.textDate)

        dateElements = mutableListOf(dateText.format12Hour.toString(), dateText.format24Hour.toString(), "", "")

        searchView = findViewById(R.id.searchView)

        preferences = PreferenceManager.getDefaultSharedPreferences(this)
    }

    private fun setShortcuts() {
        val shortcuts = arrayOf(R.id.app1, R.id.app2, R.id.app3, R.id.app4, R.id.app5, R.id.app6, R.id.app7, R.id.app8)

        for (i in shortcuts.indices) {

            val textView = findViewById<TextView>(shortcuts[i])
            val shortcutNo = sharedPreferenceManager.getShortcutNumber()

            // Only show the chosen number of shortcuts (default 4). Hide the rest.
            if (i >= shortcutNo!!) {
                textView.visibility = View.GONE
            } else {
                textView.visibility = View.VISIBLE


                val savedView = sharedPreferenceManager.getShortcut(textView)

                // Set the non-work profile drawable by default
                textView.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(resources, R.drawable.ic_empty, null), null, null, null)

                shortcutListeners(textView)

                if (savedView?.get(1) != "e") {
                    setShortcutSetup(textView, savedView)
                } else {
                    unsetShortcutSetup(textView)
                }

                uiUtils.setShortcutsAlignment(binding.homeView)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun shortcutListeners(textView: TextView) {
        // Don't go to settings on long click, but keep other gestures functional
        textView.setOnTouchListener { _, event ->
            shortcutGestureDetector.onTouchEvent(event)
            super.onTouchEvent(event)
        }

        textView.setOnLongClickListener {
            uiUtils.setMenuTitleAlignment(binding.menuTitle)
            binding.menuTitle.visibility = View.VISIBLE

            adapter?.shortcutTextView = textView
            toAppMenu()

            return@setOnLongClickListener true
        }
    }

    private fun toAppMenu() {
        try {
            // The menu opens from the top
            recyclerView.scrollToPosition(0)
        } catch (_: UninitializedPropertyAccessException) {

        }
        animations.showApps(binding.homeView, binding.appView)
        animations.backgroundIn(this@MainActivity)
        if (sharedPreferenceManager.isAutoKeyboardEnabled()) {
            val imm =
                getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            searchView.requestFocus()
            imm.showSoftInput(searchView, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun unsetShortcutSetup(textView: TextView) {
        unsetShortcutListeners(textView)
    }

    private fun unsetShortcutListeners(textView: TextView) {
        textView.setOnClickListener {
            Toast.makeText(this, "Long click to select an app", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setShortcutSetup(textView: TextView, savedView: List<String>?) {
        // Set the work profile drawable for work profile apps
        if (savedView?.get(1) != "0") {
            textView.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(resources, R.drawable.ic_work_app, null), null, null, null)
        }

        textView.text = savedView?.get(2)
        setShortcutListeners(textView, savedView)
    }

    private fun setShortcutListeners(textView: TextView, savedView: List<String>?) {
        textView.setOnClickListener {
            val mainActivity = launcherApps.getActivityList(savedView?.get(0).toString(), launcherApps.profiles[savedView?.get(1)!!.toInt()]).firstOrNull()
            if (mainActivity != null) {
                launcherApps.startMainActivity(mainActivity.componentName, launcherApps.profiles[savedView[1].toInt()], null, null)
            } else {
                Toast.makeText(this, "Cannot launch app", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setPreferences() {
        uiUtils.setBackground(window)
        uiUtils.setTextColors(binding.homeView)
        uiUtils.setMenuItemColors(searchView)
        uiUtils.setMenuItemColors(binding.menuTitle, "A9")

        uiUtils.setClockAlignment(clock, dateText)
        uiUtils.setSearchAlignment(searchView)

        uiUtils.setClockSize(clock)
        uiUtils.setDateSize(dateText)
        uiUtils.setShortcutsSize(binding.homeView)
        uiUtils.setSearchSize(searchView)

        uiUtils.setStatusBar(window)

        leftSwipeActivity = gestureUtils.getSwipeInfo(launcherApps, "left")
        rightSwipeActivity = gestureUtils.getSwipeInfo(launcherApps, "right")
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setHomeListeners() {
        registerBatteryReceiver()

        if (!sharedPreferenceManager.isBatteryEnabled()) {
            unregisterBatteryReceiver()
        }

        preferences.registerOnSharedPreferenceChangeListener(this)

        binding.homeView.setOnTouchListener { _, event ->
            super.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)
            true
        }

        binding.clockLayout.setOnClickListener { _ ->
            if (sharedPreferenceManager.isClockGestureEnabled()) {
                val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                }
            }
        }

        binding.clockLayout.setOnLongClickListener { _ ->
            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            true
        }

        // Return to home on back
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backToHome()
            }
        })
    }

    private fun registerBatteryReceiver() {
        if (!isBatteryReceiverRegistered) {
            batteryReceiver = BatteryReceiver.register(this, this@MainActivity)
            isBatteryReceiverRegistered = true
        }
    }

    private fun unregisterBatteryReceiver() {
        if (isBatteryReceiverRegistered) {
            unregisterReceiver(batteryReceiver)
            isBatteryReceiverRegistered = false
        }
    }

    // Only reload items that have had preferences changed
    override fun onSharedPreferenceChanged(preferences: SharedPreferences?, key: String?) {
        if (preferences != null) {
            when (key) {
                "bgColor" -> {
                    uiUtils.setBackground(window)
                }

                "textColor" -> {
                    uiUtils.setTextColors(binding.homeView)
                    uiUtils.setMenuItemColors(searchView)
                    uiUtils.setMenuItemColors(binding.menuTitle, "A9")
                }

                "clockAlignment" -> {
                    uiUtils.setClockAlignment(clock, dateText)
                }

                "shortcutAlignment" -> {
                    uiUtils.setShortcutsAlignment(binding.homeView)
                }

                "searchAlignment" -> {
                    uiUtils.setSearchAlignment(searchView)
                }

                "clockSize" -> {
                    uiUtils.setClockSize(clock)
                }

                "dateSize" -> {
                    uiUtils.setDateSize(dateText)
                }

                "shortcutSize" -> {
                    uiUtils.setShortcutsSize(binding.homeView)
                }

                "searchSize" -> {
                    uiUtils.setSearchSize(searchView)
                }

                "barVisibility" -> {
                    uiUtils.setStatusBar(window)
                }

                "leftSwipeApp" -> {
                    leftSwipeActivity = gestureUtils.getSwipeInfo(launcherApps, "left")
                }

                "rightSwipeApp" -> {
                    rightSwipeActivity = gestureUtils.getSwipeInfo(launcherApps, "right")
                }

                "batteryEnabled" -> {
                    if (sharedPreferenceManager.isBatteryEnabled()) {
                        registerBatteryReceiver()
                    } else {
                        unregisterBatteryReceiver()
                        modifyDate("", 3)
                    }
                }

                "shortcutNo" -> {
                    setShortcuts()
                }
            }
        }
    }

    fun modifyDate(value: String, index: Int) {
        /*Indexes:
        0 = 12h time
        1 = 24h time
        2 = Weather
        3 = Battery level*/
        dateElements[index] = value
        dateText.format12Hour = "${dateElements[0]}${stringUtils.addStartTextIfNotEmpty(dateElements[2], " | ")}${stringUtils.addStartTextIfNotEmpty(dateElements[3], " | ")}"
        dateText.format24Hour = "${dateElements[1]}${stringUtils.addStartTextIfNotEmpty(dateElements[2], " | ")}${stringUtils.addStartTextIfNotEmpty(dateElements[3], " | ")}"
    }

    fun backToHome() {
        closeKeyboard()
        animations.showHome(binding.homeView, binding.appView)
        animations.backgroundOut(this@MainActivity)
        val animSpeed = sharedPreferenceManager.getAnimationSpeed()

        // Delay app menu changes so that the user doesn't see them

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            try {
                searchView.setText(R.string.empty)
            } catch (_: UninitializedPropertyAccessException) {

            }
        }, animSpeed)

        handler.postDelayed({
            lifecycleScope.launch {
                refreshAppMenu()


            }
        }, animSpeed + 50)

    }

    private fun closeKeyboard() {
        val imm =
            getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    suspend fun refreshAppMenu() {
        try {

            // Don't reset app menu while under a search
            if (isJobActive) {
                val updatedApps = appUtils.getInstalledApps()
                if (!listsEqual(installedApps, updatedApps)) {

                    updateMenu(updatedApps)

                    installedApps = updatedApps
                }
            }
        } catch (_: UninitializedPropertyAccessException) {
        }
    }

    private fun listsEqual(list1: List<Triple<LauncherActivityInfo, UserHandle, Int>>, list2: List<Triple<LauncherActivityInfo, UserHandle, Int>>): Boolean {
        if (list1.size != list2.size) return false

        for (i in list1.indices) {
            if (list1[i].first.componentName != list2[i].first.componentName || list1[i].second != list2[i].second) {
                return false
            }
        }

        return true
    }

    private suspend fun updateMenu(updatedApps: List<Triple<LauncherActivityInfo, UserHandle, Int>>) {
        withContext(Dispatchers.Main) {
            adapter?.updateApps(updatedApps)
        }
    }

    private suspend fun updateWeather() {
        withContext(Dispatchers.IO) {
            if (sharedPreferenceManager.isWeatherEnabled()) {
                if (sharedPreferenceManager.isWeatherGPS()) {
                    weatherSystem.setGpsLocation(this@MainActivity)
                } else {
                    updateWeatherText()
                }
            } else {
                withContext(Dispatchers.Main) {
                    modifyDate("", 2)
                }
            }
        }
    }

    suspend fun updateWeatherText() {
        val temp = weatherSystem.getTemp()
        withContext(Dispatchers.Main) {
            modifyDate(temp, 2)
        }
    }

    private fun setupApps() {
        lifecycleScope.launch(Dispatchers.Default) {
            installedApps = appUtils.getInstalledApps()
            val newApps = installedApps.toMutableList()

            setupRecyclerView(newApps)

            setupSearch()
        }

    }

    private suspend fun setupRecyclerView(newApps: MutableList<Triple<LauncherActivityInfo, UserHandle, Int>>) {
        adapter = AppMenuAdapter(this@MainActivity, newApps, this@MainActivity, this@MainActivity, this@MainActivity, launcherApps)
        appMenuLinearLayoutManager.stackFromEnd = true
        recyclerView = findViewById(R.id.recyclerView)
        withContext(Dispatchers.Main) {
            recyclerView.layoutManager = appMenuLinearLayoutManager
            recyclerView.edgeEffectFactory = appMenuEdgeFactory
            recyclerView.adapter = adapter

        }

        setupRecyclerListener()
    }

    // Inform the layout manager of scroll states to calculate whether the menu is on the top
    private fun setupRecyclerListener() {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    appMenuLinearLayoutManager.setScrollInfo()
                }
            }
        })
    }

    private fun setupSearch() {
        binding.appView.addOnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->

            if (bottom - top > oldBottom - oldTop) {
                // If keyboard is closed, remove cursor from the search bar
                searchView.clearFocus()
            }

        }

        searchView.addTextChangedListener(object :
            TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                lifecycleScope.launch(Dispatchers.Default) {
                    filterItems(s.toString())
                }
            }
        })
    }

    private suspend fun filterItems(query: String?) {

        val cleanQuery = stringUtils.cleanString(query)
        val newFilteredApps = mutableListOf<Triple<LauncherActivityInfo, UserHandle, Int>>()
        val updatedApps = appUtils.getInstalledApps()

        getFilteredApps(cleanQuery, newFilteredApps, updatedApps)
    }

    private suspend fun getFilteredApps(
        cleanQuery: String?,
        newFilteredApps: MutableList<Triple<LauncherActivityInfo, UserHandle, Int>>,
        updatedApps: List<Triple<LauncherActivityInfo, UserHandle, Int>>
    ) {
        if (cleanQuery.isNullOrEmpty()) {
            isJobActive = true
            updateMenu(updatedApps)
        } else {
            isJobActive = false
            updatedApps.forEach {
                val cleanItemText = stringUtils.cleanString(
                    sharedPreferenceManager.getAppName(
                        it.first.applicationInfo.packageName,
                        it.third,
                        packageManager.getApplicationLabel(it.first.applicationInfo)
                    ).toString()
                )
                if (cleanItemText != null) {
                    if (cleanItemText.contains(cleanQuery, ignoreCase = true)) {
                        newFilteredApps.add(it)
                    }
                }
            }
            applySearchFilter(newFilteredApps)
        }
    }

    private suspend fun applySearchFilter(newFilteredApps: MutableList<Triple<LauncherActivityInfo, UserHandle, Int>>) {
        if (!listsEqual(installedApps, newFilteredApps)) {
            updateMenu(newFilteredApps)

            installedApps = newFilteredApps
        }
    }

    suspend fun applySearch() {
        withContext(Dispatchers.Default) {
            filterItems(searchView.text.toString())
        }
    }

    // On home key or swipe, return to home screen
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        backToHome()
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterBatteryReceiver()
        preferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onStart() {
        super.onStart()
        // Keyboard is sometimes open when going back to the app, so close it.
        closeKeyboard()

    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        adapter?.notifyDataSetChanged()
    }

    override fun onItemClick(appInfo: LauncherActivityInfo, userHandle: UserHandle) {
        appUtils.launchApp(appInfo, userHandle)
    }

    override fun onShortcut(
        appInfo: LauncherActivityInfo,
        userHandle: UserHandle,
        textView: TextView,
        userProfile: Int,
        shortcutView: TextView
    ) {
        if (userProfile != 0) {
            shortcutView.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(resources, R.drawable.ic_work_app, null), null, null, null)
            shortcutView.compoundDrawables[0]?.colorFilter =
                BlendModeColorFilter(sharedPreferenceManager.getTextColor(), BlendMode.SRC_ATOP)
            shortcutView.compoundDrawables[2]?.colorFilter =
                BlendModeColorFilter(sharedPreferenceManager.getTextColor(), BlendMode.SRC_ATOP)
        } else {
            shortcutView.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(resources, R.drawable.ic_empty, null), null, null, null)
        }

        shortcutView.text = textView.text.toString()
        shortcutView.setOnClickListener {
            appUtils.launchApp(appInfo, userHandle)
        }
        sharedPreferenceManager.setShortcut(
            shortcutView,
            appInfo.applicationInfo.packageName,
            userProfile
        )
        uiUtils.setDrawables(shortcutView, sharedPreferenceManager.getShortcutAlignment())
        backToHome()
    }


    override fun onItemLongClick(
        appInfo: LauncherActivityInfo,
        userHandle: UserHandle,
        userProfile: Int,
        textView: TextView,
        actionMenuLayout: LinearLayout,
        editView: LinearLayout,
        position: Int
    ) {
        textView.visibility = View.INVISIBLE
        animations.fadeViewIn(actionMenuLayout)
        val appActivity =
            launcherApps.getActivityList(appInfo.applicationInfo.packageName, userHandle)
                .firstOrNull()
        appActionMenu.setActionListeners(
            this@MainActivity,
            binding,
            textView,
            editView,
            actionMenuLayout,
            searchView,
            appInfo.applicationInfo,
            userHandle,
            userProfile,
            launcherApps,
            appActivity
        )
    }

    open inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        @SuppressLint("WrongConstant")
        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (e1 != null) {
                val deltaY = e2.y - e1.y
                val deltaX = e2.x - e1.x

                // Swipe up
                if (deltaY < -swipeThreshold && abs(velocityY) > swipeVelocityThreshold) {
                    openAppMenu()
                }

                // Swipe down
                else if (deltaY > swipeThreshold && abs(velocityY) > swipeVelocityThreshold) {
                    val statusBarService = getSystemService(STATUS_BAR_SERVICE)
                    val statusBarManager: Class<*> = Class.forName("android.app.StatusBarManager")
                    val expandMethod: Method = statusBarManager.getMethod("expandNotificationsPanel")
                    expandMethod.invoke(statusBarService)
                }

                // Swipe left
                else if (deltaX < -swipeThreshold && abs(velocityX) > swipeVelocityThreshold && sharedPreferenceManager.isGestureEnabled("left")) {

                    if (leftSwipeActivity.first != null && leftSwipeActivity.second != null) {
                        launcherApps.startMainActivity(leftSwipeActivity.first!!.componentName, launcherApps.profiles[leftSwipeActivity.second!!], null, null)
                    } else {
                        Toast.makeText(this@MainActivity, "Cannot launch app", Toast.LENGTH_SHORT).show()
                    }
                }


                // Swipe right
                else if (deltaX > -swipeThreshold && abs(velocityX) > swipeVelocityThreshold && sharedPreferenceManager.isGestureEnabled("right")) {
                    if (rightSwipeActivity.first != null && rightSwipeActivity.second != null) {
                        launcherApps.startMainActivity(rightSwipeActivity.first!!.componentName, launcherApps.profiles[rightSwipeActivity.second!!], null, null)
                    } else {
                        Toast.makeText(this@MainActivity, "Cannot launch app", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            super.onLongPress(e)
            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (sharedPreferenceManager.isDoubleTapEnabled()) {
                if (gestureUtils.isAccessibilityServiceEnabled(
                        ScreenLockService::class.java
                    )
                ) {
                    val intent = Intent(this@MainActivity, ScreenLockService::class.java)
                    intent.action = "LOCK_SCREEN"
                    startService(intent)
                } else {
                    gestureUtils.promptEnableAccessibility()
                }
            }

            return super.onDoubleTap(e)

        }

        private fun openAppMenu() {
            adapter?.shortcutTextView = null
            binding.menuTitle.visibility = View.GONE
            toAppMenu()
        }

    }

    inner class TextGestureListener : GestureListener() {
        override fun onLongPress(e: MotionEvent) {

        }
    }
}