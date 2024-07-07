package com.github.droidworksstudio.mlauncher.ui

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ClipData
import android.content.Context
import android.content.Context.VIBRATOR_SERVICE
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.util.Log
import android.view.DragEvent
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.github.droidworksstudio.mlauncher.MainViewModel
import com.github.droidworksstudio.mlauncher.R
import com.github.droidworksstudio.mlauncher.data.Prefs
import com.github.droidworksstudio.mlauncher.databinding.FragmentReorderHomeAppsBinding
import com.github.droidworksstudio.mlauncher.helper.getHexFontColor
import com.github.droidworksstudio.mlauncher.helper.getHexForOpacity
import com.github.droidworksstudio.mlauncher.helper.hideStatusBar
import com.github.droidworksstudio.mlauncher.helper.showStatusBar

class ReorderHomeAppsFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var deviceManager: DevicePolicyManager
    private lateinit var vibrator: Vibrator

    private var _binding: FragmentReorderHomeAppsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReorderHomeAppsBinding.inflate(inflater, container, false)

        val view = binding.root
        prefs = Prefs(requireContext())

        return view
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val hex = getHexForOpacity(requireContext(), prefs)
        binding.mainLayout.setBackgroundColor(hex)

        viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        deviceManager =
            context?.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        @Suppress("DEPRECATION")
        vibrator = context?.getSystemService(VIBRATOR_SERVICE) as Vibrator

        initObservers()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onStart() {
        super.onStart()
        if (prefs.showStatusBar) showStatusBar(requireActivity()) else hideStatusBar(requireActivity())

        val backgroundColor = getHexForOpacity(requireContext(), prefs)
        binding.mainLayout.setBackgroundColor(backgroundColor)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun initObservers() {
        with(viewModel) {
            homeAppsAlignment.observe(viewLifecycleOwner) { (gravity, onBottom) ->
                val horizontalAlignment = if (onBottom) Gravity.BOTTOM else Gravity.CENTER_VERTICAL
                binding.homeAppsLayout.gravity = gravity.value() or horizontalAlignment

                binding.homeAppsLayout.children.forEach { view ->
                    (view as TextView).gravity = gravity.value()
                }
            }
            homeAppsCount.observe(viewLifecycleOwner) {
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
                return true
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                // Remove highlighting when the drag ends
                targetView.background = null
                return true
            }
            else -> {
                // Extract the dragged TextView
                val draggedTextView = event.localState as TextView

                // Reorder apps based on the drop position
                val draggedIndex = (draggedTextView.parent as ViewGroup).indexOfChild(draggedTextView)
                val targetIndex = (targetView.parent as ViewGroup).indexOfChild(targetView)
                reorderApps(draggedIndex, targetIndex)

                return false
            }
        }
    }

    private fun reorderApps(draggedIndex: Int, targetIndex: Int) {
        // Ensure indices are within bounds
        if (draggedIndex < 0 || draggedIndex >= binding.homeAppsLayout.childCount ||
            targetIndex < 0 || targetIndex >= binding.homeAppsLayout.childCount
        ) {
            // Handle out of bounds indices gracefully, or log an error
            Log.e("ReorderApps", "Invalid indices: draggedIndex=$draggedIndex, targetIndex=$targetIndex")
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

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("InflateParams", "SetTextI18n")
    private fun updateAppCount(newAppsNum: Int) {
        val oldAppsNum = binding.homeAppsLayout.size // current number
        val diff = oldAppsNum - newAppsNum

        if (diff in 1 until oldAppsNum) { // 1 <= diff <= oldNumApps
            binding.homeAppsLayout.children.drop(diff)
        } else if (diff < 0) {
            val prefixDrawable: Drawable? = context?.let { ContextCompat.getDrawable(it, R.drawable.ic_prefix_drawable) }
            // add all missing apps to list
            for (i in oldAppsNum until newAppsNum) {
                val view = layoutInflater.inflate(R.layout.home_app_button, null) as TextView
                view.apply {
                    val appLabel = prefs.getHomeAppModel(i).appLabel.ifEmpty { getString(R.string.app) }
                    textSize = prefs.appSize.toFloat()
                    id = i
                    text = "   $appLabel"
                    setCompoundDrawablesWithIntrinsicBounds(prefixDrawable, null, null, null)
                    if (!prefs.extendHomeAppsArea) {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }
                }
                val padding: Int = prefs.textPaddingSize
                view.setPadding(0, padding, 0, padding)
                binding.pageName.text = getString(R.string.reorder_apps)
                binding.pageName.textSize = prefs.appSize * 1.5f

                if (prefs.followAccentColors) {
                    val fontColor = getHexFontColor(requireContext(), prefs)
                    view.setTextColor(fontColor)
                }

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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    v.startDragAndDrop(dragData, shadowBuilder, v, 0)
                } else {
                    @Suppress("DEPRECATION")
                    v.startDrag(dragData, shadowBuilder, v, 0)
                }
                true
            }
        }
    }
}
