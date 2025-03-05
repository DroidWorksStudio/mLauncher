package com.github.droidworksstudio.mlauncher.services

import android.annotation.SuppressLint
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.PixelFormat
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.setPadding
import com.github.droidworksstudio.mlauncher.R
import com.github.droidworksstudio.mlauncher.data.Prefs
import com.github.droidworksstudio.mlauncher.helper.hasUsagePermission
import com.github.droidworksstudio.mlauncher.helper.requestUsagePermission
import java.util.Calendar

class EdgePanelService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var panelView: View
    private lateinit var prefs: Prefs
    private var isPanelActive = false  // Track if a panel is currently active
    private var startX = 0f  // For swipe detection

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    @SuppressLint("InflateParams", "ClickableViewAccessibility", "ServiceCast")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        prefs = Prefs(this)

        val direction = intent?.getStringExtra("PANEL_DIRECTION") ?: "RIGHT"  // Default to right panel
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,  // Width to match the screen width
            WindowManager.LayoutParams.WRAP_CONTENT,  // Height will be set manually
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        // Inflate the edge panel layout
        panelView = LayoutInflater.from(this).inflate(R.layout.edge_panel, null)

        val edgeAppsLayout = panelView.findViewById<LinearLayout>(R.id.edgeAppsLayout)

        // Apply 30dp horizontal padding (left and right)
        val paddingHorizontal = 50  // In pixels (you can convert dp to pixels using resources)

        // Get the screen height
        val screenHeight = Resources.getSystem().displayMetrics.heightPixels

        // Set the position and gravity
        params.x = paddingHorizontal  // Set the X position (horizontal padding)
        when (direction) {
            "LEFT" -> params.gravity = Gravity.TOP or Gravity.START
            "RIGHT" -> params.gravity = Gravity.TOP or Gravity.END
        }

        // Check if a panel is already active and remove it before adding a new one
        if (isPanelActive) {
            windowManager.removeView(panelView)
        }

        // Check if permission is granted to access usage stats
        if (hasUsagePermission(this)) {
            // Get the package manager to retrieve installed apps
            val packageManager = packageManager
            val installedApps = packageManager.getInstalledApplications(0)

            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_MONTH, -30)
            val usageStatsList =
                usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, calendar.timeInMillis, System.currentTimeMillis())

            val appUsageMap = mutableMapOf<String, Long>()
            for (usageStat in usageStatsList) {
                appUsageMap[usageStat.packageName] = usageStat.lastTimeUsed
            }

            val sortedApps = appUsageMap.entries.sortedByDescending { it.value }

            val iconSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30f, this.resources.displayMetrics).toInt()
            val margin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5f, this.resources.displayMetrics).toInt()
            val padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f, this.resources.displayMetrics).toInt()

            panelView.setPadding(padding)

            // Limit to first maxApps apps (you can adjust this)
            var countApps = 0
            val maxApps = prefs.edgeAppsNum
            for (entry in sortedApps) {
                val appPackageName = entry.key
                val app = installedApps.find { it.packageName == appPackageName }
                val icon = app?.let { packageManager.getApplicationIcon(it) }
                // Check if the icon is an instance of AdaptiveIconDrawable
                if (icon is AdaptiveIconDrawable) {
                    // Get the launch intent for the app
                    val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)

                    // Only add the app if it can be launched
                    if (launchIntent != null) {
                        val imageView = ImageView(this).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                iconSize,  // Width set to 50px
                                iconSize   // Height set to 50px
                            ).apply {
                                setMargins(0, margin, 0, margin)  // Adjust spacing between icons
                            }
                            scaleType = ImageView.ScaleType.CENTER_CROP  // Ensure proper scaling
                            setImageDrawable(icon)  // Set the app icon
                            setOnClickListener {
                                startActivity(launchIntent)
                            }
                        }

                        // Add the ImageView to the edgeAppsLayout
                        edgeAppsLayout.addView(imageView)
                        countApps++

                        // Stop once 7 AdaptiveIconDrawable icons are added
                        if (countApps >= maxApps) break
                    }
                }
            }

            // Corrected total height calculation
            val panelHeight = maxApps * (iconSize + margin * 2) + padding * 2
            val panelYPosition = (screenHeight - panelHeight) / 2 // Centering panel

            params.height = panelHeight
            params.y = panelYPosition

        } else {
            Handler(Looper.getMainLooper()).post {
                requestUsagePermission(this)
            }
        }

        // Set touch listener for swipe detection
        panelView.setOnTouchListener { _, event -> onPanelTouch(event, direction) }

        windowManager.addView(panelView, params)
        isPanelActive = true  // Mark the new panel as active

        return START_STICKY
    }

    // Handle touch events for swipe detection to close panel
    private fun onPanelTouch(event: MotionEvent?, direction: String): Boolean {
        event ?: return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.rawX
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.rawX - startX

                // If the panel is swiped in the opposite direction, close it
                if ((direction == "RIGHT" && deltaX >= 100)) {
                    stopPanel()  // Close the panel
                    return true
                }
                if ((direction == "LEFT" && deltaX >= -100)) {
                    stopPanel()  // Close the panel
                    return true
                }
                return false
            }
        }
        return false
    }

    // Stop the service and remove the panel
    private fun stopPanel() {
        if (isPanelActive) {
            windowManager.removeView(panelView)  // Remove the panel view
            stopSelf()  // Stop the service
            isPanelActive = false  // Mark the panel as inactive
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isPanelActive) {
            windowManager.removeView(panelView)  // Clean up if panel is active
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
