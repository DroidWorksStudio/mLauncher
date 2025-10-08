package com.github.droidworksstudio.mlauncher.ui.widgets

import android.annotation.SuppressLint
import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.addCallback
import androidx.core.view.isVisible
import com.github.droidworksstudio.common.AppLogger
import com.github.droidworksstudio.common.appWidgetManager
import com.github.droidworksstudio.common.getLocalizedString
import com.github.droidworksstudio.mlauncher.R
import com.github.droidworksstudio.mlauncher.ui.BaseFragment
import com.github.droidworksstudio.mlauncher.ui.components.LockedBottomSheetDialog
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class SavedWidget(
    val appWidgetId: Int,
    val col: Int,
    val row: Int,
    var width: Int,
    var height: Int,
    var cellsW: Int,
    var cellsH: Int
)

data class AppWidgetGroup(
    val appName: String,
    val appIcon: Drawable?,
    val widgets: MutableList<AppWidgetProviderInfo>
)

@Suppress("DEPRECATION")
class WidgetFragment : BaseFragment() {

    private lateinit var widgetGrid: FrameLayout
    private lateinit var emptyPlaceholder: TextView
    lateinit var appWidgetManager: AppWidgetManager
    lateinit var appWidgetHost: AppWidgetHost
    private val widgetWrappers = mutableListOf<ResizableWidgetWrapper>()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val savedWidgetAdapter = moshi.adapter(Array<SavedWidget>::class.java)

    companion object {
        private const val TAG = "BaseWidgets"
        const val APP_WIDGET_HOST_ID = 1024
        const val GRID_COLUMNS = 14
        const val CELL_MARGIN = 16
        const val DEFAULT_WIDGET_CELLS_W = 1
        const val DEFAULT_WIDGET_CELLS_H = 1
    }

