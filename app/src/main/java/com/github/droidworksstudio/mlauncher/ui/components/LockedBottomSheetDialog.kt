package com.github.droidworksstudio.mlauncher.ui.components

import android.content.Context
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * BottomSheetDialog that:
 * - Disables swipe-to-dismiss
 * - Keeps tap outside, back button, and programmatic `.hide()` working
 */
class LockedBottomSheetDialog(context: Context) : BottomSheetDialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        behavior.apply {
            isDraggable = false         // ❌ Prevent swipe-to-dismiss
            skipCollapsed = true        // ✅ Always start expanded
            // DO NOT set isHideable = false — this allows `.hide()` to still work
        }
    }

    override fun onStart() {
        super.onStart()
        behavior.state = BottomSheetBehavior.STATE_EXPANDED // ✅ Force full expansion on show
    }
}
