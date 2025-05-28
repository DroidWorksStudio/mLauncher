package com.github.droidworksstudio.launcher.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.github.droidworksstudio.launcher.R
import com.github.droidworksstudio.launcher.utils.StringUtils

class AboutFragment : Fragment(), TitleProvider {

    private val stringUtils = StringUtils()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val launcherApps = requireActivity().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

        // Set up about page links
        stringUtils.setLink(requireActivity().findViewById(R.id.creditText), getString(R.string.my_website_link))
        stringUtils.setLink(requireActivity().findViewById(R.id.gitlabLink), getString(R.string.gitlab_link))
        stringUtils.setLink(requireActivity().findViewById(R.id.githubLink), getString(R.string.github_link))
        stringUtils.setLink(requireActivity().findViewById(R.id.buymeacoffeeLink), getString(R.string.buymeacoffee_link))
        stringUtils.setLink(requireActivity().findViewById(R.id.liberaLink), getString(R.string.libera_link))
        stringUtils.setLink(requireActivity().findViewById(R.id.weatherLink), getString(R.string.weather_link))

        requireActivity().findViewById<ImageView>(R.id.iconView).setOnClickListener {
            launcherApps.startAppDetailsActivity(
                ComponentName(requireContext(), this::class.java),
                launcherApps.profiles[0],
                null,
                null
            )
        }
    }

    override fun getTitle(): String {
        return getString(R.string.about_title)
    }
}