    private var activeGridDialog: LockedBottomSheetDialog? = null
    private var lastWidgetInfo: AppWidgetProviderInfo? = null
    private val widgetFileName = "widgets.json"
    private var placeholderVisible = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_widget_container, container, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back press handling for exiting resize mode
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            val resizeWidget = widgetWrappers.firstOrNull { it.isResizeMode }
            if (resizeWidget != null) {
                AppLogger.i(TAG, "🔄 Exiting resize mode for widgetId=${resizeWidget.hostView.appWidgetId}")
                resizeWidget.isResizeMode = false
                resizeWidget.setHandlesVisible(false)
                resizeWidget.reloadParentFragment()
            } else {
                isEnabled = false
                requireActivity().onBackPressed()
            }
        }

        // Initialize widget grid
        widgetGrid = view.findViewById(R.id.widget_grid)
        AppLogger.i(TAG, "🟢 Widget grid initialized")

        // Create empty placeholder programmatically
        emptyPlaceholder = TextView(requireContext()).apply {
            text = context.getString(R.string.widgets_not_added)
            textSize = 16f
            setTextColor(resources.getColor(android.R.color.darker_gray, null))
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        widgetGrid.addView(
            emptyPlaceholder, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        )
        AppLogger.i(TAG, "🟢 Empty placeholder added to widgetGrid")

        // Setup AppWidgetManager and Host
        appWidgetManager = requireContext().appWidgetManager
        appWidgetHost = AppWidgetHost(requireContext(), APP_WIDGET_HOST_ID)
        appWidgetHost.startListening()
        AppLogger.i(TAG, "🟢 AppWidgetHost started listening")

        // Long click menu
        widgetGrid.setOnLongClickListener {
            showGridMenu()
            true
        }

        // Post widget loading after layout to prevent jumps
        widgetGrid.post {
            (activity as? WidgetActivity)?.flushPendingWidgets()
            AppLogger.i(TAG, "🟢 Pending widgets flushed and grid visible")
        }

        AppLogger.i(TAG, "🟢 WidgetFragment onViewCreated setup complete")
    }

    override fun onResume() {
        super.onResume()
        AppLogger.i(TAG, "🔄 WidgetFragment onResume, widgets restored")
        updateEmptyPlaceholder(widgetWrappers)
    }

    private val pendingWidgetsList = mutableListOf<Pair<AppWidgetProviderInfo, Int>>()

    fun postPendingWidgets(widgets: List<Pair<AppWidgetProviderInfo, Int>>) {
        pendingWidgetsList.addAll(widgets)
        widgetGrid.post {
            if (!isAdded || !isViewCreated()) return@post
            AppLogger.i(TAG, "🟢 Posting ${pendingWidgetsList.size} pending widgets")

            // Add all pending widgets
            pendingWidgetsList.forEach { (info, id) ->
                createWidgetWrapperSafe(info, id)
            }
            pendingWidgetsList.clear()

            // Restore saved widgets after pending
            restoreWidgets()
            updateEmptyPlaceholder(widgetWrappers)
        }
    }


    /** Grid menu for adding/resetting widgets */
    private fun showGridMenu() {
        activeGridDialog?.dismiss()
        val bottomSheetDialog = LockedBottomSheetDialog(requireContext())
        activeGridDialog = bottomSheetDialog
        AppLogger.d(TAG, "🎛️ Showing widget grid menu")

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        fun addOption(title: String, action: () -> Unit) {
            val option = TextView(requireContext()).apply {
                text = title
                textSize = 16f
                setPadding(16, 16, 16, 16)
                setOnClickListener { action(); bottomSheetDialog.dismiss() }
            }
            container.addView(option)
        }

        addOption(getLocalizedString(R.string.widgets_add_widget)) { showCustomWidgetPicker() }
        addOption(getLocalizedString(R.string.widgets_reset_widget)) { resetAllWidgets() }

        bottomSheetDialog.setContentView(container)
        bottomSheetDialog.show()
    }

    private fun resetAllWidgets() {
        AppLogger.w(TAG, "🧹 Resetting all widgets")
        widgetWrappers.forEach { wrapper ->
            safeRemoveWidget(wrapper.hostView.appWidgetId)
        }
        widgetWrappers.clear()
        widgetGrid.removeAllViews()
        widgetGrid.addView(emptyPlaceholder)
        saveWidgets()
        updateEmptyPlaceholder(widgetWrappers)
        AppLogger.i(TAG, "🧹 All widgets cleared and placeholder shown")
    }

