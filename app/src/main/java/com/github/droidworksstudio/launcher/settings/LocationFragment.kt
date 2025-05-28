package com.github.droidworksstudio.launcher.settings

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.github.droidworksstudio.launcher.R
import com.github.droidworksstudio.launcher.utils.AppMenuEdgeFactory
import com.github.droidworksstudio.launcher.utils.StringUtils
import com.github.droidworksstudio.launcher.utils.UIUtils
import com.github.droidworksstudio.launcher.utils.WeatherSystem
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LocationFragment : Fragment(), LocationListAdapter.OnItemClickListener, TitleProvider {

    private var adapter: LocationListAdapter? = null
    private lateinit var weatherSystem: WeatherSystem
    private lateinit var sharedPreferenceManager: SharedPreferenceManager
    private val stringUtils = StringUtils()
    private lateinit var uiUtils: UIUtils

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_location, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        uiUtils = UIUtils(requireContext())
        weatherSystem = WeatherSystem(requireContext())
        sharedPreferenceManager = SharedPreferenceManager(requireContext())

        val searchView = view.findViewById<TextInputEditText>(R.id.locationSearch)

        var locationList = mutableListOf<Map<String, String>>()

        stringUtils.setLink(requireActivity().findViewById(R.id.locationLink), getString(R.string.location_link))

        lifecycleScope.launch(Dispatchers.IO) {
            locationList = weatherSystem.getSearchedLocations(
                searchView.text.toString()
            )
        }

        adapter = LocationListAdapter(requireContext(), locationList, this)
        val recyclerView = view.findViewById<RecyclerView>(R.id.locationRecycler)
        val appMenuEdgeFactory = AppMenuEdgeFactory(requireActivity())
        uiUtils.setSearchAlignment(searchView)
        uiUtils.setSearchSize(searchView)

        recyclerView.edgeEffectFactory = appMenuEdgeFactory
        recyclerView.adapter = adapter

        recyclerView.scrollToPosition(0)



        recyclerView.addOnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->

            if (bottom - top > oldBottom - oldTop) {
                searchView.clearFocus()
            }
        }

        searchView.addTextChangedListener(object :
            TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                // Filtering is not needed since we are creating a list with data pulled from the Open-Meteo api instead of searching an existing list
                lifecycleScope.launch(Dispatchers.IO) {
                    val locations = weatherSystem.getSearchedLocations(
                        searchView.text.toString()
                    )
                    withContext(Dispatchers.Main) {
                        adapter?.updateLocations(locations)
                    }
                }
            }
        })

        if (sharedPreferenceManager.isAutoKeyboardEnabled()) {
            val imm =
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            searchView.requestFocus()
            imm.showSoftInput(searchView, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun showConfirmationDialog(locationName: String?, latitude: String?, longitude: String?) {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(getString(R.string.confirm_title))
            setMessage("${getString(R.string.app_confirm_text)} $locationName?")
            setPositiveButton(getString(R.string.confirm_yes)) { _, _ ->
                performConfirmedAction(locationName, latitude, longitude)
            }
            setNegativeButton(R.string.confirm_no) { _, _ ->

            }

        }.create().show()
    }

    private fun performConfirmedAction(locationName: String?, latitude: String?, longitude: String?) {
        sharedPreferenceManager.setWeatherLocation("latitude=${latitude}&longitude=${longitude}", locationName)
        requireActivity().supportFragmentManager.popBackStack()
    }


    override fun onItemClick(name: String?, latitude: String?, longitude: String?) {
        showConfirmationDialog(name, latitude, longitude)
    }

    override fun getTitle(): String {
        return getString(R.string.find_your_city)
    }

}