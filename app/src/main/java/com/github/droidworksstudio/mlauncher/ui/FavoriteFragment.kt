/**
 * You can get to this fragment from the "reorder
 */

package com.github.droidworksstudio.mlauncher.ui

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ClipData
import android.content.Context
import android.content.Context.VIBRATOR_SERVICE
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Vibrator
import android.util.Log
import android.view.DragEvent
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.github.droidworksstudio.mlauncher.MainViewModel
import com.github.droidworksstudio.mlauncher.R
import com.github.droidworksstudio.mlauncher.data.Prefs
import com.github.droidworksstudio.mlauncher.databinding.FragmentFavoriteBinding
import com.github.droidworksstudio.mlauncher.helper.getHexForOpacity
import com.github.droidworksstudio.mlauncher.helper.hideStatusBar
import com.github.droidworksstudio.mlauncher.helper.showStatusBar

class FavoriteFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var deviceManager: DevicePolicyManager
    private lateinit var vibrator: Vibrator

    private var _binding: FragmentFavoriteBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoriteBinding.inflate(inflater, container, false)

        val view = binding.root
        prefs = Prefs(requireContext())

        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val backgroundColor = getHexForOpacity(prefs)
        binding.mainLayout.setBackgroundColor(backgroundColor)

        viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        deviceManager =
            context?.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        @Suppress("DEPRECATION")
        vibrator = context?.getSystemService(VIBRATOR_SERVICE) as Vibrator

        initObservers()
    }


    override fun onStart() {
        super.onStart()
        if (prefs.showStatusBar) showStatusBar(requireActivity()) else hideStatusBar(requireActivity())

        val backgroundColor = getHexForOpacity(prefs)
        binding.mainLayout.setBackgroundColor(backgroundColor)
    }


    private fun initObservers() {
        with(viewModel) {
            homeAppsNum.observe(viewLifecycleOwner) {
                updateAppCount(it)
            }
        }
    }

    private fun handleDragEvent(event: DragEvent, targetView: TextView): Boolean {
        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                // Indicate that we accept drag events
                return true
            }

            DragEvent.ACTION_DRAG_ENTERED -> {
                // Highlight the target view as the drag enters
                targetView.setBackgroundResource(R.drawable.reorder_apps_background)
                return true
            }

            DragEvent.ACTION_DRAG_EXITED -> {
                // Remove highlighting when the drag exits
                targetView.background = null
                return true
            }

            DragEvent.ACTION_DROP -> {
                // Remove highlighting
                targetView.background = null

                // Extract the dragged TextView
                val draggedTextView = event.localState as TextView

                // Reorder apps based on the drop position
                val draggedIndex =
                    (draggedTextView.parent as ViewGroup).indexOfChild(draggedTextView)
                val targetIndex = (targetView.parent as ViewGroup).indexOfChild(targetView)
                reorderApps(draggedIndex, targetIndex)

                return true
            }

            DragEvent.ACTION_DRAG_ENDED -> {
                // Remove highlighting when the drag ends
                targetView.background = null
                return true
            }

            else -> return false
        }
    }

    private fun reorderApps(draggedIndex: Int, targetIndex: Int) {
        // Ensure indices are within bounds
        if (draggedIndex < 0 || draggedIndex >= binding.homeAppsLayout.childCount ||
            targetIndex < 0 || targetIndex >= binding.homeAppsLayout.childCount
        ) {
            // Handle out of bounds indices gracefully, or log an error
            Log.e(
                "ReorderApps",
                "Invalid indices: draggedIndex=$draggedIndex, targetIndex=$targetIndex"
            )
            return
        }

        // Get references to the dragged and target views
        val draggedView = binding.homeAppsLayout.getChildAt(draggedIndex)

        // Remove the dragged view from its current position
        binding.homeAppsLayout.removeViewAt(draggedIndex)

        // Add the dragged view at the new position
        binding.homeAppsLayout.addView(draggedView, targetIndex)
        val packageDetailsOld = prefs.getHomeAppModel(draggedIndex)
        val packageDetailsNew = prefs.getHomeAppModel(targetIndex)
        prefs.setHomeAppModel(targetIndex, packageDetailsOld)
        prefs.setHomeAppModel(draggedIndex, packageDetailsNew)
    }

    /**
     * TODO it looks very complicated. Shouldn't we just re-render the whole list?
     *      When does it happen?
     *        - Only when the config option changes,
     *        - or also when we switch pages of the home screen?
     */

    @SuppressLint("InflateParams", "SetTextI18n")
    private fun updateAppCount(newAppsNum: Int) {
        binding.pageName.apply {
            text = getString(R.string.favorite_apps)
            textSize = prefs.appSize * 1.1f
            setTextColor(prefs.appColor)
        }
        
        val oldAppsNum = binding.homeAppsLayout.size // current number
        val diff = oldAppsNum - newAppsNum

        if (diff in 1 until oldAppsNum) { // 1 <= diff <= oldNumApps
            binding.homeAppsLayout.children.drop(diff)
        } else if (diff < 0) {
            val prefixDrawable: Drawable? =
                context?.let { ContextCompat.getDrawable(it, R.drawable.ic_order_apps) }
            // add all missing apps to list
            for (i in oldAppsNum until newAppsNum) {
                val view = layoutInflater.inflate(R.layout.home_app_button, null) as TextView
                view.apply {
                    val appLabel =
                        prefs.getHomeAppModel(i).activityLabel.ifEmpty { getString(R.string.select_app) }
                    textSize = prefs.appSize.toFloat()
                    setTextColor(prefs.appColor)
                    id = i
                    text = appLabel
                    setCompoundDrawablesWithIntrinsicBounds(null, null, prefixDrawable, null)
                    // Set the gravity to align the text to the end (right side in LTR)
                    gravity = Gravity.START
                }
                val padding: Int = prefs.textPaddingSize
                view.setPadding(0, padding, 0, padding)
                binding.homeAppsLayout.addView(view)
            }
        }

        for (i in 0 until newAppsNum) {
            val view = binding.homeAppsLayout.getChildAt(i) as TextView
            view.setOnDragListener { v, event ->
                handleDragEvent(event, v as TextView)
            }
            view.setOnLongClickListener { v ->
                val dragData = ClipData.newPlainText("", "")
                val shadowBuilder = View.DragShadowBuilder(v)
                v.startDragAndDrop(dragData, shadowBuilder, v, 0)
                true
            }
        }

        // Scroll to the top of the list after updating
        (binding.homeAppsLayout.parent as? ScrollView)?.scrollTo(0, 0)
    }
}
