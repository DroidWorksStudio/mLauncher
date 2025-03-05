package com.github.droidworksstudio.mlauncher.services

import android.app.Service
import android.content.Intent
import android.content.res.Resources
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.github.droidworksstudio.mlauncher.BuildConfig
import com.github.droidworksstudio.mlauncher.R
import com.github.droidworksstudio.mlauncher.listener.EdgeSwipeListener

class EdgeService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var leftEdgeView: View
    private lateinit var rightEdgeView: View
    private var currentPanelDirection: String = ""  // Track current panel direction
    private val debugModeInt = 0 // 1 Or 0


    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    @Suppress("KotlinConstantConditions")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val debugDetectionArea: Boolean = (debugModeInt >= 1)

        val direction = intent?.getStringExtra("PANEL_DIRECTION") ?: "RIGHT"  // Default to right panel
        currentPanelDirection = direction  // Store the current panel's direction
        val panelHeight = (Resources.getSystem().displayMetrics.heightPixels / 2)

        // Left Edge Swipe Detection
        leftEdgeView = View(this)
        val leftParams = WindowManager.LayoutParams(
            50, panelHeight,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        leftParams.gravity = Gravity.START
        if (debugDetectionArea && BuildConfig.DEBUG) leftEdgeView.setBackgroundColor(getColor(R.color.edge_effect_background))
        leftEdgeView.setOnTouchListener(EdgeSwipeListener(isLeftEdge = true))  // Detect left edge swipes
        windowManager.addView(leftEdgeView, leftParams)

        // Right Edge Swipe Detection
        rightEdgeView = View(this)
        val rightParams = WindowManager.LayoutParams(
            50, panelHeight,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        rightParams.gravity = Gravity.END
        if (debugDetectionArea && BuildConfig.DEBUG) rightEdgeView.setBackgroundColor(getColor(R.color.edge_effect_background))
        rightEdgeView.setOnTouchListener(EdgeSwipeListener(isRightEdge = true))  // Detect right edge swipes
        windowManager.addView(rightEdgeView, rightParams)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::leftEdgeView.isInitialized) windowManager.removeView(leftEdgeView)
        if (::rightEdgeView.isInitialized) windowManager.removeView(rightEdgeView)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    // Function to close the active panel
    fun closePanel() {
        if (currentPanelDirection == "LEFT") {
            // Stop the left panel
            val intent = Intent(this, EdgePanelService::class.java)
            stopService(intent)
            currentPanelDirection = ""  // Reset the direction
        } else if (currentPanelDirection == "RIGHT") {
            // Stop the right panel
            val intent = Intent(this, EdgePanelService::class.java)
            stopService(intent)
            currentPanelDirection = ""  // Reset the direction
        }
    }

    // Function to handle the opening of a panel
    fun openPanel(direction: String) {
        if (currentPanelDirection != "") {
            // If a panel is already open, do not open another one
            return
        }

        currentPanelDirection = direction
        val intent = Intent(this, EdgePanelService::class.java)
        intent.putExtra("PANEL_DIRECTION", direction)  // Pass direction to the service
        startService(intent)
    }
}