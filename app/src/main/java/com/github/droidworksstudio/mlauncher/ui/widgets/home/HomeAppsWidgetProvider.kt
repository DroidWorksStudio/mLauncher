package com.github.droidworksstudio.mlauncher.ui.widgets.home

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.core.graphics.createBitmap
import com.github.droidworksstudio.common.ColorIconsExtensions
import com.github.droidworksstudio.mlauncher.R
import com.github.droidworksstudio.mlauncher.data.Constants
import com.github.droidworksstudio.mlauncher.data.Prefs
import com.github.droidworksstudio.mlauncher.helper.IconCacheTarget
import com.github.droidworksstudio.mlauncher.helper.IconPackHelper.getSafeAppIcon
import com.github.droidworksstudio.mlauncher.helper.getSystemIcons

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
            rv.removeAllViews(R.id.homeAppsLayout)

            for (i in 0 until numApps) {
                val appModel = prefs.getHomeAppModel(i)
                val packageName = appModel.activityPackage
                if (packageName.isEmpty()) continue

                val itemRv = RemoteViews(context.packageName, R.layout.item_home_app)

                // --- App label ---
                val appLabel = try {
                    context.packageManager.getApplicationLabel(
                        context.packageManager.getApplicationInfo(appModel.label, 0)
                    ).toString()
                } catch (_: Exception) {
                    appModel.label
                }

                // --- Get icon safely ---
                val iconPackPackage = prefs.customIconPackHome
                val drawable = getSafeAppIcon(
                    context = context,
                    packageName = appModel.activityPackage,
                    useIconPack = (iconPackPackage.isNotEmpty() && prefs.iconPackHome == Constants.IconPacks.Custom),
                    iconPackTarget = IconCacheTarget.HOME
                )

                val recoloredDrawable = getSystemIcons(context, prefs, IconCacheTarget.HOME, drawable) ?: drawable

                // --- Set TextView text ---
                itemRv.setTextViewText(R.id.appLabel, appLabel)

                // Optional: text size/color from prefs
                itemRv.setTextViewTextSize(R.id.appLabel, TypedValue.COMPLEX_UNIT_SP, prefs.appSize.toFloat())
                itemRv.setTextColor(R.id.appLabel, prefs.appColor)

                // --- Set gravity based on prefs.homeAlignment ---
                itemRv.setInt(R.id.appLabel, "setGravity", prefs.homeAlignment.value())

                // --- Set icon in ImageView ---
                // Calculate icon size and padding
                var iconSize = (prefs.appSize * 1.4f).toInt()
                if (prefs.iconPackHome == Constants.IconPacks.System || prefs.iconPackHome == Constants.IconPacks.Custom) {
                    iconSize *= 2
                }
                val iconPadding = (iconSize / 1.2f).toInt() // padding next to icon

                // Convert drawable to bitmap
                val bitmap = drawableToBitmap(recoloredDrawable, iconSize, iconSize)

                // Apply alignment
                when (prefs.homeAlignment) {
                    Constants.Gravity.Left -> {
                        // Left icon visible, right icon gone
                        itemRv.setImageViewBitmap(R.id.appIconLeft, bitmap)
                        itemRv.setViewVisibility(R.id.appIconLeft, android.view.View.VISIBLE)
                        itemRv.setViewVisibility(R.id.appIconRight, android.view.View.GONE)

                        // Padding on right of icon
                        itemRv.setViewPadding(R.id.appIconLeft, 0, 0, iconPadding, 0)
                    }

                    Constants.Gravity.Right -> {
                        // Right icon visible, left icon gone
                        itemRv.setImageViewBitmap(R.id.appIconRight, bitmap)
                        itemRv.setViewVisibility(R.id.appIconRight, android.view.View.VISIBLE)
                        itemRv.setViewVisibility(R.id.appIconLeft, android.view.View.GONE)

                        // Padding on left of icon
                        itemRv.setViewPadding(R.id.appIconRight, iconPadding, 0, 0, 0)
                    }

                    else -> {
                        itemRv.setViewVisibility(R.id.appIconLeft, android.view.View.GONE)
                        itemRv.setViewVisibility(R.id.appIconRight, android.view.View.GONE)
                    }
                }

                // Optional: set color filter
                if (prefs.iconRainbowColors) {
                    val dominantColor = ColorIconsExtensions.getDominantColor(bitmap)
                    itemRv.setInt(R.id.appIconLeft, "setColorFilter", dominantColor)
                } else {
                    itemRv.setInt(R.id.appIconLeft, "setColorFilter", prefs.shortcutIconsColor)
                }

                val textPaddingSize = prefs.textPaddingSize // pixels, or convert dp to px
                itemRv.setViewPadding(R.id.appLabel, 0, textPaddingSize, 0, textPaddingSize)


                // --- Click intent ---
                val clickIntent = Intent(context, HomeAppUpdateReceiver::class.java).apply {
                    action = "HOME_APP_CLICK"
                    putExtra("PACKAGE_NAME", packageName)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context, i, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                itemRv.setOnClickPendingIntent(R.id.appIconLeft, pendingIntent)
                itemRv.setOnClickPendingIntent(R.id.appLabel, pendingIntent) // Optional: click text too

                rv.addView(R.id.homeAppsLayout, itemRv)
            }

            appWidgetManager.updateAppWidget(appWidgetId, rv)
        }
    }

    fun drawableToBitmap(drawable: Drawable, width: Int, height: Int): Bitmap {
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
        return bitmap
    }

}

