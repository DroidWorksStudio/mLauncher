package com.github.droidworksstudio.launcher.settings

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.github.droidworksstudio.launcher.R
import com.github.droidworksstudio.launcher.utils.StringUtils

class AboutFragment : Fragment() {

    private val stringUtils = StringUtils()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up about page links
        stringUtils.setLink(requireActivity().findViewById(R.id.creditText), getString(R.string.my_website_link))
        stringUtils.setLink(requireActivity().findViewById(R.id.gitlabLink), getString(R.string.gitlab_link))
        stringUtils.setLink(requireActivity().findViewById(R.id.githubLink), getString(R.string.github_link))
        stringUtils.setLink(requireActivity().findViewById(R.id.buymeacoffeeLink), getString(R.string.buymeacoffee_link))
        stringUtils.setLink(requireActivity().findViewById(R.id.liberaLink), getString(R.string.libera_link))
        stringUtils.setLink(requireActivity().findViewById(R.id.weatherLink), getString(R.string.weather_link))

        requireActivity().findViewById<ImageView>(R.id.iconView).setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:${requireContext().packageName}".toUri()
            }
            startActivity(intent)
        }
    }
}