package com.github.droidworksstudio.mlauncher.helper.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.droidworksstudio.common.getLocalizedString
import com.github.droidworksstudio.common.showLongToast
import com.github.droidworksstudio.mlauncher.R

class PrivateSpaceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // Check if the received action is for managed profile availability
        if (intent?.action == Intent.ACTION_PROFILE_AVAILABLE) {
            // Handle the event when the managed profile is available
            context.showLongToast(getLocalizedString(R.string.toast_private_space_unlocked))
        }
    }
}