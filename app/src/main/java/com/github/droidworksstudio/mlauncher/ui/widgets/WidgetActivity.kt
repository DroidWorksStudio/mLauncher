package com.github.droidworksstudio.mlauncher.ui.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.github.droidworksstudio.common.AppLogger
import com.github.droidworksstudio.mlauncher.R

class WidgetActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "WidgetActivity"
    }

    private lateinit var widgetPermissionLauncher: ActivityResultLauncher<Intent>
    private var widgetResultCallback: ((Int, Int, Intent?) -> Unit)? = null
    private val pendingWidgets = mutableListOf<Pair<AppWidgetProviderInfo, Int>>()

    // ───────────────────────────────────────────────
    // Lifecycle
    // ───────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        AppLogger.d(TAG, "🟢 onCreate() called — savedInstanceState=$savedInstanceState")
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_widget)

        // Setup result launcher
        AppLogger.v(TAG, "Initializing ActivityResult launcher for widget permissions")
        widgetPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                AppLogger.v(TAG, "🎬 Received ActivityResult: resultCode=${result.resultCode}, data=${result.data}")
                widgetResultCallback?.invoke(
                    result.resultCode,
                    result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1,
                    result.data
                )
            }

        // Attach WidgetFragment
        if (savedInstanceState == null) {
            AppLogger.i(TAG, "Attaching new WidgetFragment instance")
            supportFragmentManager.beginTransaction()
                .replace(R.id.widget_fragment_container, WidgetFragment())
                .commitNow()
        } else {
            AppLogger.d(TAG, "WidgetFragment already attached (restored from state)")
        }
    }

    override fun onStart() {
        super.onStart()
        AppLogger.v(TAG, "🟢 onStart()")
    }

    override fun onResume() {
        super.onResume()
        AppLogger.v(TAG, "🟢 onResume() — flushing any pending widgets (${pendingWidgets.size})")

        val fragment = supportFragmentManager.findFragmentById(R.id.widget_fragment_container) as? WidgetFragment
        val container = fragment?.view as? ViewGroup

        container?.post {
            flushPendingWidgets()
        }
    }


    override fun onPause() {
        super.onPause()
        AppLogger.v(TAG, "🟠 onPause()")
    }

    override fun onStop() {
        super.onStop()
        AppLogger.v(TAG, "🔴 onStop()")
    }

    override fun onDestroy() {
        super.onDestroy()
        AppLogger.v(TAG, "⚫ onDestroy() — clearing callbacks and pending list")
        widgetResultCallback = null
        pendingWidgets.clear()
    }

    // ───────────────────────────────────────────────
    // Widget management
    // ───────────────────────────────────────────────
    fun launchWidgetPermission(intent: Intent, callback: (Int, Int, Intent?) -> Unit) {
        AppLogger.d(TAG, "Launching widget permission intent: $intent")
        widgetResultCallback = callback
        widgetPermissionLauncher.launch(intent)
    }

    fun safeCreateWidget(widgetInfo: AppWidgetProviderInfo, appWidgetId: Int) {
        val widgetLabel = widgetInfo.loadLabel(packageManager).toString()
        AppLogger.d(TAG, "Attempting to create widget: $widgetLabel (id=$appWidgetId)")

        val fragment = supportFragmentManager.findFragmentById(R.id.widget_fragment_container) as? WidgetFragment

        if (fragment != null && fragment.isAdded) {
            AppLogger.i(TAG, "✅ WidgetFragment is attached, creating widget wrapper immediately")
            fragment.createWidgetWrapperSafe(widgetInfo, appWidgetId)
        } else {
            pendingWidgets.add(widgetInfo to appWidgetId)
            AppLogger.w(
                TAG,
                "⚠️ WidgetFragment not attached, queued widget (id=$appWidgetId, label=$widgetLabel)"
            )
        }
    }

    fun flushPendingWidgets() {
        val fragment = supportFragmentManager
            .findFragmentById(R.id.widget_fragment_container) as? WidgetFragment ?: return

        if (!fragment.isAdded || !fragment.isViewCreated()) {
            AppLogger.w(TAG, "Fragment not ready, deferring flush")
            return
        }

        fragment.postPendingWidgets(pendingWidgets.toList())
        pendingWidgets.clear()
    }
}
