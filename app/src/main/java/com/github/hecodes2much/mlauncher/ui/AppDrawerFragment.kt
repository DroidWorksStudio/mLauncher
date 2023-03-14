package com.github.hecodes2much.mlauncher.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.hecodes2much.mlauncher.MainViewModel
import com.github.hecodes2much.mlauncher.R
import com.github.hecodes2much.mlauncher.data.AppModel
import com.github.hecodes2much.mlauncher.data.Constants
import com.github.hecodes2much.mlauncher.data.Constants.AppDrawerFlag
import com.github.hecodes2much.mlauncher.data.Prefs
import com.github.hecodes2much.mlauncher.databinding.FragmentAppDrawerBinding
import com.github.hecodes2much.mlauncher.helper.getHexFontColor
import com.github.hecodes2much.mlauncher.helper.getHexForOpacity
import com.github.hecodes2much.mlauncher.helper.openAppInfo

class AppDrawerFragment : Fragment() {

    private lateinit var prefs: Prefs
    private var _binding: FragmentAppDrawerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppDrawerBinding.inflate(inflater, container, false)
        prefs = Prefs(requireContext())
        return binding.root
    }

    @SuppressLint("RtlHardcoded")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val hex = getHexForOpacity(requireContext(), prefs)
        binding.mainLayout.setBackgroundColor(hex)

        val flagString = arguments?.getString("flag", AppDrawerFlag.LaunchApp.toString()) ?: AppDrawerFlag.LaunchApp.toString()
        val flag = AppDrawerFlag.valueOf(flagString)
        val n = arguments?.getInt("n", 0) ?: 0

        when (flag) {
            AppDrawerFlag.SetHomeApp -> {
                binding.drawerButton.text = getString(R.string.rename)
                binding.drawerButton.isVisible = true
                binding.drawerButton.setOnClickListener { renameListener(flag, n) }
            }
            AppDrawerFlag.SetSwipeRight,
            AppDrawerFlag.SetSwipeLeft,
            AppDrawerFlag.SetSwipeUp,
            AppDrawerFlag.SetSwipeDown,
            AppDrawerFlag.SetClickClock,
            AppDrawerFlag.SetClickDate -> {
                binding.drawerButton.setOnClickListener {
                    findNavController().popBackStack()
                }
            }
            else -> {}
        }

        val viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        val gravity = when(Prefs(requireContext()).drawerAlignment) {
            Constants.Gravity.Left -> Gravity.LEFT
            Constants.Gravity.Center -> Gravity.CENTER
            Constants.Gravity.Right -> Gravity.RIGHT
        }

        val appAdapter = AppDrawerAdapter(
            flag,
            gravity,
            appClickListener(viewModel, flag, n),
            appInfoListener(),
            appShowHideListener(),
            this.appRenameListener()
        )

        val searchTextView = binding.search.findViewById<TextView>(R.id.search_src_text)
        if (searchTextView != null) searchTextView.gravity = gravity

        if (prefs.followAccentColors) {
            val fontColor = getHexFontColor(requireActivity())
            searchTextView.setTextColor(fontColor)
        }
        if (prefs.useCustomIconFont) {
            val typeface = ResourcesCompat.getFont(requireActivity(), R.font.roboto)
            searchTextView.typeface = typeface
        }
        val textSize = prefs.textSizeLauncher.toFloat()
        searchTextView.textSize = textSize

        initViewModel(flag, viewModel, appAdapter)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = appAdapter
        binding.recyclerView.addOnScrollListener(getRecyclerViewOnScrollListener())

        if (flag == AppDrawerFlag.HiddenApps) binding.search.queryHint = getString(R.string.hidden_apps)
        if (flag == AppDrawerFlag.LaunchApp) binding.search.queryHint = getString(R.string.show_apps)
        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                appAdapter.launchFirstInList()
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    appAdapter.filter.filter(it.trim())
                }
                return false
            }
        })
    }

    override fun onResume() {
        super.onResume()
        val hex = getHexForOpacity(requireContext(), prefs)
        binding.mainLayout.setBackgroundColor(hex)
    }

    private fun initViewModel(flag: AppDrawerFlag, viewModel: MainViewModel, appAdapter: AppDrawerAdapter) {
        viewModel.hiddenApps.observe(viewLifecycleOwner, Observer {
            if (flag != AppDrawerFlag.HiddenApps) return@Observer
            it?.let { appList ->
                binding.listEmptyHint.visibility = if (appList.isEmpty()) View.VISIBLE else View.GONE
                populateAppList(appList, appAdapter)
            }
        })

        viewModel.appList.observe(viewLifecycleOwner, Observer {
            if (flag == AppDrawerFlag.HiddenApps) return@Observer
            if (it == appAdapter.appsList) return@Observer
            it?.let { appList ->
                binding.listEmptyHint.visibility = if (appList.isEmpty()) View.VISIBLE else View.GONE
                populateAppList(appList, appAdapter)
            }
        })

        viewModel.firstOpen.observe(viewLifecycleOwner) {
            if (it) binding.appDrawerTip.visibility = View.VISIBLE
        }
    }

    override fun onStart() {
        super.onStart()
        binding.search.showKeyboard()
    }

    override fun onStop() {
        binding.search.hideKeyboard()
        super.onStop()
    }

    private fun View.hideKeyboard() {
        view?.clearFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    private fun View.showKeyboard() {
        if (!Prefs(requireContext()).autoShowKeyboard) return

        val searchTextView = binding.search.findViewById<TextView>(R.id.search_src_text)
        searchTextView.requestFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        searchTextView.postDelayed({
            searchTextView.requestFocus()
            @Suppress("DEPRECATION")
            imm.showSoftInput(searchTextView, InputMethodManager.SHOW_FORCED)
        }, 100)
    }

    private fun populateAppList(apps: List<AppModel>, appAdapter: AppDrawerAdapter) {
        val animation = AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_anim_from_bottom)
        binding.recyclerView.layoutAnimation = animation
        appAdapter.setAppList(apps.toMutableList())
    }

    private fun appClickListener(viewModel: MainViewModel, flag: AppDrawerFlag, n: Int = 0): (appModel: AppModel) -> Unit =
        { appModel ->
            viewModel.selectedApp(appModel, flag, n)
            if (flag == AppDrawerFlag.LaunchApp || flag == AppDrawerFlag.HiddenApps)
                findNavController().popBackStack(R.id.mainFragment, false)
            else
                findNavController().popBackStack()
        }

    private fun appInfoListener(): (appModel: AppModel) -> Unit =
        { appModel ->
            openAppInfo(
                requireContext(),
                appModel.user,
                appModel.appPackage
            )
            findNavController().popBackStack(R.id.mainFragment, false)
        }

    private fun appShowHideListener(): (flag: AppDrawerFlag, appModel: AppModel) -> Unit =
        { flag, appModel ->
            val prefs = Prefs(requireContext())
            val newSet = mutableSetOf<String>()
            newSet.addAll(prefs.hiddenApps)

            if (flag == AppDrawerFlag.HiddenApps) {
                newSet.remove(appModel.appPackage) // for backward compatibility
                newSet.remove(appModel.appPackage + "|" + appModel.user.toString())
            } else newSet.add(appModel.appPackage + "|" + appModel.user.toString())

            prefs.hiddenApps = newSet

            if (newSet.isEmpty()) findNavController().popBackStack()
        }
    private fun appRenameListener(): (appPackage: String, appAlias: String) -> Unit =
        { appPackage, appAlias ->
            val prefs = Prefs(requireContext())
            prefs.setAppAlias(appPackage, appAlias)
        }

    private fun renameListener(flag: AppDrawerFlag, i: Int) {
        val name = binding.search.query.toString().trim()
        if (name.isEmpty()) return
        if (flag == AppDrawerFlag.SetHomeApp) {
            Prefs(requireContext()).setHomeAppName(i, name)
        }

        findNavController().popBackStack()
    }

    private fun getRecyclerViewOnScrollListener(): RecyclerView.OnScrollListener {
        return object : RecyclerView.OnScrollListener() {

            var onTop = false

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                when (newState) {

                    RecyclerView.SCROLL_STATE_DRAGGING -> {
                        onTop = !recyclerView.canScrollVertically(-1)
                        if (onTop) binding.search.hideKeyboard()
                        if (onTop && !recyclerView.canScrollVertically(1))
                            findNavController().popBackStack()
                    }

                    RecyclerView.SCROLL_STATE_IDLE -> {
                        if (!recyclerView.canScrollVertically(1)) {
                            binding.search.hideKeyboard()
                        } else if (!recyclerView.canScrollVertically(-1)) {
                            if (onTop) findNavController().popBackStack()
                            else binding.search.showKeyboard()
                        }
                    }
                }
            }
        }
    }
}
