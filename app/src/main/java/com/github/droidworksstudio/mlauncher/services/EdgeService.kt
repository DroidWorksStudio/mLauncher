package com.github.droidworksstudio.mlauncher.services

import android.app.Service
import android.content.Intent
import android.content.res.Resources
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.github.droidworksstudio.mlauncher.R
import com.github.droidworksstudio.mlauncher.listener.EdgeSwipeListener

class EdgeService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var leftEdgeView: View
    private lateinit var rightEdgeView: View
    private var currentPanelDirection: String = ""  // Track current panel direction


    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val direction = intent?.getStringExtra("PANEL_DIRECTION") ?: "RIGHT"  // Default to right panel
        currentPanelDirection = direction  // Store the current panel's direction
        val panelHeight = (Resources.getSystem().displayMetrics.heightPixels / 2)
        val edgeWidth = 50

        // Left Edge Swipe Detection
        leftEdgeView = View(this)
        val leftParams = WindowManager.LayoutParams(
            edgeWidth, panelHeight,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        leftParams.gravity = Gravity.START
        leftEdgeView.setBackgroundColor(getColor(R.color.transparent))
        leftEdgeView.setOnTouchListener(EdgeSwipeListener(isLeftEdge = true))  // Detect left edge swipes
        leftEdgeView.viewTreeObserver.addOnGlobalLayoutListener {
            val exclusionRect = Rect(0, 0, edgeWidth, leftEdgeView.height)
            leftEdgeView.systemGestureExclusionRects = listOf(exclusionRect)
        }
        windowManager.addView(leftEdgeView, leftParams)

        // Right Edge Swipe Detection
        rightEdgeView = View(this)
        val rightParams = WindowManager.LayoutParams(
            edgeWidth, panelHeight,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        rightParams.gravity = Gravity.END
        rightEdgeView.setBackgroundColor(getColor(R.color.transparent))
        rightEdgeView.setOnTouchListener(EdgeSwipeListener(isRightEdge = true))  // Detect right edge swipes
        rightEdgeView.viewTreeObserver.addOnGlobalLayoutListener {
            val exclusionRect = Rect(0, 0, edgeWidth, leftEdgeView.height)
            leftEdgeView.systemGestureExclusionRects = listOf(exclusionRect)
        }
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