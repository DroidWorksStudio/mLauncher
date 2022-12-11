package app.mlauncher.data

import android.annotation.SuppressLint
import android.util.Log
import android.view.Gravity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeCompilerApi
import androidx.compose.ui.res.stringResource
import app.mlauncher.R
import app.mlauncher.data.Constants.toString
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
        SetSwipeDown,
        SetClickClock,
        SetClickDate,
    }

    enum class Language: EnumOption {
        System,
        Arabic,
        Chinese,
        Croatian,
        Dutch,
        English,
        Estonian,
        French,
        German,
        Greek,
        Indonesian,
        Italian,
        Korean,
        Persian,
        Portuguese,
        Russian,
        Spanish,
        Swedish,
        Turkish;

        @Composable
        override fun string(): String {
            return when(this) {
                System -> stringResource(R.string.lang_system)
                Arabic -> "العربية"
                Chinese -> "中文"
                Croatian -> "Hrvatski"
                Dutch -> "Nederlands"
                English -> "English"
                Estonian -> "Eesti keel"
                French -> "Français"
                German -> "Deutsch"
                Greek -> "Ελληνική"
                Indonesian -> "Bahasa Indonesia"
                Italian -> "Italiano"
                Korean -> "한국어"
                Persian -> "فارسی"
                Portuguese -> "Português"
                Russian -> "Русский"
                Spanish -> "Español"
                Swedish -> "Svenska"
                Turkish -> "Türkçe"
            }
        }
    }

    fun Language.value(): String {
        return when(this) {
            Language.System -> Locale.getDefault().language
            Language.Arabic -> "ar"
            Language.Chinese -> "cn"
            Language.Croatian -> "hr"
            Language.Dutch -> "nl"
            Language.English -> "en"
            Language.Estonian -> "et"
            Language.French -> "fr"
            Language.German -> "de"
            Language.Greek -> "gr"
            Language.Indonesian -> "id"
            Language.Italian -> "it"
            Language.Korean -> "ko"
            Language.Persian -> "fa"
            Language.Portuguese -> "pt"
            Language.Russian -> "ru"
            Language.Spanish -> "es"
            Language.Swedish -> "se"
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
