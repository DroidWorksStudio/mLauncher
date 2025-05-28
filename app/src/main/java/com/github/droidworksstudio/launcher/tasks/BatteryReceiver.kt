package com.github.droidworksstudio.launcher.tasks

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.github.droidworksstudio.launcher.MainActivity

class BatteryReceiver(private val activity: MainActivity) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let {
            val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val batteryPct = level * 100 / scale.toFloat()
            activity.modifyDate("${batteryPct.toInt()}%", 3) // Add battery to the date
        }
    }

    companion object {
        fun register(context: Context, activity: MainActivity): BatteryReceiver {
            val receiver = BatteryReceiver(activity)
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            context.registerReceiver(receiver, filter)
            return receiver
        }
    }
}