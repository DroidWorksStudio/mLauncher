package com.github.droidworksstudio.mlauncher.data

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
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

    const val BATTERY_SIZE_MIN = 20
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
        Afrikaans,
        Albanian,
        Arabic,
        Bulgarian,
        Chinese,
        Croatian,
        Czech,
        Danish,
        Dutch,
        EnglishUS,
        EnglishGB,
        EnglishCA,
        Estonian,
        Filipino,
        Finnish,
        French,
        Georgian,
        German,
        Greek,
        Hawaiian,
        Hebrew,
        Hindi,
        Hungarian,
        Icelandic,
        Indonesian,
        Irish,
        Italian,
        Japanese,
        Korean,
        Latin,
        Lithuanian,
        Luxembourgish,
        Malagasy,
        Malay,
        Malayalam,
        Maltese,
        Nepali,
        Norwegian,
        Persian,
        Polish,
        Portuguese,
        Punjabi,
        Romanian,
        Russian,
        Serbian,
        Sindhi,
        Slovak,
        Spanish,
        Swahili,
        Swedish,
        Thai,
        Turkish,
        Ukrainian,
        Vietnamese;

        @Composable
        override fun string(): String {
            return when (this) {
                System -> stringResource(R.string.lang_system)
                Afrikaans -> "afrikaans"
                Albanian -> "shqiptare"
                Arabic -> "العربية"
                Bulgarian -> "български"
                Chinese -> "中文"
                Croatian -> "Hrvatski"
                Czech -> "čeština"
                Danish -> "dansk"
                Dutch -> "Nederlands"
                EnglishUS -> "English (US)"
                EnglishGB -> "English (GB)"
                EnglishCA -> "English (CA)"
                Estonian -> "Eesti keel"
                Filipino -> "Filipino"
                Finnish -> "Suomalainen"
                French -> "Français"
                Georgian -> "ქართული"
                German -> "Deutsch"
                Greek -> "Ελληνική"
                Hawaiian -> "ʻŌlelo Hawaiʻi"
                Hebrew -> "עִברִית"
                Hindi -> "हिंदी"
                Hungarian -> "Magyar"
                Icelandic -> "íslenskur"
                Indonesian -> "Bahasa Indonesia"
                Irish -> "Gaeilge"
                Italian -> "Italiano"
                Japanese -> "日本"
                Korean -> "한국어"
                Latin -> "latin"
                Lithuanian -> "Lietuvių"
                Luxembourgish -> "lëtzebuergesch"
                Malagasy -> "Malagasy"
                Malay -> "Melayu"
                Malayalam -> "മലയാളം"
                Maltese -> "malti"
                Nepali -> "नेपाली"
                Norwegian -> "norsk"
                Persian -> "فارسی"
                Polish -> "Polski"
                Portuguese -> "Português"
                Punjabi -> "ਪੰਜਾਬੀ"
                Romanian -> "Română"
                Russian -> "Русский"
                Serbian -> "Српски"
                Sindhi -> "سنڌي"
                Slovak -> "slovenský"
                Spanish -> "Español"
                Swahili -> "kiswahili"
                Swedish -> "Svenska"
                Thai -> "ไทย"
                Turkish -> "Türkçe"
                Ukrainian -> "українська"
                Vietnamese -> "Tiếng Việt"
            }
        }


        fun locale(): Locale {
            return Locale(value())
        }

        private fun value(): String {
            return when (this) {
                System -> Locale.getDefault().language
                Afrikaans -> "af"
                Albanian -> "sq"
                Arabic -> "ar"
                Bulgarian -> "bg"
                Chinese -> "zh"
                Croatian -> "hr"
                Czech -> "cs"
                Danish -> "da"
                Dutch -> "nl"
                EnglishUS -> "en"
                EnglishGB -> "en"
                EnglishCA -> "en"
                Estonian -> "et"
                Filipino -> "fil"
                Finnish -> "fi"
                French -> "fr"
                Georgian -> "ka"
                German -> "de"
                Greek -> "el"
                Hawaiian -> "haw"
                Hebrew -> "iw"
                Hindi -> "hi"
                Hungarian -> "hu"
                Icelandic -> "is"
                Indonesian -> "in"
                Irish -> "ga"
                Italian -> "it"
                Japanese -> "ja"
                Korean -> "ko"
                Latin -> "la"
                Lithuanian -> "lt"
                Luxembourgish -> "lb"
                Malagasy -> "mg"
                Malay -> "ms"
                Malayalam -> "ml"
                Maltese -> "mt"
                Nepali -> "ne"
                Norwegian -> "no"
                Persian -> "fa"
                Polish -> "pl"
                Portuguese -> "pt"
                Punjabi -> "pa"
                Romanian -> "ro"
                Russian -> "ru"
                Serbian -> "sr"
                Sindhi -> "sd"
                Slovak -> "sk"
                Spanish -> "es"
                Swahili -> "sw"
                Swedish -> "sv"
                Thai -> "th"
                Turkish -> "tr"
                Ukrainian -> "uk"
                Vietnamese -> "vi"
            }
        }

        fun timezone(): Locale {
            return Locale(zone())
        }

        private fun zone(): String {
            return when (this) {
                System -> Locale.getDefault().toLanguageTag()
                Afrikaans -> "af-ZA"
                Albanian -> "sq-AL"
                Arabic -> "ar-SA"
                Bulgarian -> "bg-BG"
                Chinese -> "zh-CN"
                Croatian -> "hr-HR"
                Czech -> "cs-CZ"
                Danish -> "da-DK"
                Dutch -> "nl-NL"
                EnglishUS -> "en-US"
                EnglishGB -> "en-GB"
                EnglishCA -> "en-CA"
                Estonian -> "et-EE"
                Filipino -> "fil-PH"
                Finnish -> "fi-FI"
                French -> "fr-FR"
                Georgian -> "ka-GE"
                German -> "de-DE"
                Greek -> "el-GR"
                Hawaiian -> "haw-US"
                Hebrew -> "he-IL"
                Hindi -> "hi-IN"
                Hungarian -> "hu-HU"
                Icelandic -> "is-IS"
                Indonesian -> "id-ID"
                Irish -> "ga-IE"
                Italian -> "it-IT"
                Japanese -> "ja-JP"
                Korean -> "ko-KR"
                Latin -> "la-LA"
                Lithuanian -> "lt-LT"
                Luxembourgish -> "lb-LU"
                Malagasy -> "mg-MG"
                Malay -> "ms-MY"
                Malayalam -> "ml-IN"
                Maltese -> "mt-MT"
                Nepali -> "ne-NP"
                Norwegian -> "no-NO"
                Persian -> "fa-IR"
                Polish -> "pl-PL"
                Portuguese -> "pt-PT"
                Punjabi -> "pa-IN"
                Romanian -> "ro-RO"
                Russian -> "ru-RU"
                Serbian -> "sr-RS"
                Sindhi -> "sd-PK"
                Slovak -> "sk-SK"
                Spanish -> "es-ES"
                Swahili -> "sw-TZ"
                Swedish -> "sv-SE"
                Thai -> "th-TH"
                Turkish -> "tr-TR"
                Ukrainian -> "uk-UA"
                Vietnamese -> "vi-VN"
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

    const val URL_DUCK_SEARCH = "https://duckduckgo.com/?q="
    const val URL_GOOGLE_SEARCH = "https://google.com/search?q="
    const val URL_YAHOO_SEARCH = "https://search.yahoo.com/search?p="
    const val URL_BING_SEARCH = "https://bing.com/search?q="
    const val URL_BRAVE_SEARCH = "https://search.brave.com/search?q="
    const val URL_SWISSCOW_SEARCH = "https://swisscows.com/web?query="
    const val URL_GOOGLE_PLAY_STORE = "https://play.google.com/store/search?c=apps&q"
    const val APP_GOOGLE_PLAY_STORE = "market://search?c=apps&q"
}
