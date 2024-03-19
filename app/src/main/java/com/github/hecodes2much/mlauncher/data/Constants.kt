package com.github.hecodes2much.mlauncher.data

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.github.hecodes2much.mlauncher.R
import java.util.Locale

interface EnumOption {
    @Composable
    fun string(): String
}

object Constants {

    const val REQUEST_CODE_ENABLE_ADMIN = 666

    const val TRIPLE_TAP_DELAY_MS = 300
    const val LONG_PRESS_DELAY_MS = 500

    const val MAX_HOME_APPS = 30
    const val TEXT_SIZE_MIN = 10
    const val TEXT_SIZE_MAX = 50

    const val TEXT_MARGIN_MIN = 0
    const val TEXT_MARGIN_MAX = 50

    const val BACKUP_WRITE = 1
    const val BACKUP_READ = 2

    const val RECENT_COUNTER_MIN = 1
    const val RECENT_COUNTER_MAX = 35

    const val FILTER_STRENGTH_MIN = 1
    const val FILTER_STRENGTH_MAX = 99

    enum class AppDrawerFlag {
        LaunchApp,
        HiddenApps,
        SetHomeApp,
        SetSwipeLeft,
        SetSwipeRight,
        SetSwipeUp,
        SetSwipeDown,
        SetClickClock,
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
        English,
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
                English -> "English"
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
                English -> "en"
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
                Spanish -> "es"
                Swahili -> "sw"
                Swedish -> "sv"
                Thai -> "th"
                Turkish -> "tr"
                Ukrainian -> "uk"
                Vietnamese -> "vi"
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
        Disabled,
        OpenApp,
        LockScreen,
        ShowAppList,
        OpenQuickSettings,
        ShowRecents,
        OpenPowerDialog,
        TakeScreenShot,
        ShowNotification;

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
                Disabled -> stringResource(R.string.disabled)
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
}
