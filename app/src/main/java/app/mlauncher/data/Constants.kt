package app.mlauncher.data

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.mlauncher.R
import java.util.*

interface EnumOption {
    @Composable
    fun string(): String
}

object Constants {

    const val REQUEST_CODE_ENABLE_ADMIN = 666

    const val TRIPLE_TAP_DELAY_MS = 300
    const val LONG_PRESS_DELAY_MS = 500

    const val MAX_HOME_APPS = 15
    const val TEXT_SIZE_MIN = 10
    const val TEXT_SIZE_MAX = 50

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

    enum class Language: EnumOption {
        System,
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
        Lithuanian,
        Luxembourgish,
        Malay,
        Malagasy,
        Malayalam,
        Nepali,
        Norwegian,
        Persian,
        Portuguese,
        Polish,
        Punjabi,
        Romanian,
        Russian,
        Serbian,
        Sindhi,
        Spanish,
        Swedish,
        Thai,
        Turkish,
        Ukrainian,
        Vietnamese;

        @Composable
        override fun string(): String {
            return when (this) {
                System -> stringResource(R.string.lang_system)
                Albanian -> "shqiptare"
                Arabic -> "العربية"
                Bulgarian -> "български"
                Chinese -> "中文"
                Croatian -> "Hrvatski"
                Czech -> "čeština"
                Dutch -> "Nederlands"
                Danish -> "dansk"
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
                Lithuanian -> "Lietuvių"
                Luxembourgish -> "lëtzebuergesch"
                Malay -> "Melayu"
                Malagasy -> "Malagasy"
                Malayalam -> "മലയാളം"
                Nepali -> "नेपाली"
                Norwegian -> "norsk"
                Persian -> "فارسی"
                Portuguese -> "Português"
                Polish -> "Polski"
                Punjabi -> "ਪੰਜਾਬੀ"
                Romanian -> "Română"
                Russian -> "Русский"
                Serbian -> "Српски"
                Sindhi -> "سنڌي"
                Spanish -> "Español"
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
            return when(this) {
                System -> Locale.getDefault().language
                Albanian -> "sq"
                Arabic -> "ar"
                Bulgarian -> "bg"
                Chinese -> "cn"
                Croatian -> "hr"
                Czech -> "cs"
                Dutch -> "nl"
                Danish -> "da"
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
                Irish -> "ga"
                Indonesian -> "lb"
                Italian -> "it"
                Japanese -> "ja"
                Korean -> "ko"
                Lithuanian -> "lt"
                Luxembourgish -> "lb"
                Malay -> "ms"
                Malagasy -> "mg"
                Malayalam -> "ml"
                Nepali -> "ne"
                Norwegian -> "no"
                Persian -> "fa"
                Portuguese -> "pt"
                Polish -> "pl"
                Punjabi -> "pa"
                Romanian -> "ro"
                Russian -> "ru"
                Serbian -> "sr"
                Sindhi -> "sd"
                Spanish -> "es"
                Swedish -> "sv"
                Thai -> "th"
                Turkish -> "tr"
                Ukrainian -> "uk"
                Vietnamese -> "vi"
            }
        }
    }

    enum class Gravity: EnumOption {
        Left,
        Center,
        Right;

        @Composable
        override fun string(): String {
            return when(this) {
                Left -> stringResource(R.string.left)
                Center -> stringResource(R.string.center)
                Right -> stringResource(R.string.right)
            }
        }

        @SuppressLint("RtlHardcoded")
        fun value(): Int {
            return when(this) {
                Left -> android.view.Gravity.LEFT
                Center -> android.view.Gravity.CENTER
                Right -> android.view.Gravity.RIGHT
            }
        }
    }

    enum class Action: EnumOption {
        Disabled,
        OpenApp,
        LockScreen,
        ShowAppList,
        OpenQuickSettings,
        ShowRecents,
        ShowNotification;

        @Composable
        override fun string(): String {
            return when(this) {
                OpenApp -> stringResource(R.string.open_app)
                LockScreen -> stringResource(R.string.lock_screen)
                ShowNotification -> stringResource(R.string.show_notifications)
                ShowAppList -> stringResource(R.string.show_app_list)
                OpenQuickSettings -> stringResource(R.string.open_quick_settings)
                ShowRecents -> stringResource(R.string.show_recents)
                Disabled -> stringResource(R.string.disabled)
            }
        }
    }

    enum class Theme: EnumOption {
        System,
        Dark,
        Light;

        @Composable
        override fun string(): String {
            return when(this) {
                System -> stringResource(R.string.lang_system)
                Dark -> stringResource(R.string.dark)
                Light -> stringResource(R.string.light)
            }
        }
    }

    const val URL_PUBLIC_ROADMAP = "https://tanujnotes.notion.site/Olauncher-Roadmap-1522aa8182424415b5b89bed923b0ccd"
}
