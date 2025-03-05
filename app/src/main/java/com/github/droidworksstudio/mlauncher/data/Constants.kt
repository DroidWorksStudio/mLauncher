package com.github.droidworksstudio.mlauncher.data

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.core.content.res.ResourcesCompat
import com.github.droidworksstudio.mlauncher.R
import com.github.droidworksstudio.mlauncher.helper.getTrueSystemFont
import java.util.Locale

interface EnumOption {
    @Composable
    fun string(): String
}


object Constants {

    const val REQUEST_CODE_ENABLE_ADMIN = 666
    const val REQUEST_SET_DEFAULT_HOME = 777

    const val TRIPLE_TAP_DELAY_MS = 300
    const val LONG_PRESS_DELAY_MS = 500

    const val MIN_HOME_APPS = 0
    const val MAX_HOME_APPS = 30

    const val MIN_EDGE_APPS = 1
    const val MAX_EDGE_APPS = 15

    const val MIN_HOME_PAGES = 1

    const val MIN_TEXT_SIZE = 10
    const val MAX_TEXT_SIZE = 50

    const val BACKUP_WRITE = 1
    const val BACKUP_READ = 2

    const val THEME_BACKUP_WRITE = 11
    const val THEME_BACKUP_READ = 12

    const val IMPORT_WORDS_OF_THE_DAY = 21

    const val MIN_CLOCK_DATE_SIZE = 10
    const val MAX_CLOCK_DATE_SIZE = 120

    const val MIN_ALARM_SIZE = 10
    const val MAX_ALARM_SIZE = 120

    const val MIN_DAILY_WORD_SIZE = 10
    const val MAX_DAILY_WORD_SIZE = 120

    const val MIN_BATTERY_SIZE = 10
    const val MAX_BATTERY_SIZE = 75

    const val MIN_TEXT_PADDING = 0
    const val MAX_TEXT_PADDING = 50

    const val MIN_RECENT_COUNTER = 1
    const val MAX_RECENT_COUNTER = 35

    const val MIN_FILTER_STRENGTH = 0
    const val MAX_FILTER_STRENGTH = 100

    const val MIN_OPACITY = 0
    const val MAX_OPACITY = 255

    const val HOLD_DURATION_THRESHOLD = 1000L // Adjust as needed

    // Update SWIPE_DISTANCE_THRESHOLD dynamically based on screen dimensions
    var SWIPE_DISTANCE_THRESHOLD = 0f

