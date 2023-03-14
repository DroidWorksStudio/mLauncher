package com.github.hecodes2much.mlauncher.helper

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.widget.TextView
import com.github.hecodes2much.mlauncher.R
import com.github.hecodes2much.mlauncher.data.Prefs

class BatteryReceiver : BroadcastReceiver() {

    private lateinit var prefs: Prefs

    /* get current battery percentage */
    private fun batteryPercentage(intent: Intent): Int {
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val percentage = level / scale.toFloat()
        return (percentage * 100).toInt()
    }

    /* get current charging status */
    private fun chargingStatus(intent: Intent): Int {
        return intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    }

    override fun onReceive(context: Context, intent: Intent) {
        prefs = Prefs(context)
        /* set battery percentage value to the circular progress bar */
        val batteryLevel = batteryPercentage(intent)

        /* progress bar animation */
        if (
            chargingStatus(intent) == BatteryManager.BATTERY_STATUS_DISCHARGING ||
            chargingStatus(intent) == BatteryManager.BATTERY_STATUS_NOT_CHARGING
        ) {
            updateBatteryStatus(context, batteryLevel, isCharging = false)
        } else if (
            chargingStatus(intent) == BatteryManager.BATTERY_STATUS_CHARGING ||
            chargingStatus(intent) == BatteryManager.BATTERY_STATUS_FULL
        ) {
            updateBatteryStatus(context, batteryLevel, isCharging = true)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateBatteryStatus(context: Context, batteryLevel: Int, isCharging: Boolean) {
        if (!prefs.showBattery) return
        val icon: String
        @Suppress("UNUSED_EXPRESSION")
        if (isCharging) {
            icon = "\uF583"
        } else {
            icon = when (batteryLevel) {
                in 0..10 -> "\uF579"
                in 11..20 -> "\uF57A"
                in 21..30 -> "\uF57B"
                in 31..40 -> "\uF57C"
                in 41..50 -> "\uF57D"
                in 51..60 -> "\uF57E"
                in 61..70 -> "\uF57F"
                in 71..80 -> "\uF580"
                in 81..90 -> "\uF581"
                else -> "\uF578"
            }
        }

        @Suppress("NAME_SHADOWING")
        val context = context as Activity
        val iconView = (context).findViewById<TextView>(R.id.batteryIcon)
        iconView.text = icon

        val textView = (context).findViewById<TextView>(R.id.batteryText)
        textView.text = "$batteryLevel%"

    }
}

