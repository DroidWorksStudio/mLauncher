package app.olaunchercf.data

import android.content.Context
import android.content.SharedPreferences
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import androidx.core.content.getSystemService
import app.olaunchercf.helper.getUserHandleFromString
import java.lang.RuntimeException

private const val APP_LANGUAGE = "app_language"
private const val PREFS_FILENAME = "app.olauncher"

private const val FIRST_OPEN = "FIRST_OPEN"
private const val FIRST_SETTINGS_OPEN = "FIRST_SETTINGS_OPEN"
private const val FIRST_HIDE = "FIRST_HIDE"
private const val LOCK_MODE = "LOCK_MODE"
private const val HOME_APPS_NUM = "HOME_APPS_NUM"
private const val AUTO_SHOW_KEYBOARD = "AUTO_SHOW_KEYBOARD"
private const val HOME_ALIGNMENT = "HOME_ALIGNMENT"
private const val DRAWER_ALIGNMENT = "DRAWER_ALIGNMENT"
private const val TIME_ALIGNMENT = "TIME_ALIGNMENT"
private const val STATUS_BAR = "STATUS_BAR"
private const val SHOW_DATE = "SHOW_DATE"
private const val HOME_LOCKED = "HOME_LOCKED"
private const val SHOW_TIME = "SHOW_TIME"
private const val SWIPE_LEFT_ENABLED = "SWIPE_LEFT_ENABLED"
private const val SWIPE_RIGHT_ENABLED = "SWIPE_RIGHT_ENABLED"
private const val SCREEN_TIMEOUT = "SCREEN_TIMEOUT"
private const val HIDDEN_APPS = "HIDDEN_APPS"
private const val HIDDEN_APPS_UPDATED = "HIDDEN_APPS_UPDATED"
private const val SHOW_HINT_COUNTER = "SHOW_HINT_COUNTER"
private const val APP_THEME = "APP_THEME"

private const val APP_NAME = "APP_NAME"
private const val APP_PACKAGE = "APP_PACKAGE"
private const val APP_USER = "APP_USER"
private const val APP_ALIAS = "APP_ALIAS"
private const val APP_ACTIVITY = "APP_ACTIVITY"

private const val SWIPE_RIGHT = "SWIPE_RIGHT"
private const val SWIPE_LEFT = "SWIPE_LEFT"
private const val CLICK_CLOCK = "CLICK_CLOCK"
private const val CLICK_DATE = "CLICK_DATE"

private const val TEXT_SIZE = "text_size"