    // Update MAX_HOME_PAGES dynamically based on MAX_HOME_APPS
    var MAX_HOME_PAGES = 10

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
                0.50f * screenWidth // Use 50% of screen width for left/right swipe
            } else {
                0.30f * screenHeight // Use 30% of screen height for up/down swipe
            }
        }
    }

    enum class BackupType {
        FullSystem,
        Theme
    }

    enum class AppDrawerFlag {
        LaunchApp,
        HiddenApps,
        PrivateApps,
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
        SetFloating,
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

        // Function to get a string from a context (for non-Composable use)
        fun getString(context: Context): String {
            return when (this) {
                System -> context.getString(R.string.system_default)
                Arabic -> context.getString(R.string.lang_arabic)
                Dutch -> context.getString(R.string.lang_dutch)
                English -> context.getString(R.string.lang_english)
                French -> context.getString(R.string.lang_french)
                German -> context.getString(R.string.lang_german)
                Hebrew -> context.getString(R.string.lang_hebrew)
                Italian -> context.getString(R.string.lang_italian)
                Japanese -> context.getString(R.string.lang_japanese)
                Korean -> context.getString(R.string.lang_korean)
                Lithuanian -> context.getString(R.string.lang_lithuanian)
                Polish -> context.getString(R.string.lang_polish)
                Portuguese -> context.getString(R.string.lang_portuguese)
                Russian -> context.getString(R.string.lang_russian)
                Slovak -> context.getString(R.string.lang_slovak)
                Spanish -> context.getString(R.string.lang_spanish)
                Thai -> context.getString(R.string.lang_thai)
                Turkish -> context.getString(R.string.lang_turkish)
            }
        }

        @Composable
        override fun string(): String {
            return when (this) {
                System -> stringResource(R.string.system_default)
                Arabic -> stringResource(R.string.lang_arabic)
                Dutch -> stringResource(R.string.lang_dutch)
                English -> stringResource(R.string.lang_english)
                French -> stringResource(R.string.lang_french)
                German -> stringResource(R.string.lang_german)
                Hebrew -> stringResource(R.string.lang_hebrew)
                Italian -> stringResource(R.string.lang_italian)
                Japanese -> stringResource(R.string.lang_japanese)
                Korean -> stringResource(R.string.lang_korean)
                Lithuanian -> stringResource(R.string.lang_lithuanian)
                Polish -> stringResource(R.string.lang_polish)
                Portuguese -> stringResource(R.string.lang_portuguese)
                Russian -> stringResource(R.string.lang_russian)
                Slovak -> stringResource(R.string.lang_slovak)
                Spanish -> stringResource(R.string.lang_spanish)
                Thai -> stringResource(R.string.lang_thai)
                Turkish -> stringResource(R.string.lang_turkish)
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

    enum class IconPacks : EnumOption {
        System,
        EasyDots,
        NiagaraDots,
        Disabled;

        fun getString(context: Context): String {
            return when (this) {
                System -> context.getString(R.string.system_default)
                EasyDots -> context.getString(R.string.app_icons_easy_dots)
                NiagaraDots -> context.getString(R.string.app_icons_niagara_dots)
                Disabled -> context.getString(R.string.disabled)
            }
        }

        @Composable
        override fun string(): String {
            return when (this) {
                System -> stringResource(R.string.system_default)
                EasyDots -> stringResource(R.string.app_icons_easy_dots)
                NiagaraDots -> stringResource(R.string.app_icons_niagara_dots)
                Disabled -> stringResource(R.string.disabled)
            }
        }
    }

    enum class Action : EnumOption {
        OpenApp,
        TogglePrivateSpace,
        LockScreen,
        ShowNotification,
        ShowAppList,
        ShowDigitalWellbeing,
        OpenQuickSettings,
        ShowRecents,
        OpenPowerDialog,
        TakeScreenShot,
        LeftPage,
        RightPage,
        RestartApp,
        Disabled;

        fun getString(context: Context): String {
            return when (this) {
                OpenApp -> context.getString(R.string.open_app)
                LockScreen -> context.getString(R.string.lock_screen)
                TogglePrivateSpace -> context.getString(R.string.private_space)
                ShowNotification -> context.getString(R.string.show_notifications)
                ShowAppList -> context.getString(R.string.show_app_list)
                ShowDigitalWellbeing -> context.getString(R.string.show_digital_wellbeing)
                OpenQuickSettings -> context.getString(R.string.open_quick_settings)
                ShowRecents -> context.getString(R.string.show_recents)
                OpenPowerDialog -> context.getString(R.string.open_power_dialog)
                TakeScreenShot -> context.getString(R.string.take_a_screenshot)
                LeftPage -> context.getString(R.string.left_page)
                RightPage -> context.getString(R.string.right_page)
                RestartApp -> context.getString(R.string.restart_launcher)
                Disabled -> context.getString(R.string.disabled)
            }
        }

        @Composable
        override fun string(): String {
            return when (this) {
                OpenApp -> stringResource(R.string.open_app)
                LockScreen -> stringResource(R.string.lock_screen)
                TogglePrivateSpace -> stringResource(R.string.private_space)
                ShowNotification -> stringResource(R.string.show_notifications)
                ShowAppList -> stringResource(R.string.show_app_list)
                ShowDigitalWellbeing -> stringResource(R.string.show_digital_wellbeing)
                OpenQuickSettings -> stringResource(R.string.open_quick_settings)
                ShowRecents -> stringResource(R.string.show_recents)
                OpenPowerDialog -> stringResource(R.string.open_power_dialog)
                TakeScreenShot -> stringResource(R.string.take_a_screenshot)
                LeftPage -> stringResource(R.string.left_page)
                RightPage -> stringResource(R.string.right_page)
                RestartApp -> stringResource(R.string.restart_launcher)
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

        // Function to get a string from a context (for non-Composable use)
        fun getString(context: Context): String {
            return when (this) {
                System -> context.getString(R.string.system_default)
                Dark -> context.getString(R.string.dark)
                Light -> context.getString(R.string.light)
            }
        }

        // Keep this for Composable usage
        @Composable
        override fun string(): String {
            return when (this) {
                System -> stringResource(R.string.system_default)
                Dark -> stringResource(R.string.dark)
                Light -> stringResource(R.string.light)
            }
        }
    }


    enum class FontFamily : EnumOption {
        System,
        BankGothic,
        Bitter,
        Doto,
        DroidSans,
        FiraCode,
        GreatVibes,
        Hack,
        Lato,
        Lobster,
        Merriweather,
        MiSans,
        Montserrat,
        NotoSans,
        OpenSans,
        Pacifico,
        Quicksand,
        Raleway,
        Roboto,
        SourceCodePro;

        fun getFont(context: Context): Typeface? {
            return when (this) {
                System -> getTrueSystemFont()
                BankGothic -> ResourcesCompat.getFont(context, R.font.bank_gothic)
                Bitter -> ResourcesCompat.getFont(context, R.font.bitter)
                Doto -> ResourcesCompat.getFont(context, R.font.doto)
                FiraCode -> ResourcesCompat.getFont(context, R.font.fira_code)
                DroidSans -> ResourcesCompat.getFont(context, R.font.open_sans)
                GreatVibes -> ResourcesCompat.getFont(context, R.font.great_vibes)
                Hack -> ResourcesCompat.getFont(context, R.font.hack)
                Lato -> ResourcesCompat.getFont(context, R.font.lato)
                Lobster -> ResourcesCompat.getFont(context, R.font.lobster)
                Merriweather -> ResourcesCompat.getFont(context, R.font.merriweather)
                MiSans -> ResourcesCompat.getFont(context, R.font.mi_sans)
                Montserrat -> ResourcesCompat.getFont(context, R.font.montserrat)
                NotoSans -> ResourcesCompat.getFont(context, R.font.noto_sans)
                OpenSans -> ResourcesCompat.getFont(context, R.font.open_sans)
                Pacifico -> ResourcesCompat.getFont(context, R.font.pacifico)
                Quicksand -> ResourcesCompat.getFont(context, R.font.quicksand)
                Raleway -> ResourcesCompat.getFont(context, R.font.raleway)
                Roboto -> ResourcesCompat.getFont(context, R.font.roboto)
                SourceCodePro -> ResourcesCompat.getFont(context, R.font.source_code_pro)
            }
        }

        @Composable
        override fun string(): String {
            return when (this) {
                System -> stringResource(R.string.system_default)
                BankGothic -> stringResource(R.string.settings_font_bank_gothic)
                Bitter -> stringResource(R.string.settings_font_bitter)
                Doto -> stringResource(R.string.settings_font_doto)
                DroidSans -> stringResource(R.string.settings_font_droidsans)
                FiraCode -> stringResource(R.string.settings_font_firacode)
                GreatVibes -> stringResource(R.string.settings_font_greatvibes)
                Hack -> stringResource(R.string.settings_font_hack)
                Lato -> stringResource(R.string.settings_font_lato)
                Lobster -> stringResource(R.string.settings_font_lobster)
                Merriweather -> stringResource(R.string.settings_font_merriweather)
                MiSans -> stringResource(R.string.settings_font_misans)
                Montserrat -> stringResource(R.string.settings_font_montserrat)
                NotoSans -> stringResource(R.string.settings_font_notosans)
                OpenSans -> stringResource(R.string.settings_font_opensans)
                Pacifico -> stringResource(R.string.settings_font_pacifico)
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