//    private fun resetAllWidgets() {
//        AppLogger.w(TAG, "🧹 Resetting all widgets positions")
//
//        widgetWrappers.forEach { wrapper ->
//            wrapper.currentCol = 0
//            wrapper.currentRow = 0
//
//            // Snap widget to top-left in the grid
//            val parentFrame = wrapper.parent as? FrameLayout
//            parentFrame?.let {
//                wrapper.translationX = 0f
//                wrapper.translationY = 0f
//
//                val lp = wrapper.layoutParams as? FrameLayout.LayoutParams
//                lp?.let {
//                    it.leftMargin = 0
//                    it.topMargin = 0
//                    wrapper.layoutParams = it
//                }
//
//                wrapper.snapToGrid() // enforce grid snapping
//            }
//        }
//
//        saveWidgets() // Save their reset positions
//        updateEmptyPlaceholder(widgetWrappers) // refresh placeholder if needed
//
//        AppLogger.i(TAG, "🧹 All widgets reset to top-left")
//    }


    private fun showCustomWidgetPicker() {
        val widgets = appWidgetManager.installedProviders
        val pm = requireContext().packageManager

        val grouped = widgets.groupBy { it.provider.packageName }.map { (pkg, widgetList) ->
            val appInfo = try {
                pm.getApplicationInfo(pkg, 0)
            } catch (_: Exception) {
                null
            }
            val appName = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: pkg
            val appIcon = appInfo?.let { pm.getApplicationIcon(it) }
            AppWidgetGroup(appName, appIcon, widgetList.toMutableList())
        }.sortedBy { it.appName.lowercase() }

        AppLogger.d(TAG, "🧩 Showing custom widget picker with ${grouped.size} apps")

        val container = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(16, 16, 16, 16) }
        val scrollView = ScrollView(requireContext()).apply { addView(container) }

        activeGridDialog?.dismiss()
        val bottomSheetDialog = LockedBottomSheetDialog(requireContext())
        activeGridDialog = bottomSheetDialog
        bottomSheetDialog.setContentView(scrollView)
        bottomSheetDialog.setTitle(getLocalizedString(R.string.widgets_select_widget))
        bottomSheetDialog.show()

        grouped.forEach { group ->
            val appRow = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(8, 16, 8, 16)
                gravity = Gravity.CENTER_VERTICAL
            }
            val iconView = ImageView(requireContext()).apply {
                group.appIcon?.let { setImageDrawable(it) }
                layoutParams = LinearLayout.LayoutParams(64, 64)
            }
            val labelView = TextView(requireContext()).apply {
                text = group.appName
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setPadding(16, 0, 0, 0)
            }
            val expandIcon = TextView(requireContext()).apply { text = "▼"; textSize = 16f }
            appRow.addView(iconView)
            appRow.addView(labelView, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            appRow.addView(expandIcon)

            val widgetContainer = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
            group.widgets.forEach { widgetInfo ->
                val widgetRow = TextView(requireContext()).apply {
                    text = getLocalizedString(R.string.pass_a_string, widgetInfo.label)
                    textSize = 14f
                    setPadding(32, 12, 12, 12)
                    setOnClickListener {
                        AppLogger.i(TAG, "➕ Selected widget ${widgetInfo.label} to add")
                        addWidget(widgetInfo)
                        bottomSheetDialog.dismiss()
                    }
                }
                widgetContainer.addView(widgetRow)
            }

            appRow.setOnClickListener {
                if (widgetContainer.isVisible) {
                    widgetContainer.visibility = View.GONE
                    expandIcon.text = "▼"
                } else {
                    widgetContainer.visibility = View.VISIBLE
                    expandIcon.text = "▲"
                }
            }

            container.addView(appRow)
            container.addView(widgetContainer)
        }
    }

    /** Public entry point: add a widget */
    private fun addWidget(widgetInfo: AppWidgetProviderInfo) {
        lastWidgetInfo = widgetInfo
        val widgetId = appWidgetHost.allocateAppWidgetId()
        AppLogger.d(TAG, "🆕 Allocated appWidgetId=$widgetId for provider=${widgetInfo.provider.packageName}")

        val manager = requireContext().appWidgetManager

        // Check if binding is allowed
        val bound = manager.bindAppWidgetIdIfAllowed(widgetId, widgetInfo.provider)
        if (bound) {
            AppLogger.i(TAG, "✅ Bound widget immediately: widgetId=$widgetId")
            maybeConfigureOrCreate(widgetInfo, widgetId)
        } else {
            AppLogger.w(TAG, "🔒 Widget bind not allowed, requesting permission")
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, widgetInfo.provider)
            }

            (requireActivity() as WidgetActivity).launchWidgetPermission(intent) { resultCode, returnedId, _ ->
                handleWidgetResult(resultCode, returnedId)
            }
        }
    }

    /** Handle result from binding or configuration */
    private fun handleWidgetResult(resultCode: Int, appWidgetId: Int) {
        when (resultCode) {
            Activity.RESULT_OK -> {
                AppLogger.i(TAG, "✅ Widget bind/config OK for appWidgetId=$appWidgetId")
                lastWidgetInfo?.let { maybeConfigureOrCreate(it, appWidgetId) }
                lastWidgetInfo = null
            }

            Activity.RESULT_CANCELED -> {
                AppLogger.w(TAG, "❌ Widget bind/config canceled for appWidgetId=$appWidgetId")
                safeRemoveWidget(appWidgetId)
            }
        }
    }

    /** Check if widget has configuration, then create wrapper safely */
    private fun maybeConfigureOrCreate(widgetInfo: AppWidgetProviderInfo, widgetId: Int) {
        if (widgetInfo.configure != null) {
            AppLogger.i(TAG, "⚙️ Widget has configuration, launching config activity")
            val intent = Intent().apply {
                component = widgetInfo.configure
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }

            (activity as? WidgetActivity)?.let { widgetActivity ->
                widgetActivity.launchWidgetPermission(intent) { resultCode, returnedId, _ ->
                    if (resultCode == Activity.RESULT_OK) {
                        AppLogger.i(TAG, "✅ Widget configured, creating wrapper: $returnedId")
                        // Ensure widgetInfo is captured properly in the lambda
                        widgetActivity.safeCreateWidget(widgetInfo, returnedId)
                    } else {
                        AppLogger.w(TAG, "❌ Widget config canceled, removing: $returnedId")
                        safeRemoveWidget(returnedId)
                    }
                }
            }
        } else {
            AppLogger.i(TAG, "📦 No configuration needed, creating wrapper immediately")
            createWidgetWrapperSafe(widgetInfo, widgetId)
        }
    }

    fun createWidgetWrapperSafe(widgetInfo: AppWidgetProviderInfo, appWidgetId: Int) {
        if (!isAdded) {
            AppLogger.w(TAG, "⚠️ Skipping widget creation, fragment not attached")
            return
        }
        widgetGrid.post {
            createWidgetWrapper(widgetInfo, appWidgetId)
        }
    }

    fun createWidgetWrapper(widgetInfo: AppWidgetProviderInfo, appWidgetId: Int) {
        val hostView = try {
            val widgetContext = try {
                requireContext().createPackageContext(
                    widgetInfo.provider.packageName,
                    Context.CONTEXT_IGNORE_SECURITY or Context.CONTEXT_INCLUDE_CODE
                )
            } catch (_: Exception) {
                requireContext()
            }

            appWidgetHost.createView(widgetContext, appWidgetId, widgetInfo)
        } catch (e: Exception) {
            AppLogger.e(TAG, "⚠️ Failed to create widgetId=$appWidgetId, removing", e)
            safeRemoveWidget(appWidgetId)
            return
        }
        AppLogger.d(TAG, "🖼️ Creating wrapper for widgetId=$appWidgetId, provider=${widgetInfo.provider.packageName}")

        val cellWidth = (widgetGrid.width - (GRID_COLUMNS - 1) * CELL_MARGIN) / GRID_COLUMNS
        val defaultCellsW = ((widgetInfo.minWidth + CELL_MARGIN) / (cellWidth + CELL_MARGIN)).coerceAtLeast(1)
        val defaultCellsH = ((widgetInfo.minHeight + CELL_MARGIN) / (cellWidth + CELL_MARGIN)).coerceAtLeast(1)

        AppLogger.v(TAG, "📐 Default size for widgetId=$appWidgetId: ${widgetInfo.minWidth}x${widgetInfo.minHeight} → $defaultCellsW x $defaultCellsH cells")

        val wrapper = ResizableWidgetWrapper(
            requireContext(), hostView, widgetInfo, appWidgetHost,
            ::saveWidgets, GRID_COLUMNS, CELL_MARGIN, defaultCellsW, defaultCellsH
        )

        addWrapperToGrid(wrapper)
        AppLogger.i(TAG, "✅ Wrapper created for widgetId=$appWidgetId")
        updateEmptyPlaceholder(widgetWrappers)
        saveWidgets()
        logGridSnapshot()
    }

    private fun safeRemoveWidget(widgetId: Int) {
        try {
            AppLogger.w(TAG, "🗑️ Removing widgetId=$widgetId due to error")
            appWidgetHost.deleteAppWidgetId(widgetId)
            val removed = widgetWrappers.removeAll { it.hostView.appWidgetId == widgetId }
            if (removed) widgetGrid.removeView(widgetGrid.findViewById(widgetId))
            saveWidgets()
            updateEmptyPlaceholder(widgetWrappers)
        } catch (e: Exception) {
            AppLogger.e(TAG, "❌ Failed to remove widgetId=$widgetId", e)
        }
    }

    private fun addWrapperToGrid(wrapper: ResizableWidgetWrapper) {
        val id = wrapper.hostView.appWidgetId
        AppLogger.d(TAG, "➕ Adding wrapper to grid for widgetId=$id")

        // Calculate grid cell dimensions
        val parentWidth = widgetGrid.width.coerceAtLeast(1)
        val cellWidth = (parentWidth - CELL_MARGIN * (GRID_COLUMNS - 1)) / GRID_COLUMNS
        val cellHeight = cellWidth.coerceAtLeast(1)

        // Ensure wrapper size is at least 1x1 cell
        val wrapperWidth = (wrapper.defaultCellsW.coerceAtLeast(1) * cellWidth)
        val wrapperHeight = (wrapper.defaultCellsH.coerceAtLeast(1) * cellHeight)

        wrapper.layoutParams = FrameLayout.LayoutParams(wrapperWidth, wrapperHeight)

        // Build set of occupied cells
        val occupied = widgetWrappers.map { w ->
            val wCol = ((w.translationX + cellWidth / 2) / (cellWidth + CELL_MARGIN)).toInt()
            val wRow = ((w.translationY + cellHeight / 2) / (cellHeight + CELL_MARGIN)).toInt()
            Pair(wCol, wRow)
        }

        AppLogger.v(TAG, "📊 Occupied cells: $occupied")

        // Find first free spot (top-left to bottom-right)
        var placed = false
        var row = 0
        var col = 0
        loop@ for (r in 0..1000) { // arbitrary large number
            for (c in 0 until GRID_COLUMNS) {
                if (occupied.none { it.first == c && it.second == r }) {
                    col = c
                    row = r
                    placed = true
                    AppLogger.d(TAG, "📍 Empty cell found at row=$row col=$col for widgetId=$id")
                    break@loop
                }
            }
        }

        if (!placed) {
            AppLogger.w(TAG, "⚠️ No free cell found, placing widget at top-left")
            col = 0
            row = 0
        }

        // Set translation to snap widget to grid
        wrapper.translationX = col * (cellWidth + CELL_MARGIN).toFloat()
        wrapper.translationY = row * (cellHeight + CELL_MARGIN).toFloat()

        addWrapperSafely(wrapper)
        AppLogger.i(TAG, "✅ Placed widgetId=$id at row=$row col=$col | size=${wrapperWidth}x${wrapperHeight}")
    }

    private fun addWrapperSafely(wrapper: ResizableWidgetWrapper) {
        val id = wrapper.hostView.appWidgetId

        val existing = widgetWrappers.find { it.hostView.appWidgetId == id }
        if (existing != null) {
            AppLogger.w(TAG, "♻️ Replacing existing wrapper for appWidgetId=$id")
            widgetGrid.removeView(existing)
            widgetWrappers.remove(existing)
        }

        widgetGrid.addView(wrapper)
        widgetWrappers.add(wrapper)

        AppLogger.i(TAG, "🟩 Added wrapper → id=$id | wrappers=${widgetWrappers.size}")
    }

    /** Save widgets state to JSON */
    private fun saveWidgets() {
        val parentWidth = widgetGrid.width.coerceAtLeast(1)
        val cellWidth = (parentWidth - CELL_MARGIN * (GRID_COLUMNS - 1)) / GRID_COLUMNS
        val cellHeight = cellWidth.coerceAtLeast(1)

        val seenIds = HashSet<Int>()
        val savedList = mutableListOf<SavedWidget>()
        var skippedCount = 0

        widgetWrappers.forEachIndexed { index, wrapper ->
            val id = wrapper.hostView.appWidgetId
            if (!seenIds.add(id)) {
                AppLogger.w(TAG, "⚠️ SKIP duplicate appWidgetId=$id (wrapper #$index)")
                skippedCount++
                return@forEachIndexed
            }

            val col = ((wrapper.translationX + cellWidth / 2) / (cellWidth + CELL_MARGIN))
                .toInt()
                .coerceIn(0, GRID_COLUMNS - 1)  // already ensures col ≥ 0

            val row = ((wrapper.translationY + cellHeight / 2) / (cellHeight + CELL_MARGIN))
                .toInt()
                .coerceAtLeast(0)  // ensures row ≥ 0
            val cellsW = ((wrapper.width + CELL_MARGIN) / (cellWidth + CELL_MARGIN)).coerceAtLeast(1)
            val cellsH = ((wrapper.height + CELL_MARGIN) / (cellHeight + CELL_MARGIN)).coerceAtLeast(1)

            val widgetWidth = (cellWidth * cellsW).coerceAtLeast(cellWidth)
            val widgetHeight = (cellHeight * cellsH).coerceAtLeast(cellHeight)

            savedList.add(SavedWidget(id, col, row, widgetWidth, widgetHeight, cellsW, cellsH))
            AppLogger.d(TAG, "💾 SAVE wrapper #$index → id=$id | col=$col,row=$row | size=${widgetWidth}x${widgetHeight} | cells=${cellsW}x${cellsH}")
        }

        val json = savedWidgetAdapter.toJson(savedList.toTypedArray())
        requireContext().openFileOutput(widgetFileName, Context.MODE_PRIVATE).use { it.write(json.toByteArray()) }

        AppLogger.i(TAG, "📊 saveWidgets: saved=${savedList.size}, skipped=$skippedCount, totalWrappers=${widgetWrappers.size}")
        AppLogger.v(TAG, "📝 JSON → $json")
    }

    /** Restore widgets from JSON */
    private fun restoreWidgets() {
        val file = requireContext().getFileStreamPath(widgetFileName)
        if (!file.exists()) {
            AppLogger.w(TAG, "⚠️ No widget file found ($widgetFileName)")
            return
        }

        val json = file.readText()
        AppLogger.d(TAG, "🔄 restoreWidgets: Loaded JSON -> $json")

        val savedWidgets = savedWidgetAdapter.fromJson(json)
        if (savedWidgets.isNullOrEmpty()) {
            AppLogger.w(TAG, "⚠️ No widgets found in JSON")
            return
        }

        AppLogger.i(TAG, "📥 Found ${savedWidgets.size} saved widgets in JSON")

        widgetGrid.post {
            val parentWidth = widgetGrid.width.coerceAtLeast(1)
            val cellWidth = (parentWidth - CELL_MARGIN * (GRID_COLUMNS - 1)) / GRID_COLUMNS
            val cellHeight = cellWidth.coerceAtLeast(1)

            val seenIds = HashSet<Int>()
            var restoredCount = 0
            var skippedCount = 0

            savedWidgets.forEachIndexed { index, saved ->
                updateEmptyPlaceholder(widgetWrappers)
                if (!seenIds.add(saved.appWidgetId)) {
                    AppLogger.w(TAG, "⚠️ SKIP duplicate widget id=${saved.appWidgetId}")
                    skippedCount++
                    return@forEachIndexed
                }

                val info = appWidgetManager.getAppWidgetInfo(saved.appWidgetId)
                if (info == null) {
                    AppLogger.e(TAG, "❌ No AppWidgetInfo for id=${saved.appWidgetId}, removing")
                    safeRemoveWidget(saved.appWidgetId)
                    skippedCount++
                    return@forEachIndexed
                }

                val hostView = try {
                    val widgetContext = try {
                        requireContext().createPackageContext(
                            info.provider.packageName,
                            Context.CONTEXT_IGNORE_SECURITY or Context.CONTEXT_INCLUDE_CODE
                        )
                    } catch (_: Exception) {
                        requireContext()
                    }

                    appWidgetHost.createView(widgetContext, saved.appWidgetId, info)
                } catch (e: Exception) {
                    AppLogger.e(TAG, "⚠️ Failed to restore widgetId=${saved.appWidgetId}, removing it", e)
                    safeRemoveWidget(saved.appWidgetId)
                    skippedCount++
                    return@forEachIndexed
                }

                val wrapper = ResizableWidgetWrapper(
                    requireContext(), hostView, info, appWidgetHost,
                    ::saveWidgets, GRID_COLUMNS, CELL_MARGIN, saved.width / cellWidth, saved.height / cellHeight
                )

                wrapper.translationX = saved.col * (cellWidth + CELL_MARGIN).toFloat()
                wrapper.translationY = saved.row * (cellHeight + CELL_MARGIN).toFloat()
                wrapper.layoutParams = FrameLayout.LayoutParams(
                    if (saved.width > 0) saved.width else DEFAULT_WIDGET_CELLS_W * cellWidth,
                    if (saved.height > 0) saved.height else DEFAULT_WIDGET_CELLS_H * cellHeight
                )

                addWrapperSafely(wrapper)
                restoredCount++
                AppLogger.d(TAG, "🔄 RESTORED #$index → id=$id | col=$saved.col,row=$saved.row | size=${saved.width}x${saved.height} | cells=${saved.cellsW}x${saved.cellsH}")
            }

            AppLogger.i(TAG, "📊 restoreWidgets: Restored=$restoredCount, Skipped=$skippedCount")
            logGridSnapshot()
        }
    }

    private fun logGridSnapshot() {
        val file = requireContext().getFileStreamPath(widgetFileName)
        if (!file.exists()) return
        val json = file.readText()
        val savedWidgets = savedWidgetAdapter.fromJson(json) ?: return

        val maxRow = (savedWidgets.maxOfOrNull { it.row + it.cellsH } ?: 0)
        val grid = Array(maxRow) { Array(GRID_COLUMNS) { "□" } }

        savedWidgets.forEach { w ->
            for (r in w.row until w.row + w.cellsH) {
                for (c in w.col until w.col + w.cellsW) {
                    if (r in grid.indices && c in 0 until GRID_COLUMNS) {
                        grid[r][c] = "■"
                    }
                }
            }
        }

        val snapshot = grid.joinToString("\n") { it.joinToString(" ") }
        AppLogger.i(TAG, "📐 Grid Snapshot:\n$snapshot")
    }

    private fun updateEmptyPlaceholder(wrappers: List<ResizableWidgetWrapper>) {
        val shouldBeVisible = wrappers.isEmpty()

        emptyPlaceholder.isVisible = placeholderVisible
        widgetGrid.isVisible = !placeholderVisible
        if (placeholderVisible == shouldBeVisible) return // no change needed

        placeholderVisible = shouldBeVisible

        AppLogger.i(TAG, if (shouldBeVisible) "🟨 Showing placeholder" else "🟩 Hiding placeholder")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        AppLogger.i(TAG, "🔗 WidgetFragment onAttach called, context=$context")
        if (!isViewCreated()) {
            appWidgetHost = AppWidgetHost(context, APP_WIDGET_HOST_ID)
            appWidgetHost.startListening()
            AppLogger.i(TAG, "🟢 Initialized AppWidgetHost")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        appWidgetHost.stopListening()
        AppLogger.i(TAG, "🛑 AppWidgetHost stopped listening")
    }

    fun isViewCreated(): Boolean = ::widgetGrid.isInitialized
}