class Prefs(val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_FILENAME, 0)

    var firstOpen: Boolean
        get() = prefs.getBoolean(FIRST_OPEN, true)
        set(value) = prefs.edit().putBoolean(FIRST_OPEN, value).apply()

    var firstSettingsOpen: Boolean
        get() = prefs.getBoolean(FIRST_SETTINGS_OPEN, true)
        set(value) = prefs.edit().putBoolean(FIRST_SETTINGS_OPEN, value).apply()

    var lockModeOn: Boolean
        get() = prefs.getBoolean(LOCK_MODE, false)
        set(value) = prefs.edit().putBoolean(LOCK_MODE, value).apply()

    var autoShowKeyboard: Boolean
        get() = prefs.getBoolean(AUTO_SHOW_KEYBOARD, true)
        set(value) = prefs.edit().putBoolean(AUTO_SHOW_KEYBOARD, value).apply()

    var homeAppsNum: Int
        get() {
            return try {
                prefs.getInt(HOME_APPS_NUM, 4)
            } catch (_: Exception) {
                4
            }
        }
        set(value) = prefs.edit().putInt(HOME_APPS_NUM, value).apply()

    var homeAlignment: Constants.Gravity
        get() {
            return try {
                val string = prefs.getString(
                    HOME_ALIGNMENT,
                    Constants.Gravity.Left.name
                ).toString()
                Constants.Gravity.valueOf(string)
            } catch (_: Exception) {
                Constants.Gravity.Left
            }
        }
        set(value) = prefs.edit().putString(HOME_ALIGNMENT, value.toString()).apply()

    var timeAlignment: Constants.Gravity
        get() {
            val string = prefs.getString(
                TIME_ALIGNMENT,
                Constants.Gravity.Left.name
            ).toString()
            return Constants.Gravity.valueOf(string)
        }
        set(value) = prefs.edit().putString(TIME_ALIGNMENT, value.toString()).apply()

    var drawerAlignment: Constants.Gravity
        get() {
            val string = prefs.getString(
                DRAWER_ALIGNMENT,
                Constants.Gravity.Right.name
            ).toString()
            return Constants.Gravity.valueOf(string)
        }
        set(value) = prefs.edit().putString(DRAWER_ALIGNMENT, value.name).apply()

    var showStatusBar: Boolean
        get() = prefs.getBoolean(STATUS_BAR, false)
        set(value) = prefs.edit().putBoolean(STATUS_BAR, value).apply()

    var showTime: Boolean
        get() = prefs.getBoolean(SHOW_TIME, true)
        set(value) = prefs.edit().putBoolean(SHOW_TIME, value).apply()

    var showDate: Boolean
        get() = prefs.getBoolean(SHOW_DATE, true)
        set(value) = prefs.edit().putBoolean(SHOW_DATE, value).apply()

    var homeLocked: Boolean
        get() = prefs.getBoolean(HOME_LOCKED, false)
        set(value) = prefs.edit().putBoolean(HOME_LOCKED, value).apply()

    var swipeLeftEnabled: Boolean
        get() = prefs.getBoolean(SWIPE_LEFT_ENABLED, true)
        set(value) = prefs.edit().putBoolean(SWIPE_LEFT_ENABLED, value).apply()

    var swipeRightEnabled: Boolean
        get() = prefs.getBoolean(SWIPE_RIGHT_ENABLED, true)
        set(value) = prefs.edit().putBoolean(SWIPE_RIGHT_ENABLED, value).apply()

    var appTheme: Constants.Theme
        get() {
            return try {
                Constants.Theme.valueOf(prefs.getString(APP_THEME, Constants.Theme.System.name).toString())
            } catch (_: Exception) {
                Constants.Theme.System
            }
        }
        set(value) = prefs.edit().putString(APP_THEME, value.name).apply()

    var language: Constants.Language
        get() {
            return try {
                Constants.Language.valueOf(prefs.getString(APP_LANGUAGE, Constants.Language.English.name).toString())
            } catch (_: Exception) {
                Constants.Language.English
            }
        }
        set(value) = prefs.edit().putString(APP_LANGUAGE, value.name).apply()

    var hiddenApps: MutableSet<String>
        get() = prefs.getStringSet(HIDDEN_APPS, mutableSetOf()) as MutableSet<String>
        set(value) = prefs.edit().putStringSet(HIDDEN_APPS, value).apply()

    var hiddenAppsUpdated: Boolean
        get() = prefs.getBoolean(HIDDEN_APPS_UPDATED, false)
        set(value) = prefs.edit().putBoolean(HIDDEN_APPS_UPDATED, value).apply()

    var toShowHintCounter: Int
        get() = prefs.getInt(SHOW_HINT_COUNTER, 1)
        set(value) = prefs.edit().putInt(SHOW_HINT_COUNTER, value).apply()

    fun getHomeAppModel(i:Int): AppModel {
        return loadApp("$i")
    }

    fun setHomeAppModel(i: Int, appModel: AppModel) {
        storeApp("$i", appModel)
    }

    fun setHomeAppName(i: Int, name: String) {
        val nameId = "${APP_NAME}_$i"
        prefs.edit().putString(nameId, name).apply()
    }

    // this only resets name and alias because of how this was done before in HomeFragment
    fun resetHomeAppValues(i: Int) {
        val nameId = "${APP_NAME}_$i"
        val aliasId = "${APP_ALIAS}_$i"

        prefs.edit().putString(nameId, "").apply()
        prefs.edit().putString(aliasId, "").apply()
    }

    var appSwipeRight: AppModel
        get() = loadApp(SWIPE_RIGHT)
        set(appModel) = storeApp(SWIPE_RIGHT, appModel)

    var appSwipeLeft: AppModel
        get() = loadApp(SWIPE_LEFT)
        set(appModel) = storeApp(SWIPE_LEFT, appModel)

    var appClickClock: AppModel
        get() = loadApp(CLICK_CLOCK)
        set(appModel) = storeApp(CLICK_CLOCK, appModel)

    var appClickDate: AppModel
        get() = loadApp(CLICK_DATE)
        set(appModel) = storeApp(CLICK_DATE, appModel)


    private fun loadApp(id: String): AppModel {
        val name = prefs.getString("${APP_NAME}_$id", "").toString()
        val pack = prefs.getString("${APP_PACKAGE}_$id", "").toString()
        val alias = prefs.getString("${APP_ALIAS}_$id", "").toString()
        val activity = prefs.getString("${APP_ACTIVITY}_$id", "").toString()

        val userHandleString = try { prefs.getString("${APP_USER}_$id", "").toString() } catch (_: Exception) { "" }
        val userHandle: UserHandle = getUserHandleFromString(context, userHandleString)

        return AppModel(
            appLabel = name,
            appPackage = pack,
            appAlias = alias,
            appActivityName = activity,
            user = userHandle,
            key = null,
        )
    }

    private fun storeApp(id: String, appModel: AppModel) {
        val edit = prefs.edit()
        edit.putString("${APP_NAME}_$id", appModel.appLabel)
        edit.putString("${APP_PACKAGE}_$id", appModel.appPackage)
        edit.putString("${APP_ACTIVITY}_$id", appModel.appActivityName)
        edit.putString("${APP_ALIAS}_$id", appModel.appAlias)
        edit.putString("${APP_USER}_$id", appModel.user.toString())
        edit.apply()
    }

    var textSize: Int
        get() {
            return try {
                prefs.getInt(TEXT_SIZE, 18)
            } catch (_: Exception) {
                18
            }
        }
        set(value) = prefs.edit().putInt(TEXT_SIZE, value).apply()


    // return app label
    fun getAppName(location: Int): String {
        return getHomeAppModel(location).appLabel
    }

    fun getAppAlias(appName: String): String {
        return prefs.getString(appName, "").toString()
    }
    fun setAppAlias(appPackage: String, appAlias: String) {
        prefs.edit().putString(appPackage, appAlias).apply()
    }
}
