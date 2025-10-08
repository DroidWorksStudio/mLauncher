package com.github.droidworksstudio.mlauncher.ui.widgets

import android.annotation.SuppressLint
import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import com.github.droidworksstudio.common.AppLogger
import com.github.droidworksstudio.common.getLocalizedString
import com.github.droidworksstudio.mlauncher.R
import com.github.droidworksstudio.mlauncher.helper.getInstallSource
import com.github.droidworksstudio.mlauncher.ui.components.LockedBottomSheetDialog
import kotlin.math.abs

@SuppressLint("ClickableViewAccessibility", "ViewConstructor")
class ResizableWidgetWrapper(
    context: Context,
    val hostView: AppWidgetHostView,
    val widgetInfo: AppWidgetProviderInfo,
    val appWidgetHost: AppWidgetHost,
    val onUpdate: () -> Unit,
    private val gridColumns: Int,
    private val cellMargin: Int,
    val defaultCellsW: Int = 1,
    val defaultCellsH: Int = 1
) : FrameLayout(context) {

    companion object {
        private const val TAG = "ResizableWidgetWrapper"
    }

    var currentCol: Int = 0
    var currentRow: Int = 0

    private var lastX = 0f
    private var lastY = 0f
    private val minSize = 100

    var isResizeMode = false
    private val handleSize = 50

    private val topHandle = createHandle()
    private val bottomHandle = createHandle()
    private val leftHandle = createHandle()
    private val rightHandle = createHandle()

    private var activeDialog: LockedBottomSheetDialog? = null
    private var ghostView: View? = null

    private val gridOverlay: View = object : View(context) {
        @SuppressLint("DrawAllocation")
        override fun onDraw(canvas: Canvas) {
            AppLogger.v(TAG, "🎨 Drawing grid overlay")
            super.onDraw(canvas)
            val paint = Paint().apply {
                color = 0x55FFFFFF // semi-transparent white
                style = Paint.Style.STROKE
                strokeWidth = 5f
            }

            val parentFrame = parent as? FrameLayout ?: return

            // Compute width/height per cell (no margins)
            val cellWidth = parentFrame.width / gridColumns.toFloat()
            val rows = (parentFrame.height / cellWidth).toInt()
            val cellHeight = cellWidth // square cells

            for (row in 0 until rows) {
                for (col in 0 until gridColumns) {
                    val left = col * cellWidth
                    val top = row * cellHeight
                    val right = left + cellWidth
                    val bottom = top + cellHeight

                    // Draw top edge only for the first row
                    if (row == 0) canvas.drawLine(left, top, right, top, paint)
                    // Draw left edge only for the first column
                    if (col == 0) canvas.drawLine(left, top, left, bottom, paint)
                    // Always draw right and bottom edges
                    canvas.drawLine(right, top, right, bottom, paint)
                    canvas.drawLine(left, bottom, right, bottom, paint)
                }
            }
        }
    }.apply {
        visibility = GONE
    }


    init {

        AppLogger.d(TAG, "🧩 Initializing wrapper for widget: ${widgetInfo.provider.packageName}")

        // Calculate pixel width/height from cells
        post {
            val parentFrame = parent as? FrameLayout
            val parentWidth = parentFrame?.width ?: context.resources.displayMetrics.widthPixels
            val cellWidth = (parentWidth - (cellMargin * (gridColumns - 1))) / gridColumns
            val widthPx = maxOf(cellWidth, defaultCellsW * cellWidth + (defaultCellsW - 1) * cellMargin)
            val heightPx = maxOf(cellWidth, defaultCellsH * cellWidth + (defaultCellsH - 1) * cellMargin)

            layoutParams = LayoutParams(widthPx, heightPx)
            AppLogger.d(TAG, "📐 layoutParams set to ${widthPx}x${heightPx}")

            // Ensure hostView fills this wrapper and notify provider of our size
            fillHostView(widthPx, heightPx)
        }

        addView(
            hostView, LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
        )
        AppLogger.d(TAG, "✅ HostView added to wrapper")

        topHandle.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, handleSize).apply { gravity = Gravity.TOP }
        bottomHandle.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, handleSize).apply { gravity = Gravity.BOTTOM }
        leftHandle.layoutParams = LayoutParams(handleSize, LayoutParams.MATCH_PARENT).apply { gravity = Gravity.START }
        rightHandle.layoutParams = LayoutParams(handleSize, LayoutParams.MATCH_PARENT).apply { gravity = Gravity.END }

        addView(topHandle)
        addView(bottomHandle)
        addView(leftHandle)
        addView(rightHandle)

        topHandle.bringToFront()
        bottomHandle.bringToFront()
        leftHandle.bringToFront()
        rightHandle.bringToFront()

        setHandlesVisible(false)
        attachResizeAndDragHandlers()
    }

    private fun fillHostView(parentWidth: Int = width, parentHeight: Int = height) {
        AppLogger.d(TAG, "fillHostView($parentWidth x $parentHeight) called")
        // 1. Force hostView to fill THIS wrapper
        hostView.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
        hostView.requestLayout()

        // 2. Use parent’s width/height for widget sizing
        post {
            if (parentWidth <= 0 || parentHeight <= 0) {
                AppLogger.w(TAG, "⚠️ Skipping fillHostView — invalid size ($parentWidth x $parentHeight)")
                return@post
            }

            try {
                val options = Bundle().apply {
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, parentWidth / 4)
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, parentWidth)
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, parentHeight / 4)
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, parentHeight)
                }

                val appWidgetManager = AppWidgetManager.getInstance(context)
                appWidgetManager.updateAppWidgetOptions(hostView.appWidgetId, options)

                AppLogger.i(TAG, "✅ fillHostView: using parent size width=$parentWidth, height=$parentHeight")
            } catch (e: Exception) {
                AppLogger.e(TAG, "❌ Failed to update widget options: ${e.message}")
            }
        }
    }

    private fun createHandle(): View = View(context).apply {
        setBackgroundColor("#26C6A0F6".toColorInt())
        visibility = GONE
    }

    fun setHandlesVisible(visible: Boolean) {
        AppLogger.d(TAG, "setHandlesVisible($visible)")
        val state = if (visible) VISIBLE else GONE
        topHandle.visibility = state
        bottomHandle.visibility = state
        leftHandle.visibility = state
        rightHandle.visibility = state

        if (visible) {
            showGridOverlay()
            // 🔥 Bring handles and grid on top again after adding overlay
            gridOverlay.bringToFront()
            topHandle.bringToFront()
            bottomHandle.bringToFront()
            leftHandle.bringToFront()
            rightHandle.bringToFront()
        } else {
            hideGridOverlay()
        }
    }

    private var activeResizeHandle: String? = null

    private fun attachResizeAndDragHandlers() {
        val handles = listOf(topHandle, bottomHandle, leftHandle, rightHandle)
        val sides = listOf("TOP", "BOTTOM", "LEFT", "RIGHT")

        // --- Attach resize listeners to handles ---
        handles.zip(sides).forEach { (handle, side) ->
            handle.setOnTouchListener { _, event ->
                if (!isResizeMode) return@setOnTouchListener false

                val lp = layoutParams as? LayoutParams ?: return@setOnTouchListener false

                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        activeResizeHandle = side
                        lastX = event.rawX
                        lastY = event.rawY
                        parent?.requestDisallowInterceptTouchEvent(true)
                    }

                    MotionEvent.ACTION_MOVE -> {
                        activeResizeHandle?.let { resizeSide ->
                            val dx = (event.rawX - lastX).toInt()
                            val dy = (event.rawY - lastY).toInt()

                            when (resizeSide) {
                                "TOP" -> {
                                    lp.height = (lp.height - dy).coerceAtLeast(minSize); lp.topMargin += dy
                                }

                                "BOTTOM" -> lp.height = (lp.height + dy).coerceAtLeast(minSize)
                                "LEFT" -> {
                                    lp.width = (lp.width - dx).coerceAtLeast(minSize); lp.leftMargin += dx
                                }

                                "RIGHT" -> lp.width = (lp.width + dx).coerceAtLeast(minSize)
                            }

                            layoutParams = lp
                            fillHostView(lp.width, lp.height)
                            lastX = event.rawX
                            lastY = event.rawY
                        }
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        activeResizeHandle?.let { snapResizeToGrid(it) }
                        activeResizeHandle = null
                        onUpdate()
                    }
                }
                true
            }
        }

        // --- Attach drag + long-press only to non-handle areas ---
        if (!isResizeMode) attachDragToWrapperAndChildren(this, skipViews = handles)
    }


    private fun attachDragToWrapperAndChildren(root: View, skipViews: List<View> = emptyList()) {

        fun attachDrag(view: View) {
            // Attach long-press menu and drag to this view
            val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onLongPress(e: MotionEvent) {
                    if (!isResizeMode) {  // <-- Only show menu if NOT resizing
                        showWidgetMenu()
                    }
                }
            })

            var dialogDismissed = false

            view.setOnTouchListener { v, event ->
                // Skip handles
                if (v in skipViews) return@setOnTouchListener false
                gestureDetector.onTouchEvent(event)
                if (isResizeMode) return@setOnTouchListener false

                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        dialogDismissed = false

                        lastX = event.rawX
                        lastY = event.rawY

                        val parentFrame = parent as? FrameLayout
                        if (parentFrame != null) {
                            ghostView = View(context).apply {
                                setBackgroundColor("#26C6A0F6".toColorInt())
                                layoutParams = LayoutParams(width, height).apply {
                                    if (layoutParams is LayoutParams) {
                                        leftMargin = (layoutParams as LayoutParams).leftMargin
                                        topMargin = (layoutParams as LayoutParams).topMargin
                                    }
                                }
                            }
                            parentFrame.addView(ghostView)
                        }
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - lastX
                        val dy = event.rawY - lastY

                        translationX += dx
                        translationY += dy

                        lastX = event.rawX
                        lastY = event.rawY

                        updateGhostPosition()

                        // Dismiss only once if movement exceeds 10 pixels in any direction
                        if (!dialogDismissed && (abs(dx) > 5 || abs(dy) > 5)) {
                            activeDialog?.dismiss()
                            dialogDismissed = true
                        }
                    }


                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        dialogDismissed = false
                        snapToGrid()
                        (ghostView?.parent as? ViewGroup)?.removeView(ghostView)
                        ghostView = null
                        onUpdate()
                    }
                }

                true
            }
        }

        attachDrag(root)
    }

    private fun updateGhostPosition() {
        val parentFrame = parent as? FrameLayout ?: return

        val parentWidth = parentFrame.width.coerceAtLeast(1)
        val parentHeight = parentFrame.height.coerceAtLeast(1)

        val cellWidth = ((parentWidth - (cellMargin * (gridColumns - 1))) / gridColumns).coerceAtLeast(1)
        val cellHeight = cellWidth

        val maxX = (parentWidth - width).coerceAtLeast(0)
        val maxY = (parentHeight - height).coerceAtLeast(0)

        val col = ((translationX + cellWidth / 2) / (cellWidth + cellMargin))
            .toInt()
            .coerceIn(0, gridColumns - 1)
        val row = ((translationY + cellHeight / 2) / (cellHeight + cellMargin))
            .toInt()
            .coerceAtLeast(0)

        val newX = (col * (cellWidth + cellMargin)).coerceIn(0, maxX)
        val newY = (row * (cellHeight + cellMargin)).coerceIn(0, maxY)

        ghostView?.layoutParams = (ghostView?.layoutParams as LayoutParams).apply {
            leftMargin = newX
            topMargin = newY
            width = this@ResizableWidgetWrapper.width
            height = this@ResizableWidgetWrapper.height
        }
        ghostView?.requestLayout()
    }

    fun snapToGrid() {
        val parentFrame = parent as? FrameLayout ?: return

        val parentWidth = parentFrame.width.coerceAtLeast(1)
        val parentHeight = parentFrame.height.coerceAtLeast(1)

        val cellWidth = ((parentWidth - (cellMargin * (gridColumns - 1))) / gridColumns).coerceAtLeast(1)
        val cellHeight = cellWidth

        // Max translation ensures widget never leaves parent
        val maxX = (parentWidth - width).coerceAtLeast(0)
        val maxY = (parentHeight - height).coerceAtLeast(0)

        val col = ((translationX + cellWidth / 2) / (cellWidth + cellMargin))
            .toInt()
            .coerceIn(0, gridColumns - 1)

        val row = ((translationY + cellHeight / 2) / (cellHeight + cellMargin))
            .toInt()
            .coerceAtLeast(0) // row may expand beyond grid if needed

        translationX = (col * (cellWidth + cellMargin)).toFloat().coerceIn(0f, maxX.toFloat())
        translationY = (row * (cellHeight + cellMargin)).toFloat().coerceIn(0f, maxY.toFloat())

        currentCol = col
        currentRow = row
    }

    private fun snapResizeToGrid(side: String) {
        val parentFrame = parent as? FrameLayout ?: return
        val lp = layoutParams as? LayoutParams ?: return

        val parentWidth = parentFrame.width.coerceAtLeast(1)
        val parentHeight = parentFrame.height.coerceAtLeast(1)
        val cellSize = ((parentWidth - (cellMargin * (gridColumns - 1))) / gridColumns).coerceAtLeast(1)

        val maxWidth = (parentWidth - lp.leftMargin).coerceAtLeast(minSize)
        val maxHeight = (parentHeight - lp.topMargin).coerceAtLeast(minSize)

        when (side) {
            "TOP" -> {
                val bottom = lp.topMargin + lp.height
                val snappedTop = ((lp.topMargin + cellSize / 2) / (cellSize + cellMargin)) * (cellSize + cellMargin)
                lp.topMargin = snappedTop.coerceIn(0, bottom - minSize)
                lp.height = (bottom - lp.topMargin).coerceAtLeast(minSize).coerceAtMost(maxHeight)
            }

            "BOTTOM" -> {
                val snappedBottom = ((lp.topMargin + lp.height + cellSize / 2) / (cellSize + cellMargin)) * (cellSize + cellMargin)
                lp.height = (snappedBottom - lp.topMargin).coerceAtLeast(minSize).coerceAtMost(maxHeight)
            }

            "LEFT" -> {
                val right = lp.leftMargin + lp.width
                val snappedLeft = ((lp.leftMargin + cellSize / 2) / (cellSize + cellMargin)) * (cellSize + cellMargin)
                lp.leftMargin = snappedLeft.coerceIn(0, right - minSize)
                lp.width = (right - lp.leftMargin).coerceAtLeast(minSize).coerceAtMost(maxWidth)
            }

            "RIGHT" -> {
                val snappedRight = ((lp.leftMargin + lp.width + cellSize / 2) / (cellSize + cellMargin)) * (cellSize + cellMargin)
                lp.width = (snappedRight - lp.leftMargin).coerceAtLeast(minSize).coerceAtMost(maxWidth)
            }
        }

        layoutParams = lp
        fillHostView(lp.width, lp.height)
    }

    fun showWidgetMenu() {
        val dialog = LockedBottomSheetDialog(context)
        activeDialog = dialog
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        fun addMenuItem(title: String, onClick: () -> Unit) {
            val item = TextView(context).apply {
                text = title
                textSize = 16f
                setPadding(16, 32, 16, 32)
                setOnClickListener {
                    onClick()
                    dialog.dismiss()
                }
            }
            container.addView(item)
        }

        if (isResizeMode) {
            addMenuItem(getLocalizedString(R.string.widgets_exit_resize)) {
                isResizeMode = false
                setHandlesVisible(false)
                reloadActivity()
            }
        } else {
            addMenuItem(getLocalizedString(R.string.widgets_resize)) {
                isResizeMode = true
                setHandlesVisible(true)
            }
        }

        addMenuItem(getLocalizedString(R.string.widgets_remove)) {
            appWidgetHost.deleteAppWidgetId(hostView.appWidgetId)
            (parent as? ViewGroup)?.removeView(this)
            onUpdate()
        }

        addMenuItem(getLocalizedString(R.string.widgets_open)) {
            context.packageManager.getLaunchIntentForPackage(widgetInfo.provider.packageName)?.let {
                context.startActivity(it)
            }
        }

        // Settings (only if widget has a config activity)
        widgetInfo.configure?.let { configureComponent ->
            addMenuItem(getLocalizedString(R.string.widgets_settings)) {
                val intent = Intent().apply {
                    component = configureComponent
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, hostView.appWidgetId)
                }
                context.startActivity(intent)
            }
        }

        // View in Store (only if installed from Google Play)
        val packageManager = context.packageManager
        val installerPackage = getInstallSource(packageManager, widgetInfo.provider.packageName)
        when (installerPackage) {
            "Google Play Store" -> { // Google Play
                addMenuItem(getLocalizedString(R.string.widgets_view_in_store)) {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            "market://details?id=${widgetInfo.provider.packageName}".toUri()
                        )
                    )
                }
            }

            "Amazon Appstore" -> { // Amazon Appstore
                addMenuItem(getLocalizedString(R.string.widgets_view_in_store)) {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            "amzn://apps/android?p=${widgetInfo.provider.packageName}".toUri()
                        )
                    )
                }
            }

            else -> {
                // Debug / unknown installer, do not show "View in Store"
                AppLogger.d(TAG, "WidgetMenu: Skipping '${getLocalizedString(R.string.widgets_view_in_store)}': unrecognized installer package='$installerPackage'")
            }
        }

        dialog.setOnDismissListener { activeDialog = null }
        dialog.setContentView(container)
        dialog.show()
    }

    fun reloadActivity() {
        val activity = context as? Activity ?: return

        val intent = activity.intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        activity.finish()

        if (android.os.Build.VERSION.SDK_INT >= 34) {
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            activity.overridePendingTransition(0, 0)
        }

        activity.startActivity(intent)

        if (android.os.Build.VERSION.SDK_INT >= 34) {
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            activity.overridePendingTransition(0, 0)
        }
    }


    private fun showGridOverlay() {
        val parentFrame = parent as? FrameLayout ?: return

        // Add the overlay to the parent if it hasn't been added yet
        if (gridOverlay.parent == null) {
            parentFrame.addView(
                gridOverlay,
                LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT
                )
            )
        }

        // Make sure it’s on top of everything else
        gridOverlay.bringToFront()
        gridOverlay.visibility = VISIBLE
    }

    private fun hideGridOverlay() {
        gridOverlay.visibility = GONE
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (!isResizeMode || ev == null) return super.onInterceptTouchEvent(ev)

        // Only track ACTION_DOWN to detect which handle, don't intercept yet
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            activeResizeHandle = getHandleAt(ev.rawX, ev.rawY)
            lastX = ev.rawX
            lastY = ev.rawY
        }

        // Intercept only if touch is **outside any handle**
        return activeResizeHandle == null
    }

    private fun getHandleAt(x: Float, y: Float): String? {
        val handles = mapOf(
            topHandle to "TOP",
            bottomHandle to "BOTTOM",
            leftHandle to "LEFT",
            rightHandle to "RIGHT"
        )
        val location = IntArray(2)
        for ((view, name) in handles) {
            view.getLocationOnScreen(location)
            val left = location[0].toFloat()
            val top = location[1].toFloat()
            val right = left + view.width
            val bottom = top + view.height
            if (x in left..right && y in top..bottom) return name
        }
        return null
    }

}