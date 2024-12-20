package com.github.droidworksstudio.mlauncher.data

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.core.content.res.ResourcesCompat
import com.github.droidworksstudio.mlauncher.R
import java.util.Locale

interface EnumOption {
    @Composable
    fun string(): String
}

object Constants {

    const val REQUEST_CODE_ENABLE_ADMIN = 666

    const val NEW_PAGE = "\uF444"
    const val CURRENT_PAGE = "\uF192"

    const val TRIPLE_TAP_DELAY_MS = 300
    const val LONG_PRESS_DELAY_MS = 500

    const val MIN_HOME_APPS = 0
    const val MAX_HOME_APPS = 30
    const val MIN_HOME_PAGES = 1
    const val TEXT_SIZE_MIN = 10
    const val TEXT_SIZE_MAX = 50

    const val BACKUP_WRITE = 1
    const val BACKUP_READ = 2

    const val CLOCK_DATE_SIZE_MIN = 20
    const val CLOCK_DATE_SIZE_MAX = 150

    const val BATTERY_SIZE_MIN = 10
    const val BATTERY_SIZE_MAX = 150

    const val TEXT_MARGIN_MIN = 0
    const val TEXT_MARGIN_MAX = 50

    const val RECENT_COUNTER_MIN = 1
    const val RECENT_COUNTER_MAX = 35

    const val FILTER_STRENGTH_MIN = 0
    const val FILTER_STRENGTH_MAX = 100

    const val MIN_OPACITY = 0
    const val MAX_OPACITY = 255

    const val HOLD_DURATION_THRESHOLD = 1000L // Adjust as needed

    // Update SWIPE_DISTANCE_THRESHOLD dynamically based on screen dimensions
    var SWIPE_DISTANCE_THRESHOLD: Float = 0f

    // Update MAX_HOME_PAGES dynamically based on MAX_HOME_APPS
    var MAX_HOME_PAGES: Int = 5

    fun updateMaxHomePages(context: Context) {
        val prefs = Prefs(context)

        MAX_HOME_PAGES = if (prefs.homeAppsNum < MAX_HOME_PAGES) {
            prefs.homeAppsNum
        } else {
            MAX_HOME_PAGES
        }

    }

    fun updateSwipeDistanceThreshold(context: Context, direction: String) {
        val displayMetrics = context.resources.displayMetrics

        // Obtain screen dimensions from displayMetrics
        val screenWidth = displayMetrics.widthPixels.toFloat()
        val screenHeight = displayMetrics.heightPixels.toFloat()

        // Ensure display metrics are valid
        if (screenWidth > 0 && screenHeight > 0) {
            SWIPE_DISTANCE_THRESHOLD = if (direction == "left" || direction == "right") {
                0.80f * screenWidth // Use 80% of screen width for left/right swipe
            } else {
                0.50f * screenHeight // Use 50% of screen height for up/down swipe
            }
        }
    }


    enum class AppDrawerFlag {
        LaunchApp,
        HiddenApps,
        ReorderApps,
        SetHomeApp,
        SetShortSwipeUp,
        SetShortSwipeDown,
        SetShortSwipeLeft,
        SetShortSwipeRight,
        SetLongSwipeUp,
        SetLongSwipeDown,
        SetLongSwipeLeft,
        SetLongSwipeRight,
        SetClickClock,
        SetAppUsage,
        SetClickDate,
        SetDoubleTap,
    }

    enum class Language : EnumOption {
        System,
        Arabic,
        Dutch,
        English,
        French,
        German,
        Hebrew,
        Italian,
        Japanese,
        Korean,
        Lithuanian,
        Polish,
        Portuguese,
        Russian,
        Slovak,
        Spanish,
        Thai,
        Turkish;

        @Composable
        override fun string(): String {
            return when (this) {
                System -> stringResource(R.string.lang_system)
                Arabic -> "العربية"
                Dutch -> "Nederlands"
                English -> "English "
                French -> "Français"
                German -> "Deutsch"
                Hebrew -> "עִברִית"
                Italian -> "Italiano"
                Japanese -> "日本"
                Korean -> "한국어"
                Lithuanian -> "Lietuvių"
                Polish -> "Polski"
                Portuguese -> "Português"
                Russian -> "Русский"
                Slovak -> "slovenský"
                Spanish -> "Español"
                Thai -> "ไทย"
                Turkish -> "Türkçe"
            }
        }


