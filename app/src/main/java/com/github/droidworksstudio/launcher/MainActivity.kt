package com.github.droidworksstudio.launcher

import android.Manifest
import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.database.Cursor
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast
import android.widget.ViewSwitcher
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.content.res.ResourcesCompat
import androidx.core.database.getStringOrNull
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
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
import com.github.droidworksstudio.launcher.utils.PermissionUtils
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


class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener, AppMenuAdapter.OnItemClickListener, AppMenuAdapter.OnShortcutListener, AppMenuAdapter.OnItemLongClickListener, ContactsAdapter.OnContactClickListener,
    ContactsAdapter.OnContactShortcutListener {

    private lateinit var weatherSystem: WeatherSystem
    private lateinit var appUtils: AppUtils
    private val stringUtils = StringUtils()
    private val permissionUtils = PermissionUtils()
    private lateinit var uiUtils: UIUtils
    private lateinit var gestureUtils: GestureUtils

    private val appMenuLinearLayoutManager = AppMenuLinearLayoutManager(this@MainActivity)
    private val contactMenuLinearLayoutManager = AppMenuLinearLayoutManager(this@MainActivity)
    private val appMenuEdgeFactory = AppMenuEdgeFactory(this@MainActivity)

    private lateinit var sharedPreferenceManager: SharedPreferenceManager

    private lateinit var animations: Animations

    private lateinit var clock: TextClock
    private var clockMargin = 0
    private lateinit var dateText: TextClock
    private var dateElements = mutableListOf<String>()

    private lateinit var menuView: ViewSwitcher
    private lateinit var menuTitle: TextInputEditText
    private lateinit var appRecycler: RecyclerView
    private lateinit var contactRecycler: RecyclerView
    private lateinit var searchSwitcher: ImageView
    private lateinit var internetSearch: ImageView
    private lateinit var googlePlaySearch: ImageView
    private lateinit var searchView: TextInputEditText
    private var appAdapter: AppMenuAdapter? = null
    private var contactAdapter: ContactsAdapter? = null
    private var batteryReceiver: BatteryReceiver? = null

    private lateinit var binding: ActivityMainBinding
    private lateinit var launcherApps: LauncherApps
    private lateinit var installedApps: List<Triple<LauncherActivityInfo, UserHandle, Int>>

    private lateinit var preferences: SharedPreferences

    private var isBatteryReceiverRegistered = false
    private var isJobActive = true
    private var isInitialOpen = false
    private var canLaunchShortcut = true
    private var showHidden = false

    private var swipeThreshold = 100
    private var swipeVelocityThreshold = 100

    private lateinit var clockApp: Pair<LauncherActivityInfo?, Int?>
    private lateinit var dateApp: Pair<LauncherActivityInfo?, Int?>

    private lateinit var leftSwipeActivity: Pair<LauncherActivityInfo?, Int?>
    private lateinit var rightSwipeActivity: Pair<LauncherActivityInfo?, Int?>

    private lateinit var gestureDetector: GestureDetector
    private lateinit var shortcutGestureDetector: GestureDetector

    var returnAllowed = true

    private val handler = Handler(Looper.getMainLooper())

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

        clock = binding.textClock
        clockMargin = clock.marginLeft

        dateText = binding.textDate
        dateElements = mutableListOf(dateText.format12Hour.toString(), dateText.format24Hour.toString(), "", "")

        menuTitle = binding.menuTitle
        menuView = binding.menuView

        searchView = binding.searchView
        searchSwitcher = binding.searchSwitcher
        internetSearch = binding.internetSearch
        googlePlaySearch = binding.googlePlaySearch

        preferences = PreferenceManager.getDefaultSharedPreferences(this)
    }

    private fun setShortcuts() {
        val shortcuts = arrayOf(R.id.app1, R.id.app2, R.id.app3, R.id.app4, R.id.app5, R.id.app6, R.id.app7, R.id.app8, R.id.app9, R.id.app10, R.id.app11, R.id.app12, R.id.app13, R.id.app14, R.id.app15)

        for (i in shortcuts.indices) {

            val textView = findViewById<TextView>(shortcuts[i])
            val shortcutNo = sharedPreferenceManager.getShortcutNumber()

            // Only show the chosen number of shortcuts (default 4). Hide the rest.
            if (i >= shortcutNo!!) {
                textView.visibility = View.GONE
            } else {
                textView.visibility = View.VISIBLE

                val savedView = sharedPreferenceManager.getShortcut(i)

                // Set the non-work profile drawable by default
                textView.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(resources, R.drawable.ic_empty, null), null, null, null)

                shortcutListeners(i, textView, savedView)

                if (savedView?.get(1) != "e") {
                    setShortcutSetup(textView, savedView)
                } else {
                    unsetShortcutSetup(textView)
                }
            }
        }
        uiUtils.setShortcutsAlignment(binding.homeView)
        uiUtils.setShortcutsVAlignment(binding.topSpace, binding.bottomSpace)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun shortcutListeners(index: Int, textView: TextView, savedView: List<String>?) {
        // Don't go to settings on long click, but keep other gestures functional
        textView.setOnTouchListener { _, event ->
            shortcutGestureDetector.onTouchEvent(event)
            super.onTouchEvent(event)
        }

        ViewCompat.addAccessibilityAction(textView, getString(R.string.accessibility_set_shortcut)) { _, _ ->
            launchShortcutSelection(index, textView, savedView)
        }

        ViewCompat.addAccessibilityAction(textView, getString(R.string.settings_title)) { _, _ ->
            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            true
        }

        ViewCompat.addAccessibilityAction(textView, getString(R.string.open_app_menu)) { _, _ ->
            openAppMenu()
            true
        }

        textView.setOnLongClickListener {
            launchShortcutSelection(index, textView, savedView)
        }
    }

    private fun launchShortcutSelection(index: Int, textView: TextView, savedView: List<String>?): Boolean {

        if (!sharedPreferenceManager.areShortcutsLocked()) {
            uiUtils.setMenuTitleAlignment(menuTitle)
            uiUtils.setMenuTitleSize(menuTitle)
            menuTitle.hint = textView.text
            menuTitle.setText(textView.text)
            menuTitle.visibility = View.VISIBLE
            if (savedView != null) {
                setRenameShortcutListener(index, textView)
            }
            appAdapter?.shortcutIndex = index
            appAdapter?.shortcutTextView = textView
            contactAdapter?.shortcutIndex = index
            contactAdapter?.shortcutTextView = textView
            internetSearch.visibility = View.GONE
            googlePlaySearch.visibility = View.GONE

            if (sharedPreferenceManager.showHiddenShortcuts()) {
                lifecycleScope.launch(Dispatchers.Default) {
                    showHidden = true
                    refreshAppMenu()
                    runOnUiThread {
                        toAppMenu() // This is intentionally slow to happen
                    }
                }
                return true
            }
            toAppMenu()

            return true
        }

        return false
    }

    private fun setRenameShortcutListener(index: Int, textView: TextView) {
        menuTitle.setOnEditorActionListener { _, actionId, _ ->

            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (menuTitle.text.isNullOrBlank()) {
                    Toast.makeText(this@MainActivity, getString(R.string.empty_rename), Toast.LENGTH_SHORT).show()
                    return@setOnEditorActionListener true
                }
                val imm =
                    getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(menuTitle.windowToken, 0)
                val savedView = sharedPreferenceManager.getShortcut(index)!!
                textView.text = menuTitle.text
                try {
                    sharedPreferenceManager.setShortcut(
                        index,
                        textView.text,
                        savedView[0],
                        savedView[1].toInt(),
                        savedView.getOrNull(3)?.toBoolean() == true
                    )
                } catch (_: NumberFormatException) {
                    sharedPreferenceManager.setShortcut(
                        index,
                        textView.text,
                        savedView[0],
                        0,
                        savedView.getOrNull(3)?.toBoolean() == true
                    )
                }
                backToHome()
                return@setOnEditorActionListener true
            }
            false
        }
    }

    private fun toAppMenu() {
        uiUtils.setContactsVisibility(searchSwitcher, binding.searchLayout, binding.searchReplacement)

        try {
            // The menu opens from the top
            appRecycler.scrollToPosition(0)
            menuView.displayedChild = 0
            if (searchSwitcher.isVisible) {
                contactRecycler.scrollToPosition(0)
                setAppViewDetails()
            }
        } catch (_: UninitializedPropertyAccessException) {
        }
        animations.showApps(binding.homeView, binding.appView)
        animations.backgroundIn(this@MainActivity)
        if (sharedPreferenceManager.isAutoKeyboardEnabled()) {
            isInitialOpen = true
            val imm =
                getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            searchView.requestFocus()
            imm.showSoftInput(searchView, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun unsetShortcutSetup(textView: TextView) {
        textView.text = getString(R.string.shortcut_default)
        unsetShortcutListeners(textView)
    }

    private fun unsetShortcutListeners(textView: TextView) {
        textView.setOnClickListener {
            if (canLaunchShortcut) {
                Toast.makeText(this, getString(R.string.shortcut_default_click), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setShortcutSetup(textView: TextView, savedView: List<String>?) {
        // Set the work profile drawable for work profile apps
        textView.text = savedView?.get(2)
        if (savedView != null && (savedView.getOrNull(3)?.toBoolean() == true)) {
            setShortcutContactListeners(textView, savedView[1].toInt())
            return
        }
        if (savedView?.get(1) != "0") {
            textView.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(resources, R.drawable.ic_work_app, null), null, null, null)
        }

        setShortcutListeners(textView, savedView)
    }

    private fun setShortcutListeners(textView: TextView, savedView: List<String>?) {
        textView.setOnClickListener {
            if (savedView != null && canLaunchShortcut) {
                val componentName = if (savedView[0].contains("/")) {
                    val (packageName, className) = savedView[0].split("/")
                    ComponentName(packageName, className)
                } else {
                    val userHandle = launcherApps.profiles[savedView[1].toInt()]
                    val mainActivity = launcherApps.getActivityList(savedView[0], userHandle).firstOrNull()
                    if (mainActivity != null) {
                        mainActivity.componentName
                    } else {
                        Toast.makeText(this, this.getString(R.string.launch_error), Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }
                appUtils.launchApp(componentName, launcherApps.profiles[savedView[1].toInt()])
            }
        }
    }

    private fun setShortcutContactListeners(textView: TextView, contactId: Int) {
        textView.setOnClickListener {
            onContactClick(contactId)
        }
    }

    private fun setPreferences() {
        uiUtils.setBackground(window)

        uiUtils.setTextFont(binding.homeView)
        uiUtils.setFont(searchView)
        uiUtils.setFont(menuTitle)

        uiUtils.setTextColors(binding.homeView)
        uiUtils.setStatusBarColor(window)

        uiUtils.setClockVisibility(clock)
        uiUtils.setDateVisibility(dateText)
        uiUtils.setSearchVisibility(searchView, binding.searchLayout, binding.searchReplacement)

        uiUtils.setClockAlignment(clock, dateText)
        uiUtils.setSearchAlignment(searchView)

        uiUtils.setClockSize(clock)
        uiUtils.setDateSize(dateText)
        uiUtils.setShortcutsSize(binding.homeView)
        uiUtils.setSearchSize(searchView)

        uiUtils.setShortcutsSpacing(binding.homeView)

        // This didn't work and somehow delaying it by 0 makes it work
        handler.postDelayed({
            uiUtils.setStatusBar(window)
            uiUtils.setMenuItemColors(searchView)
            uiUtils.setMenuItemColors(menuTitle, "A9")
        }, 100)

        clockApp = gestureUtils.getSwipeInfo(launcherApps, "clock")
        dateApp = gestureUtils.getSwipeInfo(launcherApps, "date")

        leftSwipeActivity = gestureUtils.getSwipeInfo(launcherApps, "left")
        rightSwipeActivity = gestureUtils.getSwipeInfo(launcherApps, "right")

        swipeThreshold = sharedPreferenceManager.getSwipeThreshold()
        swipeVelocityThreshold = sharedPreferenceManager.getSwipeVelocity()
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

        clock.setOnClickListener { _ ->
            if (sharedPreferenceManager.isClockGestureEnabled()) {
                if (sharedPreferenceManager.isGestureEnabled("clock") && clockApp.first != null && clockApp.second != null) {
                    launcherApps.startMainActivity(clockApp.first!!.componentName, launcherApps.profiles[clockApp.second!!], null, null)
                } else {
                    val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                    }
                }
            }
        }

        dateText.setOnClickListener { _ ->
            if (sharedPreferenceManager.isDateGestureEnabled()) {

                if (sharedPreferenceManager.isGestureEnabled("date") && dateApp.first != null && dateApp.second != null) {
                    launcherApps.startMainActivity(dateApp.first!!.componentName, launcherApps.profiles[dateApp.second!!], null, null)
                } else {
                    try {
                        startActivity(
                            Intent(
                                Intent.makeMainSelectorActivity(
                                    Intent.ACTION_MAIN,
                                    Intent.CATEGORY_APP_CALENDAR
                                )
                            )
                        )
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(this, getString(R.string.no_calendar_app), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        clock.setOnLongClickListener { _ ->
            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            true
        }

        dateText.setOnLongClickListener { _ ->
            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            true
        }

        ViewCompat.addAccessibilityAction(binding.homeView, getString(R.string.settings_title)) { _, _ ->
            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            true
        }

        ViewCompat.addAccessibilityAction(binding.homeView, getString(R.string.open_app_menu)) { _, _ ->
            openAppMenu()
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

    private fun openAppMenu() {
        appAdapter?.shortcutTextView = null
        contactAdapter?.shortcutTextView = null
        menuTitle.visibility = View.GONE
        uiUtils.setWebSearchVisibility(internetSearch)
        uiUtils.setGooglePlaySearchVisibility(googlePlaySearch)
        toAppMenu()
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
                    uiUtils.setStatusBarColor(window)
                    uiUtils.setMenuItemColors(searchView)
                    uiUtils.setMenuItemColors(menuTitle, "A9")
                    uiUtils.setImageColor(searchSwitcher)
                    uiUtils.setImageColor(internetSearch)
                    uiUtils.setImageColor(googlePlaySearch)
                }

                "textFont" -> {
                    uiUtils.setTextFont(binding.homeView)
                    uiUtils.setFont(searchView)
                    uiUtils.setFont(menuTitle)
                }

                "textStyle" -> {
                    uiUtils.setTextFont(binding.homeView)
                    uiUtils.setFont(searchView)
                    uiUtils.setFont(menuTitle)
                }

                "clockEnabled" -> {
                    uiUtils.setClockVisibility(clock)
                }

                "dateEnabled" -> {
                    uiUtils.setDateVisibility(dateText)
                }

                "searchEnabled" -> {
                    uiUtils.setSearchVisibility(searchView, binding.searchLayout, binding.searchReplacement)
                }

                "contactsEnabled" -> {
                    try {
                        contactRecycler
                    } catch (_: UninitializedPropertyAccessException) {
                        setupContactRecycler()
                    }
                }

                "clockAlignment" -> {
                    uiUtils.setClockAlignment(clock, dateText)
                }

                "shortcutAlignment" -> {
                    uiUtils.setShortcutsAlignment(binding.homeView)
                }

                "shortcutVAlignment" -> {
                    uiUtils.setShortcutsVAlignment(binding.topSpace, binding.bottomSpace)
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

                "shortcutWeight" -> {
                    uiUtils.setShortcutsSpacing(binding.homeView)
                }

                "barVisibility" -> {
                    uiUtils.setStatusBar(window)
                }

                "clockSwipe" -> {
                    clockApp = gestureUtils.getSwipeInfo(launcherApps, "clock")
                }

                "dateSwipe" -> {
                    dateApp = gestureUtils.getSwipeInfo(launcherApps, "date")
                }

                "clockSwipeApp" -> {
                    clockApp = gestureUtils.getSwipeInfo(launcherApps, "clock")
                }

                "dateSwipeApp" -> {
                    dateApp = gestureUtils.getSwipeInfo(launcherApps, "date")
                }

                "leftSwipe" -> {
                    leftSwipeActivity = gestureUtils.getSwipeInfo(launcherApps, "left")
                }

                "rightSwipe" -> {
                    rightSwipeActivity = gestureUtils.getSwipeInfo(launcherApps, "right")
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

                "swipeThreshold" -> {
                    swipeThreshold = sharedPreferenceManager.getSwipeThreshold()
                }

                "swipeVelocity" -> {
                    swipeVelocityThreshold = sharedPreferenceManager.getSwipeVelocity()
                }

                "isRestored" -> {
                    preferences.edit { remove("isRestored") }
                    setPreferences()
                    setShortcuts()
                }

                "lockShortcuts" -> {
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

    fun backToHome(animSpeed: Long = sharedPreferenceManager.getAnimationSpeed()) {
        canLaunchShortcut = true
        showHidden = false
        closeKeyboard()
        animations.showHome(binding.homeView, binding.appView, animSpeed)
        animations.backgroundOut(this@MainActivity, animSpeed)

        // Delay app menu changes so that the user doesn't see them

        handler.postDelayed({
            try {
                searchView.setText(R.string.empty)
                appMenuLinearLayoutManager.setScrollEnabled(true)
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
                val updatedApps = appUtils.getInstalledApps(showHidden)
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
            appAdapter?.updateApps(updatedApps)
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

            setupAppRecycler(newApps)

            setupSearch()
            if (sharedPreferenceManager.areContactsEnabled()) {
                setupContactRecycler()
            }

            setupInternetSearch()
            setupGooglePlaySearch()
        }
    }

    private fun setupInternetSearch() {
        uiUtils.setImageColor(internetSearch)
        internetSearch.setOnClickListener {
            val query = searchView.text.toString().trim()
            if (query.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                    putExtra(SearchManager.QUERY, searchView.text.toString())
                }

                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                } else {
                    Toast.makeText(this@MainActivity, "No browser app found.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this@MainActivity, "Enter a search term.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupGooglePlaySearch() {
        uiUtils.setImageColor(googlePlaySearch)
        googlePlaySearch.setOnClickListener {
            val query = searchView.text.toString().trim()
            if (query.isNotEmpty()) {
                try {
                    // Try opening the Play Store app
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = "market://search?q=$query&c=apps".toUri()
                        setPackage("com.android.vending")
                    }
                    startActivity(intent)
                } catch (_: ActivityNotFoundException) {
                    // If Play Store is not available, fallback to browser
                    val browserIntent = Intent(Intent.ACTION_VIEW).apply {
                        data = "https://play.google.com/store/search?q=$query&c=apps".toUri()
                    }
                    startActivity(browserIntent)
                }
            } else {
                Toast.makeText(this@MainActivity, "Enter a search term.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private suspend fun setupAppRecycler(newApps: MutableList<Triple<LauncherActivityInfo, UserHandle, Int>>) {
        appAdapter = AppMenuAdapter(this@MainActivity, binding, newApps, this@MainActivity, this@MainActivity, this@MainActivity, launcherApps)
        appMenuLinearLayoutManager.stackFromEnd = true
        appRecycler = binding.appRecycler
        withContext(Dispatchers.Main) {
            appRecycler.layoutManager = appMenuLinearLayoutManager
            appRecycler.edgeEffectFactory = appMenuEdgeFactory
            appRecycler.adapter = appAdapter

        }

        setupRecyclerListener(appRecycler, appMenuLinearLayoutManager)
    }

    // Inform the layout manager of scroll states to calculate whether the menu is on the top
    private fun setupRecyclerListener(recycler: RecyclerView, layoutManager: AppMenuLinearLayoutManager) {
        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    layoutManager.setScrollInfo()
                }
            }
        })
    }

    private fun setupContactRecycler() {
        uiUtils.setImageColor(searchSwitcher)

        contactAdapter = ContactsAdapter(this, mutableListOf(), this@MainActivity, this@MainActivity)
        contactMenuLinearLayoutManager.stackFromEnd = true
        contactRecycler = binding.contactRecycler
        contactRecycler.layoutManager = contactMenuLinearLayoutManager
        contactRecycler.edgeEffectFactory = appMenuEdgeFactory
        contactRecycler.adapter = contactAdapter
        setupRecyclerListener(contactRecycler, contactMenuLinearLayoutManager)

        searchSwitcher.setOnClickListener {
            switchMenus()
        }
    }

    fun switchMenus() {
        menuView.showNext()
        when (menuView.displayedChild) {
            0 -> {
                setAppViewDetails()
            }

            1 -> {
                setContactViewDetails()
            }
        }
    }

    private fun setAppViewDetails() {
        lifecycleScope.launch(Dispatchers.Default) {
            filterItems(searchView.text.toString())
        }
        searchSwitcher.setImageDrawable(
            ResourcesCompat.getDrawable(
                resources,
                R.drawable.contacts,
                null
            )
        )
        searchSwitcher.contentDescription = getString(R.string.switch_to_contacts)
    }

    private fun setContactViewDetails() {
        lifecycleScope.launch(Dispatchers.Default) {
            filterItems(searchView.text.toString())
        }
        searchSwitcher.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.apps, null))
        searchSwitcher.contentDescription = getString(R.string.switch_to_apps)
    }

    private fun getContacts(filterString: String): MutableList<Pair<String, Int>> {
        val contacts = mutableListOf<Pair<String, Int>>()

        val contentResolver: ContentResolver = contentResolver

        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME
        )

        val selection = "${ContactsContract.Contacts.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$filterString%")

        val cursor: Cursor? = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${ContactsContract.Contacts.DISPLAY_NAME} ASC"
        )

        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
            while (it.moveToNext()) {
                val name = it.getStringOrNull(nameIndex)
                val id = it.getStringOrNull(idIndex)?.toInt()
                if (name != null && id != null) {
                    contacts.add(Pair(name, id))
                }
            }
        }
        return contacts
    }


    private suspend fun updateContacts(filterString: String) {
        val contacts = getContacts(filterString)
        withContext(Dispatchers.Main) {
            contactAdapter?.updateContacts(contacts)
        }
    }

    private fun setupSearch() {
        binding.appView.addOnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->

            if (bottom - top > oldBottom - oldTop) {
                // If keyboard is closed, remove cursor from the search bar
                searchView.clearFocus()
                menuTitle.clearFocus()
            } else if (bottom - top < oldBottom - oldTop && isInitialOpen) {
                isInitialOpen = false
                appRecycler.scrollToPosition(0)
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
        when (menuView.displayedChild) {
            0 -> {
                val newFilteredApps = mutableListOf<Triple<LauncherActivityInfo, UserHandle, Int>>()
                val updatedApps = appUtils.getInstalledApps(showHidden)
                val filteredApps = getFilteredApps(cleanQuery, newFilteredApps, updatedApps)
                if (filteredApps != null) {
                    applySearchFilter(filteredApps)
                }
            }

            1 -> {
                if (sharedPreferenceManager.areContactsEnabled() && cleanQuery != null) {
                    updateContacts(cleanQuery)
                }
            }
        }
    }

    private suspend fun getFilteredApps(
        cleanQuery: String?,
        newFilteredApps: MutableList<Triple<LauncherActivityInfo, UserHandle, Int>>,
        updatedApps: List<Triple<LauncherActivityInfo, UserHandle, Int>>
    ): List<Triple<LauncherActivityInfo, UserHandle, Int>>? {
        if (cleanQuery.isNullOrEmpty()) {
            isJobActive = true
            updateMenu(updatedApps)
            return null
        } else {
            isJobActive = false

            val fuzzyPattern = if (sharedPreferenceManager.isFuzzySearchEnabled()) {
                stringUtils.getFuzzyPattern(cleanQuery)
            } else {
                null
            }

            updatedApps.forEach {
                val cleanItemText = stringUtils.cleanString(
                    sharedPreferenceManager.getAppName(
                        it.first.componentName.flattenToString(),
                        it.third,
                        it.first.label
                    ).toString()
                )
                if (cleanItemText != null) {
                    if (
                        (fuzzyPattern != null && cleanItemText.contains(fuzzyPattern)) ||
                        (cleanItemText.contains(cleanQuery, ignoreCase = true))
                    ) {
                        newFilteredApps.add(it)
                    }
                }
            }
            return newFilteredApps
        }
    }

    private suspend fun applySearchFilter(newFilteredApps: List<Triple<LauncherActivityInfo, UserHandle, Int>>) {
        if (sharedPreferenceManager.isAutoLaunchEnabled() && menuView.displayedChild == 0 && appAdapter?.shortcutTextView == null && newFilteredApps.size == 1) {
            appUtils.launchApp(newFilteredApps[0].first.componentName, newFilteredApps[0].second)
        } else {
            updateMenu(newFilteredApps)
            installedApps = newFilteredApps
        }
    }

    suspend fun applySearch() {
        withContext(Dispatchers.Default) {
            filterItems(searchView.text.toString())
        }
    }

    fun disableAppMenuScroll() {
        appMenuLinearLayoutManager.setScrollEnabled(false)
        appRecycler.layoutManager = appMenuLinearLayoutManager
    }

    fun enableAppMenuScroll() {
        appMenuLinearLayoutManager.setScrollEnabled(true)
        appRecycler.layoutManager = appMenuLinearLayoutManager
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
        if (!permissionUtils.hasPermission(this@MainActivity, Manifest.permission.READ_CONTACTS)) {
            sharedPreferenceManager.setContactsEnabled(false)
        }
        if (!permissionUtils.hasPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            sharedPreferenceManager.setWeatherGPS(false)
        }
        if (returnAllowed) {
            backToHome(0)
        }
        returnAllowed = true
        appAdapter?.notifyDataSetChanged()
    }

    override fun onItemClick(appInfo: LauncherActivityInfo, userHandle: UserHandle) {
        appUtils.launchApp(appInfo.componentName, userHandle)
    }

    override fun onShortcut(
        appInfo: LauncherActivityInfo,
        userHandle: UserHandle,
        textView: TextView,
        userProfile: Int,
        shortcutView: TextView,
        shortcutIndex: Int
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
            appUtils.launchApp(appInfo.componentName, userHandle)
        }
        sharedPreferenceManager.setShortcut(
            shortcutIndex,
            shortcutView.text,
            appInfo.componentName.flattenToString(),
            userProfile
        )
        uiUtils.setDrawables(shortcutView, sharedPreferenceManager.getShortcutAlignment())
        backToHome()
    }

    override fun onItemLongClick(
        textView: TextView,
        actionMenuLayout: LinearLayout,
    ) {
        textView.visibility = View.GONE
        animations.fadeViewIn(actionMenuLayout)
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
            if (animations.isInAnim) {
                return false
            }
            if (e1 != null) {
                val deltaY = e2.y - e1.y
                val deltaX = e2.x - e1.x

                // Swipe up
                if (deltaY < -swipeThreshold && abs(velocityY) > swipeVelocityThreshold) {
                    canLaunchShortcut = false
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
                else if (deltaX < 0 && abs(deltaX) > swipeThreshold && abs(velocityX) > swipeVelocityThreshold && sharedPreferenceManager.isGestureEnabled("left")) {
                    println(leftSwipeActivity)
                    if (leftSwipeActivity.first != null && leftSwipeActivity.second != null) {
                        canLaunchShortcut = false
                        appUtils.launchApp(leftSwipeActivity.first!!.componentName, launcherApps.profiles[leftSwipeActivity.second!!])
                    } else {
                        Toast.makeText(this@MainActivity, getString(R.string.launch_error), Toast.LENGTH_SHORT).show()
                    }
                }


                // Swipe right
                else if (deltaX > 0 && abs(deltaX) > swipeThreshold && abs(velocityX) > swipeVelocityThreshold && sharedPreferenceManager.isGestureEnabled("right")) {
                    if (rightSwipeActivity.first != null && rightSwipeActivity.second != null) {
                        canLaunchShortcut = false
                        appUtils.launchApp(rightSwipeActivity.first!!.componentName, launcherApps.profiles[rightSwipeActivity.second!!])
                    } else {
                        Toast.makeText(this@MainActivity, getString(R.string.launch_error), Toast.LENGTH_SHORT).show()
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

    }

    inner class TextGestureListener : GestureListener() {
        override fun onLongPress(e: MotionEvent) {

        }
    }

    override fun onContactClick(contactId: Int) {
        val contactUri: Uri = Uri.withAppendedPath(
            ContactsContract.Contacts.CONTENT_URI,
            contactId.toString()
        )

        val intent = Intent(Intent.ACTION_VIEW, contactUri)
        startActivity(intent)
        returnAllowed = false
    }

    override fun onContactShortcut(contactId: Int, contactName: String, shortcutView: TextView, shortcutIndex: Int) {
        shortcutView.text = contactName
        shortcutView.setOnClickListener {
            onContactClick(contactId)
        }
        sharedPreferenceManager.setShortcut(
            shortcutIndex,
            shortcutView.text,
            contactName,
            contactId,
            true
        )
        shortcutView.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(resources, R.drawable.ic_empty, null), null, null, null)
        uiUtils.setDrawables(shortcutView, sharedPreferenceManager.getShortcutAlignment())
        backToHome()
    }
}