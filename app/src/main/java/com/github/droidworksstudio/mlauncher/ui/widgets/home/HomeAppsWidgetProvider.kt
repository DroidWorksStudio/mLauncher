package com.github.droidworksstudio.mlauncher.ui.widgets.home

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
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

                // Optional recolor
                val recoloredDrawable = getSystemIcons(context, prefs, IconCacheTarget.HOME, drawable) ?: drawable

                // --- Create bitmap with text + icon ---
                val bitmap = createWidgetTextWithIcon(
                    context = context,
                    text = appLabel,
                    drawable = recoloredDrawable,
                    textSizeSp = prefs.appSize.toFloat(),
                    textColor = prefs.appColor,
                    iconOnLeft = prefs.homeAlignment == Constants.Gravity.Left,
                    paddingPx = (prefs.textPaddingSize * 1.5f).toInt() // padding between icon and text
                )

                // Get dominant color from bitmap
                val dominantColor = ColorIconsExtensions.getDominantColor(bitmap)

                // Recolor the drawable
                ColorIconsExtensions.recolorDrawable(recoloredDrawable, dominantColor)

                // Set bitmap into ImageView (replaces TextView)
                itemRv.setImageViewBitmap(R.id.appIcon, bitmap)

                val textPaddingSize = prefs.textPaddingSize // pixels, or convert dp to px
                itemRv.setViewPadding(R.id.appIcon, 0, textPaddingSize, 0, textPaddingSize)

                // --- Click intent ---
                val clickIntent = Intent(context, HomeAppUpdateReceiver::class.java).apply {
                    action = "HOME_APP_CLICK"
                    putExtra("PACKAGE_NAME", packageName)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context, i, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                itemRv.setOnClickPendingIntent(R.id.appIcon, pendingIntent)

                // Add item to container
                rv.addView(R.id.homeAppsLayout, itemRv)
            }

            appWidgetManager.updateAppWidget(appWidgetId, rv)
        }
    }

    fun createWidgetTextWithIcon(
        context: Context,
        text: String,
        drawable: Drawable,
        textSizeSp: Float,
        textColor: Int,
        iconOnLeft: Boolean,
        paddingPx: Int
    ): Bitmap {
        val prefs = Prefs(context)
        // Determine icon size relative to text
        var iconSize = (textSizeSp * 1.4f).toInt()
        if (prefs.iconPackHome == Constants.IconPacks.System || prefs.iconPackHome == Constants.IconPacks.Custom) iconSize = iconSize * 2

        // Measure text
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, textSizeSp, context.resources.displayMetrics
            )
        }
        val textWidth = paint.measureText(text)
        val textHeight = paint.descent() - paint.ascent()
        val bitmapWidth = iconSize + paddingPx + textWidth.toInt()
        val bitmapHeight = iconSize.coerceAtLeast(textHeight.toInt())

        val bitmap = createBitmap(bitmapWidth, bitmapHeight)
        val canvas = Canvas(bitmap)

        val iconTop = (bitmapHeight - iconSize) / 2
        val textY = (bitmapHeight / 2f) - (paint.descent() + paint.ascent()) / 2

        if (iconOnLeft) {
            drawable.setBounds(0, iconTop, iconSize, iconTop + iconSize)
            drawable.draw(canvas)
            canvas.drawText(text, iconSize + paddingPx.toFloat(), textY, paint)
        } else {
            canvas.drawText(text, 0f, textY, paint)
            drawable.setBounds((textWidth + paddingPx).toInt(), iconTop, bitmapWidth, iconTop + iconSize)
            drawable.draw(canvas)
        }

        return bitmap
    }

}

