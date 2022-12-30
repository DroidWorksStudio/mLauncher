package app.mlauncher.ui


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.view.animation.AnimationUtils
import com.google.android.material.progressindicator.CircularProgressIndicator
import app.mlauncher.R

internal class BatteryReceiver(private val progressBar: CircularProgressIndicator) : BroadcastReceiver() {

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

    override fun onReceive(context: Context?, intent: Intent?) {
        /* set battery percentage value to the circular progress bar */
        progressBar.progress = batteryPercentage(intent!!)

        /* progress bar animation */
        if (chargingStatus(intent) == BatteryManager.BATTERY_STATUS_CHARGING ||
            chargingStatus(intent) == BatteryManager.BATTERY_STATUS_FULL) {
            progressBar.startAnimation(
                AnimationUtils.loadAnimation(context, R.anim.rotate_clockwise)
            )
        } else if (chargingStatus(intent) == BatteryManager.BATTERY_STATUS_DISCHARGING ||
            chargingStatus(intent) == BatteryManager.BATTERY_STATUS_NOT_CHARGING) {
            progressBar.clearAnimation()
        }
    }

}