package com.github.droidworksstudio.mlauncher.helper

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Xml
import androidx.core.content.res.ResourcesCompat
import com.github.droidworksstudio.common.AppLogger
import org.xmlpull.v1.XmlPullParser
import java.io.FileNotFoundException

enum class IconCacheTarget {
    APP_LIST,
    HOME
}

object IconPackHelper {
    private val appListIconCache = mutableMapOf<String, Drawable>()
    private val homeIconCache = mutableMapOf<String, Drawable>()
    private val nameCache = mutableMapOf<String, String>()
    private var isInitialized = false

    @SuppressLint("DiscouragedApi")
    fun preloadIcons(context: Context, iconPackPackage: String, target: IconCacheTarget) {
        try {
            when (target) {
                IconCacheTarget.APP_LIST -> appListIconCache.clear()
                IconCacheTarget.HOME -> homeIconCache.clear()
            }

            AppLogger.d("IconPackLoader", "Starting preload for: $iconPackPackage, target=$target")

            val pm = context.packageManager
            val installedPackages = pm.getInstalledApplications(0).map { it.packageName }.toSet()

            val iconPackContext = context.createPackageContext(iconPackPackage, 0)
            val assetManager = iconPackContext.assets

            val possibleAssetNames = listOf("appfilter.xml", "appmap.xml", "drawable.xml")

            val inputStream = possibleAssetNames
                .firstNotNullOfOrNull { fileName ->
                    try {
                        AppLogger.d("IconPackLoader", "Trying to open $fileName")
                        assetManager.open(fileName)
                    } catch (_: Exception) {
                        null
                    }
                } ?: throw FileNotFoundException("No valid XML asset found in $iconPackPackage")

            val parser = Xml.newPullParser()
            parser.setInput(inputStream, null)

            var eventType = parser.eventType
            var loadedCount = 0
            var skippedCount = 0

            val regex = Regex("""ComponentInfo\{([^/]+)(/[^}]*)?\}""")

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                    val component = parser.getAttributeValue(null, "component")
                    var drawable = parser.getAttributeValue(null, "drawable")

//                    AppLogger.d("IconPackLoader", "Parsing item: component=$component, drawable=$drawable")

                    val match = regex.find(component ?: "")
                    val pkgName = match?.groupValues?.get(1)

                    if (pkgName != null && drawable != null) {
                        if (!installedPackages.contains(pkgName)) {
//                            AppLogger.w("IconPackLoader", "Skipping $pkgName (not installed)")
                            skippedCount++
                            eventType = parser.next()
                            continue
                        }

                        drawable = drawable.removePrefix("@drawable/").removePrefix("drawable/")

//                        AppLogger.d("IconPackLoader", "Mapping found: $pkgName -> $drawable")
                        nameCache[pkgName] = drawable

                        val resId = iconPackContext.resources.getIdentifier(
                            drawable,
                            "drawable",
                            iconPackPackage
                        )
                        if (resId != 0) {
                            ResourcesCompat.getDrawable(
                                iconPackContext.resources,
                                resId,
                                iconPackContext.theme
                            )?.let {
                                when (target) {
                                    IconCacheTarget.APP_LIST -> appListIconCache[pkgName] = it
                                    IconCacheTarget.HOME -> homeIconCache[pkgName] = it
                                }
                                loadedCount++
//                                AppLogger.d("IconPackLoader", "Icon cached: $pkgName (resId=$resId)")
                            } ?: AppLogger.w("IconPackLoader", "Drawable is null for resource: $drawable (resId=$resId)")
                        } else {
                            skippedCount++
//                            AppLogger.w("IconPackLoader", "Drawable resource not found: $drawable")
                        }
                    } else {
                        skippedCount++
//                        AppLogger.w("IconPackLoader", "Invalid mapping. Component or drawable is null.")
                    }
                }
                eventType = parser.next()
            }

            inputStream.close()
            isInitialized = true

//            AppLogger.d("IconPackLoader", "Preload finished. Loaded: $loadedCount, Skipped: $skippedCount")

        } catch (e: Exception) {
            AppLogger.e("IconPackLoader", "Error while preloading icon pack: ${e.message}", e)
        }
    }

    fun getCachedIcon(context: Context, packageName: String, target: IconCacheTarget): Drawable? {
        val cachedIcon = when (target) {
            IconCacheTarget.APP_LIST -> appListIconCache[packageName]
            IconCacheTarget.HOME -> homeIconCache[packageName]
        }

        return cachedIcon ?: try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun isReady(): Boolean = isInitialized
}
