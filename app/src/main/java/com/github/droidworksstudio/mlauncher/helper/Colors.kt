package com.github.droidworksstudio.mlauncher.helper

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.github.droidworksstudio.mlauncher.R
import com.github.droidworksstudio.mlauncher.data.Prefs

class Colors {
    @RequiresApi(Build.VERSION_CODES.Q)
    fun background(context: Context, prefs: Prefs): Int {
        val opacity = getHexForOpacity(context, prefs)
        return when (prefs.appTheme.name) {
            "Dark" -> {
                when (prefs.appDarkColors.name) {
                    "Dracula" -> {
                        getColorWithAccent(context, prefs, R.color.draculaBackground)
                    }
                    "Arc-Darker" -> {
                        getColorWithAccent(context, prefs, R.color.arcBackground)
                    }
                    "Adapta-Nokto" -> {
                        getColorWithAccent(context, prefs, R.color.noktoBackground)
                    }
                    "Breeze" -> {
                        getColorWithAccent(context, prefs, R.color.breezeBackground)
                    }
                    "Yaru" -> {
                        getColorWithAccent(context, prefs, R.color.yaruBackground)
                    }
                    "Nordic" -> {
                        getColorWithAccent(context, prefs, R.color.nordicDarkBackground)
                    }
                    "Catppuccino" -> {
                        getColorWithAccent(context, prefs, R.color.catppuccinoDarkBackground)
                    }
                    else -> {
                        opacity
                    }
                }
            }
            "Light" -> {
                when (prefs.appLightColors.name) {
                    "Adwaita" -> {
                        getColorWithAccent(context, prefs, R.color.adwaitaBackground)
                    }
                    "Nordic" -> {
                        getColorWithAccent(context, prefs, R.color.nordicLightBackground)
                    }
                    "Catppuccino" -> {
                        getColorWithAccent(context, prefs, R.color.catppuccinoLightBackground)
                    }
                    else -> {
                        opacity
                    }
                }
            }
            else -> {
                opacity
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun accents(context: Context, prefs: Prefs, accentNumber: Int): Int {
        return when (prefs.appTheme.name) {
            "Dark" -> {
                when (prefs.appDarkColors.name) {
                    "Dracula" -> {
                        when (accentNumber) {
                            1 -> ContextCompat.getColor(context, R.color.draculaAccent1)
                            2 -> ContextCompat.getColor(context, R.color.draculaAccent2)
                            3 -> ContextCompat.getColor(context, R.color.draculaAccent3)
                            4 -> ContextCompat.getColor(context, R.color.draculaAccent4)
                            else -> ContextCompat.getColor(context, R.color.draculaAccent1)
                        }
                    }

                    "Arc" -> {
                        when (accentNumber) {
                            1 -> ContextCompat.getColor(context, R.color.arcAccent1)
                            2 -> ContextCompat.getColor(context, R.color.arcAccent2)
                            3 -> ContextCompat.getColor(context, R.color.arcAccent3)
                            4 -> ContextCompat.getColor(context, R.color.arcAccent4)
                            else -> ContextCompat.getColor(context, R.color.arcAccent1)
                        }
                    }

                    "Nokto" -> {
                        when (accentNumber) {
                            1 -> ContextCompat.getColor(context, R.color.noktoAccent1)
                            2 -> ContextCompat.getColor(context, R.color.noktoAccent2)
                            3 -> ContextCompat.getColor(context, R.color.noktoAccent3)
                            4 -> ContextCompat.getColor(context, R.color.noktoAccent4)
                            else -> ContextCompat.getColor(context, R.color.noktoAccent1)
                        }
                    }

                    "Breeze" -> {
                        when (accentNumber) {
                            1 -> ContextCompat.getColor(context, R.color.breezeAccent1)
                            2 -> ContextCompat.getColor(context, R.color.breezeAccent2)
                            3 -> ContextCompat.getColor(context, R.color.breezeAccent3)
                            4 -> ContextCompat.getColor(context, R.color.breezeAccent4)
                            else -> ContextCompat.getColor(context, R.color.breezeAccent1)
                        }
                    }

                    "Nordic" -> {
                        when (accentNumber) {
                            1 -> ContextCompat.getColor(context, R.color.nordicDarkAccent1)
                            2 -> ContextCompat.getColor(context, R.color.nordicDarkAccent2)
                            3 -> ContextCompat.getColor(context, R.color.nordicDarkAccent3)
                            4 -> ContextCompat.getColor(context, R.color.nordicDarkAccent4)
                            else -> ContextCompat.getColor(context, R.color.nordicDarkAccent1)
                        }
                    }

                    "Catppuccino" -> {
                        when (accentNumber) {
                            1 -> ContextCompat.getColor(context, R.color.catppuccinoDarkAccent1)
                            2 -> ContextCompat.getColor(context, R.color.catppuccinoDarkAccent2)
                            3 -> ContextCompat.getColor(context, R.color.catppuccinoDarkAccent3)
                            4 -> ContextCompat.getColor(context, R.color.catppuccinoDarkAccent4)
                            else -> ContextCompat.getColor(context, R.color.catppuccinoDarkAccent1)
                        }
                    }

                    "Yaru" -> {
                        when (accentNumber) {
                            1 -> ContextCompat.getColor(context, R.color.yaruAccent1)
                            2 -> ContextCompat.getColor(context, R.color.yaruAccent2)
                            3 -> ContextCompat.getColor(context, R.color.yaruAccent3)
                            4 -> ContextCompat.getColor(context, R.color.yaruAccent4)
                            else -> ContextCompat.getColor(context, R.color.yaruAccent1)
                        }
                    }

                    else -> {
                        when (accentNumber) {
                            1 -> getHexFontColor(context, prefs)
                            2 -> getHexFontColor(context, prefs)
                            3 -> getHexFontColor(context, prefs)
                            4 -> getHexFontColor(context, prefs)
                            else -> getHexFontColor(context, prefs)
                        }
                    }
                }
            }
            "Light" -> {
                when (prefs.appLightColors.name) {
                    "Adwaita" -> {
                        when (accentNumber) {
                            1 -> ContextCompat.getColor(context, R.color.adwaitaAccent1)
                            2 -> ContextCompat.getColor(context, R.color.adwaitaAccent2)
                            3 -> ContextCompat.getColor(context, R.color.adwaitaAccent3)
                            4 -> ContextCompat.getColor(context, R.color.adwaitaAccent4)
                            else -> ContextCompat.getColor(context, R.color.adwaitaAccent1)
                        }
                    }

                    "Nordic" -> {
                        when (accentNumber) {
                            1 -> ContextCompat.getColor(context, R.color.nordicLightAccent1)
                            2 -> ContextCompat.getColor(context, R.color.nordicLightAccent2)
                            3 -> ContextCompat.getColor(context, R.color.nordicLightAccent3)
                            4 -> ContextCompat.getColor(context, R.color.nordicLightAccent4)
                            else -> ContextCompat.getColor(context, R.color.nordicLightAccent1)
                        }
                    }

                    "Catppuccino" -> {
                        when (accentNumber) {
                            1 -> ContextCompat.getColor(context, R.color.catppuccinoLightAccent1)
                            2 -> ContextCompat.getColor(context, R.color.catppuccinoLightAccent2)
                            3 -> ContextCompat.getColor(context, R.color.catppuccinoLightAccent3)
                            4 -> ContextCompat.getColor(context, R.color.catppuccinoLightAccent4)
                            else -> ContextCompat.getColor(context, R.color.catppuccinoLightAccent1)
                        }
                    }

                    else -> {
                        when (accentNumber) {
                            1 -> getHexFontColor(context, prefs)
                            2 -> getHexFontColor(context, prefs)
                            3 -> getHexFontColor(context, prefs)
                            4 -> getHexFontColor(context, prefs)
                            else -> getHexFontColor(context, prefs)
                        }
                    }
                }
            }
            else -> {
                when (accentNumber) {
                    1 -> getHexFontColor(context, prefs)
                    2 -> getHexFontColor(context, prefs)
                    3 -> getHexFontColor(context, prefs)
                    4 -> getHexFontColor(context, prefs)
                    else -> getHexFontColor(context, prefs)
                }
            }
        }
    }

    private fun getColorWithAccent(context: Context,prefs: Prefs, backgroundColorResId: Int): Int {
        val setColor = prefs.opacityNum
        var hexColor = Integer.toHexString(setColor).toString()
        if (hexColor.length < 2)
            hexColor = "$hexColor$hexColor"
        val backgroundColor = ContextCompat.getColor(context, backgroundColorResId)
        val hexAccentColor = java.lang.String.format("%06X", 0xFFFFFF and backgroundColor)
        return android.graphics.Color.parseColor("#${hexColor}$hexAccentColor")
    }
}