        fun locale(): Locale {
            return Locale(value())
        }

        private fun value(): String {
            return when (this) {
                System -> Locale.getDefault().language
                Arabic -> "ar"
                Dutch -> "nl"
                English -> "en"
                French -> "fr"
                German -> "de"
                Hebrew -> "iw"
                Italian -> "it"
                Japanese -> "ja"
                Korean -> "ko"
                Lithuanian -> "lt"
                Polish -> "pl"
                Portuguese -> "pt"
                Russian -> "ru"
                Slovak -> "sk"
                Spanish -> "es"
                Thai -> "th"
                Turkish -> "tr"
            }
        }

        fun timezone(): Locale {
            return Locale(zone())
        }

        private fun zone(): String {
            return when (this) {
                System -> Locale.getDefault().toLanguageTag()
                Arabic -> "ar-SA"
                Dutch -> "nl-NL"
                English -> "en-US"
                French -> "fr-FR"
                German -> "de-DE"
                Hebrew -> "he-IL"
                Italian -> "it-IT"
                Japanese -> "ja-JP"
                Korean -> "ko-KR"
                Lithuanian -> "lt-LT"
                Polish -> "pl-PL"
                Portuguese -> "pt-PT"
                Russian -> "ru-RU"
                Slovak -> "sk-SK"
                Spanish -> "es-ES"
                Thai -> "th-TH"
                Turkish -> "tr-TR"
            }
        }
    }

    enum class Gravity : EnumOption {
        Left,
        Center,
        Right;

        @Composable
        override fun string(): String {
            return when (this) {
                Left -> stringResource(R.string.left)
                Center -> stringResource(R.string.center)
                Right -> stringResource(R.string.right)
            }
        }

        @SuppressLint("RtlHardcoded")
        fun value(): Int {
            return when (this) {
                Left -> android.view.Gravity.LEFT
                Center -> android.view.Gravity.CENTER
                Right -> android.view.Gravity.RIGHT
            }
        }
    }

    enum class Action : EnumOption {
        OpenApp,
        LockScreen,
        ShowNotification,
        ShowAppList,
        OpenQuickSettings,
        ShowRecents,
        OpenPowerDialog,
        TakeScreenShot,
        LeftPage,
        RightPage,
        Disabled;

        @Composable
        override fun string(): String {
            return when (this) {
                OpenApp -> stringResource(R.string.open_app)
                LockScreen -> stringResource(R.string.lock_screen)
                ShowNotification -> stringResource(R.string.show_notifications)
                ShowAppList -> stringResource(R.string.show_app_list)
                OpenQuickSettings -> stringResource(R.string.open_quick_settings)
                ShowRecents -> stringResource(R.string.show_recents)
                OpenPowerDialog -> stringResource(R.string.open_power_dialog)
                TakeScreenShot -> stringResource(R.string.take_a_screenshot)
                LeftPage -> stringResource(R.string.left_page)
                RightPage -> stringResource(R.string.right_page)
                Disabled -> stringResource(R.string.disabled)
            }
        }
    }

    enum class SearchEngines : EnumOption {
        Google,
        Yahoo,
        DuckDuckGo,
        Bing,
        Brave,
        SwissCow;

        @Composable
        override fun string(): String {
            return when (this) {
                Google -> stringResource(R.string.search_google)
                Yahoo -> stringResource(R.string.search_yahoo)
                DuckDuckGo -> stringResource(R.string.search_duckduckgo)
                Bing -> stringResource(R.string.search_bing)
                Brave -> stringResource(R.string.search_brave)
                SwissCow -> stringResource(R.string.search_swisscow)
            }
        }
    }

    enum class Theme : EnumOption {
        System,
        Dark,
        Light;

        @Composable
        override fun string(): String {
            return when (this) {
                System -> stringResource(R.string.lang_system)
                Dark -> stringResource(R.string.dark)
                Light -> stringResource(R.string.light)
            }
        }
    }

    enum class DarkColors : EnumOption {
        System,
        Dracula,
        Arc,
        Nokto,
        Breeze,
        Catppuccino,
        Nordic,
        Yaru;

