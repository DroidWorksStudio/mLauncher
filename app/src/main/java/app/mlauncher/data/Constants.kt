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
        Chinese,
        Croatian,
        Danish,
        Dutch,
        English,
        Estonian,
        French,
        German,
        Greek,
        Hawaiian,
        Hebrew,
        Icelandic,
        Indonesian,
        Irish,
        Italian,
        Korean,
        Malay,
        Malayalam,
        Norwegian,
        Persian,
        Portuguese,
        Polish,
        Romanian,
        Russian,
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
                Chinese -> "中文"
                Croatian -> "Hrvatski"
                Dutch -> "Nederlands"
                Danish -> "dansk"
                English -> "English"
                Estonian -> "Eesti keel"
                French -> "Français"
                German -> "Deutsch"
                Greek -> "Ελληνική"
                Hawaiian -> "ʻŌlelo Hawaiʻi"
                Hebrew -> "עִברִית"
                Icelandic -> "íslenskur"
                Indonesian -> "Bahasa Indonesia"
                Irish -> "Gaeilge"
                Italian -> "Italiano"
                Korean -> "한국어"
                Norwegian -> "norsk"
                Malay -> "Melayu"
                Malayalam -> "മലയാളം"
                Persian -> "فارسی"
                Portuguese -> "Português"
                Polish -> "Polski"
                Romanian -> "Română"
                Russian -> "Русский"
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
            Language.Chinese -> "cn"
            Language.Croatian -> "hr"
            Language.Dutch -> "nl"
            Language.Danish -> "da"
            Language.English -> "en"
            Language.Estonian -> "et"
            Language.French -> "fr"
            Language.German -> "de"
            Language.Greek -> "gr"
            Language.Hawaiian -> "haw"
            Language.Hebrew -> "iw"
            Language.Icelandic -> "is"
            Language.Irish -> "ga"
            Language.Indonesian -> "id"
            Language.Italian -> "it"
            Language.Korean -> "ko"
            Language.Norwegian -> "no"
            Language.Malay -> "ms"
            Language.Malayalam -> "ml"
            Language.Persian -> "fa"
            Language.Portuguese -> "pt"
            Language.Polish -> "pl"
            Language.Romanian -> "ro"
            Language.Russian -> "ru"
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
        ShowNotification;

        @Composable
        override fun string(): String {
            return when(this) {
                OpenApp -> stringResource(R.string.open_app)
                LockScreen -> stringResource(R.string.lock_screen)
                ShowNotification -> stringResource(R.string.show_notifications)
                ShowAppList -> stringResource(R.string.show_app_list)
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
