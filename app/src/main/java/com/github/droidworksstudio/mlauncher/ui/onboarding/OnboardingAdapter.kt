package com.github.droidworksstudio.mlauncher.ui.onboarding

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.github.droidworksstudio.mlauncher.R

class OnboardingAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    // Return the number of pages
    override fun getItemCount(): Int = 4  // Total number of pages in the onboarding flow

    // Return the corresponding fragment for each page
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> OnboardingPageFragment.newInstance(R.layout.fragment_onboarding_page_one)
            1 -> OnboardingPageFragment.newInstance(R.layout.fragment_onboarding_page_two)
            2 -> OnboardingPageFragment.newInstance(R.layout.fragment_onboarding_page_three)
            3 -> OnboardingPageFragment.newInstance(R.layout.fragment_onboarding_page_four)
            else -> throw IllegalArgumentException("Invalid page position: $position")
        }
    }
}

