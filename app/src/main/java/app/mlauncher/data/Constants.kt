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
    const val TEXT_SIZE_MAX = 60

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
        Spanish,
        Swedish,
        Thai,
        Turkish;

        @Composable
        override fun string(): String {
            return when(this) {
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
                Spanish -> "Español"
                Swedish -> "Svenska"
                Thai -> "ไทย"
                Turkish -> "Türkçe"
            }
        }
    }

    fun Language.value(): String {
        return when(this) {
            Language.System -> Locale.getDefault().language
            Language.Albanian -> "sq"
            Language.Arabic -> "ar"
            Language.Bulgarian -> "bg"
            Language.Chinese -> "cn"
            Language.Croatian -> "hr"
            Language.Czech -> "cs"
            Language.Dutch -> "nl"
            Language.Danish -> "da"
            Language.English -> "en"
            Language.Estonian -> "et"
            Language.Filipino -> "fil"
            Language.Finnish -> "fi"
            Language.French -> "fr"
            Language.Georgian -> "ka"
            Language.German -> "de"
            Language.Greek -> "el"
            Language.Hawaiian -> "haw"
            Language.Hebrew -> "iw"
            Language.Hindi -> "hi"
            Language.Hungarian -> "hu"
            Language.Icelandic -> "is"
            Language.Irish -> "ga"
            Language.Indonesian -> "lb"
            Language.Italian -> "it"
            Language.Japanese -> "ja"
            Language.Korean -> "ko"
            Language.Lithuanian -> "lt"
            Language.Luxembourgish -> ""
            Language.Malay -> "ms"
            Language.Malagasy -> "mg"
            Language.Malayalam -> "ml"
            Language.Nepali -> "ne"
            Language.Norwegian -> "no"
            Language.Persian -> "fa"
            Language.Portuguese -> "pt"
            Language.Polish -> "pl"
            Language.Punjabi -> "pa"
            Language.Romanian -> "ro"
            Language.Russian -> "ru"
            Language.Serbian -> "sr"
            Language.Spanish -> "es"
            Language.Swedish -> "sv"
            Language.Thai -> "th"
            Language.Turkish -> "tr"
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
}