        @Composable
        override fun string(): String {
            return when (this) {
                System -> stringResource(R.string.lang_system)
                Dracula -> stringResource(R.string.dracula)
                Arc -> stringResource(R.string.arc)
                Nokto -> stringResource(R.string.nokto)
                Breeze -> stringResource(R.string.breeze)
                Catppuccino -> stringResource(R.string.catppuccino)
                Nordic -> stringResource(R.string.nordic)
                Yaru -> stringResource(R.string.yaru)
            }
        }
    }

    enum class LightColors : EnumOption {
        System,
        Adwaita,
        Catppuccino,
        Nordic;

        @Composable
        override fun string(): String {
            return when (this) {
                System -> stringResource(R.string.lang_system)
                Adwaita -> stringResource(R.string.adwaita)
                Catppuccino -> stringResource(R.string.catppuccino)
                Nordic -> stringResource(R.string.nordic)
            }
        }
    }

    enum class Fonts : EnumOption {
        System,
        Bitter,
        Dotness,
        DroidSans,

        //        GreatVibes,
        Lato,

        //        Lobster,
        Merriweather,
        Montserrat,
        OpenSans,

        //        Pacifico,
        Quicksand,
        Raleway,
        Roboto,
        SourceCodePro;

        fun getFont(context: Context): Typeface? {
            return when (this) {
                System -> Typeface.DEFAULT
                Bitter -> ResourcesCompat.getFont(context, R.font.bitter)
                Dotness -> ResourcesCompat.getFont(context, R.font.dotness)
                DroidSans -> ResourcesCompat.getFont(context, R.font.open_sans)
//                GreatVibes -> ResourcesCompat.getFont(context, R.font.great_vibes)
                Lato -> ResourcesCompat.getFont(context, R.font.lato)
//                Lobster -> ResourcesCompat.getFont(context, R.font.lobster)
                Merriweather -> ResourcesCompat.getFont(context, R.font.merriweather)
                Montserrat -> ResourcesCompat.getFont(context, R.font.montserrat)
                OpenSans -> ResourcesCompat.getFont(context, R.font.open_sans)
//                Pacifico -> ResourcesCompat.getFont(context, R.font.pacifico)
                Quicksand -> ResourcesCompat.getFont(context, R.font.quicksand)
                Raleway -> ResourcesCompat.getFont(context, R.font.raleway)
                Roboto -> ResourcesCompat.getFont(context, R.font.roboto)
                SourceCodePro -> ResourcesCompat.getFont(context, R.font.source_code_pro)
            }
        }

        @Composable
        override fun string(): String {
            return when (this) {
                System -> stringResource(R.string.settings_font_system)
                Bitter -> stringResource(R.string.settings_font_bitter)
                Dotness -> stringResource(R.string.settings_font_dotness)
                DroidSans -> stringResource(R.string.settings_font_droidsans)
//                GreatVibes -> stringResource(R.string.settings_font_greatvibes)
                Lato -> stringResource(R.string.settings_font_lato)
//                Lobster -> stringResource(R.string.settings_font_lobster)
                Merriweather -> stringResource(R.string.settings_font_merriweather)
                Montserrat -> stringResource(R.string.settings_font_montserrat)
                OpenSans -> stringResource(R.string.settings_font_opensans)
//                Pacifico -> stringResource(R.string.settings_font_pacifico)
                Quicksand -> stringResource(R.string.settings_font_quicksand)
                Raleway -> stringResource(R.string.settings_font_raleway)
                Roboto -> stringResource(R.string.settings_font_roboto)
                SourceCodePro -> stringResource(R.string.settings_font_sourcecodepro)
            }
        }
    }

    const val URL_DUCK_SEARCH = "https://duckduckgo.com/?q="
    const val URL_GOOGLE_SEARCH = "https://google.com/search?q="
    const val URL_YAHOO_SEARCH = "https://search.yahoo.com/search?p="
    const val URL_BING_SEARCH = "https://bing.com/search?q="
    const val URL_BRAVE_SEARCH = "https://search.brave.com/search?q="
    const val URL_SWISSCOW_SEARCH = "https://swisscows.com/web?query="
    const val URL_GOOGLE_PLAY_STORE = "https://play.google.com/store/search?c=apps&q"
    const val APP_GOOGLE_PLAY_STORE = "market://search?c=apps&q"
}
