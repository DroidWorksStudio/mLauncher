package com.github.droidworksstudio.mlauncher.ui.onboarding

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.github.droidworksstudio.common.getLocalizedString
import com.github.droidworksstudio.common.openAccessibilitySettings
import com.github.droidworksstudio.common.requestLocationPermission
import com.github.droidworksstudio.common.requestUsagePermission
import com.github.droidworksstudio.mlauncher.MainActivity
import com.github.droidworksstudio.mlauncher.MainViewModel
import com.github.droidworksstudio.mlauncher.R
import com.github.droidworksstudio.mlauncher.data.Constants
import com.github.droidworksstudio.mlauncher.data.Prefs
import com.github.droidworksstudio.mlauncher.helper.hasLocationPermission
import com.github.droidworksstudio.mlauncher.helper.hasUsageAccessPermission
import com.github.droidworksstudio.mlauncher.helper.ismlauncherDefault
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class OnboardingPageFragment : Fragment() {

    // Declare the ActivityResultLauncher as a property of the Fragment
    private lateinit var roleRequestLauncher: ActivityResultLauncher<Intent>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Register the ActivityResultLauncher before the fragment's view is created
        roleRequestLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { _ ->
        }
    }

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel

    private var layoutResId: Int = 0

    private lateinit var title: TextView
    private lateinit var description: TextView
    private lateinit var permissionText: TextView
    private lateinit var permissionReviewText: TextView
    private lateinit var permissionRemovedText: TextView
    private lateinit var permissionButton: Button
    private lateinit var nextButton: Button
    private lateinit var startButton: Button
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        fun newInstance(layoutResId: Int): OnboardingPageFragment {
            val fragment = OnboardingPageFragment()
            val args = Bundle()
            args.putInt("layoutResId", layoutResId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        prefs = Prefs(requireContext())

        layoutResId = arguments?.getInt("layoutResId") ?: 0
        return inflater.inflate(layoutResId, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewPager = activity?.findViewById<ViewPager2>(R.id.viewPager)

        viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        viewModel.ismlauncherDefault()

        title = view.findViewById(R.id.title)
        description = view.findViewById(R.id.description)
        permissionButton = view.findViewById(R.id.permissionButton)
        permissionText = view.findViewById(R.id.permissionText)
        permissionReviewText = view.findViewById(R.id.permissionReviewText)
        permissionRemovedText = view.findViewById(R.id.permissionRemovedText)
        nextButton = view.findViewById(R.id.nextButton)
        startButton = view.findViewById(R.id.startButton)

        when (layoutResId) {
            R.layout.fragment_onboarding_page_one -> {
                val appName = getString(R.string.app_name)
                title.text = getLocalizedString(R.string.welcome_to_launcher, appName)

                val privacyPolicyText = getLocalizedString(R.string.continue_by_you_agree, getString(R.string.privacy_policy))
                val clickableText = getString(R.string.privacy_policy) // This should be the actual clickable portion

                val spannableString = SpannableString(privacyPolicyText)

                val startIndex = privacyPolicyText.indexOf(clickableText)
                val endIndex = startIndex + clickableText.length

                if (startIndex >= 0) {
                    val clickableSpan = object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            val intent = Intent(Intent.ACTION_VIEW, getString(R.string.privacy_policy_url).toUri())
                            context?.startActivity(intent)
                        }
                    }

                    spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }

                description.text = spannableString
                // Make sure to enable links to make the clickable span work
                description.movementMethod = LinkMovementMethod.getInstance()

                handler.removeCallbacks(usagePermissionCheckRunnable)
                handler.post(launcherDefaultCheckRunnable)

                permissionButton.text = getLocalizedString(R.string.advanced_settings_set_as_default_launcher)
                permissionButton.setOnClickListener {
                    setDefaultHomeScreen()
                }

                nextButton.text = getLocalizedString(R.string.next)
                nextButton.setOnClickListener {
                    viewPager?.let {
                        // Move to the next page
                        it.currentItem += 1
                    }
                }
            }

            R.layout.fragment_onboarding_page_two -> {
                // Start checking if permission is granted
                handler.removeCallbacks(launcherDefaultCheckRunnable)
                handler.removeCallbacks(locationPermissionCheckRunnable)
                handler.post(usagePermissionCheckRunnable)

                // Request Permission Button
                permissionButton.setOnClickListener {
                    requireContext().requestUsagePermission()
                }

                nextButton.text = getLocalizedString(R.string.next)
                // Next Button to move to the next page
                nextButton.setOnClickListener {
                    viewPager?.let {
                        // Move to the next page
                        it.currentItem += 1
                    }
                }
            }

            R.layout.fragment_onboarding_page_three -> {
                // Start checking if permission is granted
                handler.removeCallbacks(launcherDefaultCheckRunnable)
                handler.removeCallbacks(usagePermissionCheckRunnable)
                handler.post(locationPermissionCheckRunnable)

                // Request Permission Button
                permissionButton.setOnClickListener {
                    requireContext().requestLocationPermission(Constants.ACCESS_FINE_LOCATION)
                }

                nextButton.text = getLocalizedString(R.string.next)
                // Next Button to move to the next page
                nextButton.setOnClickListener {
                    viewPager?.let {
                        // Move to the next page
                        it.currentItem += 1
                    }
                }
            }

            R.layout.fragment_onboarding_page_four -> {
                // Start checking if permission is granted
                handler.removeCallbacks(launcherDefaultCheckRunnable)
                handler.removeCallbacks(usagePermissionCheckRunnable)
                handler.removeCallbacks(locationPermissionCheckRunnable)

                // Request Permission Button
                permissionButton.setOnClickListener {
                    val instructions = getLocalizedString(R.string.accessibility_service_more_info)

                    val builder = MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getLocalizedString(R.string.accessibility_service_why_we_need))
                        .setMessage(instructions)
                        .setPositiveButton(getLocalizedString(R.string.allow)) { dialog, _ ->
                            dialog.dismiss()
                            requireContext().openAccessibilitySettings()
                        }
                        .setNegativeButton(getLocalizedString(R.string.deny)) { dialog, _ ->
                            dialog.dismiss()
                            // Handle button click
                            startActivity(Intent(requireActivity(), MainActivity::class.java))
                            prefs.setOnboardingCompleted(true)
                            requireActivity().finish()
                        }
                        .setCancelable(true) // Allow the user to cancel

                    builder.create().show()
                }

                startButton.text = getLocalizedString(R.string.start)
                // Next Button to move to the next page
                startButton.setOnClickListener {
                    // Handle button click
                    startActivity(Intent(requireActivity(), MainActivity::class.java))
                    prefs.setOnboardingCompleted(true)
                    requireActivity().finish()
                }
            }
        }
    }

    private val launcherDefaultCheckRunnable = object : Runnable {
        override fun run() {
            if (ismlauncherDefault(requireContext())) {
                nextButton.isEnabled = true
                permissionButton.isEnabled = false
            } else {
                permissionButton.isEnabled = true
                nextButton.isEnabled = false
            }
            handler.postDelayed(this, 1000)  // Check every 1 second
        }
    }

    private val usagePermissionCheckRunnable = object : Runnable {
        override fun run() {
            if (hasUsageAccessPermission(requireContext())) {
                permissionText.text = getLocalizedString(R.string.permission_granted)
                nextButton.isEnabled = true
                permissionRemovedText.visibility = View.GONE
                permissionReviewText.visibility = View.GONE
                permissionButton.isEnabled = false
            } else {
                permissionText.text = getLocalizedString(R.string.grant_usage_permission)
                permissionButton.isEnabled = true
                nextButton.isEnabled = false
            }
            handler.postDelayed(this, 1000)  // Check every 1 second
        }
    }

    private val locationPermissionCheckRunnable = object : Runnable {
        override fun run() {
            if (hasLocationPermission(requireContext())) {
                permissionText.text = getLocalizedString(R.string.permission_granted)
                nextButton.isEnabled = true
                permissionRemovedText.visibility = View.GONE
                permissionReviewText.visibility = View.GONE
                permissionButton.isEnabled = false
            } else {
                permissionText.text = getLocalizedString(R.string.grant_location_permission)
                permissionButton.isEnabled = true
                nextButton.isEnabled = true
            }
            handler.postDelayed(this, 1000)  // Check every 1 second
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(launcherDefaultCheckRunnable)
        handler.removeCallbacks(usagePermissionCheckRunnable)
        handler.removeCallbacks(locationPermissionCheckRunnable)
    }

    private fun setDefaultHomeScreen() {
        // Get the RoleManager system service
        val roleManager = requireContext().getSystemService(Context.ROLE_SERVICE) as RoleManager

        if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
            // Check if this app does not have the ROLE_HOME
            if (!roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                // Request to set this app as the default home screen
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                roleRequestLauncher.launch(intent) // Launch the intent to request the role
            }
        }
    }
}

