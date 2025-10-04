package com.github.droidworksstudio.mlauncher.ui

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
import com.github.droidworksstudio.mlauncher.MainActivity
import com.github.droidworksstudio.mlauncher.R
import com.github.droidworksstudio.mlauncher.ui.components.LockedBottomSheetDialog
import com.github.droidworksstudio.mlauncher.ui.widgets.ResizableWidgetWrapper
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
        const val GRID_COLUMNS = 7
        const val CELL_MARGIN = 16
        const val DEFAULT_WIDGET_CELLS_W = 1
        const val DEFAULT_WIDGET_CELLS_H = 1
    }

    private var activeGridDialog: LockedBottomSheetDialog? = null
    private var lastWidgetInfo: AppWidgetProviderInfo? = null
    private val widgetFileName = "widgets.json"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_widget_container, container, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        widgetGrid = view.findViewById(R.id.widget_grid)
        AppLogger.i(TAG, "🟢 Widget grid initialized")
        // Create placeholder programmatically
        AppLogger.i(TAG, "🟢 Creating empty placeholder for widgets")
        emptyPlaceholder = TextView(requireContext()).apply {
            text = context.getString(R.string.widgets_not_added)
            textSize = 16f
            setTextColor(resources.getColor(android.R.color.darker_gray, null))
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        AppLogger.i(TAG, "🟢 Empty placeholder TextView created")

        widgetGrid.addView(
            emptyPlaceholder, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        )
        AppLogger.i(TAG, "🟢 Empty placeholder added to widgetGrid")

        appWidgetManager = requireContext().appWidgetManager
        appWidgetHost = AppWidgetHost(requireContext(), APP_WIDGET_HOST_ID)
        appWidgetHost.startListening()
        AppLogger.i(TAG, "🟢 AppWidgetHost started listening")

        widgetGrid.setOnLongClickListener {
            showGridMenu()
            true
        }

        restoreWidgets()

        AppLogger.i(TAG, "🟢 WidgetFragment onViewCreated, view is ready")
        (activity as? MainActivity)?.flushPendingWidgets()
    }

    override fun onResume() {
        super.onResume()
        restoreWidgets()
        AppLogger.i(TAG, "🔄 WidgetFragment onResume, widgets restored")
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
            AppLogger.d(TAG, "❌ Deleting widgetId=${wrapper.hostView.appWidgetId}")
            appWidgetHost.deleteAppWidgetId(wrapper.hostView.appWidgetId)
        }
        widgetWrappers.clear()
        widgetGrid.removeAllViews()
        widgetGrid.addView(emptyPlaceholder)
        saveWidgets()
        updateEmptyPlaceholder(widgetWrappers)
        AppLogger.i(TAG, "🧹 All widgets cleared and placeholder shown")
    }

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

    private fun addWidget(widgetInfo: AppWidgetProviderInfo) {
        lastWidgetInfo = widgetInfo
        val widgetId = appWidgetHost.allocateAppWidgetId()
        AppLogger.d(TAG, "🆕 Allocated appWidgetId=$widgetId for provider=${widgetInfo.provider.packageName}")

        val appWidgetManager = requireContext().appWidgetManager
        val bound = if (!appWidgetManager.bindAppWidgetIdIfAllowed(widgetId, widgetInfo.provider)) {
            AppLogger.w(TAG, "🔒 Binding not allowed for widgetId=$widgetId, requesting permission")

            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, widgetInfo.provider)
            }

            (requireActivity() as MainActivity).launchWidgetPermission(intent) { resultCode, returnedId, data ->
                AppLogger.v(TAG, "🎬 Returned from widget bind permission launcher: resultCode=$resultCode, appWidgetId=$returnedId")
                handleWidgetResult(resultCode, returnedId, data)
            }
            false
        } else {
            AppLogger.i(TAG, "✅ Bound widget immediately: widgetId=$widgetId, provider=${widgetInfo.provider}")
            true
        }

        if (!bound) return

        val parentFrame = requireActivity().findViewById<ViewGroup>(R.id.widget_grid)
        val gridWidth = parentFrame.width
        val cellSize = (gridWidth - (CELL_MARGIN * (GRID_COLUMNS - 1))) / GRID_COLUMNS

        val spanX = ((widgetInfo.minWidth + CELL_MARGIN) / (cellSize + CELL_MARGIN)).coerceAtLeast(1)
        val spanY = ((widgetInfo.minHeight + CELL_MARGIN) / (cellSize + CELL_MARGIN)).coerceAtLeast(1)

        AppLogger.d(TAG, "📐 Widget ${widgetInfo.provider} requires ${spanX}x$spanY cells")

        val occupied = getOccupiedGrid()
        val maxRows = occupied.size.coerceAtLeast(1)

        var foundRow = -1
        var foundCol = -1

        outer@ for (row in 0 until maxRows + 10) {
            for (col in 0 until GRID_COLUMNS) {
                var fits = true
                for (dy in 0 until spanY) {
                    for (dx in 0 until spanX) {
                        val r = row + dy
                        val c = col + dx
                        if (c >= GRID_COLUMNS || (r < occupied.size && occupied[r][c])) {
                            fits = false; break
                        }
                    }
                    if (!fits) break
                }
                if (fits) {
                    foundRow = row
                    foundCol = col
                    break@outer
                }
            }
        }

        if (foundRow == -1) {
            AppLogger.e(TAG, "❌ No free grid slot for ${widgetInfo.provider}")
            return
        }

        AppLogger.i(TAG, "📦 Placing widget ${widgetInfo.provider} at row=$foundRow col=$foundCol span=${spanX}x$spanY")
        saveWidgetPlacement(widgetId, foundRow, foundCol, spanX, spanY, parentFrame)
        maybeConfigureOrCreate(widgetInfo, widgetId)
    }

    private fun saveWidgetPlacement(
        widgetId: Int,
        row: Int,
        col: Int,
        cellsW: Int,
        cellsH: Int,
        view: View // the actual ResizableWidgetWrapper or hostView
    ) {
        val file = requireContext().getFileStreamPath(widgetFileName)

        // Load existing widgets
        val existingArray: Array<SavedWidget>? = if (file.exists()) {
            savedWidgetAdapter.fromJson(file.readText())
        } else null

        // Convert to mutable list for updates
        val existing = existingArray?.toMutableList() ?: mutableListOf()

        // Remove any old entry for this widgetId
        existing.removeAll { it.appWidgetId == widgetId }

        // Work out cellSize based on parent width
        val parentFrame = view.parent as? FrameLayout
        val cellSize = if (parentFrame != null) {
            (parentFrame.width - (CELL_MARGIN * (GRID_COLUMNS - 1))) / GRID_COLUMNS
        } else 0

        // Grab actual pixel size from LayoutParams, fallback to cell-based size
        val lp = view.layoutParams as? FrameLayout.LayoutParams
        val pxWidth = lp?.width ?: (cellsW * cellSize).coerceAtLeast(1)
        val pxHeight = lp?.height ?: (cellsH * cellSize).coerceAtLeast(1)

        // Add new/updated placement
        existing.add(
            SavedWidget(
                appWidgetId = widgetId,
                row = row,
                col = col,
                cellsW = cellsW,
                cellsH = cellsH,
                width = pxWidth,
                height = pxHeight
            )
        )

        // Convert back to array before saving
        file.writeText(savedWidgetAdapter.toJson(existing.toTypedArray()))

        AppLogger.i(
            TAG,
            "💾 Saved widgetId=$widgetId at row=$row col=$col size=${cellsW}x${cellsH} px=${pxWidth}x${pxHeight}"
        )
    }


    private fun getOccupiedGrid(): Array<Array<Boolean>> {
        val file = requireContext().getFileStreamPath(widgetFileName)
        if (!file.exists()) return emptyArray()

        val json = file.readText()
        val savedWidgets = savedWidgetAdapter.fromJson(json) ?: return emptyArray()

        val maxRow = (savedWidgets.maxOfOrNull { it.row + it.cellsH } ?: 0)
        val grid = Array(maxRow.coerceAtLeast(1)) { Array(GRID_COLUMNS) { false } }

        savedWidgets.forEach { w ->
            for (r in w.row until w.row + w.cellsH) {
                for (c in w.col until w.col + w.cellsW) {
                    if (r in grid.indices && c in 0 until GRID_COLUMNS) {
                        grid[r][c] = true
                    }
                }
            }
        }

        return grid
    }

    private fun handleWidgetResult(resultCode: Int, appWidgetId: Int, data: Intent?) {
        AppLogger.v(TAG, "📦 handleWidgetResult: resultCode=$resultCode, appWidgetId=$appWidgetId, extras=${data?.extras?.keySet()}")

        when (resultCode) {
            Activity.RESULT_OK -> {
                AppLogger.i(TAG, "✅ RESULT_OK for appWidgetId=$appWidgetId")
                lastWidgetInfo?.let {
                    AppLogger.d(TAG, "🔧 Passing widgetInfo=${it.provider.packageName} to maybeConfigureOrCreate")
                    maybeConfigureOrCreate(it, appWidgetId)
                }
                lastWidgetInfo = null
            }

            Activity.RESULT_CANCELED -> {
                AppLogger.w(TAG, "❌ RESULT_CANCELED for appWidgetId=$appWidgetId, cleaning up")
                try {
                    appWidgetHost.deleteAppWidgetId(appWidgetId)
                    AppLogger.d(TAG, "🗑️ Deleted appWidgetId=$appWidgetId after cancel")
                } catch (e: Exception) {
                    AppLogger.e(TAG, "❌ Failed to delete appWidgetId=$appWidgetId", e)
                }
            }
        }
    }

    private fun maybeConfigureOrCreate(widgetInfo: AppWidgetProviderInfo, widgetId: Int) {
        AppLogger.d(TAG, "🔍 maybeConfigureOrCreate called for widgetId=$widgetId, provider=${widgetInfo.provider.packageName}")
        if (widgetInfo.configure != null) {
            AppLogger.i(TAG, "⚙️ Found configuration activity: ${widgetInfo.configure.className} for widgetId=$widgetId")
            val intent = Intent().apply {
                component = widgetInfo.configure
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }

            activity?.let { act ->
                (act as MainActivity).launchWidgetPermission(intent) { resultCode, returnedId, _ ->
                    AppLogger.v(TAG, "🎬 Returned from widget config launcher: resultCode=$resultCode, appWidgetId=$returnedId")
                    if (resultCode == Activity.RESULT_OK) {
                        AppLogger.i(TAG, "✅ Configured widgetId=$returnedId, creating wrapper")
                        act.safeCreateWidget(widgetInfo, returnedId)
                    } else {
                        AppLogger.w(TAG, "❌ Config cancelled for widgetId=$returnedId, deleting")
                        appWidgetHost.deleteAppWidgetId(returnedId)
                    }
                }
            } ?: run {
                AppLogger.w(TAG, "⚠️ Fragment not attached to activity yet, cannot launch widget")
                // Optionally queue this widget request to try later (e.g., in onResume or onAttach)
            }

        } else {
            AppLogger.i(TAG, "📦 No config required for widgetId=$widgetId, creating wrapper immediately")
            createWidgetWrapperSafe(widgetInfo, widgetId)
        }
    }

    fun createWidgetWrapperSafe(widgetInfo: AppWidgetProviderInfo, appWidgetId: Int) {
        if (!isAdded) {
            AppLogger.w(TAG, "⚠️ Skipping widget creation, fragment not attached")
            return
        }
        createWidgetWrapper(widgetInfo, appWidgetId)
    }

    fun createWidgetWrapper(widgetInfo: AppWidgetProviderInfo, appWidgetId: Int) {
        val hostView = try {
            // Try using the widget's own package context
            val widgetContext = requireContext().createPackageContext(
                widgetInfo.provider.packageName,
                Context.CONTEXT_IGNORE_SECURITY or Context.CONTEXT_INCLUDE_CODE
            )
            appWidgetHost.createView(widgetContext, appWidgetId, widgetInfo)
        } catch (e: Exception) {
            AppLogger.e(TAG, "⚠️ Failed to create widget with package context, falling back to launcher context", e)
            // Fallback to the launcher's context
            appWidgetHost.createView(requireContext(), appWidgetId, widgetInfo)
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
    }

    private fun addWrapperToGrid(wrapper: ResizableWidgetWrapper) {
        AppLogger.d(TAG, "➕ Adding wrapper to grid for widgetId=${wrapper.hostView.appWidgetId}")

        val parentWidth = widgetGrid.width
        val cellWidth = (parentWidth - CELL_MARGIN * (GRID_COLUMNS - 1)) / GRID_COLUMNS
        val cellHeight = cellWidth

        wrapper.layoutParams = FrameLayout.LayoutParams(
            DEFAULT_WIDGET_CELLS_W * cellWidth,
            DEFAULT_WIDGET_CELLS_H * cellHeight
        )

        val occupied = widgetWrappers.map { w ->
            val wCol = ((w.translationX + cellWidth / 2) / (cellWidth + CELL_MARGIN)).toInt()
            val wRow = ((w.translationY + cellHeight / 2) / (cellHeight + CELL_MARGIN)).toInt()
            Pair(wCol, wRow)
        }

        AppLogger.v(TAG, "📊 Occupied cells: $occupied")

        var row = 0
        var col = 0
        loop@ for (r in 0..1000) {
            for (c in 0 until GRID_COLUMNS) {
                if (occupied.none { it.first == c && it.second == r }) {
                    col = c
                    row = r
                    AppLogger.d(TAG, "📍 Found empty cell at row=$row col=$col for widgetId=${wrapper.hostView.appWidgetId}")
                    break@loop
                }
            }
        }

        wrapper.translationX = col * (cellWidth + CELL_MARGIN).toFloat()
        wrapper.translationY = row * (cellHeight + CELL_MARGIN).toFloat()

        AppLogger.i(TAG, "✅ Placed widgetId=${wrapper.hostView.appWidgetId} at row=$row col=$col")

        addWrapperSafely(wrapper)
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
        val parentWidth = widgetGrid.width
        val cellWidth = (parentWidth - CELL_MARGIN * (GRID_COLUMNS - 1)) / GRID_COLUMNS
        val cellHeight = cellWidth

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

            savedList.add(SavedWidget(id, col, row, wrapper.width, wrapper.height, cellsW, cellsH))
            AppLogger.d(TAG, "💾 SAVE wrapper #$index → id=$id | col=$col,row=$row | size=${wrapper.width}x${wrapper.height} | cells=${cellsW}x${cellsH}")
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
        if (savedWidgets == null || savedWidgets.isEmpty()) {
            AppLogger.w(TAG, "⚠️ No widgets found in JSON")
            return
        }

        AppLogger.i(TAG, "📥 Found ${savedWidgets.size} saved widgets in JSON")

        widgetGrid.post {
            val totalWidth = widgetGrid.width
            val cellWidth = (totalWidth - (GRID_COLUMNS - 1) * CELL_MARGIN) / GRID_COLUMNS
            val cellHeight = cellWidth

            val seenIds = HashSet<Int>()
            var restoredCount = 0
            var skippedCount = 0

            savedWidgets.forEachIndexed { index, saved ->
                if (!seenIds.add(saved.appWidgetId)) {
                    AppLogger.w(TAG, "⚠️ SKIP duplicate widget id=${saved.appWidgetId}")
                    skippedCount++
                    return@forEachIndexed
                }

                try {
                    val info = appWidgetManager.getAppWidgetInfo(saved.appWidgetId)
                    if (info == null) {
                        AppLogger.w(TAG, "❌ No AppWidgetInfo for id=${saved.appWidgetId}, skipping")
                        skippedCount++
                        return@forEachIndexed
                    }

                    val hostView = try {
                        // Try using the widget's package context
                        val widgetContext = requireContext().createPackageContext(
                            info.provider.packageName,
                            Context.CONTEXT_IGNORE_SECURITY or Context.CONTEXT_INCLUDE_CODE
                        )
                        appWidgetHost.createView(widgetContext, saved.appWidgetId, info)
                    } catch (e: Exception) {
                        AppLogger.e(TAG, "⚠️ Failed to create widget with package context, falling back to launcher context", e)
                        // Fallback to your launcher's context
                        appWidgetHost.createView(requireContext(), saved.appWidgetId, info)
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

                    AppLogger.i(
                        TAG,
                        "🔄 RESTORED → id=${saved.appWidgetId} | Pinned -> col=${saved.col}, row=${saved.row} | Size -> width=${saved.width}, height=${saved.height} | Cells -> width=${saved.cellsW}, height=${saved.cellsH} ✅"
                    )
                    logGridSnapshot()
                } catch (e: Exception) {
                    skippedCount++
                    AppLogger.e(TAG, "❌ Failed to restore widget id=${saved.appWidgetId}", e)
                }
            }

            AppLogger.i(TAG, "📊 restoreWidgets: Restored=$restoredCount, Skipped=$skippedCount")
        }
    }

    private fun logGridSnapshot() {
        val file = requireContext().getFileStreamPath(widgetFileName)
        if (!file.exists()) {
            AppLogger.w(TAG, "⚠️ No widget file found ($widgetFileName)")
            return
        }

        val json = file.readText()
        val savedWidgets = savedWidgetAdapter.fromJson(json)
        if (savedWidgets.isNullOrEmpty()) {
            AppLogger.w(TAG, "⚠️ No widgets found in JSON")
            return
        }

        // Determine grid size
        val maxRow = (savedWidgets.maxOfOrNull { it.row + it.cellsH } ?: 0)
        val grid = Array(maxRow) { Array(GRID_COLUMNS) { "□" } } // empty cells

        savedWidgets.forEachIndexed { index, w ->
            for (r in w.row until w.row + w.cellsH) {
                for (c in w.col until w.col + w.cellsW) {
                    if (r in grid.indices && c in 0 until GRID_COLUMNS) {
                        grid[r][c] = "■" // label with widget index
                    }
                }
            }
        }

        val snapshot = grid.joinToString("\n") { it.joinToString(" ") }
        AppLogger.i(TAG, "📐 Grid Snapshot:\n$snapshot")
    }

    private fun updateEmptyPlaceholder(wrappers: List<ResizableWidgetWrapper>) {
        val wasVisible = emptyPlaceholder.isVisible
        val shouldBeVisible = wrappers.isEmpty()

        emptyPlaceholder.isVisible = shouldBeVisible

        if (shouldBeVisible && !wasVisible) {
            AppLogger.i(TAG, "🟨 No widgets present, showing placeholder")
        } else if (!shouldBeVisible && wasVisible) {
            AppLogger.i(TAG, "🟩 Widgets present, hiding placeholder")
        } else {
            AppLogger.v(TAG, "🔄 Placeholder visibility unchanged: ${emptyPlaceholder.isVisible}")
        }
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        AppLogger.i(TAG, "🔗 WidgetFragment onAttach called, context=$context")

        if (!isViewCreated()) {
            AppLogger.i(TAG, "🟢 Initializing appWidgetHost for the first time")
            appWidgetHost = AppWidgetHost(context, APP_WIDGET_HOST_ID)
            appWidgetHost.startListening()
        } else {
            AppLogger.i(TAG, "⚠️ appWidgetHost already initialized")
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        appWidgetHost.stopListening()
    }

    fun isViewCreated(): Boolean = ::widgetGrid.isInitialized
}
