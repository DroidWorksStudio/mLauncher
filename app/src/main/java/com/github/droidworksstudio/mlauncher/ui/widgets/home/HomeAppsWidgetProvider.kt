package com.github.droidworksstudio.mlauncher.ui.widgets.home

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.widget.RemoteViews
import com.github.droidworksstudio.mlauncher.R
import com.github.droidworksstudio.mlauncher.data.Prefs

class HomeAppsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val prefs = Prefs(context)
        val numApps = prefs.homeAppsNum

        for (appWidgetId in appWidgetIds) {
            val rv = RemoteViews(context.packageName, R.layout.widget_home_apps)

            // Clear previous views
            rv.removeAllViews(R.id.homeAppsLayout)

            // Loop through apps dynamically
            for (i in 0 until numApps) {
                val appModel = prefs.getHomeAppModel(i)
                val packageName = appModel.activityPackage
                if (packageName.isEmpty()) continue

                val itemRv = RemoteViews(context.packageName, R.layout.item_home_app)

                // Get app label safely
                val appLabel = try {
                    context.packageManager.getApplicationLabel(
                        context.packageManager.getApplicationInfo(packageName, 0)
                    ).toString()
                } catch (_: Exception) {
                    packageName
                }
                itemRv.setTextViewText(R.id.appName, appLabel)

                // Optional: text size/color from prefs
                itemRv.setTextViewTextSize(R.id.appName, TypedValue.COMPLEX_UNIT_SP, prefs.appSize.toFloat())
                itemRv.setTextColor(R.id.appName, prefs.appColor)

                val bottomPadding = prefs.textPaddingSize // pixels, or convert dp to px
                itemRv.setViewPadding(R.id.appName, 0, 0, 0, bottomPadding)

                // Click intent
                val clickIntent = Intent(context, HomeAppUpdateReceiver::class.java).apply {
                    action = "HOME_APP_CLICK"
                    putExtra("PACKAGE_NAME", packageName)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    i,
                    clickIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                itemRv.setOnClickPendingIntent(R.id.appName, pendingIntent)

                // Add item to the container
                rv.addView(R.id.homeAppsLayout, itemRv)
            }

            appWidgetManager.updateAppWidget(appWidgetId, rv)
        }
    }

}

