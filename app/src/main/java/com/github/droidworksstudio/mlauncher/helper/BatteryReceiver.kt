package com.github.droidworksstudio.mlauncher.helper

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.widget.TextView
import com.github.droidworksstudio.mlauncher.R
import com.github.droidworksstudio.mlauncher.data.Prefs

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
        if (isCharging) {
            icon = "\uDB80\uDC84"
        } else {
            icon = when (batteryLevel) {
                in 0..10 -> "\uDB80\uDC7A"
                in 11..20 -> "\uDB80\uDC7B"
                in 21..30 -> "\uDB80\uDC7C"
                in 31..40 -> "\uDB80\uDC7D"
                in 41..50 -> "\uDB80\uDC7E"
                in 51..60 -> "\uDB80\uDC7F"
                in 61..70 -> "\uDB80\uDC80"
                in 71..80 -> "\uDB80\uDC81"
                in 81..90 -> "\uDB80\uDC82"
                else -> "\uDB80\uDC79"
            }
        }

        val contextBattery = context as? Activity
        val iconView = (contextBattery)?.findViewById<TextView>(R.id.batteryIcon)
        iconView?.text = icon

        val textView = (contextBattery)?.findViewById<TextView>(R.id.batteryText)
        textView?.text = "$batteryLevel%"
        textView?.setOnClickListener {
            val powerUsageIntent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
            context.startActivity(powerUsageIntent)
        }
    }
}

